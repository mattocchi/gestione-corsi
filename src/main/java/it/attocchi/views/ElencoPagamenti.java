package it.attocchi.views;

import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.signals.Signal;
import it.attocchi.entity.Allievo;
import it.attocchi.entity.Corso;
import it.attocchi.entity.Genitore;
import it.attocchi.entity.Pagamento;
import it.attocchi.service.PagamentoService;
import it.attocchi.service.PagamentoService.RigaPagamento;
import it.attocchi.service.PagamentoService.Situazione;
import it.attocchi.service.PagamentoService.StatoPagamento;
import it.attocchi.views.admin.CorsiView.FiltroStato;
import it.attocchi.views.admin.InvioEmailDialog;
import it.attocchi.views.admin.QuotaPagamentiDialog;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Elenco dei partecipanti con quota, pagato, residuo e stato del pagamento, filtri e totali dei partecipanti filtrati.
 * <p>
 * In gestione (admin e segreteria): filtri per stato del corso, stato del pagamento, corso, allievo, genitore e anno;
 * sulla riga "Aggiungi pagamento", "Quota e pagamenti" e "Ricevuta". In consultazione (genitore): filtro per figlio
 * se ne ha più di uno, pagamenti in sola lettura e ricevuta.
 */
public class ElencoPagamenti extends VerticalLayout {

    /** Filtro per stato del pagamento; {@code TUTTI} non filtra. */
    public enum FiltroPagamento {
        TUTTI("Tutti", null),
        DA_PAGARE("Da pagare", StatoPagamento.NESSUN_PAGAMENTO),
        PARZIALE("Parziale", StatoPagamento.PARZIALE),
        SALDATO("Saldato", StatoPagamento.SALDATO);

        private final String etichetta;
        private final StatoPagamento stato;

        FiltroPagamento(String etichetta, StatoPagamento stato) {
            this.etichetta = etichetta;
            this.stato = stato;
        }

        public String getEtichetta() {
            return etichetta;
        }

        boolean ammette(Situazione situazione) {
            return stato == null || situazione.stato() == stato;
        }
    }

    private final PagamentoService pagamentoService;
    private final SerializableSupplier<List<RigaPagamento>> caricaRighe;
    private final boolean gestione;
    private final Signal<Boolean> mobile = ViewSupport.isMobile();

    private final Grid<RigaPagamento> grid = new Grid<>(RigaPagamento.class, false);
    private final Div totali = new Div();
    private final SegmentedControl<FiltroStato> filtroCorsi = new SegmentedControl<>("Filtra per stato del corso",
        List.of(FiltroStato.values()), FiltroStato::getEtichetta, f -> "Corsi: " + f.getEtichetta(), null);
    private final SegmentedControl<FiltroPagamento> filtroPagamento = new SegmentedControl<>("Filtra per stato del pagamento",
        List.of(FiltroPagamento.values()), FiltroPagamento::getEtichetta, f -> "Pagamento: " + f.getEtichetta(), null);
    private final ComboBox<Corso> filtroCorso = filtro("Tutti i corsi", "Filtra per corso", "14rem");
    private final ComboBox<Allievo> filtroAllievo = filtro("Tutti gli allievi", "Filtra per allievo", "13rem");
    private final ComboBox<Genitore> filtroGenitore = filtro("Tutti i genitori", "Filtra per genitore", "13rem");
    private final ComboBox<String> filtroAnno = filtro("Tutti gli anni", "Filtra per anno scolastico", "10rem");
    private GridListDataView<RigaPagamento> righe;

    /**
     * @param caricaRighe righe da mostrare, ricaricate dopo ogni modifica
     * @param gestione    {@code true} per admin e segreteria (filtri completi e modifica), {@code false} per il genitore
     */
    public ElencoPagamenti(PagamentoService pagamentoService, SerializableSupplier<List<RigaPagamento>> caricaRighe,
                           boolean gestione) {
        this.pagamentoService = pagamentoService;
        this.caricaRighe = caricaRighe;
        this.gestione = gestione;

        setSizeFull();
        setPadding(false);
        configureGrid();

        // Il genitore vede anche i corsi non attivi e non ha filtri per stato
        filtroCorsi.setValue(gestione ? FiltroStato.ATTIVI : FiltroStato.TUTTI);
        filtroPagamento.setValue(FiltroPagamento.TUTTI);
        filtroCorsi.addValueChangeListener(e -> applicaFiltri());
        filtroPagamento.addValueChangeListener(e -> applicaFiltri());
        filtroCorso.setItemLabelGenerator(c -> c.getNome() + " (" + c.getAnnoScolastico() + ")");
        filtroAllievo.setItemLabelGenerator(Allievo::getNomeCompleto);
        filtroGenitore.setItemLabelGenerator(Genitore::getNomeCompleto);

        HorizontalLayout filtri = new HorizontalLayout();
        filtri.setWrap(true);
        filtri.setWidthFull();
        filtri.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        if (gestione) {
            filtri.add(filtroCorsi, filtroPagamento, filtroCorso, filtroAllievo, filtroGenitore, filtroAnno);
        } else {
            filtroAllievo.setPlaceholder("Tutti i figli");
            filtroAllievo.setAriaLabel("Filtra per figlio");
            filtri.add(filtroAllievo);
        }
        totali.addClassName("pagamenti-riepilogo");

        add(filtri, totali, grid);
        expand(grid);
        aggiorna();
    }

