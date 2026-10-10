package it.attocchi.views.admin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
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
import it.attocchi.views.AnagraficaFields;
import it.attocchi.views.Formati;
import it.attocchi.views.MainLayout;
import it.attocchi.views.UtenteFields;
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
        var indirizzo = grid.addColumn(Formati::indirizzo).setHeader("Indirizzo");
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

        ViewSupport.hideOnMobile(grid, codiceFiscale, dataNascita, genitore, indirizzo);
    }

    private void openDialog(Studente studente) {
        boolean nuovo = studente.getId() == null;
        Dialog dialog = ViewSupport.dialog(nuovo ? "Nuovo Studente" : "Modifica Studente", "700px");

        // Nome, cognome, contatti e credenziali sono dell'utente collegato allo studente
        User user = nuovo ? new User() : studente.getUser();
        UtenteFields utente = new UtenteFields(user, userService);

        Binder<Studente> binder = new Binder<>(Studente.class);
        ComboBox<Genitore> genitore = new ComboBox<>("Genitore");
        genitore.setItems(genitoreService.findAll());
        genitore.setItemLabelGenerator(Genitore::getNomeCompleto);
        binder.forField(genitore).asRequired("Scegli il genitore dello studente")
            .bind(Studente::getGenitore, Studente::setGenitore);
        AnagraficaFields<Studente> anagrafica = new AnagraficaFields<>(binder, true);
        binder.readBean(studente);

        Span indirizzoGenitore = new Span();
        indirizzoGenitore.addClassName("avviso-sola-lettura");
        genitore.addValueChangeListener(e -> mostraIndirizzoGenitore(indirizzoGenitore, e.getValue()));
        mostraIndirizzoGenitore(indirizzoGenitore, genitore.getValue());

        FormLayout utenteForm = new FormLayout(utente.getComponents());
        utenteForm.add(genitore);
        VerticalLayout content = new VerticalLayout(
            ViewSupport.sezione("Utente e genitore"), utenteForm,
            ViewSupport.sezione("Dati anagrafici"), new FormLayout(anagrafica.getComponents()), indirizzoGenitore);
        content.setPadding(false);

        Button saveButton = new Button("Salva", e -> {
            // Valida tutto prima di salvare: niente utenti creati per uno studente che poi non si salva
            boolean valido = binder.validate().isOk() & utente.validate();
            if (!valido || !binder.writeBeanIfValid(studente) || !utente.writeBeanIfValid(user)) {
                return;
            }
            try {
                studenteService.salva(studente, user, utente.getPassword());
                updateGrid();
                dialog.close();
                Notification.show("Studente salvato con successo");
            } catch (RuntimeException ex) {
                Notification.show("Errore nel salvataggio: " + ex.getMessage());
            }
        });
        saveButton.addThemeVariants(ButtonVariant.PRIMARY);

        dialog.add(content);
        dialog.getFooter().add(ViewSupport.closeButton(dialog, "Annulla"), saveButton);
        dialog.open();
    }

    /** Indirizzo del genitore, mostrato quando lo studente non ne ha uno proprio. */
    private static void mostraIndirizzoGenitore(Span span, Genitore genitore) {
        String indirizzo = genitore != null ? genitore.getIndirizzo().getFormattato() : "";
        span.setText("Se l'indirizzo è vuoto si mostra quello del genitore: " + indirizzo);
        span.setVisible(!indirizzo.isEmpty());
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
