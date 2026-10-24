package it.attocchi.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
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
import it.attocchi.entity.TipoDocumento;
import it.attocchi.security.SecurityService;
import it.attocchi.service.AllievoService;
import it.attocchi.service.DocumentiService;
import it.attocchi.service.DocumentiService.Pdf;
import it.attocchi.service.ElencoDocumentiService;
import it.attocchi.service.ElencoDocumentiService.DocumentoFamiglia;
import it.attocchi.service.ElencoDocumentiService.DocumentoIscrizione;
import it.attocchi.service.ElencoDocumentiService.DocumentoRiepilogo;
import it.attocchi.service.EmailService.Destinatario;
import it.attocchi.views.admin.InvioEmailDialog;
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
@RolesAllowed({"ADMIN", "SEGRETERIA", "GENITORE", "ALLIEVO"})
public class DocumentiView extends VerticalLayout {

    /** Percorso della vista, usato da {@code MainLayout} per nasconderla all'allievo con genitore. */
    public static final String PERCORSO = "documenti";

    private final ElencoDocumentiService elencoDocumentiService;
    private final DocumentiService documentiService;
    private final String username;

    private final SegmentedControl<TipoDocumento> tipo;
    private final ComboBox<String> filtroAnno = new ComboBox<>();
    private final ComboBox<Corso> filtroCorso = new ComboBox<>();
    private final RicercaRapida ricerca = new RicercaRapida();
    private final Button confermeATutti = new Button("Invia conferme di iscrizione a tutti", VaadinIcon.ENVELOPES.create());
    private final Grid<DocumentoIscrizione> gridIscrizioni = new Grid<>();
    private final Grid<DocumentoRiepilogo> gridRiepiloghi = new Grid<>();
    private List<DocumentoIscrizione> iscrizioni = List.of();
    private List<DocumentoRiepilogo> riepiloghi = List.of();
    private GridListDataView<DocumentoIscrizione> righeIscrizioni;
    private GridListDataView<DocumentoRiepilogo> righeRiepiloghi;
    private boolean primaApertura = true;

    public DocumentiView(ElencoDocumentiService elencoDocumentiService, DocumentiService documentiService,
                         AllievoService allievoService, SecurityService securityService) {
        this.elencoDocumentiService = elencoDocumentiService;
        this.documentiService = documentiService;
        this.username = securityService.getUsername().orElse("");
        addClassNames("size-full");
        tipo = new SegmentedControl<>("Tipo di documento", List.of(TipoDocumento.values()), TipoDocumento::getPlurale,
            t -> t == TipoDocumento.RIEPILOGO ? "Riepiloghi annuali dei pagamenti" : t.getTitolo(), null);
        if (!securityService.hasRole("ADMIN") && !securityService.hasRole("SEGRETERIA")) {
            if (!securityService.hasRole("GENITORE") && !allievoService.gestisceDaSe(username)) {
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
        add(tipo, filtri, gridIscrizioni, gridRiepiloghi);
        gridIscrizioni.addClassNames("grow");
        gridRiepiloghi.addClassNames("grow");
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
        Span nota = new Span("Conferme di iscrizione, ricevute dei pagamenti e riepiloghi annuali, da vedere o scaricare in PDF.");
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
        return documento.iscrizione() != null ? documento.iscrizione().getCorso().getNome() : "Tutti i corsi dell'anno";
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
        gridIscrizioni.setVisible(!perRiepilogo);
        gridRiepiloghi.setVisible(perRiepilogo);
        filtroCorso.setVisible(!perRiepilogo);
        Stream<String> anni;
        if (perRiepilogo) {
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
        Corso corso = filtroCorso.getValue();
        confermeATutti.setVisible(tipo.getValue() == TipoDocumento.CONFERMA_ISCRIZIONE && corso != null);
        if (perRiepilogo && righeRiepiloghi != null) {
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
