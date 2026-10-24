package it.attocchi.service;

import it.attocchi.entity.Corso;
import it.attocchi.entity.GruppoCorso;
import it.attocchi.entity.Lezione;
import it.attocchi.service.CalendarioService.VoceCalendario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.DayOfWeek;
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

    @Autowired
    private AllievoService allievoService;

    @Test
    void prossimaLezione_laPrimaDaFareDaOggiPerCorso() {
        Corso canto = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow();
        // Nessuna lezione futura: niente da mostrare (quelle passate non completate non contano)
        assertTrue(dashboardService.prossimeLezioniInsegnante("insegnante1").isEmpty());

        // All'orario del corso (17:00): ogni allievo la vede al suo orario
        Lezione dopo = lezioneService.aggiungi(canto.getId(), LocalDate.now().plusDays(10), null, null, "insegnante1");
        Lezione oggi = lezioneService.aggiungi(canto.getId(), LocalDate.now(), null, null, "insegnante1");
        try {
            List<VoceCalendario> insegnante = dashboardService.prossimeLezioniInsegnante("insegnante1");
            // Una per corso: quella di oggi
            assertEquals(1, insegnante.size());
            assertEquals(oggi.getId(), insegnante.getFirst().lezione().getId());
            assertEquals(LocalTime.of(17, 0), insegnante.getFirst().orario());
            assertNull(insegnante.getFirst().perChi());
            assertTrue(dashboardService.prossimeLezioniInsegnante("insegnante2").isEmpty());

            // L'allievo con l'orario della sua lezione
            List<VoceCalendario> allievo = dashboardService.prossimeLezioniAllievo("allievo_canto5");
            assertEquals(1, allievo.size());
            assertEquals(LocalTime.of(17, 0), allievo.getFirst().orario());

            // Il genitore: una per figlio e corso, con il nome (Chitarra1 non ha lezioni future)
            List<VoceCalendario> figli = dashboardService.prossimeLezioniDeiFigli("genitore3");
            assertEquals(1, figli.size());
            assertEquals("Allievo Canto3", figli.getFirst().perChi());
            assertEquals(LocalTime.of(18, 20), figli.getFirst().orario());

            // Completata quella di oggi, la prossima è l'altra
            lezioneService.completa(oggi.getId(), "insegnante1");
            assertEquals(dopo.getId(), dashboardService.prossimeLezioniInsegnante("insegnante1").getFirst().lezione().getId());
        } finally {
            lezioneService.eliminaAggiunta(dopo.getId(), "insegnante1");
            lezioneService.findByCorso(canto.getId()).stream().filter(l -> l.getId().equals(oggi.getId())).findFirst()
                .ifPresent(l -> lezioneService.delete(l));
        }
    }

    @Test
    void prossimaLezione_conDueGruppiOgnunoConIlSuoOrario() {
        // Chitarra (insegnante2): il prossimo sabato Piccoli alle 10 (Chitarra1, figlio di genitore3) e Grandi alle 11
        // (Chitarra3); gli altri senza gruppo
        Corso chitarra = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Chitarra")).findFirst().orElseThrow();
        GruppoCorso piccoli = corsoService.salvaGruppo(chitarra.getId(), new GruppoCorso(null, "Piccoli", DayOfWeek.SATURDAY, LocalTime.of(10, 0)));
        GruppoCorso grandi = corsoService.salvaGruppo(chitarra.getId(), new GruppoCorso(null, "Grandi", DayOfWeek.SATURDAY, LocalTime.of(11, 0)));
        Long chitarra1 = allievoService.findByUsername("allievo_chitarra1").orElseThrow().getId();
        Long chitarra3 = allievoService.findByUsername("allievo_chitarra3").orElseThrow().getId();
        corsoService.assegnaGruppo(chitarra.getId(), chitarra1, piccoli.getId());
        corsoService.assegnaGruppo(chitarra.getId(), chitarra3, grandi.getId());
        // Senza un orario proprio: vale quello del gruppo
        corsoService.impostaOrario(chitarra.getId(), chitarra1, null);
        corsoService.impostaOrario(chitarra.getId(), chitarra3, null);
        LocalDate sabato = LocalDate.now().plusDays(1);
        while (sabato.getDayOfWeek() != DayOfWeek.SATURDAY) {
            sabato = sabato.plusDays(1);
        }
        // All'orario del corso: per ognuno vale quello del suo gruppo
        Lezione lezione = lezioneService.aggiungi(chitarra.getId(), sabato, null, null, "insegnante2");
        try {
            // L'insegnante: una voce per gruppo con il suo orario, più chi non ha gruppo
            List<VoceCalendario> insegnante = dashboardService.prossimeLezioniInsegnante("insegnante2");
            assertEquals(3, insegnante.size());
            assertEquals(piccoli.getId(), insegnante.get(0).gruppo().getId());
            assertEquals(LocalTime.of(10, 0), insegnante.get(0).orario());
            assertEquals(grandi.getId(), insegnante.get(1).gruppo().getId());
            assertEquals(LocalTime.of(11, 0), insegnante.get(1).orario());
            assertTrue(insegnante.get(2).senzaGruppo());

            // L'allievo dei Grandi: alle 11, con il gruppo
            VoceCalendario allievo = dashboardService.prossimeLezioniAllievo("allievo_chitarra3").getFirst();
            assertEquals(LocalTime.of(11, 0), allievo.orario());
            assertEquals(grandi.getId(), allievo.gruppo().getId());

            // Il genitore: il figlio dei Piccoli alle 10
            VoceCalendario figlio = dashboardService.prossimeLezioniDeiFigli("genitore3").stream()
                .filter(v -> "Allievo Chitarra1".equals(v.perChi())).findFirst().orElseThrow();
            assertEquals(LocalTime.of(10, 0), figlio.orario());
            assertEquals(piccoli.getId(), figlio.gruppo().getId());
        } finally {
            lezioneService.delete(lezioneService.findById(lezione.getId()));
            corsoService.eliminaGruppo(piccoli.getId());
            corsoService.eliminaGruppo(grandi.getId());
        }
    }
}
