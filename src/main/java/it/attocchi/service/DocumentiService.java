package it.attocchi.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import it.attocchi.config.AssociazioneProperties;
import it.attocchi.entity.Allievo;
import it.attocchi.entity.Corso;
import it.attocchi.entity.Genitore;
import it.attocchi.entity.Indirizzo;
import it.attocchi.entity.Insegnante;
import it.attocchi.entity.Iscrizione;
import it.attocchi.entity.MetodoPagamento;
import it.attocchi.entity.Pagamento;
import it.attocchi.entity.TipoDocumento;
import it.attocchi.entity.User;
import it.attocchi.repository.UserRepository;
import it.attocchi.service.PagamentoService.Ricevuta;
import it.attocchi.service.PagamentoService.Situazione;
import it.attocchi.views.Formati;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ricevute e conferme di iscrizione da template FreeMarker ({@code templates/documenti}, {@code templates/email}):
 * lo stesso HTML diventa il corpo dell'email e, con OpenHTMLtoPDF, il PDF allegato e scaricato dal web.
 */
@Service
@Transactional(readOnly = true)
public class DocumentiService {

    /** PDF pronto per lo scarico o l'allegato. */
    public record Pdf(String nomeFile, byte[] contenuto) {
    }

    private static final String LOGO = "/META-INF/resources/images/logo-mosson.png";

    private final PagamentoService pagamentoService;
    private final AssociazioneProperties associazione;
    private final Configuration freemarker;
    private final String logo;

    private final UserRepository userRepository;

    public DocumentiService(PagamentoService pagamentoService, AssociazioneProperties associazione,
                            UserRepository userRepository) {
        this.pagamentoService = pagamentoService;
        this.userRepository = userRepository;
        this.associazione = associazione;
        this.freemarker = new Configuration(Configuration.VERSION_2_3_34);
        freemarker.setClassForTemplateLoading(DocumentiService.class, "/templates");
        freemarker.setDefaultEncoding(StandardCharsets.UTF_8.name());
        freemarker.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        freemarker.setLogTemplateExceptions(false);
        this.logo = logoDataUri();
    }

    /**
     * Avviso per l'allievo a cui mancano i dati per ricevute e conferme, es. "Mancano codice fiscale e data di
     * nascita di Mario Rossi: …"; {@code null} se i dati ci sono.
     */
    public static String avvisoDatiMancanti(Allievo allievo) {
        List<String> mancanti = allievo.getDatiMancantiPerDocumenti();
        if (mancanti.isEmpty()) {
            return null;
        }
        return (mancanti.size() > 1 ? "Mancano " : "Manca ") + String.join(" e ", mancanti) + " di "
            + allievo.getNomeCompleto() + ": completa i dati nella scheda dell'allievo per stampare o inviare ricevute e conferme";
    }

    /** HTML del documento (pagina A4) dell'iscrizione. */
    public String documentoHtml(TipoDocumento tipo, Long corsoId, Long allievoId) {
        return render("documenti/" + tipo.getTemplate() + ".ftlh", modello(conDati(pagamentoService.ricevuta(corsoId, allievoId))));
    }

    /** PDF del documento dell'iscrizione. */
    public byte[] documentoPdf(TipoDocumento tipo, Long corsoId, Long allievoId) {
        return pdf(documentoHtml(tipo, corsoId, allievoId));
    }

    /**
     * Ricevuta in PDF per l'utente indicato: admin e segreteria le vedono tutte, il genitore solo quelle dei
     * propri figli. Altrimenti (o se l'iscrizione non esiste) {@link IllegalArgumentException}, senza distinguere
     * i due casi per non rivelare le iscrizioni di altri.
     */
    public Pdf ricevutaPer(Long corsoId, Long allievoId, String username) {
        Iscrizione iscrizione;
        try {
            iscrizione = pagamentoService.findIscrizione(corsoId, allievoId);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Ricevuta non trovata");
        }
        if (!puoVedere(iscrizione.getAllievo(), username)) {
            throw new IllegalArgumentException("Ricevuta non trovata");
        }
        return new Pdf(nomeFile(TipoDocumento.RICEVUTA, iscrizione), documentoPdf(TipoDocumento.RICEVUTA, corsoId, allievoId));
    }

