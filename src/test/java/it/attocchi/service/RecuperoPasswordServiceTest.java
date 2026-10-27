package it.attocchi.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.List;
import it.attocchi.entity.RecuperoPassword;
import it.attocchi.entity.User;
import it.attocchi.repository.RecuperoPasswordRepository;
import it.attocchi.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Recupero password sui dati di DataInitializer: invio simulato (SMTP non configurato nei test). */
@SpringBootTest
class RecuperoPasswordServiceTest {

    @Autowired
    private RecuperoPasswordService recuperoPasswordService;
    @Autowired
    private RecuperoPasswordRepository recuperoPasswordRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private String passwordOriginale;

    @BeforeEach
    void salvaPassword() {
        passwordOriginale = utente().getPassword();
    }

    @AfterEach
    void ripristina() {
        User user = utente();
        user.setPassword(passwordOriginale);
        user.setEnabled(true);
        user.setUltimoAccesso(null);
        user.setInvitatoIl(null);
        userRepository.save(user);
        recuperoPasswordRepository.deleteAll();
    }

    @Test
    void invito_soloStaffEAccountMaiEntratoConEmail() {
        User user = utente();
        user.setEnabled(false);
        userRepository.save(user);
        assertThrows(IllegalStateException.class, () -> recuperoPasswordService.invita(user.getId(), "insegnante1"));

        recuperoPasswordService.invita(user.getId(), "segreteria");
        RecuperoPassword link = recuperoPasswordRepository.findAll().getFirst();
        assertTrue(link.isInvito());
        assertTrue(link.getScadenza().isAfter(LocalDateTime.now().plusDays(6)));
        assertNotNull(utente().getInvitatoIl());

        // Già entrato: non si invita più (usa il login o il recupero)
        User entrato = utente();
        entrato.setUltimoAccesso(LocalDateTime.now());
        userRepository.save(entrato);
        assertFalse(RecuperoPasswordService.invitabile(entrato));
        assertThrows(IllegalStateException.class, () -> recuperoPasswordService.invita(entrato.getId(), "admin"));
        assertEquals(new RecuperoPasswordService.EsitoInviti(0, 1),
            recuperoPasswordService.invitaTutti(List.of(entrato.getId()), "admin"));
    }

    @Test
    void invito_impostataLaPasswordLAccountSiAttiva() {
        User user = utente();
        user.setEnabled(false);
        userRepository.save(user);
        String token = "invito-di-prova";
        recuperoPasswordRepository.save(new RecuperoPassword(user, RecuperoPasswordService.hash(token),
            LocalDateTime.now().plusDays(7), true));
        assertTrue(recuperoPasswordService.isInvito(token));
        assertFalse(recuperoPasswordService.isInvito("altro"));

        assertEquals("insegnante2", recuperoPasswordService.reimposta(token, "nuovaPassword1"));
        assertTrue(utente().isEnabled());
        assertTrue(passwordEncoder.matches("nuovaPassword1", utente().getPassword()));
        assertEquals(0, recuperoPasswordRepository.count());
    }

    @Test
    void richiesta_perUsernameOEmail_unSoloLinkValidoPerAccount() {
        assertEquals(0, recuperoPasswordService.richiedi(" "));
        assertEquals(0, recuperoPasswordService.richiedi("nessuno"));
        assertEquals(1, recuperoPasswordService.richiedi("insegnante2"));
        assertEquals(1, recuperoPasswordService.richiedi("INSEGNANTE2@example.com"));
        // La seconda richiesta annulla il primo link
        assertEquals(1, recuperoPasswordRepository.count());
        assertEquals(36, recuperoPasswordService.validitaOre());
    }

    @Test
    void accountDisattivatoDopoLUso_nessunLinkENessunLinkValido() {
        String token = link(LocalDateTime.now().plusHours(1));
        User user = utente();
        user.setEnabled(false);
        user.setUltimoAccesso(LocalDateTime.now().minusDays(3));
        userRepository.save(user);

        assertEquals(0, recuperoPasswordService.richiedi("insegnante2"));
        assertEquals(Optional.empty(), recuperoPasswordService.verifica(token));
    }

    @Test
    void accountDisattivatoMaiEntrato_siAttivaConLaNuovaPassword() {
        User user = utente();
        user.setEnabled(false);
        user.setUltimoAccesso(null);
        userRepository.save(user);

        // Con l'email riceve il link e impostando la password si attiva
        assertEquals(1, recuperoPasswordService.richiedi("insegnante2@example.com"));
        String token = link(LocalDateTime.now().plusHours(1));
        assertEquals(Optional.of("insegnante2"), recuperoPasswordService.verifica(token));
        recuperoPasswordService.reimposta(token, "nuovaPassword1");
        assertTrue(utente().isEnabled());
        assertTrue(passwordEncoder.matches("nuovaPassword1", utente().getPassword()));
    }

    @Test
    void reimposta_conLinkValido_unaSolaVolta() {
        String token = link(LocalDateTime.now().plusHours(1));
        assertEquals(Optional.of("insegnante2"), recuperoPasswordService.verifica(token));
        assertThrows(IllegalArgumentException.class, () -> recuperoPasswordService.reimposta(token, "corta"));

        assertEquals("insegnante2", recuperoPasswordService.reimposta(token, "nuovaPassword1"));
        assertTrue(passwordEncoder.matches("nuovaPassword1", utente().getPassword()));
        assertEquals(Optional.empty(), recuperoPasswordService.verifica(token));
        assertThrows(IllegalStateException.class, () -> recuperoPasswordService.reimposta(token, "altraPassword2"));
    }

    @Test
    void linkScadutoOInventato_nonValido() {
        String scaduto = link(LocalDateTime.now().minusMinutes(1));
        assertEquals(Optional.empty(), recuperoPasswordService.verifica(scaduto));
        assertEquals(Optional.empty(), recuperoPasswordService.verifica("inventato"));
        assertEquals(Optional.empty(), recuperoPasswordService.verifica(null));
    }

    private String link(LocalDateTime scadenza) {
        String token = "token-di-prova-" + scadenza;
        recuperoPasswordRepository.save(new RecuperoPassword(utente(), RecuperoPasswordService.hash(token), scadenza));
        return token;
    }

    private User utente() {
        return userRepository.findByUsername("insegnante2").orElseThrow();
    }
}
