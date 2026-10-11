package it.attocchi.views.admin;

import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.signals.Signal;
import it.attocchi.service.BilancioService.Bilancio;
import it.attocchi.service.BilancioService.BilancioCorso;
import it.attocchi.views.Formati;
import it.attocchi.views.HomeView;
import it.attocchi.views.ViewSupport;

import java.math.BigDecimal;
import java.util.function.Function;

/**
 * Mini bilancio di un insegnante per anno scolastico: effettivo (incassato dagli allievi dei suoi corsi e
 * rimborsato all'insegnante), maturato (costo delle lezioni completate) e teorico (quote assegnate e costo di
 * tutte le lezioni programmate), con il dettaglio per corso.
 */
public class BilancioDialog extends Dialog {

    public BilancioDialog(Bilancio bilancio) {
        setHeaderTitle("Bilancio " + bilancio.insegnante().getNomeCompleto() + " – " + bilancio.annoScolastico());
        setWidth("960px");
        setMaxWidth("100%");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        Span tariffa = new Span(bilancio.senzaTariffa()
            ? "Tariffa oraria non impostata: i costi delle lezioni sono a zero"
            : "Tariffa oraria " + Formati.euro(bilancio.insegnante().getTariffaOraria())
                + " · costo di una lezione = allievi iscritti × durata × tariffa");
        tariffa.addClassName("avviso-sola-lettura");
        content.add(tariffa,
            HomeView.sezione("Effettivo",
                HomeView.indicatore(VaadinIcon.EURO, "Incassato", Formati.euro(bilancio.incassato()), "dagli allievi"),
                HomeView.indicatore(VaadinIcon.WALLET, "Rimborsato", Formati.euro(bilancio.rimborsato()), "all'insegnante"),
                HomeView.indicatore(VaadinIcon.SCALE, "Saldo", Formati.euro(bilancio.saldo()), "incassato − rimborsato")),
            HomeView.sezione("Maturato",
                HomeView.indicatore(VaadinIcon.CHECK, "Lezioni completate", Formati.euro(bilancio.costoMaturato()), "costo"),
                HomeView.indicatore(VaadinIcon.HOURGLASS, "Da rimborsare", Formati.euro(bilancio.daRimborsare()),
                    "maturato − rimborsato")),
            HomeView.sezione("Teorico",
                HomeView.indicatore(VaadinIcon.EURO, "Incasso previsto", Formati.euro(bilancio.incassoPrevisto()),
                    "quote assegnate"),
                HomeView.indicatore(VaadinIcon.CALENDAR, "Costo previsto", Formati.euro(bilancio.costoPrevisto()),
                    "lezioni programmate"),
                HomeView.indicatore(VaadinIcon.SCALE, "Margine previsto", Formati.euro(bilancio.marginePrevisto()),
                    "incasso − costo previsti")),
            ViewSupport.sezione("Per corso"), grigliaCorsi(bilancio));
        add(content);
        getFooter().add(ViewSupport.closeButton(this, "Chiudi"));
    }

    private static Grid<BilancioCorso> grigliaCorsi(Bilancio bilancio) {
        Grid<BilancioCorso> grid = new Grid<>(BilancioCorso.class, false);
        // Su mobile corso e importi su più righe
        var riepilogo = grid.addColumn(LitRenderer.<BilancioCorso>of(
                "<div class=\"riepilogo-righe\"><div class=\"riepilogo-righe-titolo\">${item.corso}</div>"
                    + "<div class=\"riepilogo-righe-dettaglio\">${item.incassi}</div>"
                    + "<div class=\"riepilogo-righe-dettaglio\">${item.costi}</div></div>")
                .withProperty("corso", c -> c.corso().getNome() + " (" + c.iscritti() + " allievi)")
                .withProperty("incassi", c -> "Incassato " + Formati.euro(c.incassato()) + " su " + Formati.euro(c.incassoPrevisto()))
                .withProperty("costi", c -> "Costo " + Formati.euro(c.costoMaturato()) + " su " + Formati.euro(c.costoPrevisto())))
            .setHeader("Corso");
        var corso = grid.addColumn(c -> c.corso().getNome()).setHeader("Corso");
        var allievi = grid.addColumn(BilancioCorso::iscritti).setHeader("Allievi").setWidth("6rem").setFlexGrow(0);
        var incassato = euro(grid, "Incassato", BilancioCorso::incassato);
        var incassoPrevisto = euro(grid, "Incasso previsto", BilancioCorso::incassoPrevisto);
        var costoMaturato = euro(grid, "Costo maturato", BilancioCorso::costoMaturato);
        var costoPrevisto = euro(grid, "Costo previsto", BilancioCorso::costoPrevisto);
        Signal<Boolean> mobile = ViewSupport.isMobile();
        Signal.effect(grid, () -> riepilogo.setVisible(mobile.get()));
        ViewSupport.hideOnMobile(grid, corso, allievi, incassato, incassoPrevisto, costoMaturato, costoPrevisto);
        grid.setItems(bilancio.corsi());
        grid.setAllRowsVisible(true);
        grid.setEmptyStateText("Nessun corso dell'insegnante in questo anno scolastico");
        return grid;
    }

    private static Grid.Column<BilancioCorso> euro(Grid<BilancioCorso> grid, String header,
                                                   Function<BilancioCorso, BigDecimal> valore) {
        return grid.addColumn(c -> Formati.euro(valore.apply(c))).setHeader(header).setWidth("9rem").setFlexGrow(0);
    }
}
