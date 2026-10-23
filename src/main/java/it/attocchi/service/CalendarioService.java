package it.attocchi.service;

import it.attocchi.entity.Allievo;
import it.attocchi.entity.GruppoCorso;
import it.attocchi.entity.Lezione;
import it.attocchi.entity.Presenza;
import it.attocchi.entity.User;
import it.attocchi.repository.AllievoRepository;
import it.attocchi.repository.GenitoreRepository;
import it.attocchi.repository.InsegnanteRepository;
import it.attocchi.repository.IscrizioneRepository;
import it.attocchi.repository.LezioneRepository;
import it.attocchi.repository.PresenzaRepository;
import it.attocchi.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     * ha (o è in un gruppo senza orario); {@code orario} quello del gruppo, altrimenti quello della lezione.
     */
    public record VoceCalendario(Lezione lezione, String perChi, GruppoCorso gruppo, boolean senzaGruppo, LocalTime orario) {

        /** Voce della lezione intera, all'orario della lezione. */
        public VoceCalendario(Lezione lezione, String perChi) {
            this(lezione, perChi, null, false, lezione.getOrarioInizio());
        }
    }

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

    public CalendarioService(UserRepository userRepository, LezioneRepository lezioneRepository,
                             InsegnanteRepository insegnanteRepository, AllievoRepository allievoRepository,
                             GenitoreRepository genitoreRepository, PresenzaRepository presenzaRepository,
                             IscrizioneRepository iscrizioneRepository) {
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
     * Voci dello staff: nei corsi con gruppi una per ogni gruppo con orario che ha allievi alla lezione, più una
     * all'orario della lezione per chi non ha gruppo (o è in un gruppo senza orario); se nessuno dei presenti ha un
     * gruppo con orario, o nei corsi senza gruppi, la lezione intera.
     */
    private List<VoceCalendario> perGruppo(Collection<Lezione> lezioni) {
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

    /** Lezioni dell'allievo (le sue presenze), con il suo gruppo e l'orario del gruppo, se ne ha uno. */
    private List<VoceCalendario> vociDi(Allievo allievo, String perChi, Predicate<Lezione> visibile) {
        Map<Long, GruppoCorso> gruppi = new HashMap<>();
        iscrizioneRepository.findByAllievoIdWithCorso(allievo.getId()).stream()
            .filter(i -> i.getGruppo() != null)
            .forEach(i -> gruppi.put(i.getCorso().getId(), i.getGruppo()));
        return presenzaRepository.findByAllievoId(allievo.getId()).stream()
            .map(Presenza::getLezione)
            .filter(visibile)
            .map(l -> {
                GruppoCorso gruppo = gruppi.get(l.getCorso().getId());
                return gruppo == null ? new VoceCalendario(l, perChi)
                    : new VoceCalendario(l, perChi, gruppo, false, gruppo.getOrario() != null ? gruppo.getOrario() : l.getOrarioInizio());
            })
            .toList();
    }

    /** Una voce per lezione, gruppo e persona: la stessa voce vista da più ruoli compare una volta sola. */
    private static void aggiungi(Map<String, VoceCalendario> voci, VoceCalendario voce) {
        voci.putIfAbsent(voce.lezione().getId() + "|" + Objects.toString(voce.perChi(), "") + "|"
            + (voce.gruppo() != null ? voce.gruppo().getId() : voce.senzaGruppo() ? "senza" : ""), voce);
    }
}
