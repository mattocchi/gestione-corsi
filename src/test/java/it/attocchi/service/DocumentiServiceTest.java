package it.attocchi.service;

import it.attocchi.entity.Allievo;
import it.attocchi.entity.Corso;
import it.attocchi.entity.Iscrizione;
import it.attocchi.entity.MetodoPagamento;
import it.attocchi.entity.Pagamento;
import it.attocchi.entity.TipoDocumento;
import it.attocchi.views.Formati;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Autowired
    private AllievoService allievoService;

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
            assertTrue(testo.contains("Già pagato"), testo);
            assertTrue(testo.contains("DA PAGARE"), testo);
            assertTrue(testo.contains("150,00 €"), testo);
            assertTrue(testo.contains("450,00 €"), testo);
            // La ricevuta riporta gli insegnanti, la conferma no
            assertTrue(testo.contains("Insegnante " + iscrizione.getCorso().getNomiInsegnanti()), testo);
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
        assertFalse(testo.contains(iscrizione.getCorso().getNomiInsegnanti()), testo);
        assertTrue(testo.replaceAll("\\s+", " ").contains("con lezione il giovedì"), testo);
        assertTrue(testo.contains("DA PAGARE"), testo);
        assertTrue(testo.contains("QUOTA DOVUTA"), testo);
        // Senza sconto niente listino
        assertFalse(testo.contains("Prezzo di listino"), testo);
        assertTrue(testo.contains("600,00 €"), testo);
        assertTrue(testo.contains("IT60X0542811101000000123456"), testo);
        assertTrue(testo.contains("Già pagato"), testo);
        // Unica soluzione o due rate, con gli anni dell'anno scolastico 2024/2025
        assertTrue(testo.replaceAll("\\s+", " ").contains("in un'unica soluzione entro il 30 novembre 2024 oppure in due rate: "
            + "la prima entro il 30 novembre 2024 e la seconda entro il 31 gennaio 2025."), testo);
        // Regole su recuperi, assenze e rinuncia, uguali nel PDF e nell'email
        String regole = testo.replaceAll("\\s+", " ");
        assertTrue(regole.contains("Lezioni, assenze e recuperi"), testo);
        assertTrue(regole.contains("Se l'insegnante non può fare lezione"), testo);
        assertTrue(regole.contains("al termine del corso: il numero di lezioni previsto è garantito"), testo);
        assertTrue(regole.contains("Assenze dell’allievo (solo lezioni individuali)"), testo);
        assertTrue(regole.contains("sempre tramite la community WhatsApp"), testo);
        assertTrue(regole.contains("Con un preavviso di almeno una settimana, il recupero della lezione è garantito"), testo);
        assertTrue(regole.contains("la lezione verrà comunque conteggiata e dovrà essere pagata"), testo);
        assertTrue(regole.contains("valutate caso per caso dalla segreteria dei corsi."), testo);
        assertFalse(regole.contains("insindacabile"), testo);

        String email = documentiService.emailHtml(TipoDocumento.CONFERMA_ISCRIZIONE, corsoId, allievoId, "Mario Rossi");
        assertTrue(email.contains("Gentile Mario Rossi"));
        assertTrue(email.contains("IT60X0542811101000000123456"));
        assertTrue(email.contains("la seconda entro il 31 gennaio 2025"));
        String regoleEmail = email.replaceAll("\\s+", " ");
        assertTrue(regoleEmail.contains("Lezioni, assenze e recuperi"));
        assertTrue(regoleEmail.contains("Assenze dell’allievo (solo lezioni individuali)"));
        assertTrue(regoleEmail.contains("il recupero sarà difficilmente realizzabile"));
        assertTrue(regoleEmail.contains("Le lezioni non frequentate non vengono rimborsate"));
        assertFalse(email.contains("data:image"));
        assertFalse(email.contains(iscrizione.getCorso().getNomiInsegnanti()));
        assertEquals("conferma-iscrizione-corso-di-canto-" + iscrizione.getAllievo().getNomeCompleto().toLowerCase().replace(' ', '-') + ".pdf",
            documentiService.nomeFile(TipoDocumento.CONFERMA_ISCRIZIONE, iscrizione));
    }

    @Test
    void conferma_suUnaSolaPaginaAncheConScontoGenitoreEBonifico() throws IOException {
        // Il caso più lungo: sconto con motivo (listino, sconto e nota), genitore, IBAN, rate e regole
        Iscrizione iscrizione = iscrizione();
        Long corsoId = iscrizione.getCorso().getId();
        Long allievoId = iscrizione.getAllievo().getId();
        pagamentoService.impostaQuota(corsoId, allievoId, new BigDecimal("450"), "fratelli iscritti allo stesso corso");
        try {
            byte[] pdf = documentiService.documentoPdf(TipoDocumento.CONFERMA_ISCRIZIONE, corsoId, allievoId);
            if (System.getProperty("conferma.pdf") != null) {
                java.nio.file.Files.write(java.nio.file.Path.of(System.getProperty("conferma.pdf")), pdf);
            }
            try (PDDocument documento = Loader.loadPDF(pdf)) {
                assertEquals(1, documento.getNumberOfPages());
            }
            String testo = testo(pdf);
            assertTrue(testo.contains("Genitore"), testo);
            assertTrue(testo.contains("IT60X0542811101000000123456"), testo);
            assertTrue(testo.contains("Data di nascita " + Formati.dataNumerica(iscrizione.getAllievo().getDataNascita())), testo);
        } finally {
            pagamentoService.impostaQuota(corsoId, allievoId, null, null);
        }
    }

    @Test
    void conferma_senzaCodiceFiscaleDellAllievo_senzaLaRiga() throws IOException {
        Iscrizione iscrizione = iscrizione();
        Allievo allievo = allievoService.findById(iscrizione.getAllievo().getId());
        String codiceFiscale = allievo.getCodiceFiscale();
        allievo.setCodiceFiscale(null);
        allievoService.save(allievo);
        try {
            String testo = testo(documentiService.documentoPdf(TipoDocumento.CONFERMA_ISCRIZIONE,
                iscrizione.getCorso().getId(), allievo.getId()));
            assertFalse(testo.contains(codiceFiscale), testo);
            // Resta solo il codice fiscale del genitore
            assertEquals(1, testo.split("Codice fiscale", -1).length - 1, testo);
        } finally {
            allievo.setCodiceFiscale(codiceFiscale);
            allievoService.save(allievo);
        }
    }

    @Test
    void avvisoDatiMancanti_diceACosaServono() {
        Allievo allievo = allievoService.findById(iscrizione().getAllievo().getId());
        allievo.setCodiceFiscale(null);
        assertEquals("Manca codice fiscale di " + allievo.getNomeCompleto() + ": serve per le ricevute",
            DocumentiService.avvisoDatiMancanti(allievo));
        assertNull(DocumentiService.avvisoDatiMancanti(allievo, TipoDocumento.CONFERMA_ISCRIZIONE));
        allievo.setDataNascita(null);
        assertEquals("Mancano codice fiscale e data di nascita di " + allievo.getNomeCompleto() + ": la data di nascita "
            + "serve per conferme e ricevute, il codice fiscale per le ricevute", DocumentiService.avvisoDatiMancanti(allievo));
    }

    @Test
    void modalitaPagamento_senzaAnnoScolasticoLeDateSenzaAnno() {
        assertEquals("Il pagamento può avvenire in un'unica soluzione entro il 30 novembre oppure in due rate: la prima "
            + "entro il 30 novembre e la seconda entro il 31 gennaio.", DocumentiService.modalitaPagamento(null));
        assertTrue(DocumentiService.modalitaPagamento("2026/2027").endsWith("entro il 31 gennaio 2027."));
    }

    @Test
    void scontoApplicato_listinoBarratoScontoEQuotaDovuta() throws IOException {
        Iscrizione iscrizione = iscrizione();
        Long corsoId = iscrizione.getCorso().getId();
        Long allievoId = iscrizione.getAllievo().getId();
        pagamentoService.impostaQuota(corsoId, allievoId, new BigDecimal("450"), "fratelli");
        try {
            String testo = testo(documentiService.documentoPdf(TipoDocumento.CONFERMA_ISCRIZIONE, corsoId, allievoId))
                .replaceAll("\\s+", " ");
            assertTrue(testo.contains("Prezzo di listino del corso 600,00 €"), testo);
            assertTrue(testo.contains("Sconto applicato (fratelli) -150,00 €"), testo);
            assertTrue(testo.contains("QUOTA DOVUTA 450,00 €"), testo);
            assertFalse(testo.contains("solo un riferimento"), testo);

            String email = documentiService.emailHtml(TipoDocumento.RICEVUTA, corsoId, allievoId, "Mario Rossi");
            assertTrue(email.contains("<s>600,00 €</s>"));
            assertTrue(email.contains("Sconto applicato (fratelli)"));

            // Senza motivo, con la quota uguale al prezzo di pre-iscrizione (510 €)
            it.attocchi.entity.Corso corso = corsoService.findById(corsoId);
            corso.setPrezzoPreiscrizione(new BigDecimal("510.00"));
            corsoService.save(corso);
            pagamentoService.impostaQuota(corsoId, allievoId, new BigDecimal("510"), null);
            String preiscrizione = documentiService.emailHtml(TipoDocumento.CONFERMA_ISCRIZIONE, corsoId, allievoId, "Mario Rossi");
            assertTrue(preiscrizione.contains("Sconto applicato (pre-iscrizione)"), preiscrizione);
            assertTrue(preiscrizione.contains("-90,00 €"));
        } finally {
            pagamentoService.impostaQuota(corsoId, allievoId, null, null);
            it.attocchi.entity.Corso corso = corsoService.findById(corsoId);
            corso.setPrezzoPreiscrizione(null);
            corsoService.save(corso);
        }
    }

    @Test
    void esempio_templateCompilatiConDatiFittizi() throws IOException {
        String email = documentiService.emailEsempioHtml(TipoDocumento.RICEVUTA, "Prova");
        assertTrue(email.contains("Gentile Prova"));
        assertTrue(email.contains("Anna Esempio"));
        assertTrue(email.contains("350,00 €"));

        String conferma = documentiService.emailEsempioHtml(TipoDocumento.CONFERMA_ISCRIZIONE, "Prova");
        assertTrue(conferma.contains("Anna Esempio"));
        // L'insegnante non è nella conferma
        assertFalse(conferma.contains("Luca Esempio"));
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
    void conferma_vistaDalGenitoreDelFiglioEDalloStaff() throws IOException {
        it.attocchi.entity.Allievo allievo = allievoService.findByUsername("allievo_canto3").orElseThrow();
        Long cantoId = iscrizione().getCorso().getId();
        DocumentiService.Pdf pdf = documentiService.confermaPer(cantoId, allievo.getId(), "genitore3");
        assertTrue(pdf.nomeFile().startsWith("conferma-iscrizione-"), pdf.nomeFile());
        assertTrue(testo(pdf.contenuto()).contains("Allievo Canto3"));
        assertNotNull(documentiService.confermaPer(cantoId, allievo.getId(), "segreteria"));
        // Un altro genitore, l'insegnante e l'allievo con genitore no
        assertThrows(IllegalArgumentException.class, () -> documentiService.confermaPer(cantoId, allievo.getId(), "genitore1"));
        assertThrows(IllegalArgumentException.class, () -> documentiService.confermaPer(cantoId, allievo.getId(), "insegnante1"));
        assertThrows(IllegalArgumentException.class, () -> documentiService.confermaPer(cantoId, allievo.getId(), "allievo_canto3"));
        assertThrows(IllegalArgumentException.class,
            () -> documentiService.documentoPer(TipoDocumento.RIEPILOGO, cantoId, allievo.getId(), "segreteria"));
    }

    @Test
    void allievoAdulto_vedeIPropriPagamentiERiepilogo() throws IOException {
        it.attocchi.entity.Allievo allievo = allievoService.findByUsername("allievo_canto1").orElseThrow();
        it.attocchi.entity.Genitore genitore = allievo.getGenitore();
        Long cantoId = iscrizione().getCorso().getId();
        // Con il genitore: niente pagamenti propri, ricevuta solo al genitore
        assertFalse(allievoService.gestisceDaSe("allievo_canto1"));
        assertTrue(pagamentoService.righePagamentiPropri("allievo_canto1").isEmpty());
        assertThrows(IllegalArgumentException.class, () -> documentiService.ricevutaPer(cantoId, allievo.getId(), "allievo_canto1"));
        allievo.setGenitore(null);
        allievoService.save(allievo);
        try {
            assertTrue(allievoService.gestisceDaSe("allievo_canto1"));
            assertEquals(1, pagamentoService.righePagamentiPropri("allievo_canto1").size());
            assertEquals(0, new BigDecimal("600").compareTo(pagamentoService.situazionePropria("allievo_canto1").orElseThrow().quota()));
            assertEquals(java.util.List.of("2024/2025"), pagamentoService.anniDellAllievo(allievo.getId()));
            assertNotNull(documentiService.ricevutaPer(cantoId, allievo.getId(), "allievo_canto1"));
            // Un altro allievo non vede i suoi documenti
            assertThrows(IllegalArgumentException.class, () -> documentiService.ricevutaPer(cantoId, allievo.getId(), "allievo_canto2"));

            DocumentiService.Pdf pdf = documentiService.riepilogoAllievoPer(allievo.getId(), "2024/2025", "allievo_canto1");
            assertEquals("riepilogo-pagamenti-allievo-canto1-2024-2025.pdf", pdf.nomeFile());
            String testo = testo(pdf.contenuto()).replaceAll("\\s+", " ");
            assertTrue(testo.contains("Riepilogo pagamenti"), testo);
            assertTrue(testo.contains("Allievo Allievo Canto1"), testo);
            assertTrue(testo.contains("Totale spesa (1 corso)"), testo);
            assertThrows(IllegalArgumentException.class,
                () -> documentiService.riepilogoAllievoPer(allievo.getId(), "2024/2025", "allievo_canto2"));
        } finally {
            it.attocchi.entity.Allievo ripristino = allievoService.findById(allievo.getId());
            ripristino.setGenitore(genitore);
            allievoService.save(ripristino);
        }
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
        var gruppo = corsoService.salvaGruppo(corsoId, new it.attocchi.entity.GruppoCorso(null, null,
            java.time.DayOfWeek.SATURDAY, java.time.LocalTime.of(10, 0)));
        corsoService.assegnaGruppo(corsoId, allievoId, gruppo.getId());
        try {
            assertTrue(documentiService.emailHtml(TipoDocumento.CONFERMA_ISCRIZIONE, corsoId, allievoId, "Prova")
                .contains("con lezione il sabato alle 10:00."));
        } finally {
            corsoService.assegnaGruppo(corsoId, allievoId, null);
            corsoService.impostaOrario(corsoId, allievoId, null);
            corsoService.eliminaGruppo(gruppo.getId());
        }
    }

    private Iscrizione iscrizione() {
        Corso canto = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow();
        return corsoService.findPartecipanti(canto.getId()).getFirst();
    }

    private static String testo(byte[] pdf) throws IOException {
        assertEquals("%PDF", new String(pdf, 0, 4));
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            // In ordine di posizione: senza, i tratti in grassetto finiscono in fondo alla riga
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(documento);
        }
    }
}
