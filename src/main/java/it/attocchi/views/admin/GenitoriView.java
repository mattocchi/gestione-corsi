package it.attocchi.views.admin;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import it.attocchi.entity.Genitore;
import it.attocchi.entity.Studente;
import it.attocchi.service.GenitoreService;
import it.attocchi.views.MainLayout;
import it.attocchi.views.ViewSupport;
import jakarta.annotation.security.RolesAllowed;

import java.util.stream.Collectors;

/**
 * Elenco in sola lettura dei genitori con i loro figli.
 */
@Route(value = "genitori", layout = MainLayout.class)
@PageTitle("Genitori")
@Menu(title = "Genitori", order = 14, icon = "vaadin:family")
@RolesAllowed({"ADMIN", "SEGRETERIA"})
public class GenitoriView extends VerticalLayout {

    private final Grid<Genitore> grid = new Grid<>(Genitore.class, false);

    public GenitoriView(GenitoreService genitoreService) {
        setSizeFull();

        grid.addColumn(Genitore::getNomeCompleto).setHeader("Nome").setSortable(true);
        var email = grid.addColumn(genitore -> genitore.getUser() != null ? genitore.getUser().getEmail() : "")
            .setHeader("Email");
        var telefono = grid.addColumn(genitore -> genitore.getUser() != null ? genitore.getUser().getTelefono() : "")
            .setHeader("Telefono");
        var figli = grid.addColumn(GenitoriView::nomiFigli).setHeader("Figli");
        grid.addColumn(genitore -> genitore.getFigli().size()).setHeader("N. Figli").setWidth("6.5rem").setFlexGrow(0);
        ViewSupport.hideOnMobile(grid, email, telefono, figli);

        grid.setItems(genitoreService.findAllWithFigli());
        add(grid);
        expand(grid);
    }

    private static String nomiFigli(Genitore genitore) {
        return genitore.getFigli().stream()
            .map(Studente::getNomeCompleto)
            .sorted()
            .collect(Collectors.joining(", "));
    }
}
