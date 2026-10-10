package it.attocchi.views.admin;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import it.attocchi.entity.Insegnante;
import it.attocchi.service.InsegnanteService;
import it.attocchi.views.MainLayout;
import it.attocchi.views.ViewSupport;
import jakarta.annotation.security.RolesAllowed;

/**
 * Elenco in sola lettura degli insegnanti con il numero di corsi assegnati.
 */
@Route(value = "insegnanti", layout = MainLayout.class)
@PageTitle("Insegnanti")
@Menu(title = "Insegnanti", order = 13, icon = "vaadin:user-star")
@RolesAllowed({"ADMIN", "SEGRETERIA"})
public class InsegnantiView extends VerticalLayout {

    private final Grid<Insegnante> grid = new Grid<>(Insegnante.class, false);

    public InsegnantiView(InsegnanteService insegnanteService) {
        setSizeFull();

        grid.addColumn(Insegnante::getNomeCompleto).setHeader("Nome").setSortable(true);
        var specializzazione = grid.addColumn(Insegnante::getSpecializzazione).setHeader("Specializzazione").setSortable(true);
        var email = grid.addColumn(insegnante -> insegnante.getUser() != null ? insegnante.getUser().getEmail() : "")
            .setHeader("Email");
        grid.addColumn(insegnante -> insegnante.getCorsi().size()).setHeader("N. Corsi").setWidth("6.5rem").setFlexGrow(0);
        ViewSupport.hideOnMobile(grid, specializzazione, email);

        grid.setItems(insegnanteService.findAllWithCorsi());
        add(grid);
        expand(grid);
    }
}
