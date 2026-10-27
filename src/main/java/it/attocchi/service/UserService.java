package it.attocchi.service;

import it.attocchi.entity.User;
import it.attocchi.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserService {

    /** Lunghezza minima di una nuova password (recupero password e cambio dal profilo). */
    public static final int PASSWORD_MINIMA = 8;


    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    /**
     * Salva l'utente. Se {@code rawPassword} non è vuota viene cifrata e impostata;
     * altrimenti la password esistente resta invariata (obbligatoria per i nuovi utenti).
     */
    @Transactional
    public User save(User user, String rawPassword) {
        // Riattivato a mano (o disattivato a mano): non è più una sospensione per inattività
        if (user.isEnabled() || user.getId() == null) {
            user.setSospesoPerInattivita(false);
        }
        if (rawPassword != null && !rawPassword.isBlank()) {
            user.setPassword(passwordEncoder.encode(rawPassword));
        } else if (user.getId() == null) {
            throw new IllegalArgumentException("La password è obbligatoria per un nuovo utente");
        }
        return userRepository.save(user);
    }

    /** Mesi senza accessi dopo cui un account viene sospeso ({@link #sospendiInattivi}). */
    public static final int MESI_INATTIVITA = 6;

    /**
     * Sospende gli account attivi che non entrano da più di {@link #MESI_INATTIVITA} mesi (rispetto a {@code adesso}),
     * tranne gli admin e chi non è mai entrato: disattivati e segnati "sospesi per inattività", si riattivano con il
     * recupero password. Restituisce gli username sospesi.
     */
    @Transactional
    public List<String> sospendiInattivi(LocalDateTime adesso) {
        List<String> sospesi = new ArrayList<>();
        for (User user : userRepository.findByEnabledTrueAndUltimoAccessoBefore(adesso.minusMonths(MESI_INATTIVITA))) {
            if (user.getRoles().contains("ADMIN")) {
                continue;
            }
            user.setEnabled(false);
            user.setSospesoPerInattivita(true);
            sospesi.add(user.getUsername());
        }
        return sospesi;
    }

    /** Registra adesso come ultimo accesso dell'utente indicato (dopo un login riuscito). */
    @Transactional
    public void registraAccesso(String username) {
        userRepository.findByUsername(username).ifPresent(user -> user.setUltimoAccesso(LocalDateTime.now()));
    }

    /** Cambia la password dell'utente indicato (profilo personale). */
    @Transactional
    public void cambiaPassword(String username, String nuovaPassword) {
        if (nuovaPassword == null || nuovaPassword.length() < PASSWORD_MINIMA) {
            throw new IllegalArgumentException("La password deve avere almeno " + PASSWORD_MINIMA + " caratteri");
        }
        utente(username).setPassword(passwordEncoder.encode(nuovaPassword));
    }

    /** Copia nell'utente indicato solo i dati che ognuno può modificare nel proprio profilo. */
    @Transactional
    public void aggiornaContatti(String username, User dati) {
        User user = utente(username);
        user.setNome(dati.getNome());
        user.setCognome(dati.getCognome());
        user.setEmail(dati.getEmail());
        user.setTelefono(dati.getTelefono());
    }

    @Transactional
    public void delete(User user) {
        userRepository.delete(user);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * Per un utente nuovo senza username (accesso facoltativo): genera lo username da nome e cognome, disattiva
     * l'utente e restituisce una password casuale da usare al salvataggio; altrimenti restituisce {@code rawPassword}.
     * L'accesso si attiva poi da Gestione Utenti.
     */
    public String accessoGeneratoSeManca(User user, String rawPassword) {
        if (user.getId() != null || (user.getUsername() != null && !user.getUsername().isBlank())) {
            return rawPassword;
        }
        user.setUsername(usernameLibero(user.getNome(), user.getCognome()));
        user.setEnabled(false);
        return UUID.randomUUID().toString();
    }

    /** Primo username libero ricavato da nome e cognome: "mario.rossi", poi "mario.rossi2", "mario.rossi3"… */
    public String usernameLibero(String nome, String cognome) {
        String base = (semplifica(nome) + "." + semplifica(cognome)).replaceAll("(^\\.|\\.$)", "");
        if (base.isEmpty()) {
            base = "utente";
        }
        String candidato = base;
        for (int n = 2; userRepository.existsByUsername(candidato); n++) {
            candidato = base + n;
        }
        return candidato;
    }

    /** Username come quelli generati da {@link #usernameLibero}: "nome.cognome", con un eventuale numero in fondo. */
    private static final Pattern GENERATO = Pattern.compile("[a-z0-9]+\\.[a-z0-9]+");

    /** Username generato da allineare a nome e cognome attuali: quello di oggi e quello proposto. */
    public record UsernameDaAllineare(User user, String attuale, String proposto) {
    }

    /**
     * Utenti con uno username generato ("nome.cognome", eventualmente con un numero) che non corrisponde più a nome e
     * cognome attuali, con lo username libero proposto; gli username scelti a mano non compaiono. Per username.
     */
    public List<UsernameDaAllineare> usernameDaAllineare() {
        List<UsernameDaAllineare> elenco = new ArrayList<>();
        for (User user : userRepository.findAll()) {
            proposta(user).ifPresent(proposto -> elenco.add(new UsernameDaAllineare(user, user.getUsername(), proposto)));
        }
        elenco.sort(Comparator.comparing(UsernameDaAllineare::attuale));
        return elenco;
    }

    /** Username proposto per l'utente, se il suo è generato e non corrisponde più a nome e cognome. */
    public Optional<String> proposta(User user) {
        String attuale = user.getUsername();
        if (attuale == null || !GENERATO.matcher(attuale).matches()) {
            return Optional.empty();
        }
        String base = (semplifica(user.getNome()) + "." + semplifica(user.getCognome())).replaceAll("(^\\.|\\.$)", "");
        if (base.isEmpty() || !base.contains(".") || attuale.equals(base) || attuale.matches(Pattern.quote(base) + "\\d+")) {
            return Optional.empty();
        }
        return Optional.of(usernameLibero(user.getNome(), user.getCognome()));
    }

    /**
     * Allinea gli username degli utenti indicati (solo quelli ancora da allineare) a nome e cognome, ognuno con uno
     * username libero. Solo l'admin. Restituisce vecchio → nuovo username degli utenti cambiati.
     */
    @Transactional
    public Map<String, String> allineaUsername(Collection<Long> userIds, String admin) {
        if (!utente(admin).getRoles().contains("ADMIN")) {
            throw new IllegalStateException("Solo l'admin può allineare gli username");
        }
        Map<String, String> cambiati = new LinkedHashMap<>();
        for (Long id : userIds) {
            User user = userRepository.findById(id).orElse(null);
            if (user == null) {
                continue;
            }
            proposta(user).ifPresent(nuovo -> {
                cambiati.put(user.getUsername(), nuovo);
                user.setUsername(nuovo);
                // Subito in database: il prossimo usernameLibero vede anche questo
                userRepository.saveAndFlush(user);
            });
        }
        return cambiati;
    }

    /** Minuscolo, senza accenti, solo lettere e cifre. */
    private static String semplifica(String testo) {
        if (testo == null) {
            return "";
        }
        return Normalizer.normalize(testo, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private User utente(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("Utente non trovato: " + username));
    }
}
