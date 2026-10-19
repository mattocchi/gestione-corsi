package it.attocchi.views.admin;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.FooterRow;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import it.attocchi.entity.Corso;
import it.attocchi.service.PagamentoService;
import it.attocchi.service.PagamentoService.IscrittiCorso;
import it.attocchi.service.PagamentoService.RigaPagamento;
import it.attocchi.service.PagamentoService.Situazione;
import it.attocchi.views.CardMobile;
import it.attocchi.views.Formati;
import it.attocchi.views.MainLayout;
import it.attocchi.views.RicercaRapida;
import it.attocchi.views.SegmentedControl;
import it.attocchi.views.StatoPagamentoUI;
import it.attocchi.views.Stili;
import it.attocchi.views.ViewSupport;
import it.attocchi.views.admin.CorsiView.FiltroStato;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Iscritti corso per corso con quanto devono pagare e quanto hanno pagato finora, in sola lettura: una
 * {@link TreeGrid} con un livello di raggruppamento (riga del corso con i totali, sotto i suoi iscritti) e il totale
 * generale in fondo. I pagamenti si registrano in Gestione Pagamenti.
 */
@Route(value = "riepilogo-iscritti", layout = MainLayout.class)
@PageTitle("Riepilogo Iscritti")
@Menu(title = "Riepilogo Iscritti", order = 10.6, icon = "vaadin:table")
@RolesAllowed({"ADMIN", "SEGRETERIA"})
public class RiepilogoIscrittiView extends VerticalLayout {

    /** Riga della griglia: un corso (gruppo) o un suo iscritto. */
    public record Voce(IscrittiCorso corso, RigaPagamento iscritto) {

        public boolean isCorso() {
            return iscritto == null;
        }

        Situazione situazione() {
            return iscritto.situazione();
        }

        BigDecimal importo(Function<Situazione, BigDecimal> importo, Function<IscrittiCorso, BigDecimal> totale) {
            return isCorso() ? totale.apply(corso) : importo.apply(situazione());
        }
    }

    private final PagamentoService pagamentoService;
    private final TreeGrid<Voce> grid = new TreeGrid<>();
    private final SegmentedControl<FiltroStato> filtroCorsi = new SegmentedControl<>("Filtra per stato del corso",
        List.of(FiltroStato.values()), FiltroStato::getEtichetta, f -> "Corsi: " + f.getEtichetta(), null);
    private final ComboBox<String> filtroAnno;
    private final RicercaRapida ricerca = new RicercaRapida();
    private final List<Voce> corsiMostrati = new ArrayList<>();
    /** Iscritti mostrati sotto ogni corso: tutti, o con la ricerca solo quelli che corrispondono. */
    private final Map<IscrittiCorso, List<RigaPagamento>> iscrittiMostrati = new HashMap<>();
    private final FooterRow totale;
    private Grid.Column<Voce> colonnaNome;
    private Grid.Column<Voce> colonnaIscritti;
    private Grid.Column<Voce> colonnaQuota;
    private Grid.Column<Voce> colonnaPagato;
    private Grid.Column<Voce> colonnaResiduo;
    private Grid.Column<Voce> colonnaCard;
    private List<IscrittiCorso> corsi = List.of();

    public RiepilogoIscrittiView(PagamentoService pagamentoService) {
        this.pagamentoService = pagamentoService;
        addClassNames("size-full");

        configureGrid();
        totale = grid.appendFooterRow();

        corsi = pagamentoService.iscrittiPerCorso();
        List<String> anni = corsi.stream().map(c -> c.corso().getAnnoScolastico()).filter(Objects::nonNull).distinct()
            .sorted(Comparator.reverseOrder()).toList();
        filtroAnno = ViewSupport.filtroAnnoScolastico(anni);
        filtroAnno.setPlaceholder("Tutti gli anni");
        filtroAnno.setClearButtonVisible(true);
        filtroCorsi.setValue(FiltroStato.ATTIVI);
        filtroCorsi.addValueChangeListener(e -> applicaFiltri());
        filtroAnno.addValueChangeListener(e -> applicaFiltri());
        ricerca.addRicercaListener(this::applicaFiltri);

        Button espandi = new Button("Espandi tutti", VaadinIcon.PLUS_SQUARE_O.create(), e -> grid.expand(corsiMostrati));
        Button comprimi = new Button("Comprimi tutti", VaadinIcon.MINUS_SQUARE_O.create(), e -> grid.collapse(corsiMostrati));
        espandi.addThemeVariants(ButtonVariant.TERTIARY);
        comprimi.addThemeVariants(ButtonVariant.TERTIARY);
        HorizontalLayout filtri = new HorizontalLayout(filtroCorsi, filtroAnno, ricerca, espandi, comprimi);
        filtri.addClassNames("flex-wrap w-full items-center");

        add(filtri, grid);
        grid.addClassNames("grow");
        applicaFiltri();
    }

