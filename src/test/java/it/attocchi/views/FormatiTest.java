package it.attocchi.views;

import it.attocchi.entity.Corso;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Year;
import java.util.Set;

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

    @Test
    void euro_formatoItaliano() {
        assertEquals("1.250,50 €", Formati.euro(new BigDecimal("1250.5")));
        assertEquals("0,00 €", Formati.euro(BigDecimal.ZERO));
        assertEquals("", Formati.euro(null));
    }

    @Test
    void giorniECadenza_cadenzaSoloSeNonSettimanale() {
        Corso corso = new Corso();
        corso.setGiorniSettimana(Set.of(DayOfWeek.SATURDAY, DayOfWeek.TUESDAY));
        assertEquals("mar, sab", Formati.giorniECadenza(corso));
        corso.setCadenzaSettimane(2);
        assertEquals("mar, sab, ogni 2 settimane", Formati.giorniECadenza(corso));
        corso.setGiorniSettimana(Set.of());
        assertEquals("ogni 2 settimane", Formati.giorniECadenza(corso));
    }
}
