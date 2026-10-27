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

import java.util.Set;
import java.util.Collection;
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

    /** Giorni di validità del link d'invito. */
    public static final int VALIDITA_INVITO_GIORNI = 7;

    /** Esito di un invio multiplo di inviti: invitati e saltati (senza email o già entrati). */
    public record EsitoInviti(int invitati, int saltati) {
    }

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

    /**
     * Un account si può invitare se ha un'email e non è mai entrato (es. creato con username generato e accesso
     * disattivato): chi è già entrato usa il login o «Password dimenticata?».
     */
    public static boolean invitabile(User user) {
        return user.getUltimoAccesso() == null && user.getEmail() != null && !user.getEmail().isBlank();
    }

    /** Invito per chi non è mai entrato, o reimpostazione dell'accesso (solo admin) per chi è già entrato. */
    public enum TipoInvito {
        INVITO, REIMPOSTAZIONE
    }

    /** Email d'invito come verrebbe inviata (con un link d'esempio), senza inviarla né cambiare l'account. */
    public record AnteprimaInvito(TipoInvito tipo, String destinatario, String oggetto, String corpoHtml) {
    }

    /**
     * Cosa può fare l'utente {@code username} su un altro account con email: invitarlo (admin e segreteria, se non è mai
     * entrato) o reimpostarne l'accesso (solo admin, se è già entrato); vuoto altrimenti e sul proprio account.
     */
    public Optional<TipoInvito> tipoInvito(User user, String username) {
        Set<String> ruoli = userRepository.findByUsername(username).map(User::getRoles).orElse(Set.of());
        // Mai sul proprio account (si cambia la password dal profilo)
        if (user.getEmail() == null || user.getEmail().isBlank() || user.getUsername().equals(username)) {
            return Optional.empty();
        }
        if (invitabile(user)) {
            return ruoli.contains("ADMIN") || ruoli.contains("SEGRETERIA") ? Optional.of(TipoInvito.INVITO) : Optional.empty();
        }
        return ruoli.contains("ADMIN") ? Optional.of(TipoInvito.REIMPOSTAZIONE) : Optional.empty();
    }

    /** Anteprima dell'email di {@link #invita}, con gli stessi controlli. */
    public AnteprimaInvito anteprimaInvito(Long userId, String username) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("Utente non trovato"));
        TipoInvito tipo = verificaInvito(user, username);
        return new AnteprimaInvito(tipo, user.getEmail(), oggettoInvito(tipo),
            corpoInvito(user, tipo, proprieta.linkNuovaPassword("…")));
    }

    /**
     * Invita l'utente ad accedere: email con il link per impostare la password, valido {@link #VALIDITA_INVITO_GIORNI}
     * giorni (annulla i link precedenti); impostata la password l'account si attiva e l'utente entra subito. Per chi non
     * è mai entrato admin e segreteria; per chi è già entrato è una reimpostazione, solo dell'admin e non sul proprio
     * account: l'account si blocca subito (disattivato, password vecchia non più valida, anche per il "Ricordami") fino
     * al nuovo accesso dal link; le sessioni aperte le chiude chi chiama. Altrimenti {@link IllegalStateException}.
     */
    @Transactional
    public TipoInvito invita(Long userId, String username) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("Utente non trovato"));
        TipoInvito tipo = verificaInvito(user, username);
        if (tipo == TipoInvito.REIMPOSTAZIONE) {
            user.setEnabled(false);
            user.setSospesoPerInattivita(false);
            user.setPassword(passwordEncoder.encode(nuovoToken()));
            log.info("Accesso di {} reimpostato da {}", user.getUsername(), username);
        }
        String token = nuovoToken();
        recuperoPasswordRepository.deleteByUserId(user.getId());
        recuperoPasswordRepository.save(new RecuperoPassword(user, hash(token),
            LocalDateTime.now().plusDays(VALIDITA_INVITO_GIORNI), true));
        user.setInvitatoIl(LocalDateTime.now());
        String link = proprieta.linkNuovaPassword(token);
        var esito = emailService.inviaMessaggio(user.getEmail(), oggettoInvito(tipo), corpoInvito(user, tipo, link));
        if (esito == Esito.ERRORE) {
            throw new IllegalStateException("Invio dell'email non riuscito per " + user.getUsername());
        }
        if (esito == Esito.SIMULATA && environment.acceptsProfiles(Profiles.of("dev"))) {
            log.info("Link d'invito per {} (solo dev): {}", user.getUsername(), link);
        }
        return tipo;
    }

    private TipoInvito verificaInvito(User user, String username) {
        return tipoInvito(user, username).orElseThrow(() -> new IllegalStateException(
            user.getEmail() == null || user.getEmail().isBlank() ? user.getUsername() + " non ha un indirizzo email"
                : user.getUltimoAccesso() != null ? "Solo l'admin può reimpostare l'accesso di chi è già entrato (non il proprio)"
                : "Gli inviti li mandano admin e segreteria"));
    }

    private static String oggettoInvito(TipoInvito tipo) {
        return tipo == TipoInvito.REIMPOSTAZIONE ? "Il tuo accesso all'Area Corsi è stato reimpostato"
            : "Invito ad accedere all'Area Corsi";
    }

    private String corpoInvito(User user, TipoInvito tipo, String link) {
        return documentiService.emailInvitoHtml(saluto(user), user.getUsername(), link, VALIDITA_INVITO_GIORNI,
            tipo == TipoInvito.REIMPOSTAZIONE);
    }

    /** Invita ogni utente {@link #invitabile} tra quelli indicati; gli altri sono saltati. */
    @Transactional
    public EsitoInviti invitaTutti(Collection<Long> userIds, String username) {
        int invitati = 0;
        int saltati = 0;
        for (Long id : userIds) {
            if (userRepository.findById(id).filter(RecuperoPasswordService::invitabile).isEmpty()) {
                saltati++;
                continue;
            }
            try {
                invita(id, username);
                invitati++;
            } catch (IllegalStateException ex) {
                // Email non partita: gli altri inviti proseguono
                log.warn("Invito non inviato: {}", ex.getMessage());
                saltati++;
            }
        }
        return new EsitoInviti(invitati, saltati);
    }

    /** {@code true} se il link è valido ed è un invito: impostata la password si entra subito. */
    public boolean isInvito(String token) {
        return valido(token).map(RecuperoPassword::isInvito).orElse(false);
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
            user.setSospesoPerInattivita(false);
            log.info("Account {} attivato con il link di recupero password", user.getUsername());
        }
        recuperoPasswordRepository.deleteByUserId(user.getId());
        if (recupero.isInvito()) {
            // Primo accesso dall'invito: nessun avviso di "password cambiata"
            log.info("Password impostata con l'invito per {}", user.getUsername());
            return user.getUsername();
        }
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
            // Un invito lo emette lo staff: vale anche per un account disattivato dopo l'uso (reimpostazione)
            .filter(recupero -> recupero.isInvito() || puoRecuperare(recupero.getUser()));
    }

    /**
     * Un account può usare il recupero password se è attivo, se è disattivato ma non è mai entrato (creato
     * bloccato) o se è sospeso per inattività: chi è stato disattivato a mano dopo aver usato l'app resta bloccato.
     */
    static boolean puoRecuperare(User user) {
        return user.isEnabled() || user.getUltimoAccesso() == null || user.isSospesoPerInattivita();
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
