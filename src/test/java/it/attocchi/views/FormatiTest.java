package it.attocchi.views;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Year;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FormatiTest {

    private static final Year ANNO_CORRENTE = Year.of(2026);

    @Test
    void data_nellAnnoCorrente_senzaAnno() {
        assertEquals("mer 7 ottobre", Formati.data(LocalDate.of(2026, 10, 7), ANNO_CORRENTE));
        assertEquals("lun 2 marzo", Formati.data(LocalDate.of(2026, 3, 2), ANNO_CORRENTE));
    }

    @Test
    void data_diAltroAnno_conAnno() {
        assertEquals("gio 3 ottobre 2024", Formati.data(LocalDate.of(2024, 10, 3), ANNO_CORRENTE));
    }

    @Test
    void data_nulla_stringaVuota() {
        assertEquals("", Formati.data(null, ANNO_CORRENTE));
    }
}
