package it.attocchi.views.admin;

import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import it.attocchi.service.BilancioService.Bilancio;
import it.attocchi.service.BilancioService.BilancioCorso;
import it.attocchi.views.CardMobile;
import it.attocchi.views.Formati;
import it.attocchi.views.HomeView;
import it.attocchi.views.Stili;
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
        content.addClassNames("p-0");
        Span tariffa = new Span(bilancio.senzaTariffa()
            ? "Tariffa oraria non impostata: i costi delle lezioni sono a zero"
            : "Tariffa oraria " + Formati.euro(bilancio.insegnante().getTariffaOraria())
                + " · costo di una lezione = allievi iscritti × durata × tariffa");
        tariffa.addClassNames(Stili.TESTO_SECONDARIO);
        content.add(tariffa);
        if (bilancio.forfait().signum() > 0) {
            Span forfait = new Span("Rimborso forfettario annuo " + Formati.euro(bilancio.forfait())
                + " · compreso nel maturato e nel costo previsto");
            forfait.addClassNames(Stili.TESTO_SECONDARIO);
            content.add(forfait);
        }
        content.add(
            HomeView.sezione("Effettivo",
                HomeView.indicatore(VaadinIcon.EURO, "Incassato", Formati.euro(bilancio.incassato()), "dagli allievi"),
                HomeView.indicatore(VaadinIcon.WALLET, "Rimborsato", Formati.euro(bilancio.rimborsato()), "all'insegnante"),
                HomeView.indicatore(VaadinIcon.SCALE, "Saldo", Formati.euro(bilancio.saldo()), "incassato − rimborsato")),
            HomeView.sezione("Maturato",
                HomeView.indicatore(VaadinIcon.CHECK, "Lezioni completate", Formati.euro(bilancio.costoLezioniMaturato()), "costo"),
                HomeView.indicatore(VaadinIcon.HOURGLASS, "Da rimborsare", Formati.euro(bilancio.daRimborsare()),
                    bilancio.forfait().signum() > 0 ? "lezioni + forfait − rimborsato" : "maturato − rimborsato")),
            HomeView.sezione("Teorico",
                HomeView.indicatore(VaadinIcon.EURO, "Incasso previsto", Formati.euro(bilancio.incassoPrevisto()),
                    "quote assegnate"),
                HomeView.indicatore(VaadinIcon.CALENDAR, "Costo previsto", Formati.euro(bilancio.costoPrevisto()),
                    bilancio.forfait().signum() > 0 ? "lezioni programmate + forfait" : "lezioni programmate"),
                HomeView.indicatore(VaadinIcon.SCALE, "Margine previsto", Formati.euro(bilancio.marginePrevisto()),
                    "incasso − costo previsti")),
            ViewSupport.sezione("Per corso"), grigliaCorsi(bilancio));
        add(content);
        getFooter().add(ViewSupport.closeButton(this, "Chiudi"));
    }

    private static Grid<BilancioCorso> grigliaCorsi(Bilancio bilancio) {
        Grid<BilancioCorso> grid = new Grid<>(BilancioCorso.class, false);
        grid.addColumn(c -> c.corso().getNome()).setHeader("Corso");
        grid.addColumn(BilancioCorso::iscritti).setHeader("Allievi").setWidth("6rem").setFlexGrow(0);
        euro(grid, "Incassato", BilancioCorso::incassato);
        euro(grid, "Incasso previsto", BilancioCorso::incassoPrevisto);
        euro(grid, "Costo maturato", BilancioCorso::costoMaturato);
        euro(grid, "Costo previsto", BilancioCorso::costoPrevisto);
        CardMobile.per(grid)
            .titolo(c -> c.corso().getNome())
            .campo("Allievi", BilancioCorso::iscritti)
            .campo("Incassato", c -> Formati.euro(c.incassato()))
            .campo("Incasso previsto", c -> Formati.euro(c.incassoPrevisto()))
            .campo("Costo maturato", c -> Formati.euro(c.costoMaturato()))
            .campo("Costo previsto", c -> Formati.euro(c.costoPrevisto()))
            .applica();
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
