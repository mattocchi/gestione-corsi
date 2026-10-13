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
import it.attocchi.entity.Allievo;
import it.attocchi.entity.User;
import it.attocchi.security.SecurityService;
import it.attocchi.service.GenitoreService;
import it.attocchi.service.AllievoService;
import it.attocchi.service.UserService;
import it.attocchi.views.AnagraficaFields;
import it.attocchi.views.Formati;
import it.attocchi.views.MainLayout;
import it.attocchi.views.UtenteFields;
import it.attocchi.views.ViewSupport;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "allievi", layout = MainLayout.class)
@PageTitle("Gestione Allievi")
@Menu(title = "Gestione Allievi", order = 11, icon = "vaadin:academy-cap")
@RolesAllowed({"ADMIN", "SEGRETERIA"})
public class AllieviView extends VerticalLayout {

    private final AllievoService allievoService;
    private final UserService userService;
    private final GenitoreService genitoreService;
    /** L'eliminazione degli allievi è riservata all'admin. */
    private final boolean puoEliminare;

    private final Grid<Allievo> grid = new Grid<>(Allievo.class, false);

    public AllieviView(AllievoService allievoService, UserService userService, GenitoreService genitoreService,
                        SecurityService securityService) {
        this.allievoService = allievoService;
        this.userService = userService;
        this.genitoreService = genitoreService;
        this.puoEliminare = securityService.hasRole("ADMIN");

        setSizeFull();
        configureGrid();

        Button addButton = new Button("Nuovo Allievo", e -> openDialog(new Allievo()));
        add(addButton, grid);
        expand(grid);

        updateGrid();
    }

    private void configureGrid() {
        grid.addColumn(Allievo::getNomeCompleto).setHeader("Nome").setSortable(true);
        var codiceFiscale = grid.addColumn(Allievo::getCodiceFiscale).setHeader("Codice Fiscale");
        var dataNascita = grid.addColumn(allievo -> Formati.data(allievo.getDataNascita())).setHeader("Data Nascita")
            .setComparator(Allievo::getDataNascita).setAutoWidth(true).setFlexGrow(0);
        var genitore = grid.addColumn(allievo -> allievo.getGenitore() != null ? allievo.getGenitore().getNomeCompleto() : "")
            .setHeader("Genitore");
        var indirizzo = grid.addColumn(Formati::indirizzo).setHeader("Indirizzo");
        grid.addColumn(allievo -> allievo.getCorsi().size()).setHeader("N. Corsi").setAutoWidth(true).setFlexGrow(0);

        ViewSupport.configureActionsColumn(grid.addComponentColumn(allievo -> {
            Button editButton = ViewSupport.iconButton(VaadinIcon.PENCIL, "Modifica", e -> openDialog(allievo));
            HorizontalLayout actions = ViewSupport.actions(editButton);
            if (puoEliminare) {
                Button deleteButton = ViewSupport.iconButton(VaadinIcon.TRASH, "Elimina", e -> deleteAllievo(allievo));
                deleteButton.addThemeVariants(ButtonVariant.ERROR);
                actions.add(deleteButton);
            }
            return actions;
        }));

        ViewSupport.hideOnMobile(grid, codiceFiscale, dataNascita, genitore, indirizzo);
    }

    private void openDialog(Allievo allievo) {
        boolean nuovo = allievo.getId() == null;
        Dialog dialog = ViewSupport.dialog(nuovo ? "Nuovo Allievo" : "Modifica Allievo", "700px");

        // Nome, cognome, contatti e credenziali sono dell'utente collegato all'allievo
        User user = nuovo ? new User() : allievo.getUser();
        UtenteFields utente = new UtenteFields(user, userService, true);

        Binder<Allievo> binder = new Binder<>(Allievo.class);
        ComboBox<Genitore> genitore = new ComboBox<>("Genitore");
        genitore.setItems(genitoreService.findAll());
        genitore.setItemLabelGenerator(Genitore::getNomeCompleto);
        binder.forField(genitore).asRequired("Scegli il genitore dell'allievo")
            .bind(Allievo::getGenitore, Allievo::setGenitore);
        AnagraficaFields<Allievo> anagrafica = new AnagraficaFields<>(binder, true);
        binder.readBean(allievo);

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
            // Valida tutto prima di salvare: niente utenti creati per un allievo che poi non si salva
            boolean valido = binder.validate().isOk() & utente.validate();
            if (!valido || !binder.writeBeanIfValid(allievo) || !utente.writeBeanIfValid(user)) {
                return;
            }
            try {
                allievoService.salva(allievo, user, utente.getPassword());
                updateGrid();
                dialog.close();
                Notification.show("Allievo salvato con successo");
            } catch (RuntimeException ex) {
                Notification.show("Errore nel salvataggio: " + ex.getMessage());
            }
        });
        saveButton.addThemeVariants(ButtonVariant.PRIMARY);

        dialog.add(content);
        dialog.getFooter().add(ViewSupport.closeButton(dialog, "Annulla"), saveButton);
        dialog.open();
    }

    /** Indirizzo del genitore, mostrato quando l'allievo non ne ha uno proprio. */
    private static void mostraIndirizzoGenitore(Span span, Genitore genitore) {
        String indirizzo = genitore != null ? genitore.getIndirizzo().getFormattato() : "";
        span.setText("Se l'indirizzo è vuoto si mostra quello del genitore: " + indirizzo);
        span.setVisible(!indirizzo.isEmpty());
    }

    private void deleteAllievo(Allievo allievo) {
        try {
            allievoService.delete(allievo);
            updateGrid();
            Notification.show("Allievo eliminato");
        } catch (RuntimeException ex) {
            Notification.show("Impossibile eliminare l'allievo: " + ex.getMessage());
        }
    }

    private void updateGrid() {
        grid.setItems(allievoService.findAll());
    }
}
