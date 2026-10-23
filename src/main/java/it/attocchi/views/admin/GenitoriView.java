package it.attocchi.views.admin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.grid.Grid;
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
import it.attocchi.service.AllievoService;
import it.attocchi.service.GenitoreService;
import it.attocchi.service.UserService;
import it.attocchi.views.AnagraficaFields;
import it.attocchi.views.CardMobile;
import it.attocchi.views.MainLayout;
import it.attocchi.views.RicercaRapida;
import it.attocchi.views.UtenteFields;
import it.attocchi.views.ViewSupport;
import jakarta.annotation.security.RolesAllowed;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
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
    private final AllievoService allievoService;
    /** Eliminare una scheda genitore è riservato all'admin. */
    private final boolean puoEliminare;
    private final Grid<Genitore> grid = new Grid<>(Genitore.class, false);
    private final RicercaRapida ricerca = new RicercaRapida();

    public GenitoriView(GenitoreService genitoreService, UserService userService, AllievoService allievoService,
                        SecurityService securityService) {
        this.genitoreService = genitoreService;
        this.userService = userService;
        this.allievoService = allievoService;
        this.puoEliminare = securityService.hasRole("ADMIN");
        addClassNames("size-full");

        grid.addColumn(Genitore::getNomeCompleto).setHeader("Nome").setSortable(true);
        grid.addColumn(genitore -> genitore.getUser() != null ? genitore.getUser().getEmail() : "")
            .setHeader("Email");
        grid.addColumn(genitore -> genitore.getUser() != null ? genitore.getUser().getTelefono() : "")
            .setHeader("Telefono");
        grid.addColumn(GenitoriView::nomiFigli).setHeader("Figli");
        grid.addColumn(genitore -> genitore.getFigli().size()).setHeader("N. Figli").setWidth("6.5rem").setFlexGrow(0);
        ViewSupport.configureActionsColumn(grid.addComponentColumn(this::createActions));
        CardMobile.per(grid)
            .titolo(Genitore::getNomeCompleto)
            .campo("Email", genitore -> genitore.getUser() != null ? genitore.getUser().getEmail() : null)
            .campo("Telefono", genitore -> genitore.getUser() != null ? genitore.getUser().getTelefono() : null)
            .campo("Figli", GenitoriView::nomiFigli)
            .azioni(this::createActions)
            .applica();

        Button addButton = new Button("Nuovo Genitore", e -> openDialog(new Genitore()));
        HorizontalLayout toolbar = new HorizontalLayout(addButton, ricerca);
        toolbar.addClassNames("flex-wrap w-full items-center");
        add(toolbar, grid);
        grid.addClassNames("grow");
        updateGrid();
    }

    private HorizontalLayout createActions(Genitore genitore) {
        HorizontalLayout azioni = ViewSupport.actions(ViewSupport.iconButton(VaadinIcon.PENCIL, "Modifica", e -> openDialog(genitore)));
        if (puoEliminare) {
            Button elimina = ViewSupport.iconButton(VaadinIcon.TRASH, "Elimina", e -> confermaEliminazione(genitore));
            elimina.addThemeVariants(ButtonVariant.ERROR);
            azioni.add(elimina);
        }
        return azioni;
    }

    /** Conferma: si elimina solo la scheda del genitore, l'utente resta (senza il ruolo GENITORE). */
    private void confermaEliminazione(Genitore genitore) {
        int figli = genitore.getFigli().size();
        boolean unicoRuolo = genitore.getUser() != null && genitore.getUser().getRoles().equals(Set.of("GENITORE"));
        Dialog conferma = ViewSupport.dialog("Eliminare la scheda genitore?", "460px");
        conferma.add(new Span(genitore.getNomeCompleto() + ": si elimina la scheda genitore, non l'utente, che perde il ruolo"
            + " GENITORE." + (figli > 0 ? " " + figli + (figli == 1 ? " figlio resta" : " figli restano") + " senza genitore." : "")
            + (unicoRuolo ? " L'utente non avrà più ruoli: se serve, eliminalo da Gestione Utenti." : "")));
        Button elimina = new Button("Elimina", e -> {
            try {
                genitoreService.elimina(genitore.getId());
                conferma.close();
                updateGrid();
                Notification.show("Scheda genitore eliminata");
            } catch (RuntimeException ex) {
                Notification.show("Impossibile eliminare la scheda: " + ex.getMessage());
            }
        });
        elimina.addThemeVariants(ButtonVariant.PRIMARY, ButtonVariant.ERROR);
        conferma.getFooter().add(ViewSupport.closeButton(conferma, "Annulla"), elimina);
        conferma.open();
    }

    private void openDialog(Genitore genitore) {
        boolean nuovo = genitore.getId() == null;
        Dialog dialog = ViewSupport.dialog(nuovo ? "Nuovo Genitore" : "Modifica Genitore", "700px");

        User user = nuovo ? new User() : genitore.getUser();
        UtenteFields utente = new UtenteFields(user, userService, true);
        Binder<Genitore> binder = new Binder<>(Genitore.class);
        AnagraficaFields<Genitore> anagrafica = new AnagraficaFields<>(binder, false);
        binder.readBean(genitore);

        // Figli: anche allievi di un altro genitore, che al salvataggio passano a questo
        MultiSelectComboBox<Allievo> figli = new MultiSelectComboBox<>("Figli");
        figli.setPlaceholder("Cerca un allievo");
        figli.setHelperText("Un allievo con un altro genitore passa a questo; togliendolo resta senza genitore");
        figli.setItems(allievoService.findAll().stream()
            .sorted(Comparator.comparing(Allievo::getNomeCompleto, String.CASE_INSENSITIVE_ORDER)).toList());
        figli.setItemLabelGenerator(allievo -> allievo.getGenitore() != null && !allievo.getGenitore().equals(genitore)
            ? allievo.getNomeCompleto() + " (figlio di " + allievo.getGenitore().getNomeCompleto() + ")"
            : allievo.getNomeCompleto());
        figli.setValue(nuovo ? Set.of() : Set.copyOf(allievoService.findByGenitore(genitore.getId())));
        figli.addClassNames("w-full");

        VerticalLayout content = new VerticalLayout(
            ViewSupport.sezione("Utente e contatti"), new FormLayout(utente.getComponents()),
            ViewSupport.sezione("Figli"), figli,
            ViewSupport.sezione("Dati anagrafici"), new FormLayout(anagrafica.getComponents()));
        content.addClassNames("p-0");

        Button saveButton = new Button("Salva", e -> {
            // Valida tutto prima di salvare: niente utenti creati per un genitore che poi non si salva
            boolean valido = binder.validate().isOk() & utente.validate();
            if (!valido || !binder.writeBeanIfValid(genitore) || !utente.writeBeanIfValid(user)) {
                return;
            }
            try {
                List<String> spostati = genitoreService.salvaConFigli(genitore, user, utente.getPassword(),
                    figli.getValue().stream().map(Allievo::getId).toList());
                updateGrid();
                dialog.close();
                Notification.show(spostati.isEmpty() ? "Genitore salvato con successo"
                    : "Genitore salvato. Spostati a questo genitore: " + String.join(", ", spostati));
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
        ricerca.collega(grid.setItems(genitoreService.findAllWithFigli()), genitore -> RicercaRapida.testo(
            RicercaRapida.testo(genitore.getUser()), RicercaRapida.testo(genitore), nomiFigli(genitore)));
    }

    private static String nomiFigli(Genitore genitore) {
        return genitore.getFigli().stream()
            .map(Allievo::getNomeCompleto)
            .sorted()
            .collect(Collectors.joining(", "));
    }
}
