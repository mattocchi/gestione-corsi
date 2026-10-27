package it.attocchi.service;

import it.attocchi.entity.Insegnante;
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
import it.attocchi.repository.LetteraIncaricoRepository;
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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
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
    private final NumerazioneLezioni numerazioneLezioni;
    private final LetteraIncaricoRepository letteraIncaricoRepository;

    public CorsoService(CorsoRepository corsoRepository, LezioneRepository lezioneRepository,
                       PresenzaRepository presenzaRepository, IscrizioneRepository iscrizioneRepository,
                       AllievoRepository allievoRepository, PagamentoRepository pagamentoRepository,
                       FestivitaService festivitaService, GruppoCorsoRepository gruppoRepository,
                       NumerazioneLezioni numerazioneLezioni, LetteraIncaricoRepository letteraIncaricoRepository) {
        this.letteraIncaricoRepository = letteraIncaricoRepository;
        this.gruppoRepository = gruppoRepository;
        this.numerazioneLezioni = numerazioneLezioni;
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

    /** Numero di gruppi per id del corso; i corsi senza gruppi non ci sono. */
    public Map<Long, Long> numeroGruppi() {
        Map<Long, Long> numero = new HashMap<>();
        gruppoRepository.contaPerCorso().forEach(riga -> numero.put((Long) riga[0], (Long) riga[1]));
        return numero;
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

    /** Partecipanti del corso per gruppo, orario e ordine di inserimento, vedi {@link Iscrizione#ORDINE}. */
    public List<Iscrizione> findPartecipanti(Long corsoId) {
        return iscrizioneRepository.findByCorsoIdWithAllievo(corsoId).stream()
            .sorted(Iscrizione.ORDINE)
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
            .filter(lezione -> iscrizione.frequenta(lezione.getDataDelCalendario()))
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
        gruppoRepository.flush();
        numerazioneLezioni.rinumera(corsoId);
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
        gruppoRepository.flush();
        // Con altri gruppi resta la numerazione per giorno; senza gruppi si torna al contatore unico
        Long corsoId = gruppo.getCorso().getId();
        numerazioneLezioni.rinumera(corsoId);
        numerazioneLezioni.rinumeraSenzaGruppi(corsoId);
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

    /**
     * Converte i giorni per allievo della versione precedente (id dell'iscrizione → giorni scelti): chi ne aveva
     * uno solo tra quelli del corso passa al gruppo di quel giorno (creato se manca, senza nome, con l'orario del
     * corso); gli altri restano senza gruppo e frequentano tutti i giorni. Le presenze delle lezioni da fare si
     * allineano. Vale anche per i corsi non attivi; le iscrizioni che hanno già un gruppo non si toccano.
     *
     * @return numero di partecipanti assegnati a un gruppo
     */
    @Transactional
    public int convertiGiorniInGruppi(Map<IscrizioneId, Set<DayOfWeek>> giorniPerIscrizione) {
        int assegnati = 0;
        for (Map.Entry<IscrizioneId, Set<DayOfWeek>> voce : giorniPerIscrizione.entrySet()) {
            Iscrizione iscrizione = iscrizioneRepository.findById(voce.getKey()).orElse(null);
            if (iscrizione == null || iscrizione.getGruppo() != null) {
                continue;
            }
            Corso corso = iscrizione.getCorso();
            Set<DayOfWeek> giorni = new HashSet<>(voce.getValue());
            giorni.retainAll(corso.getGiorniSettimana());
            if (giorni.size() == 1 && corso.getGiorniSettimana().size() > 1) {
                DayOfWeek giorno = giorni.iterator().next();
                GruppoCorso gruppo = gruppoRepository.findByCorsoId(corso.getId()).stream()
                    .filter(g -> g.getGiorno() == giorno && g.getNome() == null)
                    .findFirst()
                    .orElseGet(() -> gruppoRepository.save(new GruppoCorso(corso, null, giorno, corso.getOrarioInizio())));
                iscrizione.setGruppo(gruppo);
                assegnati++;
            }
            sincronizzaPresenze(iscrizione);
        }
        return assegnati;
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
            boolean frequenta = iscrizione.frequenta(lezione.getDataDelCalendario());
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

    /** Gruppo dell'allievo nei suoi corsi, per id del corso (solo i corsi in cui è in un gruppo). */
    public Map<Long, GruppoCorso> gruppiDellAllievo(Long allievoId) {
        Map<Long, GruppoCorso> gruppi = new HashMap<>();
        iscrizioneRepository.findByAllievoIdWithCorso(allievoId).stream()
            .filter(i -> i.getGruppo() != null)
            .forEach(i -> gruppi.put(i.getCorso().getId(), i.getGruppo()));
        return gruppi;
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
        if (corso.getNumeroLezioni() == null && corso.getDataFine() == null) {
            throw new IllegalArgumentException("Indica il numero di lezioni o la data di fine");
        }
        if (corso.getNumeroLezioni() != null && corso.getNumeroLezioni() < 1) {
            throw new IllegalArgumentException("Il numero di lezioni deve essere almeno 1");
        }
        if (corso.getDataFine() != null && corso.getDataInizio() != null && corso.getDataFine().isBefore(corso.getDataInizio())) {
            throw new IllegalArgumentException("La data di fine non può essere prima della data di inizio");
        }
        if (corso.getCadenzaSettimane() != 1 && corso.getCadenzaSettimane() != 2) {
            throw new IllegalArgumentException("La cadenza è ogni settimana o ogni 2 settimane");
        }
        List<Insegnante> facoltativi = corso.getInsegnantiFacoltativi();
        if (facoltativi.size() > Corso.MAX_FACOLTATIVI) {
            throw new IllegalArgumentException("Al massimo " + Corso.MAX_FACOLTATIVI + " insegnanti oltre al titolare");
        }
        if (facoltativi.contains(corso.getInsegnante()) || new HashSet<>(facoltativi).size() != facoltativi.size()) {
            throw new IllegalArgumentException("Gli altri insegnanti devono essere diversi tra loro e dal titolare");
        }
        if (corso.getId() != null) {
            for (GruppoCorso gruppo : gruppoRepository.findByCorsoId(corso.getId())) {
                if (!corso.getGiorniSettimana().contains(gruppo.getGiorno())) {
                    throw new IllegalArgumentException("Il " + Formati.giorniEstesi(Set.of(gruppo.getGiorno()))
                        + " è il giorno del gruppo \"" + Formati.gruppo(gruppo) + "\": cambia o elimina prima il gruppo");
                }
            }
        }
        // Il luogo predefinito è sempre tra quelli abilitati
        if (corso.getLuogo() != null) {
            corso.getLuoghiAbilitati().add(corso.getLuogo());
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
        if (letteraIncaricoRepository.existsByCorsoId(corso.getId())) {
            throw new IllegalStateException("Il corso ha lettere di incarico: eliminale prima di eliminare il corso");
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
     * Con la cadenza ogni 2 settimane ({@link Corso#getCadenzaSettimane()}) ogni giorno del corso ha lezione una
     * settimana sì e una no, a partire dalla data di inizio; una lezione che cade in una festività si sposta alla
     * settimana dopo e il ritmo riparte da lì.
     * <p>
     * Aggiorna solo le lezioni programmate ancora da fare: restano completate, non effettuate, aggiunte a mano e
     * spostate; le programmate conservate contano per chi frequenta quel giorno, e la loro data e il loro numero non si
     * ripetono. Le programmate ancora valide restano com'erano (note, luogo, presenze già segnate), quelle che cadono
     * in una festività o oltre la fine si tolgono, le mancanti si aggiungono ({@link #aggiornaLezioni}).
     */
    @Transactional
    public void generaLezioni(Corso corsoDaUi) {
        aggiornaLezioni(corsoDaUi.getId());
    }

    /** Effetto di un aggiornamento delle lezioni: date aggiunte e tolte, quante lezioni da fare restano com'erano. */
    public record AggiornamentoLezioni(List<LocalDate> aggiunte, List<LocalDate> tolte, int invariate) {

        public boolean cambia() {
            return !aggiunte.isEmpty() || !tolte.isEmpty();
        }
    }

    /** Una data del calendario del corso con i partecipanti che la frequentano. */
    private record DataPianificata(LocalDate data, List<Iscrizione> partecipanti) {
    }

    /** Lezioni conservate, programmate da fare (da confrontare) e date del nuovo calendario. */
    private record Piano(Corso corso, List<Lezione> conservate, List<Lezione> programmate, List<DataPianificata> date) {
    }

    /** Cosa cambierebbe {@link #aggiornaLezioni} sul corso, senza modificare nulla. */
    public AggiornamentoLezioni anteprimaAggiornamento(Long corsoId) {
        Piano piano = piano(corsoAttivo(corsoId));
        Set<LocalDate> pianificate = piano.date().stream().map(DataPianificata::data).collect(Collectors.toSet());
        Set<LocalDate> esistenti = piano.programmate().stream().map(Lezione::getData).collect(Collectors.toSet());
        return new AggiornamentoLezioni(
            piano.date().stream().map(DataPianificata::data).filter(d -> !esistenti.contains(d)).toList(),
            piano.programmate().stream().map(Lezione::getData).filter(d -> !pianificate.contains(d)).sorted().toList(),
            (int) piano.programmate().stream().filter(l -> pianificate.contains(l.getData())).count());
    }

    /**
     * Allinea le lezioni da fare al calendario del corso (numero di lezioni, data di fine, giorni, cadenza, festività,
     * partecipanti): conserva quelle ancora valide (con l'orario del corso e i partecipanti del calendario, le presenze
     * già segnate restano), toglie le altre e aggiunge le mancanti; poi rinumera.
     */
    @Transactional
    public AggiornamentoLezioni aggiornaLezioni(Long corsoId) {
        Piano piano = piano(corsoAttivo(corsoId));
        Corso corso = piano.corso();
        Map<LocalDate, Lezione> programmate = new HashMap<>();
        piano.programmate().forEach(l -> programmate.putIfAbsent(l.getData(), l));
        List<LocalDate> aggiunte = new ArrayList<>();
        List<Lezione> calendario = new ArrayList<>();
        for (DataPianificata pianificata : piano.date()) {
            Lezione lezione = programmate.remove(pianificata.data());
            if (lezione == null) {
                lezione = new Lezione(corso, pianificata.data(), 0);
                lezione.setInsegnantiPresenti(new HashSet<>(corso.getInsegnanti()));
                aggiunte.add(pianificata.data());
            }
            lezione.setOrarioInizio(corso.getOrarioInizio());
            lezione.setOrarioFine(corso.getOrarioInizio() != null && corso.getDurataLezioneMinuti() != null
                ? corso.getOrarioInizio().plusMinutes(corso.getDurataLezioneMinuti()) : null);
            lezione = lezioneRepository.save(lezione);
            sincronizzaPartecipanti(lezione, pianificata.partecipanti());
            calendario.add(lezione);
        }
        // Le programmate rimaste fuori dal calendario (festività, oltre la fine) si tolgono
        List<LocalDate> tolte = programmate.values().stream().map(Lezione::getData).sorted().toList();
        lezioneRepository.deleteAll(programmate.values());
        lezioneRepository.flush();

        // Numeri in ordine di data, senza ripetere quelli delle lezioni conservate
        Set<Integer> numeriConservati = piano.conservate().stream().map(Lezione::getNumeroLezione).collect(Collectors.toSet());
        int numero = 1;
        for (Lezione lezione : calendario) {
            while (numeriConservati.contains(numero)) {
                numero++;
            }
            lezione.setNumeroLezione(numero++);
        }
        lezioneRepository.flush();
        numerazioneLezioni.rinumera(corso.getId());
        return new AggiornamentoLezioni(aggiunte, tolte, calendario.size() - aggiunte.size());
    }

    /** Presenze della lezione = partecipanti del calendario: quelle già segnate restano, le mancanti nascono PRESENTE. */
    private void sincronizzaPartecipanti(Lezione lezione, List<Iscrizione> partecipanti) {
        Set<Long> attesi = partecipanti.stream().map(i -> i.getAllievo().getId()).collect(Collectors.toSet());
        List<Presenza> esistenti = presenzaRepository.findByLezioneId(lezione.getId());
        Set<Long> presenti = new HashSet<>();
        for (Presenza presenza : esistenti) {
            if (attesi.contains(presenza.getAllievo().getId())) {
                presenti.add(presenza.getAllievo().getId());
            } else {
                lezione.getPresenze().remove(presenza);
                presenzaRepository.delete(presenza);
            }
        }
        for (Iscrizione iscrizione : partecipanti) {
            if (!presenti.contains(iscrizione.getAllievo().getId())) {
                presenzaRepository.save(new Presenza(lezione, iscrizione.getAllievo(), Presenza.StatoPresenza.PRESENTE));
            }
        }
    }

    /**
     * Calendario del corso: scorre le date dei giorni del corso dalla data di inizio, saltando le festività, fino al
     * numero di lezioni di ogni partecipante o alla data di fine (il primo limite raggiunto).
     */
    private Piano piano(Corso corso) {
        List<Lezione> conservate = new ArrayList<>();
        List<Lezione> programmate = new ArrayList<>();
        for (Lezione esistente : lezioneRepository.findByCorsoIdOrderByDataAsc(corso.getId())) {
            // Le lezioni spostate a mano restano dove sono state messe; le aggiunte non contano
            if (esistente.isDaFare() && !esistente.isAggiunta() && !esistente.isSpostata()) {
                programmate.add(esistente);
            } else if (!esistente.isAggiunta()) {
                conservate.add(esistente);
            }
        }
        Set<LocalDate> dateConservate = new HashSet<>();
        conservate.forEach(l -> {
            dateConservate.add(l.getData());
            dateConservate.add(l.getDataDelCalendario());
        });

        List<Iscrizione> iscrizioni = iscrizioneRepository.findByCorsoIdWithAllievo(corso.getId());
        // Senza numero di lezioni nessun limite per allievo: ci si ferma alla data di fine
        int lezioniPerAllievo = corso.getNumeroLezioni() != null ? corso.getNumeroLezioni() : Integer.MAX_VALUE;
        Map<Iscrizione, Integer> contate = new HashMap<>();
        iscrizioni.forEach(i -> contate.put(i, (int) conservate.stream().filter(l -> i.frequenta(l.getDataDelCalendario())).count()));

        // I giorni festivi si saltano senza consumare un numero: la fine del corso slitta
        CalendarioFestivita festivita = festivitaService.calendario();
        LocalDate dataCorrente = corso.getDataInizio();
        LocalDate limite = corso.getDataFine() != null ? corso.getDataFine() : corso.getDataInizio().plusYears(10);
        int senzaIscritti = conservate.size();
        // Cadenza: per ogni giorno del corso la prima data utile da cui cercare la prossima lezione. Dopo una
        // lezione si salta di "cadenza" settimane; una festività sposta la lezione alla settimana dopo
        int giorniTraLezioni = 7 * corso.getCadenzaSettimane();
        Map<DayOfWeek, LocalDate> prossima = new EnumMap<>(DayOfWeek.class);
        List<DataPianificata> date = new ArrayList<>();

        while (!dataCorrente.isAfter(limite) && !corso.getGiorniSettimana().isEmpty()) {
            if (iscrizioni.isEmpty() ? senzaIscritti >= lezioniPerAllievo
                : contate.values().stream().allMatch(n -> n >= lezioniPerAllievo)) {
                break;
            }
            LocalDate data = dataCorrente;
            dataCorrente = dataCorrente.plusDays(1);
            DayOfWeek giorno = data.getDayOfWeek();
            if (!corso.getGiorniSettimana().contains(giorno) || data.isBefore(prossima.getOrDefault(giorno, data))) {
                continue;
            }
            if (festivita.festivo(data)) {
                prossima.put(giorno, data.plusWeeks(1));
                continue;
            }
            prossima.put(giorno, data.plusDays(giorniTraLezioni));
            if (dateConservate.contains(data)) {
                continue;
            }
            List<Iscrizione> partecipanti = iscrizioni.stream()
                .filter(i -> i.frequenta(data) && contate.get(i) < lezioniPerAllievo)
                .toList();
            if (!iscrizioni.isEmpty() && partecipanti.isEmpty()) {
                continue;
            }
            date.add(new DataPianificata(data, partecipanti));
            senzaIscritti++;
            partecipanti.forEach(i -> contate.merge(i, 1, Integer::sum));
        }
        return new Piano(corso, conservate, programmate, date);
    }
}
