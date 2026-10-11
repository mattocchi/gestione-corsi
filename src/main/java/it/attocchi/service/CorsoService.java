package it.attocchi.service;

import it.attocchi.entity.Corso;
import it.attocchi.entity.Iscrizione;
import it.attocchi.entity.IscrizioneId;
import it.attocchi.entity.Lezione;
import it.attocchi.entity.Presenza;
import it.attocchi.entity.Allievo;
import it.attocchi.repository.CorsoRepository;
import it.attocchi.repository.IscrizioneRepository;
import it.attocchi.repository.LezioneRepository;
import it.attocchi.repository.PagamentoRepository;
import it.attocchi.repository.PresenzaRepository;
import it.attocchi.repository.AllievoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CorsoService {

    private final CorsoRepository corsoRepository;
    private final LezioneRepository lezioneRepository;
    private final PresenzaRepository presenzaRepository;
    private final IscrizioneRepository iscrizioneRepository;
    private final AllievoRepository allievoRepository;
    private final PagamentoRepository pagamentoRepository;

    public CorsoService(CorsoRepository corsoRepository, LezioneRepository lezioneRepository,
                       PresenzaRepository presenzaRepository, IscrizioneRepository iscrizioneRepository,
                       AllievoRepository allievoRepository, PagamentoRepository pagamentoRepository) {
        this.pagamentoRepository = pagamentoRepository;
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
        return corsoRepository.findByInsegnanteId(insegnanteId);
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
        iscrizioneRepository.save(new Iscrizione(corso, allievo));

        lezioneRepository.findByCorsoIdOrderByDataAsc(corsoId).stream()
            .filter(lezione -> !lezione.isCompletata())
            .forEach(lezione -> presenzaRepository.save(new Presenza(lezione, allievo, Presenza.StatoPresenza.PRESENTE)));
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
        return corsoRepository.save(corso);
    }

    @Transactional
    public void delete(Corso corso) {
        if (pagamentoRepository.existsByCorso(corso.getId())) {
            throw new IllegalStateException("Il corso ha pagamenti registrati: eliminali prima di eliminare il corso");
        }
        corsoRepository.delete(corso);
    }

    @Transactional
    public void generaLezioni(Corso corsoDaUi) {
        Corso corso = corsoAttivo(corsoDaUi.getId());

        // Elimina lezioni esistenti non completate
        List<Lezione> lezioniEsistenti = lezioneRepository.findByCorsoIdOrderByDataAsc(corso.getId());
        lezioniEsistenti.stream()
            .filter(l -> !l.isCompletata())
            .forEach(lezioneRepository::delete);

        LocalDate dataCorrente = corso.getDataInizio();
        int numeroLezione = 1;

        while (numeroLezione <= corso.getNumeroLezioni()) {
            DayOfWeek giornoSettimana = dataCorrente.getDayOfWeek();

            if (corso.getGiorniSettimana().contains(giornoSettimana)) {
                Lezione lezione = new Lezione(corso, dataCorrente, numeroLezione);
                lezione.setOrarioInizio(corso.getOrarioInizio());
                if (corso.getOrarioInizio() != null && corso.getDurataLezioneMinuti() != null) {
                    lezione.setOrarioFine(corso.getOrarioInizio().plusMinutes(corso.getDurataLezioneMinuti()));
                }
                lezioneRepository.save(lezione);

                // Crea presenze per tutti gli allievi iscritti
                for (Allievo allievo : corso.getAllievi()) {
                    Presenza presenza = new Presenza(lezione, allievo, Presenza.StatoPresenza.PRESENTE);
                    presenzaRepository.save(presenza);
                }

                numeroLezione++;
            }
            dataCorrente = dataCorrente.plusDays(1);
        }
    }
}
