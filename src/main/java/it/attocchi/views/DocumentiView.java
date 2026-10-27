package it.attocchi.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import it.attocchi.entity.AnnoScolastico;
import it.attocchi.entity.Corso;
import it.attocchi.entity.InvioEmail;
import it.attocchi.entity.LetteraIncarico;
import it.attocchi.entity.TipoDocumento;
import it.attocchi.security.SecurityService;
import it.attocchi.service.AllievoService;
import it.attocchi.service.DocumentiService;
import it.attocchi.service.DocumentiService.Pdf;
import it.attocchi.service.ElencoDocumentiService;
import it.attocchi.service.LetteraIncaricoService;
import it.attocchi.service.ElencoDocumentiService.DocumentoAttestazione;
import it.attocchi.service.ElencoDocumentiService.DocumentoFamiglia;
import it.attocchi.service.ElencoDocumentiService.DocumentoIscrizione;
import it.attocchi.service.ElencoDocumentiService.DocumentoRiepilogo;
import it.attocchi.service.EmailService.Destinatario;
import it.attocchi.views.admin.InvioEmailDialog;
import it.attocchi.views.admin.LetteraIncaricoDialog;
import jakarta.annotation.security.RolesAllowed;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Documenti in PDF e loro invio per email. Admin e segreteria scelgono il tipo (conferme di iscrizione, ricevute,
 * riepiloghi annuali) e, su ogni riga, vedono o scaricano il PDF e lo inviano (con l'anteprima dell'email); scelto un
 * corso, inviano le conferme a tutti i suoi iscritti. Genitori e allievi senza genitore vedono o scaricano i propri
 * documenti (dei figli o propri), senza invii; la voce è nascosta all'allievo con genitore ({@code MainLayout}).
 * Permessi di PDF e invii in {@link DocumentiService} e {@code EmailService}.
 */
@Route(value = DocumentiView.PERCORSO, layout = MainLayout.class)
@PageTitle("Documenti")
@Menu(title = "Documenti", order = 42, icon = "vaadin:file-text-o")
@RolesAllowed({"ADMIN", "SEGRETERIA", "GENITORE", "ALLIEVO", "INSEGNANTE"})
public class DocumentiView extends VerticalLayout {

    /** Percorso della vista, usato da {@code MainLayout} per nasconderla all'allievo con genitore. */
    public static final String PERCORSO = "documenti";

    private final ElencoDocumentiService elencoDocumentiService;
    private final DocumentiService documentiService;
    private final LetteraIncaricoService letteraIncaricoService;
    private final String username;

    private final SegmentedControl<TipoDocumento> tipo;
    private final ComboBox<String> filtroAnno = new ComboBox<>();
    private final ComboBox<Corso> filtroCorso = new ComboBox<>();
    private final RicercaRapida ricerca = new RicercaRapida();
    private final Button confermeATutti = new Button("Invia conferme di iscrizione a tutti", VaadinIcon.ENVELOPES.create());
    private final Grid<DocumentoIscrizione> gridIscrizioni = new Grid<>();
    private final Grid<DocumentoRiepilogo> gridRiepiloghi = new Grid<>();
    private final Grid<DocumentoAttestazione> gridAttestazioni = new Grid<>();
    private final Grid<LetteraIncarico> gridLettere = new Grid<>();
    private final Button nuovaLettera = new Button("Nuova lettera di incarico", VaadinIcon.PLUS.create());
    private List<LetteraIncarico> lettere = List.of();
    private GridListDataView<LetteraIncarico> righeLettere;
    private boolean admin;
    private List<DocumentoAttestazione> attestazioni = List.of();
    private GridListDataView<DocumentoAttestazione> righeAttestazioni;
    private List<DocumentoIscrizione> iscrizioni = List.of();
    private List<DocumentoRiepilogo> riepiloghi = List.of();
    private GridListDataView<DocumentoIscrizione> righeIscrizioni;
    private GridListDataView<DocumentoRiepilogo> righeRiepiloghi;
    private boolean primaApertura = true;

