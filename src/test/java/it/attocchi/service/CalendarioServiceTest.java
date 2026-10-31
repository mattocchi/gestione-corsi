package it.attocchi.service;

import it.attocchi.entity.Corso;
import it.attocchi.entity.GruppoCorso;
import it.attocchi.service.CalendarioService.VoceCalendario;
import it.attocchi.service.CalendarioService.VoceSettimana;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Calendario di ottobre 2024 sui dati di DataInitializer: Canto il giovedì (3, 10, 17, 24, 31), Chitarra il martedì
 * (1, 8, 15, 22, 29) e il sabato (5, 12, 19, 26).
 */
@SpringBootTest
class CalendarioServiceTest {

    private static final YearMonth OTTOBRE = YearMonth.of(2024, 10);

    @Autowired
    private CalendarioService calendarioService;

    @Autowired
    private CorsoService corsoService;

    @Autowired
    private AllievoService allievoService;

    @Test
    void adminESegreteria_tutteLeLezioniConIlFiltroPerCorso() {
        assertEquals(14, calendarioService.lezioniDelMese("admin", OTTOBRE, null).size());
        assertEquals(14, calendarioService.lezioniDelMese("segreteria", OTTOBRE, null).size());
        Corso canto = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow();
        List<VoceCalendario> soloCanto = calendarioService.lezioniDelMese("admin", OTTOBRE, canto.getId());
        assertEquals(5, soloCanto.size());
        assertTrue(soloCanto.stream().allMatch(v -> v.lezione().getData().getDayOfWeek() == DayOfWeek.THURSDAY));
        // In ordine di data
        assertEquals(1, calendarioService.lezioniDelMese("admin", OTTOBRE, null).getFirst().lezione().getData().getDayOfMonth());
        assertTrue(calendarioService.lezioniDelMese("admin", YearMonth.of(2030, 1), null).isEmpty());
    }

    @Test
    void insegnanteEAllievo_soloLeProprieLezioni() {
        assertEquals(Set.of("Corso di Canto"), corsi(calendarioService.lezioniDelMese("insegnante1", OTTOBRE, null)));
        assertEquals(5, calendarioService.lezioniDelMese("insegnante1", OTTOBRE, null).size());
        assertEquals(9, calendarioService.lezioniDelMese("insegnante2", OTTOBRE, null).size());
        assertEquals(5, calendarioService.lezioniDelMese("allievo_canto1", OTTOBRE, null).size());
        assertEquals(9, calendarioService.lezioniDelMese("allievo_chitarra1", OTTOBRE, null).size());
    }

    @Test
    void genitore_leLezioniDeiFigliConIlNome() {
        // genitore3: Allievo Canto3 (Canto) e Allievo Chitarra1 (Chitarra)
        List<VoceCalendario> voci = calendarioService.lezioniDelMese("genitore3", OTTOBRE, null);
        assertEquals(14, voci.size());
        assertEquals(Set.of("Allievo Canto3", "Allievo Chitarra1"), voci.stream().map(VoceCalendario::perChi).collect(Collectors.toSet()));
    }

