package it.attocchi.views.admin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import it.attocchi.entity.Insegnante;
import it.attocchi.entity.User;
import it.attocchi.service.InsegnanteService;
import it.attocchi.service.UserService;
import it.attocchi.views.AnagraficaFields;
import it.attocchi.views.Formati;
import it.attocchi.views.MainLayout;
import it.attocchi.views.UtenteFields;
import it.attocchi.views.ViewSupport;
import jakarta.annotation.security.RolesAllowed;

import java.util.Locale;

/**
 * Insegnanti con il numero di corsi assegnati: admin e segreteria li creano e ne modificano
 * utente, anagrafica e tariffa oraria.
 */
@Route(value = "insegnanti", layout = MainLayout.class)
@PageTitle("Insegnanti")
@Menu(title = "Insegnanti", order = 13, icon = "vaadin:user-star")
@RolesAllowed({"ADMIN", "SEGRETERIA"})
public class InsegnantiView extends VerticalLayout {

    private final InsegnanteService insegnanteService;
    private final UserService userService;
    private final Grid<Insegnante> grid = new Grid<>(Insegnante.class, false);

    public InsegnantiView(InsegnanteService insegnanteService, UserService userService) {
        this.insegnanteService = insegnanteService;
        this.userService = userService;
        setSizeFull();

        grid.addColumn(Insegnante::getNomeCompleto).setHeader("Nome").setSortable(true);
        var specializzazione = grid.addColumn(Insegnante::getSpecializzazione).setHeader("Specializzazione").setSortable(true);
        var email = grid.addColumn(insegnante -> insegnante.getUser() != null ? insegnante.getUser().getEmail() : "")
            .setHeader("Email");
        var tariffa = grid.addColumn(insegnante -> Formati.euro(insegnante.getTariffaOraria())).setHeader("Tariffa oraria")
            .setWidth("8rem").setFlexGrow(0);
        grid.addColumn(insegnante -> insegnante.getCorsi().size()).setHeader("N. Corsi").setWidth("6.5rem").setFlexGrow(0);
        ViewSupport.configureActionsColumn(grid.addComponentColumn(insegnante -> ViewSupport.actions(
            ViewSupport.iconButton(VaadinIcon.PENCIL, "Modifica", e -> openDialog(insegnante)))));
        ViewSupport.hideOnMobile(grid, specializzazione, email, tariffa);

        Button addButton = new Button("Nuovo Insegnante", e -> openDialog(new Insegnante()));
        add(addButton, grid);
        expand(grid);
        updateGrid();
    }

    private void openDialog(Insegnante insegnante) {
        boolean nuovo = insegnante.getId() == null;
        Dialog dialog = ViewSupport.dialog(nuovo ? "Nuovo Insegnante" : "Modifica Insegnante", "700px");

        User user = nuovo ? new User() : insegnante.getUser();
        UtenteFields utente = new UtenteFields(user, userService);

        Binder<Insegnante> binder = new Binder<>(Insegnante.class);
        TextField specializzazione = new TextField("Specializzazione");
        BigDecimalField tariffa = new BigDecimalField("Tariffa oraria");
        tariffa.setLocale(Locale.ITALY);
        tariffa.setPrefixComponent(new Span("€"));
        tariffa.setSuffixComponent(new Span("/ora"));
        TextArea biografia = new TextArea("Biografia");
        binder.forField(specializzazione).withNullRepresentation("")
            .bind(Insegnante::getSpecializzazione, Insegnante::setSpecializzazione);
        binder.forField(tariffa).withValidator(t -> t == null || t.signum() >= 0, "La tariffa non può essere negativa")
            .bind(Insegnante::getTariffaOraria, Insegnante::setTariffaOraria);
        binder.forField(biografia).withNullRepresentation("").bind(Insegnante::getBiografia, Insegnante::setBiografia);
        AnagraficaFields<Insegnante> anagrafica = new AnagraficaFields<>(binder, false);
        binder.readBean(insegnante);

        FormLayout insegnamento = new FormLayout(specializzazione, tariffa, biografia);
        insegnamento.setColspan(biografia, 2);
        VerticalLayout content = new VerticalLayout(
            ViewSupport.sezione("Utente e contatti"), new FormLayout(utente.getComponents()),
            ViewSupport.sezione("Insegnamento"), insegnamento,
            ViewSupport.sezione("Dati anagrafici"), new FormLayout(anagrafica.getComponents()));
        content.setPadding(false);

        Button saveButton = new Button("Salva", e -> {
            // Valida tutto prima di salvare: niente utenti creati per un insegnante che poi non si salva
            boolean valido = binder.validate().isOk() & utente.validate();
            if (!valido || !binder.writeBeanIfValid(insegnante) || !utente.writeBeanIfValid(user)) {
                return;
            }
            try {
                insegnanteService.salva(insegnante, user, utente.getPassword());
                updateGrid();
                dialog.close();
                Notification.show("Insegnante salvato con successo");
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
        grid.setItems(insegnanteService.findAllWithCorsi());
    }
}
