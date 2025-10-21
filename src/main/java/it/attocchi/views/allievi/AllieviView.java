package it.attocchi.views.allievi;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import it.attocchi.entity.Allievo;
import it.attocchi.service.AllievoService;
import it.attocchi.views.MainLayout;
import org.vaadin.lineawesome.LineAwesomeIcon;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

@PageTitle("Gestione Allievi")
@Route(value = "allievi", layout = MainLayout.class)
public class AllieviView extends Div {

    private final AllievoService allievoService;
    
    private Grid<Allievo> grid = new Grid<>(Allievo.class, false);
    private TextField filterText = new TextField();
    private AllieveForm form;
    
    public AllieviView(AllievoService allievoService) {
        this.allievoService = allievoService;
        
        addClassName("allievi-view");
        setSizeFull();
        
        configureGrid();
        configureForm();
        
        add(getToolbar(), getContent());
        updateList();
        closeEditor();
    }
    
    private void configureGrid() {
        grid.addClassName("allievi-grid");
        grid.setSizeFull();
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_WRAP_CELL_CONTENT);

        grid.addColumn(Allievo::getCognome).setHeader("Cognome").setSortable(true).setFlexGrow(1);
        grid.addColumn(Allievo::getNome).setHeader("Nome").setSortable(true).setFlexGrow(1);
        grid.addColumn(allievo -> allievo.getDataNascita().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
            .setHeader("Data di Nascita").setSortable(true).setFlexGrow(0).setWidth("150px");

        grid.asSingleSelect().addValueChangeListener(event -> editAllievo(event.getValue()));
    }
    
    private Component getToolbar() {
        filterText.setPlaceholder("Cerca allievo...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);
        filterText.addValueChangeListener(e -> updateList());
        filterText.setPrefixComponent(LineAwesomeIcon.SEARCH_SOLID.create());
        
        Button addButton = new Button("Nuovo Allievo");
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.setPrefixComponent(LineAwesomeIcon.PLUS_SOLID.create());
        addButton.addClickListener(click -> addAllievo());
        
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
        form = new AllieveForm();
        form.setWidth("25em");
        
        form.addSaveListener(this::saveAllievo);
        form.addDeleteListener(this::deleteAllievo);
        form.addCloseListener(e -> closeEditor());
    }
    
    private void saveAllievo(AllieveForm.SaveEvent event) {
        allievoService.save(event.getAllievo());
        updateList();
        closeEditor();
        Notification.show("Allievo salvato con successo")
            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
    
    private void deleteAllievo(AllieveForm.DeleteEvent event) {
        allievoService.delete(event.getAllievo());
        updateList();
        closeEditor();
        Notification.show("Allievo eliminato")
            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
    
    private void addAllievo() {
        grid.asSingleSelect().clear();
        editAllievo(new Allievo());
    }
    
    private void editAllievo(Allievo allievo) {
        if (allievo == null) {
            closeEditor();
        } else {
            form.setAllievo(allievo);
            form.setVisible(true);
            addClassName("editing");
        }
    }
    
    private void closeEditor() {
        form.setAllievo(null);
        form.setVisible(false);
        removeClassName("editing");
    }
    
    private void updateList() {
        grid.setItems(allievoService.search(filterText.getValue()));
    }
}
