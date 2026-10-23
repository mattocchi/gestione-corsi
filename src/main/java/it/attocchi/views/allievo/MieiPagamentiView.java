package it.attocchi.views.allievo;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import it.attocchi.security.SecurityService;
import it.attocchi.service.AllievoService;
import it.attocchi.service.PagamentoService;
import it.attocchi.views.ElencoPagamenti;
import it.attocchi.views.MainLayout;
import it.attocchi.views.Stili;
import jakarta.annotation.security.RolesAllowed;

/**
 * Pagamenti dell'allievo adulto (senza genitore), che li gestisce da sé come un genitore di sé stesso: quote e rate
 * dei suoi corsi, in sola lettura (ricevute e riepiloghi sono in Documenti). La voce di menu compare solo a lui
 * ({@code MainLayout}); chi ha un genitore vede un avviso.
 */
@Route(value = "miei-pagamenti", layout = MainLayout.class)
@PageTitle("Pagamenti")
@Menu(title = "Pagamenti", order = 31, icon = "vaadin:euro")
@RolesAllowed("ALLIEVO")
public class MieiPagamentiView extends VerticalLayout {

    /** Percorso della vista, usato da {@code MainLayout} per mostrare la voce solo agli allievi adulti. */
    public static final String PERCORSO = "miei-pagamenti";

    public MieiPagamentiView(PagamentoService pagamentoService, AllievoService allievoService,
                             SecurityService securityService) {
        addClassNames("size-full");
        String username = securityService.getUsername().orElse(null);
        if (!allievoService.gestisceDaSe(username)) {
            Span avviso = new Span("I pagamenti dei tuoi corsi sono gestiti dal genitore.");
            avviso.addClassNames(Stili.TESTO_SECONDARIO);
            add(avviso);
            return;
        }
        ElencoPagamenti elenco = new ElencoPagamenti(pagamentoService, () -> pagamentoService.righePagamentiPropri(username), false);
        add(elenco);
        elenco.addClassNames("grow");
    }
}
