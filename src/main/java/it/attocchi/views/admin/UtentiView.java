package it.attocchi.views.admin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import it.attocchi.entity.User;
import it.attocchi.service.SchedeRuoliService;
import it.attocchi.service.UserService;
import it.attocchi.views.CardMobile;
import it.attocchi.views.MainLayout;
import it.attocchi.views.RicercaRapida;
import it.attocchi.views.ViewSupport;
import jakarta.annotation.security.RolesAllowed;

import java.util.Arrays;

@Route(value = "utenti", layout = MainLayout.class)
@PageTitle("Gestione Utenti")
@Menu(title = "Gestione Utenti", order = 12, icon = "vaadin:users")
@RolesAllowed("ADMIN")
public class UtentiView extends VerticalLayout {

    private final UserService userService;
    private final SchedeRuoliService schedeRuoliService;
    private final Grid<User> grid = new Grid<>(User.class, false);
    private final RicercaRapida ricerca = new RicercaRapida();

    public UtentiView(UserService userService, SchedeRuoliService schedeRuoliService) {
        this.userService = userService;
        this.schedeRuoliService = schedeRuoliService;

        addClassNames("size-full");
        configureGrid();

        Button addButton = new Button("Nuovo Utente", e -> openDialog(new User()));
        HorizontalLayout toolbar = new HorizontalLayout(addButton, ricerca);
        toolbar.addClassNames("flex-wrap w-full items-center");
        add(toolbar, grid);
        grid.addClassNames("grow");

        updateGrid();
    }

    private void configureGrid() {
        grid.addColumn(User::getUsername).setHeader("Username").setSortable(true);
        grid.addColumn(User::getNome).setHeader("Nome").setSortable(true);
        grid.addColumn(User::getCognome).setHeader("Cognome").setSortable(true);
        grid.addColumn(User::getEmail).setHeader("Email");
        grid.addColumn(user -> String.join(", ", user.getRoles())).setHeader("Ruoli");

        ViewSupport.configureActionsColumn(grid.addComponentColumn(this::createActions));

        CardMobile.per(grid)
            .titolo(User::getUsername)
            .campo("Nome", User::getNome)
            .campo("Cognome", User::getCognome)
            .campo("Email", User::getEmail)
            .campo("Ruoli", user -> String.join(", ", user.getRoles()))
            .azioni(this::createActions)
            .applica();
    }

    private HorizontalLayout createActions(User user) {
        Button editButton = ViewSupport.iconButton(VaadinIcon.PENCIL, "Modifica", e -> openDialog(user));
        Button deleteButton = ViewSupport.iconButton(VaadinIcon.TRASH, "Elimina", e -> deleteUser(user));
        deleteButton.addThemeVariants(ButtonVariant.ERROR);
        return ViewSupport.actions(editButton, deleteButton);
    }

    private void openDialog(User user) {
        Dialog dialog = ViewSupport.dialog(user.getId() == null ? "Nuovo Utente" : "Modifica Utente", "600px");

        FormLayout formLayout = new FormLayout();
        Binder<User> binder = new Binder<>(User.class);

        TextField username = new TextField("Username");
        PasswordField password = new PasswordField("Password");
        TextField nome = new TextField("Nome");
        TextField cognome = new TextField("Cognome");
        TextField email = new TextField("Email");
        TextField telefono = new TextField("Telefono");

        Checkbox attivo = new Checkbox("Attivo");
        attivo.setTooltipText("Solo un utente attivo può accedere all'app");

        CheckboxGroup<String> roles = new CheckboxGroup<>();
        roles.setLabel("Ruoli");
        roles.setItems(Arrays.asList("ADMIN", "SEGRETERIA", "INSEGNANTE", "ALLIEVO", "GENITORE"));

        if (user.getId() != null) {
            password.setPlaceholder("Lascia vuoto per non modificare");
        }

        binder.forField(username).bind(User::getUsername, User::setUsername);
        binder.forField(nome).bind(User::getNome, User::setNome);
        binder.forField(cognome).bind(User::getCognome, User::setCognome);
        binder.forField(email).bind(User::getEmail, User::setEmail);
        binder.forField(telefono).bind(User::getTelefono, User::setTelefono);
        binder.forField(attivo).bind(User::isEnabled, User::setEnabled);
        binder.forField(roles).bind(User::getRoles, User::setRoles);

        binder.readBean(user);

        formLayout.add(username, password, nome, cognome, email, telefono, attivo, roles);

        Button saveButton = new Button("Salva", e -> {
            try {
                binder.writeBean(user);
                // Con i ruoli Insegnante o Genitore crea anche la scheda che manca, così compare in quegli elenchi
                schedeRuoliService.salvaUtente(user, password.getValue());
                updateGrid();
                dialog.close();
                Notification.show("Utente salvato con successo");
            } catch (Exception ex) {
                Notification.show("Errore nel salvataggio: " + ex.getMessage());
            }
        });
        saveButton.addThemeVariants(ButtonVariant.PRIMARY);

        dialog.add(formLayout);
        dialog.getFooter().add(ViewSupport.closeButton(dialog, "Annulla"), saveButton);
        dialog.open();
    }

    private void deleteUser(User user) {
        userService.delete(user);
        updateGrid();
        Notification.show("Utente eliminato");
    }

    private void updateGrid() {
        ricerca.collega(grid.setItems(userService.findAll()), RicercaRapida::testo);
    }
}