    public DocumentiView(ElencoDocumentiService elencoDocumentiService, DocumentiService documentiService,
                         AllievoService allievoService, LetteraIncaricoService letteraIncaricoService,
                         SecurityService securityService) {
        this.letteraIncaricoService = letteraIncaricoService;
        this.admin = securityService.hasRole("ADMIN");
        this.elencoDocumentiService = elencoDocumentiService;
        this.documentiService = documentiService;
        this.username = securityService.getUsername().orElse("");
        addClassNames("size-full");
        tipo = new SegmentedControl<>("Tipo di documento", List.of(TipoDocumento.values()), TipoDocumento::getPlurale,
            t -> t == TipoDocumento.RIEPILOGO ? "Riepiloghi annuali dei pagamenti"
                : t == TipoDocumento.ATTESTAZIONE ? "Attestazioni delle spese per anno solare" : t.getTitolo(), null);
        if (!securityService.hasRole("ADMIN") && !securityService.hasRole("SEGRETERIA")) {
            boolean famiglia = securityService.hasRole("GENITORE") || allievoService.gestisceDaSe(username);
            if (securityService.hasRole("INSEGNANTE")) {
                creaElencoInsegnante();
                if (!famiglia) {
                    return;
                }
            }
            if (!famiglia) {
                // Allievo con genitore (la voce di menu è nascosta, qui solo dall'indirizzo)
                Span avviso = new Span("I documenti dei tuoi corsi sono nell'area del genitore.");
                avviso.addClassNames(Stili.TESTO_SECONDARIO);
                add(avviso);
                return;
            }
            creaElencoFamiglia();
            return;
        }
        tipo.setValue(TipoDocumento.CONFERMA_ISCRIZIONE);
        tipo.addClassNames("max-w-xl");
        tipo.addValueChangeListener(e -> aggiorna());

        filtroAnno.setPlaceholder("Tutti gli anni");
        filtroAnno.setAriaLabel("Filtra per anno scolastico");
        filtroAnno.setClearButtonVisible(true);
        filtroAnno.addClassNames("w-40");
        filtroAnno.addValueChangeListener(e -> {
            aggiornaCorsi();
            applicaFiltri();
        });
        filtroCorso.setPlaceholder("Tutti i corsi");
        filtroCorso.setAriaLabel("Filtra per corso");
        filtroCorso.setClearButtonVisible(true);
        filtroCorso.setItemLabelGenerator(Corso::getNome);
        filtroCorso.addClassNames("w-60");
        filtroCorso.addValueChangeListener(e -> applicaFiltri());
        ricerca.addRicercaListener(this::applicaFiltri);
        confermeATutti.addClickListener(e -> {
            if (filtroCorso.getValue() != null) {
                InvioEmailDialog.inviaConferme(filtroCorso.getValue(), this::aggiorna);
            }
        });
        HorizontalLayout filtri = new HorizontalLayout(filtroAnno, filtroCorso, ricerca, confermeATutti);
        filtri.addClassNames("flex-wrap w-full items-center");

        creaGrigliaIscrizioni();
        creaGrigliaRiepiloghi();
        creaGrigliaAttestazioni();
        creaGrigliaLettere();
        nuovaLettera.addClickListener(e -> LetteraIncaricoDialog.apri(new LetteraIncarico(), username, this::aggiorna));
        filtri.add(nuovaLettera);
        add(tipo, filtri, gridIscrizioni, gridRiepiloghi, gridAttestazioni, gridLettere);
        gridLettere.addClassNames("grow");
        gridIscrizioni.addClassNames("grow");
        gridRiepiloghi.addClassNames("grow");
        gridAttestazioni.addClassNames("grow");
        aggiorna();
    }

