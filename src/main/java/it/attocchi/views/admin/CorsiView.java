package it.attocchi.views.admin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import it.attocchi.entity.Corso;
import it.attocchi.entity.Insegnante;
import it.attocchi.security.SecurityService;
import it.attocchi.service.CorsoService;
import it.attocchi.service.InsegnanteService;
import it.attocchi.views.Formati;
import it.attocchi.views.MainLayout;
import it.attocchi.views.ViewSupport;
import jakarta.annotation.security.RolesAllowed;

import java.time.DayOfWeek;
import java.util.Arrays;

@Route(value = "corsi", layout = MainLayout.class)
@PageTitle("Gestione Corsi")
@Menu(title = "Gestione Corsi", order = 10, icon = "vaadin:music")
@RolesAllowed({"ADMIN", "SEGRETERIA"})
public class CorsiView extends VerticalLayout {

    private final CorsoService corsoService;
    private final InsegnanteService insegnanteService;
    /** L'eliminazione dei corsi è riservata all'admin; la segreteria li gestisce senza eliminarli. */
    private final boolean puoEliminare;

    private final Grid<Corso> grid = new Grid<>(Corso.class, false);

    public CorsiView(CorsoService corsoService, InsegnanteService insegnanteService, SecurityService securityService) {
        this.corsoService = corsoService;
        this.insegnanteService = insegnanteService;
        this.puoEliminare = securityService.hasRole("ADMIN");

        setSizeFull();
        configureGrid();

        Button addButton = new Button("Nuovo Corso", e -> openDialog(new Corso()));
        add(addButton, grid);
        expand(grid);

        updateGrid();
    }

    private void configureGrid() {
        grid.addColumn(Corso::getNome).setHeader("Nome").setSortable(true);
        var insegnante = grid.addColumn(corso -> corso.getInsegnante() != null ? corso.getInsegnante().getNomeCompleto() : "").setHeader("Insegnante");
        var dataInizio = grid.addColumn(corso -> Formati.data(corso.getDataInizio())).setHeader("Data Inizio")
            .setComparator(Corso::getDataInizio).setAutoWidth(true).setFlexGrow(0);
        var numeroLezioni = grid.addColumn(Corso::getNumeroLezioni).setHeader("N. Lezioni");
        var durata = grid.addColumn(Corso::getDurataLezioneMinuti).setHeader("Durata (min)");
        var numeroStudenti = grid.addColumn(corso -> corso.getStudenti().size()).setHeader("N. Studenti");

        ViewSupport.configureActionsColumn(grid.addComponentColumn(corso -> {
            Button editButton = ViewSupport.iconButton(VaadinIcon.PENCIL, "Modifica", e -> openDialog(corso));
            Button partecipantiButton = ViewSupport.iconButton(VaadinIcon.USERS, "Partecipanti",
                e -> new PartecipantiDialog(corsoService, corso).open());
            Button generateButton = ViewSupport.iconButton(VaadinIcon.REFRESH, "Genera Lezioni", e -> generaLezioni(corso));
            HorizontalLayout actions = ViewSupport.actions(editButton, partecipantiButton, generateButton);
            if (puoEliminare) {
                Button deleteButton = ViewSupport.iconButton(VaadinIcon.TRASH, "Elimina", e -> deleteCorso(corso));
                deleteButton.addThemeVariants(ButtonVariant.ERROR);
                actions.add(deleteButton);
            }
            return actions;
        }));

        ViewSupport.hideOnMobile(grid, insegnante, dataInizio, numeroLezioni, durata, numeroStudenti);
    }

    private void openDialog(Corso corso) {
        Dialog dialog = ViewSupport.dialog(corso.getId() == null ? "Nuovo Corso" : "Modifica Corso", "600px");

        FormLayout formLayout = new FormLayout();
        Binder<Corso> binder = new Binder<>(Corso.class);

        TextField nome = new TextField("Nome");
        TextArea descrizione = new TextArea("Descrizione");
        DatePicker dataInizio = new DatePicker("Data Inizio");
        IntegerField numeroLezioni = new IntegerField("Numero Lezioni");
        IntegerField durataLezioni = new IntegerField("Durata Lezione (minuti)");
        TimePicker orarioInizio = new TimePicker("Orario Inizio");

        CheckboxGroup<DayOfWeek> giorniSettimana = new CheckboxGroup<>();
        giorniSettimana.setLabel("Giorni Settimana");
        giorniSettimana.setItems(Arrays.asList(DayOfWeek.values()));
        giorniSettimana.setItemLabelGenerator(day -> {
            switch (day) {
                case MONDAY: return "Lunedì";
                case TUESDAY: return "Martedì";
                case WEDNESDAY: return "Mercoledì";
                case THURSDAY: return "Giovedì";
                case FRIDAY: return "Venerdì";
                case SATURDAY: return "Sabato";
                case SUNDAY: return "Domenica";
                default: return day.name();
            }
        });

        ComboBox<Insegnante> insegnante = new ComboBox<>("Insegnante");
        insegnante.setItems(insegnanteService.findAll());
        insegnante.setItemLabelGenerator(Insegnante::getNomeCompleto);

        binder.forField(nome).bind(Corso::getNome, Corso::setNome);
        binder.forField(descrizione).bind(Corso::getDescrizione, Corso::setDescrizione);
        binder.forField(dataInizio).bind(Corso::getDataInizio, Corso::setDataInizio);
        binder.forField(numeroLezioni).bind(Corso::getNumeroLezioni, Corso::setNumeroLezioni);
        binder.forField(durataLezioni).bind(Corso::getDurataLezioneMinuti, Corso::setDurataLezioneMinuti);
        binder.forField(orarioInizio).bind(Corso::getOrarioInizio, Corso::setOrarioInizio);
        binder.forField(giorniSettimana).bind(Corso::getGiorniSettimana, Corso::setGiorniSettimana);
        binder.forField(insegnante).bind(Corso::getInsegnante, Corso::setInsegnante);

        binder.readBean(corso);

        formLayout.add(nome, insegnante, dataInizio, numeroLezioni, durataLezioni, orarioInizio, giorniSettimana, descrizione);

        Button saveButton = new Button("Salva", e -> {
            try {
                binder.writeBean(corso);
                corsoService.save(corso);
                updateGrid();
                dialog.close();
                Notification.show("Corso salvato con successo");
            } catch (Exception ex) {
                Notification.show("Errore nel salvataggio: " + ex.getMessage());
            }
        });
        saveButton.addThemeVariants(ButtonVariant.PRIMARY);

        formLayout.setColspan(giorniSettimana, 2);
        formLayout.setColspan(descrizione, 2);

        dialog.add(formLayout);
        dialog.getFooter().add(ViewSupport.closeButton(dialog, "Annulla"), saveButton);
        dialog.open();
    }

    private void generaLezioni(Corso corso) {
        try {
            corsoService.generaLezioni(corso);
            Notification.show("Lezioni generate con successo");
        } catch (Exception e) {
            Notification.show("Errore nella generazione delle lezioni: " + e.getMessage());
        }
    }

    private void deleteCorso(Corso corso) {
        corsoService.delete(corso);
        updateGrid();
        Notification.show("Corso eliminato");
    }

    private void updateGrid() {
        grid.setItems(corsoService.findAll());
    }
}