    @Test
    void gruppi_unaVocePerGruppoConIlSuoOrario() {
        // Chitarra (insegnante2): sabato due gruppi, Piccoli alle 10 (Chitarra1, Chitarra2) e Grandi alle 11 (Chitarra3);
        // Chitarra4..6 senza gruppo frequentano martedì e sabato
        Corso chitarra = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Chitarra")).findFirst().orElseThrow();
        GruppoCorso piccoli = corsoService.salvaGruppo(chitarra.getId(), new GruppoCorso(null, "Piccoli", DayOfWeek.SATURDAY, LocalTime.of(10, 0)));
        GruppoCorso grandi = corsoService.salvaGruppo(chitarra.getId(), new GruppoCorso(null, "Grandi", DayOfWeek.SATURDAY, LocalTime.of(11, 0)));
        try {
            assegna(chitarra, "allievo_chitarra1", piccoli);
            assegna(chitarra, "allievo_chitarra2", piccoli);
            assegna(chitarra, "allievo_chitarra3", grandi);

            // Sabato 5 ottobre: Piccoli 10:00, Grandi 11:00, poi chi non ha gruppo all'orario della lezione
            List<VoceCalendario> sabato = delGiorno(calendarioService.lezioniDelMese("insegnante2", OTTOBRE, null), 5);
            assertEquals(3, sabato.size());
            assertEquals(piccoli.getId(), sabato.get(0).gruppo().getId());
            assertEquals(LocalTime.of(10, 0), sabato.get(0).orario());
            assertEquals(grandi.getId(), sabato.get(1).gruppo().getId());
            assertEquals(LocalTime.of(11, 0), sabato.get(1).orario());
            assertTrue(sabato.get(2).senzaGruppo());
            // Tutte e tre sono la stessa lezione
            assertEquals(1, sabato.stream().map(v -> v.lezione().getId()).distinct().count());
            // Martedì 1: nessuno dei presenti ha un gruppo, la lezione intera
            List<VoceCalendario> martedi = delGiorno(calendarioService.lezioniDelMese("insegnante2", OTTOBRE, null), 1);
            assertEquals(1, martedi.size());
            assertNull(martedi.getFirst().gruppo());
            assertFalse(martedi.getFirst().senzaGruppo());
            // Anche per l'admin
            assertEquals(3, delGiorno(calendarioService.lezioniDelMese("admin", OTTOBRE, chitarra.getId()), 5).size());

            // L'allievo: solo il suo gruppo, con l'orario del gruppo (e niente martedì)
            List<VoceCalendario> allievo = calendarioService.lezioniDelMese("allievo_chitarra3", OTTOBRE, null);
            assertEquals(4, allievo.size());
            assertTrue(allievo.stream().allMatch(v -> v.gruppo().getId().equals(grandi.getId()) && v.orario().equals(LocalTime.of(11, 0))));

            // Senza un orario suo vale quello del gruppo, con un orario suo vale il suo
            Long chitarra3 = allievoService.findByUsername("allievo_chitarra3").orElseThrow().getId();
            corsoService.impostaOrario(chitarra.getId(), chitarra3, null);
            assertTrue(calendarioService.lezioniDelMese("allievo_chitarra3", OTTOBRE, null).stream()
                .allMatch(v -> v.orario().equals(LocalTime.of(11, 0))));
            corsoService.impostaOrario(chitarra.getId(), chitarra3, LocalTime.of(11, 30));
            List<VoceCalendario> suo = calendarioService.lezioniDelMese("allievo_chitarra3", OTTOBRE, null);
            assertTrue(suo.stream().allMatch(v -> v.orario().equals(LocalTime.of(11, 30))));
            // Lo staff vede sempre l'orario del gruppo
            assertEquals(LocalTime.of(11, 0), delGiorno(calendarioService.lezioniDelMese("insegnante2", OTTOBRE, null), 5).get(1).orario());
        } finally {
            corsoService.eliminaGruppo(piccoli.getId());
            corsoService.eliminaGruppo(grandi.getId());
            // Assegnati al gruppo avevano preso il suo orario: tornano senza
            for (String username : List.of("allievo_chitarra1", "allievo_chitarra2", "allievo_chitarra3")) {
                corsoService.impostaOrario(chitarra.getId(), allievoService.findByUsername(username).orElseThrow().getId(), null);
            }
        }
    }

