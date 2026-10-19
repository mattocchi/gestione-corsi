package it.attocchi.service;

import it.attocchi.entity.Anagrafica;
import it.attocchi.entity.Genitore;
import it.attocchi.entity.Insegnante;
import it.attocchi.entity.User;
import it.attocchi.repository.AllievoRepository;
import it.attocchi.repository.GenitoreRepository;
import it.attocchi.repository.InsegnanteRepository;
import it.attocchi.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Schede di insegnante e genitore degli utenti con quei ruoli: gli elenchi Insegnanti e Genitori mostrano le
 * schede, non gli utenti, quindi un ruolo assegnato da Gestione Utenti crea la scheda che manca (con l'anagrafica
 * copiata da un'altra scheda dello stesso utente). Togliere il ruolo non cancella la scheda.
 */
@Service
@Transactional(readOnly = true)
public class SchedeRuoliService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final InsegnanteRepository insegnanteRepository;
    private final GenitoreRepository genitoreRepository;
    private final AllievoRepository allievoRepository;

    public SchedeRuoliService(UserService userService, UserRepository userRepository,
                              InsegnanteRepository insegnanteRepository, GenitoreRepository genitoreRepository,
                              AllievoRepository allievoRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.insegnanteRepository = insegnanteRepository;
        this.genitoreRepository = genitoreRepository;
        this.allievoRepository = allievoRepository;
    }

    /** Salva l'utente da Gestione Utenti e crea le schede che mancano per i suoi ruoli. */
    @Transactional
    public User salvaUtente(User user, String rawPassword) {
        User salvato = userService.save(user, rawPassword);
        completaSchede(salvato);
        return salvato;
    }

    /** Crea le schede mancanti di tutti gli utenti (all'avvio); restituisce quante ne ha create. */
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
