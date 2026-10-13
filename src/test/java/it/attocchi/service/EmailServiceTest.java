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
    void inviaConferme_aTuttiGliIscrittiDelCorso() {
        EmailService.Riepilogo riepilogo = emailService.inviaConferme(canto().getId(), "segreteria");
        assertEquals(new EmailService.Riepilogo(0, 7, 0, 0), riepilogo);
        assertEquals(7, emailService.tuttiGliInvii().size());
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