    private <T> ComboBox<T> filtro(String placeholder, String ariaLabel, String width) {
        ComboBox<T> comboBox = new ComboBox<>();
        comboBox.setPlaceholder(placeholder);
        comboBox.setAriaLabel(ariaLabel);
        comboBox.setClearButtonVisible(true);
        comboBox.setWidth(width);
        comboBox.setMaxWidth("100%");
        comboBox.addValueChangeListener(e -> applicaFiltri());
        return comboBox;
    }

    private void configureGrid() {
        // Su mobile una sola colonna con allievo, corso e residuo al posto delle singole colonne
        var riepilogo = grid.addColumn(LitRenderer.<RigaPagamento>of(
                "<div class=\"riepilogo-righe\"><div class=\"riepilogo-righe-titolo\">${item.allievo}</div>"
                    + "<div class=\"riepilogo-righe-dettaglio\">${item.dettaglio}</div></div>")
                .withProperty("allievo", r -> r.allievo().getNomeCompleto())
                .withProperty("dettaglio", r -> r.corso().getNome() + " · residuo " + Formati.euro(r.situazione().residuo())))
            .setHeader("Partecipante");
        var corso = grid.addColumn(r -> r.corso().getNome()).setHeader("Corso").setSortable(true);
        var anno = grid.addColumn(r -> r.corso().getAnnoScolastico()).setHeader("Anno").setSortable(true)
            .setWidth("7rem").setFlexGrow(0);
        var allievo = grid.addColumn(r -> r.allievo().getNomeCompleto()).setHeader("Allievo").setSortable(true);
        var genitore = grid.addColumn(r -> r.genitore() != null ? r.genitore().getNomeCompleto() : "").setHeader("Genitore")
            .setSortable(true);
        genitore.setVisible(gestione);
        var quota = euro("Quota", Situazione::quota);
        var pagato = euro("Pagato", Situazione::pagato);
        var residuo = euro("Residuo", Situazione::residuo);
        var stato = grid.addComponentColumn(this::createStatoBadge).setHeader("Stato").setWidth("9rem").setFlexGrow(0)
            .setComparator(r -> r.situazione().stato()).setKey("stato");
        ViewSupport.configureActionsColumn(grid.addComponentColumn(this::createAzioni).setKey("azioni"));

        Signal.effect(grid, () -> {
            boolean m = mobile.get();
            riepilogo.setVisible(m);
            stato.setHeader(m ? "" : "Stato").setWidth(m ? "3rem" : "9rem");
        });
        if (gestione) {
            ViewSupport.hideOnMobile(grid, corso, anno, allievo, genitore, quota, pagato, residuo);
        } else {
            ViewSupport.hideOnMobile(grid, corso, anno, allievo, quota, pagato, residuo);
        }
        grid.setEmptyStateText("Nessun partecipante");
    }

    private Grid.Column<RigaPagamento> euro(String header, Function<Situazione, BigDecimal> valore) {
        return grid.addColumn(r -> Formati.euro(valore.apply(r.situazione()))).setHeader(header)
            .setComparator(r -> valore.apply(r.situazione())).setWidth("7.5rem").setFlexGrow(0);
    }

    private Badge createStatoBadge(RigaPagamento riga) {
        Badge badge = StatoPagamentoUI.badge(riga.situazione());
        badge.bindThemeVariant(BadgeVariant.ICON_ONLY, mobile);
        return badge;
    }

