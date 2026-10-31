package it.attocchi.views.admin;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import it.attocchi.service.PagamentoService;
import it.attocchi.views.ElencoPagamenti;
import it.attocchi.views.MainLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import jakarta.annotation.security.RolesAllowed;

/** Quote e pagamenti di tutti i partecipanti, con filtri (admin e segreteria). */
@Route(value = "pagamenti", layout = MainLayout.class)
@PageTitle("Pagamenti")
@Menu(title = "Pagamenti", order = 10.5, icon = "vaadin:euro")
@RolesAllowed({"ADMIN", "SEGRETERIA"})
public class GestionePagamentiView extends VerticalLayout implements BeforeEnterObserver {

    private final ElencoPagamenti elenco;

    public GestionePagamentiView(PagamentoService pagamentoService) {
        addClassNames("size-full");
        elenco = new ElencoPagamenti(pagamentoService, pagamentoService::righePagamenti, true);
        add(elenco);
        elenco.addClassNames("grow");
    }

    /** Con {@code ?stato=da-pagare} (dalla Home) apre la scheda di quello stato del pagamento. */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        elenco.mostraStato(event.getLocation().getQueryParameters());
    }
}
