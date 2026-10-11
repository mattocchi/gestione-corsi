package it.attocchi.views.insegnante;

import it.attocchi.entity.Lezione;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static it.attocchi.views.insegnante.QuandoLezione.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class QuandoLezioneTest {

    /** Domenica 11 ottobre 2026: la settimana corrente va da lunedì 5 a domenica 11. */
    private static final LocalDate OGGI_DOMENICA = LocalDate.of(2026, 10, 11);
    /** Mercoledì 7 ottobre 2026. */
    private static final LocalDate OGGI_MERCOLEDI = LocalDate.of(2026, 10, 7);

    @Test
    void lezioniFutureEDiQuestaSettimana_ancheSeCompletate() {
        assertEquals(Optional.of(OGGI), quando(OGGI_MERCOLEDI, true));
        assertEquals(Optional.of(QUESTA_SETTIMANA), quando(LocalDate.of(2026, 10, 5), true));
        assertEquals(Optional.of(QUESTA_SETTIMANA), quando(LocalDate.of(2026, 10, 11), false));
        assertEquals(Optional.of(SETTIMANA_PROSSIMA), quando(LocalDate.of(2026, 10, 12), false));
        assertEquals(Optional.of(SETTIMANA_PROSSIMA), quando(LocalDate.of(2026, 10, 18), false));
        assertEquals(Optional.empty(), quando(LocalDate.of(2026, 10, 19), false));
    }

    @Test
    void lezioniPassate_soloSeDaCompilare() {
        assertEquals(Optional.of(SETTIMANA_SCORSA), quando(LocalDate.of(2026, 9, 28), false));
        assertEquals(Optional.of(SETTIMANA_SCORSA), quando(LocalDate.of(2026, 10, 4), false));
        assertEquals(Optional.empty(), quando(LocalDate.of(2026, 10, 4), true));
        assertEquals(Optional.of(IN_RITARDO), quando(LocalDate.of(2026, 9, 27), false));
        assertEquals(Optional.empty(), quando(LocalDate.of(2026, 9, 27), true));
    }

    @Test
    void laDomenicaLaSettimanaProssimaIniziaDomani() {
        Lezione domani = new Lezione(null, LocalDate.of(2026, 10, 12), 1);
        assertEquals(Optional.of(SETTIMANA_PROSSIMA), QuandoLezione.di(domani, OGGI_DOMENICA));
        Lezione oggi = new Lezione(null, OGGI_DOMENICA, 1);
        assertEquals(Optional.of(OGGI), QuandoLezione.di(oggi, OGGI_DOMENICA));
    }

    private static Optional<QuandoLezione> quando(LocalDate data, boolean completata) {
        Lezione lezione = new Lezione(null, data, 1);
        lezione.setCompletata(completata);
        return QuandoLezione.di(lezione, OGGI_MERCOLEDI);
    }
}