    private void creaGrigliaIscrizioni() {
        gridIscrizioni.addColumn(d -> d.iscrizione().getAllievo().getNomeCompleto()).setHeader("Allievo")
            .setComparator(d -> d.iscrizione().getAllievo().getNomeCompleto());
        gridIscrizioni.addColumn(d -> d.iscrizione().getCorso().getNome()).setHeader("Corso")
            .setComparator(d -> d.iscrizione().getCorso().getNome());
        gridIscrizioni.addColumn(d -> d.iscrizione().getCorso().getAnnoScolastico()).setHeader("Anno")
            .setAutoWidth(true).setFlexGrow(0);
        gridIscrizioni.addColumn(d -> destinatario(d.destinatario())).setHeader("Destinatario");
        gridIscrizioni.addColumn(d -> ultimoInvio(d.ultimoInvio())).setHeader("Ultimo invio").setAutoWidth(true);
        gridIscrizioni.addComponentColumn(d -> datiMancanti(d.datiMancanti())).setHeader("Stato").setAutoWidth(true).setFlexGrow(0);
        ViewSupport.configureActionsColumn(gridIscrizioni.addComponentColumn(this::azioniIscrizione));
        gridIscrizioni.setEmptyStateText("Nessun documento");
        CardMobile.per(gridIscrizioni)
            .titolo(d -> d.iscrizione().getAllievo().getNomeCompleto())
            .sottotitolo(d -> d.iscrizione().getCorso().getNome() + anno(d.iscrizione().getCorso().getAnnoScolastico()))
            .stato(d -> datiMancanti(d.datiMancanti()))
            .campo("Destinatario", d -> destinatario(d.destinatario()))
            .campo("Ultimo invio", d -> ultimoInvio(d.ultimoInvio()))
            .azioni(this::azioniIscrizione)
            .applica();
    }

    private void creaGrigliaRiepiloghi() {
        gridRiepiloghi.addColumn(DocumentiView::intestatario).setHeader("Intestatario")
            .setComparator(DocumentoRiepilogo::intestatario);
        gridRiepiloghi.addColumn(r -> String.join(", ", r.allievi())).setHeader("Allievi");
        gridRiepiloghi.addColumn(r -> Objects.requireNonNullElse(r.anno(), "Senza anno")).setHeader("Anno")
            .setAutoWidth(true).setFlexGrow(0);
        gridRiepiloghi.addColumn(DocumentoRiepilogo::corsi).setHeader("Corsi").setAutoWidth(true).setFlexGrow(0);
        gridRiepiloghi.addColumn(r -> destinatario(r.destinatario())).setHeader("Destinatario");
        gridRiepiloghi.addColumn(r -> ultimoInvio(r.ultimoInvio())).setHeader("Ultimo invio").setAutoWidth(true);
        ViewSupport.configureActionsColumn(gridRiepiloghi.addComponentColumn(this::azioniRiepilogo));
        gridRiepiloghi.setEmptyStateText("Nessun riepilogo");
        CardMobile.per(gridRiepiloghi)
            .titolo(DocumentiView::intestatario)
            .sottotitolo(r -> Objects.requireNonNullElse(r.anno(), "Senza anno scolastico"))
            .campo("Allievi", r -> String.join(", ", r.allievi()))
            .campo("Corsi", DocumentoRiepilogo::corsi)
            .campo("Destinatario", r -> destinatario(r.destinatario()))
            .campo("Ultimo invio", r -> ultimoInvio(r.ultimoInvio()))
            .azioni(this::azioniRiepilogo)
            .applica();
    }

    private void creaGrigliaAttestazioni() {
        gridAttestazioni.addColumn(a -> a.allievo().getNomeCompleto()).setHeader("Allievo")
            .setComparator(a -> a.allievo().getNomeCompleto());
        gridAttestazioni.addColumn(DocumentiView::chiPaga).setHeader("Spese di");
        gridAttestazioni.addColumn(DocumentoAttestazione::anno).setHeader("Anno").setAutoWidth(true).setFlexGrow(0);
        gridAttestazioni.addColumn(a -> Formati.euro(a.totale())).setHeader("Pagato nell'anno")
            .setComparator(DocumentoAttestazione::totale).setAutoWidth(true).setFlexGrow(0);
        gridAttestazioni.addColumn(a -> destinatario(a.destinatario())).setHeader("Destinatario");
        gridAttestazioni.addColumn(a -> ultimoInvio(a.ultimoInvio())).setHeader("Ultimo invio").setAutoWidth(true);
        ViewSupport.configureActionsColumn(gridAttestazioni.addComponentColumn(this::azioniAttestazione));
        gridAttestazioni.setEmptyStateText("Nessuna attestazione: compaiono con i pagamenti");
        CardMobile.per(gridAttestazioni)
            .titolo(a -> a.allievo().getNomeCompleto())
            .sottotitolo(a -> "Anno " + a.anno())
            .campo("Spese di", DocumentiView::chiPaga)
            .campo("Pagato nell'anno", a -> Formati.euro(a.totale()))
            .campo("Destinatario", a -> destinatario(a.destinatario()))
            .campo("Ultimo invio", a -> ultimoInvio(a.ultimoInvio()))
            .azioni(this::azioniAttestazione)
            .applica();
    }

