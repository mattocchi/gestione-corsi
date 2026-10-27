package it.attocchi.views;

import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import it.attocchi.service.BilancioService.GiornoDiLezioni;
import it.attocchi.service.BilancioService.LezioniDelMese;

/**
 * Lezioni di un mese dell'insegnante (dal rimborso e da "I miei rimborsi"): una riga per data con corsi, allievi e
 * ore, poi il totale del mese con il preventivo e quanto è già maturato con le lezioni svolte.
 */
public class RiepilogoLezioniMese extends Div {

    private final Grid<GiornoDiLezioni> grid = new Grid<>(GiornoDiLezioni.class, false);
    private final Span totale = new Span();
    private final Span svolte = new Span();

    public RiepilogoLezioniMese() {
        grid.addColumn(g -> Formati.data(g.data())).setHeader("Data").setComparator(GiornoDiLezioni::data)
            .setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(GiornoDiLezioni::corsi).setHeader("Corsi");
        grid.addColumn(g -> g.allievi() > 0 ? String.valueOf(g.allievi()) : "").setHeader("Allievi")
            .setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(g -> Formati.durata(g.minuti())).setHeader("Ore").setComparator(GiornoDiLezioni::minuti)
            .setAutoWidth(true).setFlexGrow(0);
        grid.addComponentColumn(RiepilogoLezioniMese::stato).setHeader("Stato").setWidth("8rem").setFlexGrow(0);
        grid.setAllRowsVisible(true);
        grid.setEmptyStateText("Nessuna lezione in questo mese");
        CardMobile.per(grid)
            .titolo(g -> Formati.data(g.data()))
            .stato(RiepilogoLezioniMese::stato)
            .campo("Corsi", GiornoDiLezioni::corsi)
            .campo("Allievi", g -> g.allievi() > 0 ? g.allievi() : null)
            .campo("Ore", g -> Formati.durata(g.minuti()))
            .applica();

        totale.addClassNames("font-(--aura-font-weight-semibold)");
        svolte.addClassNames(Stili.TESTO_SECONDARIO);
        Div totali = new Div(totale, svolte);
        totali.addClassNames("flex flex-col gap-1");
        add(grid, totali);
        addClassNames("flex flex-col gap-2 w-full");
    }

    /** Mostra le lezioni del mese; {@code null} svuota il riepilogo. */
    public void mostra(LezioniDelMese lezioni) {
        if (lezioni == null) {
            grid.setItems();
            totale.setText("");
            svolte.setText("");
            return;
        }
        grid.setItems(lezioni.giorni());
        String mese = Formati.mese(lezioni.mese());
        totale.setText("Totale di " + mese + ": " + Formati.durata(lezioni.minuti())
            + (lezioni.senzaTariffa() ? " (senza tariffa oraria)" : " · preventivo " + Formati.euro(lezioni.preventivo())));
        svolte.setText("Già svolte: " + Formati.durata(lezioni.minutiSvolti())
            + (lezioni.senzaTariffa() ? "" : " · " + Formati.euro(lezioni.maturato())));
    }

    private static Badge stato(GiornoDiLezioni giorno) {
        if (giorno.svolto()) {
            Badge badge = new Badge("Svolte", VaadinIcon.CHECK.create());
            badge.addThemeVariants(BadgeVariant.SUCCESS);
            return badge;
        }
        return new Badge("Da fare", VaadinIcon.CLOCK.create());
    }
}
