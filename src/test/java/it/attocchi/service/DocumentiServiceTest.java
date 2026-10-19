package it.attocchi.service;

import it.attocchi.entity.Corso;
import it.attocchi.entity.Iscrizione;
import it.attocchi.entity.MetodoPagamento;
import it.attocchi.entity.Pagamento;
import it.attocchi.entity.TipoDocumento;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Documenti sui dati di DataInitializer, con IBAN e codice fiscale dell'associazione di prova. */
@SpringBootTest(properties = {"app.associazione.iban=IT60X0542811101000000123456", "app.associazione.codice-fiscale=90000000000"})
class DocumentiServiceTest {

    @Autowired
    private DocumentiService documentiService;

    @Autowired
    private CorsoService corsoService;

    @Autowired
    private PagamentoService pagamentoService;

    @Autowired
    private GenitoreService genitoreService;

    @Test
    void ricevuta_htmlEPdfConIPagamenti() throws IOException {
        Iscrizione iscrizione = iscrizione();
        Long corsoId = iscrizione.getCorso().getId();
        Long allievoId = iscrizione.getAllievo().getId();
        Pagamento rata = new Pagamento();
        rata.setData(LocalDate.of(2024, 10, 10));
        rata.setImporto(new BigDecimal("150"));
        rata.setMetodo(MetodoPagamento.BONIFICO);
        rata.setNota("prima rata <acconto>");
        pagamentoService.salva(corsoId, allievoId, rata);
        try {
            String html = documentiService.documentoHtml(TipoDocumento.RICEVUTA, corsoId, allievoId);
            assertTrue(html.contains("Ricevuta di pagamento"));
            assertTrue(html.contains(iscrizione.getAllievo().getNomeCompleto()));
            assertTrue(html.contains("150,00 €"));
            // Nei documenti la data ha sempre l'anno
            assertTrue(html.contains("gio 10 ottobre 2024"));
            assertTrue(html.contains("Emessa il " + it.attocchi.views.Formati.dataCompleta(LocalDate.now())));
            assertTrue(html.contains("C.F. 90000000000"));
            // Testo dell'utente sempre escapato
            assertTrue(html.contains("prima rata &lt;acconto&gt;"));

            String testo = testo(documentiService.documentoPdf(TipoDocumento.RICEVUTA, corsoId, allievoId));
            assertTrue(testo.contains("Ricevuta di pagamento"), testo);
            assertTrue(testo.contains("Totale pagato"), testo);
            assertTrue(testo.contains("150,00 €"), testo);
            assertTrue(testo.contains("450,00 €"), testo);
        } finally {
            pagamentoService.findPagamenti(corsoId, allievoId).forEach(pagamentoService::elimina);
        }
    }

    @Test
    void conferma_importoDaPagareEIstruzioniPerIlBonifico() throws IOException {
        Iscrizione iscrizione = iscrizione();
        Long corsoId = iscrizione.getCorso().getId();
        Long allievoId = iscrizione.getAllievo().getId();

        String testo = testo(documentiService.documentoPdf(TipoDocumento.CONFERMA_ISCRIZIONE, corsoId, allievoId));
        assertTrue(testo.contains("Conferma di iscrizione"), testo);
        assertTrue(testo.replaceAll("\\s+", " ").contains("con lezione il giovedì"), testo);
        assertTrue(testo.contains("Da pagare"), testo);
        assertTrue(testo.contains("600,00 €"), testo);
        assertTrue(testo.contains("IT60X0542811101000000123456"), testo);
        assertTrue(testo.contains("Già pagato"), testo);

        String email = documentiService.emailHtml(TipoDocumento.CONFERMA_ISCRIZIONE, corsoId, allievoId, "Mario Rossi");
        assertTrue(email.contains("Gentile Mario Rossi"));
        assertTrue(email.contains("IT60X0542811101000000123456"));
        assertFalse(email.contains("data:image"));
        assertEquals("conferma-iscrizione-corso-di-canto-" + iscrizione.getAllievo().getNomeCompleto().toLowerCase().replace(' ', '-') + ".pdf",
            documentiService.nomeFile(TipoDocumento.CONFERMA_ISCRIZIONE, iscrizione));
    }

    @Test
    void esempio_templateCompilatiConDatiFittizi() throws IOException {
        String email = documentiService.emailEsempioHtml(TipoDocumento.RICEVUTA, "Prova");
        assertTrue(email.contains("Gentile Prova"));
        assertTrue(email.contains("Anna Esempio"));
        assertTrue(email.contains("350,00 €"));

        String conferma = documentiService.emailEsempioHtml(TipoDocumento.CONFERMA_ISCRIZIONE, "Prova");
        assertTrue(conferma.contains("Luca Esempio"));
        assertTrue(conferma.contains("190,00 €"));

        String testo = testo(documentiService.documentoEsempioPdf(TipoDocumento.RICEVUTA));
        assertTrue(testo.contains("Ricevuta di pagamento"), testo);
        assertTrue(testo.contains("Maria Esempio"), testo);
        assertTrue(testo.contains("sconto fratelli"), testo);
        assertEquals("ricevuta-corso-di-esempio-anna-esempio.pdf", documentiService.nomeFileEsempio(TipoDocumento.RICEVUTA));

        String prova = documentiService.emailProvaHtml("Prova");
        assertTrue(prova.contains("Gentile Prova"));
        assertTrue(prova.contains("email di prova"));
    }

