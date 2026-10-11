package it.attocchi.views;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.signals.Signal;
import it.attocchi.entity.Corso;
import it.attocchi.service.PagamentoService.Situazione;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Prezzi del corso nelle liste di allievi e genitori: costo totale, prezzo con pre-iscrizione e prezzo applicato
 * (quota dovuta dal partecipante). Su mobile la colonna Corso e i prezzi sono riassunti in una colonna su due righe.
 */
public final class PrezziCorso {

    private PrezziCorso() {
    }

    /**
     * Aggiunge alla griglia le colonne dei prezzi; {@code nome} è la colonna con il nome del corso, sostituita su mobile
     * dal riepilogo. {@code situazioni} è indicizzata per id del corso.
     */
    public static void aggiungiColonne(Grid<Corso> grid, Grid.Column<Corso> nome, Map<Long, Situazione> situazioni) {
        SerializableFunction<Corso, BigDecimal> applicato = corso -> {
            Situazione situazione = situazioni.get(corso.getId());
            return situazione != null ? situazione.quota() : null;
        };
        var riepilogo = grid.addColumn(LitRenderer.<Corso>of(
                "<div class=\"riepilogo-righe\"><div class=\"riepilogo-righe-titolo\">${item.corso}</div>"
                    + "<div class=\"riepilogo-righe-dettaglio\">${item.dettaglio}</div></div>")
                .withProperty("corso", Corso::getNome)
                .withProperty("dettaglio", corso -> dettaglio(corso, applicato.apply(corso))))
            .setHeader("Corso");
        var costo = euro(grid, "Costo totale", Corso::getCostoTotale);
        var preiscrizione = euro(grid, "Pre-iscrizione", Corso::getPrezzoPreiscrizione);
        var quota = euro(grid, "Applicato", applicato);
        // Riepilogo al posto della colonna Corso
        List<Grid.Column<Corso>> ordine = new ArrayList<>(grid.getColumns());
        ordine.remove(riepilogo);
        ordine.add(ordine.indexOf(nome), riepilogo);
        grid.setColumnOrder(ordine);
        Signal<Boolean> mobile = ViewSupport.isMobile();
        Signal.effect(grid, () -> {
            boolean m = mobile.get();
            riepilogo.setVisible(m);
            nome.setVisible(!m);
            costo.setVisible(!m);
            preiscrizione.setVisible(!m);
            quota.setVisible(!m);
        });
    }

    private static String dettaglio(Corso corso, BigDecimal applicato) {
        String testo = "Applicato " + Formati.euro(applicato) + " · costo " + Formati.euro(corso.getCostoTotale());
        if (corso.getPrezzoPreiscrizione() != null) {
            testo += " · pre-iscrizione " + Formati.euro(corso.getPrezzoPreiscrizione());
        }
        return testo;
    }

    private static Grid.Column<Corso> euro(Grid<Corso> grid, String header, SerializableFunction<Corso, BigDecimal> valore) {
        return grid.addColumn(corso -> Formati.euro(valore.apply(corso))).setHeader(header)
            .setComparator(Comparator.comparing(valore, Comparator.nullsFirst(Comparator.naturalOrder())))
            .setWidth("8.5rem").setFlexGrow(0);
    }
}
