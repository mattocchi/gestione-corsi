package it.attocchi.service;

import it.attocchi.entity.Allievo;
import it.attocchi.entity.Corso;
import it.attocchi.entity.GruppoCorso;
import it.attocchi.entity.Iscrizione;
import it.attocchi.entity.Lezione;
import it.attocchi.entity.Presenza;
import it.attocchi.entity.User;
import it.attocchi.repository.AllievoRepository;
import it.attocchi.repository.CorsoRepository;
import it.attocchi.repository.GenitoreRepository;
import it.attocchi.repository.GruppoCorsoRepository;
import it.attocchi.repository.InsegnanteRepository;
import it.attocchi.repository.IscrizioneRepository;
import it.attocchi.repository.LezioneRepository;
import it.attocchi.repository.PresenzaRepository;
import it.attocchi.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Lezioni del calendario mensile secondo il ruolo: admin e segreteria tutte quelle dei corsi attivi, l'insegnante
 * quelle dei suoi corsi (titolare o secondo), l'allievo quelle a cui partecipa (le sue presenze, quindi solo il suo
 * gruppo), il genitore quelle dei figli. Chi ha più ruoli le vede tutte, senza doppioni.
 */
@Service
@Transactional(readOnly = true)
public class CalendarioService {

    /**
     * Lezione del calendario; {@code perChi} è il figlio per il genitore ({@code null} negli altri casi). Nei corsi con
     * gruppi una voce per gruppo: {@code gruppo} è il gruppo della voce, {@code senzaGruppo} la voce di chi non ne
     * ha (o è in un gruppo senza orario); {@code orario} quello del gruppo, altrimenti quello della lezione (per
     * allievi e genitori quello dell'allievo, {@code Iscrizione.orarioDi}).
     */
    public record VoceCalendario(Lezione lezione, String perChi, GruppoCorso gruppo, boolean senzaGruppo, LocalTime orario) {

        /** Voce della lezione intera, all'orario della lezione. */
        public VoceCalendario(Lezione lezione, String perChi) {
            this(lezione, perChi, null, false, lezione.getOrarioInizio());
        }
    }

    /**
     * Voce della settimana tipo: un corso (o un suo gruppo) in un giorno della settimana, dall'orario d'inizio
     * ({@code null} se il corso non ne ha) alla fine secondo la durata; {@code senzaGruppo} la voce di chi non ha
     * gruppo in un corso con gruppi quel giorno; {@code perChi} il figlio per il genitore.
     */
    public record VoceSettimana(DayOfWeek giorno, LocalTime inizio, LocalTime fine, Corso corso, GruppoCorso gruppo,
                                boolean senzaGruppo, String perChi) {
    }

    private static final Comparator<VoceSettimana> PER_GIORNO_E_ORARIO = Comparator
        .comparing(VoceSettimana::giorno)
        .thenComparing(VoceSettimana::inizio, Comparator.nullsLast(Comparator.<LocalTime>naturalOrder()))
        .thenComparing(v -> v.corso().getNome(), String.CASE_INSENSITIVE_ORDER)
        .thenComparing(VoceSettimana::senzaGruppo)
        .thenComparing(v -> Objects.toString(v.perChi(), ""));

    private static final Comparator<VoceCalendario> PER_DATA_E_ORARIO = Comparator
        .comparing((VoceCalendario v) -> v.lezione().getData())
        .thenComparing(VoceCalendario::orario, Comparator.nullsLast(Comparator.<LocalTime>naturalOrder()))
        .thenComparing(v -> v.lezione().getCorso().getNome(), String.CASE_INSENSITIVE_ORDER)
        .thenComparing(VoceCalendario::senzaGruppo)
        .thenComparing(v -> Objects.toString(v.perChi(), ""));

    private final UserRepository userRepository;
    private final LezioneRepository lezioneRepository;
    private final InsegnanteRepository insegnanteRepository;
    private final AllievoRepository allievoRepository;
    private final GenitoreRepository genitoreRepository;
    private final PresenzaRepository presenzaRepository;
    private final IscrizioneRepository iscrizioneRepository;
    private final CorsoRepository corsoRepository;
    private final GruppoCorsoRepository gruppoCorsoRepository;