    private HorizontalLayout createAzioni(RigaPagamento riga) {
        Long corsoId = riga.corso().getId();
        Long allievoId = riga.allievo().getId();
        HorizontalLayout azioni = ViewSupport.actions();
        if (gestione) {
            azioni.add(ViewSupport.iconButton(VaadinIcon.PLUS, "Aggiungi pagamento",
                e -> QuotaPagamentiDialog.apriPagamento(pagamentoService, corsoId, allievoId, new Pagamento(), this::aggiorna)));
            Button quota = ViewSupport.iconButton(VaadinIcon.EURO, "Quota e pagamenti", e -> {
                QuotaPagamentiDialog dialog = new QuotaPagamentiDialog(pagamentoService, riga.corso(), riga.allievo());
                dialog.addOpenedChangeListener(o -> {
                    if (!o.isOpened()) {
                        aggiorna();
                    }
                });
                dialog.open();
            });
            StatoPagamentoUI.coloraPulsante(quota, "Quota e pagamenti", riga.situazione());
            azioni.add(quota, ViewSupport.iconButton(VaadinIcon.ENVELOPE, "Invia email",
                e -> InvioEmailDialog.apri(riga.corso(), riga.allievo(), () -> { })));
        } else {
            Button pagamenti = ViewSupport.iconButton(VaadinIcon.EURO, "Pagamenti", e -> new PagamentiAllievoDialog(
                "Pagamenti " + riga.corso().getNome() + " – " + riga.allievo().getNomeCompleto(), riga.iscrizione(),
                riga.situazione(), pagamentoService.findPagamenti(corsoId, allievoId)).open());
            StatoPagamentoUI.coloraPulsante(pagamenti, "Pagamenti", riga.situazione());
            azioni.add(pagamenti);
        }
        azioni.add(Ricevute.link(corsoId, riga.allievo(), ViewSupport.iconButton(VaadinIcon.PRINT, "Ricevuta")));
        return azioni;
    }

    /** Ricarica le righe mantenendo i filtri scelti. */
    public void aggiorna() {
        List<RigaPagamento> elenco = caricaRighe.get();
        aggiornaVoci(filtroCorso, elenco, RigaPagamento::corso, Comparator.comparing(Corso::getNome).thenComparing(Corso::getAnnoScolastico, Comparator.nullsLast(Comparator.naturalOrder())));
        aggiornaVoci(filtroAllievo, elenco, RigaPagamento::allievo, Comparator.comparing(Allievo::getNomeCompleto));
        aggiornaVoci(filtroGenitore, elenco, RigaPagamento::genitore, Comparator.comparing(Genitore::getNomeCompleto));
        aggiornaVoci(filtroAnno, elenco, r -> r.corso().getAnnoScolastico(), Comparator.reverseOrder());
        filtroAllievo.setVisible(gestione || filtroAllievo.getListDataView().getItemCount() > 1);
        righe = grid.setItems(elenco);
        applicaFiltri();
    }

    private <T> void aggiornaVoci(ComboBox<T> comboBox, List<RigaPagamento> elenco, Function<RigaPagamento, T> voce,
                                  Comparator<T> ordine) {
        T scelta = comboBox.getValue();
        List<T> voci = elenco.stream().map(voce).filter(Objects::nonNull).distinct().sorted(ordine).toList();
        comboBox.setItems(voci);
        comboBox.setValue(voci.contains(scelta) ? scelta : null);
    }

    private void applicaFiltri() {
        if (righe == null) {
            return;
        }
        righe.setFilter(r -> filtroCorsi.getValue().ammette(r.corso())
            && filtroPagamento.getValue().ammette(r.situazione())
            && (filtroCorso.getValue() == null || filtroCorso.getValue().equals(r.corso()))
            && (filtroAllievo.getValue() == null || filtroAllievo.getValue().equals(r.allievo()))
            && (filtroGenitore.getValue() == null || filtroGenitore.getValue().equals(r.genitore()))
            && (filtroAnno.getValue() == null || filtroAnno.getValue().equals(r.corso().getAnnoScolastico())));
        aggiornaTotali(righe.getItems().toList());
    }

    /** Numero di partecipanti e totali di quota, incassato e residuo delle righe filtrate. */
    private void aggiornaTotali(List<RigaPagamento> filtrate) {
        BigDecimal quota = BigDecimal.ZERO;
        BigDecimal pagato = BigDecimal.ZERO;
        BigDecimal residuo = BigDecimal.ZERO;
        for (RigaPagamento riga : filtrate) {
            quota = quota.add(riga.situazione().quota());
            pagato = pagato.add(riga.situazione().pagato());
            residuo = residuo.add(riga.situazione().residuo());
        }
        totali.removeAll();
        totali.add(new Span(filtrate.size() + (filtrate.size() == 1 ? " partecipante" : " partecipanti")),
            new Span("Da pagare " + Formati.euro(quota)),
            new Span("Incassato " + Formati.euro(pagato)),
            new Span("Residuo " + Formati.euro(residuo)));
    }
}
