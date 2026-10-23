package it.attocchi.service;

import it.attocchi.entity.Corso;
import it.attocchi.service.CalendarioService.VoceCalendario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.DayOfWeek;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private static Set<String> corsi(List<VoceCalendario> voci) {
        return voci.stream().map(v -> v.lezione().getCorso().getNome()).collect(Collectors.toSet());
    }
}
