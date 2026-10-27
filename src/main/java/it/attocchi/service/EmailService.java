package it.attocchi.service;

import it.attocchi.service.PagamentoService.Attestazione;
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
import it.attocchi.service.DocumentiService.Pdf;
import it.attocchi.service.PagamentoService.RiepilogoFamiglia;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Invio per email di conferme di iscrizione, ricevute e riepiloghi annuali (admin e segreteria), con il PDF allegato. Senza SMTP
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
    /** Anteprima di un'email dell'iscrizione: com'è costruita dall'invio, senza inviarla né registrarla. */
    public record Anteprima(Destinatario destinatario, String oggetto, String corpoHtml, String allegato, byte[] pdf) {
    }

    /** Partecipante dell'invio delle conferme a tutti: il destinatario, o il motivo per cui è saltato. */
    public record DestinatarioConferma(Long allievoId, String allievo, Destinatario destinatario, String motivoEsclusione) {

        public boolean riceve() {
            return motivoEsclusione == null;
        }
    }

    public record Riepilogo(int inviate, int simulate, int errori, int senzaEmail, int datiMancanti) {
    }

    /** Configurazione SMTP in uso, mai con la password; {@code reale} è {@code false} se l'invio è simulato. */
    public record ConfigurazioneSmtp(String host, int port, String username, String from, boolean starttls, boolean reale) {
    }

    /** Esito di un'email di test; {@code errore} (con le cause) solo con esito {@link Esito#ERRORE}. */
    public record EsitoTest(Esito esito, String oggetto, String errore) {
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

    /** Configurazione SMTP in uso, per verificarla dall'email di test. */
    public ConfigurazioneSmtp configurazione() {
        return new ConfigurazioneSmtp(mail.host(), mail.port(), mail.username(), mail.from(), mail.starttls(), invioReale());
    }

    /**
     * Email di test (solo admin) a un indirizzo qualunque, per verificare l'SMTP e la resa dei template: con
     * {@code tipo} il template di quel documento compilato con dati fittizi e il PDF allegato, con {@code null} un
     * semplice messaggio di prova. Non è registrata tra gli invii; un errore di invio è restituito nell'esito.
     */
    public EsitoTest inviaTest(String destinatario, TipoDocumento tipo, String username) {
        verificaAdmin(username);
        String indirizzo = destinatario != null ? destinatario.strip() : "";
        try {
            new InternetAddress(indirizzo, true).validate();
        } catch (AddressException ex) {
            throw new IllegalArgumentException("Indirizzo email non valido: " + indirizzo);
        }
        String oggetto = "[TEST] " + (tipo != null ? tipo.getTitolo() + " – esempio" : "Email di prova");
        try {
            String corpo = tipo != null ? documentiService.emailEsempioHtml(tipo, "Maria Esempio") : documentiService.emailProvaHtml(indirizzo);
            String allegato = tipo != null ? documentiService.nomeFileEsempio(tipo) : null;
            byte[] pdf = tipo != null ? documentiService.documentoEsempioPdf(tipo) : null;
            JavaMailSender sender = mailSender.getIfAvailable();
            if (sender == null) {
                log.info("Email di test simulata (SMTP non configurato): {} a {}", oggetto, indirizzo);
                return new EsitoTest(Esito.SIMULATA, oggetto, null);
            }
            spedisci(sender, indirizzo, oggetto, corpo, allegato, pdf);
            log.info("Email di test inviata da {}: {} a {}", username, oggetto, indirizzo);
            return new EsitoTest(Esito.INVIATA, oggetto, null);
        } catch (Exception ex) {
            log.warn("Email di test non riuscita: {} a {}", oggetto, indirizzo, ex);
            return new EsitoTest(Esito.ERRORE, oggetto, cause(ex));
        }
    }

    /**
     * Email di servizio senza allegati (recupero password, avvisi all'utente), non registrata tra gli invii: con l'SMTP
     * non configurato è simulata. Gli errori di invio vengono scritti nel log e restituiti come {@link Esito#ERRORE}.
     */
    public Esito inviaMessaggio(String a, String oggetto, String corpoHtml) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.info("Email simulata (SMTP non configurato): {} a {}", oggetto, a);
            return Esito.SIMULATA;
        }
        try {
            spedisci(sender, a, oggetto, corpoHtml, null, null);
            log.info("Email inviata: {} a {}", oggetto, a);
            return Esito.INVIATA;
        } catch (Exception ex) {
            log.warn("Email non riuscita: {} a {}", oggetto, a, ex);
            return Esito.ERRORE;
        }
    }

    /** Destinatario dell'iscrizione, vuoto se né il genitore né l'allievo hanno un'email. */
    public Optional<Destinatario> destinatario(Long corsoId, Long allievoId) {
        return destinatarioDi(pagamentoService.findIscrizione(corsoId, allievoId).getAllievo());
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
        Destinatario destinatario = verificaInviabile(tipo, iscrizione);

        InvioEmail invio = new InvioEmail();
        invio.setDataOra(LocalDateTime.now());
        invio.setTipo(tipo);
        invio.setCorsoId(corsoId);
        invio.setCorsoNome(iscrizione.getCorso().getNome());
        invio.setAllievoId(allievoId);
        invio.setAllievoNome(iscrizione.getAllievo().getNomeCompleto());
        invio.setDestinatario(destinatario.email());
        invio.setDestinatarioNome(destinatario.nome());
        invio.setOggetto(oggetto(tipo, iscrizione));
        invio.setAllegato(documentiService.nomeFile(tipo, iscrizione));
        invio.setInviatoDa(username);

        return spedisciERegistra(invio, () -> documentiService.emailHtml(tipo, corsoId, allievoId, destinatario.nome()),
            () -> documentiService.documentoPdf(tipo, corsoId, allievoId));
    }

    /** Corpo e PDF di un invio, calcolati solo al momento di spedire (un errore diventa l'esito dell'invio). */
    private interface Contenuto<T> {
        T get() throws Exception;
    }

    /** Spedisce (o simula) l'email dell'invio già compilato e lo registra con l'esito. */
    private InvioEmail spedisciERegistra(InvioEmail invio, Contenuto<String> corpo, Contenuto<byte[]> pdf) {
        try {
            String testo = corpo.get();
            byte[] allegato = pdf.get();
            JavaMailSender sender = mailSender.getIfAvailable();
            if (sender == null) {
                log.info("Invio email simulato (SMTP non configurato): {} a {}", invio.getOggetto(), invio.getDestinatario());
                invio.setEsito(Esito.SIMULATA);
            } else {
                spedisci(sender, invio.getDestinatario(), invio.getOggetto(), testo, invio.getAllegato(), allegato);
                invio.setEsito(Esito.INVIATA);
            }
        } catch (Exception ex) {
            log.warn("Invio email non riuscito: {} a {}", invio.getOggetto(), invio.getDestinatario(), ex);
            invio.setEsito(Esito.ERRORE);
            String messaggio = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            invio.setErrore(messaggio.length() > 1000 ? messaggio.substring(0, 1000) : messaggio);
        }
        return invioEmailRepository.save(invio);
    }

    /**
     * Destinatario del riepilogo annuale: il genitore ({@code genitoreId}) o l'allievo adulto ({@code allievoId}), se
     * ha un'email.
     */
    public Optional<Destinatario> destinatarioRiepilogo(Long genitoreId, Long allievoId, String anno) {
        return destinatario(riepilogo(genitoreId, allievoId, anno));
    }

    /**
     * Invia il riepilogo annuale della famiglia ({@code genitoreId}) o dell'allievo adulto ({@code allievoId}) e
     * registra il tentativo come {@link #invia}; senza destinatario o senza corsi nell'anno {@link IllegalStateException}.
     */
    @Transactional
    public InvioEmail inviaRiepilogo(Long genitoreId, Long allievoId, String anno, String username) {
        verificaPermesso(username);
        RiepilogoFamiglia riepilogo = riepilogo(genitoreId, allievoId, anno);
        Destinatario destinatario = verificaInviabile(riepilogo);

        InvioEmail invio = new InvioEmail();
        invio.setDataOra(LocalDateTime.now());
        invio.setTipo(TipoDocumento.RIEPILOGO);
        invio.setGenitoreId(genitoreId);
        invio.setAllievoId(allievoId);
        invio.setAllievoNome(riepilogo.intestatario());
        invio.setAnno(anno);
        invio.setCorsoNome(anno != null ? "Anno " + anno : "Senza anno scolastico");
        invio.setDestinatario(destinatario.email());
        invio.setDestinatarioNome(destinatario.nome());
        invio.setOggetto(oggetto(riepilogo));
        Pdf pdf = documentiService.riepilogoPdf(riepilogo);
        invio.setAllegato(pdf.nomeFile());
        invio.setInviatoDa(username);
        return spedisciERegistra(invio, () -> documentiService.emailRiepilogoHtml(riepilogo, destinatario.nome()), pdf::contenuto);
    }

    /** Anteprima dell'email del riepilogo, con gli stessi controlli di {@link #inviaRiepilogo}, senza inviarla. */
    public Anteprima anteprimaRiepilogo(Long genitoreId, Long allievoId, String anno, String username) {
        verificaPermesso(username);
        RiepilogoFamiglia riepilogo = riepilogo(genitoreId, allievoId, anno);
        Destinatario destinatario = verificaInviabile(riepilogo);
        Pdf pdf = documentiService.riepilogoPdf(riepilogo);
        return new Anteprima(destinatario, oggetto(riepilogo), documentiService.emailRiepilogoHtml(riepilogo, destinatario.nome()),
            pdf.nomeFile(), pdf.contenuto());
    }

    /** Destinatario dell'attestazione: il genitore dell'allievo, o l'allievo adulto, se ha un'email. */
    public Optional<Destinatario> destinatarioAttestazione(Long allievoId, int anno) {
        return destinatario(pagamentoService.attestazione(allievoId, anno));
    }

    /**
     * Invia l'attestazione delle spese dell'allievo nell'anno solare e registra il tentativo come {@link #invia}; senza
     * destinatario o senza pagamenti nell'anno {@link IllegalStateException}.
     */
    @Transactional
    public InvioEmail inviaAttestazione(Long allievoId, int anno, String username) {
        verificaPermesso(username);
        Attestazione attestazione = pagamentoService.attestazione(allievoId, anno);
        Destinatario destinatario = verificaInviabile(attestazione);

        InvioEmail invio = new InvioEmail();
        invio.setDataOra(LocalDateTime.now());
        invio.setTipo(TipoDocumento.ATTESTAZIONE);
        invio.setGenitoreId(attestazione.genitore() != null ? attestazione.genitore().getId() : null);
        invio.setAllievoId(allievoId);
        invio.setAllievoNome(attestazione.allievo().getNomeCompleto());
        invio.setAnno(String.valueOf(anno));
        invio.setCorsoNome("Anno solare " + anno);
        invio.setDestinatario(destinatario.email());
        invio.setDestinatarioNome(destinatario.nome());
        invio.setOggetto(oggetto(attestazione));
        Pdf pdf = documentiService.attestazionePdf(attestazione);
        invio.setAllegato(pdf.nomeFile());
        invio.setInviatoDa(username);
        return spedisciERegistra(invio, () -> documentiService.emailAttestazioneHtml(attestazione, destinatario.nome()),
            pdf::contenuto);
    }

    /** Anteprima dell'email dell'attestazione, con gli stessi controlli di {@link #inviaAttestazione}, senza inviarla. */
    public Anteprima anteprimaAttestazione(Long allievoId, int anno, String username) {
        verificaPermesso(username);
        Attestazione attestazione = pagamentoService.attestazione(allievoId, anno);
        Destinatario destinatario = verificaInviabile(attestazione);
        Pdf pdf = documentiService.attestazionePdf(attestazione);
        return new Anteprima(destinatario, oggetto(attestazione),
            documentiService.emailAttestazioneHtml(attestazione, destinatario.nome()), pdf.nomeFile(), pdf.contenuto());
    }

    private static Optional<Destinatario> destinatario(Attestazione attestazione) {
        Genitore genitore = attestazione.genitore();
        return destinatarioRiepilogoDi(genitore, genitore == null ? attestazione.allievo() : null);
    }

    private static Destinatario verificaInviabile(Attestazione attestazione) {
        if (attestazione.pagamenti().isEmpty()) {
            throw new IllegalStateException("Nessun pagamento nell'anno " + attestazione.anno());
        }
        return destinatario(attestazione).orElseThrow(() -> new IllegalStateException(
            (attestazione.genitore() != null ? "Il genitore" : "L'allievo") + " non ha un indirizzo email"));
    }

    private static String oggetto(Attestazione attestazione) {
        return TipoDocumento.ATTESTAZIONE.getTitolo() + " " + attestazione.anno() + " – " + attestazione.allievo().getNomeCompleto();
    }

    private RiepilogoFamiglia riepilogo(Long genitoreId, Long allievoId, String anno) {
        if ((genitoreId == null) == (allievoId == null)) {
            throw new IllegalArgumentException("Indicare il genitore o l'allievo del riepilogo");
        }
        return genitoreId != null ? pagamentoService.riepilogoFamiglia(genitoreId, anno) : pagamentoService.riepilogoAllievo(allievoId, anno);
    }

    private static Optional<Destinatario> destinatario(RiepilogoFamiglia riepilogo) {
        return destinatarioRiepilogoDi(riepilogo.genitore(), riepilogo.allievo());
    }

    /** Destinatario del riepilogo: il genitore se indicato, altrimenti l'allievo adulto; vuoto se non ha un'email. */
    public static Optional<Destinatario> destinatarioRiepilogoDi(Genitore genitore, Allievo allievo) {
        User user = genitore != null ? genitore.getUser() : allievo.getUser();
        if (user == null || !conEmail(user)) {
            return Optional.empty();
        }
        return Optional.of(new Destinatario(genitore != null ? genitore.getNomeCompleto() : allievo.getNomeCompleto(),
            user.getEmail().strip()));
    }

    private static Destinatario verificaInviabile(RiepilogoFamiglia riepilogo) {
        if (riepilogo.righe().isEmpty()) {
            throw new IllegalStateException("Nessun corso nell'anno scolastico scelto");
        }
        return destinatario(riepilogo).orElseThrow(() -> new IllegalStateException(
            (riepilogo.genitore() != null ? "Il genitore" : "L'allievo") + " non ha un indirizzo email"));
    }

    private static String oggetto(RiepilogoFamiglia riepilogo) {
        return TipoDocumento.RIEPILOGO.getTitolo() + (riepilogo.anno() != null ? " " + riepilogo.anno() : "")
            + " – " + riepilogo.intestatario();
    }

    /**
     * Anteprima dell'email che {@link #invia} manderebbe (stessi controlli, destinatario, oggetto, testo e PDF), senza
     * inviarla né registrarla.
     */
    public Anteprima anteprima(TipoDocumento tipo, Long corsoId, Long allievoId, String username) {
        verificaPermesso(username);
        Iscrizione iscrizione = pagamentoService.findIscrizione(corsoId, allievoId);
        Destinatario destinatario = verificaInviabile(tipo, iscrizione);
        return new Anteprima(destinatario, oggetto(tipo, iscrizione),
            documentiService.emailHtml(tipo, corsoId, allievoId, destinatario.nome()),
            documentiService.nomeFile(tipo, iscrizione), documentiService.documentoPdf(tipo, corsoId, allievoId));
    }

    /**
     * Partecipanti del corso nell'ordine dei partecipanti, con chi riceverebbe la conferma di iscrizione da
     * {@link #inviaConferme} e chi verrebbe saltato (e perché).
     */
    public List<DestinatarioConferma> destinatariConferme(Long corsoId, String username) {
        verificaPermesso(username);
        return iscrizioneRepository.findByCorsoIdWithAllievo(corsoId).stream()
            .sorted(Iscrizione.ORDINE)
            .map(iscrizione -> {
                Allievo allievo = iscrizione.getAllievo();
                Destinatario destinatario = destinatarioDi(allievo).orElse(null);
                String motivo = !allievo.isDatiPer(TipoDocumento.CONFERMA_ISCRIZIONE) ? "senza data di nascita"
                    : destinatario == null ? "senza email" : null;
                return new DestinatarioConferma(allievo.getId(), allievo.getNomeCompleto(), destinatario, motivo);
            })
            .toList();
    }

    /**
     * Invia la conferma di iscrizione a tutti i partecipanti del corso che hanno un destinatario e i dati per i
     * documenti (la data di nascita); gli altri sono solo contati.
     */
    @Transactional
    public Riepilogo inviaConferme(Long corsoId, String username) {
        verificaPermesso(username);
        int inviate = 0;
        int simulate = 0;
        int errori = 0;
        int senzaEmail = 0;
        int datiMancanti = 0;
        for (Iscrizione iscrizione : iscrizioneRepository.findByCorsoIdWithAllievo(corsoId)) {
            if (!iscrizione.getAllievo().isDatiPer(TipoDocumento.CONFERMA_ISCRIZIONE)) {
                datiMancanti++;
                continue;
            }
            if (destinatarioDi(iscrizione.getAllievo()).isEmpty()) {
                senzaEmail++;
                continue;
            }
            switch (invia(TipoDocumento.CONFERMA_ISCRIZIONE, corsoId, iscrizione.getAllievo().getId(), username).getEsito()) {
                case INVIATA -> inviate++;
                case SIMULATA -> simulate++;
                case ERRORE -> errori++;
            }
        }
        return new Riepilogo(inviate, simulate, errori, senzaEmail, datiMancanti);
    }

    /** Invii di un'iscrizione, dal più recente. */
    public List<InvioEmail> invii(Long corsoId, Long allievoId) {
        return invioEmailRepository.findByCorsoIdAndAllievoIdOrderByDataOraDesc(corsoId, allievoId);
    }

    /** Tutti gli invii, dal più recente. */
    public List<InvioEmail> tuttiGliInvii() {
        return invioEmailRepository.findAllByOrderByDataOraDesc();
    }

    /** Messaggio HTML con l'eventuale PDF allegato ({@code allegato} null = nessun allegato). */
    private void spedisci(JavaMailSender sender, String a, String oggetto, String corpo, String allegato, byte[] pdf)
            throws MessagingException {
        MimeMessage messaggio = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(messaggio, true, "UTF-8");
        helper.setTo(a);
        if (mail.from() != null && !mail.from().isBlank()) {
            helper.setFrom(mail.from());
        }
        helper.setSubject(oggetto);
        helper.setText(corpo, true);
        if (allegato != null) {
            helper.addAttachment(allegato, new ByteArrayResource(pdf), "application/pdf");
        }
        sender.send(messaggio);
    }

    /** Messaggi dell'eccezione e delle sue cause, per capire un errore SMTP (es. autenticazione, TLS, host). */
    private static String cause(Throwable ex) {
        Set<String> messaggi = new LinkedHashSet<>();
        for (Throwable t = ex; t != null && messaggi.size() < 5; t = t.getCause()) {
            messaggi.add(t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName());
        }
        return String.join(" ← ", messaggi);
    }

    /** Destinatario dei documenti dell'allievo: il genitore con email o, in mancanza, l'allievo. */
    public static Optional<Destinatario> destinatarioDi(Allievo allievo) {
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

    private void verificaAdmin(String username) {
        if (!userRepository.findByUsername(username).map(User::getRoles).orElse(Set.of()).contains("ADMIN")) {
            throw new IllegalStateException("Solo l'admin può inviare email di test");
        }
    }

    /** Destinatario dell'email, se l'iscrizione la può ricevere; altrimenti {@link IllegalStateException}. */
    private Destinatario verificaInviabile(TipoDocumento tipo, Iscrizione iscrizione) {
        Destinatario destinatario = destinatarioDi(iscrizione.getAllievo())
            .orElseThrow(() -> new IllegalStateException("Né il genitore né l'allievo hanno un indirizzo email"));
        if (tipo == TipoDocumento.RICEVUTA
                && !haPagamenti(iscrizione.getCorso().getId(), iscrizione.getAllievo().getId())) {
            throw new IllegalStateException("Nessun pagamento registrato: non c'è una ricevuta da inviare");
        }
        String datiMancanti = DocumentiService.avvisoDatiMancanti(iscrizione.getAllievo(), tipo);
        if (datiMancanti != null) {
            throw new IllegalStateException(datiMancanti);
        }
        return destinatario;
    }

    private static String oggetto(TipoDocumento tipo, Iscrizione iscrizione) {
        return tipo.getTitolo() + " – " + iscrizione.getCorso().getNome()
            + (iscrizione.getCorso().getAnnoScolastico() != null ? " " + iscrizione.getCorso().getAnnoScolastico() : "")
            + " – " + iscrizione.getAllievo().getNomeCompleto();
    }

    private void verificaPermesso(String username) {
        Set<String> ruoli = userRepository.findByUsername(username).map(User::getRoles).orElse(Set.of());
        if (!ruoli.contains("ADMIN") && !ruoli.contains("SEGRETERIA")) {
            throw new IllegalStateException("Solo admin e segreteria possono inviare email");
        }
    }
}
