package it.attocchi.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodiceFiscaleTest {

    @Test
    void codiciValidi() {
        assertTrue(CodiceFiscale.isValido("MRTMTT25D09F205Z"));
        assertTrue(CodiceFiscale.isValido("RSSMRA85T10A562S"));
        // Minuscole e spazi sono ammessi
        assertTrue(CodiceFiscale.isValido(" rssmra85t10a562s "));
        // Omocodia: cifre sostituite da lettere
        assertTrue(CodiceFiscale.isValido("RSSMRA85T10A56NH"));
    }

    @Test
    void codiciNonValidi() {
        assertFalse(CodiceFiscale.isValido(null));
        assertFalse(CodiceFiscale.isValido(""));
        // Carattere di controllo sbagliato
        assertFalse(CodiceFiscale.isValido("RSSMRA85T10A562T"));
        // Mese inesistente (lettera F)
        assertFalse(CodiceFiscale.isValido("RSSMRA85F10A562S"));
        // Lunghezza errata
        assertFalse(CodiceFiscale.isValido("RSSMRA85T10A562"));
    }

    @Test
    void normalizza() {
        assertEquals("RSSMRA85T10A562S", CodiceFiscale.normalizza(" rssmra 85t10a562s"));
        assertNull(CodiceFiscale.normalizza("  "));
    }

    @Test
    void indirizzoFormattato() {
        assertEquals("Via Roma 1, 00184 Roma (RM)", new Indirizzo("Via Roma 1", "00184", "Roma", "RM").getFormattato());
        assertEquals("Roma", new Indirizzo(null, "", "Roma", null).getFormattato());
        assertTrue(new Indirizzo(" ", null, null, "").isVuoto());
        assertFalse(new Indirizzo(null, null, "Roma", null).isVuoto());
    }
}
