package it.attocchi.entity;

import it.attocchi.entity.Festivita.Tipo;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FestivitaTest {

    @Test
    void pasqua_dateNote() {
        assertEquals(LocalDate.of(2024, 3, 31), Festivita.pasqua(2024));
        assertEquals(LocalDate.of(2025, 4, 20), Festivita.pasqua(2025));
        assertEquals(LocalDate.of(2026, 4, 5), Festivita.pasqua(2026));
        assertEquals(LocalDate.of(2027, 3, 28), Festivita.pasqua(2027));
        assertEquals(LocalDate.of(2038, 4, 25), Festivita.pasqua(2038));
    }

    @Test
    void pasquetta_ilGiornoDopoPasqua() {
        Festivita pasquetta = Festivita.calcolata("Lunedì dell'Angelo", Tipo.PASQUETTA);
        assertTrue(pasquetta.comprende(LocalDate.of(2026, 4, 6)));
        assertFalse(pasquetta.comprende(LocalDate.of(2026, 4, 5)));
        assertEquals(LocalDate.of(2027, 3, 29), pasquetta.prossima(LocalDate.of(2026, 4, 7)));
    }

    @Test
    void ogniAnno_giornoSingoloEPeriodoACavalloDAnno() {
        Festivita natale = Festivita.ogniAnno("Natale", 25, 12);
        assertTrue(natale.comprende(LocalDate.of(2031, 12, 25)));
        assertFalse(natale.comprende(LocalDate.of(2031, 12, 26)));

        Festivita vacanze = Festivita.ogniAnno("Vacanze di Natale", 23, 12);
        vacanze.setGiornoFine(6);
        vacanze.setMeseFine(1);
        assertTrue(vacanze.comprende(LocalDate.of(2026, 12, 23)));
        assertTrue(vacanze.comprende(LocalDate.of(2026, 12, 31)));
        assertTrue(vacanze.comprende(LocalDate.of(2027, 1, 6)));
        assertFalse(vacanze.comprende(LocalDate.of(2027, 1, 7)));
        assertFalse(vacanze.comprende(LocalDate.of(2026, 12, 22)));
    }

    @Test
    void datePrecise_periodoEGiornoSingolo() {
        Festivita ponte = new Festivita();
        ponte.setTipo(Tipo.DATE);
        ponte.setDataInizio(LocalDate.of(2026, 11, 2));
        assertTrue(ponte.comprende(LocalDate.of(2026, 11, 2)));
        assertFalse(ponte.comprende(LocalDate.of(2026, 11, 3)));

        ponte.setDataFine(LocalDate.of(2026, 11, 3));
        assertTrue(ponte.comprende(LocalDate.of(2026, 11, 3)));
        assertNull(ponte.prossima(LocalDate.of(2026, 11, 4)));
    }
}
