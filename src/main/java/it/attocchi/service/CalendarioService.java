package it.attocchi.service;

import it.attocchi.entity.Allievo;
import it.attocchi.entity.Lezione;
import it.attocchi.entity.User;
import it.attocchi.repository.AllievoRepository;
import it.attocchi.repository.GenitoreRepository;
import it.attocchi.repository.InsegnanteRepository;
import it.attocchi.repository.LezioneRepository;
import it.attocchi.repository.PresenzaRepository;
import it.attocchi.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashMap;
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

    /** Lezione del calendario; {@code perChi} è il figlio per il genitore ({@code null} negli altri casi). */
    public record VoceCalendario(Lezione lezione, String perChi) {
    }

    private static final Comparator<VoceCalendario> PER_DATA_E_ORARIO = Comparator
        .comparing((VoceCalendario v) -> v.lezione().getData())
        .thenComparing(v -> v.lezione().getOrarioInizio(), Comparator.nullsLast(Comparator.<LocalTime>naturalOrder()))
        .thenComparing(v -> v.lezione().getCorso().getNome(), String.CASE_INSENSITIVE_ORDER)
        .thenComparing(v -> Objects.toString(v.perChi(), ""));

    private final UserRepository userRepository;
    private final LezioneRepository lezioneRepository;
    private final InsegnanteRepository insegnanteRepository;
    private final AllievoRepository allievoRepository;
    private final GenitoreRepository genitoreRepository;
    private final PresenzaRepository presenzaRepository;

    public CalendarioService(UserRepository userRepository, LezioneRepository lezioneRepository,
                             InsegnanteRepository insegnanteRepository, AllievoRepository allievoRepository,
                             GenitoreRepository genitoreRepository, PresenzaRepository presenzaRepository) {
        this.userRepository = userRepository;
        this.lezioneRepository = lezioneRepository;
        this.insegnanteRepository = insegnanteRepository;
        this.allievoRepository = allievoRepository;
        this.genitoreRepository = genitoreRepository;
        this.presenzaRepository = presenzaRepository;
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
        if (ruoli.contains("ADMIN") || ruoli.contains("SEGRETERIA")) {
            lezioneRepository.findByDataBetweenOrderByDataAsc(mese.atDay(1), mese.atEndOfMonth()).stream()
                .filter(visibile).forEach(l -> aggiungi(voci, l, null));
        }
        if (ruoli.contains("INSEGNANTE")) {
            insegnanteRepository.findByUserId(user.getId()).ifPresent(insegnante ->
                lezioneRepository.findDeiCorsiAttiviDellInsegnante(insegnante.getId()).stream()
                    .filter(visibile).forEach(l -> aggiungi(voci, l, null)));
        }
        if (ruoli.contains("ALLIEVO")) {
            allievoRepository.findByUserId(user.getId()).ifPresent(allievo -> lezioniDi(allievo).stream()
                .filter(visibile).forEach(l -> aggiungi(voci, l, null)));
        }
        if (ruoli.contains("GENITORE")) {
            genitoreRepository.findByUserId(user.getId()).ifPresent(genitore ->
                allievoRepository.findByGenitoreIdWithCorsi(genitore.getId()).forEach(figlio -> lezioniDi(figlio).stream()
                    .filter(visibile).forEach(l -> aggiungi(voci, l, figlio.getNomeCompleto()))));
        }
        return voci.values().stream().sorted(PER_DATA_E_ORARIO).toList();
    }

    private List<Lezione> lezioniDi(Allievo allievo) {
        return presenzaRepository.findByAllievoId(allievo.getId()).stream().map(p -> p.getLezione()).toList();
    }

    /** Una voce per lezione e persona: la stessa lezione vista da più ruoli compare una volta sola. */
    private static void aggiungi(Map<String, VoceCalendario> voci, Lezione lezione, String perChi) {
        voci.putIfAbsent(lezione.getId() + "|" + Objects.toString(perChi, ""), new VoceCalendario(lezione, perChi));
    }
}