    private void creaGrigliaLettere() {
        gridLettere.addColumn(l -> l.getInsegnante().getNomeCompleto()).setHeader("Insegnante")
            .setComparator(l -> l.getInsegnante().getNomeCompleto());
        gridLettere.addComponentColumn(l -> TipoCorsoUI.nome(l.getCorso())).setHeader("Corso")
            .setComparator(l -> l.getCorso().getNome());
        gridLettere.addColumn(LetteraIncarico::getAnnoScolastico).setHeader("Anno").setAutoWidth(true).setFlexGrow(0);
        gridLettere.addColumn(LetteraIncaricoDialog::compenso).setHeader("Compenso");
        gridLettere.addColumn(l -> Formati.data(l.getDataEmissione())).setHeader("Emessa il")
            .setComparator(LetteraIncarico::getDataEmissione).setAutoWidth(true).setFlexGrow(0);
        ViewSupport.configureActionsColumn(gridLettere.addComponentColumn(this::azioniLettera));
        gridLettere.setEmptyStateText("Nessuna lettera di incarico");
        CardMobile.per(gridLettere)
            .titolo(l -> l.getInsegnante().getNomeCompleto())
            .sottotitolo(l -> l.getCorso().getNome() + anno(l.getAnnoScolastico()))
            .campo("Compenso", LetteraIncaricoDialog::compenso)
            .campo("Emessa il", l -> Formati.data(l.getDataEmissione()))
            .azioni(this::azioniLettera)
            .applica();
    }

    private HorizontalLayout azioniLettera(LetteraIncarico lettera) {
        Long id = lettera.getId();
        Button modifica = ViewSupport.iconButton(VaadinIcon.PENCIL, "Modifica",
            e -> LetteraIncaricoDialog.apri(lettera, username, this::aggiorna));
        Button invia = ViewSupport.iconButton(VaadinIcon.ENVELOPE, "Invia per email",
            e -> InvioEmailDialog.apriLettera(lettera, this::aggiorna));
        HorizontalLayout azioni = ViewSupport.actions(modifica, invia);
        azioni.addComponentAsFirst(pulsantePdf(VaadinIcon.FILE_TEXT_O, TipoDocumento.LETTERA_INCARICO.getTitolo(), null,
            () -> documentiService.letteraPer(id, username)));
        if (admin) {
            Button elimina = ViewSupport.iconButton(VaadinIcon.TRASH, "Elimina", e -> {
                letteraIncaricoService.elimina(id, username);
                Notification.show("Lettera di incarico eliminata");
                aggiorna();
            });
            elimina.addThemeVariants(ButtonVariant.ERROR);
            azioni.add(elimina);
        }
        return azioni;
    }

    private HorizontalLayout pdfLettera(LetteraIncarico lettera) {
        Long id = lettera.getId();
        HorizontalLayout azioni = ViewSupport.actions();
        azioni.add(pulsantePdf(VaadinIcon.FILE_TEXT_O, TipoDocumento.LETTERA_INCARICO.getTitolo(), null,
            () -> documentiService.letteraPer(id, username)));
        return azioni;
    }

