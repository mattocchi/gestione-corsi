package it.attocchi.service;

import it.attocchi.entity.Genitore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Inserimento rapido e omonimi dei genitori sui dati di DataInitializer. */
@SpringBootTest
class GenitoreServiceTest {

    @Autowired
    private GenitoreService genitoreService;

    @Autowired
    private UserService userService;

    @Test
    void creaRapido_conNomeCognomeEContattiFacoltativi() {
        Genitore nuovo = genitoreService.creaRapido(" Anna ", "Verdi", " ", "333 1234567");
        try {
            assertEquals("Anna Verdi", nuovo.getNomeCompleto());
            assertNull(nuovo.getUser().getEmail());
            assertEquals("333 1234567", nuovo.getUser().getTelefono());
            // Utente generato e disattivato, con il ruolo GENITORE
            assertEquals("anna.verdi", nuovo.getUser().getUsername());
            assertFalse(nuovo.getUser().isEnabled());
            assertTrue(nuovo.getUser().getRoles().contains("GENITORE"));
            assertEquals(1, genitoreService.omonimi("ANNA", " verdi").size());
        } finally {
            genitoreService.elimina(nuovo.getId());
            userService.delete(userService.findByUsername("anna.verdi"));
        }
        assertThrows(IllegalArgumentException.class, () -> genitoreService.creaRapido(" ", "Verdi", null, null));
    }

    @Test
    void omonimi_soloStessoNomeECognome() {
        assertEquals(1, genitoreService.omonimi("genitore1", "cognome1").size());
        assertTrue(genitoreService.omonimi("Genitore1", "Cognome").isEmpty());
    }
}
