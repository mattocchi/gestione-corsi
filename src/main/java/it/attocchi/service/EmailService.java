package it.attocchi.service;

import it.attocchi.config.MailProperties;
import it.attocchi.entity.Allievo;
import it.attocchi.entity.Genitore;
import it.attocchi.entity.InvioEmail;
import it.attocchi.entity.InvioEmail.Esito;
import it.attocchi.entity.Iscrizione;
import it.attocchi.entity.TipoDocumento;
import it.attocchi.entity.User;
import it.attocchi.repository.InvioEmailRepository;
import it.attocchi.repository.IscrizioneRepository;
import it.attocchi.repository.PagamentoRepository;
import it.attocchi.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Invio per email di conferme di iscrizione e ricevute (admin e segreteria), con il PDF allegato. Senza SMTP
 * configurato l'invio è simulato. Ogni tentativo è registrato in {@link InvioEmail} con il suo esito.
 */
@Service
@Transactional(readOnly = true)
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    /** Destinatario dell'email: il genitore o, in mancanza di un genitore con email, l'allievo. */
    public record Destinatario(String nome, String email) {
    }

    /** Esito di un invio multiplo. */
    public record Riepilogo(int inviate, int simulate, int errori, int senzaEmail) {
    }

    private final PagamentoService pagamentoService;
    private final DocumentiService documentiService;
    private final IscrizioneRepository iscrizioneRepository;
    private final PagamentoRepository pagamentoRepository;
    private final InvioEmailRepository invioEmailRepository;
    private final UserRepository userRepository;
    private final ObjectProvider<JavaMailSender> mailSender;
    private final MailProperties mail;

    public EmailService(PagamentoService pagamentoService, DocumentiService documentiService,
                        IscrizioneRepository iscrizioneRepository, PagamentoRepository pagamentoRepository,
                        InvioEmailRepository invioEmailRepository, UserRepository userRepository,
                        ObjectProvider<JavaMailSender> mailSender, MailProperties mail) {
        this.pagamentoService = pagamentoService;
        this.documentiService = documentiService;
        this.iscrizioneRepository = iscrizioneRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.invioEmailRepository = invioEmailRepository;
        this.userRepository = userRepository;
        this.mailSender = mailSender;
        this.mail = mail;
    }

    /** {@code true} se l'invio è reale (SMTP configurato), {@code false} se simulato. */
    public boolean invioReale() {
        return mailSender.getIfAvailable() != null;
    }

    /** Destinatario dell'iscrizione, vuoto se né il genitore né l'allievo hanno un'email. */
    public Optional<Destinatario> destinatario(Long corsoId, Long allievoId) {
        return destinatario(pagamentoService.findIscrizione(corsoId, allievoId).getAllievo());
    }

    /** {@code true} se l'iscrizione ha pagamenti, e quindi una ricevuta da inviare. */
    public boolean haPagamenti(Long corsoId, Long allievoId) {
        return pagamentoRepository.existsByIscrizione(corsoId, allievoId);
    }

    /**
     * Invia il documento dell'iscrizione e registra il tentativo; un errore di invio non solleva eccezioni ma è
     * registrato con esito {@link Esito#ERRORE}. Senza destinatario, o una ricevuta senza pagamenti,
     * {@link IllegalStateException} (nessun invio da registrare).
     */
    @Transactional
    public InvioEmail invia(TipoDocumento tipo, Long corsoId, Long allievoId, String username) {
        verificaPermesso(username);
        Iscrizione iscrizione = pagamentoService.findIscrizione(corsoId, allievoId);
        Destinatario destinatario = destinatario(iscrizione.getAllievo())
            .orElseThrow(() -> new IllegalStateException("Né il genitore né l'allievo hanno un indirizzo email"));
        if (tipo == TipoDocumento.RICEVUTA && !haPagamenti(corsoId, allievoId)) {
            throw new IllegalStateException("Nessun pagamento registrato: non c'è una ricevuta da inviare");
        }

        InvioEmail invio = new InvioEmail();
        invio.setDataOra(LocalDateTime.now());
        invio.setTipo(tipo);
        invio.setCorsoId(corsoId);
        invio.setCorsoNome(iscrizione.getCorso().getNome());
        invio.setAllievoId(allievoId);
        invio.setAllievoNome(iscrizione.getAllievo().getNomeCompleto());
        invio.setDestinatario(destinatario.email());
        invio.setDestinatarioNome(destinatario.nome());
        invio.setOggetto(tipo.getTitolo() + " – " + iscrizione.getCorso().getNome()
            + (iscrizione.getCorso().getAnnoScolastico() != null ? " " + iscrizione.getCorso().getAnnoScolastico() : "")
            + " – " + iscrizione.getAllievo().getNomeCompleto());
        invio.setAllegato(documentiService.nomeFile(tipo, iscrizione));
        invio.setInviatoDa(username);

        try {
            String corpo = documentiService.emailHtml(tipo, corsoId, allievoId, destinatario.nome());
            byte[] pdf = documentiService.documentoPdf(tipo, corsoId, allievoId);
            JavaMailSender sender = mailSender.getIfAvailable();
            if (sender == null) {
                log.info("Invio email simulato (SMTP non configurato): {} a {}", invio.getOggetto(), destinatario.email());
                invio.setEsito(Esito.SIMULATA);
            } else {
                MimeMessage messaggio = sender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(messaggio, true, "UTF-8");
                helper.setTo(destinatario.email());
                if (mail.from() != null && !mail.from().isBlank()) {
                    helper.setFrom(mail.from());
                }
                helper.setSubject(invio.getOggetto());
                helper.setText(corpo, true);
                helper.addAttachment(invio.getAllegato(), new ByteArrayResource(pdf), "application/pdf");
                sender.send(messaggio);
                invio.setEsito(Esito.INVIATA);
            }
        } catch (Exception ex) {
            log.warn("Invio email non riuscito: {} a {}", invio.getOggetto(), destinatario.email(), ex);
            invio.setEsito(Esito.ERRORE);
            String messaggio = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            invio.setErrore(messaggio.length() > 1000 ? messaggio.substring(0, 1000) : messaggio);
        }
        return invioEmailRepository.save(invio);
    }

    /** Invia la conferma di iscrizione a tutti i partecipanti del corso che hanno un destinatario. */
    @Transactional
    public Riepilogo inviaConferme(Long corsoId, String username) {
        verificaPermesso(username);
        int inviate = 0;
        int simulate = 0;
        int errori = 0;
        int senzaEmail = 0;
        for (Iscrizione iscrizione : iscrizioneRepository.findByCorsoIdWithAllievo(corsoId)) {
            if (destinatario(iscrizione.getAllievo()).isEmpty()) {
                senzaEmail++;
                continue;
            }
            switch (invia(TipoDocumento.CONFERMA_ISCRIZIONE, corsoId, iscrizione.getAllievo().getId(), username).getEsito()) {
                case INVIATA -> inviate++;
                case SIMULATA -> simulate++;
                case ERRORE -> errori++;
            }
        }
        return new Riepilogo(inviate, simulate, errori, senzaEmail);
    }

    /** Invii di un'iscrizione, dal più recente. */
    public List<InvioEmail> invii(Long corsoId, Long allievoId) {
        return invioEmailRepository.findByCorsoIdAndAllievoIdOrderByDataOraDesc(corsoId, allievoId);
    }

    /** Tutti gli invii, dal più recente. */
    public List<InvioEmail> tuttiGliInvii() {
        return invioEmailRepository.findAllByOrderByDataOraDesc();
    }

    private static Optional<Destinatario> destinatario(Allievo allievo) {
        Genitore genitore = allievo.getGenitore();
        if (genitore != null && genitore.getUser() != null && conEmail(genitore.getUser())) {
            return Optional.of(new Destinatario(genitore.getNomeCompleto(), genitore.getUser().getEmail().strip()));
        }
        if (allievo.getUser() != null && conEmail(allievo.getUser())) {
            return Optional.of(new Destinatario(allievo.getNomeCompleto(), allievo.getUser().getEmail().strip()));
        }
        return Optional.empty();
    }

    private static boolean conEmail(User user) {
        return user.getEmail() != null && !user.getEmail().isBlank();
    }

    private void verificaPermesso(String username) {
        Set<String> ruoli = userRepository.findByUsername(username).map(User::getRoles).orElse(Set.of());
        if (!ruoli.contains("ADMIN") && !ruoli.contains("SEGRETERIA")) {
            throw new IllegalStateException("Solo admin e segreteria possono inviare email");
        }
    }
}
