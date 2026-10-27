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
    @Autowired
    private UserService userService;

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
        user.setSospesoPerInattivita(false);
        userRepository.save(user);
        recuperoPasswordRepository.deleteAll();
    }

    @Test
    void inattivi_sospesiDopoSeiMesiTranneAdminERiattivatiDalRecupero() {
        LocalDateTime adesso = LocalDateTime.now();
        User user = utente();
        user.setUltimoAccesso(adesso.minusMonths(7));
        userRepository.save(user);
        User admin = userRepository.findByUsername("admin").orElseThrow();
        LocalDateTime accessoAdmin = admin.getUltimoAccesso();
        admin.setUltimoAccesso(adesso.minusYears(1));
        userRepository.save(admin);
        try {
            // Gli altri non sono mai entrati (ultimo accesso vuoto): restano attivi
            assertEquals(List.of("insegnante2"), userService.sospendiInattivi(adesso));
            User sospeso = utente();
            assertFalse(sospeso.isEnabled());
            assertTrue(sospeso.isSospesoPerInattivita());
            assertTrue(userRepository.findByUsername("admin").orElseThrow().isEnabled());

            // Si riattiva da solo con il recupero password
            assertEquals(1, recuperoPasswordService.richiedi("insegnante2"));
            String token = link(adesso.plusHours(1));
            recuperoPasswordService.reimposta(token, "nuovaPassword1");
            assertTrue(utente().isEnabled());
            assertFalse(utente().isSospesoPerInattivita());
        } finally {
            User ripristino = userRepository.findByUsername("admin").orElseThrow();
            ripristino.setUltimoAccesso(accessoAdmin);
            userRepository.save(ripristino);
        }
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

        // Già entrato: la segreteria non lo invita più (l'admin ne reimposta l'accesso); gli inviti multipli lo saltano
        User entrato = utente();
        entrato.setUltimoAccesso(LocalDateTime.now());
        userRepository.save(entrato);
        assertFalse(RecuperoPasswordService.invitabile(entrato));
        assertThrows(IllegalStateException.class, () -> recuperoPasswordService.invita(entrato.getId(), "segreteria"));
        assertEquals(new RecuperoPasswordService.EsitoInviti(0, 1),
            recuperoPasswordService.invitaTutti(List.of(entrato.getId()), "admin"));
    }

    @Test
    void reimpostazione_soloAdminBloccaLAccountFinoAlNuovoAccesso() {
        User user = utente();
        user.setUltimoAccesso(LocalDateTime.now().minusDays(2));
        userRepository.save(user);
        User entrato = utente();
        assertEquals(Optional.empty(), recuperoPasswordService.tipoInvito(entrato, "segreteria"));
        assertEquals(Optional.of(RecuperoPasswordService.TipoInvito.REIMPOSTAZIONE),
            recuperoPasswordService.tipoInvito(entrato, "admin"));
        // Non sul proprio utente
        assertEquals(Optional.empty(), recuperoPasswordService.tipoInvito(
            userRepository.findByUsername("admin").orElseThrow(), "admin"));
        assertThrows(IllegalStateException.class, () -> recuperoPasswordService.invita(entrato.getId(), "segreteria"));

        var anteprima = recuperoPasswordService.anteprimaInvito(entrato.getId(), "admin");
        assertEquals("Il tuo accesso all'Area Corsi è stato reimpostato", anteprima.oggetto());
        assertTrue(utente().isEnabled(), "L'anteprima non cambia l'account");

        assertEquals(RecuperoPasswordService.TipoInvito.REIMPOSTAZIONE, recuperoPasswordService.invita(entrato.getId(), "admin"));
        User bloccato = utente();
        assertFalse(bloccato.isEnabled());
        assertFalse(passwordEncoder.matches("insegnante2", bloccato.getPassword()));
        RecuperoPassword link = recuperoPasswordRepository.findAll().getFirst();
        assertTrue(link.isInvito());

        // Il link d'invito vale anche se l'account è disattivato dopo l'uso: riattiva con la nuova password
        String token = "reimpostazione-di-prova";
        recuperoPasswordRepository.deleteAll();
        recuperoPasswordRepository.save(new RecuperoPassword(bloccato, RecuperoPasswordService.hash(token),
            LocalDateTime.now().plusDays(7), true));
        assertEquals(Optional.of("insegnante2"), recuperoPasswordService.verifica(token));
        recuperoPasswordService.reimposta(token, "nuovaPassword1");
        assertTrue(utente().isEnabled());
    }

    @Test
    void disattivazioneManuale_annullaILinkAperti() {
        recuperoPasswordService.invita(utente().getId(), "admin");
        assertEquals(1, recuperoPasswordRepository.count());
        User user = utente();
        user.setEnabled(false);
        userService.save(user, null);
        assertEquals(0, recuperoPasswordRepository.count());
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
