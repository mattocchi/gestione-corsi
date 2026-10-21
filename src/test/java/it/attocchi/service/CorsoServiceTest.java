package it.attocchi.service;

import it.attocchi.entity.Allievo;
import it.attocchi.entity.Corso;
import it.attocchi.entity.GruppoCorso;
import it.attocchi.entity.Insegnante;
import it.attocchi.entity.Iscrizione;
import it.attocchi.entity.Lezione;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    void generaLezioni_ogniDueSettimane_festivitaSpostaAllaSettimanaDopo() {
        // Canto: il giovedì dal 3/10/2024 a settimane alterne; Santo Stefano (26/12/2024) slitta al 2/1/2025
        Corso canto = corso("Corso di Canto");
        canto.setCadenzaSettimane(3);
        assertThrows(IllegalArgumentException.class, () -> corsoService.save(canto));
        canto.setCadenzaSettimane(2);
        corsoService.save(canto);
        try {
            corsoService.generaLezioni(canto);

            List<LocalDate> date = lezioneService.findByCorso(canto.getId()).stream().map(Lezione::getData).toList();
            assertEquals(30, date.size());
            assertEquals(LocalDate.of(2024, 10, 3), date.getFirst());
            assertTrue(date.containsAll(List.of(LocalDate.of(2024, 12, 12), LocalDate.of(2025, 1, 2),
                LocalDate.of(2025, 1, 16))));
            assertFalse(date.contains(LocalDate.of(2024, 12, 26)));
            for (int i = 1; i < date.size(); i++) {
                long giorni = date.get(i - 1).until(date.get(i), java.time.temporal.ChronoUnit.DAYS);
                assertEquals(date.get(i).equals(LocalDate.of(2025, 1, 2)) ? 21 : 14, giorni, "dopo " + date.get(i - 1));
            }
        } finally {
            Corso ripristino = corso("Corso di Canto");
            ripristino.setCadenzaSettimane(1);
            corsoService.save(ripristino);
            corsoService.generaLezioni(ripristino);
        }
        assertEquals(1, corso("Corso di Canto").getCadenzaSettimane());
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
    void gruppoDelPartecipante_presenzeSoloNelSuoGiorno() {
        // Chitarra: martedì e sabato, 6 iscritti senza orario
        Corso chitarra = corso("Corso di Chitarra");
        corsoService.generaLezioni(chitarra);
        Allievo allievo = allievoService.findByUsername("allievo_chitarra1").orElseThrow();
        GruppoCorso gruppo = corsoService.salvaGruppo(chitarra.getId(),
            new GruppoCorso(null, "  ", DayOfWeek.TUESDAY, LocalTime.of(17, 0)));
        Long nuovaSabato = null;
        Long nuovaLunedi = null;
        try {
            assertNull(gruppo.getNome());
            assertEquals(List.of(gruppo), corsoService.gruppi(chitarra.getId()));
            corsoService.assegnaGruppo(chitarra.getId(), allievo.getId(), gruppo.getId());
            assertEquals(Set.of(DayOfWeek.TUESDAY), giorniConPresenza(chitarra, allievo));
            assertEquals(gruppo, iscrizione(chitarra, allievo).getGruppo());
            // Senza orario prende quello del gruppo
            assertEquals(LocalTime.of(17, 0), iscrizione(chitarra, allievo).getOrarioLezione());

            // Solo giorni del corso, orario obbligatorio; un giorno usato da un gruppo non si toglie dal corso
            assertThrows(IllegalArgumentException.class, () -> corsoService.salvaGruppo(chitarra.getId(),
                new GruppoCorso(null, null, DayOfWeek.MONDAY, LocalTime.of(17, 0))));
            assertThrows(IllegalArgumentException.class, () -> corsoService.salvaGruppo(chitarra.getId(),
                new GruppoCorso(null, null, DayOfWeek.SATURDAY, null)));
            Corso senzaMartedi = corsoService.findById(chitarra.getId());
            senzaMartedi.getGiorniSettimana().remove(DayOfWeek.TUESDAY);
            assertThrows(IllegalArgumentException.class, () -> corsoService.save(senzaMartedi));

            // Lezione aggiunta di sabato: non è sua; di lunedì (non è un giorno del corso): di tutti
            nuovaSabato = lezioneService.aggiungi(chitarra.getId(), LocalDate.of(2025, 8, 2), null, null, "admin").getId();
            nuovaLunedi = lezioneService.aggiungi(chitarra.getId(), LocalDate.of(2025, 8, 4), null, null, "admin").getId();
            assertEquals(5, presenzaService.findByLezione(nuovaSabato).size());
            assertEquals(6, presenzaService.findByLezione(nuovaLunedi).size());

            // Il gruppo passa al sabato: le presenze da fare lo seguono
            gruppo.setGiorno(DayOfWeek.SATURDAY);
            gruppo = corsoService.salvaGruppo(chitarra.getId(), gruppo);
            assertEquals(Set.of(DayOfWeek.SATURDAY, DayOfWeek.MONDAY), giorniConPresenza(chitarra, allievo));

            // Gruppo eliminato: di nuovo tutti i giorni
            corsoService.eliminaGruppo(gruppo.getId());
            assertNull(iscrizione(chitarra, allievo).getGruppo());
            assertTrue(corsoService.gruppi(chitarra.getId()).isEmpty());
            assertEquals(Set.of(DayOfWeek.TUESDAY, DayOfWeek.SATURDAY, DayOfWeek.MONDAY), giorniConPresenza(chitarra, allievo));
        } finally {
            corsoService.assegnaGruppo(chitarra.getId(), allievo.getId(), null);
            corsoService.impostaOrario(chitarra.getId(), allievo.getId(), null);
            corsoService.gruppi(chitarra.getId()).forEach(g -> corsoService.eliminaGruppo(g.getId()));
            if (nuovaSabato != null) {
                lezioneService.eliminaAggiunta(nuovaSabato, "admin");
            }
            if (nuovaLunedi != null) {
                lezioneService.eliminaAggiunta(nuovaLunedi, "admin");
            }
        }
    }

    @Test
    void generaLezioni_numeroLezioniPerOgniAllievoNelGiornoDelSuoGruppo() {
        // Chitarra (martedì e sabato, 30 lezioni): Chitarra1..3 nel gruppo del martedì, Chitarra4..6 in quello del sabato
        Corso chitarra = corso("Corso di Chitarra");
        List<Allievo> allievi = java.util.stream.IntStream.rangeClosed(1, 6)
            .mapToObj(n -> allievoService.findByUsername("allievo_chitarra" + n).orElseThrow()).toList();
        GruppoCorso martedi = corsoService.salvaGruppo(chitarra.getId(),
            new GruppoCorso(null, null, DayOfWeek.TUESDAY, LocalTime.of(17, 0)));
        GruppoCorso sabato = corsoService.salvaGruppo(chitarra.getId(),
            new GruppoCorso(null, "Principianti", DayOfWeek.SATURDAY, LocalTime.of(10, 0)));
        try {
            for (int n = 0; n < 6; n++) {
                corsoService.assegnaGruppo(chitarra.getId(), allievi.get(n).getId(), (n < 3 ? martedi : sabato).getId());
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
            allievi.forEach(a -> {
                corsoService.assegnaGruppo(chitarra.getId(), a.getId(), null);
                corsoService.impostaOrario(chitarra.getId(), a.getId(), null);
            });
            corsoService.eliminaGruppo(martedi.getId());
            corsoService.eliminaGruppo(sabato.getId());
            corsoService.generaLezioni(chitarra);
        }
        // Senza gruppi: come prima, 30 date con tutti gli iscritti
        List<Lezione> lezioni = lezioneService.findByCorso(chitarra.getId());
        assertEquals(30, lezioni.size());
        lezioni.forEach(l -> assertEquals(6, presenzaService.findByLezione(l.getId()).size()));
    }

    private Iscrizione iscrizione(Corso corso, Allievo allievo) {
        return corsoService.findPartecipanti(corso.getId()).stream()
            .filter(i -> i.getAllievo().equals(allievo)).findFirst().orElseThrow();
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
