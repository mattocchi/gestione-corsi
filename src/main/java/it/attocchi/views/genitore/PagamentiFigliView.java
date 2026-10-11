package it.attocchi.views.genitore;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import it.attocchi.security.SecurityService;
import it.attocchi.service.PagamentoService;
import it.attocchi.views.ElencoPagamenti;
import it.attocchi.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;

/** Quote e pagamenti dei figli del genitore, anche nei corsi non attivi, in sola lettura con la ricevuta. */
@Route(value = "pagamenti-figli", layout = MainLayout.class)
@PageTitle("Pagamenti")
@Menu(title = "Pagamenti", order = 41, icon = "vaadin:euro")
@RolesAllowed("GENITORE")
public class PagamentiFigliView extends VerticalLayout {

    public PagamentiFigliView(PagamentoService pagamentoService, SecurityService securityService) {
        setSizeFull();
        String username = securityService.getUsername().orElse(null);
        ElencoPagamenti elenco = new ElencoPagamenti(pagamentoService,
            () -> username != null ? pagamentoService.righePagamentiDeiFigli(username) : List.of(), false);
        add(elenco);
        expand(elenco);
    }
}
