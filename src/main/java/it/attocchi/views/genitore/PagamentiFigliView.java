package it.attocchi.views.genitore;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import it.attocchi.security.SecurityService;
import it.attocchi.service.PagamentoService;
import it.attocchi.views.ElencoPagamenti;
import it.attocchi.views.MainLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;

/**
 * Quote e pagamenti dei figli del genitore, anche nei corsi non attivi, in sola lettura (ricevute e riepilogo della
 * famiglia sono in Documenti).
 */
@Route(value = "pagamenti-figli", layout = MainLayout.class)
@PageTitle("Pagamenti")
@Menu(title = "Pagamenti", order = 41, icon = "vaadin:euro")
@RolesAllowed("GENITORE")
public class PagamentiFigliView extends VerticalLayout implements BeforeEnterObserver {

    private final ElencoPagamenti elenco;

    public PagamentiFigliView(PagamentoService pagamentoService, SecurityService securityService) {
        addClassNames("size-full");
        String username = securityService.getUsername().orElse(null);
        elenco = new ElencoPagamenti(pagamentoService,
            () -> username != null ? pagamentoService.righePagamentiDeiFigli(username) : List.of(), false);
        add(elenco);
        elenco.addClassNames("grow");
    }

    /** Con {@code ?stato=da-pagare} (dalla Home) apre la scheda di quello stato del pagamento. */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        elenco.mostraStato(event.getLocation().getQueryParameters());
    }
}
