package it.attocchi.views.corsi;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import it.attocchi.entity.Corso;
import it.attocchi.service.AllievoService;
import it.attocchi.service.CorsoService;
import it.attocchi.service.DocenteService;
import it.attocchi.views.MainLayout;
import org.vaadin.lineawesome.LineAwesomeIcon;

import java.time.format.DateTimeFormatter;

@PageTitle("Gestione Corsi")
@Route(value = "corsi", layout = MainLayout.class)
public class CorsiView extends Div {

    private final CorsoService corsoService;
    private final DocenteService docenteService;
    private final AllievoService allievoService;
    
    private Grid<Corso> grid = new Grid<>(Corso.class, false);
    private TextField filterText = new TextField();
    private CorsiForm form;
    
    public CorsiView(CorsoService corsoService, DocenteService docenteService, AllievoService allievoService) {
        this.corsoService = corsoService;
        this.docenteService = docenteService;
        this.allievoService = allievoService;
        
        addClassName("corsi-view");
        setSizeFull();
        
        configureGrid();
        configureForm();
        
        add(getToolbar(), getContent());
        updateList();
        closeEditor();
    }
    
    private void configureGrid() {
        grid.addClassName("corsi-grid");
        grid.setSizeFull();
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_WRAP_CELL_CONTENT);

        grid.addColumn(Corso::getNome).setHeader("Nome Corso").setSortable(true).setFlexGrow(2);
        grid.addColumn(corso -> corso.getDocente() != null ? corso.getDocente().getNomeCompleto() : "")
            .setHeader("Docente").setSortable(true).setFlexGrow(1);
        grid.addColumn(corso -> corso.getDataInizio().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
            .setHeader("Data Inizio").setSortable(true).setFlexGrow(0).setWidth("130px");
        grid.addColumn(corso -> corso.getDataFine().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
            .setHeader("Data Fine").setSortable(true).setFlexGrow(0).setWidth("130px");
        grid.addColumn(corso -> corso.getAllievi().size()).setHeader("N. Allievi").setSortable(true).setFlexGrow(0).setWidth("120px");

        grid.asSingleSelect().addValueChangeListener(event -> editCorso(event.getValue()));
    }
    
    private Component getToolbar() {
        filterText.setPlaceholder("Cerca corso...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);
        filterText.addValueChangeListener(e -> updateList());
        filterText.setPrefixComponent(LineAwesomeIcon.SEARCH_SOLID.create());
        
        Button addButton = new Button("Nuovo Corso");
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.setPrefixComponent(LineAwesomeIcon.PLUS_SOLID.create());
        addButton.addClickListener(click -> addCorso());
        
        HorizontalLayout toolbar = new HorizontalLayout(filterText, addButton);
        toolbar.addClassName("toolbar");
        return toolbar;
    }
    
    private Component getContent() {
        HorizontalLayout content = new HorizontalLayout(grid, form);
        content.setFlexGrow(2, grid);
        content.setFlexGrow(1, form);
        content.addClassName("content");
        content.setSizeFull();
        return content;
    }
    
    private void configureForm() {
        form = new CorsiForm(docenteService.findAll(), allievoService.findAll());
        form.setWidth("25em");
        
        form.addSaveListener(this::saveCorso);
        form.addDeleteListener(this::deleteCorso);
        form.addCloseListener(e -> closeEditor());
    }
    
    private void saveCorso(CorsiForm.SaveEvent event) {
        corsoService.save(event.getCorso());
        updateList();
        closeEditor();
        Notification.show("Corso salvato con successo")
            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
    
    private void deleteCorso(CorsiForm.DeleteEvent event) {
        corsoService.delete(event.getCorso());
        updateList();
        closeEditor();
        Notification.show("Corso eliminato")
            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
    
    private void addCorso() {
        grid.asSingleSelect().clear();
        editCorso(new Corso());
    }
    
    private void editCorso(Corso corso) {
        if (corso == null) {
            closeEditor();
        } else {
            form.setCorso(corso);
            form.setVisible(true);
            addClassName("editing");
        }
    }
    
    private void closeEditor() {
        form.setCorso(null);
        form.setVisible(false);
        removeClassName("editing");
    }
    
    private void updateList() {
        grid.setItems(corsoService.search(filterText.getValue()));
    }
}
