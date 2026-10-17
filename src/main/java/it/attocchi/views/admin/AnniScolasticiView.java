package it.attocchi.views.admin;

import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import it.attocchi.entity.AnnoScolastico;
import it.attocchi.security.SecurityService;
import it.attocchi.service.AnnoScolasticoService;
import it.attocchi.views.CardMobile;
import it.attocchi.views.MainLayout;
import it.attocchi.views.ViewSupport;
import jakarta.annotation.security.RolesAllowed;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Elenco degli anni scolastici che si possono scegliere per i corsi: admin e segreteria li aggiungono
 * e li attivano/disattivano; solo l'admin elimina, e solo gli anni che nessun corso usa.
 */
@Route(value = "anni-scolastici", layout = MainLayout.class)
@PageTitle("Anni scolastici")
@Menu(title = "Anni scolastici", order = 15, icon = "vaadin:calendar-o")
@RolesAllowed({"ADMIN", "SEGRETERIA"})
public class AnniScolasticiView extends VerticalLayout {

    private final AnnoScolasticoService annoScolasticoService;
    private final boolean puoEliminare;
    private final Grid<AnnoScolastico> grid = new Grid<>(AnnoScolastico.class, false);
    private Set<String> usati = Set.of();

    public AnniScolasticiView(AnnoScolasticoService annoScolasticoService, SecurityService securityService) {
        this.annoScolasticoService = annoScolasticoService;
        this.puoEliminare = securityService.hasRole("ADMIN");
        addClassNames("size-full");

        grid.addColumn(AnnoScolastico::getNome).setHeader("Anno").setSortable(true);
        grid.addComponentColumn(this::createStatoBadge).setHeader("Stato").setWidth("8.5rem").setFlexGrow(0);
        grid.addColumn(anno -> usati.contains(anno.getNome()) ? "Sì" : "No").setHeader("Usato nei corsi")
            .setWidth("9rem").setFlexGrow(0);
        ViewSupport.configureActionsColumn(grid.addComponentColumn(this::createAzioni));
        CardMobile.per(grid)
            .titolo(AnnoScolastico::getNome)
            .stato(this::createStatoBadge)
            .campo("Usato nei corsi", anno -> usati.contains(anno.getNome()) ? "Sì" : "No")
            .azioni(this::createAzioni)
            .applica();

        Button addButton = new Button("Nuovo Anno Scolastico", e -> openDialog());
        add(addButton, grid);
        grid.addClassNames("grow");
        updateGrid();
    }

    private Badge createStatoBadge(AnnoScolastico anno) {
        Badge badge = anno.isAttivo()
            ? new Badge("Attivo", VaadinIcon.CHECK.create())
            : new Badge("Non attivo", VaadinIcon.BAN.create());
        if (anno.isAttivo()) {
            badge.addThemeVariants(BadgeVariant.SUCCESS);
        }
        return badge;
    }

    private HorizontalLayout createAzioni(AnnoScolastico anno) {
        Button attivaButton = anno.isAttivo()
            ? ViewSupport.iconButton(VaadinIcon.EYE_SLASH, "Disattiva", e -> impostaAttivo(anno, false))
            : ViewSupport.iconButton(VaadinIcon.EYE, "Attiva", e -> impostaAttivo(anno, true));
        HorizontalLayout actions = ViewSupport.actions(attivaButton);
        if (puoEliminare) {
            boolean usato = usati.contains(anno.getNome());
            Button deleteButton = ViewSupport.iconButton(VaadinIcon.TRASH, usato ? "Usato da almeno un corso: non si può eliminare" : "Elimina",
                e -> elimina(anno));
            deleteButton.setAriaLabel("Elimina");
            deleteButton.addThemeVariants(ButtonVariant.ERROR);
            deleteButton.setEnabled(!usato);
            actions.add(deleteButton);
        }
        return actions;
    }

    private void openDialog() {
        Dialog dialog = ViewSupport.dialog("Nuovo Anno Scolastico", "400px");

        TextField nome = new TextField("Anno Scolastico");
        nome.setPlaceholder("2026/2027");
        nome.setHelperText("Formato AAAA/AAAA, anni consecutivi");
        nome.addClassNames("w-full");
        nome.setValue(prossimoAnno());

        Binder<AnnoScolastico> binder = new Binder<>(AnnoScolastico.class);
        binder.forField(nome).asRequired("Inserisci l'anno scolastico")
            .withValidator(valore -> AnnoScolastico.valido(valore.strip()), "Usa il formato AAAA/AAAA con anni consecutivi, es. 2026/2027")
            .bind(AnnoScolastico::getNome, AnnoScolastico::setNome);

        Button saveButton = new Button("Salva", e -> {
            AnnoScolastico nuovo = new AnnoScolastico();
            if (!binder.writeBeanIfValid(nuovo)) {
                return;
            }
            try {
                annoScolasticoService.crea(nuovo.getNome());
            } catch (IllegalArgumentException ex) {
                nome.setErrorMessage(ex.getMessage());
                nome.setInvalid(true);
                return;
            }
            updateGrid();
            dialog.close();
            Notification.show("Anno scolastico aggiunto");
        });
        saveButton.addThemeVariants(ButtonVariant.PRIMARY);

        dialog.add(nome);
        dialog.getFooter().add(ViewSupport.closeButton(dialog, "Annulla"), saveButton);
        dialog.open();
    }

    /** Proposta per il nuovo anno: quello dopo il più recente dell'elenco (o l'anno corrente se l'elenco è vuoto). */
    private String prossimoAnno() {
        List<AnnoScolastico> anni = annoScolasticoService.elenco();
        if (anni.isEmpty() || !AnnoScolastico.valido(anni.getFirst().getNome())) {
            return AnnoScolastico.di(LocalDate.now());
        }
        int fine = Integer.parseInt(anni.getFirst().getNome().substring(5));
        return fine + "/" + (fine + 1);
    }

    private void impostaAttivo(AnnoScolastico anno, boolean attivo) {
        annoScolasticoService.impostaAttivo(anno.getId(), attivo);
        updateGrid();
    }

    private void elimina(AnnoScolastico anno) {
        try {
            annoScolasticoService.elimina(anno.getId());
            updateGrid();
            Notification.show("Anno scolastico eliminato");
        } catch (RuntimeException ex) {
            Notification.show("Impossibile eliminare: " + ex.getMessage());
        }
    }

    private void updateGrid() {
        usati = annoScolasticoService.usati();
        grid.setItems(annoScolasticoService.elenco());
    }
}
