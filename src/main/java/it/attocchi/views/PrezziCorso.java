package it.attocchi.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.function.SerializableFunction;
import it.attocchi.entity.Corso;
import it.attocchi.service.PagamentoService.Situazione;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Map;

/**
 * Prezzi del corso nelle liste di allievi e genitori: prezzo di listino, prezzo con pre-iscrizione e quota dovuta dal
 * partecipante (l'importo che vale, in evidenza, con un badge se è scontata o maggiorata rispetto al listino), come
 * colonne su desktop e come campi della card su mobile. {@code situazioni} è indicizzata per id del corso.
 */
public final class PrezziCorso {

    private PrezziCorso() {
    }

    /** Aggiunge alla griglia le colonne dei prezzi. */
    public static void aggiungiColonne(Grid<Corso> grid, Map<Long, Situazione> situazioni) {
        euro(grid, "Prezzo di listino", Corso::getCostoTotale);
        euro(grid, "Pre-iscrizione", Corso::getPrezzoPreiscrizione);
        SerializableFunction<Corso, BigDecimal> dovuta = applicato(situazioni);
        grid.addComponentColumn(corso -> quotaDovuta(corso, dovuta.apply(corso))).setHeader("Quota dovuta")
            .setComparator(Comparator.comparing(dovuta, Comparator.nullsFirst(Comparator.naturalOrder())))
            .setAutoWidth(true).setFlexGrow(0);
    }

    /** Aggiunge alla card mobile gli stessi prezzi delle colonne. */
    public static CardMobile<Corso> aggiungiCampi(CardMobile<Corso> card, Map<Long, Situazione> situazioni) {
        SerializableFunction<Corso, BigDecimal> applicato = applicato(situazioni);
        return card
            .campo("Costo totale", corso -> Formati.euro(corso.getCostoTotale()))
            .campo("Pre-iscrizione", corso -> corso.getPrezzoPreiscrizione() != null
                ? Formati.euro(corso.getPrezzoPreiscrizione()) : null)
            .campo("Quota dovuta", corso -> applicato.apply(corso) != null ? quotaDovuta(corso, applicato.apply(corso)) : null);
    }

    /** Quota dovuta in evidenza, con il badge "Sconto -X €" (o "Maggiorazione +X €") rispetto al listino. */
    private static Component quotaDovuta(Corso corso, BigDecimal dovuta) {
        Span importo = new Span(Formati.euro(dovuta));
        importo.addClassNames("font-(--aura-font-weight-semibold)");
        Div cella = new Div(importo);
        cella.addClassNames("flex flex-wrap items-center gap-1");
        BigDecimal listino = corso.getCostoTotale();
        if (dovuta != null && listino != null && dovuta.compareTo(listino) != 0) {
            boolean sconto = dovuta.compareTo(listino) < 0;
            Badge badge = new Badge((sconto ? "Sconto -" : "Maggiorazione +") + Formati.euro(listino.subtract(dovuta).abs()));
            badge.addThemeVariants(sconto ? BadgeVariant.SUCCESS : BadgeVariant.WARNING);
            cella.add(badge);
        }
        return cella;
    }

    private static SerializableFunction<Corso, BigDecimal> applicato(Map<Long, Situazione> situazioni) {
        return corso -> {
            Situazione situazione = situazioni.get(corso.getId());
            return situazione != null ? situazione.quota() : null;
        };
    }

    private static Grid.Column<Corso> euro(Grid<Corso> grid, String header, SerializableFunction<Corso, BigDecimal> valore) {
        return grid.addColumn(corso -> Formati.euro(valore.apply(corso))).setHeader(header)
            .setComparator(Comparator.comparing(valore, Comparator.nullsFirst(Comparator.naturalOrder())))
            .setWidth("8.5rem").setFlexGrow(0);
    }
}
