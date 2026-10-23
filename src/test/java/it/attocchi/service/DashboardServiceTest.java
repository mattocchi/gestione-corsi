package it.attocchi.service;

import it.attocchi.entity.Corso;
import it.attocchi.entity.Lezione;
import it.attocchi.service.DashboardService.ProssimaLezione;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prossime lezioni della dashboard sui dati di DataInitializer: le lezioni generate sono del 2024, quindi le
 * "prossime" sono lezioni aggiunte qui nel futuro (e tolte alla fine). Canto: insegnante1, Canto5 alle 17:00.
 */
@SpringBootTest
class DashboardServiceTest {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private LezioneService lezioneService;

    @Autowired
    private CorsoService corsoService;

    @Test
    void prossimaLezione_laPrimaDaFareDaOggiPerCorso() {
        Corso canto = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow();
        // Nessuna lezione futura: niente da mostrare (quelle passate non completate non contano)
        assertTrue(dashboardService.prossimeLezioniInsegnante("insegnante1").isEmpty());

        Lezione dopo = lezioneService.aggiungi(canto.getId(), LocalDate.now().plusDays(10), LocalTime.of(18, 0), null, "insegnante1");
        Lezione oggi = lezioneService.aggiungi(canto.getId(), LocalDate.now(), LocalTime.of(18, 0), null, "insegnante1");
        try {
            List<ProssimaLezione> insegnante = dashboardService.prossimeLezioniInsegnante("insegnante1");
            // Una per corso: quella di oggi
            assertEquals(1, insegnante.size());
            assertEquals(oggi.getId(), insegnante.getFirst().lezione().getId());
            assertEquals(LocalTime.of(18, 0), insegnante.getFirst().orario());
            assertNull(insegnante.getFirst().perChi());
            assertTrue(dashboardService.prossimeLezioniInsegnante("insegnante2").isEmpty());

            // L'allievo con l'orario della sua lezione
            List<ProssimaLezione> allievo = dashboardService.prossimeLezioniAllievo("allievo_canto5");
            assertEquals(1, allievo.size());
            assertEquals(LocalTime.of(17, 0), allievo.getFirst().orario());

            // Il genitore: una per figlio e corso, con il nome (Chitarra1 non ha lezioni future)
            List<ProssimaLezione> figli = dashboardService.prossimeLezioniDeiFigli("genitore3");
            assertEquals(1, figli.size());
            assertEquals("Allievo Canto3", figli.getFirst().perChi());

            // Completata quella di oggi, la prossima è l'altra
            lezioneService.completa(oggi.getId(), "insegnante1");
            assertEquals(dopo.getId(), dashboardService.prossimeLezioniInsegnante("insegnante1").getFirst().lezione().getId());
        } finally {
            lezioneService.eliminaAggiunta(dopo.getId(), "insegnante1");
            lezioneService.findByCorso(canto.getId()).stream().filter(l -> l.getId().equals(oggi.getId())).findFirst()
                .ifPresent(l -> lezioneService.delete(l));
        }
    }
}
