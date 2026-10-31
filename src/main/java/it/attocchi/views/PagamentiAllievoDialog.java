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
import java.util.stream.Collectors;

/**
 * Dialog in sola lettura con quota, pagato, residuo e rate di un allievo in un corso (vista del genitore).
 * Su mobile la griglia mostra solo data e importo.
 */
public class PagamentiAllievoDialog extends Dialog {

    public PagamentiAllievoDialog(String title, Iscrizione iscrizione, Situazione situazione, List<Pagamento> pagamenti) {
        setHeaderTitle(title);
        setWidth("700px");
        setMaxWidth("100%");

        Div riepilogo = new Div(new Span("Da pagare " + Formati.euro(situazione.quota())),
            new Span("Pagato " + Formati.euro(situazione.pagato())),
            new Span("Residuo " + Formati.euro(situazione.residuo())),
            StatoPagamentoUI.badge(situazione));
        riepilogo.addClassNames(Stili.RIEPILOGO_PAGAMENTI);

        VerticalLayout content = new VerticalLayout(riepilogo);
        if (iscrizione.getQuotaPersonalizzata() != null) {
            Span quota = new Span("Quota personalizzata (quota piena " + Formati.euro(iscrizione.getCorso().getCostoTotale()) + ")"
                + (iscrizione.getNotaQuota() != null ? ": " + iscrizione.getNotaQuota() : ""));
            quota.addClassNames(Stili.TESTO_SECONDARIO);
            content.add(quota);
        }
        if (!situazione.opzioni().isEmpty()) {
            // Opzioni aggiuntive attive, già comprese nella quota
            Span opzioni = new Span("Comprende le opzioni: " + situazione.opzioni().stream()
                .map(o -> o.getDescrizione() + " +" + Formati.euro(o.getImporto())).collect(Collectors.joining(", ")));
            opzioni.addClassNames(Stili.TESTO_SECONDARIO);
            content.add(opzioni);
        }

        Grid<Pagamento> grid = new Grid<>(Pagamento.class, false);
        grid.addColumn(p -> Formati.data(p.getData())).setHeader("Data").setComparator(Pagamento::getData);
        grid.addColumn(p -> Formati.euro(p.getImporto())).setHeader("Importo").setWidth("7rem").setFlexGrow(0);
        grid.addColumn(p -> p.getMetodo().getDescrizione()).setHeader("Metodo").setWidth("6.5rem").setFlexGrow(0);
        grid.addColumn(Pagamento::getNota).setHeader("Nota");
        grid.setItems(pagamenti);
        grid.setAllRowsVisible(true);
        grid.setEmptyStateText("Nessun pagamento registrato");
        CardMobile.per(grid)
            .titolo(p -> Formati.euro(p.getImporto()))
            .sottotitolo(p -> Formati.data(p.getData()))
            .campo("Metodo", p -> p.getMetodo().getDescrizione())
            .campo("Nota", Pagamento::getNota)
            .applica();

        content.add(grid);
        content.addClassNames("p-0");
        add(content);
        getFooter().add(ViewSupport.closeButton(this, "Chiudi"));
    }
}
