package it.attocchi.service;

import it.attocchi.entity.Corso;
import it.attocchi.entity.Lezione;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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

/** Lezioni non effettuate e lezioni aggiunte, sui dati di DataInitializer (profilo dev). */
@SpringBootTest
class LezioneServiceTest {

    @Autowired
    private LezioneService lezioneService;

    @Autowired
    private CorsoService corsoService;

    @Autowired
    private PresenzaService presenzaService;

    @Autowired
    private InsegnanteService insegnanteService;

    @Autowired
    private DashboardService dashboardService;

    @Test
    void nonEffettuata_insegnanteDelCorsoOSegreteria_eRipristino() {
        Lezione lezione = lezioniDi("Corso di Canto").get(3);
        var prima = dashboardService.riepilogoInsegnante("insegnante1");

        assertThrows(IllegalStateException.class, () -> lezioneService.segnaNonEffettuata(lezione.getId(), null, "insegnante2"));
        assertThrows(IllegalStateException.class, () -> lezioneService.segnaNonEffettuata(lezione.getId(), null, "genitore1"));
        lezioneService.segnaNonEffettuata(lezione.getId(), "  insegnante assente ", "insegnante1");
        try {
            Lezione salvata = lezioneService.findById(lezione.getId());
            assertTrue(salvata.isNonEffettuata());
            assertFalse(salvata.isDaFare());
            assertEquals("insegnante assente", salvata.getMotivoNonEffettuata());
            var dopo = dashboardService.riepilogoInsegnante("insegnante1");
            assertEquals(prima.lezioniDaFare() - 1, dopo.lezioniDaFare());
            assertEquals(prima.lezioniNonEffettuate() + 1, dopo.lezioniNonEffettuate());
            assertThrows(IllegalStateException.class, () -> lezioneService.completa(lezione.getId(), "insegnante1"));
        } finally {
            lezioneService.ripristina(lezione.getId(), "segreteria");
        }
        Lezione ripristinata = lezioneService.findById(lezione.getId());
        assertTrue(ripristinata.isDaFare());
        assertNull(ripristinata.getMotivoNonEffettuata());
        assertEquals(7, presenzaService.findByLezione(lezione.getId()).size());
    }

    @Test
    void recupero_prendeIlNumeroEHaLePresenze_eSiEliminaPrimaDiRipristinare() {
        Lezione saltata = lezioniDi("Corso di Canto").get(4);
        Corso canto = saltata.getCorso();
        lezioneService.segnaNonEffettuata(saltata.getId(), null, "insegnante1");
        Lezione recupero = lezioneService.aggiungi(canto.getId(), LocalDate.of(2025, 6, 10), null, saltata.getId(), "segreteria");
        try {
            assertTrue(recupero.isAggiunta());
            assertEquals(saltata.getNumeroLezione(), recupero.getNumeroLezione());
            assertEquals(canto.getOrarioInizio(), recupero.getOrarioInizio());
            assertEquals(7, presenzaService.findByLezione(recupero.getId()).size());
            // Un solo recupero; il ripristino richiede di eliminarlo prima
            assertThrows(IllegalStateException.class,
                () -> lezioneService.aggiungi(canto.getId(), LocalDate.of(2025, 6, 11), null, saltata.getId(), "insegnante1"));
            assertThrows(IllegalStateException.class, () -> lezioneService.ripristina(saltata.getId(), "insegnante1"));
            // La segreteria non elimina
            assertThrows(IllegalStateException.class, () -> lezioneService.eliminaAggiunta(recupero.getId(), "segreteria"));
        } finally {
            lezioneService.eliminaAggiunta(recupero.getId(), "insegnante1");
            lezioneService.ripristina(saltata.getId(), "insegnante1");
        }
        assertNull(lezioneService.findById(recupero.getId()));
    }

    @Test
    void lezioneLibera_numeroSuccessivoEOrarioScelto_eLeProgrammateNonSiEliminano() {
        Corso canto = lezioniDi("Corso di Canto").getFirst().getCorso();
        int massimo = lezioniDi("Corso di Canto").stream().mapToInt(Lezione::getNumeroLezione).max().orElseThrow();
        Lezione extra = lezioneService.aggiungi(canto.getId(), LocalDate.of(2025, 6, 12), LocalTime.of(18, 0), null, "admin");
        try {
            assertEquals(massimo + 1, extra.getNumeroLezione());
            assertEquals(LocalTime.of(18, 40), extra.getOrarioFine());
            Lezione programmata = lezioniDi("Corso di Canto").getFirst();
            assertThrows(IllegalStateException.class, () -> lezioneService.eliminaAggiunta(programmata.getId(), "admin"));
        } finally {
            lezioneService.eliminaAggiunta(extra.getId(), "admin");
        }
    }

