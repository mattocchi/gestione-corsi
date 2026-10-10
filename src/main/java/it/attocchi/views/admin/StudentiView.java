package it.attocchi.views.admin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
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
import it.attocchi.entity.Genitore;
import it.attocchi.entity.Studente;
import it.attocchi.entity.User;
import it.attocchi.security.SecurityService;
import it.attocchi.service.GenitoreService;
import it.attocchi.service.StudenteService;
import it.attocchi.service.UserService;
import it.attocchi.views.Formati;
import it.attocchi.views.MainLayout;
import it.attocchi.views.ViewSupport;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "studenti", layout = MainLayout.class)
@PageTitle("Gestione Studenti")
@Menu(title = "Gestione Studenti", order = 11, icon = "vaadin:academy-cap")
@RolesAllowed({"ADMIN", "SEGRETERIA"})
public class StudentiView extends VerticalLayout {

    private final StudenteService studenteService;
    private final UserService userService;
    private final GenitoreService genitoreService;
    /** L'eliminazione degli studenti è riservata all'admin. */
    private final boolean puoEliminare;

    private final Grid<Studente> grid = new Grid<>(Studente.class, false);

    public StudentiView(StudenteService studenteService, UserService userService, GenitoreService genitoreService,
                        SecurityService securityService) {
        this.studenteService = studenteService;
        this.userService = userService;
        this.genitoreService = genitoreService;
        this.puoEliminare = securityService.hasRole("ADMIN");

        setSizeFull();
        configureGrid();

        Button addButton = new Button("Nuovo Studente", e -> openDialog(new Studente()));
        add(addButton, grid);
        expand(grid);

        updateGrid();
    }

    private void configureGrid() {
        grid.addColumn(Studente::getNomeCompleto).setHeader("Nome").setSortable(true);
        var codiceFiscale = grid.addColumn(Studente::getCodiceFiscale).setHeader("Codice Fiscale");
        var dataNascita = grid.addColumn(studente -> Formati.data(studente.getDataNascita())).setHeader("Data Nascita")
            .setComparator(Studente::getDataNascita).setAutoWidth(true).setFlexGrow(0);
        var genitore = grid.addColumn(studente -> studente.getGenitore() != null ? studente.getGenitore().getNomeCompleto() : "")
            .setHeader("Genitore");
        grid.addColumn(studente -> studente.getCorsi().size()).setHeader("N. Corsi").setAutoWidth(true).setFlexGrow(0);

        ViewSupport.configureActionsColumn(grid.addComponentColumn(studente -> {
            Button editButton = ViewSupport.iconButton(VaadinIcon.PENCIL, "Modifica", e -> openDialog(studente));
            HorizontalLayout actions = ViewSupport.actions(editButton);
            if (puoEliminare) {
                Button deleteButton = ViewSupport.iconButton(VaadinIcon.TRASH, "Elimina", e -> deleteStudente(studente));
                deleteButton.addThemeVariants(ButtonVariant.ERROR);
                actions.add(deleteButton);
            }
            return actions;
        }));

        ViewSupport.hideOnMobile(grid, codiceFiscale, dataNascita, genitore);
    }

    private void openDialog(Studente studente) {
        boolean nuovo = studente.getUser() == null;
        Dialog dialog = ViewSupport.dialog(nuovo ? "Nuovo Studente" : "Modifica Studente", "600px");

        FormLayout formLayout = new FormLayout();
        Binder<Studente> binder = new Binder<>(Studente.class);
        // Nome, cognome e credenziali sono dell'utente collegato allo studente
        User user = nuovo ? new User() : studente.getUser();
        Binder<User> userBinder = new Binder<>(User.class);

        TextField username = new TextField("Username");
        PasswordField password = new PasswordField("Password");
        TextField nome = new TextField("Nome");
        TextField cognome = new TextField("Cognome");
        TextField email = new TextField("Email");
        TextField codiceFiscale = new TextField("Codice Fiscale");
        DatePicker dataNascita = new DatePicker("Data Nascita");
        TextField luogoNascita = new TextField("Luogo Nascita");
        TextField indirizzo = new TextField("Indirizzo");
        TextField citta = new TextField("Città");

        ComboBox<Genitore> genitore = new ComboBox<>("Genitore");
        genitore.setItems(genitoreService.findAll());
        genitore.setItemLabelGenerator(Genitore::getNomeCompleto);

        userBinder.forField(username).asRequired("Inserisci lo username")
            .withValidator(u -> !nuovo || !userService.existsByUsername(u), "Username già in uso")
            .bind(User::getUsername, User::setUsername);
        userBinder.forField(nome).asRequired("Inserisci il nome").bind(User::getNome, User::setNome);
        userBinder.forField(cognome).asRequired("Inserisci il cognome").bind(User::getCognome, User::setCognome);
        userBinder.forField(email).bind(User::getEmail, User::setEmail);
        userBinder.readBean(user);
        if (nuovo) {
            password.setRequiredIndicatorVisible(true);
        } else {
            username.setReadOnly(true);
            password.setVisible(false);
        }

        binder.forField(codiceFiscale).asRequired("Inserisci il codice fiscale")
            .bind(Studente::getCodiceFiscale, Studente::setCodiceFiscale);
        binder.forField(dataNascita).asRequired("Inserisci la data di nascita")
            .bind(Studente::getDataNascita, Studente::setDataNascita);
        binder.forField(luogoNascita).bind(Studente::getLuogoNascita, Studente::setLuogoNascita);
        binder.forField(indirizzo).bind(Studente::getIndirizzo, Studente::setIndirizzo);
        binder.forField(citta).bind(Studente::getCitta, Studente::setCitta);
        binder.forField(genitore).asRequired("Scegli il genitore dello studente")
            .bind(Studente::getGenitore, Studente::setGenitore);

        binder.readBean(studente);

        formLayout.add(username, password, nome, cognome, email, codiceFiscale, dataNascita, luogoNascita, indirizzo, citta, genitore);

        Button saveButton = new Button("Salva", e -> {
            // Valida tutto prima di salvare: niente utenti creati per uno studente che poi non si salva
            boolean passwordOk = !nuovo || !password.isEmpty();
            password.setInvalid(!passwordOk);
            password.setErrorMessage(passwordOk ? null : "Inserisci la password");
            boolean valido = binder.validate().isOk() & userBinder.validate().isOk() & passwordOk;
            if (!valido || !binder.writeBeanIfValid(studente) || !userBinder.writeBeanIfValid(user)) {
                return;
            }
            try {
                if (nuovo) {
                    user.getRoles().add("STUDENTE");
                    studente.setUser(userService.save(user, password.getValue()));
                } else {
                    userService.save(user, null);
                }
                studenteService.save(studente);
                updateGrid();
                dialog.close();
                Notification.show("Studente salvato con successo");
            } catch (Exception ex) {
                Notification.show("Errore nel salvataggio: " + ex.getMessage());
            }
        });
        saveButton.addThemeVariants(ButtonVariant.PRIMARY);

        dialog.add(formLayout);
        dialog.getFooter().add(ViewSupport.closeButton(dialog, "Annulla"), saveButton);
        dialog.open();
    }

    private void deleteStudente(Studente studente) {
        try {
            studenteService.delete(studente);
            updateGrid();
            Notification.show("Studente eliminato");
        } catch (RuntimeException ex) {
            Notification.show("Impossibile eliminare lo studente: " + ex.getMessage());
        }
    }

    private void updateGrid() {
        grid.setItems(studenteService.findAll());
    }
}
