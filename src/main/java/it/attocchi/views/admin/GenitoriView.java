package it.attocchi.views.admin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import it.attocchi.entity.Genitore;
import it.attocchi.entity.Studente;
import it.attocchi.entity.User;
import it.attocchi.service.GenitoreService;
import it.attocchi.service.UserService;
import it.attocchi.views.AnagraficaFields;
import it.attocchi.views.MainLayout;
import it.attocchi.views.UtenteFields;
import it.attocchi.views.ViewSupport;
import jakarta.annotation.security.RolesAllowed;

import java.util.stream.Collectors;

/**
 * Genitori con i loro figli: admin e segreteria li creano e ne modificano utente e dati anagrafici.
 */
@Route(value = "genitori", layout = MainLayout.class)
@PageTitle("Genitori")
@Menu(title = "Genitori", order = 14, icon = "vaadin:family")
@RolesAllowed({"ADMIN", "SEGRETERIA"})
public class GenitoriView extends VerticalLayout {

    private final GenitoreService genitoreService;
    private final UserService userService;
    private final Grid<Genitore> grid = new Grid<>(Genitore.class, false);

    public GenitoriView(GenitoreService genitoreService, UserService userService) {
        this.genitoreService = genitoreService;
        this.userService = userService;
        setSizeFull();

        grid.addColumn(Genitore::getNomeCompleto).setHeader("Nome").setSortable(true);
        var email = grid.addColumn(genitore -> genitore.getUser() != null ? genitore.getUser().getEmail() : "")
            .setHeader("Email");
        var telefono = grid.addColumn(genitore -> genitore.getUser() != null ? genitore.getUser().getTelefono() : "")
            .setHeader("Telefono");
        var figli = grid.addColumn(GenitoriView::nomiFigli).setHeader("Figli");
        grid.addColumn(genitore -> genitore.getFigli().size()).setHeader("N. Figli").setWidth("6.5rem").setFlexGrow(0);
        ViewSupport.configureActionsColumn(grid.addComponentColumn(genitore -> ViewSupport.actions(
            ViewSupport.iconButton(VaadinIcon.PENCIL, "Modifica", e -> openDialog(genitore)))));
        ViewSupport.hideOnMobile(grid, email, telefono, figli);

        Button addButton = new Button("Nuovo Genitore", e -> openDialog(new Genitore()));
        add(addButton, grid);
        expand(grid);
        updateGrid();
    }

    private void openDialog(Genitore genitore) {
        boolean nuovo = genitore.getId() == null;
        Dialog dialog = ViewSupport.dialog(nuovo ? "Nuovo Genitore" : "Modifica Genitore", "700px");

        User user = nuovo ? new User() : genitore.getUser();
        UtenteFields utente = new UtenteFields(user, userService);
        Binder<Genitore> binder = new Binder<>(Genitore.class);
        AnagraficaFields<Genitore> anagrafica = new AnagraficaFields<>(binder, false);
        binder.readBean(genitore);

        VerticalLayout content = new VerticalLayout(
            ViewSupport.sezione("Utente e contatti"), new FormLayout(utente.getComponents()),
            ViewSupport.sezione("Dati anagrafici"), new FormLayout(anagrafica.getComponents()));
        content.setPadding(false);

        Button saveButton = new Button("Salva", e -> {
            // Valida tutto prima di salvare: niente utenti creati per un genitore che poi non si salva
            boolean valido = binder.validate().isOk() & utente.validate();
            if (!valido || !binder.writeBeanIfValid(genitore) || !utente.writeBeanIfValid(user)) {
                return;
            }
            try {
                genitoreService.salva(genitore, user, utente.getPassword());
                updateGrid();
                dialog.close();
                Notification.show("Genitore salvato con successo");
            } catch (RuntimeException ex) {
                Notification.show("Errore nel salvataggio: " + ex.getMessage());
            }
        });
        saveButton.addThemeVariants(ButtonVariant.PRIMARY);

        dialog.add(content);
        dialog.getFooter().add(ViewSupport.closeButton(dialog, "Annulla"), saveButton);
        dialog.open();
    }

    private void updateGrid() {
        grid.setItems(genitoreService.findAllWithFigli());
    }

    private static String nomiFigli(Genitore genitore) {
        return genitore.getFigli().stream()
            .map(Studente::getNomeCompleto)
            .sorted()
            .collect(Collectors.joining(", "));
    }
}
