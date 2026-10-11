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
import it.attocchi.entity.Iscrizione;
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
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
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

    /** HTML del documento (pagina A4) dell'iscrizione. */
    public String documentoHtml(TipoDocumento tipo, Long corsoId, Long allievoId) {
        return render("documenti/" + tipo.getTemplate() + ".ftlh", modello(pagamentoService.ricevuta(corsoId, allievoId)));
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
        Map<String, Object> modello = modello(pagamentoService.ricevuta(corsoId, allievoId));
        modello.put("saluto", saluto);
        return render("email/" + tipo.getTemplate() + ".ftlh", modello);
    }

    /** Nome del file PDF, es. "ricevuta-corso-di-canto-mario-rossi.pdf". */
    public String nomeFile(TipoDocumento tipo, Iscrizione iscrizione) {
        String base = (tipo == TipoDocumento.RICEVUTA ? "ricevuta" : "conferma-iscrizione") + "-"
            + iscrizione.getCorso().getNome() + "-" + iscrizione.getAllievo().getNomeCompleto();
        return base.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "") + ".pdf";
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
        modello.put("associazione", Map.of(
            "nome", testo(associazione.nome()), "indirizzo", testo(associazione.indirizzo()),
            "codiceFiscale", testo(associazione.codiceFiscale()), "email", testo(associazione.email()),
            "iban", testo(associazione.iban())));
        if (logo != null) {
            modello.put("logo", logo);
        }
        modello.put("emessa", Formati.data(LocalDate.now()));
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
            "insegnante", corso.getInsegnante() != null ? corso.getInsegnante().getNomeCompleto() : "", "quota", quota));
        List<Map<String, String>> pagamenti = ricevuta.pagamenti().stream().map(DocumentiService::pagamento).toList();
        modello.put("pagamenti", pagamenti);
        modello.put("totali", Map.of("quota", Formati.euro(situazione.quota()), "pagato", Formati.euro(situazione.pagato()),
            "residuo", Formati.euro(situazione.residuo())));
        return modello;
    }

    private static Map<String, String> pagamento(Pagamento pagamento) {
        Map<String, String> riga = new LinkedHashMap<>();
        riga.put("data", Formati.data(pagamento.getData()));
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
