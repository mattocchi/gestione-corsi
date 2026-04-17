package it.attocchi.views.admin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import it.attocchi.entity.User;
import it.attocchi.service.UserService;
import it.attocchi.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;

import java.util.Arrays;

@Route(value = "utenti", layout = MainLayout.class)
@PageTitle("Gestione Utenti")
@RolesAllowed("ADMIN")
public class UtentiView extends VerticalLayout {

    private final UserService userService;
    private final Grid<User> grid = new Grid<>(User.class, false);

    public UtentiView(UserService userService) {
        this.userService = userService;

        setSizeFull();
        configureGrid();

        Button addButton = new Button("Nuovo Utente", e -> openDialog(new User()));
        add(addButton, grid);

        updateGrid();
    }

    private void configureGrid() {
        grid.addColumn(User::getUsername).setHeader("Username").setSortable(true);
        grid.addColumn(User::getNome).setHeader("Nome").setSortable(true);
        grid.addColumn(User::getCognome).setHeader("Cognome").setSortable(true);
        grid.addColumn(User::getEmail).setHeader("Email");
        grid.addColumn(user -> String.join(", ", user.getRoles())).setHeader("Ruoli");

        grid.addComponentColumn(user -> {
            Button editButton = new Button("Modifica", e -> openDialog(user));
            Button deleteButton = new Button("Elimina", e -> deleteUser(user));
            deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
            return new HorizontalLayout(editButton, deleteButton);
        }).setHeader("Azioni");
    }

    private void openDialog(User user) {
        Dialog dialog = new Dialog();
        dialog.setWidth("500px");

        FormLayout formLayout = new FormLayout();
        Binder<User> binder = new Binder<>(User.class);

        TextField username = new TextField("Username");
        TextField password = new TextField("Password");
        TextField nome = new TextField("Nome");
        TextField cognome = new TextField("Cognome");
        TextField email = new TextField("Email");
        TextField telefono = new TextField("Telefono");

        CheckboxGroup<String> roles = new CheckboxGroup<>();
        roles.setLabel("Ruoli");
        roles.setItems(Arrays.asList("ADMIN", "DOCENTE", "STUDENTE", "GENITORE"));

        if (user.getId() != null) {
            password.setPlaceholder("Lascia vuoto per non modificare");
        }

        binder.forField(username).bind(User::getUsername, User::setUsername);
        binder.forField(nome).bind(User::getNome, User::setNome);
        binder.forField(cognome).bind(User::getCognome, User::setCognome);
        binder.forField(email).bind(User::getEmail, User::setEmail);
        binder.forField(telefono).bind(User::getTelefono, User::setTelefono);
        binder.forField(roles).bind(User::getRoles, User::setRoles);

        binder.readBean(user);

        formLayout.add(username, password, nome, cognome, email, telefono, roles);

        Button saveButton = new Button("Salva", e -> {
            try {
                binder.writeBean(user);
                if (!password.isEmpty()) {
                    user.setPassword(password.getValue());
                }
                userService.save(user);
                updateGrid();
                dialog.close();
                Notification.show("Utente salvato con successo");
            } catch (Exception ex) {
                Notification.show("Errore nel salvataggio: " + ex.getMessage());
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Annulla", e -> dialog.close());

        dialog.add(new VerticalLayout(formLayout, new HorizontalLayout(saveButton, cancelButton)));
        dialog.open();
    }

    private void deleteUser(User user) {
        userService.delete(user);
        updateGrid();
        Notification.show("Utente eliminato");
    }

    private void updateGrid() {
        grid.setItems(userService.findAll());
    }
}
