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
import it.attocchi.entity.Luogo;
import it.attocchi.security.SecurityService;
import it.attocchi.service.LuogoService;
import it.attocchi.views.CardMobile;
import it.attocchi.views.ViewSupport;

import java.util.Set;

/**
 * Luoghi di corsi e lezioni (Aula 1, Palestra, Esterno…): admin e segreteria li aggiungono, rinominano e
 * attivano/disattivano; solo l'admin elimina, e solo i luoghi che nessun corso o lezione usa.
 */
public class LuoghiScheda extends VerticalLayout {

    private final LuogoService luogoService;
    private final boolean puoEliminare;
    private final Grid<Luogo> grid = new Grid<>(Luogo.class, false);
    private Set<Long> usati = Set.of();

    public LuoghiScheda(LuogoService luogoService, SecurityService securityService) {
        this.luogoService = luogoService;
        this.puoEliminare = securityService.hasRole("ADMIN");
        addClassNames("size-full");

        grid.addColumn(Luogo::getNome).setHeader("Luogo").setSortable(true);
        grid.addComponentColumn(this::createStatoBadge).setHeader("Stato").setWidth("8.5rem").setFlexGrow(0);
        grid.addColumn(this::usato).setHeader("Usato").setWidth("7rem").setFlexGrow(0);
        ViewSupport.configureActionsColumn(grid.addComponentColumn(this::createAzioni));
        grid.setEmptyStateText("Nessun luogo: aggiungi le aule e gli spazi in cui si svolgono i corsi");
        CardMobile.per(grid)
            .titolo(Luogo::getNome)
            .stato(this::createStatoBadge)
            .campo("Usato", this::usato)
            .azioni(this::createAzioni)
            .applica();

        Button addButton = new Button("Nuovo Luogo", e -> openDialog(new Luogo()));
        add(addButton, grid);
        grid.addClassNames("grow");
        updateGrid();
    }

    private String usato(Luogo luogo) {
        return usati.contains(luogo.getId()) ? "Sì" : "No";
    }

    private Badge createStatoBadge(Luogo luogo) {
        Badge badge = luogo.isAttivo()
            ? new Badge("Attivo", VaadinIcon.CHECK.create())
            : new Badge("Non attivo", VaadinIcon.BAN.create());
        if (luogo.isAttivo()) {
            badge.addThemeVariants(BadgeVariant.SUCCESS);
        }
        return badge;
    }

    private HorizontalLayout createAzioni(Luogo luogo) {
        Button editButton = ViewSupport.iconButton(VaadinIcon.PENCIL, "Modifica", e -> openDialog(luogo));
        Button attivaButton = luogo.isAttivo()
            ? ViewSupport.iconButton(VaadinIcon.EYE_SLASH, "Disattiva", e -> impostaAttivo(luogo, false))
            : ViewSupport.iconButton(VaadinIcon.EYE, "Attiva", e -> impostaAttivo(luogo, true));
        HorizontalLayout actions = ViewSupport.actions(editButton, attivaButton);
        if (puoEliminare) {
            boolean usato = usati.contains(luogo.getId());
            Button deleteButton = ViewSupport.iconButton(VaadinIcon.TRASH,
                usato ? "Usato da corsi o lezioni: non si può eliminare" : "Elimina", e -> elimina(luogo));
            deleteButton.setAriaLabel("Elimina");
            deleteButton.addThemeVariants(ButtonVariant.ERROR);
            deleteButton.setEnabled(!usato);
            actions.add(deleteButton);
        }
        return actions;
    }

    private void openDialog(Luogo luogo) {
        Dialog dialog = ViewSupport.dialog(luogo.getId() == null ? "Nuovo Luogo" : "Modifica Luogo", "400px");

        TextField nome = new TextField("Nome");
        nome.setPlaceholder("Aula 1");
        nome.setMaxLength(Luogo.LUNGHEZZA_NOME);
        nome.addClassNames("w-full");

        Binder<Luogo> binder = new Binder<>(Luogo.class);
        binder.forField(nome).asRequired("Inserisci il nome del luogo").bind(Luogo::getNome, Luogo::setNome);
        binder.readBean(luogo);

        Button saveButton = new Button("Salva", e -> {
            if (!binder.writeBeanIfValid(luogo)) {
                return;
            }
            try {
                luogoService.salva(luogo);
            } catch (IllegalArgumentException ex) {
                nome.setErrorMessage(ex.getMessage());
                nome.setInvalid(true);
                return;
            }
            updateGrid();
            dialog.close();
            Notification.show("Luogo salvato");
        });
        saveButton.addThemeVariants(ButtonVariant.PRIMARY);

        dialog.add(nome);
        dialog.getFooter().add(ViewSupport.closeButton(dialog, "Annulla"), saveButton);
        dialog.open();
    }

    private void impostaAttivo(Luogo luogo, boolean attivo) {
        luogoService.impostaAttivo(luogo.getId(), attivo);
        updateGrid();
    }

    private void elimina(Luogo luogo) {
        try {
            luogoService.elimina(luogo.getId());
            updateGrid();
            Notification.show("Luogo eliminato");
        } catch (RuntimeException ex) {
            Notification.show("Impossibile eliminare: " + ex.getMessage());
        }
    }

    private void updateGrid() {
        usati = luogoService.usati();
        grid.setItems(luogoService.elenco());
    }
}
