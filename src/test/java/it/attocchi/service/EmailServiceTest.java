package it.attocchi.service;

import it.attocchi.entity.Allievo;
import it.attocchi.entity.Corso;
import it.attocchi.entity.InvioEmail;
import it.attocchi.entity.InvioEmail.Esito;
import it.attocchi.entity.MetodoPagamento;
import it.attocchi.entity.Pagamento;
import it.attocchi.entity.TipoDocumento;
import it.attocchi.repository.InvioEmailRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Invio senza SMTP configurato (configurazione dei test): simulato e registrato. */
@SpringBootTest
class EmailServiceTest {

    @Autowired
    private EmailService emailService;

    @Autowired
    private CorsoService corsoService;

    @Autowired
    private AllievoService allievoService;

    @Autowired
    private PagamentoService pagamentoService;

    @Autowired
    private InvioEmailRepository invioEmailRepository;

    @Autowired
    private DocumentiService documentiService;

    @Autowired
    private GenitoreService genitoreService;

    @AfterEach
    void pulisci() {
        invioEmailRepository.deleteAll();
    }

    @Test
    void conferma_simulataERegistrataConIMetadati() {
        Corso canto = canto();
        Allievo allievo = allievoService.findByUsername("allievo_canto3").orElseThrow();
        assertFalse(emailService.invioReale());
        assertEquals("genitore3@example.com", emailService.destinatario(canto.getId(), allievo.getId()).orElseThrow().email());

        InvioEmail invio = emailService.invia(TipoDocumento.CONFERMA_ISCRIZIONE, canto.getId(), allievo.getId(), "segreteria");
        assertEquals(Esito.SIMULATA, invio.getEsito());
        assertEquals("genitore3@example.com", invio.getDestinatario());
        assertEquals("Corso di Canto", invio.getCorsoNome());
        assertEquals("Allievo Canto3", invio.getAllievoNome());
        assertEquals("segreteria", invio.getInviatoDa());
        assertEquals("Conferma di iscrizione – Corso di Canto 2024/2025 – Allievo Canto3", invio.getOggetto());
        assertTrue(invio.getAllegato().endsWith(".pdf"));
        assertNull(invio.getErrore());
        assertEquals(1, emailService.invii(canto.getId(), allievo.getId()).size());
    }

    @Test
    void ricevuta_soloConPagamentiESoloAdminOSegreteria() {
        Corso canto = canto();
        Allievo allievo = allievoService.findByUsername("allievo_canto3").orElseThrow();
        assertThrows(IllegalStateException.class,
            () -> emailService.invia(TipoDocumento.RICEVUTA, canto.getId(), allievo.getId(), "admin"));
        assertThrows(IllegalStateException.class,
            () -> emailService.invia(TipoDocumento.CONFERMA_ISCRIZIONE, canto.getId(), allievo.getId(), "insegnante1"));
        assertEquals(0, emailService.tuttiGliInvii().size());

        Pagamento rata = new Pagamento();
        rata.setData(LocalDate.now());
        rata.setImporto(new BigDecimal("100"));
        rata.setMetodo(MetodoPagamento.CASSA);
        pagamentoService.salva(canto.getId(), allievo.getId(), rata);
        try {
            assertEquals(Esito.SIMULATA, emailService.invia(TipoDocumento.RICEVUTA, canto.getId(), allievo.getId(), "admin").getEsito());
        } finally {
            pagamentoService.findPagamenti(canto.getId(), allievo.getId()).forEach(pagamentoService::elimina);
        }
    }

    @Test
    void anteprima_comeLInvioMaSenzaInviareNeRegistrare() {
        Corso canto = canto();
        Allievo allievo = allievoService.findByUsername("allievo_canto3").orElseThrow();
        EmailService.Anteprima anteprima = emailService.anteprima(TipoDocumento.CONFERMA_ISCRIZIONE, canto.getId(),
            allievo.getId(), "segreteria");
        assertEquals("genitore3@example.com", anteprima.destinatario().email());
        assertEquals("Conferma di iscrizione – Corso di Canto 2024/2025 – Allievo Canto3", anteprima.oggetto());
        assertEquals(documentiService.emailHtml(TipoDocumento.CONFERMA_ISCRIZIONE, canto.getId(), allievo.getId(),
            anteprima.destinatario().nome()), anteprima.corpoHtml());
        assertTrue(anteprima.allegato().endsWith(".pdf"));
        assertTrue(anteprima.pdf().length > 0);
        assertEquals(0, emailService.tuttiGliInvii().size());

        // Stessi controlli dell'invio: ricevuta senza pagamenti, utente non autorizzato
        assertThrows(IllegalStateException.class,
            () -> emailService.anteprima(TipoDocumento.RICEVUTA, canto.getId(), allievo.getId(), "segreteria"));
        assertThrows(IllegalStateException.class,
            () -> emailService.anteprima(TipoDocumento.CONFERMA_ISCRIZIONE, canto.getId(), allievo.getId(), "insegnante1"));
    }

