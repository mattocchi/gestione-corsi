package it.attocchi.views;

import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import it.attocchi.entity.GruppoCorso;
import it.attocchi.entity.Iscrizione;
import it.attocchi.entity.Lezione;
import it.attocchi.entity.Presenza;
import it.attocchi.service.ConteggioLezioni;

import java.util.List;

/**
 * Dialog in sola lettura con le lezioni di un allievo in un corso, usato dalle view di allievi, genitori e
 * insegnanti: in alto il {@link ConteggioLezioni} (svolte, da fare, mancanti, presenze e assenze), sotto ogni
 * lezione con il suo stato ("Da fare" finché non è completata). Su mobile mostra solo data e stato; la nota resta
 * leggibile come tooltip dello stato.
 */
public class PresenzeAllievoDialog extends Dialog {

    /**
     * @param iscrizione     l'iscrizione dell'allievo al corso, per l'orario di ogni lezione ({@code Iscrizione.orarioDi})
     * @param gruppiDelCorso i gruppi del corso, per il numero della lezione con il gruppo o il giorno
     * @param previste       lezioni previste per partecipante ({@code Corso.numeroLezioni})
     * @param noteStaff      mostra anche le note della lezione per lo staff (solo agli insegnanti)
     */
    public PresenzeAllievoDialog(String title, List<Presenza> presenze, Iscrizione iscrizione, List<GruppoCorso> gruppiDelCorso,
                                 Integer previste, boolean noteStaff) {
        setHeaderTitle(title);
        setWidth("800px");
        setMaxWidth("100%");

        Grid<Presenza> grid = new Grid<>(Presenza.class, false);
        grid.addColumn(presenza -> Formati.data(presenza.getLezione().getData())).setHeader("Data")
            .setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(presenza -> Formati.ora(iscrizione.orarioDi(presenza.getLezione()))).setHeader("Orario")
            .setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(presenza -> Formati.numeroLezione(presenza.getLezione(), gruppiDelCorso)).setHeader("N. Lezione")
            .setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(PresenzeAllievoDialog::stato).setHeader("Stato")
            .setTooltipGenerator(presenza -> presenza.getLezione().isNonEffettuata()
                ? presenza.getLezione().getMotivoNonEffettuata() : presenza.getNote());
        grid.addColumn(Presenza::getNote).setHeader("Note");
        grid.addColumn(presenza -> presenza.getLezione().getNotePubbliche()).setHeader("Note della lezione");
        grid.addColumn(presenza -> presenza.getLezione().getNote()).setHeader("Note per lo staff").setVisible(noteStaff);
        grid.setItems(presenze);
        CardMobile<Presenza> card = CardMobile.per(grid)
            .titolo(presenza -> Formati.data(presenza.getLezione().getData()))
            .campo("Orario", presenza -> Formati.ora(iscrizione.orarioDi(presenza.getLezione())))
            .campo("N. Lezione", presenza -> Formati.numeroLezione(presenza.getLezione(), gruppiDelCorso))
            .campo("Stato", PresenzeAllievoDialog::stato)
            .campo("Note", presenza -> presenza.getLezione().isNonEffettuata()
                ? presenza.getLezione().getMotivoNonEffettuata() : presenza.getNote())
            .campo("Note della lezione", presenza -> presenza.getLezione().getNotePubbliche());
        if (noteStaff) {
            card.campo("Note per lo staff", presenza -> presenza.getLezione().getNote());
        }
        card.applica();

        VerticalLayout content = new VerticalLayout(statistiche(ConteggioLezioni.di(presenze, previste)), grid);
        content.addClassNames("p-0");
        add(content);
        getFooter().add(ViewSupport.closeButton(this, "Chiudi"));
    }

    private static String stato(Presenza presenza) {
        Lezione lezione = presenza.getLezione();
        if (lezione.isNonEffettuata()) {
            return "Lezione non effettuata";
        }
        // Le presenze delle lezioni future nascono "Presente": finché la lezione non è completata non dicono nulla
        return lezione.isCompletata() ? presenza.getStato().getDescrizione() : "Da fare";
    }

    /** Lezioni svolte, da fare e mancanti sulle previste; presenze e assenze solo sulle svolte. */
    private static HorizontalLayout statistiche(ConteggioLezioni conteggio) {
        HorizontalLayout stats = new HorizontalLayout(
            new Span("Svolte: " + conteggio.svolte() + " su " + conteggio.previste()),
            new Span("Da fare: " + conteggio.daFare()),
            new Span("Mancanti: " + conteggio.mancanti()),
            new Span("Presenze: " + conteggio.presenze()),
            new Span("Assenze giustificate: " + conteggio.giustificate()),
            new Span("Assenze ingiustificate: " + conteggio.ingiustificate()));
        stats.addClassNames("flex-wrap items-baseline");
        return stats;
    }
}
