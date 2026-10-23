package it.attocchi.views;

import it.attocchi.entity.Corso;
import it.attocchi.entity.GruppoCorso;
import it.attocchi.entity.Lezione;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Year;
import java.util.List;
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

    @Test
    void numeroLezione_conIlGruppoOIlGiornoNeiCorsiConGruppi() {
        Corso corso = new Corso();
        // Lunedì 6 e venerdì 10 ottobre 2025
        Lezione lunedi = new Lezione(corso, LocalDate.of(2025, 10, 6), 5);
        Lezione venerdi = new Lezione(corso, LocalDate.of(2025, 10, 10), 5);
        assertEquals("5", Formati.numeroLezione(lunedi, List.of()));
        List<GruppoCorso> gruppi = List.of(new GruppoCorso(corso, "Piccoli", DayOfWeek.MONDAY, LocalTime.of(10, 0)),
            new GruppoCorso(corso, null, DayOfWeek.FRIDAY, LocalTime.of(17, 0)));
        assertEquals("5 · Piccoli", Formati.numeroLezione(lunedi, gruppi));
        assertEquals("5 · ven", Formati.numeroLezione(venerdi, gruppi));
        // Un recupero di mercoledì della lezione del lunedì resta del gruppo del lunedì
        Lezione recupero = new Lezione(corso, LocalDate.of(2025, 10, 8), 5);
        recupero.setRecuperoDi(lunedi);
        assertEquals("5 · Piccoli", Formati.numeroLezione(recupero, gruppi));
    }
}
