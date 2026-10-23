package it.attocchi.security;

import it.attocchi.entity.User;
import it.attocchi.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** "Ricordami" sul filtro di login reale di Spring Security (dati di DataInitializer). */
@SpringBootTest
class RicordamiTest {

    private static final String COOKIE_ACCESSO = "ricordami-accesso";

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private TokenBasedRememberMeServices rememberMeServices;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mvc;
    private String passwordOriginale;

    @BeforeEach
    void prepara() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        passwordOriginale = utente().getPassword();
    }

    @AfterEach
    void ripristina() {
        User user = utente();
        user.setPassword(passwordOriginale);
        user.setEnabled(true);
        userRepository.save(user);
    }

    @Test
    void senzaLaCasella_nessunCookie() throws Exception {
        MockHttpServletResponse risposta = login(false);
        assertNull(risposta.getCookie(COOKIE_ACCESSO));
    }

    @Test
    void conLaCasella_cookieDi30GiorniCheRientra() throws Exception {
        Cookie ricordato = login(true).getCookie(COOKIE_ACCESSO);
        assertNotNull(ricordato);
        assertEquals(30 * 24 * 3600, ricordato.getMaxAge());
        assertEquals("insegnante2", rientro(ricordato));
    }

    @Test
    void cambioPasswordOUtenteDisattivato_nonRientra() throws Exception {
        Cookie ricordato = login(true).getCookie(COOKIE_ACCESSO);

        User user = utente();
        user.setPassword(passwordEncoder.encode("altraPassword1"));
        userRepository.save(user);
        assertNull(rientro(ricordato));

        user.setPassword(passwordOriginale);
        user.setEnabled(false);
        userRepository.save(user);
        assertNull(rientro(ricordato));
    }

    @Test
    void uscita_cancellaIlCookie() throws Exception {
        Cookie ricordato = login(true).getCookie(COOKIE_ACCESSO);
        Cookie dopoUscita = mvc.perform(post("/logout").cookie(ricordato).with(csrf())).andReturn().getResponse()
            .getCookie(COOKIE_ACCESSO);
        assertNotNull(dopoUscita);
        assertEquals(0, dopoUscita.getMaxAge());
    }

    private MockHttpServletResponse login(boolean ricordami) throws Exception {
        var richiesta = post("/login").param("username", "insegnante2").param("password", "insegnante2").with(csrf());
        if (ricordami) {
            richiesta.cookie(new Cookie(SecurityConfig.COOKIE_RICORDAMI, "1"));
        }
        return mvc.perform(richiesta).andReturn().getResponse();
    }

    /** Username con cui rientra il cookie ricordato in una nuova richiesta senza sessione, o {@code null}. */
    private String rientro(Cookie ricordato) {
        MockHttpServletRequest richiesta = new MockHttpServletRequest();
        richiesta.setCookies(ricordato);
        var autenticazione = rememberMeServices.autoLogin(richiesta, new MockHttpServletResponse());
        return autenticazione != null ? autenticazione.getName() : null;
    }

    private User utente() {
        return userRepository.findByUsername("insegnante2").orElseThrow();
    }
}