    @Test
    void riepilogoFamiglia_corsiDeiFigliConTotaliDellAnno() throws IOException {
        // genitore3: Allievo Canto3 al Corso di Canto e Allievo Chitarra1 al Corso di Chitarra (600 € ciascuno, 2024/2025)
        Long genitoreId = genitoreService.findByUsername("genitore3").orElseThrow().getId();
        assertEquals(List.of("2024/2025"), pagamentoService.anniDellaFamiglia(genitoreId));
        PagamentoService.RigaPagamento chitarra = pagamentoService.righePagamentiDeiFigli("genitore3").stream()
            .filter(r -> r.corso().getNome().equals("Corso di Chitarra")).findFirst().orElseThrow();
        Long corsoId = chitarra.corso().getId();
        Long allievoId = chitarra.allievo().getId();
        Pagamento rata = new Pagamento();
        rata.setData(LocalDate.now());
        rata.setImporto(new BigDecimal("100"));
        rata.setMetodo(MetodoPagamento.CASSA);
        pagamentoService.salva(corsoId, allievoId, rata);
        try {
            PagamentoService.RiepilogoFamiglia riepilogo = pagamentoService.riepilogoFamiglia(genitoreId, "2024/2025");
            assertEquals(2, riepilogo.righe().size());
            assertEquals(0, new BigDecimal("1200").compareTo(riepilogo.quota()));
            assertEquals(0, new BigDecimal("1100").compareTo(riepilogo.residuo()));
            assertTrue(pagamentoService.riepilogoFamiglia(genitoreId, "2023/2024").righe().isEmpty());

            DocumentiService.Pdf pdf = documentiService.riepilogoFamigliaPer(genitoreId, "2024/2025", "genitore3");
            assertEquals("riepilogo-famiglia-genitore3-cognome3-2024-2025.pdf", pdf.nomeFile());
            String testo = testo(pdf.contenuto());
            assertTrue(testo.contains("Riepilogo famiglia"), testo);
            assertTrue(testo.contains("Anno scolastico 2024/2025"), testo);
            assertTrue(testo.contains("Allievo Canto3"), testo);
            assertTrue(testo.contains("Allievo Chitarra1"), testo);
            assertTrue(testo.contains("1.200,00 €"), testo);
            assertTrue(testo.contains("1.100,00 €"), testo);

            // Admin e segreteria ogni famiglia, il genitore solo la propria
            documentiService.riepilogoFamigliaPer(genitoreId, "2024/2025", "segreteria");
            documentiService.riepilogoFamigliaPer(genitoreId, "2024/2025", "admin");
            assertThrows(IllegalArgumentException.class, () -> documentiService.riepilogoFamigliaPer(genitoreId, "2024/2025", "genitore1"));
            assertThrows(IllegalArgumentException.class, () -> documentiService.riepilogoFamigliaPer(genitoreId, "2024/2025", "insegnante1"));
            assertThrows(IllegalArgumentException.class, () -> documentiService.riepilogoFamigliaPer(-1L, "2024/2025", "admin"));
        } finally {
            pagamentoService.findPagamenti(corsoId, allievoId).forEach(pagamentoService::elimina);
        }
    }

    @Test
    void conferma_conIGiorniDelPartecipante() {
        PagamentoService.RigaPagamento chitarra = pagamentoService.righePagamentiDeiFigli("genitore3").stream()
            .filter(r -> r.corso().getNome().equals("Corso di Chitarra")).findFirst().orElseThrow();
        Long corsoId = chitarra.corso().getId();
        Long allievoId = chitarra.allievo().getId();
        assertTrue(documentiService.emailHtml(TipoDocumento.CONFERMA_ISCRIZIONE, corsoId, allievoId, "Prova")
            .contains("con lezione il martedì e sabato"));
        corsoService.impostaGiorni(corsoId, allievoId, java.util.Set.of(java.time.DayOfWeek.SATURDAY));
        try {
            assertTrue(documentiService.emailHtml(TipoDocumento.CONFERMA_ISCRIZIONE, corsoId, allievoId, "Prova")
                .contains("con lezione il sabato."));
        } finally {
            corsoService.impostaGiorni(corsoId, allievoId, java.util.Set.of());
        }
    }

    private Iscrizione iscrizione() {
        Corso canto = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow();
        return corsoService.findPartecipanti(canto.getId()).getFirst();
    }

    private static String testo(byte[] pdf) throws IOException {
        assertEquals("%PDF", new String(pdf, 0, 4));
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(documento);
        }
    }
}
