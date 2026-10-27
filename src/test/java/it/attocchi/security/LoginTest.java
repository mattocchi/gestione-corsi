package it.attocchi.security;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import it.attocchi.entity.User;
import it.attocchi.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Accesso con username o email sui dati di DataInitializer, con lo stesso AuthenticationManager del form di login. */
@SpringBootTest
class LoginTest {

    private static final String EMAIL = "insegnante1@example.com";

    @Autowired
    private AuthenticationConfiguration authenticationConfiguration;
    @Autowired
    private UserRepository userRepository;

    private String emailOriginaleAllievo;

    @AfterEach
    void ripristina() {
        if (emailOriginaleAllievo != null) {
            User allievo = allievo();
            allievo.setEmail(emailOriginaleAllievo);
            allievo.setEnabled(true);
            userRepository.save(allievo);
        }
    }

    @Test
    void accesso_conUsernameOEmail_ilNomeERestaLoUsername() throws Exception {
        assertEquals("insegnante1", accedi("insegnante1", "insegnante1").getName());
        assertEquals("insegnante1", accedi(EMAIL, "insegnante1").getName());
        assertEquals("insegnante1", accedi(" INSEGNANTE1@Example.com ", "insegnante1").getName());
        assertThrows(BadCredentialsException.class, () -> accedi(EMAIL, "sbagliata"));
        assertThrows(BadCredentialsException.class, () -> accedi("nessuno@example.com", "insegnante1"));
    }

    @Test
    void accesso_registraLUltimoAccesso() throws Exception {
        LocalDateTime prima = LocalDateTime.now().minusSeconds(1);
        accedi(EMAIL, "insegnante1");
        LocalDateTime ultimo = userRepository.findByUsername("insegnante1").orElseThrow().getUltimoAccesso();
        assertTrue(ultimo != null && ultimo.isAfter(prima));
        // Un tentativo fallito non lo cambia
        assertThrows(BadCredentialsException.class, () -> accedi("insegnante1", "sbagliata"));
        assertEquals(ultimo, userRepository.findByUsername("insegnante1").orElseThrow().getUltimoAccesso());
    }

    @Test
    void accesso_conEmailCondivisa_soloSeUnUnicoAccountAttivo() throws Exception {
        User allievo = allievo();
        emailOriginaleAllievo = allievo.getEmail();
        allievo.setEmail(EMAIL);
        userRepository.save(allievo);
        assertThrows(BadCredentialsException.class, () -> accedi(EMAIL, "insegnante1"));
        assertEquals("insegnante1", accedi("insegnante1", "insegnante1").getName());

        allievo.setEnabled(false);
        userRepository.save(allievo);
        assertEquals("insegnante1", accedi(EMAIL, "insegnante1").getName());
    }

    @Test
    void accesso_sospesoPerInattivita_sololaPasswordGiustaLoDice() throws Exception {
        User insegnante = userRepository.findByUsername("insegnante2").orElseThrow();
        insegnante.setEnabled(false);
        insegnante.setSospesoPerInattivita(true);
        userRepository.save(insegnante);
        try {
            assertThrows(LockedException.class, () -> accedi("insegnante2", "insegnante2"));
            assertThrows(LockedException.class, () -> accedi("insegnante2@example.com", "insegnante2"));
            // Con la password sbagliata non si scopre nulla
            assertThrows(BadCredentialsException.class, () -> accedi("insegnante2", "sbagliata"));
            // Disattivato a mano: solo "credenziali errate" o disattivato, mai "sospeso"
            insegnante.setSospesoPerInattivita(false);
            userRepository.save(insegnante);
            assertThrows(DisabledException.class, () -> accedi("insegnante2", "insegnante2"));
            assertThrows(BadCredentialsException.class, () -> accedi("insegnante2", "sbagliata"));
        } finally {
            insegnante.setEnabled(true);
            insegnante.setSospesoPerInattivita(false);
            userRepository.save(insegnante);
        }
    }

    private Authentication accedi(String usernameOEmail, String password) throws Exception {
        AuthenticationManager manager = authenticationConfiguration.getAuthenticationManager();
        return manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(usernameOEmail, password));
    }

    private User allievo() {
        return userRepository.findByUsername("allievo_canto1").orElseThrow();
    }
}
