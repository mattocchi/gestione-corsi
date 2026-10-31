package it.attocchi.views;

import it.attocchi.service.AnnoScolasticoService;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.function.SerializableSupplier;
import it.attocchi.entity.Allievo;
import it.attocchi.entity.Corso;
import it.attocchi.entity.Genitore;
import it.attocchi.entity.GruppoCorso;
import it.attocchi.entity.Pagamento;
import it.attocchi.service.PagamentoService;
import it.attocchi.service.PagamentoService.RigaPagamento;
import it.attocchi.service.PagamentoService.Situazione;
import it.attocchi.service.PagamentoService.StatoPagamento;
import it.attocchi.views.admin.CorsiView.FiltroStato;
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
 * sulla riga "Aggiungi pagamento" e "Quota e pagamenti". In consultazione (genitore, allievo adulto): filtro per
 * figlio se ne ha più di uno e pagamenti in sola lettura. Ricevute e loro invio sono in Documenti.
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

    private final Grid<RigaPagamento> grid = new Grid<>(RigaPagamento.class, false);
    private final Div totali = new Div();
    private final Select<FiltroStato> filtroCorsi = new Select<>();
    private final Schede<FiltroPagamento> filtroPagamento = new Schede<>("Filtra per stato del pagamento",
        List.of(FiltroPagamento.values()), FiltroPagamento::getEtichetta, f -> "Pagamento: " + f.getEtichetta());
    private final ComboBox<Corso> filtroCorso = filtro("Tutti i corsi", "Filtra per corso", "w-56");
    private final ComboBox<Allievo> filtroAllievo = filtro("Tutti gli allievi", "Filtra per allievo", "w-52");
    private final ComboBox<Genitore> filtroGenitore = filtro("Tutti i genitori", "Filtra per genitore", "w-52");
    private final ComboBox<String> filtroAnno = filtro("Tutti gli anni", "Filtra per anno scolastico", "w-40");
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

        addClassNames("size-full p-0");
        configureGrid();

        // Il genitore vede anche i corsi non attivi e non ha il filtro per stato del corso
        filtroCorsi.setItems(FiltroStato.values());
        filtroCorsi.setItemLabelGenerator(f -> switch (f) {
            case ATTIVI -> "Corsi attivi";
            case NON_ATTIVI -> "Corsi non attivi";
            case TUTTI -> "Tutti i corsi";
        });
        filtroCorsi.setAriaLabel("Filtra per stato del corso");
        filtroCorsi.addClassNames("w-44");
        filtroCorsi.setValue(gestione ? FiltroStato.ATTIVI : FiltroStato.TUTTI);
        filtroPagamento.setValue(FiltroPagamento.TUTTI);
        filtroCorsi.addValueChangeListener(e -> applicaFiltri());
        filtroPagamento.addValueChangeListener(e -> applicaFiltri());
        filtroCorso.setItemLabelGenerator(c -> c.getNome() + " (" + c.getAnnoScolastico() + ")");
        filtroAllievo.setItemLabelGenerator(Allievo::getNomeCompleto);
        filtroGenitore.setItemLabelGenerator(Genitore::getNomeCompleto);

        HorizontalLayout filtri = new HorizontalLayout();
        filtri.addClassNames("flex-wrap w-full items-center");
        if (gestione) {
            filtri.add(filtroCorsi, filtroCorso, filtroAllievo, filtroGenitore, filtroAnno);
        } else {
            filtroAllievo.setPlaceholder("Tutti i figli");
            filtroAllievo.setAriaLabel("Filtra per figlio");
            filtri.add(filtroAllievo);
        }
        totali.addClassNames(Stili.RIEPILOGO_PAGAMENTI);

        add(filtroPagamento, filtri, totali, grid);
        grid.addClassNames("grow");
        aggiorna();
    }

    /** Con {@code ?stato=…} ({@link ViewSupport#stato(Enum)}) mostra la scheda di quello stato del pagamento. */
    public void mostraStato(QueryParameters parametri) {
        ViewSupport.stato(FiltroPagamento.class, parametri).ifPresent(filtroPagamento::setValue);
    }

    /** {@code larghezza}: classe Tailwind di larghezza, scritta per intero dal chiamante (es. {@code "w-56"}). */
    private <T> ComboBox<T> filtro(String placeholder, String ariaLabel, String larghezza) {
        ComboBox<T> comboBox = new ComboBox<>();
        comboBox.setPlaceholder(placeholder);
        comboBox.setAriaLabel(ariaLabel);
        comboBox.setClearButtonVisible(true);
        comboBox.addClassNames(larghezza, "max-w-full");
        comboBox.addValueChangeListener(e -> applicaFiltri());
        return comboBox;
    }

    private void configureGrid() {
        grid.addColumn(r -> r.corso().getNome()).setHeader("Corso").setSortable(true);
        grid.addColumn(r -> r.corso().getAnnoScolastico()).setHeader("Anno").setSortable(true)
            .setWidth("7rem").setFlexGrow(0);
        grid.addColumn(r -> r.allievo().getNomeCompleto()).setHeader("Allievo").setSortable(true);
        // Il gruppo del partecipante, solo in gestione
        grid.addColumn(r -> Formati.gruppo(r.iscrizione().getGruppo())).setHeader("Gruppo")
            .setComparator(Comparator.comparing((RigaPagamento r) -> r.iscrizione().getGruppo(),
                Comparator.nullsLast(GruppoCorso.PER_GIORNO)))
            .setAutoWidth(true).setFlexGrow(0).setVisible(gestione);
        var genitore = grid.addColumn(r -> r.genitore() != null ? r.genitore().getNomeCompleto() : "").setHeader("Genitore")
            .setSortable(true);
        genitore.setVisible(gestione);
        euro("Quota", Situazione::quota);
        euro("Pagato", Situazione::pagato);
        euro("Residuo", Situazione::residuo);
        grid.addComponentColumn(r -> StatoPagamentoUI.badge(r.situazione())).setHeader("Stato").setWidth("9rem").setFlexGrow(0)
            .setComparator(r -> r.situazione().stato()).setKey("stato");
        ViewSupport.configureActionsColumn(grid.addComponentColumn(this::createAzioni).setKey("azioni"));

        CardMobile.per(grid)
            .titolo(r -> r.allievo().getNomeCompleto())
            .sottotitolo(r -> r.corso().getNome() + " · " + r.corso().getAnnoScolastico())
            .stato(r -> StatoPagamentoUI.badge(r.situazione()))
            .campo("Gruppo", r -> gestione ? Formati.gruppo(r.iscrizione().getGruppo()) : null)
            .campo("Genitore", r -> gestione && r.genitore() != null ? r.genitore().getNomeCompleto() : null)
            .campo("Quota", r -> Formati.euro(r.situazione().quota()))
            .campo("Pagato", r -> Formati.euro(r.situazione().pagato()))
            .campo("Residuo", r -> Formati.euro(r.situazione().residuo()))
            .azioni(this::createAzioni)
            .applica();
        grid.setEmptyStateText("Nessun partecipante");
    }

    private Grid.Column<RigaPagamento> euro(String header, Function<Situazione, BigDecimal> valore) {
        return grid.addColumn(r -> Formati.euro(valore.apply(r.situazione()))).setHeader(header)
            .setComparator(r -> valore.apply(r.situazione())).setWidth("7.5rem").setFlexGrow(0);
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
            azioni.add(quota);
        } else {
            Button pagamenti = ViewSupport.iconButton(VaadinIcon.EURO, "Pagamenti", e -> new PagamentiAllievoDialog(
                "Pagamenti " + riga.corso().getNome() + " – " + riga.allievo().getNomeCompleto(), riga.iscrizione(),
                riga.situazione(), pagamentoService.findPagamenti(corsoId, allievoId)).open());
            StatoPagamentoUI.coloraPulsante(pagamenti, "Pagamenti", riga.situazione());
            azioni.add(pagamenti);
        }
        return azioni;
    }

    /** Ricarica le righe mantenendo i filtri scelti. */
    public void aggiorna() {
        List<RigaPagamento> elenco = caricaRighe.get();
        aggiornaVoci(filtroCorso, elenco, RigaPagamento::corso, Comparator.comparing(Corso::getNome).thenComparing(Corso::getAnnoScolastico, Comparator.nullsLast(Comparator.naturalOrder())));
        aggiornaVoci(filtroAllievo, elenco, RigaPagamento::allievo, Comparator.comparing(Allievo::getNomeCompleto));
        aggiornaVoci(filtroGenitore, elenco, RigaPagamento::genitore, Comparator.comparing(Genitore::getNomeCompleto));
        aggiornaVoci(filtroAnno, elenco, r -> r.corso().getAnnoScolastico(), Comparator.reverseOrder());
        if (gestione) {
            // Lo staff filtra anche per gli anni non attivi dell'elenco (archivio)
            AnnoScolasticoService anni = VaadinService.getCurrent().getInstantiator().getOrCreate(AnnoScolasticoService.class);
            String scelto = filtroAnno.getValue();
            filtroAnno.setItems(anni.perFiltro(elenco.stream().map(r -> r.corso().getAnnoScolastico()).toList()));
            filtroAnno.setItemLabelGenerator(ViewSupport.etichettaAnno(anni.nonAttivi()));
            filtroAnno.setValue(scelto);
        }
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