    /** Lettere di incarico dell'insegnante, anche degli anni passati: solo PDF. */
    private void creaElencoInsegnante() {
        Grid<LetteraIncarico> grid = new Grid<>();
        grid.addComponentColumn(l -> TipoCorsoUI.nome(l.getCorso())).setHeader("Corso");
        grid.addColumn(LetteraIncarico::getAnnoScolastico).setHeader("Anno").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(LetteraIncaricoDialog::compenso).setHeader("Compenso");
        grid.addColumn(l -> Formati.data(l.getDataEmissione())).setHeader("Emessa il").setAutoWidth(true).setFlexGrow(0);
        ViewSupport.configureActionsColumn(grid.addComponentColumn(this::pdfLettera));
        grid.setEmptyStateText("Nessuna lettera di incarico");
        grid.setAllRowsVisible(true);
        CardMobile.per(grid)
            .titolo(l -> l.getCorso().getNome())
            .sottotitolo(l -> Objects.requireNonNullElse(l.getAnnoScolastico(), ""))
            .campo("Compenso", LetteraIncaricoDialog::compenso)
            .campo("Emessa il", l -> Formati.data(l.getDataEmissione()))
            .azioni(this::pdfLettera)
            .applica();
        grid.setItems(letteraIncaricoService.dellInsegnante(username));
        add(ViewSupport.sezione("Lettere di incarico"), grid);
    }

    /** Chi sostiene le spese: il genitore, o l'allievo adulto. */
    private static String chiPaga(DocumentoAttestazione attestazione) {
        return attestazione.allievo().getGenitore() != null ? attestazione.allievo().getGenitore().getNomeCompleto()
            : "Allievo adulto";
    }

    private HorizontalLayout azioniAttestazione(DocumentoAttestazione attestazione) {
        Long allievoId = attestazione.allievo().getId();
        int anno = attestazione.anno();
        Button invia = ViewSupport.iconButton(VaadinIcon.ENVELOPE, "Invia per email",
            e -> InvioEmailDialog.apriAttestazione(attestazione.allievo(), anno, this::aggiorna));
        HorizontalLayout azioni = ViewSupport.actions(invia);
        azioni.addComponentAsFirst(pulsantePdf(VaadinIcon.FILE_TEXT_O, TipoDocumento.ATTESTAZIONE.getTitolo(), null,
            () -> documentiService.attestazionePer(allievoId, anno, username)));
        return azioni;
    }

    /**
     * Documenti del genitore (dei figli e della famiglia) o dell'allievo senza genitore: solo PDF, filtro per anno
     * se ce n'è più di uno.
     */
    private void creaElencoFamiglia() {
        List<DocumentoFamiglia> documenti = elencoDocumentiService.documentiDi(username);
        Grid<DocumentoFamiglia> grid = new Grid<>();
        grid.addColumn(d -> d.tipo().getTitolo()).setHeader("Documento").setAutoWidth(true);
        grid.addColumn(DocumentiView::perChi).setHeader("Allievo");
        grid.addColumn(DocumentiView::corso).setHeader("Corso");
        grid.addColumn(d -> Objects.requireNonNullElse(d.anno(), "Senza anno")).setHeader("Anno").setAutoWidth(true).setFlexGrow(0);
        grid.addComponentColumn(d -> datiMancanti(d.datiMancanti())).setHeader("Stato").setAutoWidth(true).setFlexGrow(0);
        ViewSupport.configureActionsColumn(grid.addComponentColumn(this::azioniFamiglia));
        grid.setEmptyStateText("Nessun documento: compaiono con l'iscrizione a un corso");
        CardMobile.per(grid)
            .titolo(d -> d.tipo().getTitolo())
            .sottotitolo(d -> Objects.requireNonNullElse(d.anno(), "Senza anno scolastico"))
            .stato(d -> datiMancanti(d.datiMancanti()))
            .campo("Allievo", DocumentiView::perChi)
            .campo("Corso", DocumentiView::corso)
            .azioni(this::azioniFamiglia)
            .applica();
        GridListDataView<DocumentoFamiglia> righe = grid.setItems(documenti);

        List<String> anni = documenti.stream().map(DocumentoFamiglia::anno).filter(Objects::nonNull).distinct()
            .sorted(Comparator.reverseOrder()).toList();
        filtroAnno.setItems(anni);
        filtroAnno.addValueChangeListener(e -> righe.setFilter(d -> e.getValue() == null || e.getValue().equals(d.anno())));
        filtroAnno.setVisible(anni.size() > 1);
        Span nota = new Span("Conferme di iscrizione, ricevute dei pagamenti, riepiloghi annuali e attestazioni delle "
            + "spese per anno solare, da vedere o scaricare in PDF.");
        nota.addClassNames(Stili.TESTO_SECONDARIO);
        add(nota, filtroAnno, grid);
        grid.addClassNames("grow");
    }

