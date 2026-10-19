package it.attocchi.service;

import it.attocchi.entity.Allievo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Inserimento rapido e omonimi sui dati di DataInitializer. */
@SpringBootTest
class AllievoServiceTest {

    @Autowired
    private AllievoService allievoService;

    @Autowired
    private UserService userService;

    @Test
    void creaRapido_conNomeCognomeEContattiFacoltativi() {
        Allievo nuovo = allievoService.creaRapido(" Giulia ", "Bianchi", "giulia@example.com", " ");
        try {
            assertEquals("Giulia Bianchi", nuovo.getNomeCompleto());
            assertEquals("giulia@example.com", nuovo.getUser().getEmail());
            assertNull(nuovo.getUser().getTelefono());
            assertNull(nuovo.getGenitore());
            // Utente generato e disattivato, con il ruolo ALLIEVO
            assertEquals("giulia.bianchi", nuovo.getUser().getUsername());
            assertFalse(nuovo.getUser().isEnabled());
            assertTrue(nuovo.getUser().getRoles().contains("ALLIEVO"));
            assertEquals(1, allievoService.omonimi("GIULIA", " bianchi").size());
        } finally {
            allievoService.delete(nuovo);
            userService.delete(nuovo.getUser());
        }
        assertThrows(IllegalArgumentException.class, () -> allievoService.creaRapido("Solo", " ", null, null));
    }

    @Test
    void omonimi_soloStessoNomeECognome() {
        assertEquals(1, allievoService.omonimi("allievo", "canto1").size());
        assertTrue(allievoService.omonimi("Allievo", "Canto").isEmpty());
    }
}
