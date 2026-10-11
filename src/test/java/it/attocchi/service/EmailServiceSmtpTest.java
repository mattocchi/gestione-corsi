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

    private Corso canto() {
        return corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow();
    }
}
