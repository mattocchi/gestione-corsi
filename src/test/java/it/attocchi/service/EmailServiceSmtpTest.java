package it.attocchi.service;

import it.attocchi.entity.Allievo;
import it.attocchi.entity.Corso;
import it.attocchi.entity.InvioEmail;
import it.attocchi.entity.InvioEmail.Esito;
import it.attocchi.entity.TipoDocumento;
import it.attocchi.repository.InvioEmailRepository;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Invio reale con un JavaMailSender finto: messaggio con oggetto, destinatario e PDF allegato; errori registrati. */
@SpringBootTest(properties = "app.mail.from=MOSSON <segreteria@example.org>")
class EmailServiceSmtpTest {

    @MockitoBean
    private JavaMailSender mailSender;

    @Autowired
    private EmailService emailService;

    @Autowired
    private CorsoService corsoService;

    @Autowired
    private AllievoService allievoService;

    @Autowired
    private InvioEmailRepository invioEmailRepository;

    @AfterEach
    void pulisci() {
        invioEmailRepository.deleteAll();
        reset(mailSender);
    }

    @Test
    void invia_messaggioConAllegatoPdf() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        Corso canto = canto();
        Allievo allievo = allievoService.findByUsername("allievo_canto3").orElseThrow();

        InvioEmail invio = emailService.invia(TipoDocumento.CONFERMA_ISCRIZIONE, canto.getId(), allievo.getId(), "admin");

        assertEquals(Esito.INVIATA, invio.getEsito());
        ArgumentCaptor<MimeMessage> inviato = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(inviato.capture());
        MimeMessage messaggio = inviato.getValue();
        messaggio.saveChanges();
        assertEquals(invio.getOggetto(), messaggio.getSubject());
        assertEquals("genitore3@example.com", messaggio.getAllRecipients()[0].toString());
        assertTrue(messaggio.getFrom()[0].toString().contains("segreteria@example.org"));
        Multipart parti = (Multipart) messaggio.getContent();
        boolean conPdf = false;
        for (int i = 0; i < parti.getCount(); i++) {
            conPdf |= invio.getAllegato().equals(parti.getBodyPart(i).getFileName());
        }
        assertTrue(conPdf);
    }

    @Test
    void invia_erroreRegistrato() {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        doThrow(new MailSendException("server non raggiungibile")).when(mailSender).send(any(MimeMessage.class));
        Corso canto = canto();
        Allievo allievo = allievoService.findByUsername("allievo_canto3").orElseThrow();

        InvioEmail invio = emailService.invia(TipoDocumento.CONFERMA_ISCRIZIONE, canto.getId(), allievo.getId(), "admin");

        assertEquals(Esito.ERRORE, invio.getEsito());
        assertEquals("server non raggiungibile", invio.getErrore());
        assertEquals(1, emailService.invii(canto.getId(), allievo.getId()).size());
    }

    @Test
    void test_templateConAllegatoOSoloTesto() throws Exception {
        when(mailSender.createMimeMessage()).thenAnswer(i -> new MimeMessage(Session.getInstance(new Properties())));
        long registrati = invioEmailRepository.count();

        EmailService.EsitoTest esito = emailService.inviaTest("prova@example.org", TipoDocumento.CONFERMA_ISCRIZIONE, "admin");
        assertEquals(Esito.INVIATA, esito.esito());
        ArgumentCaptor<MimeMessage> inviato = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(inviato.capture());
        MimeMessage messaggio = inviato.getValue();
        messaggio.saveChanges();
        assertEquals("[TEST] Conferma di iscrizione – esempio", messaggio.getSubject());
        assertEquals("prova@example.org", messaggio.getAllRecipients()[0].toString());
        assertEquals("conferma-iscrizione-corso-di-esempio-anna-esempio.pdf", ((Multipart) messaggio.getContent()).getBodyPart(1).getFileName());

        reset(mailSender);
        when(mailSender.createMimeMessage()).thenAnswer(i -> new MimeMessage(Session.getInstance(new Properties())));
        assertEquals(Esito.INVIATA, emailService.inviaTest("prova@example.org", null, "admin").esito());
        verify(mailSender).send(inviato.capture());
        inviato.getValue().saveChanges();
        assertEquals(1, ((Multipart) inviato.getValue().getContent()).getCount());
        // Le email di test non si registrano
        assertEquals(registrati, invioEmailRepository.count());
    }

    @Test
    void test_erroreConLeCause() {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        doThrow(new MailSendException("invio fallito", new IllegalStateException("535 autenticazione rifiutata")))
            .when(mailSender).send(any(MimeMessage.class));

        EmailService.EsitoTest esito = emailService.inviaTest("prova@example.org", null, "admin");

        assertEquals(Esito.ERRORE, esito.esito());
        assertTrue(esito.errore().contains("535 autenticazione rifiutata"), esito.errore());
    }

    private Corso canto() {
        return corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow();
    }
}
