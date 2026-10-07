package it.attocchi.views;

import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import it.attocchi.entity.Presenza;
import it.attocchi.entity.Presenza.StatoPresenza;

import java.util.List;

/**
 * Dialog in sola lettura con le presenze di uno studente in un corso, usato dalle view
 * di studenti e genitori. Su mobile mostra solo data e stato; la nota resta leggibile
 * come tooltip dello stato.
 */
public class PresenzeStudenteDialog extends Dialog {

    public PresenzeStudenteDialog(String title, List<Presenza> presenze) {
        setHeaderTitle(title);
        setWidth("800px");
        setMaxWidth("100%");

        Grid<Presenza> grid = new Grid<>(Presenza.class, false);
        grid.addColumn(presenza -> presenza.getLezione().getData()).setHeader("Data").setAutoWidth(true).setFlexGrow(0);
        var numero = grid.addColumn(presenza -> presenza.getLezione().getNumeroLezione()).setHeader("N. Lezione")
            .setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(presenza -> presenza.getStato().getDescrizione()).setHeader("Stato")
            .setTooltipGenerator(Presenza::getNote);
        var note = grid.addColumn(Presenza::getNote).setHeader("Note");
        grid.setItems(presenze);
        ViewSupport.hideOnMobile(grid, numero, note);

        VerticalLayout content = new VerticalLayout(statistiche(presenze), grid);
        content.setPadding(false);
        add(content);
        getFooter().add(ViewSupport.closeButton(this, "Chiudi"));
    }

    private static HorizontalLayout statistiche(List<Presenza> presenze) {
        HorizontalLayout stats = new HorizontalLayout(
            new Span("Totale lezioni: " + presenze.size()),
            new Span("Presenze: " + conta(presenze, StatoPresenza.PRESENTE)),
            new Span("Assenze giustificate: " + conta(presenze, StatoPresenza.ASSENTE_GIUSTIFICATA)),
            new Span("Assenze ingiustificate: " + conta(presenze, StatoPresenza.ASSENTE_INGIUSTIFICATA)));
        stats.setWrap(true);
        stats.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);
        return stats;
    }

    private static long conta(List<Presenza> presenze, StatoPresenza stato) {
        return presenze.stream().filter(p -> p.getStato() == stato).count();
    }
}