    @Test
    void destinatariConferme_chiLaRiceveEChiESaltato() {
        Allievo allievo = allievoService.findByUsername("allievo_canto6").orElseThrow();
        String codiceFiscale = allievo.getCodiceFiscale();
        allievo.setCodiceFiscale(null);
        allievoService.save(allievo);
        try {
            List<EmailService.DestinatarioConferma> destinatari = emailService.destinatariConferme(canto().getId(), "segreteria");
            assertEquals(7, destinatari.size());
            // Nell'ordine dei partecipanti: prima per orario (Canto5 alle 17:00)
            assertEquals("Allievo Canto5", destinatari.getFirst().allievo());
            assertTrue(destinatari.getFirst().riceve());
            EmailService.DestinatarioConferma saltato = destinatari.stream()
                .filter(d -> d.allievo().equals("Allievo Canto6")).findFirst().orElseThrow();
            assertFalse(saltato.riceve());
            assertEquals("senza codice fiscale o data di nascita", saltato.motivoEsclusione());
            assertEquals(6, destinatari.stream().filter(EmailService.DestinatarioConferma::riceve).count());
            assertEquals(0, emailService.tuttiGliInvii().size());
        } finally {
            Allievo ripristino = allievoService.findById(allievo.getId());
            ripristino.setCodiceFiscale(codiceFiscale);
            allievoService.save(ripristino);
        }
    }

    @Test
    void inviaConferme_aTuttiGliIscrittiDelCorso() {
        EmailService.Riepilogo riepilogo = emailService.inviaConferme(canto().getId(), "segreteria");
        assertEquals(new EmailService.Riepilogo(0, 7, 0, 0, 0), riepilogo);
        assertEquals(7, emailService.tuttiGliInvii().size());
    }

    @Test
    void senzaCodiceFiscale_niente_documentiNeEmail() {
        Corso canto = canto();
        Allievo allievo = allievoService.findByUsername("allievo_canto3").orElseThrow();
        String codiceFiscale = allievo.getCodiceFiscale();
        allievo.setCodiceFiscale(null);
        allievoService.save(allievo);
        try {
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> emailService.invia(TipoDocumento.CONFERMA_ISCRIZIONE, canto.getId(), allievo.getId(), "segreteria"));
            assertEquals("Manca codice fiscale di Allievo Canto3: completa i dati nella scheda dell'allievo per stampare o inviare"
                + " ricevute e conferme", ex.getMessage());
            assertThrows(IllegalStateException.class,
                () -> documentiService.documentoPdf(TipoDocumento.CONFERMA_ISCRIZIONE, canto.getId(), allievo.getId()));
            assertEquals(new EmailService.Riepilogo(0, 6, 0, 0, 1), emailService.inviaConferme(canto.getId(), "segreteria"));
        } finally {
            allievo.setCodiceFiscale(codiceFiscale);
            allievoService.save(allievo);
        }
    }

    @Test
    void riepilogo_dellaFamigliaSimulatoERegistrato() {
        // genitore3: Allievo Canto3 (Canto) e Allievo Chitarra1 (Chitarra), entrambi nel 2024/2025
        Long genitoreId = genitoreService.findByUsername("genitore3").orElseThrow().getId();
        assertEquals("genitore3@example.com", emailService.destinatarioRiepilogo(genitoreId, null, "2024/2025").orElseThrow().email());

        EmailService.Anteprima anteprima = emailService.anteprimaRiepilogo(genitoreId, null, "2024/2025", "segreteria");
        assertTrue(anteprima.oggetto().startsWith("Riepilogo pagamenti 2024/2025 – "), anteprima.oggetto());
        assertTrue(anteprima.corpoHtml().contains("anno scolastico 2024/2025 per i corsi dei tuoi figli"), anteprima.corpoHtml());
        assertTrue(anteprima.corpoHtml().contains("(2 corsi)"), anteprima.corpoHtml());
        assertTrue(anteprima.allegato().startsWith("riepilogo-famiglia-"), anteprima.allegato());
        assertTrue(emailService.tuttiGliInvii().isEmpty());

        InvioEmail invio = emailService.inviaRiepilogo(genitoreId, null, "2024/2025", "segreteria");
        assertEquals(Esito.SIMULATA, invio.getEsito());
        assertEquals(TipoDocumento.RIEPILOGO, invio.getTipo());
        assertEquals(genitoreId, invio.getGenitoreId());
        assertNull(invio.getAllievoId());
        assertEquals("2024/2025", invio.getAnno());
        assertEquals("genitore3@example.com", invio.getDestinatario());
        assertEquals(1, emailService.tuttiGliInvii().size());

        // Solo admin e segreteria; un anno senza corsi non ha nulla da inviare
        assertThrows(IllegalStateException.class, () -> emailService.inviaRiepilogo(genitoreId, null, "2024/2025", "insegnante1"));
        assertThrows(IllegalStateException.class, () -> emailService.inviaRiepilogo(genitoreId, null, "2030/2031", "segreteria"));
        assertThrows(IllegalArgumentException.class, () -> emailService.inviaRiepilogo(null, null, "2024/2025", "segreteria"));
    }

    @Test
    void test_soloAdminSimulataENonRegistrata() {
        EmailService.EsitoTest esito = emailService.inviaTest(" prova@example.org ", TipoDocumento.RICEVUTA, "admin");
        assertEquals(Esito.SIMULATA, esito.esito());
        assertEquals("[TEST] Ricevuta di pagamento – esempio", esito.oggetto());
        assertNull(esito.errore());
        assertEquals(Esito.SIMULATA, emailService.inviaTest("prova@example.org", null, "admin").esito());
        assertTrue(emailService.tuttiGliInvii().isEmpty());
        assertFalse(emailService.configurazione().reale());

        assertThrows(IllegalStateException.class, () -> emailService.inviaTest("prova@example.org", null, "segreteria"));
        assertThrows(IllegalArgumentException.class, () -> emailService.inviaTest("non-un-indirizzo", null, "admin"));
        assertThrows(IllegalArgumentException.class, () -> emailService.inviaTest("", null, "admin"));
    }

    private Corso canto() {
        return corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow();
    }
}
