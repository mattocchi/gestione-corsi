package it.attocchi.service;

import it.attocchi.config.RecuperoPasswordProperties;
import java.util.Objects;
import it.attocchi.entity.LetteraIncarico;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import it.attocchi.config.AssociazioneProperties;
import it.attocchi.entity.Allievo;
import it.attocchi.entity.AnnoScolastico;
import it.attocchi.entity.Corso;
import it.attocchi.entity.Genitore;
import it.attocchi.entity.Indirizzo;
import it.attocchi.entity.Insegnante;
import it.attocchi.entity.GruppoCorso;
import it.attocchi.entity.Iscrizione;
import it.attocchi.entity.MetodoPagamento;
import it.attocchi.entity.Pagamento;
import it.attocchi.entity.TipoDocumento;
import it.attocchi.entity.User;
import it.attocchi.repository.UserRepository;
import it.attocchi.service.PagamentoService.Ricevuta;
import it.attocchi.service.PagamentoService.Attestazione;
import it.attocchi.service.PagamentoService.RiepilogoFamiglia;
import it.attocchi.service.PagamentoService.RigaPagamento;
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
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private final LetteraIncaricoService letteraIncaricoService;
    /** Indirizzo dell'app ({@code app.url}) per il link in fondo alle email ({@code email/piede.ftlh}). */
    private final String appUrl;

    public DocumentiService(PagamentoService pagamentoService, AssociazioneProperties associazione,
                            UserRepository userRepository, LetteraIncaricoService letteraIncaricoService,
                            RecuperoPasswordProperties applicazione) {
        this.appUrl = applicazione.url();
        this.letteraIncaricoService = letteraIncaricoService;
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
     * Avviso per l'allievo a cui mancano dati per almeno un documento, con i documenti per cui servono, es. "Manca
     * codice fiscale di Mario Rossi: serve per le ricevute"; {@code null} se i dati ci sono.
     */
    public static String avvisoDatiMancanti(Allievo allievo) {
        List<String> mancanti = allievo.getDatiMancantiPerDocumenti();
        if (mancanti.isEmpty()) {
            return null;
        }
        String perChi = mancanti.size() > 1 ? "la data di nascita serve per conferme e ricevute, il codice fiscale per le ricevute"
            : mancanti.contains("codice fiscale") ? "serve per le ricevute" : "serve per conferme e ricevute";
        return (mancanti.size() > 1 ? "Mancano " : "Manca ") + String.join(" e ", mancanti) + " di "
            + allievo.getNomeCompleto() + ": " + perChi;
    }

    /**
     * Avviso per l'allievo a cui mancano i dati del documento (la data di nascita, e per la ricevuta anche il codice
     * fiscale), es. "Manca data di nascita di Mario Rossi: … la conferma di iscrizione"; {@code null} se ci sono.
     */
    public static String avvisoDatiMancanti(Allievo allievo, TipoDocumento tipo) {
        List<String> mancanti = allievo.getDatiMancantiPer(tipo);
        if (mancanti.isEmpty()) {
            return null;
        }
        return (mancanti.size() > 1 ? "Mancano " : "Manca ") + String.join(" e ", mancanti) + " di "
            + allievo.getNomeCompleto() + ": completa i dati nella scheda dell'allievo per stampare o inviare la "
            + tipo.getTitolo().toLowerCase();
    }

    /** HTML del documento (pagina A4) dell'iscrizione. */
    public String documentoHtml(TipoDocumento tipo, Long corsoId, Long allievoId) {
        return render("documenti/" + tipo.getTemplate() + ".ftlh", modello(conDati(pagamentoService.ricevuta(corsoId, allievoId), tipo)));
    }

    /** PDF del documento dell'iscrizione. */
    public byte[] documentoPdf(TipoDocumento tipo, Long corsoId, Long allievoId) {
        return pdf(documentoHtml(tipo, corsoId, allievoId));
    }

    /**
     * Ricevuta in PDF per l'utente indicato: admin e segreteria le vedono tutte, il genitore solo quelle dei
     * propri figli, l'allievo adulto le proprie. Altrimenti (o se l'iscrizione non esiste)
     * {@link IllegalArgumentException}, senza distinguere i due casi per non rivelare le iscrizioni di altri.
     */
    public Pdf ricevutaPer(Long corsoId, Long allievoId, String username) {
        return documentoPer(TipoDocumento.RICEVUTA, corsoId, allievoId, username);
    }

    /** Conferma di iscrizione in PDF per l'utente indicato, con gli stessi permessi di {@link #ricevutaPer}. */
    public Pdf confermaPer(Long corsoId, Long allievoId, String username) {
        return documentoPer(TipoDocumento.CONFERMA_ISCRIZIONE, corsoId, allievoId, username);
    }

    /** Documento dell'iscrizione (conferma o ricevuta) in PDF, con i permessi di {@link #ricevutaPer}. */
    public Pdf documentoPer(TipoDocumento tipo, Long corsoId, Long allievoId, String username) {
        if (!tipo.isDellIscrizione()) {
            throw new IllegalArgumentException("Non è un documento dell'iscrizione: " + tipo);
        }
        Iscrizione iscrizione;
        try {
            iscrizione = pagamentoService.findIscrizione(corsoId, allievoId);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException(tipo.getBreve() + " non trovata");
        }
        // Il corso in preparazione (non pubblicato) lo vede solo lo staff
        if (!puoVedere(iscrizione.getAllievo(), username)
                || (!iscrizione.getCorso().isPubblicato() && !staff(username))) {
            throw new IllegalArgumentException(tipo.getBreve() + " non trovata");
        }
        return new Pdf(nomeFile(tipo, iscrizione), documentoPdf(tipo, corsoId, allievoId));
    }

    /** Come per il genitore; un allievo senza genitore (adulto) vede i propri documenti. */
    private boolean puoVedere(Allievo allievo, String username) {
        if (allievo.getGenitore() == null && allievo.getUser() != null && username.equals(allievo.getUser().getUsername())) {
            return true;
        }
        return puoVedere(allievo.getGenitore(), username);
    }

    /** Admin e segreteria sempre; altrimenti solo il genitore stesso. */
    private boolean staff(String username) {
        return userRepository.findByUsername(username).map(User::getRoles)
            .filter(ruoli -> ruoli.contains("ADMIN") || ruoli.contains("SEGRETERIA")).isPresent();
    }

    private boolean puoVedere(Genitore genitore, String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return false;
        }
        if (user.getRoles().contains("ADMIN") || user.getRoles().contains("SEGRETERIA")) {
            return true;
        }
        return user.getRoles().contains("GENITORE") && genitore != null && genitore.getUser() != null
            && username.equals(genitore.getUser().getUsername());
    }

    /**
     * Riepilogo della famiglia in PDF per l'utente indicato: admin e segreteria quello di ogni genitore, il
     * genitore solo il proprio. Altrimenti (o se il genitore non esiste) {@link IllegalArgumentException}.
     */
    public Pdf riepilogoFamigliaPer(Long genitoreId, String anno, String username) {
        RiepilogoFamiglia riepilogo;
        try {
            riepilogo = pagamentoService.riepilogoFamiglia(genitoreId, anno);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Riepilogo non trovato");
        }
        if (!puoVedere(riepilogo.genitore(), username)) {
            throw new IllegalArgumentException("Riepilogo non trovato");
        }
        return new Pdf(nomeFile(riepilogo), pdf(riepilogoFamigliaHtml(riepilogo)));
    }

    /**
     * Riepilogo PDF dei corsi di un allievo per anno: admin e segreteria ogni allievo, l'allievo adulto (senza
     * genitore) solo il proprio. Altrimenti {@link IllegalArgumentException}.
     */
    public Pdf riepilogoAllievoPer(Long allievoId, String anno, String username) {
        RiepilogoFamiglia riepilogo;
        try {
            riepilogo = pagamentoService.riepilogoAllievo(allievoId, anno);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Riepilogo non trovato");
        }
        if (!puoVedere(riepilogo.allievo(), username)) {
            throw new IllegalArgumentException("Riepilogo non trovato");
        }
        return new Pdf(nomeFile(riepilogo), pdf(riepilogoFamigliaHtml(riepilogo)));
    }

    /** PDF del riepilogo, senza controllo dei permessi (allegato delle email, inviate solo da admin e segreteria). */
    public Pdf riepilogoPdf(RiepilogoFamiglia riepilogo) {
        return new Pdf(nomeFile(riepilogo), pdf(riepilogoFamigliaHtml(riepilogo)));
    }

    /** Corpo HTML dell'email che accompagna il riepilogo; {@code saluto} è il nome del destinatario. */
    public String emailRiepilogoHtml(RiepilogoFamiglia riepilogo, String saluto) {
        Map<String, Object> modello = new HashMap<>();
        modello.put("associazione", associazione());
        modello.put("saluto", saluto);
        modello.put("famiglia", riepilogo.genitore() != null);
        modello.put("intestatario", riepilogo.intestatario());
        modello.put("anno", riepilogo.anno() != null ? "dell'anno scolastico " + riepilogo.anno() : "dei corsi senza anno scolastico");
        modello.put("numeroCorsi", riepilogo.righe().size());
        modello.put("totali", Map.of("quota", Formati.euro(riepilogo.quota()), "pagato", Formati.euro(riepilogo.pagato()),
            "residuo", Formati.euro(riepilogo.residuo())));
        return render("email/" + TipoDocumento.RIEPILOGO.getTemplate() + ".ftlh", modello);
    }

    /** HTML (pagina A4) del riepilogo della famiglia: corsi dei figli nell'anno con quota, pagato e residuo. */
    public String riepilogoFamigliaHtml(Long genitoreId, String anno) {
        return riepilogoFamigliaHtml(pagamentoService.riepilogoFamiglia(genitoreId, anno));
    }

    private String riepilogoFamigliaHtml(RiepilogoFamiglia riepilogo) {
        Map<String, Object> modello = new HashMap<>();
        modello.put("associazione", associazione());
        if (logo != null) {
            modello.put("logo", logo);
        }
        modello.put("emessa", Formati.dataCompleta(LocalDate.now()));
        modello.put("titolo", riepilogo.genitore() != null ? "Riepilogo famiglia" : "Riepilogo pagamenti");
        modello.put("etichettaIntestatario", riepilogo.genitore() != null ? "Genitore" : "Allievo");
        modello.put("intestatario", riepilogo.intestatario());
        modello.put("etichettaTotale", riepilogo.genitore() != null ? "Totale spesa della famiglia" : "Totale spesa");
        modello.put("anno", riepilogo.anno() != null ? "Anno scolastico " + riepilogo.anno() : "Corsi senza anno scolastico");
        List<Map<String, Object>> figli = new ArrayList<>();
        riepilogo.perFiglio().forEach((allievo, righe) -> {
            Map<String, Object> figlio = new HashMap<>();
            figlio.put("nome", allievo.getNomeCompleto());
            figlio.put("corsi", righe.stream().map(DocumentiService::rigaRiepilogo).toList());
            figlio.put("quota", Formati.euro(RiepilogoFamiglia.somma(righe, Situazione::quota)));
            figlio.put("pagato", Formati.euro(RiepilogoFamiglia.somma(righe, Situazione::pagato)));
            figlio.put("residuo", Formati.euro(RiepilogoFamiglia.somma(righe, Situazione::residuo)));
            figli.add(figlio);
        });
        modello.put("figli", figli);
        modello.put("numeroCorsi", riepilogo.righe().size());
        modello.put("totali", Map.of("quota", Formati.euro(riepilogo.quota()), "pagato", Formati.euro(riepilogo.pagato()),
            "residuo", Formati.euro(riepilogo.residuo())));
        return render("documenti/riepilogo-famiglia.ftlh", modello);
    }

    private static Map<String, String> rigaRiepilogo(RigaPagamento riga) {
        Corso corso = riga.corso();
        Iscrizione iscrizione = riga.iscrizione();
        Map<String, String> corsoRiga = new LinkedHashMap<>();
        corsoRiga.put("nome", corso.getNome());
        corsoRiga.put("insegnanti", corso.getNomiInsegnanti());
        corsoRiga.put("nota", iscrizione.getQuotaPersonalizzata() != null
            ? "quota piena " + Formati.euro(corso.getCostoTotale())
                + (iscrizione.getNotaQuota() != null ? ", " + iscrizione.getNotaQuota() : "")
            : "");
        corsoRiga.put("quota", Formati.euro(riga.situazione().quota()));
        corsoRiga.put("pagato", Formati.euro(riga.situazione().pagato()));
        corsoRiga.put("residuo", Formati.euro(riga.situazione().residuo()));
        return corsoRiga;
    }

    /** Nome del file PDF del riepilogo, es. "riepilogo-famiglia-mario-rossi-2026-2027.pdf". */
    public String nomeFile(RiepilogoFamiglia riepilogo) {
        String base = (riepilogo.genitore() != null ? "riepilogo-famiglia-" : "riepilogo-pagamenti-") + riepilogo.intestatario()
            + (riepilogo.anno() != null ? "-" + riepilogo.anno() : "");
        return base.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "") + ".pdf";
    }

    /** Lettera di incarico in PDF: admin e segreteria ogni lettera, l'insegnante le proprie; altrimenti {@link IllegalArgumentException}. */
    public Pdf letteraPer(Long letteraId, String username) {
        return letteraPdf(letteraIncaricoService.lettera(letteraId, username));
    }

    /** PDF della lettera, senza controllo dei permessi (allegato delle email, inviate solo da admin e segreteria). */
    public Pdf letteraPdf(LetteraIncarico lettera) {
        return new Pdf(nomeFile(lettera), pdf(letteraHtml(lettera)));
    }

    /** Corpo HTML dell'email che accompagna la lettera di incarico; {@code saluto} è il nome dell'insegnante. */
    public String emailLetteraHtml(LetteraIncarico lettera, String saluto) {
        Map<String, Object> modello = modelloLettera(lettera);
        modello.put("saluto", saluto);
        return render("email/" + TipoDocumento.LETTERA_INCARICO.getTemplate() + ".ftlh", modello);
    }

    /** HTML (pagina A4) della lettera di incarico, con i dati fissati all'emissione. */
    public String letteraHtml(LetteraIncarico lettera) {
        Map<String, Object> modello = modelloLettera(lettera);
        if (logo != null) {
            modello.put("logo", logo);
        }
        modello.put("emessa", Formati.dataCompleta(lettera.getDataEmissione()));
        Insegnante insegnante = lettera.getInsegnante();
        modello.put("codiceFiscale", testo(insegnante.getCodiceFiscale()));
        modello.put("indirizzo", testo(insegnante.getIndirizzo().getFormattato()));
        modello.put("nascita", insegnante.getDataNascita() != null ? Formati.dataNumerica(insegnante.getDataNascita())
            + (insegnante.getLuogoNascita() != null ? " a " + insegnante.getLuogoNascita() : "") : "");
        return render("documenti/" + TipoDocumento.LETTERA_INCARICO.getTemplate() + ".ftlh", modello);
    }

    private Map<String, Object> modelloLettera(LetteraIncarico lettera) {
        Corso corso = lettera.getCorso();
        Map<String, Object> modello = new HashMap<>();
        modello.put("associazione", associazione());
        modello.put("insegnante", lettera.getInsegnante().getNomeCompleto());
        modello.put("servizio", corso.isServizio());
        modello.put("tipo", corso.getTipo().getDescrizione().toLowerCase());
        modello.put("corso", corso.getNome());
        modello.put("anno", testo(lettera.getAnnoScolastico()));
        modello.put("giorniEOrari", testo(lettera.getGiorniEOrari()));
        modello.put("luogo", testo(corso.getNomeLuogo()));
        modello.put("dataInizio", corso.getDataInizio() != null ? Formati.dataCompleta(corso.getDataInizio()) : "");
        modello.put("tariffaOraria", lettera.getTariffaOraria() != null ? Formati.euro(lettera.getTariffaOraria()) : "");
        modello.put("unaTantum", lettera.getImportoUnaTantum() != null ? Formati.euro(lettera.getImportoUnaTantum()) : "");
        modello.put("nota", testo(lettera.getNota()));
        return modello;
    }

    /** Nome del file PDF della lettera, es. "lettera-incarico-2026-2027-corso-di-canto-mario-rossi.pdf". */
    public String nomeFile(LetteraIncarico lettera) {
        String base = "lettera-incarico-" + Objects.toString(lettera.getAnnoScolastico(), "") + "-" + lettera.getCorso().getNome()
            + "-" + lettera.getInsegnante().getNomeCompleto();
        return base.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "") + ".pdf";
    }

    /**
     * Attestazione delle spese di un allievo in un anno solare: admin e segreteria ogni allievo, il genitore i figli,
     * l'allievo adulto la propria. Altrimenti {@link IllegalArgumentException}.
     */
    public Pdf attestazionePer(Long allievoId, int anno, String username) {
        Attestazione attestazione;
        try {
            attestazione = pagamentoService.attestazione(allievoId, anno);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Attestazione non trovata");
        }
        if (!puoVedere(attestazione.allievo(), username)) {
            throw new IllegalArgumentException("Attestazione non trovata");
        }
        return attestazionePdf(attestazione);
    }

    /** PDF dell'attestazione, senza controllo dei permessi (allegato delle email, inviate solo da admin e segreteria). */
    public Pdf attestazionePdf(Attestazione attestazione) {
        return new Pdf(nomeFile(attestazione), pdf(attestazioneHtml(attestazione)));
    }

    /** Corpo HTML dell'email che accompagna l'attestazione; {@code saluto} è il nome del destinatario. */
    public String emailAttestazioneHtml(Attestazione attestazione, String saluto) {
        Map<String, Object> modello = new HashMap<>();
        modello.put("associazione", associazione());
        modello.put("saluto", saluto);
        modello.put("allievo", attestazione.allievo().getNomeCompleto());
        modello.put("famiglia", attestazione.genitore() != null);
        modello.put("anno", String.valueOf(attestazione.anno()));
        modello.put("numeroPagamenti", attestazione.pagamenti().size());
        modello.put("totale", Formati.euro(attestazione.totale()));
        return render("email/" + TipoDocumento.ATTESTAZIONE.getTemplate() + ".ftlh", modello);
    }

    /** HTML (pagina A4) dell'attestazione: allievo, chi sostiene le spese, rate dell'anno e totale. */
    public String attestazioneHtml(Attestazione attestazione) {
        Map<String, Object> modello = new HashMap<>();
        modello.put("associazione", associazione());
        if (logo != null) {
            modello.put("logo", logo);
        }
        modello.put("emessa", Formati.dataCompleta(LocalDate.now()));
        modello.put("anno", String.valueOf(attestazione.anno()));
        Allievo allievo = attestazione.allievo();
        modello.put("allievo", persona(allievo.getNomeCompleto(), allievo.getCodiceFiscale(), allievo.getDataNascita()));
        Genitore genitore = attestazione.genitore();
        if (genitore != null) {
            modello.put("genitore", persona(genitore.getNomeCompleto(), genitore.getCodiceFiscale(), null));
        }
        modello.put("pagamenti", attestazione.pagamenti().stream().map(p -> Map.of(
            "data", Formati.dataNumerica(p.getData()),
            "corso", p.getIscrizione().getCorso().getNome()
                + (p.getIscrizione().getCorso().getAnnoScolastico() != null ? " (" + p.getIscrizione().getCorso().getAnnoScolastico() + ")" : ""),
            "metodo", p.getMetodo().getDescrizione(),
            "importo", Formati.euro(p.getImporto()))).toList());
        modello.put("totale", Formati.euro(attestazione.totale()));
        return render("documenti/" + TipoDocumento.ATTESTAZIONE.getTemplate() + ".ftlh", modello);
    }

    private static Map<String, String> persona(String nome, String codiceFiscale, LocalDate nascita) {
        Map<String, String> persona = new HashMap<>();
        persona.put("nome", nome);
        persona.put("codiceFiscale", codiceFiscale != null ? codiceFiscale : "");
        persona.put("nascita", nascita != null ? Formati.dataNumerica(nascita) : "");
        return persona;
    }

    /** Nome del file PDF dell'attestazione, es. "attestazione-spese-2026-mario-rossi.pdf". */
    public String nomeFile(Attestazione attestazione) {
        String base = "attestazione-spese-" + attestazione.anno() + "-" + attestazione.allievo().getNomeCompleto();
        return base.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "") + ".pdf";
    }

    /** Corpo HTML dell'email che accompagna il documento; {@code saluto} è il nome del destinatario. */
    public String emailHtml(TipoDocumento tipo, Long corsoId, Long allievoId, String saluto) {
        Map<String, Object> modello = modello(conDati(pagamentoService.ricevuta(corsoId, allievoId), tipo));
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

    /**
     * Invito ad accedere, o reimpostazione dell'accesso ({@code reimpostazione}): link per impostare la password,
     * valido {@code giorni} giorni.
     */
    public String emailInvitoHtml(String saluto, String username, String link, int giorni, boolean reimpostazione) {
        Map<String, Object> modello = new HashMap<>();
        modello.put("associazione", associazione());
        modello.put("saluto", saluto);
        modello.put("username", username);
        modello.put("link", link);
        modello.put("giorni", giorni);
        modello.put("reimpostazione", reimpostazione);
        return render("email/invito.ftlh", modello);
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

    /** Rifiuta il documento dell'allievo senza i dati che servono ({@link IllegalStateException}). */
    private static Ricevuta conDati(Ricevuta ricevuta, TipoDocumento tipo) {
        String avviso = avvisoDatiMancanti(ricevuta.iscrizione().getAllievo(), tipo);
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
            modello.putIfAbsent("appUrl", appUrl);
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
            "dataNascita", Formati.dataNumerica(allievo.getDataNascita()), "indirizzo", testo(Formati.indirizzo(allievo))));
        if (genitore != null) {
            modello.put("genitore", Map.of("nome", genitore.getNomeCompleto(), "codiceFiscale", testo(genitore.getCodiceFiscale())));
        }
        modello.put("corso", Map.of("nome", corso.getNome(), "anno", testo(corso.getAnnoScolastico()),
            "insegnante", corso.getNomiInsegnanti(),
            "etichettaInsegnante", corso.getInsegnanti().size() > 1 ? "Insegnanti" : "Insegnante",
            "quota", Formati.euro(situazione.quota()), "giorni", giorni(iscrizione)));
        modello.put("importi", importi(iscrizione, situazione));
        modello.put("modalitaPagamento", modalitaPagamento(corso.getAnnoScolastico()));
        List<Map<String, String>> pagamenti = ricevuta.pagamenti().stream().map(DocumentiService::pagamento).toList();
        modello.put("pagamenti", pagamenti);
        modello.put("totali", Map.of("quota", Formati.euro(situazione.quota()), "pagato", Formati.euro(situazione.pagato()),
            "residuo", Formati.euro(situazione.residuo())));
        return modello;
    }

    /**
     * Importi della conferma e della ricevuta: quota dovuta, pagato e residuo; con una quota personalizzata diversa
     * dal listino anche il prezzo di listino, la riga dello sconto (o maggiorazione) con il motivo e la nota che
     * chiarisce che l'importo dovuto è la quota.
     */
    private static Map<String, Object> importi(Iscrizione iscrizione, Situazione situazione) {
        Corso corso = iscrizione.getCorso();
        Map<String, Object> importi = new HashMap<>();
        importi.put("quota", Formati.euro(situazione.quota()));
        importi.put("pagato", Formati.euro(situazione.pagato()));
        importi.put("residuo", Formati.euro(situazione.residuo()));
        BigDecimal listino = corso.getCostoTotale();
        BigDecimal differenza = listino != null && iscrizione.getQuotaPersonalizzata() != null
            ? iscrizione.getQuotaPersonalizzata().subtract(listino) : BigDecimal.ZERO;
        importi.put("conVariazione", differenza.signum() != 0);
        if (differenza.signum() != 0) {
            boolean sconto = differenza.signum() < 0;
            String motivo = iscrizione.getNotaQuota() != null ? iscrizione.getNotaQuota()
                : sconto && corso.getPrezzoPreiscrizione() != null
                    && iscrizione.getQuotaPersonalizzata().compareTo(corso.getPrezzoPreiscrizione()) == 0 ? "pre-iscrizione" : null;
            importi.put("listino", Formati.euro(listino));
            importi.put("etichettaVariazione", (sconto ? "Sconto applicato" : "Maggiorazione applicata")
                + (motivo != null ? " (" + motivo + ")" : ""));
            // Trattino normale: il segno meno tipografico non è nel font del PDF
            importi.put("variazione", (sconto ? "-" : "+") + Formati.euro(differenza.abs()));
        }
        return importi;
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
        allievo.setDataNascita(LocalDate.of(2012, 3, 5));
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

    /**
     * Modalità di pagamento della conferma: unica soluzione entro il 30 novembre o due rate (30 novembre e 31
     * gennaio), con gli anni dall'anno scolastico del corso ("2026/2027"); senza anno scolastico valido le date senza anno.
     */
    static String modalitaPagamento(String annoScolastico) {
        String novembre = "30 novembre";
        String gennaio = "31 gennaio";
        if (AnnoScolastico.valido(annoScolastico)) {
            int inizio = Integer.parseInt(annoScolastico.substring(0, 4));
            novembre += " " + inizio;
            gennaio += " " + (inizio + 1);
        }
        return "Il pagamento può avvenire in un'unica soluzione entro il " + novembre + " oppure in due rate: la prima entro il "
            + novembre + " e la seconda entro il " + gennaio + ".";
    }

    /** Giorni di lezione per la conferma: "sabato alle 10:00" dal gruppo, altrimenti i giorni del corso. */
    private static String giorni(Iscrizione iscrizione) {
        GruppoCorso gruppo = iscrizione.getGruppo();
        String giorni = Formati.giorniEstesi(iscrizione.giorniEffettivi());
        return gruppo != null && gruppo.getOrario() != null && giorni.equals(Formati.giorniEstesi(Set.of(gruppo.getGiorno())))
            ? giorni + " alle " + Formati.ora(gruppo.getOrario()) : giorni;
    }
}
