package it.attocchi.service;

import it.attocchi.entity.Corso;
import it.attocchi.entity.Iscrizione;
import it.attocchi.entity.IscrizioneId;
import it.attocchi.entity.Lezione;
import it.attocchi.entity.Presenza;
import it.attocchi.entity.Studente;
import it.attocchi.repository.CorsoRepository;
import it.attocchi.repository.IscrizioneRepository;
import it.attocchi.repository.LezioneRepository;
import it.attocchi.repository.PresenzaRepository;
import it.attocchi.repository.StudenteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final StudenteRepository studenteRepository;

    public CorsoService(CorsoRepository corsoRepository, LezioneRepository lezioneRepository,
                       PresenzaRepository presenzaRepository, IscrizioneRepository iscrizioneRepository,
                       StudenteRepository studenteRepository) {
        this.corsoRepository = corsoRepository;
        this.lezioneRepository = lezioneRepository;
        this.presenzaRepository = presenzaRepository;
        this.iscrizioneRepository = iscrizioneRepository;
        this.studenteRepository = studenteRepository;
    }

    public List<Corso> findAll() {
        return corsoRepository.findAllWithStudenti();
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

    /** Tutti i corsi dello studente, anche non attivi (consultazione del genitore). */
    public List<Corso> findByStudente(Long studenteId) {
        return corsoRepository.findByStudenteId(studenteId);
    }

    /** Solo i corsi attivi dello studente: l'allievo non vede i corsi non attivi. */
    public List<Corso> findAttiviByStudente(Long studenteId) {
        return corsoRepository.findByStudenteId(studenteId).stream().filter(Corso::isAttivo).toList();
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
        return iscrizioneRepository.findByCorsoIdWithStudente(corsoId).stream()
            .sorted(Iscrizione.PER_ORARIO)
            .toList();
    }

    /**
     * Iscrive lo studente al corso e gli crea le presenze nelle lezioni non ancora completate,
     * così compare subito nel registro dell'insegnante. Nessun effetto se è già iscritto.
     */
    @Transactional
    public void iscrivi(Long corsoId, Long studenteId) {
        Corso corso = corsoAttivo(corsoId);
        if (iscrizioneRepository.existsById(new IscrizioneId(corsoId, studenteId))) {
            return;
        }
        Studente studente = studenteRepository.findById(studenteId)
            .orElseThrow(() -> new IllegalArgumentException("Studente non trovato: " + studenteId));
        iscrizioneRepository.save(new Iscrizione(corso, studente));

        lezioneRepository.findByCorsoIdOrderByDataAsc(corsoId).stream()
            .filter(lezione -> !lezione.isCompletata())
            .forEach(lezione -> presenzaRepository.save(new Presenza(lezione, studente, Presenza.StatoPresenza.PRESENTE)));
    }

    /**
     * Rimuove lo studente dal corso eliminando le sue presenze nelle lezioni non completate;
     * quelle delle lezioni già svolte restano come storico.
     */
    @Transactional
    public void rimuoviIscrizione(Long corsoId, Long studenteId) {
        corsoAttivo(corsoId);
        presenzaRepository.findByCorsoIdAndStudenteId(corsoId, studenteId).stream()
            .filter(presenza -> !presenza.getLezione().isCompletata())
            .forEach(presenzaRepository::delete);
        iscrizioneRepository.deleteById(new IscrizioneId(corsoId, studenteId));
    }

    /** Imposta (o con {@code null} toglie) l'orario di inizio lezione di un partecipante. */
    @Transactional
    public void impostaOrario(Long corsoId, Long studenteId, LocalTime orario) {
        corsoAttivo(corsoId);
        Iscrizione iscrizione = iscrizioneRepository.findById(new IscrizioneId(corsoId, studenteId))
            .orElseThrow(() -> new IllegalArgumentException("Lo studente " + studenteId + " non è iscritto al corso " + corsoId));
        iscrizione.setOrarioLezione(orario);
    }

    @Transactional
    public Corso save(Corso corso) {
        return corsoRepository.save(corso);
    }

    @Transactional
    public void delete(Corso corso) {
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

                // Crea presenze per tutti gli studenti iscritti
                for (Studente studente : corso.getStudenti()) {
                    Presenza presenza = new Presenza(lezione, studente, Presenza.StatoPresenza.PRESENTE);
                    presenzaRepository.save(presenza);
                }

                numeroLezione++;
            }
            dataCorrente = dataCorrente.plusDays(1);
        }
    }
}
