package it.attocchi.service;

import it.attocchi.entity.GruppoCorso;
import it.attocchi.entity.Iscrizione;
import it.attocchi.repository.CorsoRepository;
import it.attocchi.repository.GenitoreRepository;
import it.attocchi.repository.GruppoCorsoRepository;
import it.attocchi.repository.InsegnanteRepository;
import it.attocchi.repository.IscrizioneRepository;
import it.attocchi.repository.LezioneRepository;
import it.attocchi.repository.AllievoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
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

    public DashboardService(CorsoRepository corsoRepository, AllievoRepository allievoRepository,
                            GenitoreRepository genitoreRepository, InsegnanteRepository insegnanteRepository,
                            IscrizioneRepository iscrizioneRepository, LezioneRepository lezioneRepository,
                            GruppoCorsoRepository gruppoRepository) {
        this.corsoRepository = corsoRepository;
        this.allievoRepository = allievoRepository;
        this.genitoreRepository = genitoreRepository;
        this.insegnanteRepository = insegnanteRepository;
        this.iscrizioneRepository = iscrizioneRepository;
        this.lezioneRepository = lezioneRepository;
        this.gruppoRepository = gruppoRepository;
    }

    public RiepilogoGenerale riepilogoGenerale() {
        return new RiepilogoGenerale(corsoRepository.count(), allievoRepository.count(),
            iscrizioneRepository.countByCorsoAttivoTrue(), genitoreRepository.count(), insegnanteRepository.count());
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
