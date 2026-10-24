package it.attocchi.entity;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IscrizioneTest {

    @Test
    void orarioDi_ilSuoPoiQuelloDelGruppoPoiQuelloDellaLezione() {
        Corso corso = new Corso();
        corso.setOrarioInizio(LocalTime.of(10, 0));
        Iscrizione iscrizione = new Iscrizione(corso, new Allievo());
        Lezione lezione = new Lezione(corso, LocalDate.of(2026, 10, 24), 1);
        lezione.setOrarioInizio(LocalTime.of(10, 0));
        assertEquals(LocalTime.of(10, 0), iscrizione.orarioDi(lezione));

        iscrizione.setGruppo(new GruppoCorso(corso, "Grandi", DayOfWeek.SATURDAY, LocalTime.of(11, 0)));
        assertEquals(LocalTime.of(11, 0), iscrizione.orarioDi(lezione));

        iscrizione.setOrarioLezione(LocalTime.of(11, 30));
        assertEquals(LocalTime.of(11, 30), iscrizione.orarioDi(lezione));

        // Lezione a un orario diverso da quello del corso (spostata o aggiunta a un'altra ora): il suo orario
        lezione.setOrarioInizio(LocalTime.of(15, 0));
        assertEquals(LocalTime.of(15, 0), iscrizione.orarioDi(lezione));
    }
}
