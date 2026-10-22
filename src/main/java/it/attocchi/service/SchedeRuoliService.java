package it.attocchi.service;

import it.attocchi.entity.Allievo;
import it.attocchi.entity.Anagrafica;
import it.attocchi.entity.Genitore;
import it.attocchi.entity.Insegnante;
import it.attocchi.entity.User;
import it.attocchi.repository.AllievoRepository;
import it.attocchi.repository.GenitoreRepository;
import it.attocchi.repository.IscrizioneRepository;
import it.attocchi.repository.InsegnanteRepository;
import it.attocchi.repository.PagamentoRepository;
import it.attocchi.repository.PresenzaRepository;
import it.attocchi.repository.RecuperoPasswordRepository;
import it.attocchi.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Schede di insegnante, genitore e allievo degli utenti con quei ruoli: gli elenchi mostrano le schede, non gli
 * utenti, quindi un ruolo assegnato da Gestione Utenti crea la scheda che manca (con l'anagrafica copiata da
 * un'altra scheda dello stesso utente). Togliere il ruolo non cancella la scheda, che resta come incongruenza.
 */
@Service
@Transactional(readOnly = true)
public class SchedeRuoliService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final InsegnanteRepository insegnanteRepository;
    private final GenitoreRepository genitoreRepository;
    private final AllievoRepository allievoRepository;
    private final InsegnanteService insegnanteService;
    private final PagamentoRepository pagamentoRepository;
    private final IscrizioneRepository iscrizioneRepository;
    private final PresenzaRepository presenzaRepository;
    private final RecuperoPasswordRepository recuperoPasswordRepository;

    public SchedeRuoliService(UserService userService, UserRepository userRepository,
                              InsegnanteRepository insegnanteRepository, GenitoreRepository genitoreRepository,
                              AllievoRepository allievoRepository, InsegnanteService insegnanteService,
                              PagamentoRepository pagamentoRepository, IscrizioneRepository iscrizioneRepository,
                              PresenzaRepository presenzaRepository, RecuperoPasswordRepository recuperoPasswordRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.insegnanteRepository = insegnanteRepository;
        this.genitoreRepository = genitoreRepository;
        this.allievoRepository = allievoRepository;
        this.insegnanteService = insegnanteService;
        this.pagamentoRepository = pagamentoRepository;
        this.iscrizioneRepository = iscrizioneRepository;
        this.presenzaRepository = presenzaRepository;
        this.recuperoPasswordRepository = recuperoPasswordRepository;
    }

    /**
     * Cosa comporta eliminare l'utente: le schede che verrebbero eliminate con lui (es. "scheda genitore, 2 figli
     * restano senza genitore") e, se non si può, il motivo (scheda con collegamenti, o il proprio utente).
     */
    public record EliminazioneUtente(List<String> schede, String motivoBlocco) {

        public boolean possibile() {
            return motivoBlocco == null;
        }
    }

    public EliminazioneUtente verificaEliminazione(Long userId, String usernameCorrente) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("Utente non trovato: " + userId));
        if (user.getUsername().equals(usernameCorrente)) {
            return new EliminazioneUtente(List.of(), "non puoi eliminare il tuo utente");
        }
        List<String> schede = new ArrayList<>();
        Optional<Allievo> allievo = allievoRepository.findByUserId(userId);
        if (allievo.isPresent()) {
            Long allievoId = allievo.get().getId();
            if (pagamentoRepository.existsByAllievo(allievoId)) {
                return new EliminazioneUtente(List.of(), "la scheda allievo ha pagamenti registrati");
            }
            if (!iscrizioneRepository.findByAllievoIdWithCorso(allievoId).isEmpty()) {
                return new EliminazioneUtente(List.of(), "l'allievo è iscritto a dei corsi: toglilo prima dai partecipanti");
            }
            if (!presenzaRepository.findByAllievoId(allievoId).isEmpty()) {
                return new EliminazioneUtente(List.of(), "l'allievo ha presenze registrate in lezioni passate");
            }
            schede.add("scheda allievo");
        }
        Optional<Insegnante> insegnante = insegnanteRepository.findByUserId(userId);
        if (insegnante.isPresent()) {
            String motivo = insegnanteService.motivoPerNonEliminare(insegnante.get().getId());
            if (motivo != null) {
                return new EliminazioneUtente(List.of(), "la scheda insegnante " + motivo);
            }
            schede.add("scheda insegnante");
        }
        Optional<Genitore> genitore = genitoreRepository.findByUserId(userId);
        if (genitore.isPresent()) {
            long figli = figli(genitore.get()).size();
            schede.add("scheda genitore" + (figli == 0 ? "" : figli == 1 ? " (1 figlio resta senza genitore)"
                : " (" + figli + " figli restano senza genitore)"));
        }
        return new EliminazioneUtente(schede, null);
    }

    /**
     * Elimina l'utente con le sue schede e i suoi link di recupero password, se {@link #verificaEliminazione} lo
     * permette; altrimenti {@link IllegalStateException} con il motivo.
     */
    @Transactional
    public void eliminaUtente(Long userId, String usernameCorrente) {
        EliminazioneUtente verifica = verificaEliminazione(userId, usernameCorrente);
        if (!verifica.possibile()) {
            throw new IllegalStateException("Impossibile eliminare l'utente: " + verifica.motivoBlocco());
        }
        allievoRepository.findByUserId(userId).ifPresent(allievoRepository::delete);
        insegnanteRepository.findByUserId(userId).ifPresent(insegnanteRepository::delete);
        genitoreRepository.findByUserId(userId).ifPresent(genitore -> {
            figli(genitore).forEach(figlio -> figlio.setGenitore(null));
            genitoreRepository.delete(genitore);
        });
        recuperoPasswordRepository.deleteByUserId(userId);
        userRepository.deleteById(userId);
    }

    private List<Allievo> figli(Genitore genitore) {
        return allievoRepository.findAll().stream().filter(a -> genitore.equals(a.getGenitore())).toList();
    }

    /** Salva l'utente da Gestione Utenti e crea le schede che mancano per i suoi ruoli. */
    @Transactional
    public User salvaUtente(User user, String rawPassword) {
        User salvato = userService.save(user, rawPassword);
        completaSchede(salvato);
        return salvato;
    }

    /**
     * Incongruenze tra ruoli e schede, per id dell'utente (solo gli utenti che ne hanno): un ruolo senza la sua scheda
     * ("Allievo senza scheda") o una scheda rimasta senza il ruolo ("Scheda allievo senza ruolo").
     */
    public Map<Long, List<String>> incongruenze() {
        Map<String, Set<Long>> schede = new LinkedHashMap<>();
        schede.put("INSEGNANTE", insegnanteRepository.findAll().stream().map(i -> i.getUser().getId()).collect(Collectors.toSet()));
        schede.put("GENITORE", genitoreRepository.findAll().stream().map(g -> g.getUser().getId()).collect(Collectors.toSet()));
        schede.put("ALLIEVO", allievoRepository.findAll().stream().map(a -> a.getUser().getId()).collect(Collectors.toSet()));
        Map<Long, List<String>> incongruenze = new HashMap<>();
        for (User user : userRepository.findAll()) {
            schede.forEach((ruolo, utentiConScheda) -> {
                boolean haRuolo = user.getRoles().contains(ruolo);
                boolean haScheda = utentiConScheda.contains(user.getId());
                String nome = ruolo.charAt(0) + ruolo.substring(1).toLowerCase(Locale.ITALIAN);
                if (haRuolo && !haScheda) {
                    incongruenze.computeIfAbsent(user.getId(), k -> new ArrayList<>()).add(nome + " senza scheda");
                } else if (!haRuolo && haScheda) {
                    incongruenze.computeIfAbsent(user.getId(), k -> new ArrayList<>())
                        .add("Scheda " + nome.toLowerCase(Locale.ITALIAN) + " senza ruolo");
                }
            });
        }
        return incongruenze;
    }

    /** Crea le schede mancanti di tutti gli utenti (all'avvio e da Gestione Utenti); restituisce quante ne ha create. */
    @Transactional
    public int completaTutte() {
        return userRepository.findAll().stream().mapToInt(this::completaSchede).sum();
    }

    private int completaSchede(User user) {
        int create = 0;
        if (user.getRoles().contains("INSEGNANTE") && insegnanteRepository.findByUserId(user.getId()).isEmpty()) {
            Insegnante insegnante = new Insegnante();
            insegnante.setUser(user);
            anagraficaEsistente(user).ifPresent(da -> Anagrafica.copia(da, insegnante));
            insegnanteRepository.save(insegnante);
            create++;
        }
        if (user.getRoles().contains("GENITORE") && genitoreRepository.findByUserId(user.getId()).isEmpty()) {
            Genitore genitore = new Genitore();
            genitore.setUser(user);
            anagraficaEsistente(user).ifPresent(da -> Anagrafica.copia(da, genitore));
            genitoreRepository.save(genitore);
            create++;
        }
        if (user.getRoles().contains("ALLIEVO") && allievoRepository.findByUserId(user.getId()).isEmpty()) {
            Allievo allievo = new Allievo();
            allievo.setUser(user);
            anagraficaEsistente(user).ifPresent(da -> Anagrafica.copia(da, allievo));
            allievoRepository.save(allievo);
            create++;
        }
        return create;
    }

    /** Una scheda già esistente dello stesso utente da cui copiare codice fiscale, nascita e indirizzo. */
    private Optional<Anagrafica> anagraficaEsistente(User user) {
        return Stream.<Optional<? extends Anagrafica>>of(allievoRepository.findByUserId(user.getId()),
                insegnanteRepository.findByUserId(user.getId()), genitoreRepository.findByUserId(user.getId()))
            .flatMap(Optional::stream)
            .map(Anagrafica.class::cast)
            .findFirst();
    }
}
