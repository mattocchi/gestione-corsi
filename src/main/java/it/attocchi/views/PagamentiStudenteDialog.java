package it.attocchi.views;

import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import it.attocchi.entity.Iscrizione;
import it.attocchi.entity.Pagamento;
import it.attocchi.service.PagamentoService.Situazione;

import java.util.List;

/**
 * Dialog in sola lettura con quota, pagato, residuo e rate di uno studente in un corso (vista del genitore).
 * Su mobile la griglia mostra solo data e importo.
 */
public class PagamentiStudenteDialog extends Dialog {

    public PagamentiStudenteDialog(String title, Iscrizione iscrizione, Situazione situazione, List<Pagamento> pagamenti) {
        setHeaderTitle(title);
        setWidth("700px");
        setMaxWidth("100%");

        Div riepilogo = new Div(new Span("Da pagare " + Formati.euro(situazione.quota())),
            new Span("Pagato " + Formati.euro(situazione.pagato())),
            new Span("Residuo " + Formati.euro(situazione.residuo())),
            StatoPagamentoUI.badge(situazione));
        riepilogo.addClassName("pagamenti-riepilogo");

        VerticalLayout content = new VerticalLayout(riepilogo);
        if (iscrizione.getQuotaPersonalizzata() != null) {
            Span quota = new Span("Quota personalizzata (quota piena " + Formati.euro(iscrizione.getCorso().getCostoTotale()) + ")"
                + (iscrizione.getNotaQuota() != null ? ": " + iscrizione.getNotaQuota() : ""));
            quota.addClassName("avviso-sola-lettura");
            content.add(quota);
        }

        Grid<Pagamento> grid = new Grid<>(Pagamento.class, false);
        grid.addColumn(p -> Formati.data(p.getData())).setHeader("Data").setComparator(Pagamento::getData);
        grid.addColumn(p -> Formati.euro(p.getImporto())).setHeader("Importo").setWidth("7rem").setFlexGrow(0);
        var metodo = grid.addColumn(p -> p.getMetodo().getDescrizione()).setHeader("Metodo").setWidth("6.5rem").setFlexGrow(0);
        var nota = grid.addColumn(Pagamento::getNota).setHeader("Nota");
        grid.setItems(pagamenti);
        grid.setAllRowsVisible(true);
        grid.setEmptyStateText("Nessun pagamento registrato");
        ViewSupport.hideOnMobile(grid, metodo, nota);

        content.add(grid);
        content.setPadding(false);
        add(content);
        getFooter().add(ViewSupport.closeButton(this, "Chiudi"));
    }
}
