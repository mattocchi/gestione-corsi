package it.attocchi.service;

import it.attocchi.entity.User;
import it.attocchi.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class UserService {

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
        if (rawPassword != null && !rawPassword.isBlank()) {
            user.setPassword(passwordEncoder.encode(rawPassword));
        } else if (user.getId() == null) {
            throw new IllegalArgumentException("La password è obbligatoria per un nuovo utente");
        }
        return userRepository.save(user);
    }

    /** Cambia la password dell'utente indicato (profilo personale). */
    @Transactional
    public void cambiaPassword(String username, String nuovaPassword) {
        if (nuovaPassword == null || nuovaPassword.isBlank()) {
            throw new IllegalArgumentException("La nuova password è obbligatoria");
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