    public CalendarioService(UserRepository userRepository, LezioneRepository lezioneRepository,
                             InsegnanteRepository insegnanteRepository, AllievoRepository allievoRepository,
                             GenitoreRepository genitoreRepository, PresenzaRepository presenzaRepository,
                             IscrizioneRepository iscrizioneRepository, CorsoRepository corsoRepository,
                             GruppoCorsoRepository gruppoCorsoRepository) {
        this.corsoRepository = corsoRepository;
        this.gruppoCorsoRepository = gruppoCorsoRepository;
        this.userRepository = userRepository;
        this.lezioneRepository = lezioneRepository;
        this.insegnanteRepository = insegnanteRepository;
        this.allievoRepository = allievoRepository;
        this.genitoreRepository = genitoreRepository;
        this.presenzaRepository = presenzaRepository;
        this.iscrizioneRepository = iscrizioneRepository;
    }

    /** Lezioni del mese visibili all'utente, dei corsi attivi; {@code corsoId} non nullo = solo quel corso. */
    public List<VoceCalendario> lezioniDelMese(String username, YearMonth mese, Long corsoId) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return List.of();
        }
        Set<String> ruoli = user.getRoles();
        Predicate<Lezione> visibile = l -> l.getCorso().isAttivo() && YearMonth.from(l.getData()).equals(mese)
            && (corsoId == null || l.getCorso().getId().equals(corsoId));
        Map<String, VoceCalendario> voci = new LinkedHashMap<>();
        // Staff: le lezioni dei corsi (tutti o i propri), una voce per gruppo nei corsi con gruppi
        Map<Long, Lezione> delloStaff = new LinkedHashMap<>();
        if (ruoli.contains("ADMIN") || ruoli.contains("SEGRETERIA")) {
            lezioneRepository.findByDataBetweenOrderByDataAsc(mese.atDay(1), mese.atEndOfMonth()).stream()
                .filter(visibile).forEach(l -> delloStaff.putIfAbsent(l.getId(), l));
        }
        if (ruoli.contains("INSEGNANTE")) {
            insegnanteRepository.findByUserId(user.getId()).ifPresent(insegnante ->
                lezioneRepository.findDeiCorsiAttiviDellInsegnante(insegnante.getId()).stream()
                    .filter(visibile).forEach(l -> delloStaff.putIfAbsent(l.getId(), l)));
        }
        perGruppo(delloStaff.values()).forEach(v -> aggiungi(voci, v));
        // Famiglie: le lezioni dell'allievo, con il suo gruppo
        if (ruoli.contains("ALLIEVO")) {
            allievoRepository.findByUserId(user.getId()).ifPresent(allievo -> vociDi(allievo, null, visibile)
                .forEach(v -> aggiungi(voci, v)));
        }
        if (ruoli.contains("GENITORE")) {
            genitoreRepository.findByUserId(user.getId()).ifPresent(genitore ->
                allievoRepository.findByGenitoreIdWithCorsi(genitore.getId()).forEach(figlio ->
                    vociDi(figlio, figlio.getNomeCompleto(), visibile).forEach(v -> aggiungi(voci, v))));
        }
        return voci.values().stream().sorted(PER_DATA_E_ORARIO).toList();
    }

    /**
     * Settimana tipo dei corsi attivi visibili all'utente (stessi ruoli del calendario del mese), per giorno e orario;
     * {@code corsoId} non nullo = solo quel corso. Staff: per ogni giorno del corso una voce, o una per gruppo di
     * quel giorno (al suo orario) più "senza gruppo" se qualcuno non ne ha; allievo e genitore: il giorno del gruppo
     * dell'allievo, o tutti i giorni del corso, al suo orario ({@link Iscrizione#orarioAbituale()}) o a quello del corso.
     */
    public List<VoceSettimana> settimanaTipo(String username, Long corsoId) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return List.of();
        }
        Set<String> ruoli = user.getRoles();
        Predicate<Corso> visibile = c -> c.isAttivo() && (corsoId == null || c.getId().equals(corsoId));
        Map<Long, Corso> delloStaff = new LinkedHashMap<>();
        if (ruoli.contains("ADMIN") || ruoli.contains("SEGRETERIA")) {
            corsoRepository.findByAttivoTrue().stream().filter(visibile).forEach(c -> delloStaff.putIfAbsent(c.getId(), c));
        }
        if (ruoli.contains("INSEGNANTE")) {
            insegnanteRepository.findByUserId(user.getId()).ifPresent(insegnante ->
                corsoRepository.findDellInsegnante(insegnante.getId()).stream().filter(visibile)
                    .forEach(c -> delloStaff.putIfAbsent(c.getId(), c)));
        }
        Set<VoceSettimana> voci = new LinkedHashSet<>();
        delloStaff.values().forEach(corso -> voci.addAll(settimanaDelCorso(corso)));
        if (ruoli.contains("ALLIEVO")) {
            allievoRepository.findByUserId(user.getId()).ifPresent(allievo ->
                voci.addAll(settimanaDi(allievo, null, visibile)));
        }
        if (ruoli.contains("GENITORE")) {
            genitoreRepository.findByUserId(user.getId()).ifPresent(genitore ->
                allievoRepository.findByGenitoreIdWithCorsi(genitore.getId()).forEach(figlio ->
                    voci.addAll(settimanaDi(figlio, figlio.getNomeCompleto(), visibile))));
        }
        return voci.stream().sorted(PER_GIORNO_E_ORARIO).toList();
    }

    /** Voci dello staff per un corso: per giorno, o per gruppo del giorno più chi non ha gruppo. */
    private List<VoceSettimana> settimanaDelCorso(Corso corso) {
        List<GruppoCorso> gruppi = gruppoCorsoRepository.findByCorsoId(corso.getId());
        boolean qualcunoSenzaGruppo = iscrizioneRepository.findByCorsoIdWithAllievo(corso.getId()).stream()
            .anyMatch(i -> i.getGruppo() == null);
        List<VoceSettimana> voci = new ArrayList<>();
        for (DayOfWeek giorno : corso.getGiorniSettimana()) {
            List<GruppoCorso> delGiorno = gruppi.stream().filter(g -> g.getGiorno() == giorno).toList();
            if (delGiorno.isEmpty()) {
                voci.add(voce(giorno, corso.getOrarioInizio(), corso, null, false, null));
                continue;
            }
            delGiorno.forEach(g -> voci.add(voce(giorno,
                g.getOrario() != null ? g.getOrario() : corso.getOrarioInizio(), corso, g, false, null)));
            if (qualcunoSenzaGruppo) {
                voci.add(voce(giorno, corso.getOrarioInizio(), corso, null, true, null));
            }
        }
        return voci;
    }

    /** Voci dell'allievo: il giorno del suo gruppo, o tutti quelli del corso, al suo orario. */
    private List<VoceSettimana> settimanaDi(Allievo allievo, String perChi, Predicate<Corso> visibile) {
        List<VoceSettimana> voci = new ArrayList<>();
        for (Iscrizione iscrizione : iscrizioneRepository.findByAllievoIdWithCorso(allievo.getId())) {
            Corso corso = iscrizione.getCorso();
            if (!visibile.test(corso)) {
                continue;
            }
            GruppoCorso gruppo = iscrizione.getGruppo();
            LocalTime orario = Objects.requireNonNullElse(iscrizione.orarioAbituale(), corso.getOrarioInizio());
            Collection<DayOfWeek> giorni = gruppo != null ? List.of(gruppo.getGiorno()) : corso.getGiorniSettimana();
            giorni.forEach(giorno -> voci.add(voce(giorno, orario, corso, gruppo, false, perChi)));
        }
        return voci;
    }

    private static VoceSettimana voce(DayOfWeek giorno, LocalTime inizio, Corso corso, GruppoCorso gruppo,
                                      boolean senzaGruppo, String perChi) {
        Integer durata = corso.getDurataLezioneMinuti();
        LocalTime fine = inizio != null && durata != null ? inizio.plusMinutes(durata) : null;
        return new VoceSettimana(giorno, inizio, fine, corso, gruppo, senzaGruppo, perChi);
    }

    /**
     * Voci dello staff: nei corsi con gruppi una per ogni gruppo con orario che ha allievi alla lezione, più una
     * all'orario della lezione per chi non ha gruppo (o è in un gruppo senza orario); se nessuno dei presenti ha un
     * gruppo con orario, o nei corsi senza gruppi, la lezione intera.
     */
    public List<VoceCalendario> perGruppo(Collection<Lezione> lezioni) {
        Map<Long, Map<Long, GruppoCorso>> gruppiPerCorso = new HashMap<>();
        Map<Long, List<Long>> presentiPerLezione = new HashMap<>();
        List<Lezione> conGruppi = new ArrayList<>();
        for (Lezione lezione : lezioni) {
            Map<Long, GruppoCorso> gruppi = gruppiPerCorso.computeIfAbsent(lezione.getCorso().getId(), this::gruppiDegliAllievi);
            if (!gruppi.isEmpty()) {
                conGruppi.add(lezione);
            }
        }
        if (!conGruppi.isEmpty()) {
            presenzaRepository.findByLezioneIdIn(conGruppi.stream().map(Lezione::getId).toList()).forEach(p ->
                presentiPerLezione.computeIfAbsent(p.getLezione().getId(), k -> new ArrayList<>()).add(p.getAllievo().getId()));
        }
        List<VoceCalendario> voci = new ArrayList<>();
        for (Lezione lezione : lezioni) {
            Map<Long, GruppoCorso> gruppi = gruppiPerCorso.get(lezione.getCorso().getId());
            List<Long> presenti = presentiPerLezione.getOrDefault(lezione.getId(), List.of());
            Set<GruppoCorso> conOrario = new LinkedHashSet<>();
            Set<GruppoCorso> altri = new HashSet<>();
            boolean senzaGruppo = false;
            for (Long allievoId : presenti) {
                GruppoCorso gruppo = gruppi.get(allievoId);
                if (gruppo != null && gruppo.getOrario() != null) {
                    conOrario.add(gruppo);
                } else if (gruppo != null) {
                    altri.add(gruppo);
                } else {
                    senzaGruppo = true;
                }
            }
            if (conOrario.isEmpty()) {
                voci.add(new VoceCalendario(lezione, null));
                continue;
            }
            conOrario.forEach(g -> voci.add(new VoceCalendario(lezione, null, g, false, g.getOrario())));
            if (senzaGruppo || altri.size() > 1) {
                voci.add(new VoceCalendario(lezione, null, null, true, lezione.getOrarioInizio()));
            } else if (altri.size() == 1) {
                voci.add(new VoceCalendario(lezione, null, altri.iterator().next(), false, lezione.getOrarioInizio()));
            }
        }
        return voci;
    }

    /** Gruppo di ogni partecipante del corso che ne ha uno, per id dell'allievo (vuota se nessuno ha un gruppo). */
    private Map<Long, GruppoCorso> gruppiDegliAllievi(Long corsoId) {
        Map<Long, GruppoCorso> gruppi = new HashMap<>();
        iscrizioneRepository.findByCorsoIdWithAllievo(corsoId).stream()
            .filter(i -> i.getGruppo() != null)
            .forEach(i -> gruppi.put(i.getAllievo().getId(), i.getGruppo()));
        return gruppi;
    }

    /**
     * Tutte le lezioni dell'allievo (le sue presenze), con il suo gruppo e il suo orario ({@link Iscrizione#orarioDi}:
     * il suo, poi quello del gruppo, poi quello della lezione); {@code perChi} è il nome del figlio per il genitore.
     */
    public List<VoceCalendario> vociDi(Allievo allievo, String perChi) {
        return vociDi(allievo, perChi, l -> true);
    }

    private List<VoceCalendario> vociDi(Allievo allievo, String perChi, Predicate<Lezione> visibile) {
        Map<Long, Iscrizione> iscrizioni = new HashMap<>();
        iscrizioneRepository.findByAllievoIdWithCorso(allievo.getId()).forEach(i -> iscrizioni.put(i.getCorso().getId(), i));
        return presenzaRepository.findByAllievoId(allievo.getId()).stream()
            .map(Presenza::getLezione)
            .filter(visibile)
            .map(l -> {
                Iscrizione iscrizione = iscrizioni.get(l.getCorso().getId());
                return iscrizione == null ? new VoceCalendario(l, perChi)
                    : new VoceCalendario(l, perChi, iscrizione.getGruppo(), false, iscrizione.orarioDi(l));
            })
            .toList();
    }

    /** Una voce per lezione, gruppo e persona: la stessa voce vista da più ruoli compare una volta sola. */
    private static void aggiungi(Map<String, VoceCalendario> voci, VoceCalendario voce) {
        voci.putIfAbsent(voce.lezione().getId() + "|" + Objects.toString(voce.perChi(), "") + "|"
            + (voce.gruppo() != null ? voce.gruppo().getId() : voce.senzaGruppo() ? "senza" : ""), voce);
    }
}
