package it.attocchi.views.docenti;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.shared.Registration;
import it.attocchi.entity.Docente;

public class DocentiForm extends FormLayout {

    private Docente docente;
    
    TextField nome = new TextField("Nome");
    TextField cognome = new TextField("Cognome");
    
    Button save = new Button("Salva");
    Button delete = new Button("Elimina");
    Button close = new Button("Annulla");
    
    Binder<Docente> binder = new BeanValidationBinder<>(Docente.class);
    
    public DocentiForm() {
        addClassName("docenti-form");
        
        binder.bindInstanceFields(this);
        
        add(nome, cognome, createButtonsLayout());
    }
    
    private HorizontalLayout createButtonsLayout() {
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        close.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        
        save.addClickShortcut(Key.ENTER);
        close.addClickShortcut(Key.ESCAPE);
        
        save.addClickListener(event -> validateAndSave());
        delete.addClickListener(event -> fireEvent(new DeleteEvent(this, docente)));
        close.addClickListener(event -> fireEvent(new CloseEvent(this)));
        
        binder.addStatusChangeListener(e -> save.setEnabled(binder.isValid()));
        
        return new HorizontalLayout(save, delete, close);
    }
    
    private void validateAndSave() {
        if (binder.isValid()) {
            fireEvent(new SaveEvent(this, docente));
        }
    }
    
    public void setDocente(Docente docente) {
        this.docente = docente;
        binder.readBean(docente);
    }
    
    // Events
    public static abstract class DocentiFormEvent extends ComponentEvent<DocentiForm> {
        private Docente docente;
        
        protected DocentiFormEvent(DocentiForm source, Docente docente) {
            super(source, false);
            this.docente = docente;
        }
        
        public Docente getDocente() {
            return docente;
        }
    }
    
    public static class SaveEvent extends DocentiFormEvent {
        SaveEvent(DocentiForm source, Docente docente) {
            super(source, docente);
        }
    }
    
    public static class DeleteEvent extends DocentiFormEvent {
        DeleteEvent(DocentiForm source, Docente docente) {
            super(source, docente);
        }
    }
    
    public static class CloseEvent extends DocentiFormEvent {
        CloseEvent(DocentiForm source) {
            super(source, null);
        }
    }
    
    public Registration addSaveListener(ComponentEventListener<SaveEvent> listener) {
        return addListener(SaveEvent.class, listener);
    }
    
    public Registration addDeleteListener(ComponentEventListener<DeleteEvent> listener) {
        return addListener(DeleteEvent.class, listener);
    }
    
    public Registration addCloseListener(ComponentEventListener<CloseEvent> listener) {
        return addListener(CloseEvent.class, listener);
    }
}
