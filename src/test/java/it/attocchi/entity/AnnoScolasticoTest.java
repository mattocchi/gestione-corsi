package it.attocchi.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnnoScolasticoTest {

    @Test
    void di_daSettembreEGiaLAnnoSuccessivo() {
        assertEquals("2025/2026", AnnoScolastico.di(LocalDate.of(2026, 8, 31)));
        assertEquals("2026/2027", AnnoScolastico.di(LocalDate.of(2026, 9, 1)));
    }

    @Test
    void valido_soloAAAASlashAAAAConAnniConsecutivi() {
        assertTrue(AnnoScolastico.valido("2026/2027"));
        assertFalse(AnnoScolastico.valido("2026/2028"));
        assertFalse(AnnoScolastico.valido("2026-2027"));
        assertFalse(AnnoScolastico.valido("26/27"));
        assertFalse(AnnoScolastico.valido(" 2026/2027"));
        assertFalse(AnnoScolastico.valido(null));
    }
}
