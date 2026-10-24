package it.attocchi.service;

import it.attocchi.entity.Allievo;
import it.attocchi.entity.GruppoCorso;
import it.attocchi.entity.Iscrizione;
import it.attocchi.entity.Lezione;
import it.attocchi.repository.CorsoRepository;
import it.attocchi.repository.GenitoreRepository;
import it.attocchi.repository.GruppoCorsoRepository;
import it.attocchi.repository.InsegnanteRepository;
import it.attocchi.repository.IscrizioneRepository;
import it.attocchi.repository.LezioneRepository;
import it.attocchi.repository.AllievoRepository;
import it.attocchi.service.CalendarioService.VoceCalendario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Conteggi di riepilogo per la dashboard iniziale, in funzione del ruolo.
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    /** Riepilogo generale (admin e segreteria); {@code iscritti} = iscrizioni ai corsi attivi (un allievo può averne più). */
    public record RiepilogoGenerale(long corsi, long allievi, long iscritti, long genitori, long insegnanti) {
    }

    /** Riepilogo dell'insegnante sui propri corsi. */
    /** Età in anni compiuti degli allievi dei corsi attivi: media arrotondata, più giovane, più anziano. */
    public record EtaAllievi(long media, long minima, long massima, int allievi) {
    }

    public record RiepilogoInsegnante(long corsi, long lezioniFatte, long lezioniDaFare, long lezioniNonEffettuate) {
    }

    /** Un gruppo di un corso attivo con il numero dei suoi iscritti. */
    public record RigaGruppo(String corso, GruppoCorso gruppo, long iscritti) {
    }

    /**
     * Gruppi dei corsi attivi: corsi che ne hanno, numero di gruppi, iscritti senza gruppo in quei corsi e una riga
     * per gruppo.
     */
    public record RiepilogoGruppi(long corsiConGruppi, long gruppi, long allieviSenzaGruppo, List<RigaGruppo> righe) {
    }

    private final CorsoRepository corsoRepository;
    private final AllievoRepository allievoRepository;
    private final GenitoreRepository genitoreRepository;
    private final InsegnanteRepository insegnanteRepository;
    private final IscrizioneRepository iscrizioneRepository;
    private final LezioneRepository lezioneRepository;
    private final GruppoCorsoRepository gruppoRepository;
    private final CalendarioService calendarioService;

    public DashboardService(CorsoRepository corsoRepository, AllievoRepository allievoRepository,
                            GenitoreRepository genitoreRepository, InsegnanteRepository insegnanteRepository,
                            IscrizioneRepository iscrizioneRepository, LezioneRepository lezioneRepository,
                            GruppoCorsoRepository gruppoRepository, CalendarioService calendarioService) {
        this.corsoRepository = corsoRepository;
        this.allievoRepository = allievoRepository;
        this.genitoreRepository = genitoreRepository;
        this.insegnanteRepository = insegnanteRepository;
        this.iscrizioneRepository = iscrizioneRepository;
        this.lezioneRepository = lezioneRepository;
        this.gruppoRepository = gruppoRepository;
        this.calendarioService = calendarioService;
    }

    private static final Comparator<VoceCalendario> PER_DATA_E_ORARIO = Comparator
        .comparing((VoceCalendario v) -> v.lezione().getData())
        .thenComparing(VoceCalendario::orario, Comparator.nullsLast(Comparator.<LocalTime>naturalOrder()))
        .thenComparing(v -> v.lezione().getCorso().getNome(), String.CASE_INSENSITIVE_ORDER)
        .thenComparing(VoceCalendario::senzaGruppo)
        .thenComparing(v -> Objects.toString(v.perChi(), ""));

    /**
     * Prossima lezione di ogni corso attivo dell'insegnante (titolare o secondo): la prima da fare da oggi in poi,
     * divisa per gruppo come nel calendario (una voce per gruppo con orario, più "Senza gruppo").
     */
    public List<VoceCalendario> prossimeLezioniInsegnante(String usernameInsegnante) {
        return insegnanteRepository.findByUserUsername(usernameInsegnante)
            .map(insegnante -> {
                Map<Long, Lezione> prime = new HashMap<>();
                lezioneRepository.findDeiCorsiAttiviDellInsegnante(insegnante.getId()).stream()
                    .filter(DashboardService::prossima)
                    .sorted(Comparator.comparing(Lezione::getData).thenComparing(Lezione::getOrarioInizio,
                        Comparator.nullsLast(Comparator.<LocalTime>naturalOrder())))
                    .forEach(l -> prime.putIfAbsent(l.getCorso().getId(), l));
                return calendarioService.perGruppo(prime.values()).stream().sorted(PER_DATA_E_ORARIO).toList();
            })
            .orElse(List.of());
    }

    /** Prossima lezione di ogni corso attivo dell'allievo, con il suo gruppo e il suo orario. */
    public List<VoceCalendario> prossimeLezioniAllievo(String usernameAllievo) {
        return allievoRepository.findByUserUsername(usernameAllievo)
            .map(allievo -> primaPerCorso(calendarioService.vociDi(allievo, null)))
            .orElse(List.of());
    }

    /** Prossima lezione di ogni figlio del genitore in ogni suo corso attivo, con il nome, il gruppo e l'orario del figlio. */
    public List<VoceCalendario> prossimeLezioniDeiFigli(String usernameGenitore) {
        return genitoreRepository.findByUserUsername(usernameGenitore)
            .map(genitore -> primaPerCorso(allievoRepository.findByGenitoreIdWithCorsi(genitore.getId()).stream()
                .flatMap(figlio -> calendarioService.vociDi(figlio, figlio.getNomeCompleto()).stream()).toList()))
            .orElse(List.of());
    }

    /** Lezione da fare di un corso attivo, da oggi in poi. */
    private static boolean prossima(Lezione lezione) {
        return lezione.getCorso().isAttivo() && lezione.isDaFare() && !lezione.getData().isBefore(LocalDate.now());
    }

    /** Per corso (e figlio) la prima lezione da fare da oggi in poi, dei corsi attivi; per data e orario. */
    private static List<VoceCalendario> primaPerCorso(List<VoceCalendario> voci) {
        Map<String, VoceCalendario> prime = new HashMap<>();
        voci.stream()
            .filter(v -> prossima(v.lezione()))
            .sorted(PER_DATA_E_ORARIO)
            .forEach(v -> prime.putIfAbsent(v.lezione().getCorso().getId() + "|" + Objects.toString(v.perChi(), ""), v));
        return prime.values().stream().sorted(PER_DATA_E_ORARIO).toList();
    }

    public RiepilogoGenerale riepilogoGenerale() {
        return new RiepilogoGenerale(corsoRepository.count(), allievoRepository.count(),
            iscrizioneRepository.countByCorsoAttivoTrue(), genitoreRepository.count(), insegnanteRepository.count());
    }

    /**
     * Età degli allievi iscritti ad almeno un corso attivo con la data di nascita, in anni compiuti a {@code oggi};
     * vuoto se nessuno ha la data.
     */
    public Optional<EtaAllievi> etaAllievi(LocalDate oggi) {
        List<Integer> eta = iscrizioneRepository.dateNascitaIscrittiCorsiAttivi().stream()
            .map(nascita -> Period.between(nascita, oggi).getYears()).toList();
        if (eta.isEmpty()) {
            return Optional.empty();
        }
        var statistiche = eta.stream().mapToInt(Integer::intValue).summaryStatistics();
        return Optional.of(new EtaAllievi(Math.round(statistiche.getAverage()), statistiche.getMin(),
            statistiche.getMax(), eta.size()));
    }

    /** Corsi attivi dell'insegnante (titolare o secondo) e lezioni completate / da fare / non effettuate di quei corsi. */
    public RiepilogoInsegnante riepilogoInsegnante(String usernameInsegnante) {
        return new RiepilogoInsegnante(corsoRepository.contaAttiviDellInsegnante(usernameInsegnante),
            lezioneRepository.contaDellInsegnante(usernameInsegnante, true, false),
            lezioneRepository.contaDellInsegnante(usernameInsegnante, false, false),
            lezioneRepository.contaDellInsegnante(usernameInsegnante, false, true));
    }

    /** Numero di corsi attivi assegnati all'insegnante. */
    public long corsiInsegnante(String usernameInsegnante) {
        return corsoRepository.contaAttiviDellInsegnante(usernameInsegnante);
    }

    /** Numero di corsi attivi a cui è iscritto l'allievo. */
    public long corsiAllievo(String usernameAllievo) {
        return iscrizioneRepository.countByAllievoUserUsernameAndCorsoAttivoTrue(usernameAllievo);
    }

    /** Numero di corsi distinti a cui sono iscritti i figli del genitore. */
    public long corsiDeiFigli(String usernameGenitore) {
        return iscrizioneRepository.countCorsiFigliByGenitoreUsername(usernameGenitore);
    }

    /** Riepilogo dei gruppi di tutti i corsi attivi (admin e segreteria). */
    public RiepilogoGruppi riepilogoGruppi() {
        List<GruppoCorso> gruppi = gruppoRepository.findDeiCorsiAttivi();
        List<Iscrizione> iscrizioni = iscrizioneRepository.findByCorsoAttivo();
        Set<Long> corsiConGruppi = gruppi.stream().map(g -> g.getCorso().getId()).collect(Collectors.toSet());
        long senzaGruppo = iscrizioni.stream()
            .filter(i -> i.getGruppo() == null && corsiConGruppi.contains(i.getCorso().getId()))
            .count();
        return new RiepilogoGruppi(corsiConGruppi.size(), gruppi.size(), senzaGruppo, righe(gruppi, iscrizioni));
    }

    /** Gruppi dei corsi attivi dell'insegnante (titolare o secondo), una riga per gruppo. */
    public List<RigaGruppo> gruppiDellInsegnante(String usernameInsegnante) {
        List<GruppoCorso> gruppi = gruppoRepository.findDeiCorsiAttivi().stream()
            .filter(g -> g.getCorso().haInsegnante(usernameInsegnante))
            .toList();
        return gruppi.isEmpty() ? List.of() : righe(gruppi, iscrizioneRepository.findByCorsoAttivo());
    }

    /** Una riga per gruppo con i suoi iscritti, per nome del corso e poi per giorno, orario e nome del gruppo. */
    private static List<RigaGruppo> righe(List<GruppoCorso> gruppi, List<Iscrizione> iscrizioni) {
        Map<Long, Long> iscrittiPerGruppo = iscrizioni.stream()
            .filter(i -> i.getGruppo() != null)
            .collect(Collectors.groupingBy(i -> i.getGruppo().getId(), Collectors.counting()));
        Function<GruppoCorso, String> corso = g -> g.getCorso().getNome();
        return gruppi.stream()
            .sorted(Comparator.comparing(corso).thenComparing(GruppoCorso.PER_GIORNO))
            .map(g -> new RigaGruppo(corso.apply(g), g, iscrittiPerGruppo.getOrDefault(g.getId(), 0L)))
            .toList();
    }
}
