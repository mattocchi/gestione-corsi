package it.attocchi.service;

import it.attocchi.config.RecuperoPasswordProperties;
import it.attocchi.entity.InvioEmail.Esito;
import it.attocchi.entity.RecuperoPassword;
import it.attocchi.entity.User;
import it.attocchi.repository.RecuperoPasswordRepository;
import it.attocchi.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * Recupero password via email. Il link contiene un token casuale di 256 bit; in database c'è solo il suo hash SHA-256.
 * Il link vale {@link RecuperoPasswordProperties#validitaOre()} ore e una sola volta; una nuova richiesta annulla i link
 * precedenti dello stesso account. Per non rivelare quali account esistono, {@link #richiedi} non dice mai se ha inviato
 * qualcosa: account inesistenti o senza email non ricevono nulla, e nemmeno quelli disattivati dopo aver usato l'app.
 * Un account disattivato che non è mai entrato (es. creato con username generato e password casuale) riceve il link
 * e si attiva impostando la nuova password.
 */
@Service
@Transactional(readOnly = true)
public class RecuperoPasswordService {

    private static final Logger log = LoggerFactory.getLogger(RecuperoPasswordService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RecuperoPasswordRepository recuperoPasswordRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final DocumentiService documentiService;
    private final RecuperoPasswordProperties proprieta;
    private final Environment environment;

    public RecuperoPasswordService(UserRepository userRepository, RecuperoPasswordRepository recuperoPasswordRepository,
                                   PasswordEncoder passwordEncoder, EmailService emailService,
                                   DocumentiService documentiService, RecuperoPasswordProperties proprieta,
                                   Environment environment) {
        this.userRepository = userRepository;
        this.recuperoPasswordRepository = recuperoPasswordRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.documentiService = documentiService;
        this.proprieta = proprieta;
        this.environment = environment;
        if (environment.acceptsProfiles(Profiles.of("prod")) && proprieta.url().contains("localhost")) {
            log.warn("APP_URL non impostata: i link di recupero password punterebbero a {}", proprieta.url());
        }
    }

    public int validitaOre() {
        return proprieta.validitaOre();
    }

    /**
     * Invia un link per una nuova password a ogni account con email che corrisponde a {@code usernameOEmail}, attivo
     * o disattivato ma mai entrato ({@link #puoRecuperare})
     * (lo username, oppure l'email di uno o più account). Restituisce il numero di link creati, solo per i test:
     * l'interfaccia mostra sempre lo stesso messaggio.
     */
    @Transactional
    public int richiedi(String usernameOEmail) {
        String valore = usernameOEmail == null ? "" : usernameOEmail.strip();
        if (valore.isEmpty()) {
            return 0;
        }
        List<User> account = userRepository.findByUsername(valore).map(List::of)
            .orElseGet(() -> userRepository.findByEmailIgnoreCase(valore));
        int inviati = 0;
        for (User user : account) {
            if (!puoRecuperare(user) || user.getEmail() == null || user.getEmail().isBlank()) {
                log.info("Recupero password non inviato per {}: account disattivato dopo l'uso o senza email",
                    user.getUsername());
                continue;
            }
            String token = nuovoToken();
            recuperoPasswordRepository.deleteByUserId(user.getId());
            recuperoPasswordRepository.save(new RecuperoPassword(user, hash(token),
                LocalDateTime.now().plusHours(proprieta.validitaOre())));
            String link = proprieta.linkNuovaPassword(token);
            String corpo = documentiService.emailRecuperoPasswordHtml(saluto(user), user.getUsername(), link,
                proprieta.validitaOre());
            var esito = emailService.inviaMessaggio(user.getEmail(), "Reimpostazione della password", corpo);
            if (esito == Esito.SIMULATA && environment.acceptsProfiles(Profiles.of("dev"))) {
                // Solo in sviluppo senza SMTP: il link nel log permette di provare la procedura
                log.info("Link di recupero password per {} (solo dev): {}", user.getUsername(), link);
            }
            inviati++;
        }
        return inviati;
    }

    /** Username dell'account del link, se il link è valido (esiste, non scaduto, account recuperabile). */
    public Optional<String> verifica(String token) {
        return valido(token).map(recupero -> recupero.getUser().getUsername());
    }

    /**
     * Imposta la nuova password dell'account del link e annulla tutti i suoi link; un account disattivato (mai
     * entrato) diventa attivo. Avvisa l'utente via email. Restituisce lo username.
     */
    @Transactional
    public String reimposta(String token, String nuovaPassword) {
        if (nuovaPassword == null || nuovaPassword.length() < UserService.PASSWORD_MINIMA) {
            throw new IllegalArgumentException("La password deve avere almeno " + UserService.PASSWORD_MINIMA + " caratteri");
        }
        RecuperoPassword recupero = valido(token)
            .orElseThrow(() -> new IllegalStateException("Il link non è valido o è scaduto: chiedine uno nuovo"));
        User user = recupero.getUser();
        user.setPassword(passwordEncoder.encode(nuovaPassword));
        if (!user.isEnabled()) {
            user.setEnabled(true);
            log.info("Account {} attivato con il link di recupero password", user.getUsername());
        }
        recuperoPasswordRepository.deleteByUserId(user.getId());
        log.info("Password reimpostata con il link di recupero per {}", user.getUsername());
        emailService.inviaMessaggio(user.getEmail(), "La tua password è stata cambiata",
            documentiService.emailPasswordCambiataHtml(saluto(user), user.getUsername()));
        return user.getUsername();
    }

    private Optional<RecuperoPassword> valido(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return recuperoPasswordRepository.findByTokenHash(hash(token))
            .filter(recupero -> recupero.getScadenza().isAfter(LocalDateTime.now()))
            .filter(recupero -> puoRecuperare(recupero.getUser()));
    }

    /**
     * Un account può usare il recupero password se è attivo, oppure se è disattivato ma non è mai entrato (creato
     * bloccato): chi è stato disattivato dopo aver usato l'app resta bloccato.
     */
    static boolean puoRecuperare(User user) {
        return user.isEnabled() || user.getUltimoAccesso() == null;
    }

    private static String saluto(User user) {
        String nome = ((user.getNome() != null ? user.getNome() : "") + " "
            + (user.getCognome() != null ? user.getCognome() : "")).strip();
        return nome.isEmpty() ? user.getUsername() : nome;
    }

    private static String nuovoToken() {
        byte[] byteCasuali = new byte[32];
        RANDOM.nextBytes(byteCasuali);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(byteCasuali);
    }

    /** Hash SHA-256 (esadecimale) del token, come è salvato in database. */
    public static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
