package it.attocchi.views.docenti;

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
import it.attocchi.entity.Docente;
import it.attocchi.service.DocenteService;
import it.attocchi.views.MainLayout;
import org.vaadin.lineawesome.LineAwesomeIcon;

@PageTitle("Gestione Docenti")
@Route(value = "docenti", layout = MainLayout.class)
public class DocentiView extends Div {

    private final DocenteService docenteService;
    
    private Grid<Docente> grid = new Grid<>(Docente.class, false);
    private TextField filterText = new TextField();
    private DocentiForm form;
    
    public DocentiView(DocenteService docenteService) {
        this.docenteService = docenteService;
        
        addClassName("docenti-view");
        setSizeFull();
        
        configureGrid();
        configureForm();
        
        add(getToolbar(), getContent());
        updateList();
        closeEditor();
    }
    
    private void configureGrid() {
        grid.addClassName("docenti-grid");
        grid.setSizeFull();
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_WRAP_CELL_CONTENT);

        grid.addColumn(Docente::getCognome).setHeader("Cognome").setSortable(true).setFlexGrow(1);
        grid.addColumn(Docente::getNome).setHeader("Nome").setSortable(true).setFlexGrow(1);
        grid.addColumn(docente -> docente.getCorsi().size()).setHeader("N. Corsi").setSortable(true).setFlexGrow(0).setWidth("120px");

        grid.asSingleSelect().addValueChangeListener(event -> editDocente(event.getValue()));
    }
    
    private Component getToolbar() {
        filterText.setPlaceholder("Cerca docente...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);
        filterText.addValueChangeListener(e -> updateList());
        filterText.setPrefixComponent(LineAwesomeIcon.SEARCH_SOLID.create());
        
        Button addButton = new Button("Nuovo Docente");
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.setPrefixComponent(LineAwesomeIcon.PLUS_SOLID.create());
        addButton.addClickListener(click -> addDocente());
        
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
        form = new DocentiForm();
        form.setWidth("25em");
        
        form.addSaveListener(this::saveDocente);
        form.addDeleteListener(this::deleteDocente);
        form.addCloseListener(e -> closeEditor());
    }
    
    private void saveDocente(DocentiForm.SaveEvent event) {
        docenteService.save(event.getDocente());
        updateList();
        closeEditor();
        Notification.show("Docente salvato con successo")
            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
    
    private void deleteDocente(DocentiForm.DeleteEvent event) {
        docenteService.delete(event.getDocente());
        updateList();
        closeEditor();
        Notification.show("Docente eliminato")
            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
    
    private void addDocente() {
        grid.asSingleSelect().clear();
        editDocente(new Docente());
    }
    
    private void editDocente(Docente docente) {
        if (docente == null) {
            closeEditor();
        } else {
            form.setDocente(docente);
            form.setVisible(true);
            addClassName("editing");
        }
    }
    
    private void closeEditor() {
        form.setDocente(null);
        form.setVisible(false);
        removeClassName("editing");
    }
    
    private void updateList() {
        grid.setItems(docenteService.search(filterText.getValue()));
    }
}
