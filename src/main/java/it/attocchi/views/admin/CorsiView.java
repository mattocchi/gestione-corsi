package it.attocchi.views.admin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import it.attocchi.entity.Corso;
import it.attocchi.entity.Docente;
import it.attocchi.repository.DocenteRepository;
import it.attocchi.service.CorsoService;
import it.attocchi.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;

import java.time.DayOfWeek;
import java.util.Arrays;

@Route(value = "corsi", layout = MainLayout.class)
@PageTitle("Gestione Corsi")
@RolesAllowed("ADMIN")
public class CorsiView extends VerticalLayout {

    private final CorsoService corsoService;
    private final DocenteRepository docenteRepository;

    private final Grid<Corso> grid = new Grid<>(Corso.class, false);
    private final Binder<Corso> binder = new Binder<>(Corso.class);

    public CorsiView(CorsoService corsoService, DocenteRepository docenteRepository) {
        this.corsoService = corsoService;
        this.docenteRepository = docenteRepository;

        setSizeFull();
        configureGrid();

        Button addButton = new Button("Nuovo Corso", e -> openDialog(new Corso()));
        add(addButton, grid);

        updateGrid();
    }

    private void configureGrid() {
        grid.addColumn(Corso::getNome).setHeader("Nome").setSortable(true);
        grid.addColumn(corso -> corso.getDocente() != null ? corso.getDocente().getNomeCompleto() : "").setHeader("Docente");
        grid.addColumn(Corso::getDataInizio).setHeader("Data Inizio");
        grid.addColumn(Corso::getNumeroLezioni).setHeader("N. Lezioni");
        grid.addColumn(Corso::getDurataLezioneMinuti).setHeader("Durata (min)");
        grid.addColumn(corso -> corso.getStudenti().size()).setHeader("N. Studenti");

        grid.addComponentColumn(corso -> {
            Button editButton = new Button("Modifica", e -> openDialog(corso));
            Button generateButton = new Button("Genera Lezioni", e -> generaLezioni(corso));
            generateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            Button deleteButton = new Button("Elimina", e -> deleteCorso(corso));
            deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
            return new HorizontalLayout(editButton, generateButton, deleteButton);
        }).setHeader("Azioni");
    }

    private void openDialog(Corso corso) {
        Dialog dialog = new Dialog();
        dialog.setWidth("600px");

        FormLayout formLayout = new FormLayout();

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

        ComboBox<Docente> docente = new ComboBox<>("Docente");
        docente.setItems(docenteRepository.findAll());
        docente.setItemLabelGenerator(Docente::getNomeCompleto);

        binder.forField(nome).bind(Corso::getNome, Corso::setNome);
        binder.forField(descrizione).bind(Corso::getDescrizione, Corso::setDescrizione);
        binder.forField(dataInizio).bind(Corso::getDataInizio, Corso::setDataInizio);
        binder.forField(numeroLezioni).bind(Corso::getNumeroLezioni, Corso::setNumeroLezioni);
        binder.forField(durataLezioni).bind(Corso::getDurataLezioneMinuti, Corso::setDurataLezioneMinuti);
        binder.forField(orarioInizio).bind(Corso::getOrarioInizio, Corso::setOrarioInizio);
        binder.forField(giorniSettimana).bind(Corso::getGiorniSettimana, Corso::setGiorniSettimana);
        binder.forField(docente).bind(Corso::getDocente, Corso::setDocente);

        binder.readBean(corso);

        formLayout.add(nome, docente, dataInizio, numeroLezioni, durataLezioni, orarioInizio, giorniSettimana, descrizione);

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
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Annulla", e -> dialog.close());

        dialog.add(new VerticalLayout(formLayout, new HorizontalLayout(saveButton, cancelButton)));
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