    private HorizontalLayout azioniFamiglia(DocumentoFamiglia documento) {
        SerializableSupplier<Pdf> pdf;
        if (documento.iscrizione() != null) {
            TipoDocumento tipoDocumento = documento.tipo();
            Long corsoId = documento.iscrizione().getCorso().getId();
            Long allievoId = documento.iscrizione().getAllievo().getId();
            pdf = () -> documentiService.documentoPer(tipoDocumento, corsoId, allievoId, username);
        } else if (documento.tipo() == TipoDocumento.ATTESTAZIONE) {
            Long allievoId = documento.allievo().getId();
            int anno = Integer.parseInt(documento.anno());
            pdf = () -> documentiService.attestazionePer(allievoId, anno, username);
        } else {
            String anno = documento.anno();
            Long genitoreId = documento.genitore() != null ? documento.genitore().getId() : null;
            Long allievoId = documento.allievo() != null ? documento.allievo().getId() : null;
            pdf = genitoreId != null ? () -> documentiService.riepilogoFamigliaPer(genitoreId, anno, username)
                : () -> documentiService.riepilogoAllievoPer(allievoId, anno, username);
        }
        VaadinIcon icona = documento.tipo() == TipoDocumento.RICEVUTA ? VaadinIcon.PRINT : VaadinIcon.FILE_TEXT_O;
        HorizontalLayout azioni = ViewSupport.actions();
        azioni.add(pulsantePdf(icona, documento.tipo().getTitolo(), documento.datiMancanti(), pdf));
        return azioni;
    }

    private static String perChi(DocumentoFamiglia documento) {
        return documento.iscrizione() != null ? documento.iscrizione().getAllievo().getNomeCompleto()
            : documento.genitore() != null ? "Tutti i figli" : documento.allievo().getNomeCompleto();
    }

    private static String corso(DocumentoFamiglia documento) {
        return documento.iscrizione() != null ? documento.iscrizione().getCorso().getNome()
            : documento.tipo() == TipoDocumento.ATTESTAZIONE ? "Pagamenti dell'anno solare" : "Tutti i corsi dell'anno";
    }

    /** PDF (anteprima o scarico) e invio; con i dati mancanti i pulsanti mostrano solo l'avviso. */
    private HorizontalLayout azioniIscrizione(DocumentoIscrizione documento) {
        TipoDocumento tipoDocumento = documento.tipo();
        Long corsoId = documento.iscrizione().getCorso().getId();
        Long allievoId = documento.iscrizione().getAllievo().getId();
        VaadinIcon icona = tipoDocumento == TipoDocumento.RICEVUTA ? VaadinIcon.PRINT : VaadinIcon.FILE_TEXT_O;
        Component pdf = pulsantePdf(icona, tipoDocumento.getTitolo(), documento.datiMancanti(),
            () -> documentiService.documentoPer(tipoDocumento, corsoId, allievoId, username));
        Button invia = ViewSupport.iconButton(VaadinIcon.ENVELOPE, "Invia per email", e -> InvioEmailDialog.apri(tipoDocumento,
            documento.iscrizione().getCorso(), documento.iscrizione().getAllievo(), this::aggiorna));
        HorizontalLayout azioni = ViewSupport.actions(invia);
        azioni.addComponentAsFirst(pdf);
        return azioni;
    }

    private HorizontalLayout azioniRiepilogo(DocumentoRiepilogo riepilogo) {
        String anno = riepilogo.anno();
        SerializableSupplier<Pdf> pdf = riepilogo.genitore() != null
            ? () -> documentiService.riepilogoFamigliaPer(riepilogo.genitore().getId(), anno, username)
            : () -> documentiService.riepilogoAllievoPer(riepilogo.allievo().getId(), anno, username);
        Button invia = ViewSupport.iconButton(VaadinIcon.ENVELOPE, "Invia per email",
            e -> InvioEmailDialog.apriRiepilogo(riepilogo.genitore(), riepilogo.allievo(), anno, this::aggiorna));
        HorizontalLayout azioni = ViewSupport.actions(invia);
        azioni.addComponentAsFirst(pulsantePdf(VaadinIcon.FILE_TEXT_O, TipoDocumento.RIEPILOGO.getTitolo(), null, pdf));
        return azioni;
    }

