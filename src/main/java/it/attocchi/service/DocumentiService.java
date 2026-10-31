package it.attocchi.service;

import it.attocchi.config.RecuperoPasswordProperties;
import java.util.stream.Collectors;
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
import it.attocchi.service.PagamentoService.IscrittiCorso;
import it.attocchi.service.PagamentoService.Ricevuta;
import it.attocchi.service.PagamentoService.RiepilogoPagamenti;
import it.attocchi.service.RiepilogoCorsiService.RiepilogoCorsi;
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
    private final RiepilogoCorsiService riepilogoCorsiService;
    /** Indirizzo dell'app ({@code app.url}) per il link in fondo alle email ({@code email/piede.ftlh}). */
    private final String appUrl;

    public DocumentiService(PagamentoService pagamentoService, AssociazioneProperties associazione,
                            UserRepository userRepository, LetteraIncaricoService letteraIncaricoService,
                            RecuperoPasswordProperties applicazione, RiepilogoCorsiService riepilogoCorsiService) {
        this.appUrl = applicazione.url();
        this.riepilogoCorsiService = riepilogoCorsiService;
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
        corsoRiga.put("anno", corso.getAnnoScolastico() != null ? "Anno scolastico " + corso.getAnnoScolastico() : "");
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

    /** Riepilogo dei corsi attivi in PDF, solo per admin e segreteria; altrimenti {@link IllegalArgumentException}. */
    public Pdf riepilogoCorsiPer(String username) {
        return riepilogoCorsiPdf(riepilogoCorsi(username));
    }

    /** Dati del riepilogo dei corsi attivi, solo per admin e segreteria; altrimenti {@link IllegalArgumentException}. */
    public RiepilogoCorsi riepilogoCorsi(String username) {
        if (!staff(username)) {
            throw new IllegalArgumentException("Riepilogo non trovato");
        }
        return riepilogoCorsiService.riepilogo();
    }

    /** PDF del riepilogo dei corsi, senza controllo dei permessi (allegato dell'email). */
    public Pdf riepilogoCorsiPdf(RiepilogoCorsi riepilogo) {
        return new Pdf(nomeFile(riepilogo), pdf(riepilogoCorsiHtml(riepilogo)));
    }

    /** Nome del file PDF, es. "riepilogo-corsi-attivi-2026-10-31.pdf". */
    public static String nomeFile(RiepilogoCorsi riepilogo) {
        return "riepilogo-corsi-attivi-" + riepilogo.data() + ".pdf";
    }

    /** Corpo HTML dell'email che accompagna il riepilogo dei corsi, con i numeri principali. */
    public String emailRiepilogoCorsiHtml(RiepilogoCorsi riepilogo, String saluto) {
        Map<String, Object> modello = new HashMap<>();
        modello.put("associazione", associazione());
        modello.put("saluto", saluto);
        modello.put("data", Formati.dataCompleta(riepilogo.data()));
        RiepilogoPagamenti totale = riepilogo.totale();
        modello.put("corsi", riepilogo.corsi().size());
        modello.put("iscritti", riepilogo.generale().iscritti());
        modello.put("incassato", Formati.euro(totale.incassato()));
        modello.put("daIncassare", Formati.euro(totale.daIncassare()));
        return render("email/" + TipoDocumento.RIEPILOGO_CORSI.getTemplate() + ".ftlh", modello);
    }

    /**
     * HTML (A4 orizzontale) del riepilogo dei corsi attivi: le sezioni Pagamenti e Gruppi della dashboard di admin e
     * segreteria con le stesse etichette (senza il riepilogo generale), poi per ogni corso gli iscritti divisi per gruppo con orario e stato del pagamento.
     */
    public String riepilogoCorsiHtml(RiepilogoCorsi riepilogo) {
        Map<String, Object> modello = new HashMap<>();
        modello.put("associazione", associazione());
        if (logo != null) {
            modello.put("logo", logo);
        }
        modello.put("titolo", TipoDocumento.RIEPILOGO_CORSI.getTitolo());
        modello.put("emessa", Formati.dataCompleta(riepilogo.data()));

        RiepilogoPagamenti totale = riepilogo.totale();
        long senzaTariffa = riepilogo.corsiSenzaTariffa();
        String esclusi = senzaTariffa == 0 ? null
            : senzaTariffa + (senzaTariffa == 1 ? " corso senza tariffa escluso" : " corsi senza tariffa esclusi");
        List<Map<String, String>> pagamenti = new ArrayList<>(List.of(
            riquadro("Saldati", totale.saldati(), null),
            riquadro("Pagamento parziale", totale.parziali(), null),
            riquadro("Senza pagamenti", totale.senzaPagamenti(), null),
            riquadro("Incassato", Formati.euro(totale.incassato()), totale.percentualeIncassata() + "% del totale"),
            riquadro("Da incassare", Formati.euro(totale.daIncassare()), totale.percentualeDaIncassare() + "% del totale"),
            riquadro("Costo insegnanti previsto", Formati.euro(riepilogo.costoTotale()),
                esclusi != null ? esclusi : "lezioni programmate"),
            riquadro("Margine previsto", Formati.euro(riepilogo.margineTotale()),
                esclusi != null ? esclusi : "quote - costo insegnanti")));
        riepilogo.oreDonate().ifPresent(ore -> pagamenti.add(riquadro("Ore donate", Formati.durata(ore.minutiPrevisti()),
            "di cui " + Formati.durata(ore.minutiSvolti()) + " già svolte · " + ore.insegnanti()
                + (ore.insegnanti() == 1 ? " insegnante" : " insegnanti"))));
        modello.put("pagamenti", pagamenti);
        modello.put("pagamentiCorsi", riepilogo.corsi().stream().map(corso -> {
            Map<String, String> riga = rigaPagamenti(corso);
            BigDecimal costo = riepilogo.costo(corso.corsoId());
            BigDecimal margine = riepilogo.margine(corso);
            riga.put("costo", costo != null ? Formati.euro(costo) : "Senza tariffa");
            riga.put("margine", margine != null ? Formati.euro(margine) : "");
            riga.put("esito", margine == null ? "" : margine.signum() >= 0 ? "Attivo" : "Perdita");
            return riga;
        }).toList());
        Map<String, String> rigaTotale = rigaPagamenti(totale);
        rigaTotale.put("costo", Formati.euro(riepilogo.costoTotale()));
        rigaTotale.put("margine", Formati.euro(riepilogo.margineTotale()));
        rigaTotale.put("esito", "");
        modello.put("pagamentiTotale", rigaTotale);

        modello.put("gruppiRiquadri", List.of(
            riquadro("Corsi con gruppi", riepilogo.gruppi().corsiConGruppi(), null),
            riquadro("Gruppi", riepilogo.gruppi().gruppi(), null),
            riquadro("Allievi senza gruppo", riepilogo.gruppi().allieviSenzaGruppo(), null)));
        modello.put("gruppi", riepilogo.gruppi().righe().stream().map(r -> Map.of("corso", r.corso(),
            "gruppo", Formati.gruppo(r.gruppo()), "iscritti", String.valueOf(r.iscritti()))).toList());

        modello.put("corsi", riepilogo.iscritti().stream().map(DocumentiService::corsoConIscritti).toList());
        return render("documenti/" + TipoDocumento.RIEPILOGO_CORSI.getTemplate() + ".ftlh", modello);
    }

    private static Map<String, String> riquadro(String etichetta, Object valore, String dettaglio) {
        return Map.of("etichetta", etichetta, "valore", String.valueOf(valore), "dettaglio", testo(dettaglio));
    }

    private static Map<String, String> rigaPagamenti(RiepilogoPagamenti corso) {
        Map<String, String> riga = new HashMap<>();
        riga.put("corso", testo(corso.corso()));
        riga.put("saldati", String.valueOf(corso.saldati()));
        riga.put("parziali", String.valueOf(corso.parziali()));
        riga.put("senzaPagamenti", String.valueOf(corso.senzaPagamenti()));
        riga.put("incassato", Formati.euro(corso.incassato()) + " (" + corso.percentualeIncassata() + "%)");
        riga.put("daIncassare", Formati.euro(corso.daIncassare()) + " (" + corso.percentualeDaIncassare() + "%)");
        return riga;
    }

    /** Un corso con i totali e gli iscritti per gruppo (nell'ordine dei gruppi, poi chi non ne ha). */
    private static Map<String, Object> corsoConIscritti(IscrittiCorso iscritti) {
        Corso corso = iscritti.corso();
        Map<String, Object> modello = new HashMap<>();
        modello.put("nome", corso.getNome());
        List<String> dettagli = new ArrayList<>();
        if (corso.getAnnoScolastico() != null) {
            dettagli.add("Anno scolastico " + corso.getAnnoScolastico());
        }
        dettagli.add(Formati.giorniECadenza(corso));
        dettagli.add((corso.isServizio() ? "Incaricati: " : "Insegnanti: ") + corso.getNomiInsegnanti());
        modello.put("dettaglio", String.join(" · ", dettagli));
        int numero = iscritti.iscritti().size();
        modello.put("numeroIscritti", numero + (numero == 1 ? " iscritto" : " iscritti"));
        modello.put("totali", Map.of("quota", Formati.euro(iscritti.quota()), "pagato", Formati.euro(iscritti.pagato()),
            "residuo", Formati.euro(iscritti.residuo()), "saldati", String.valueOf(iscritti.saldati())));
        // Senza iscritti nessuna sezione (il template scrive "Nessun iscritto")
        List<Map<String, Object>> sezioni = new ArrayList<>();
        if (numero > 0 && iscritti.gruppi().isEmpty()) {
            sezioni.add(sezioneIscritti("", iscritti.iscritti()));
        } else if (numero > 0) {
            for (GruppoCorso gruppo : iscritti.gruppi()) {
                sezioni.add(sezioneIscritti(Formati.gruppo(gruppo), IscrittiCorso.delGruppo(iscritti.iscritti(), gruppo)));
            }
            List<RigaPagamento> senzaGruppo = IscrittiCorso.delGruppo(iscritti.iscritti(), null);
            if (!senzaGruppo.isEmpty()) {
                sezioni.add(sezioneIscritti("Senza gruppo (tutti i giorni)", senzaGruppo));
            }
        }
        modello.put("sezioni", sezioni);
        return modello;
    }

    private static Map<String, Object> sezioneIscritti(String gruppo, List<RigaPagamento> righe) {
        List<Map<String, String>> iscritti = new ArrayList<>();
        for (RigaPagamento riga : righe) {
            Map<String, String> iscritto = new HashMap<>();
            iscritto.put("numero", String.valueOf(iscritti.size() + 1));
            iscritto.put("allievo", riga.allievo().getNomeCompleto());
            iscritto.put("genitore", riga.genitore() != null ? riga.genitore().getNomeCompleto() : "");
            iscritto.put("orario", Formati.ora(riga.iscrizione().orarioAbituale()));
            iscritto.put("quota", Formati.euro(riga.situazione().quota()));
            iscritto.put("pagato", Formati.euro(riga.situazione().pagato()));
            iscritto.put("residuo", Formati.euro(riga.situazione().residuo()));
            iscritto.put("stato", riga.situazione().stato().getDescrizione());
            iscritti.add(iscritto);
        }
        return Map.of("gruppo", gruppo, "numeroIscritti", righe.size() + (righe.size() == 1 ? " iscritto" : " iscritti"),
            "iscritti", iscritti);
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
        String mancanti = avvisoDatiMancanti(attestazione);
        if (mancanti != null) {
            throw new IllegalStateException(mancanti);
        }
        return attestazionePdf(attestazione);
    }

    /**
     * Avviso se mancano i dati richiesti dalla dichiarazione per la detrazione (fac-simile del vademecum), es.
     * "Per la dichiarazione mancano: codice fiscale dell'allievo"; {@code null} se completi.
     */
    public static String avvisoDatiMancanti(Attestazione attestazione) {
        List<String> mancanti = attestazione.datiMancanti();
        return mancanti.isEmpty() ? null : "Per la dichiarazione mancano: " + String.join(", ", mancanti);
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
        modello.put("data", Formati.dataNumerica(LocalDate.now()));
        modello.put("anno", String.valueOf(attestazione.anno()));
        // Fascia 5-18 anni: il testo del fac-simile ("il minore", detrazione del 19%); fuori fascia un testo generico
        modello.put("minore", attestazione.inEta());
        Allievo allievo = attestazione.allievo();
        Indirizzo residenza = allievo.getIndirizzoEffettivo();
        Map<String, String> datiAllievo = new HashMap<>();
        datiAllievo.put("nome", allievo.getUser() != null
            ? (testo(allievo.getUser().getCognome()) + " " + testo(allievo.getUser().getNome())).strip() : allievo.getNomeCompleto());
        datiAllievo.put("luogoNascita", testo(allievo.getLuogoNascita()));
        datiAllievo.put("nascita", allievo.getDataNascita() != null ? Formati.dataNumerica(allievo.getDataNascita()) : "");
        datiAllievo.put("codiceFiscale", testo(allievo.getCodiceFiscale()));
        datiAllievo.put("comune", testo(residenza.getComune()));
        datiAllievo.put("provincia", testo(residenza.getProvincia()));
        datiAllievo.put("via", testo(residenza.getVia()));
        datiAllievo.put("cap", testo(residenza.getCap()));
        modello.put("allievo", datiAllievo);
        // Intestatario (Spett.): il genitore che sostiene le spese, o l'allievo adulto, con nome, cognome e codice
        // fiscale dell'intestazione dei documenti (es. il coniuge che detrae); l'indirizzo resta del genitore
        Genitore genitore = attestazione.genitore();
        Map<String, String> intestatario = new HashMap<>();
        intestatario.put("nome", attestazione.intestatario().getNomeIntestatario());
        intestatario.put("indirizzo", testo((genitore != null ? genitore.getIndirizzo() : residenza).getFormattato()));
        intestatario.put("codiceFiscale", testo(attestazione.intestatario().getCodiceFiscaleIntestatario()));
        modello.put("intestatario", intestatario);
        modello.put("corsi", attestazione.pagamenti().stream().map(p -> p.getIscrizione().getCorso().getNome())
            .distinct().collect(Collectors.joining(", ")));
        modello.put("pagamenti", attestazione.pagamenti().stream().map(p -> Map.of(
            "data", Formati.dataNumerica(p.getData()),
            "corso", p.getIscrizione().getCorso().getNome()
                + (p.getIscrizione().getCorso().getAnnoScolastico() != null ? " (" + p.getIscrizione().getCorso().getAnnoScolastico() + ")" : ""),
            "metodo", p.getMetodo().getDescrizione(),
            "importo", Formati.euro(p.getImporto()))).toList());
        modello.put("totale", Formati.euro(attestazione.totale()));
        modello.put("totaleInLettere", Formati.inLettere(attestazione.totale()));
        return render("documenti/" + TipoDocumento.ATTESTAZIONE.getTemplate() + ".ftlh", modello);
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
     * Invito ad accedere, o reimpostazione dell'accesso ({@code reimpostazione}): si entra con lo username o con
     * {@code email} ({@code null} se l'email è di più account), link per impostare la password valido {@code giorni} giorni.
     */
    public String emailInvitoHtml(String saluto, String username, String email, String link, int giorni,
                                  boolean reimpostazione) {
        Map<String, Object> modello = new HashMap<>();
        modello.put("associazione", associazione());
        modello.put("saluto", saluto);
        modello.put("username", username);
        modello.put("link", link);
        modello.put("giorni", giorni);
        modello.put("reimpostazione", reimpostazione);
        // Vuota se l'email non basta per entrare (usata da più account)
        modello.put("email", email != null ? email : "");
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
            "iban", testo(associazione.iban()), "luogo", testo(associazione.luogo()));
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
