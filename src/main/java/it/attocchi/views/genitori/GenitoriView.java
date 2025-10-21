package it.attocchi.views.genitori;

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
import it.attocchi.entity.Genitore;
import it.attocchi.service.AllievoService;
import it.attocchi.service.GenitoreService;
import it.attocchi.views.MainLayout;
import org.vaadin.lineawesome.LineAwesomeIcon;

@PageTitle("Gestione Genitori")
@Route(value = "genitori", layout = MainLayout.class)
public class GenitoriView extends Div {

    private final GenitoreService genitoreService;
    private final AllievoService allievoService;
    
    private Grid<Genitore> grid = new Grid<>(Genitore.class, false);
    private TextField filterText = new TextField();
    private GenitoriForm form;
    
    public GenitoriView(GenitoreService genitoreService, AllievoService allievoService) {
        this.genitoreService = genitoreService;
        this.allievoService = allievoService;
        
        addClassName("genitori-view");
        setSizeFull();
        
        configureGrid();
        configureForm();
        
        add(getToolbar(), getContent());
        updateList();
        closeEditor();
    }
    
    private void configureGrid() {
        grid.addClassName("genitori-grid");
        grid.setSizeFull();
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_WRAP_CELL_CONTENT);

        grid.addColumn(Genitore::getCognome).setHeader("Cognome").setSortable(true).setFlexGrow(1);
        grid.addColumn(Genitore::getNome).setHeader("Nome").setSortable(true).setFlexGrow(1);
        grid.addColumn(Genitore::getTelefono).setHeader("Telefono").setSortable(true).setFlexGrow(0).setWidth("150px");
        grid.addColumn(Genitore::getEmail).setHeader("Email").setSortable(true).setFlexGrow(2);
        grid.addColumn(genitore -> genitore.getAllievi().size()).setHeader("N. Figli").setSortable(true).setFlexGrow(0).setWidth("120px");

        grid.asSingleSelect().addValueChangeListener(event -> editGenitore(event.getValue()));
    }
    
    private Component getToolbar() {
        filterText.setPlaceholder("Cerca genitore...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);
        filterText.addValueChangeListener(e -> updateList());
        filterText.setPrefixComponent(LineAwesomeIcon.SEARCH_SOLID.create());
        
        Button addButton = new Button("Nuovo Genitore");
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.setPrefixComponent(LineAwesomeIcon.PLUS_SOLID.create());
        addButton.addClickListener(click -> addGenitore());
        
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
        form = new GenitoriForm(allievoService.findAll());
        form.setWidth("25em");
        
        form.addSaveListener(this::saveGenitore);
        form.addDeleteListener(this::deleteGenitore);
        form.addCloseListener(e -> closeEditor());
    }
    
    private void saveGenitore(GenitoriForm.SaveEvent event) {
        genitoreService.save(event.getGenitore());
        updateList();
        closeEditor();
        Notification.show("Genitore salvato con successo")
            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
    
    private void deleteGenitore(GenitoriForm.DeleteEvent event) {
        genitoreService.delete(event.getGenitore());
        updateList();
        closeEditor();
        Notification.show("Genitore eliminato")
            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
    
    private void addGenitore() {
        grid.asSingleSelect().clear();
        editGenitore(new Genitore());
    }
    
    private void editGenitore(Genitore genitore) {
        if (genitore == null) {
            closeEditor();
        } else {
            form.setGenitore(genitore);
            form.setVisible(true);
            addClassName("editing");
        }
    }
    
    private void closeEditor() {
        form.setGenitore(null);
        form.setVisible(false);
        removeClassName("editing");
    }
    
    private void updateList() {
        grid.setItems(genitoreService.search(filterText.getValue()));
    }
}