    /**
     * Menu del PDF ({@link MenuPdf}); con {@code datiMancanti} un pulsante che mostra solo l'avviso. Condiviso con
     * l'elenco della famiglia.
     */
    static Component pulsantePdf(VaadinIcon icona, String etichetta, String datiMancanti, SerializableSupplier<Pdf> pdf) {
        if (datiMancanti != null) {
            return ViewSupport.iconButton(icona, etichetta, e -> Notification.show(datiMancanti));
        }
        return MenuPdf.soloIcona(icona, etichetta, pdf);
    }

    /** Badge "Dati mancanti" con l'avviso nel tooltip, {@code null} se i dati ci sono. */
    public static Badge datiMancanti(String avviso) {
        if (avviso == null) {
            return null;
        }
        Badge badge = new Badge("Dati mancanti", VaadinIcon.WARNING.create());
        badge.addThemeVariants(BadgeVariant.WARNING);
        Tooltip.forComponent(badge).setText(avviso);
        return badge;
    }

    private static String intestatario(DocumentoRiepilogo riepilogo) {
        return riepilogo.genitore() != null ? riepilogo.intestatario() : riepilogo.intestatario() + " (allievo)";
    }

    private static String destinatario(Optional<Destinatario> destinatario) {
        return destinatario.map(Destinatario::email).orElse("Nessuna email");
    }

    private static String ultimoInvio(InvioEmail invio) {
        if (invio == null) {
            return "";
        }
        return Formati.dataOra(invio.getDataOra()) + (invio.getEsito() == InvioEmail.Esito.SIMULATA ? " (simulato)" : "");
    }

    private static String anno(String anno) {
        return anno != null ? " " + anno : "";
    }

    /** Ricarica l'elenco del tipo scelto mantenendo filtri e ricerca. */
    private void aggiorna() {
        boolean perRiepilogo = tipo.getValue() == TipoDocumento.RIEPILOGO;
        boolean perAttestazione = tipo.getValue() == TipoDocumento.ATTESTAZIONE;
        boolean perLettera = tipo.getValue() == TipoDocumento.LETTERA_INCARICO;
        gridIscrizioni.setVisible(!perRiepilogo && !perAttestazione && !perLettera);
        gridRiepiloghi.setVisible(perRiepilogo);
        gridAttestazioni.setVisible(perAttestazione);
        gridLettere.setVisible(perLettera);
        nuovaLettera.setVisible(perLettera);
        filtroCorso.setVisible(!perRiepilogo && !perAttestazione && !perLettera);
        Stream<String> anni;
        if (perLettera) {
            lettere = letteraIncaricoService.tutte(username);
            righeLettere = gridLettere.setItems(lettere);
            anni = lettere.stream().map(LetteraIncarico::getAnnoScolastico);
        } else if (perAttestazione) {
            attestazioni = elencoDocumentiService.attestazioni(username);
            righeAttestazioni = gridAttestazioni.setItems(attestazioni);
            anni = attestazioni.stream().map(a -> String.valueOf(a.anno()));
        } else if (perRiepilogo) {
            riepiloghi = elencoDocumentiService.riepiloghi(username);
            righeRiepiloghi = gridRiepiloghi.setItems(riepiloghi);
            anni = riepiloghi.stream().map(DocumentoRiepilogo::anno);
        } else {
            iscrizioni = elencoDocumentiService.documentiIscrizioni(tipo.getValue(), username);
            righeIscrizioni = gridIscrizioni.setItems(iscrizioni);
            anni = iscrizioni.stream().map(d -> d.iscrizione().getCorso().getAnnoScolastico());
        }
        String annoScelto = filtroAnno.getValue();
        List<String> elencoAnni = anni.filter(Objects::nonNull).distinct().sorted(Comparator.reverseOrder()).toList();
        filtroAnno.setItems(elencoAnni);
        // Alla prima apertura l'anno corrente (o il più recente), poi quello scelto se c'è ancora
        if (primaApertura) {
            primaApertura = false;
            String corrente = AnnoScolastico.di(LocalDate.now());
            filtroAnno.setValue(elencoAnni.contains(corrente) ? corrente : elencoAnni.isEmpty() ? null : elencoAnni.getFirst());
        } else {
            filtroAnno.setValue(annoScelto != null && elencoAnni.contains(annoScelto) ? annoScelto : null);
        }
        aggiornaCorsi();
        applicaFiltri();
    }