    private void configureGrid() {
        colonnaNome = grid.addHierarchyColumn(v -> v.isCorso() ? v.corso().corso().getNome() : v.iscritto().allievo().getNomeCompleto())
            .setHeader("Corso / Iscritto").setSortable(false).setWidth("14rem").setFlexGrow(2);
        grid.addColumn(v -> v.isCorso() ? Objects.toString(v.corso().corso().getAnnoScolastico(), "") : "")
            .setHeader("Anno").setWidth("7rem").setFlexGrow(0);
        grid.addColumn(v -> !v.isCorso() && v.iscritto().genitore() != null ? v.iscritto().genitore().getNomeCompleto() : "")
            .setHeader("Genitore");
        grid.addColumn(this::giorni).setHeader("Giorni").setAutoWidth(true).setFlexGrow(0);
        colonnaIscritti = grid.addColumn(v -> v.isCorso() ? String.valueOf(v.corso().iscritti().size()) : "")
            .setHeader("Iscritti").setWidth("6rem").setFlexGrow(0);
        colonnaQuota = euro("Da pagare", Situazione::quota, IscrittiCorso::quota);
        colonnaPagato = euro("Pagato", Situazione::pagato, IscrittiCorso::pagato);
        colonnaResiduo = euro("Residuo", Situazione::residuo, IscrittiCorso::residuo);
        grid.addComponentColumn(this::stato).setHeader("Stato").setWidth("9rem").setFlexGrow(0);
        grid.setPartNameGenerator(v -> v.isCorso() ? "riga-corso" : null);

        colonnaCard = CardMobile.per(grid)
            .titolo(v -> v.isCorso() ? v.corso().corso().getNome() : v.iscritto().allievo().getNomeCompleto())
            .sottotitolo(v -> v.isCorso() ? Objects.toString(v.corso().corso().getAnnoScolastico(), "")
                : v.iscritto().genitore() != null ? "Genitore: " + v.iscritto().genitore().getNomeCompleto() : null)
            .stato(v -> v.isCorso() ? null : StatoPagamentoUI.badge(v.situazione()))
            .campo("Giorni", this::giorni)
            .campo("Iscritti", v -> v.isCorso() ? v.corso().iscritti().size() + " (" + saldati(v.corso()) + ")" : null)
            .campo("Da pagare", v -> Formati.euro(v.importo(Situazione::quota, IscrittiCorso::quota)))
            .campo("Pagato", v -> Formati.euro(v.importo(Situazione::pagato, IscrittiCorso::pagato)))
            .campo("Residuo", v -> Formati.euro(v.importo(Situazione::residuo, IscrittiCorso::residuo)))
            .azioni(v -> v.isCorso() && !v.corso().iscritti().isEmpty() ? apriChiudi(v) : null)
            .classi(v -> v.isCorso() ? null : "ms-6 w-[calc(100%-1.5rem)]")
            .applica();

        // Il pulsante della card del corso cambia testo quando il corso si apre o si chiude
        grid.addExpandListener(e -> e.getItems().forEach(v -> grid.getDataProvider().refreshItem(v)));
        grid.addCollapseListener(e -> e.getItems().forEach(v -> grid.getDataProvider().refreshItem(v)));
        grid.setEmptyStateText("Nessun corso");
    }

    private Grid.Column<Voce> euro(String header, Function<Situazione, BigDecimal> importo,
                                   Function<IscrittiCorso, BigDecimal> totale) {
        return grid.addColumn(v -> Formati.euro(v.importo(importo, totale))).setHeader(header)
            .setWidth("8rem").setFlexGrow(0).setTextAlign(ColumnTextAlign.END);
    }

    private Component stato(Voce voce) {
        if (!voce.isCorso()) {
            return StatoPagamentoUI.badge(voce.situazione());
        }
        Span saldati = new Span(voce.corso().iscritti().isEmpty() ? "Nessun iscritto" : saldati(voce.corso()));
        saldati.addClassNames(Stili.TESTO_SECONDARIO);
        return saldati;
    }

