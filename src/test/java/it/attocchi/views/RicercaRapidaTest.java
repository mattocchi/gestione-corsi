package it.attocchi.views;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RicercaRapidaTest {

    @Test
    void corrisponde_senzaMaiuscoleNeAccentiConTutteLeParole() {
        String testo = RicercaRapida.testo("Niccolò", "Rossi", "niccolo.rossi@example.com", null, "Via Roma 1");
        assertEquals("Niccolò Rossi niccolo.rossi@example.com Via Roma 1", testo);
        assertTrue(RicercaRapida.corrisponde("", testo));
        assertTrue(RicercaRapida.corrisponde("  ", testo));
        assertTrue(RicercaRapida.corrisponde("NICCOLO", testo));
        assertTrue(RicercaRapida.corrisponde("rossi niccolò", testo));
        assertTrue(RicercaRapida.corrisponde("roma  ross", testo));
        assertFalse(RicercaRapida.corrisponde("rossi milano", testo));
        assertFalse(RicercaRapida.corrisponde("bianchi", null));
    }
}