    /** Corsi dell'anno scelto (tutti senza anno scelto), per nome. */
    private void aggiornaCorsi() {
        Corso scelto = filtroCorso.getValue();
        String anno = filtroAnno.getValue();
        Map<Long, Corso> corsi = iscrizioni.stream().map(d -> d.iscrizione().getCorso())
            .filter(c -> anno == null || anno.equals(c.getAnnoScolastico()))
            .collect(Collectors.toMap(Corso::getId, Function.identity(), (a, b) -> a));
        filtroCorso.setItems(corsi.values().stream().sorted(Comparator.comparing(Corso::getNome, String.CASE_INSENSITIVE_ORDER)).toList());
        if (scelto != null && corsi.containsKey(scelto.getId())) {
            filtroCorso.setValue(corsi.get(scelto.getId()));
        }
    }

    /** Anno, corso e ricerca; "N di M" sulle righe dell'anno e del corso scelti. */
    private void applicaFiltri() {
        String anno = filtroAnno.getValue();
        boolean perRiepilogo = tipo.getValue() == TipoDocumento.RIEPILOGO;
        boolean perAttestazione = tipo.getValue() == TipoDocumento.ATTESTAZIONE;
        boolean perLettera = tipo.getValue() == TipoDocumento.LETTERA_INCARICO;
        Corso corso = filtroCorso.getValue();
        confermeATutti.setVisible(tipo.getValue() == TipoDocumento.CONFERMA_ISCRIZIONE && corso != null);
        if (perLettera) {
            if (righeLettere != null) {
                SerializablePredicate<LetteraIncarico> dellAnno = l -> anno == null || anno.equals(l.getAnnoScolastico());
                righeLettere.setFilter(l -> dellAnno.test(l) && ricerca.corrisponde(RicercaRapida.testo(
                    l.getInsegnante().getNomeCompleto(), l.getCorso().getNome())));
                ricerca.mostraConteggio(righeLettere.getItemCount(), (int) lettere.stream().filter(dellAnno).count());
            }
        } else if (perAttestazione) {
            if (righeAttestazioni != null) {
                SerializablePredicate<DocumentoAttestazione> dellAnno = a -> anno == null || anno.equals(String.valueOf(a.anno()));
                righeAttestazioni.setFilter(a -> dellAnno.test(a) && ricerca.corrisponde(RicercaRapida.testo(
                    a.allievo().getNomeCompleto(), chiPaga(a), destinatario(a.destinatario()))));
                ricerca.mostraConteggio(righeAttestazioni.getItemCount(), (int) attestazioni.stream().filter(dellAnno).count());
            }
        } else if (perRiepilogo && righeRiepiloghi != null) {
            SerializablePredicate<DocumentoRiepilogo> dellAnno = r -> anno == null || anno.equals(r.anno());
            righeRiepiloghi.setFilter(r -> dellAnno.test(r) && ricerca.corrisponde(RicercaRapida.testo(
                r.intestatario(), String.join(" ", r.allievi()), destinatario(r.destinatario()))));
            ricerca.mostraConteggio(righeRiepiloghi.getItemCount(), (int) riepiloghi.stream().filter(dellAnno).count());
        } else if (!perRiepilogo && righeIscrizioni != null) {
            SerializablePredicate<DocumentoIscrizione> scelti = d -> (anno == null || anno.equals(d.iscrizione().getCorso().getAnnoScolastico()))
                && (corso == null || corso.getId().equals(d.iscrizione().getCorso().getId()));
            righeIscrizioni.setFilter(d -> scelti.test(d) && ricerca.corrisponde(RicercaRapida.testo(
                d.iscrizione().getAllievo().getNomeCompleto(), d.iscrizione().getCorso().getNome(), destinatario(d.destinatario()))));
            ricerca.mostraConteggio(righeIscrizioni.getItemCount(), (int) iscrizioni.stream().filter(scelti).count());
        }
    }
}
