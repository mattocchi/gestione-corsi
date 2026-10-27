package it.attocchi.security;

import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import it.attocchi.views.LoginView;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;

import static com.vaadin.flow.spring.security.VaadinSecurityConfigurer.vaadin;

@EnableWebSecurity
@Configuration
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    /** Cookie tecnico impostato dalla casella "Ricordami su questo dispositivo" della pagina di accesso. */
    public static final String COOKIE_RICORDAMI = "ricordami";

    /** Durata dell'accesso ricordato. */
    static final Duration DURATA_RICORDAMI = Duration.ofDays(30);

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * "Ricordami": cookie firmato valido {@link #DURATA_RICORDAMI}, creato al login se è presente il cookie
     * {@link #COOKIE_RICORDAMI} (la casella è fuori dal form nativo di {@code LoginForm}). La firma include la password,
     * quindi un cambio password lo annulla; all'uscita si cancella; un utente disattivato non rientra.
     */
    @Bean
    public TokenBasedRememberMeServices rememberMeServices(UserDetailsService userDetailsService,
                                                           @Value("${app.remember-me.key:}") String chiave) {
        if (chiave.isBlank()) {
            byte[] casuale = new byte[32];
            new SecureRandom().nextBytes(casuale);
            chiave = Base64.getEncoder().encodeToString(casuale);
            log.warn("REMEMBER_ME_KEY non impostata: chiave casuale, gli accessi ricordati non sopravvivono a un riavvio");
        }
        TokenBasedRememberMeServices servizi = new TokenBasedRememberMeServices(chiave, userDetailsService) {
            @Override
            protected boolean rememberMeRequested(HttpServletRequest request, String parameter) {
                Cookie[] cookies = request.getCookies();
                return cookies != null && Arrays.stream(cookies)
                    .anyMatch(c -> COOKIE_RICORDAMI.equals(c.getName()) && "1".equals(c.getValue()));
            }
        };
        servizi.setTokenValiditySeconds((int) DURATA_RICORDAMI.toSeconds());
        servizi.setCookieName("ricordami-accesso");
        return servizi;
    }

    /**
     * Login con username o email ({@link UserDetailsServiceImpl}): lo stato dell'account si controlla solo dopo la
     * password, così il motivo (es. sospeso per inattività, {@link LockedException}) non rivela a chi non conosce la
     * password quali account esistono; con la password sbagliata resta "credenziali errate".
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        AccountStatusUserDetailsChecker stato = new AccountStatusUserDetailsChecker();
        provider.setPreAuthenticationChecks(utente -> { });
        provider.setPostAuthenticationChecks(stato);
        return provider;
    }

    /** Sessioni aperte per utente: servono a chiudere quelle di chi cambia username ({@link SessioniUtenti}). */
    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    /** Avvisa il {@link SessionRegistry} delle sessioni scadute o chiuse dal server. */
    @Bean
    public static HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public SecurityFilterChain vaadinSecurityFilterChain(HttpSecurity http, TokenBasedRememberMeServices rememberMeServices,
                                                         SessionRegistry sessionRegistry) throws Exception {
        http.authorizeHttpRequests(auth -> {
            auth.requestMatchers(HttpMethod.GET, "/styles.css").permitAll();
            auth.requestMatchers(HttpMethod.GET, "/images/*.png").permitAll();
            // Health check per il monitoraggio: senza login solo lo stato (dettagli agli admin, vedi application.properties)
            auth.requestMatchers(HttpMethod.GET, "/actuator/health").permitAll();
        });
        http.rememberMe(ricordami -> ricordami.rememberMeServices(rememberMeServices));
        // Sessioni senza limite di numero, registrate per poterle chiudere; una sessione chiusa torna all'accesso
        http.sessionManagement(sessioni -> sessioni.maximumSessions(-1).sessionRegistry(sessionRegistry).expiredUrl("/login"));
        http.with(vaadin(), vaadin -> vaadin.loginView(LoginView.class));
        return http.build();
    }

}
