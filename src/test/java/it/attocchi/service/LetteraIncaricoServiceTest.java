package it.attocchi.service;

import it.attocchi.entity.Corso;
import it.attocchi.entity.Insegnante;
import it.attocchi.entity.LetteraIncarico;
import it.attocchi.repository.InvioEmailRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Lettere di incarico sui dati di DataInitializer: insegnante1 (25 €/h) ha il Corso di Canto del giovedì alle 17:00. */
@SpringBootTest
class LetteraIncaricoServiceTest {

    @Autowired
    private LetteraIncaricoService letteraIncaricoService;

    @Autowired
    private DocumentiService documentiService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private InsegnanteService insegnanteService;

    @Autowired
    private CorsoService corsoService;

    @Autowired
    private InvioEmailRepository invioEmailRepository;

    @Test
    void lettera_propostaDaiDatiAttualiPoiFissataEVisibileAllInsegnante() throws IOException {
        Insegnante insegnante = insegnanteService.findByUsername("insegnante1").orElseThrow();
        Corso canto = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow();
        LetteraIncarico lettera = letteraIncaricoService.proposta(insegnante.getId(), canto.getId());
        assertEquals("2024/2025", lettera.getAnnoScolastico());
        assertEquals(0, new BigDecimal("25").compareTo(lettera.getTariffaOraria()));
        assertNull(lettera.getImportoUnaTantum());
        assertEquals("giovedì alle 17:00, lezioni da 40 minuti", lettera.getGiorniEOrari());

        // Solo lo staff emette; l'insegnante deve essere del corso; serve un compenso
        assertThrows(IllegalStateException.class, () -> letteraIncaricoService.salva(lettera, "insegnante1"));
        LetteraIncarico altro = letteraIncaricoService.proposta(insegnanteService.findByUsername("insegnante2").orElseThrow().getId(), canto.getId());
        assertThrows(IllegalArgumentException.class, () -> letteraIncaricoService.salva(altro, "segreteria"));
        lettera.setTariffaOraria(null);
        assertThrows(IllegalArgumentException.class, () -> letteraIncaricoService.salva(lettera, "segreteria"));
        lettera.setTariffaOraria(new BigDecimal("25"));
        lettera.setImportoUnaTantum(new BigDecimal("300"));
        LetteraIncarico salvata = letteraIncaricoService.salva(lettera, "segreteria");
        try {
            // Cambia la tariffa dell'insegnante: la lettera resta com'era
            insegnante.setTariffaOraria(new BigDecimal("30"));
            insegnanteService.save(insegnante);
            DocumentiService.Pdf pdf = documentiService.letteraPer(salvata.getId(), "insegnante1");
            String testo = testo(pdf.contenuto());
            assertTrue(testo.contains("Lettera di incarico"), testo);
            assertTrue(testo.contains("Corso di Canto"), testo);
            assertTrue(testo.contains("25,00 € all'ora"), testo);
            assertTrue(testo.contains("300,00 € per l'anno"), testo);
            assertTrue(testo.contains("2024/2025"), testo);

            assertEquals(1, letteraIncaricoService.dellInsegnante("insegnante1").size());
            assertTrue(letteraIncaricoService.dellInsegnante("insegnante2").isEmpty());
            assertThrows(IllegalArgumentException.class, () -> documentiService.letteraPer(salvata.getId(), "insegnante2"));
            assertThrows(IllegalArgumentException.class, () -> documentiService.letteraPer(salvata.getId(), "genitore1"));
            assertTrue(insegnanteService.motivoPerNonEliminare(insegnante.getId()) != null);

            emailService.inviaLettera(salvata.getId(), "segreteria");
            assertEquals(1, invioEmailRepository.findAll().stream()
                .filter(i -> i.getTipo() == it.attocchi.entity.TipoDocumento.LETTERA_INCARICO).count());
            assertThrows(IllegalStateException.class, () -> letteraIncaricoService.elimina(salvata.getId(), "segreteria"));
        } finally {
            insegnante.setTariffaOraria(new BigDecimal("25"));
            insegnanteService.save(insegnante);
            letteraIncaricoService.elimina(salvata.getId(), "admin");
            invioEmailRepository.deleteAll();
        }
    }

    private static String testo(byte[] pdf) throws IOException {
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(documento);
        }
    }
}
