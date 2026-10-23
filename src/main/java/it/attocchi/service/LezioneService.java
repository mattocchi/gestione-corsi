package it.attocchi.service;

import it.attocchi.entity.Corso;
import it.attocchi.entity.GruppoCorso;
import it.attocchi.entity.Iscrizione;
import it.attocchi.entity.Insegnante;
import it.attocchi.entity.Lezione;
import it.attocchi.entity.Presenza;
import it.attocchi.entity.User;
import it.attocchi.repository.CorsoRepository;
import it.attocchi.repository.IscrizioneRepository;
import it.attocchi.repository.LezioneRepository;
import it.attocchi.repository.PresenzaRepository;
import it.attocchi.repository.UserRepository;
import it.attocchi.views.Formati;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class LezioneService {

    private final LezioneRepository lezioneRepository;
    private final CorsoRepository corsoRepository;
    private final IscrizioneRepository iscrizioneRepository;
    private final PresenzaRepository presenzaRepository;
    private final UserRepository userRepository;
    private final NumerazioneLezioni numerazioneLezioni;

    public LezioneService(LezioneRepository lezioneRepository, CorsoRepository corsoRepository,
                          IscrizioneRepository iscrizioneRepository, PresenzaRepository presenzaRepository,
                          UserRepository userRepository, NumerazioneLezioni numerazioneLezioni) {
        this.lezioneRepository = lezioneRepository;
        this.corsoRepository = corsoRepository;
        this.iscrizioneRepository = iscrizioneRepository;
        this.presenzaRepository = presenzaRepository;
        this.userRepository = userRepository;
        this.numerazioneLezioni = numerazioneLezioni;
    }

    public List<Lezione> findAll() {
        return lezioneRepository.findAll();
    }

    /**
     * Registra gli insegnanti del corso sulle lezioni che non ne hanno (dati creati prima dei due insegnanti per
     * corso). Idempotente: chiamato a ogni avvio da {@code InsegnantiPresentiIniziali}. Restituisce le lezioni aggiornate.
     */
    @Transactional
    public int completaInsegnantiPresenti() {
        List<Lezione> senza = lezioneRepository.findSenzaInsegnantiPresenti();
        senza.forEach(lezione -> lezione.setInsegnantiPresenti(new HashSet<>(lezione.getCorso().getInsegnanti())));
        return senza.size();
    }

    public Lezione findById(Long id) {
        return lezioneRepository.findById(id).orElse(null);
    }

    public List<Lezione> findByCorso(Long corsoId) {
        return lezioneRepository.findByCorsoIdOrderByDataAsc(corsoId);
    }

    /** Lezioni dei corsi attivi dell'insegnante (titolare o secondo): i corsi non attivi non sono visibili all'insegnante. */
    public List<Lezione> findByInsegnante(Long insegnanteId) {
        return lezioneRepository.findDeiCorsiAttiviDellInsegnante(insegnanteId);
    }

    /** Lezioni di tutti i corsi attivi (vista Lezioni di admin e segreteria). */
    public List<Lezione> findDeiCorsiAttivi() {
        return lezioneRepository.findByCorsoAttivoTrueOrderByDataAsc();
    }

    public List<Lezione> findByCorsoAndPeriodo(Long corsoId, LocalDate dataInizio, LocalDate dataFine) {
        return lezioneRepository.findByCorsoIdAndDataBetweenOrderByDataAsc(corsoId, dataInizio, dataFine);
    }

    public List<Lezione> findByPeriodo(LocalDate dataInizio, LocalDate dataFine) {
        return lezioneRepository.findByDataBetweenOrderByDataAsc(dataInizio, dataFine);
    }

    /** Le lezioni di un corso non attivo non si possono modificare né completare. */
    @Transactional
    public Lezione save(Lezione lezione) {
        if (lezione.getId() != null) {
            lezioneRepository.findById(lezione.getId()).ifPresent(l -> CorsoService.verificaAttivo(l.getCorso()));
        }
        return lezioneRepository.save(lezione);
    }

    @Transactional
    public void delete(Lezione lezione) {
        lezioneRepository.delete(lezione);
    }

    /**
     * Insegnanti presenti alla lezione (almeno uno, tra quelli del corso): un insegnante del corso, la segreteria o
     * l'admin, anche dopo il completamento; non per le lezioni non effettuate né per i corsi non attivi.
     */
    @Transactional
    public void impostaInsegnantiPresenti(Long lezioneId, Set<Long> insegnantiIds, String username) {
        Lezione lezione = gestibile(lezioneId, username, false);
        if (lezione.isNonEffettuata()) {
            throw new IllegalStateException("La lezione non è stata effettuata");
        }
        List<Insegnante> delCorso = lezione.getCorso().getInsegnanti();
        Set<Insegnante> presenti = new HashSet<>();
        for (Insegnante insegnante : delCorso) {
            if (insegnantiIds.contains(insegnante.getId())) {
                presenti.add(insegnante);
            }
        }
        if (presenti.isEmpty() || presenti.size() != insegnantiIds.size()) {
            throw new IllegalArgumentException("Indica almeno uno degli insegnanti del corso");
        }
        lezione.setInsegnantiPresenti(presenti);
    }

    /**
     * Note della lezione per lo staff e per allievi e genitori (testo ripulito, vuoto = nessuna nota), in qualsiasi
     * stato della lezione: un insegnante del corso, la segreteria o l'admin, sui corsi attivi.
     */
    @Transactional
    public void impostaNote(Long lezioneId, String noteStaff, String notePubbliche, String username) {
        Lezione lezione = gestibile(lezioneId, username, false);
        if (lunga(noteStaff) || lunga(notePubbliche)) {
            throw new IllegalArgumentException("Le note non possono superare " + Lezione.LUNGHEZZA_NOTE + " caratteri");
        }
        lezione.setNote(testo(noteStaff));
        lezione.setNotePubbliche(testo(notePubbliche));
    }

    private static boolean lunga(String valore) {
        return valore != null && valore.strip().length() > Lezione.LUNGHEZZA_NOTE;
    }

    private static String testo(String valore) {
        return valore != null && !valore.isBlank() ? valore.strip() : null;
    }

    /** Segna svolta una lezione da fare. */
    @Transactional
    public void completa(Long lezioneId, String username) {
        Lezione lezione = gestibile(lezioneId, username, false);
        if (lezione.isNonEffettuata()) {
            throw new IllegalStateException("La lezione è segnata come non effettuata: ripristinala prima di completarla");
        }
        lezione.setCompletata(true);
    }

    /** Segna come non effettuata una lezione da fare; il motivo è facoltativo. Le sue presenze non contano più. */
    @Transactional
    public void segnaNonEffettuata(Long lezioneId, String motivo, String username) {
        Lezione lezione = gestibile(lezioneId, username, false);
        if (lezione.isCompletata()) {
            throw new IllegalStateException("La lezione è già completata");
        }
        lezione.setNonEffettuata(true);
        lezione.setMotivoNonEffettuata(motivo != null && !motivo.isBlank() ? motivo.strip() : null);
    }

    /** Riporta da fare una lezione non effettuata, creando le presenze dei partecipanti iscritti nel frattempo. */
    @Transactional
    public void ripristina(Long lezioneId, String username) {
        Lezione lezione = gestibile(lezioneId, username, false);
        if (!lezione.isNonEffettuata()) {
            return;
        }
        if (lezioneRepository.existsByRecuperoDiId(lezioneId)) {
            throw new IllegalStateException("La lezione ha già un recupero: elimina prima il recupero");
        }
        lezione.setNonEffettuata(false);
        lezione.setMotivoNonEffettuata(null);
        Set<Long> conPresenza = presenzaRepository.findByLezioneId(lezioneId).stream()
            .map(p -> p.getAllievo().getId()).collect(Collectors.toSet());
        for (Iscrizione iscrizione : iscrizioneRepository.findByCorsoIdWithAllievo(lezione.getCorso().getId())) {
            if (!conPresenza.contains(iscrizione.getAllievo().getId()) && iscrizione.frequenta(lezione.getDataDelCalendario())) {
                presenzaRepository.save(new Presenza(lezione, iscrizione.getAllievo(), Presenza.StatoPresenza.PRESENTE));
            }
        }
    }

    /**
     * Aggiunge una lezione al corso con le presenze (Presente) dei partecipanti che frequentano quel giorno (tutti se
     * non è un giorno del corso). Con {@code recuperoDiId} è il recupero di quella lezione non effettuata, ne prende
     * il numero e le presenze vanno a chi aveva la lezione saltata; altrimenti il numero è il successivo all'ultimo.
     * Senza orario usa quello del corso.
     */
    @Transactional
    public Lezione aggiungi(Long corsoId, LocalDate data, LocalTime orario, Long recuperoDiId, String username) {
        if (data == null) {
            throw new IllegalArgumentException("Inserisci la data della lezione");
        }
        Corso corso = corsoRepository.findById(corsoId)
            .orElseThrow(() -> new IllegalArgumentException("Corso non trovato: " + corsoId));
        verificaPermesso(corso, username, false);
        Lezione lezione = new Lezione();
        lezione.setCorso(corso);
        lezione.setData(data);
        lezione.setAggiunta(true);
        lezione.setInsegnantiPresenti(new HashSet<>(corso.getInsegnanti()));
        Set<Long> diChiRecupera = null;
        if (recuperoDiId != null) {
            Lezione saltata = lezioneRepository.findById(recuperoDiId)
                .orElseThrow(() -> new IllegalArgumentException("Lezione non trovata: " + recuperoDiId));
            if (!saltata.isNonEffettuata() || !saltata.getCorso().getId().equals(corsoId)) {
                throw new IllegalArgumentException("Si recupera solo una lezione non effettuata dello stesso corso");
            }
            if (lezioneRepository.existsByRecuperoDiId(recuperoDiId)) {
                throw new IllegalStateException("La lezione ha già un recupero");
            }
            lezione.setRecuperoDi(saltata);
            lezione.setNumeroLezione(saltata.getNumeroLezione());
            diChiRecupera = presenzaRepository.findByLezioneId(recuperoDiId).stream()
                .map(p -> p.getAllievo().getId()).collect(Collectors.toSet());
        } else {
            Integer massimo = lezioneRepository.findMaxNumeroLezione(corsoId);
            lezione.setNumeroLezione(massimo == null ? 1 : massimo + 1);
        }
        LocalTime inizio = orario != null ? orario : corso.getOrarioInizio();
        lezione.setOrarioInizio(inizio);
        if (inizio != null && corso.getDurataLezioneMinuti() != null) {
            lezione.setOrarioFine(inizio.plusMinutes(corso.getDurataLezioneMinuti()));
        }
        Lezione salvata = lezioneRepository.save(lezione);
        // Un recupero è di chi aveva la lezione saltata; le altre di chi frequenta quel giorno (tutti se non è un giorno del corso)
        for (Iscrizione iscrizione : iscrizioneRepository.findByCorsoIdWithAllievo(corsoId)) {
            boolean partecipa = diChiRecupera != null
                ? diChiRecupera.contains(iscrizione.getAllievo().getId())
                : iscrizione.frequenta(data);
            if (partecipa) {
                presenzaRepository.save(new Presenza(salvata, iscrizione.getAllievo(), Presenza.StatoPresenza.PRESENTE));
            }
        }
        // Nei corsi con gruppi il numero si ricalcola per giorno (il recupero prende quello della lezione saltata)
        lezioneRepository.flush();
        numerazioneLezioni.rinumera(corsoId);
        return salvata;
    }

    /**
     * Sposta una lezione da fare a un'altra data e orario (senza orario la lezione resta senza; la fine segue la durata
     * del corso). Tiene partecipanti e numero; la data originale resta memorizzata, così "Genera Lezioni" non la
     * ricrea; riportata alla data originale torna una lezione normale. Rifiutata se il corso ha già una lezione quel
     * giorno. Insegnante del corso, segreteria o admin.
     */
    @Transactional
    public void sposta(Long lezioneId, LocalDate data, LocalTime orario, String username) {
        if (data == null) {
            throw new IllegalArgumentException("Inserisci la data della lezione");
        }
        Lezione lezione = gestibile(lezioneId, username, false);
        if (!lezione.isDaFare()) {
            throw new IllegalStateException("Si sposta solo una lezione ancora da fare");
        }
        boolean occupata = lezioneRepository.findByCorsoIdOrderByDataAsc(lezione.getCorso().getId()).stream()
            .anyMatch(l -> !l.getId().equals(lezioneId) && l.getData().equals(data));
        if (occupata) {
            throw new IllegalStateException("Il corso ha già una lezione " + Formati.data(data));
        }
        LocalDate delCalendario = lezione.getDataDelCalendario();
        lezione.setDataOriginale(data.equals(delCalendario) ? null : delCalendario);
        lezione.setData(data);
        Integer durata = lezione.getCorso().getDurataLezioneMinuti();
        lezione.setOrarioInizio(orario);
        lezione.setOrarioFine(orario != null && durata != null ? orario.plusMinutes(durata) : null);
        lezioneRepository.flush();
        numerazioneLezioni.rinumera(lezione.getCorso().getId());
    }

    /** Elimina una lezione aggiunta a mano non ancora completata (insegnante del corso o admin). */
    @Transactional
    public void eliminaAggiunta(Long lezioneId, String username) {
        Lezione lezione = gestibile(lezioneId, username, true);
        if (!lezione.isAggiunta() || lezione.isCompletata()) {
            throw new IllegalStateException("Si eliminano solo le lezioni aggiunte non completate");
        }
        lezioneRepository.delete(lezione);
        lezioneRepository.flush();
        numerazioneLezioni.rinumera(lezione.getCorso().getId());
    }

    /** {@code true} se l'utente può eliminare le lezioni aggiunte del corso (il suo insegnante o un admin). */
    public boolean puoEliminare(Corso corso, String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        return user != null && (user.getRoles().contains("ADMIN") || insegnanteDelCorso(corso, username));
    }

    private Lezione gestibile(Long lezioneId, String username, boolean elimina) {
        Lezione lezione = lezioneRepository.findById(lezioneId)
            .orElseThrow(() -> new IllegalArgumentException("Lezione non trovata: " + lezioneId));
        verificaPermesso(lezione.getCorso(), username, elimina);
        return lezione;
    }

    /**
     * Corso attivo e utente autorizzato: l'insegnante del corso, oppure admin e segreteria
     * (per eliminare solo admin).
     */
    private void verificaPermesso(Corso corso, String username, boolean elimina) {
        CorsoService.verificaAttivo(corso);
        User user = userRepository.findByUsername(username).orElse(null);
        Set<String> ruoli = user != null ? user.getRoles() : Set.of();
        boolean autorizzato = insegnanteDelCorso(corso, username) || ruoli.contains("ADMIN")
            || (!elimina && ruoli.contains("SEGRETERIA"));
        if (!autorizzato) {
            throw new IllegalStateException("Non puoi modificare le lezioni di questo corso");
        }
    }

    private static boolean insegnanteDelCorso(Corso corso, String username) {
        return corso.haInsegnante(username);
    }

    /**
     * Presenze della lezione nell'ordine dei partecipanti del corso ({@link Iscrizione#ORDINE}: gruppo, orario,
     * inserimento); in fondo, per nome, quelle di allievi non più iscritti.
     */
    public List<Presenza> presenzeInOrdine(Long lezioneId) {
        List<Presenza> presenze = presenzaRepository.findByLezioneId(lezioneId);
        if (presenze.isEmpty()) {
            return presenze;
        }
        Map<Long, Iscrizione> iscrizioni = new HashMap<>();
        iscrizioneRepository.findByCorsoIdWithAllievo(presenze.getFirst().getLezione().getCorso().getId())
            .forEach(i -> iscrizioni.put(i.getAllievo().getId(), i));
        return presenze.stream()
            .sorted(Comparator.comparing((Presenza p) -> iscrizioni.get(p.getAllievo().getId()),
                    Comparator.nullsLast(Iscrizione.ORDINE))
                .thenComparing(p -> p.getAllievo().getNomeCompleto()))
            .toList();
    }

    /** Gruppi per id del corso, per il numero della lezione con il gruppo o il giorno ({@code Formati.numeroLezione}). */
    public Map<Long, List<GruppoCorso>> gruppiPerCorso() {
        return numerazioneLezioni.gruppiPerCorso();
    }

    /** Gruppo dei partecipanti del corso che ne hanno uno, per id dell'allievo (vuota se il corso non ha gruppi). */
    public Map<Long, GruppoCorso> gruppiDeiPartecipanti(Long corsoId) {
        Map<Long, GruppoCorso> gruppi = new HashMap<>();
        iscrizioneRepository.findByCorsoIdWithAllievo(corsoId).stream()
            .filter(i -> i.getGruppo() != null)
            .forEach(i -> gruppi.put(i.getAllievo().getId(), i.getGruppo()));
        return gruppi;
    }
}
