package it.attocchi.views.admin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import it.attocchi.entity.Insegnante;
import it.attocchi.entity.Corso;
import it.attocchi.entity.User;
import it.attocchi.security.SecurityService;
import it.attocchi.service.InsegnanteService;
import it.attocchi.service.UserService;
import it.attocchi.views.AnagraficaFields;
import it.attocchi.views.CardMobile;
import it.attocchi.views.Formati;
import it.attocchi.views.MainLayout;
import it.attocchi.views.RicercaRapida;
import it.attocchi.views.UtenteFields;
import it.attocchi.views.ViewSupport;
import jakarta.annotation.security.RolesAllowed;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

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
    /** Eliminare una scheda insegnante è riservato all'admin. */
    private final boolean puoEliminare;
    private final Grid<Insegnante> grid = new Grid<>(Insegnante.class, false);
    private final RicercaRapida ricerca = new RicercaRapida();

    public InsegnantiView(InsegnanteService insegnanteService, UserService userService, SecurityService securityService) {
        this.insegnanteService = insegnanteService;
        this.userService = userService;
        this.puoEliminare = securityService.hasRole("ADMIN");
        addClassNames("size-full");

        grid.addColumn(Insegnante::getNomeCompleto).setHeader("Nome").setSortable(true);
        grid.addColumn(Insegnante::getSpecializzazione).setHeader("Specializzazione").setSortable(true);
        grid.addColumn(insegnante -> insegnante.getUser() != null ? insegnante.getUser().getEmail() : "")
            .setHeader("Email");
        grid.addColumn(insegnante -> Formati.euro(insegnante.getTariffaOraria())).setHeader("Tariffa oraria")
            .setWidth("8rem").setFlexGrow(0);
        grid.addColumn(insegnante -> Formati.euro(insegnante.getRimborsoForfettario())).setHeader("Forfait annuo")
            .setWidth("8rem").setFlexGrow(0);
        grid.addColumn(insegnante -> insegnante.getNumeroCorsi()).setHeader("N. Corsi").setWidth("6.5rem").setFlexGrow(0);
        ViewSupport.configureActionsColumn(grid.addComponentColumn(this::createActions));
        CardMobile.per(grid)
            .titolo(Insegnante::getNomeCompleto)
            .sottotitolo(Insegnante::getSpecializzazione)
            .campo("Email", insegnante -> insegnante.getUser() != null ? insegnante.getUser().getEmail() : null)
            .campo("Tariffa oraria", insegnante -> Formati.euro(insegnante.getTariffaOraria()))
            .campo("Forfait annuo", insegnante -> Formati.euro(insegnante.getRimborsoForfettario()))
            .campo("N. Corsi", insegnante -> insegnante.getNumeroCorsi())
            .azioni(this::createActions)
            .applica();

        Button addButton = new Button("Nuovo Insegnante", e -> openDialog(new Insegnante()));
        HorizontalLayout toolbar = new HorizontalLayout(addButton, ricerca);
        toolbar.addClassNames("flex-wrap w-full items-center");
        add(toolbar, grid);
        grid.addClassNames("grow");
        updateGrid();
    }

    private HorizontalLayout createActions(Insegnante insegnante) {
        HorizontalLayout azioni = ViewSupport.actions(ViewSupport.iconButton(VaadinIcon.PENCIL, "Modifica", e -> openDialog(insegnante)));
        Button invita = InvitoUI.pulsante(insegnante.getUser(), this::updateGrid);
        if (invita != null) {
            azioni.add(invita);
        }
        if (puoEliminare) {
            Button elimina = ViewSupport.iconButton(VaadinIcon.TRASH, "Elimina", e -> confermaEliminazione(insegnante));
            elimina.addThemeVariants(ButtonVariant.ERROR);
            azioni.add(elimina);
        }
        return azioni;
    }

    /** Conferma (o motivo del blocco): si elimina solo la scheda, l'utente resta senza il ruolo INSEGNANTE. */
    private void confermaEliminazione(Insegnante insegnante) {
        String motivo = insegnanteService.motivoPerNonEliminare(insegnante.getId());
        if (motivo != null) {
            Notification.show("Impossibile eliminare: " + insegnante.getNomeCompleto() + " " + motivo);
            return;
        }
        boolean unicoRuolo = insegnante.getUser() != null && insegnante.getUser().getRoles().equals(Set.of("INSEGNANTE"));
        Dialog conferma = ViewSupport.dialog("Eliminare la scheda insegnante?", "460px");
        conferma.add(new Span(insegnante.getNomeCompleto() + ": si elimina la scheda insegnante, non l'utente, che perde il"
            + " ruolo INSEGNANTE." + (unicoRuolo ? " L'utente non avrà più ruoli: se serve, eliminalo da Gestione Utenti." : "")));
        Button elimina = new Button("Elimina", e -> {
            try {
                insegnanteService.elimina(insegnante.getId());
                conferma.close();
                updateGrid();
                Notification.show("Scheda insegnante eliminata");
            } catch (RuntimeException ex) {
                Notification.show("Impossibile eliminare la scheda: " + ex.getMessage());
            }
        });
        elimina.addThemeVariants(ButtonVariant.PRIMARY, ButtonVariant.ERROR);
        conferma.getFooter().add(ViewSupport.closeButton(conferma, "Annulla"), elimina);
        conferma.open();
    }

    private void openDialog(Insegnante insegnante) {
        boolean nuovo = insegnante.getId() == null;
        Dialog dialog = ViewSupport.dialog(nuovo ? "Nuovo Insegnante" : "Modifica Insegnante", "700px");

        User user = nuovo ? new User() : insegnante.getUser();
        UtenteFields utente = new UtenteFields(user, userService, true);

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
        BigDecimalField forfait = new BigDecimalField("Rimborso forfettario annuo");
        forfait.setLocale(Locale.ITALY);
        forfait.setPrefixComponent(new Span("€"));
        forfait.setSuffixComponent(new Span("/anno"));
        forfait.setClearButtonVisible(true);
        forfait.setHelperText("Facoltativo: per ogni anno scolastico in cui ha corsi, si somma al costo delle lezioni");
        binder.forField(forfait).withValidator(f -> f == null || f.signum() >= 0, "Il rimborso non può essere negativo")
            .bind(Insegnante::getRimborsoForfettario, Insegnante::setRimborsoForfettario);
        binder.forField(biografia).withNullRepresentation("").bind(Insegnante::getBiografia, Insegnante::setBiografia);
        AnagraficaFields<Insegnante> anagrafica = new AnagraficaFields<>(binder, false);
        binder.readBean(insegnante);

        FormLayout insegnamento = new FormLayout(specializzazione, tariffa, forfait, biografia);
        insegnamento.setColspan(biografia, 2);
        VerticalLayout content = new VerticalLayout(
            ViewSupport.sezione("Utente e contatti"), new FormLayout(utente.getComponents()),
            ViewSupport.sezione("Insegnamento"), insegnamento,
            ViewSupport.sezione("Dati anagrafici"), new FormLayout(anagrafica.getComponents()));
        content.addClassNames("p-0");

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
        ricerca.collega(grid.setItems(insegnanteService.findAllWithCorsi()), insegnante -> RicercaRapida.testo(
            RicercaRapida.testo(insegnante.getUser()), RicercaRapida.testo(insegnante), insegnante.getSpecializzazione(),
            insegnante.getTuttiICorsi().stream().map(Corso::getNome).collect(Collectors.joining(" "))));
    }
}
