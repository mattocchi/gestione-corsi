package it.attocchi.views;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.function.SerializableFunction;
import it.attocchi.entity.Corso;
import it.attocchi.service.PagamentoService.Situazione;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Map;

/**
 * Prezzi del corso nelle liste di allievi e genitori: costo totale, prezzo con pre-iscrizione e prezzo applicato
 * (quota dovuta dal partecipante), come colonne su desktop e come campi della card su mobile.
 * {@code situazioni} è indicizzata per id del corso.
 */
public final class PrezziCorso {

    private PrezziCorso() {
    }

    /** Aggiunge alla griglia le colonne dei prezzi. */
    public static void aggiungiColonne(Grid<Corso> grid, Map<Long, Situazione> situazioni) {
        euro(grid, "Costo totale", Corso::getCostoTotale);
        euro(grid, "Pre-iscrizione", Corso::getPrezzoPreiscrizione);
        euro(grid, "Applicato", applicato(situazioni));
    }

    /** Aggiunge alla card mobile gli stessi prezzi delle colonne. */
    public static CardMobile<Corso> aggiungiCampi(CardMobile<Corso> card, Map<Long, Situazione> situazioni) {
        SerializableFunction<Corso, BigDecimal> applicato = applicato(situazioni);
        return card
            .campo("Costo totale", corso -> Formati.euro(corso.getCostoTotale()))
            .campo("Pre-iscrizione", corso -> corso.getPrezzoPreiscrizione() != null
                ? Formati.euro(corso.getPrezzoPreiscrizione()) : null)
            .campo("Applicato", corso -> Formati.euro(applicato.apply(corso)));
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
