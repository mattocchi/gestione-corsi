package it.attocchi.views;

import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import it.attocchi.entity.Presenza;
import it.attocchi.entity.Presenza.StatoPresenza;

import java.util.List;

/**
 * Dialog in sola lettura con le presenze di un allievo in un corso, usato dalle view
 * di allievi e genitori. Su mobile mostra solo data e stato; la nota resta leggibile
 * come tooltip dello stato.
 */
public class PresenzeAllievoDialog extends Dialog {

    public PresenzeAllievoDialog(String title, List<Presenza> presenze) {
        setHeaderTitle(title);
        setWidth("800px");
        setMaxWidth("100%");

        Grid<Presenza> grid = new Grid<>(Presenza.class, false);
        grid.addColumn(presenza -> Formati.data(presenza.getLezione().getData())).setHeader("Data")
            .setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(presenza -> presenza.getLezione().getNumeroLezione()).setHeader("N. Lezione")
            .setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(PresenzeAllievoDialog::stato).setHeader("Stato")
            .setTooltipGenerator(presenza -> presenza.getLezione().isNonEffettuata()
                ? presenza.getLezione().getMotivoNonEffettuata() : presenza.getNote());
        grid.addColumn(Presenza::getNote).setHeader("Note");
        grid.setItems(presenze);
        CardMobile.per(grid)
            .titolo(presenza -> Formati.data(presenza.getLezione().getData()))
            .campo("N. Lezione", presenza -> presenza.getLezione().getNumeroLezione())
            .campo("Stato", PresenzeAllievoDialog::stato)
            .campo("Note", presenza -> presenza.getLezione().isNonEffettuata()
                ? presenza.getLezione().getMotivoNonEffettuata() : presenza.getNote())
            .applica();

        VerticalLayout content = new VerticalLayout(statistiche(presenze), grid);
        content.addClassNames("p-0");
        add(content);
        getFooter().add(ViewSupport.closeButton(this, "Chiudi"));
    }

    private static String stato(Presenza presenza) {
        return presenza.getLezione().isNonEffettuata() ? "Lezione non effettuata" : presenza.getStato().getDescrizione();
    }

    /** Statistiche delle sole lezioni effettuate (o da fare): le non effettuate non contano. */
    private static HorizontalLayout statistiche(List<Presenza> tutte) {
        List<Presenza> presenze = tutte.stream().filter(p -> !p.getLezione().isNonEffettuata()).toList();
        HorizontalLayout stats = new HorizontalLayout(
            new Span("Totale lezioni: " + presenze.size()),
            new Span("Presenze: " + conta(presenze, StatoPresenza.PRESENTE)),
            new Span("Assenze giustificate: " + conta(presenze, StatoPresenza.ASSENTE_GIUSTIFICATA)),
            new Span("Assenze ingiustificate: " + conta(presenze, StatoPresenza.ASSENTE_INGIUSTIFICATA)));
        stats.addClassNames("flex-wrap items-baseline");
        return stats;
    }

    private static long conta(List<Presenza> presenze, StatoPresenza stato) {
        return presenze.stream().filter(p -> p.getStato() == stato).count();
    }
}
