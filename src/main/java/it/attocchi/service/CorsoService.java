package it.attocchi.service;

import it.attocchi.entity.Corso;
import it.attocchi.entity.GruppoCorso;
import it.attocchi.entity.Iscrizione;
import it.attocchi.entity.IscrizioneId;
import it.attocchi.entity.Lezione;
import it.attocchi.entity.Presenza;
import it.attocchi.entity.Allievo;
import it.attocchi.repository.CorsoRepository;
import it.attocchi.repository.GruppoCorsoRepository;
import it.attocchi.repository.IscrizioneRepository;
import it.attocchi.repository.LezioneRepository;
import it.attocchi.repository.PagamentoRepository;
import it.attocchi.repository.PresenzaRepository;
import it.attocchi.repository.AllievoRepository;
import it.attocchi.views.Formati;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class CorsoService {

    private final CorsoRepository corsoRepository;
    private final LezioneRepository lezioneRepository;
    private final PresenzaRepository presenzaRepository;
    private final IscrizioneRepository iscrizioneRepository;
    private final AllievoRepository allievoRepository;
    private final PagamentoRepository pagamentoRepository;
    private final FestivitaService festivitaService;
    private final GruppoCorsoRepository gruppoRepository;

    public CorsoService(CorsoRepository corsoRepository, LezioneRepository lezioneRepository,
                       PresenzaRepository presenzaRepository, IscrizioneRepository iscrizioneRepository,
                       AllievoRepository allievoRepository, PagamentoRepository pagamentoRepository,
                       FestivitaService festivitaService, GruppoCorsoRepository gruppoRepository) {
        this.gruppoRepository = gruppoRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.festivitaService = festivitaService;
        this.corsoRepository = corsoRepository;
        this.lezioneRepository = lezioneRepository;
        this.presenzaRepository = presenzaRepository;
        this.iscrizioneRepository = iscrizioneRepository;
        this.allievoRepository = allievoRepository;
    }

    public List<Corso> findAll() {
        return corsoRepository.findAllWithAllievi();
    }

    public List<Corso> findAllAttivi() {
        return corsoRepository.findByAttivoTrue();
    }

    public Corso findById(Long id) {
        return corsoRepository.findById(id).orElse(null);
    }

    public List<Corso> findByInsegnante(Long insegnanteId) {
        return corsoRepository.findDellInsegnante(insegnanteId);
    }

    /** Tutti i corsi dell'allievo, anche non attivi (consultazione del genitore). */
    public List<Corso> findByAllievo(Long allievoId) {
        return corsoRepository.findByAllievoId(allievoId);
    }

    /** Solo i corsi attivi dell'allievo: l'allievo non vede i corsi non attivi. */
    public List<Corso> findAttiviByAllievo(Long allievoId) {
        return corsoRepository.findByAllievoId(allievoId).stream().filter(Corso::isAttivo).toList();
    }

    /**
     * In un corso non attivo partecipanti, orari, lezioni e presenze non sono modificabili
     * (quote e pagamenti sì).
     */
    static void verificaAttivo(Corso corso) {
        if (!corso.isAttivo()) {
            throw new IllegalStateException("Il corso \"" + corso.getNome() + "\" non è attivo");
        }
    }

    private Corso corsoAttivo(Long corsoId) {
        Corso corso = corsoRepository.findById(corsoId)
            .orElseThrow(() -> new IllegalArgumentException("Corso non trovato: " + corsoId));
        verificaAttivo(corso);
        return corso;
    }

    /** Partecipanti del corso in ordine di orario di lezione (senza orario in fondo), vedi {@link Iscrizione#PER_ORARIO}. */
    public List<Iscrizione> findPartecipanti(Long corsoId) {
        return iscrizioneRepository.findByCorsoIdWithAllievo(corsoId).stream()
            .sorted(Iscrizione.PER_ORARIO)
            .toList();
    }

    /**
     * Iscrive l'allievo al corso e gli crea le presenze nelle lezioni non ancora completate,
     * così compare subito nel registro dell'insegnante. Nessun effetto se è già iscritto.
     */
    @Transactional
    public void iscrivi(Long corsoId, Long allievoId) {
        Corso corso = corsoAttivo(corsoId);
        if (iscrizioneRepository.existsById(new IscrizioneId(corsoId, allievoId))) {
            return;
        }
        Allievo allievo = allievoRepository.findById(allievoId)
            .orElseThrow(() -> new IllegalArgumentException("Allievo non trovato: " + allievoId));
        Iscrizione iscrizione = iscrizioneRepository.save(new Iscrizione(corso, allievo));

        lezioneRepository.findByCorsoIdOrderByDataAsc(corsoId).stream()
            .filter(Lezione::isDaFare)
            .filter(lezione -> iscrizione.frequenta(lezione.getData()))
            .forEach(lezione -> presenzaRepository.save(new Presenza(lezione, allievo, Presenza.StatoPresenza.PRESENTE)));
    }

    /** Gruppi del corso, per giorno e orario. */
    public List<GruppoCorso> gruppi(Long corsoId) {
        return gruppoRepository.findByCorsoId(corsoId).stream().sorted(GruppoCorso.PER_GIORNO).toList();
    }

    /**
     * Crea o modifica un gruppo del corso: il giorno deve essere uno di quelli del corso e l'orario è obbligatorio.
     * Se cambia il giorno si aggiornano le presenze dei suoi partecipanti nelle lezioni da fare.
     */
    @Transactional
    public GruppoCorso salvaGruppo(Long corsoId, GruppoCorso gruppo) {
        Corso corso = corsoAttivo(corsoId);
        if (gruppo.getGiorno() == null || !corso.getGiorniSettimana().contains(gruppo.getGiorno())) {
            throw new IllegalArgumentException("Scegli uno dei giorni del corso");
        }
        if (gruppo.getOrario() == null) {
            throw new IllegalArgumentException("Indica l'orario del gruppo");
        }
        if (gruppo.getId() != null && gruppoRepository.findById(gruppo.getId())
                .filter(prima -> prima.getCorso().getId().equals(corsoId)).isEmpty()) {
            throw new IllegalArgumentException("Il gruppo non è di questo corso");
        }
        gruppo.setNome(gruppo.getNome() != null && !gruppo.getNome().isBlank() ? gruppo.getNome().strip() : null);
        gruppo.setCorso(corso);
        GruppoCorso salvato = gruppoRepository.save(gruppo);
        // Il gruppo caricato nelle iscrizioni è quello aggiornato: le presenze seguono il nuovo giorno
        partecipantiDelGruppo(salvato).forEach(this::sincronizzaPresenze);
        return salvato;
    }

    /** Elimina il gruppo: i suoi partecipanti restano senza gruppo (tutti i giorni del corso). */
    @Transactional
    public void eliminaGruppo(Long gruppoId) {
        GruppoCorso gruppo = gruppoRepository.findById(gruppoId)
            .orElseThrow(() -> new IllegalArgumentException("Gruppo non trovato: " + gruppoId));
        verificaAttivo(gruppo.getCorso());
        for (Iscrizione iscrizione : partecipantiDelGruppo(gruppo)) {
            iscrizione.setGruppo(null);
            sincronizzaPresenze(iscrizione);
        }
        gruppoRepository.delete(gruppo);
    }

    /**
     * Assegna il partecipante a un gruppo del corso ({@code null} = nessun gruppo, tutti i giorni) e aggiorna le sue
     * presenze nelle lezioni ancora da fare: tolte nei giorni non più suoi, aggiunte nei nuovi. Le lezioni svolte
     * restano come storico. Se il partecipante non ha ancora un orario prende quello del gruppo.
     */
    @Transactional
    public void assegnaGruppo(Long corsoId, Long allievoId, Long gruppoId) {
        corsoAttivo(corsoId);
        Iscrizione iscrizione = iscrizioneRepository.findById(new IscrizioneId(corsoId, allievoId))
            .orElseThrow(() -> new IllegalArgumentException("L'allievo " + allievoId + " non è iscritto al corso " + corsoId));
        GruppoCorso gruppo = gruppoId == null ? null : gruppoRepository.findById(gruppoId)
            .filter(g -> g.getCorso().getId().equals(corsoId))
            .orElseThrow(() -> new IllegalArgumentException("Il gruppo non è di questo corso"));
        iscrizione.setGruppo(gruppo);
        if (gruppo != null && iscrizione.getOrarioLezione() == null) {
            iscrizione.setOrarioLezione(gruppo.getOrario());
        }
        sincronizzaPresenze(iscrizione);
    }

    private List<Iscrizione> partecipantiDelGruppo(GruppoCorso gruppo) {
        return iscrizioneRepository.findByCorsoIdWithAllievo(gruppo.getCorso().getId()).stream()
            .filter(i -> gruppo.equals(i.getGruppo()))
            .toList();
    }

    /**
     * Presenze del partecipante nelle lezioni ancora da fare secondo i giorni che frequenta: aggiunte dove manca,
     * tolte dove non frequenta più. Le lezioni svolte, non effettuate o completate restano com'erano.
     */
    void sincronizzaPresenze(Iscrizione iscrizione) {
        Long corsoId = iscrizione.getCorso().getId();
        Map<Long, Presenza> presenze = new HashMap<>();
        presenzaRepository.findByCorsoIdAndAllievoId(corsoId, iscrizione.getAllievo().getId())
            .forEach(presenza -> presenze.put(presenza.getLezione().getId(), presenza));
        for (Lezione lezione : lezioneRepository.findByCorsoIdOrderByDataAsc(corsoId)) {
            if (!lezione.isDaFare()) {
                continue;
            }
            Presenza presenza = presenze.get(lezione.getId());
            boolean frequenta = iscrizione.frequenta(lezione.getData());
            if (frequenta && presenza == null) {
                presenzaRepository.save(new Presenza(lezione, iscrizione.getAllievo(), Presenza.StatoPresenza.PRESENTE));
            } else if (!frequenta && presenza != null) {
                presenzaRepository.delete(presenza);
            }
        }
    }

    /**
     * Rimuove l'allievo dal corso eliminando le sue presenze nelle lezioni non completate;
     * quelle delle lezioni già svolte restano come storico.
     */
    @Transactional
    public void rimuoviIscrizione(Long corsoId, Long allievoId) {
        corsoAttivo(corsoId);
        if (pagamentoRepository.existsByIscrizione(corsoId, allievoId)) {
            throw new IllegalStateException("ha pagamenti registrati: eliminali prima di rimuoverlo dal corso");
        }
        presenzaRepository.findByCorsoIdAndAllievoId(corsoId, allievoId).stream()
            .filter(presenza -> !presenza.getLezione().isCompletata())
            .forEach(presenzaRepository::delete);
        iscrizioneRepository.deleteById(new IscrizioneId(corsoId, allievoId));
    }

    /** Giorni frequentati dall'allievo in ognuno dei suoi corsi, per id del corso. */
    public Map<Long, Set<DayOfWeek>> giorniDellAllievo(Long allievoId) {
        Map<Long, Set<DayOfWeek>> giorni = new HashMap<>();
        iscrizioneRepository.findByAllievoIdWithCorso(allievoId)
            .forEach(i -> giorni.put(i.getCorso().getId(), i.giorniEffettivi()));
        return giorni;
    }

    /** Imposta (o con {@code null} toglie) l'orario di inizio lezione di un partecipante. */
    @Transactional
    public void impostaOrario(Long corsoId, Long allievoId, LocalTime orario) {
        corsoAttivo(corsoId);
        Iscrizione iscrizione = iscrizioneRepository.findById(new IscrizioneId(corsoId, allievoId))
            .orElseThrow(() -> new IllegalArgumentException("L'allievo " + allievoId + " non è iscritto al corso " + corsoId));
        iscrizione.setOrarioLezione(orario);
    }

    @Transactional
    public Corso save(Corso corso) {
        BigDecimal preiscrizione = corso.getPrezzoPreiscrizione();
        if (preiscrizione != null && (preiscrizione.signum() < 0
                || corso.getCostoTotale() == null || preiscrizione.compareTo(corso.getCostoTotale()) > 0)) {
            throw new IllegalArgumentException("Il prezzo con pre-iscrizione deve essere tra zero e il costo totale");
        }
        if (corso.getSecondoInsegnante() != null && corso.getSecondoInsegnante().equals(corso.getInsegnante())) {
            throw new IllegalArgumentException("Il secondo insegnante deve essere diverso dal primo");
        }
        if (corso.getId() != null) {
            for (GruppoCorso gruppo : gruppoRepository.findByCorsoId(corso.getId())) {
                if (!corso.getGiorniSettimana().contains(gruppo.getGiorno())) {
                    throw new IllegalArgumentException("Il " + Formati.giorniEstesi(Set.of(gruppo.getGiorno()))
                        + " è il giorno del gruppo \"" + Formati.gruppo(gruppo) + "\": cambia o elimina prima il gruppo");
                }
            }
        }
        boolean insegnantiCambiati = corso.getId() != null && corsoRepository.findById(corso.getId())
            .map(prima -> !Set.copyOf(prima.getInsegnanti()).equals(Set.copyOf(corso.getInsegnanti())))
            .orElse(false);
        Corso salvato = corsoRepository.save(corso);
        if (insegnantiCambiati) {
            // Le lezioni svolte restano a chi c'era: si aggiornano solo quelle ancora da fare
            lezioneRepository.findByCorsoIdOrderByDataAsc(salvato.getId()).stream()
                .filter(Lezione::isDaFare)
                .forEach(lezione -> lezione.setInsegnantiPresenti(new HashSet<>(salvato.getInsegnanti())));
        }
        return salvato;
    }

    @Transactional
    public void delete(Corso corso) {
        if (pagamentoRepository.existsByCorso(corso.getId())) {
            throw new IllegalStateException("Il corso ha pagamenti registrati: eliminali prima di eliminare il corso");
        }
        // Prima si staccano le iscrizioni dai gruppi, poi si eliminano i gruppi e il corso (con le iscrizioni)
        iscrizioneRepository.findByCorsoIdWithAllievo(corso.getId()).forEach(i -> i.setGruppo(null));
        iscrizioneRepository.flush();
        gruppoRepository.deleteAll(gruppoRepository.findByCorsoId(corso.getId()));
        gruppoRepository.flush();
        corsoRepository.delete(corso);
    }

    /**
     * Genera il calendario: "Numero lezioni" è il numero di lezioni di <b>ogni</b> partecipante nei suoi giorni
     * ({@link Iscrizione#giorniEffettivi()}). Si scorrono le date dei giorni del corso, saltando le festività: una
     * data diventa lezione se almeno un partecipante che frequenta quel giorno non ha ancora le sue lezioni, e ha
     * le presenze solo di quei partecipanti. Senza iscritti si generano {@code numeroLezioni} date. Se tutti
     * frequentano tutti i giorni il risultato è quello di sempre: {@code numeroLezioni} date per tutti.
     * <p>
     * Ricrea solo le lezioni programmate ancora da fare: restano completate, non effettuate e aggiunte a mano; le
     * programmate conservate contano per chi frequenta quel giorno, e la loro data e il loro numero non si ripetono.
     */
    @Transactional
    public void generaLezioni(Corso corsoDaUi) {
        Corso corso = corsoAttivo(corsoDaUi.getId());

        List<Lezione> lezioniEsistenti = lezioneRepository.findByCorsoIdOrderByDataAsc(corso.getId());
        List<Lezione> conservate = new ArrayList<>();
        for (Lezione esistente : lezioniEsistenti) {
            if (esistente.isDaFare() && !esistente.isAggiunta()) {
                lezioneRepository.delete(esistente);
            } else if (!esistente.isAggiunta()) {
                conservate.add(esistente);
            }
        }
        Set<Integer> numeriConservati = new HashSet<>();
        Set<LocalDate> dateConservate = new HashSet<>();
        conservate.forEach(l -> {
            numeriConservati.add(l.getNumeroLezione());
            dateConservate.add(l.getData());
        });

        List<Iscrizione> iscrizioni = iscrizioneRepository.findByCorsoIdWithAllievo(corso.getId());
        int lezioniPerAllievo = corso.getNumeroLezioni() != null ? corso.getNumeroLezioni() : 0;
        Map<Iscrizione, Integer> contate = new HashMap<>();
        iscrizioni.forEach(i -> contate.put(i, (int) conservate.stream().filter(l -> i.frequenta(l.getData())).count()));

        // I giorni festivi si saltano senza consumare un numero: la fine del corso slitta
        CalendarioFestivita festivita = festivitaService.calendario();
        LocalDate dataCorrente = corso.getDataInizio();
        LocalDate limite = corso.getDataInizio().plusYears(10);
        int numeroLezione = 1;
        int generateSenzaIscritti = conservate.size();

        while (!dataCorrente.isAfter(limite) && !corso.getGiorniSettimana().isEmpty()) {
            if (iscrizioni.isEmpty() ? generateSenzaIscritti >= lezioniPerAllievo
                : contate.values().stream().allMatch(n -> n >= lezioniPerAllievo)) {
                break;
            }
            LocalDate data = dataCorrente;
            dataCorrente = dataCorrente.plusDays(1);
            if (!corso.getGiorniSettimana().contains(data.getDayOfWeek()) || festivita.festivo(data)
                    || dateConservate.contains(data)) {
                continue;
            }
            List<Iscrizione> partecipanti = iscrizioni.stream()
                .filter(i -> i.frequenta(data) && contate.get(i) < lezioniPerAllievo)
                .toList();
            if (!iscrizioni.isEmpty() && partecipanti.isEmpty()) {
                continue;
            }
            while (numeriConservati.contains(numeroLezione)) {
                numeroLezione++;
            }
            Lezione lezione = new Lezione(corso, data, numeroLezione++);
            lezione.setInsegnantiPresenti(new HashSet<>(corso.getInsegnanti()));
            lezione.setOrarioInizio(corso.getOrarioInizio());
            if (corso.getOrarioInizio() != null && corso.getDurataLezioneMinuti() != null) {
                lezione.setOrarioFine(corso.getOrarioInizio().plusMinutes(corso.getDurataLezioneMinuti()));
            }
            lezioneRepository.save(lezione);
            generateSenzaIscritti++;
            for (Iscrizione iscrizione : partecipanti) {
                presenzaRepository.save(new Presenza(lezione, iscrizione.getAllievo(), Presenza.StatoPresenza.PRESENTE));
                contate.merge(iscrizione, 1, Integer::sum);
            }
        }
    }
}
