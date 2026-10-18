package it.attocchi.service;

import it.attocchi.entity.Corso;
import it.attocchi.entity.Insegnante;
import it.attocchi.entity.Lezione;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica la generazione delle lezioni sui dati di test creati da DataInitializer (profilo dev).
 */
@SpringBootTest
class CorsoServiceTest {

    @Autowired
    private InsegnanteService insegnanteService;

    @Autowired
    private FestivitaService festivitaService;

    @Autowired
    private CorsoService corsoService;

    @Autowired
    private LezioneService lezioneService;

    @Autowired
    private PresenzaService presenzaService;

    @Test
    void save_prezzoConPreiscrizioneTraZeroECostoTotale() {
        Corso canto = corso("Corso di Canto");
        canto.setPrezzoPreiscrizione(new java.math.BigDecimal("600.01"));
        assertThrows(IllegalArgumentException.class, () -> corsoService.save(canto));
        canto.setPrezzoPreiscrizione(new java.math.BigDecimal("-1"));
        assertThrows(IllegalArgumentException.class, () -> corsoService.save(canto));
    }

    @Test
    void generaLezioni_rispettaNumeroLezioniEGiorniSettimana() {
        Corso chitarra = corso("Corso di Chitarra");

        // Rigenera partendo da un'istanza detached, come avviene dalla UI
        corsoService.generaLezioni(chitarra);

        List<Lezione> lezioni = lezioneService.findByCorso(chitarra.getId());
        assertEquals(30, lezioni.size());
        Set<DayOfWeek> giorni = lezioni.stream().map(l -> l.getData().getDayOfWeek()).collect(Collectors.toSet());
        assertEquals(Set.of(DayOfWeek.TUESDAY, DayOfWeek.SATURDAY), giorni);
        assertEquals(1, lezioni.getFirst().getNumeroLezione());
        assertEquals(chitarra.getOrarioInizio().plusMinutes(40), lezioni.getFirst().getOrarioFine());
    }

    @Test
    void generaLezioni_creaUnaPresenzaPerAllievo() {
        Corso canto = corso("Corso di Canto");

        corsoService.generaLezioni(canto);

        List<Lezione> lezioni = lezioneService.findByCorso(canto.getId());
        assertTrue(lezioni.stream().allMatch(l -> l.getData().getDayOfWeek() == DayOfWeek.THURSDAY));
        lezioni.forEach(l -> assertEquals(7, presenzaService.findByLezione(l.getId()).size()));
    }

    @Test
    void generaLezioni_saltaLeFestivitaSenzaPerdereLezioni() {
        // Canto: il giovedì dal 3/10/2024; Santo Stefano (26/12/2024) e Festa del Lavoro (1/5/2025) sono giovedì
        Corso canto = corso("Corso di Canto");

        corsoService.generaLezioni(canto);

        List<LocalDate> date = lezioneService.findByCorso(canto.getId()).stream().map(Lezione::getData).toList();
        assertEquals(30, date.size());
        assertFalse(date.contains(LocalDate.of(2024, 12, 26)));
        assertTrue(date.containsAll(List.of(LocalDate.of(2024, 12, 19), LocalDate.of(2025, 1, 2))));
        CalendarioFestivita festivita = festivitaService.calendario();
        assertTrue(date.stream().noneMatch(festivita::festivo));
    }

    @Test
    void secondoInsegnante_aggiornaSoloLeLezioniDaFare() {
        Corso canto = corso("Corso di Canto");
        corsoService.generaLezioni(canto);
        Insegnante maria = canto.getInsegnante();
        Insegnante marco = insegnanteService.findByUsername("insegnante2").orElseThrow();
        Lezione prima = lezioneService.findByCorso(canto.getId()).getFirst();
        assertEquals(Set.of(maria), prima.getInsegnantiPresenti());
        prima.setCompletata(true);
        lezioneService.save(prima);

        canto.setSecondoInsegnante(maria);
        assertThrows(IllegalArgumentException.class, () -> corsoService.save(canto));
        canto.setSecondoInsegnante(marco);
        corsoService.save(canto);
        try {
            List<Lezione> lezioni = lezioneService.findByCorso(canto.getId());
            assertEquals(Set.of(maria), lezioni.getFirst().getInsegnantiPresenti());
            assertTrue(lezioni.stream().skip(1).allMatch(l -> l.getInsegnantiPresenti().equals(Set.of(maria, marco))));
            assertEquals("Maria Verdi, Marco Rossi", corso("Corso di Canto").getNomiInsegnanti());
        } finally {
            Corso ripristino = corso("Corso di Canto");
            ripristino.setSecondoInsegnante(null);
            corsoService.save(ripristino);
            Lezione completata = lezioneService.findById(prima.getId());
            completata.setCompletata(false);
            lezioneService.save(completata);
        }
        assertTrue(lezioneService.findByCorso(canto.getId()).stream()
            .allMatch(l -> l.getInsegnantiPresenti().equals(Set.of(maria))));
    }

    @Test
    void avvio_completaLeLezioniSenzaInsegnanti() {
        Corso chitarra = corso("Corso di Chitarra");
        Lezione lezione = lezioneService.findByCorso(chitarra.getId()).getFirst();
        lezione.getInsegnantiPresenti().clear();
        lezioneService.save(lezione);

        assertEquals(1, lezioneService.completaInsegnantiPresenti());
        assertEquals(Set.of(chitarra.getInsegnante()), lezioneService.findById(lezione.getId()).getInsegnantiPresenti());
        assertEquals(0, lezioneService.completaInsegnantiPresenti());
    }

    private Corso corso(String nome) {
        return corsoService.findAll().stream()
            .filter(c -> c.getNome().equals(nome))
            .findFirst()
            .orElseThrow();
    }
}