    @Test
    void settimanaTipo_iCorsiPerGiornoEOrarioSecondoIlRuolo() {
        // Chitarra martedì e sabato alle 18:00, Canto giovedì alle 17:00, lezioni da 40 minuti
        List<VoceSettimana> admin = calendarioService.settimanaTipo("admin", null);
        assertEquals(List.of("TUESDAY Corso di Chitarra 18:00-18:40", "THURSDAY Corso di Canto 17:00-17:40",
            "SATURDAY Corso di Chitarra 18:00-18:40"), admin.stream().filter(CalendarioServiceTest::diProva)
            .map(CalendarioServiceTest::testo).toList());
        // Dati demo: Propedeutica con due gruppi il sabato, uno per orario
        assertEquals(List.of("SATURDAY Propedeutica musicale 10:00-10:50 Piccoli", "SATURDAY Propedeutica musicale 11:00-11:50 Grandi"),
            admin.stream().filter(v -> v.corso().getNome().equals("Propedeutica musicale")).map(CalendarioServiceTest::testo).toList());
        Corso chitarra = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Chitarra")).findFirst().orElseThrow();
        assertEquals(2, calendarioService.settimanaTipo("segreteria", chitarra.getId()).size());
        // L'insegnante i suoi corsi
        assertEquals(List.of("THURSDAY Corso di Canto 17:00-17:40"),
            calendarioService.settimanaTipo("insegnante1", null).stream().map(CalendarioServiceTest::testo).toList());
        // Il genitore i corsi dei figli, con il nome: genitore3 ha Canto3 (Canto) e Chitarra1 (Chitarra)
        List<VoceSettimana> genitore = calendarioService.settimanaTipo("genitore3", null);
        assertEquals(List.of("TUESDAY Allievo Chitarra1", "THURSDAY Allievo Canto3", "SATURDAY Allievo Chitarra1"),
            genitore.stream().map(v -> v.giorno() + " " + v.perChi()).toList());

        // Con i gruppi del sabato: lo staff una voce per gruppo più chi non ne ha, l'allievo solo il suo gruppo
        GruppoCorso piccoli = corsoService.salvaGruppo(chitarra.getId(), new GruppoCorso(null, "Piccoli", DayOfWeek.SATURDAY, LocalTime.of(10, 0)));
        GruppoCorso grandi = corsoService.salvaGruppo(chitarra.getId(), new GruppoCorso(null, "Grandi", DayOfWeek.SATURDAY, LocalTime.of(11, 0)));
        try {
            assegna(chitarra, "allievo_chitarra1", piccoli);
            assegna(chitarra, "allievo_chitarra3", grandi);
            List<VoceSettimana> insegnante = calendarioService.settimanaTipo("insegnante2", null);
            assertEquals(List.of("TUESDAY Corso di Chitarra 18:00-18:40", "SATURDAY Corso di Chitarra 10:00-10:40 Piccoli",
                "SATURDAY Corso di Chitarra 11:00-11:40 Grandi", "SATURDAY Corso di Chitarra 18:00-18:40 senza gruppo"),
                insegnante.stream().map(CalendarioServiceTest::testo).toList());
            assertEquals(List.of("SATURDAY Corso di Chitarra 11:00-11:40 Grandi"),
                calendarioService.settimanaTipo("allievo_chitarra3", null).stream().map(CalendarioServiceTest::testo).toList());
        } finally {
            corsoService.eliminaGruppo(piccoli.getId());
            corsoService.eliminaGruppo(grandi.getId());
            for (String username : List.of("allievo_chitarra1", "allievo_chitarra3")) {
                corsoService.impostaOrario(chitarra.getId(), allievoService.findByUsername(username).orElseThrow().getId(), null);
            }
        }
    }

    /** Voce dei corsi di prova (Canto e Chitarra), non dei dati demo. */
    private static boolean diProva(VoceSettimana voce) {
        return voce.corso().getNome().startsWith("Corso di");
    }

    private static String testo(VoceSettimana voce) {
        return voce.giorno() + " " + voce.corso().getNome() + " " + voce.inizio() + "-" + voce.fine()
            + (voce.gruppo() != null ? " " + voce.gruppo().getNome() : "") + (voce.senzaGruppo() ? " senza gruppo" : "");
    }

    private void assegna(Corso corso, String username, GruppoCorso gruppo) {
        corsoService.assegnaGruppo(corso.getId(), allievoService.findByUsername(username).orElseThrow().getId(), gruppo.getId());
    }

    private static List<VoceCalendario> delGiorno(List<VoceCalendario> voci, int giorno) {
        return voci.stream().filter(v -> v.lezione().getData().getDayOfMonth() == giorno).toList();
    }

    private static Set<String> corsi(List<VoceCalendario> voci) {
        return voci.stream().map(v -> v.lezione().getCorso().getNome()).collect(Collectors.toSet());
    }
}
