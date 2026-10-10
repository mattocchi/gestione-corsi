package it.attocchi.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnnoScolasticoTest {

    @Test
    void di_daSettembreEGiaLAnnoSuccessivo() {
        assertEquals("2025/2026", AnnoScolastico.di(LocalDate.of(2026, 8, 31)));
        assertEquals("2026/2027", AnnoScolastico.di(LocalDate.of(2026, 9, 1)));
    }

    @Test
    void elenco_annoPrecedenteCorrenteSuccessivoPiuValoreAttuale() {
        LocalDate oggi = LocalDate.of(2026, 10, 10);
        assertEquals(List.of("2025/2026", "2026/2027", "2027/2028"), AnnoScolastico.elenco(oggi, null));
        assertEquals(List.of("2024/2025", "2025/2026", "2026/2027", "2027/2028"), AnnoScolastico.elenco(oggi, "2024/2025"));
    }
}