    /** Giorni del corso sulla riga del corso, quelli frequentati sulla riga dell'iscritto. */
    private String giorni(Voce voce) {
        return Formati.giorni(voce.isCorso() ? voce.corso().corso().getGiorniSettimana()
            : voce.iscritto().iscrizione().giorniEffettivi());
    }

    private static String saldati(IscrittiCorso corso) {
        return corso.saldati() + (corso.saldati() == 1 ? " saldato" : " saldati");
    }

    private Button apriChiudi(Voce voce) {
        boolean aperto = grid.isExpanded(voce);
        Button button = new Button(aperto ? "Nascondi iscritti" : "Mostra iscritti",
            (aperto ? VaadinIcon.ANGLE_UP : VaadinIcon.ANGLE_DOWN).create(), e -> {
                if (grid.isExpanded(voce)) {
                    grid.collapse(voce);
                } else {
                    grid.expand(voce);
                }
            });
        button.addThemeVariants(ButtonVariant.TERTIARY);
        return button;
    }

    private void applicaFiltri() {
        FiltroStato stato = filtroCorsi.getValue();
        String anno = filtroAnno.getValue();
        corsiMostrati.clear();
        iscrittiMostrati.clear();
        int conStatoEAnno = 0;
        for (IscrittiCorso corso : corsi) {
            Corso c = corso.corso();
            if ((stato == null || stato.ammette(c)) && (anno == null || anno.equals(c.getAnnoScolastico()))) {
                conStatoEAnno++;
                // Il corso che corrisponde mostra tutti gli iscritti, altrimenti solo quelli che corrispondono
                boolean corsoTrovato = ricerca.corrisponde(RicercaRapida.testo(c.getNome(), c.getAnnoScolastico(), c.getNomiInsegnanti()));
                List<RigaPagamento> iscritti = corsoTrovato ? corso.iscritti() : corso.iscritti().stream()
                    .filter(r -> ricerca.corrisponde(RicercaRapida.testo(r.allievo().getNomeCompleto(),
                        r.allievo().getUser() != null ? r.allievo().getUser().getEmail() : null,
                        r.genitore() != null ? r.genitore().getNomeCompleto() : null)))
                    .toList();
                if (corsoTrovato || !iscritti.isEmpty()) {
                    corsiMostrati.add(new Voce(corso, null));
                    iscrittiMostrati.put(corso, iscritti);
                }
            }
        }
        ricerca.mostraConteggio(corsiMostrati.size(), conStatoEAnno);
        grid.setItems(corsiMostrati, v -> v.isCorso()
            ? iscrittiMostrati.getOrDefault(v.corso(), List.of()).stream().map(r -> new Voce(v.corso(), r)).toList()
            : List.of());
        grid.expand(corsiMostrati);
        aggiornaTotale();
    }

    /** Totale generale dei corsi mostrati, nel footer (su mobile nel footer della colonna card). */
    private void aggiornaTotale() {
        List<IscrittiCorso> mostrati = corsiMostrati.stream().map(Voce::corso).toList();
        int iscritti = mostrati.stream().mapToInt(c -> c.iscritti().size()).sum();
        BigDecimal quota = somma(mostrati, IscrittiCorso::quota);
        BigDecimal pagato = somma(mostrati, IscrittiCorso::pagato);
        BigDecimal residuo = somma(mostrati, IscrittiCorso::residuo);
        totale.getCell(colonnaNome).setText("Totale (" + mostrati.size() + (mostrati.size() == 1 ? " corso)" : " corsi)"));
        totale.getCell(colonnaIscritti).setText(String.valueOf(iscritti));
        totale.getCell(colonnaQuota).setText(Formati.euro(quota));
        totale.getCell(colonnaPagato).setText(Formati.euro(pagato));
        totale.getCell(colonnaResiduo).setText(Formati.euro(residuo));
        totale.getCell(colonnaCard).setText("Totale: " + iscritti + " iscritti · da pagare " + Formati.euro(quota)
            + " · pagato " + Formati.euro(pagato) + " · residuo " + Formati.euro(residuo));
    }

    private static BigDecimal somma(List<IscrittiCorso> corsi, Function<IscrittiCorso, BigDecimal> importo) {
        return corsi.stream().map(importo).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
