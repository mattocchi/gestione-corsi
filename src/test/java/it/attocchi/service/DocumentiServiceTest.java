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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