    @Test
    void generaLezioni_conservaNonEffettuateEAggiunteSenzaDoppioni() {
        Corso chitarra = lezioniDi("Corso di Chitarra").getFirst().getCorso();
        List<Lezione> lezioni = lezioniDi("Corso di Chitarra");
        Lezione completata = lezioni.get(0);
        Lezione saltata = lezioni.get(1);
        lezioneService.completa(completata.getId(), "insegnante2");
        lezioneService.segnaNonEffettuata(saltata.getId(), "festività", "insegnante2");
        Lezione extra = lezioneService.aggiungi(chitarra.getId(), LocalDate.of(2025, 6, 14), null, null, "insegnante2");
        try {
            corsoService.generaLezioni(chitarra);

            List<Lezione> dopo = lezioniDi("Corso di Chitarra");
            assertEquals(31, dopo.size());
            assertTrue(dopo.stream().anyMatch(l -> l.getId().equals(completata.getId()) && l.isCompletata()));
            assertTrue(dopo.stream().anyMatch(l -> l.getId().equals(saltata.getId()) && l.isNonEffettuata()));
            assertTrue(dopo.stream().anyMatch(l -> l.getId().equals(extra.getId())));
            Set<Integer> numeriProgrammati = dopo.stream().filter(l -> !l.isAggiunta())
                .map(Lezione::getNumeroLezione).collect(Collectors.toSet());
            assertEquals(30, numeriProgrammati.size());
        } finally {
            lezioneService.eliminaAggiunta(extra.getId(), "insegnante2");
            lezioneService.ripristina(saltata.getId(), "insegnante2");
            Lezione daRiaprire = lezioneService.findById(completata.getId());
            daRiaprire.setCompletata(false);
            lezioneService.save(daRiaprire);
        }
    }

    private List<Lezione> lezioniDi(String nomeCorso) {
        Corso corso = corsoService.findAll().stream().filter(c -> c.getNome().equals(nomeCorso)).findFirst().orElseThrow();
        return lezioneService.findByCorso(corso.getId());
    }

    @Test
    void secondoInsegnante_vedeEGestisceLeLezioniDelCorso() {
        Corso canto = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow();
        var marco = insegnanteService.findByUsername("insegnante2").orElseThrow();
        assertEquals(1, dashboardService.corsiInsegnante("insegnante2"));
        canto.setSecondoInsegnante(marco);
        corsoService.save(canto);
        Lezione lezione = lezioneService.findByCorso(canto.getId()).getFirst();
        try {
            assertEquals(2, dashboardService.corsiInsegnante("insegnante2"));
            assertTrue(lezioneService.findByInsegnante(marco.getId()).stream()
                .anyMatch(l -> l.getCorso().getId().equals(canto.getId())));
            assertEquals(2, corsoService.findByInsegnante(marco.getId()).size());

            lezioneService.completa(lezione.getId(), "insegnante2");
            assertTrue(lezioneService.findById(lezione.getId()).isCompletata());
        } finally {
            Lezione ripristino = lezioneService.findById(lezione.getId());
            ripristino.setCompletata(false);
            lezioneService.save(ripristino);
            canto.setSecondoInsegnante(null);
            corsoService.save(canto);
        }
        assertEquals(1, dashboardService.corsiInsegnante("insegnante2"));
        assertThrows(IllegalStateException.class, () -> lezioneService.completa(lezione.getId(), "insegnante2"));
    }

    @Test
    void insegnantiPresenti_soloInsegnantiDelCorsoSegreteriaEAdmin() {
        Corso canto = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow();
        var maria = canto.getInsegnante();
        var marco = insegnanteService.findByUsername("insegnante2").orElseThrow();
        Lezione lezione = lezioneService.findByCorso(canto.getId()).getFirst();
        // Corso con un solo insegnante: Marco non è del corso
        assertThrows(IllegalArgumentException.class,
            () -> lezioneService.impostaInsegnantiPresenti(lezione.getId(), Set.of(marco.getId()), "admin"));
        canto.setSecondoInsegnante(marco);
        corsoService.save(canto);
        try {
            assertThrows(IllegalStateException.class,
                () -> lezioneService.impostaInsegnantiPresenti(lezione.getId(), Set.of(maria.getId()), "genitore1"));
            assertThrows(IllegalStateException.class,
                () -> lezioneService.impostaInsegnantiPresenti(lezione.getId(), Set.of(maria.getId()), "allievo_canto1"));
            assertThrows(IllegalArgumentException.class,
                () -> lezioneService.impostaInsegnantiPresenti(lezione.getId(), Set.of(), "admin"));

            lezioneService.impostaInsegnantiPresenti(lezione.getId(), Set.of(marco.getId()), "segreteria");
            assertEquals(Set.of(marco), lezioneService.findById(lezione.getId()).getInsegnantiPresenti());
            lezioneService.impostaInsegnantiPresenti(lezione.getId(), Set.of(maria.getId(), marco.getId()), "insegnante1");
            assertEquals(Set.of(maria, marco), lezioneService.findById(lezione.getId()).getInsegnantiPresenti());
        } finally {
            canto.setSecondoInsegnante(null);
            corsoService.save(canto);
        }
        assertEquals(Set.of(maria), lezioneService.findById(lezione.getId()).getInsegnantiPresenti());
    }
}
