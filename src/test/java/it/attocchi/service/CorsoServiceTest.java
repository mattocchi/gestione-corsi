package it.attocchi.service;

import it.attocchi.entity.Allievo;
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

    @Autowired
    private AllievoService allievoService;

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

    @Test
    void giorniDelPartecipante_presenzeSoloNeiSuoiGiorni() {
        // Chitarra: martedì e sabato, 6 iscritti
        Corso chitarra = corso("Corso di Chitarra");
        corsoService.generaLezioni(chitarra);
        Allievo allievo = allievoService.findByUsername("allievo_chitarra1").orElseThrow();
        Long nuovaSabato = null;
        Long nuovaLunedi = null;
        try {
            corsoService.impostaGiorni(chitarra.getId(), allievo.getId(), Set.of(DayOfWeek.TUESDAY));
            assertTrue(giorniConPresenza(chitarra, allievo).equals(Set.of(DayOfWeek.TUESDAY)));
            assertEquals(Set.of(DayOfWeek.TUESDAY), corsoService.findPartecipanti(chitarra.getId()).stream()
                .filter(i -> i.getAllievo().equals(allievo)).findFirst().orElseThrow().getGiorni());
            // Solo giorni del corso
            assertThrows(IllegalArgumentException.class,
                () -> corsoService.impostaGiorni(chitarra.getId(), allievo.getId(), Set.of(DayOfWeek.MONDAY)));

            // Lezione aggiunta di sabato: non è sua; di lunedì (non è un giorno del corso): di tutti
            nuovaSabato = lezioneService.aggiungi(chitarra.getId(), LocalDate.of(2025, 8, 2), null, null, "admin").getId();
            nuovaLunedi = lezioneService.aggiungi(chitarra.getId(), LocalDate.of(2025, 8, 4), null, null, "admin").getId();
            assertEquals(5, presenzaService.findByLezione(nuovaSabato).size());
            assertEquals(6, presenzaService.findByLezione(nuovaLunedi).size());

            // Tutti i giorni = di nuovo in ogni lezione da fare
            corsoService.impostaGiorni(chitarra.getId(), allievo.getId(), Set.of(DayOfWeek.TUESDAY, DayOfWeek.SATURDAY));
            assertTrue(corsoService.findPartecipanti(chitarra.getId()).stream()
                .filter(i -> i.getAllievo().equals(allievo)).findFirst().orElseThrow().getGiorni().isEmpty());
            assertEquals(Set.of(DayOfWeek.TUESDAY, DayOfWeek.SATURDAY, DayOfWeek.MONDAY), giorniConPresenza(chitarra, allievo));
        } finally {
            corsoService.impostaGiorni(chitarra.getId(), allievo.getId(), Set.of());
            if (nuovaSabato != null) {
                lezioneService.eliminaAggiunta(nuovaSabato, "admin");
            }
            if (nuovaLunedi != null) {
                lezioneService.eliminaAggiunta(nuovaLunedi, "admin");
            }
        }
    }

    @Test
    void generaLezioni_numeroLezioniPerOgniAllievoNeiSuoiGiorni() {
        // Chitarra (martedì e sabato, 30 lezioni): Chitarra1..3 solo il martedì, Chitarra4..6 solo il sabato
        Corso chitarra = corso("Corso di Chitarra");
        List<Allievo> allievi = java.util.stream.IntStream.rangeClosed(1, 6)
            .mapToObj(n -> allievoService.findByUsername("allievo_chitarra" + n).orElseThrow()).toList();
        try {
            for (int n = 0; n < 6; n++) {
                corsoService.impostaGiorni(chitarra.getId(), allievi.get(n).getId(),
                    Set.of(n < 3 ? DayOfWeek.TUESDAY : DayOfWeek.SATURDAY));
            }
            corsoService.generaLezioni(chitarra);

            List<Lezione> lezioni = lezioneService.findByCorso(chitarra.getId());
            assertEquals(60, lezioni.size());
            assertEquals(java.util.stream.IntStream.rangeClosed(1, 60).boxed().toList(),
                lezioni.stream().map(Lezione::getNumeroLezione).sorted().toList());
            lezioni.forEach(l -> assertEquals(3, presenzaService.findByLezione(l.getId()).size()));
            for (int n = 0; n < 6; n++) {
                Allievo allievo = allievi.get(n);
                assertEquals(30, presenzaService.findByCorsoAndAllievo(chitarra.getId(), allievo.getId()).size());
                assertEquals(Set.of(n < 3 ? DayOfWeek.TUESDAY : DayOfWeek.SATURDAY), giorniConPresenza(chitarra, allievo));
            }

            // Lezioni svolte conservate: contano e non si ripetono
            Lezione prima = lezioni.getFirst();
            prima.setCompletata(true);
            lezioneService.save(prima);
            corsoService.generaLezioni(chitarra);
            List<Lezione> rigenerate = lezioneService.findByCorso(chitarra.getId());
            assertEquals(60, rigenerate.size());
            assertEquals(1, rigenerate.stream().filter(l -> l.getData().equals(prima.getData())).count());
        } finally {
            Lezione prima = lezioneService.findByCorso(chitarra.getId()).getFirst();
            prima.setCompletata(false);
            lezioneService.save(prima);
            allievi.forEach(a -> corsoService.impostaGiorni(chitarra.getId(), a.getId(), Set.of()));
            corsoService.generaLezioni(chitarra);
        }
        // Tutti i giorni a tutti: come prima, 30 date con tutti gli iscritti
        List<Lezione> lezioni = lezioneService.findByCorso(chitarra.getId());
        assertEquals(30, lezioni.size());
        lezioni.forEach(l -> assertEquals(6, presenzaService.findByLezione(l.getId()).size()));
    }

    private Set<DayOfWeek> giorniConPresenza(Corso corso, Allievo allievo) {
        return presenzaService.findByCorsoAndAllievo(corso.getId(), allievo.getId()).stream()
            .map(p -> p.getLezione().getData().getDayOfWeek()).collect(Collectors.toSet());
    }

    private Corso corso(String nome) {
        return corsoService.findAll().stream()
            .filter(c -> c.getNome().equals(nome))
            .findFirst()
            .orElseThrow();
    }
}
