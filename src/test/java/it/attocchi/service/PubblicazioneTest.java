package it.attocchi.service;

import it.attocchi.entity.Allievo;
import it.attocchi.entity.Corso;
import it.attocchi.entity.TipoDocumento;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Corso non pubblicato sui dati di DataInitializer: genitore3 ha Allievo Canto3 a Canto e Allievo Chitarra1 a
 * Chitarra; con Chitarra non pubblicato la famiglia vede solo Canto, lo staff tutto.
 */
@SpringBootTest
class PubblicazioneTest {

    @Autowired
    private CorsoService corsoService;
    @Autowired
    private AllievoService allievoService;
    @Autowired
    private PagamentoService pagamentoService;
    @Autowired
    private CalendarioService calendarioService;
    @Autowired
    private DashboardService dashboardService;
    @Autowired
    private ElencoDocumentiService elencoDocumentiService;
    @Autowired
    private DocumentiService documentiService;
    @Autowired
    private EmailService emailService;

    @Test
    void corsoNonPubblicato_nascostoAlleFamiglieVisibileAlloStaff() {
        Corso chitarra = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Chitarra")).findFirst().orElseThrow();
        Allievo chitarra1 = allievoService.findByUsername("allievo_chitarra1").orElseThrow();
        YearMonth mese = YearMonth.from(chitarra.getDataInizio().plusDays(7));
        chitarra.setPubblicato(false);
        corsoService.save(chitarra);
        try {
            assertTrue(corsoService.findByAllievo(chitarra1.getId()).isEmpty());
            assertTrue(corsoService.findAttiviByAllievo(chitarra1.getId()).isEmpty());
            assertTrue(pagamentoService.righePagamentiDeiFigli("genitore3").stream()
                .noneMatch(r -> r.corso().getId().equals(chitarra.getId())));
            assertEquals(1, dashboardService.corsiDeiFigli("genitore3"));
            assertEquals(0, dashboardService.corsiAllievo("allievo_chitarra1"));
            assertTrue(calendarioService.lezioniDelMese("genitore3", mese, null).stream()
                .noneMatch(v -> v.lezione().getCorso().getId().equals(chitarra.getId())));
            assertFalse(calendarioService.lezioniDelMese("segreteria", mese, chitarra.getId()).isEmpty());
            assertTrue(elencoDocumentiService.documentiDi("genitore3").stream()
                .noneMatch(d -> d.iscrizione() != null && d.iscrizione().getCorso().getId().equals(chitarra.getId())));

            // Documenti: lo staff sì, la famiglia no; nessun invio alle famiglie
            documentiService.documentoPer(TipoDocumento.CONFERMA_ISCRIZIONE, chitarra.getId(), chitarra1.getId(), "segreteria");
            assertThrows(IllegalArgumentException.class, () -> documentiService.documentoPer(
                TipoDocumento.CONFERMA_ISCRIZIONE, chitarra.getId(), chitarra1.getId(), "genitore3"));
            assertThrows(IllegalStateException.class, () -> emailService.invia(
                TipoDocumento.CONFERMA_ISCRIZIONE, chitarra.getId(), chitarra1.getId(), "segreteria"));
            assertThrows(IllegalStateException.class, () -> emailService.inviaConferme(chitarra.getId(), "segreteria"));
        } finally {
            Corso ripristino = corsoService.findById(chitarra.getId());
            ripristino.setPubblicato(true);
            corsoService.save(ripristino);
        }
        assertEquals(2, dashboardService.corsiDeiFigli("genitore3"));
    }
}
