package it.attocchi.views.admin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
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
import it.attocchi.entity.Genitore;
import it.attocchi.entity.Studente;
import it.attocchi.entity.User;
import it.attocchi.repository.GenitoreRepository;
import it.attocchi.service.StudenteService;
import it.attocchi.service.UserService;
import it.attocchi.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "studenti", layout = MainLayout.class)
@PageTitle("Gestione Studenti")
@RolesAllowed("ADMIN")
public class StudentiView extends VerticalLayout {

    private final StudenteService studenteService;
    private final UserService userService;
    private final GenitoreRepository genitoreRepository;

    private final Grid<Studente> grid = new Grid<>(Studente.class, false);

    public StudentiView(StudenteService studenteService, UserService userService, GenitoreRepository genitoreRepository) {
        this.studenteService = studenteService;
        this.userService = userService;
        this.genitoreRepository = genitoreRepository;

        setSizeFull();
        configureGrid();

        Button addButton = new Button("Nuovo Studente", e -> openDialog(new Studente()));
        add(addButton, grid);

        updateGrid();
    }

    private void configureGrid() {
        grid.addColumn(Studente::getNomeCompleto).setHeader("Nome").setSortable(true);
        grid.addColumn(Studente::getCodiceFiscale).setHeader("Codice Fiscale");
        grid.addColumn(Studente::getDataNascita).setHeader("Data Nascita");
        grid.addColumn(studente -> studente.getGenitore() != null ? studente.getGenitore().getNomeCompleto() : "")
            .setHeader("Genitore");
        grid.addColumn(studente -> studente.getCorsi().size()).setHeader("N. Corsi");

        grid.addComponentColumn(studente -> {
            Button editButton = new Button("Modifica", e -> openDialog(studente));
            Button deleteButton = new Button("Elimina", e -> deleteStudente(studente));
            deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
            return new HorizontalLayout(editButton, deleteButton);
        }).setHeader("Azioni");
    }

    private void openDialog(Studente studente) {
        Dialog dialog = new Dialog();
        dialog.setWidth("500px");

        FormLayout formLayout = new FormLayout();
        Binder<Studente> binder = new Binder<>(Studente.class);

        TextField username = new TextField("Username");
        TextField password = new TextField("Password");
        TextField nome = new TextField("Nome");
        TextField cognome = new TextField("Cognome");
        TextField email = new TextField("Email");
        TextField codiceFiscale = new TextField("Codice Fiscale");
        DatePicker dataNascita = new DatePicker("Data Nascita");
        TextField luogoNascita = new TextField("Luogo Nascita");
        TextField indirizzo = new TextField("Indirizzo");
        TextField citta = new TextField("Città");

        ComboBox<Genitore> genitore = new ComboBox<>("Genitore");
        genitore.setItems(genitoreRepository.findAll());
        genitore.setItemLabelGenerator(Genitore::getNomeCompleto);

        if (studente.getUser() != null) {
            username.setValue(studente.getUser().getUsername());
            nome.setValue(studente.getUser().getNome());
            cognome.setValue(studente.getUser().getCognome());
            email.setValue(studente.getUser().getEmail() != null ? studente.getUser().getEmail() : "");
            password.setVisible(false);
        }

        binder.forField(codiceFiscale).bind(Studente::getCodiceFiscale, Studente::setCodiceFiscale);
        binder.forField(dataNascita).bind(Studente::getDataNascita, Studente::setDataNascita);
        binder.forField(luogoNascita).bind(Studente::getLuogoNascita, Studente::setLuogoNascita);
        binder.forField(indirizzo).bind(Studente::getIndirizzo, Studente::setIndirizzo);
        binder.forField(citta).bind(Studente::getCitta, Studente::setCitta);
        binder.forField(genitore).bind(Studente::getGenitore, Studente::setGenitore);

        binder.readBean(studente);

        formLayout.add(username, password, nome, cognome, email, codiceFiscale, dataNascita, luogoNascita, indirizzo, citta, genitore);

        Button saveButton = new Button("Salva", e -> {
            try {
                binder.writeBean(studente);

                if (studente.getUser() == null) {
                    User user = new User();
                    user.setUsername(username.getValue());
                    user.setPassword(password.getValue());
                    user.setNome(nome.getValue());
                    user.setCognome(cognome.getValue());
                    user.setEmail(email.getValue());
                    user.getRoles().add("STUDENTE");
                    user = userService.save(user);
                    studente.setUser(user);
                } else {
                    User user = studente.getUser();
                    user.setNome(nome.getValue());
                    user.setCognome(cognome.getValue());
                    user.setEmail(email.getValue());
                    userService.save(user);
                }

                studenteService.save(studente);
                updateGrid();
                dialog.close();
                Notification.show("Studente salvato con successo");
            } catch (Exception ex) {
                Notification.show("Errore nel salvataggio: " + ex.getMessage());
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Annulla", e -> dialog.close());

        dialog.add(new VerticalLayout(formLayout, new HorizontalLayout(saveButton, cancelButton)));
        dialog.open();
    }

    private void deleteStudente(Studente studente) {
        studenteService.delete(studente);
        updateGrid();
        Notification.show("Studente eliminato");
    }

    private void updateGrid() {
        grid.setItems(studenteService.findAll());
    }
}