    private boolean puoVedere(Allievo allievo, String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return false;
        }
        if (user.getRoles().contains("ADMIN") || user.getRoles().contains("SEGRETERIA")) {
            return true;
        }
        Genitore genitore = allievo.getGenitore();
        return user.getRoles().contains("GENITORE") && genitore != null && genitore.getUser() != null
            && username.equals(genitore.getUser().getUsername());
    }

    /** Corpo HTML dell'email che accompagna il documento; {@code saluto} è il nome del destinatario. */
    public String emailHtml(TipoDocumento tipo, Long corsoId, Long allievoId, String saluto) {
        Map<String, Object> modello = modello(conDati(pagamentoService.ricevuta(corsoId, allievoId)));
        modello.put("saluto", saluto);
        return render("email/" + tipo.getTemplate() + ".ftlh", modello);
    }

    /** Corpo HTML di prova del template, compilato con i dati fittizi di {@link #ricevutaEsempio()}. */
    public String emailEsempioHtml(TipoDocumento tipo, String saluto) {
        Map<String, Object> modello = modello(ricevutaEsempio());
        modello.put("saluto", saluto);
        return render("email/" + tipo.getTemplate() + ".ftlh", modello);
    }

    /** PDF del documento compilato con i dati fittizi di {@link #ricevutaEsempio()}. */
    public byte[] documentoEsempioPdf(TipoDocumento tipo) {
        return pdf(render("documenti/" + tipo.getTemplate() + ".ftlh", modello(ricevutaEsempio())));
    }

    /** Nome del file PDF di esempio, es. "ricevuta-corso-di-esempio-anna-esempio.pdf". */
    public String nomeFileEsempio(TipoDocumento tipo) {
        return nomeFile(tipo, ricevutaEsempio().iscrizione());
    }

    /** Corpo HTML del semplice messaggio di prova (solo testo, senza documento). */
    public String emailProvaHtml(String saluto) {
        Map<String, Object> modello = new HashMap<>();
        modello.put("associazione", associazione());
        modello.put("saluto", saluto);
        modello.put("dataOra", Formati.dataOra(LocalDateTime.now()));
        return render("email/test.ftlh", modello);
    }

    /** Email con il link per una nuova password, valido {@code ore} ore. */
    public String emailRecuperoPasswordHtml(String saluto, String username, String link, int ore) {
        Map<String, Object> modello = new HashMap<>();
        modello.put("associazione", associazione());
        modello.put("saluto", saluto);
        modello.put("username", username);
        modello.put("link", link);
        modello.put("ore", ore);
        return render("email/recupero-password.ftlh", modello);
    }

    /** Avviso all'utente dopo il cambio della password. */
    public String emailPasswordCambiataHtml(String saluto, String username) {
        Map<String, Object> modello = new HashMap<>();
        modello.put("associazione", associazione());
        modello.put("saluto", saluto);
        modello.put("username", username);
        modello.put("dataOra", Formati.dataOra(LocalDateTime.now()));
        return render("email/password-cambiata.ftlh", modello);
    }

    /** Nome del file PDF, es. "ricevuta-corso-di-canto-mario-rossi.pdf". */
    public String nomeFile(TipoDocumento tipo, Iscrizione iscrizione) {
        String base = (tipo == TipoDocumento.RICEVUTA ? "ricevuta" : "conferma-iscrizione") + "-"
            + iscrizione.getCorso().getNome() + "-" + iscrizione.getAllievo().getNomeCompleto();
        return base.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "") + ".pdf";
    }

    /** Rifiuta i documenti dell'allievo senza codice fiscale o data di nascita ({@link IllegalStateException}). */
    private static Ricevuta conDati(Ricevuta ricevuta) {
        String avviso = avvisoDatiMancanti(ricevuta.iscrizione().getAllievo());
        if (avviso != null) {
            throw new IllegalStateException(avviso);
        }
        return ricevuta;
    }

    static byte[] pdf(String html) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            new PdfRendererBuilder().useFastMode().withHtmlContent(html, null).toStream(out).run();
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Errore nella creazione del PDF", ex);
        }
    }

    private String render(String nome, Map<String, Object> modello) {
        try {
            Template template = freemarker.getTemplate(nome);
            StringWriter out = new StringWriter();
            template.process(modello, out);
            return out.toString();
        } catch (IOException ex) {
            throw new UncheckedIOException("Template non trovato: " + nome, ex);
        } catch (TemplateException ex) {
            throw new IllegalStateException("Errore nel template " + nome + ": " + ex.getMessage(), ex);
        }
    }

    /** Valori già formattati per i template (stringhe, mai null). */
    private Map<String, Object> modello(Ricevuta ricevuta) {
        Iscrizione iscrizione = ricevuta.iscrizione();
        Corso corso = iscrizione.getCorso();
        Allievo allievo = iscrizione.getAllievo();
        Genitore genitore = allievo.getGenitore();
        Situazione situazione = ricevuta.situazione();

        Map<String, Object> modello = new HashMap<>();
        modello.put("associazione", associazione());
        if (logo != null) {
            modello.put("logo", logo);
        }
        modello.put("emessa", Formati.dataCompleta(LocalDate.now()));
        modello.put("allievo", Map.of("nome", allievo.getNomeCompleto(), "codiceFiscale", testo(allievo.getCodiceFiscale()),
            "indirizzo", testo(Formati.indirizzo(allievo))));
        if (genitore != null) {
            modello.put("genitore", Map.of("nome", genitore.getNomeCompleto(), "codiceFiscale", testo(genitore.getCodiceFiscale())));
        }
        String quota = Formati.euro(situazione.quota());
        if (iscrizione.getQuotaPersonalizzata() != null) {
            quota += " (quota piena " + Formati.euro(corso.getCostoTotale())
                + (iscrizione.getNotaQuota() != null ? ", " + iscrizione.getNotaQuota() : "") + ")";
        }
        modello.put("corso", Map.of("nome", corso.getNome(), "anno", testo(corso.getAnnoScolastico()),
            "insegnante", corso.getNomiInsegnanti(),
            "etichettaInsegnante", corso.getInsegnanti().size() > 1 ? "Insegnanti" : "Insegnante", "quota", quota));
        List<Map<String, String>> pagamenti = ricevuta.pagamenti().stream().map(DocumentiService::pagamento).toList();
        modello.put("pagamenti", pagamenti);
        modello.put("totali", Map.of("quota", Formati.euro(situazione.quota()), "pagato", Formati.euro(situazione.pagato()),
            "residuo", Formati.euro(situazione.residuo())));
        return modello;
    }

    private Map<String, String> associazione() {
        return Map.of("nome", testo(associazione.nome()), "indirizzo", testo(associazione.indirizzo()),
            "codiceFiscale", testo(associazione.codiceFiscale()), "email", testo(associazione.email()),
            "iban", testo(associazione.iban()));
    }

    /**
     * Iscrizione fittizia (mai salvata) per le email di test: corso con insegnante, allievo con genitore, quota
     * personalizzata e due pagamenti, così i template mostrano tutte le loro parti senza usare dati reali.
     */
    static Ricevuta ricevutaEsempio() {
        Corso corso = new Corso();
        corso.setNome("Corso di esempio");
        corso.setAnnoScolastico("2026/2027");
        corso.setCostoTotale(new BigDecimal("600.00"));
        Insegnante insegnante = new Insegnante();
        insegnante.setUser(new User("esempio.insegnante", null, "Luca", "Esempio", null));
        corso.setInsegnante(insegnante);

        Genitore genitore = new Genitore();
        genitore.setUser(new User("esempio.genitore", null, "Maria", "Esempio", null));
        genitore.setCodiceFiscale("SMPMRA80A41H501X");
        Allievo allievo = new Allievo();
        allievo.setUser(new User("esempio.allievo", null, "Anna", "Esempio", null));
        allievo.setCodiceFiscale("SMPNNA12C45H501Y");
        Indirizzo indirizzo = new Indirizzo();
        indirizzo.setVia("Via di Prova 1");
        indirizzo.setCap("00100");
        indirizzo.setComune("Roma");
        indirizzo.setProvincia("RM");
        allievo.setIndirizzo(indirizzo);
        allievo.setGenitore(genitore);

        Iscrizione iscrizione = new Iscrizione(corso, allievo);
        iscrizione.setQuotaPersonalizzata(new BigDecimal("540.00"));
        iscrizione.setNotaQuota("sconto fratelli");

        LocalDate oggi = LocalDate.now();
        Pagamento acconto = new Pagamento(iscrizione);
        acconto.setData(oggi.minusMonths(1));
        acconto.setImporto(new BigDecimal("200.00"));
        acconto.setMetodo(MetodoPagamento.BONIFICO);
        acconto.setNota("acconto");
        Pagamento rata = new Pagamento(iscrizione);
        rata.setData(oggi);
        rata.setImporto(new BigDecimal("150.00"));
        rata.setMetodo(MetodoPagamento.CASSA);
        List<Pagamento> pagamenti = List.of(acconto, rata);
        BigDecimal pagato = pagamenti.stream().map(Pagamento::getImporto).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Ricevuta(iscrizione, pagamenti, new Situazione(iscrizione.getQuotaPersonalizzata(), pagato));
    }

    private static Map<String, String> pagamento(Pagamento pagamento) {
        Map<String, String> riga = new LinkedHashMap<>();
        riga.put("data", Formati.dataCompleta(pagamento.getData()));
        riga.put("importo", Formati.euro(pagamento.getImporto()));
        riga.put("metodo", pagamento.getMetodo().getDescrizione());
        riga.put("nota", testo(pagamento.getNota()));
        return riga;
    }

    private static String testo(String valore) {
        return valore != null ? valore : "";
    }

    /** Logo come data URI, così il PDF non dipende da risorse esterne; null se manca. */
    private static String logoDataUri() {
        try (InputStream in = DocumentiService.class.getResourceAsStream(LOGO)) {
            return in != null ? "data:image/png;base64," + Base64.getEncoder().encodeToString(in.readAllBytes()) : null;
        } catch (IOException ex) {
            return null;
        }
    }
}
