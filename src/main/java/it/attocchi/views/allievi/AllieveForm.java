package it.attocchi.views.allievi;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.shared.Registration;
import it.attocchi.entity.Allievo;

import java.util.Locale;

public class AllieveForm extends FormLayout {

    private Allievo allievo;
    
    TextField nome = new TextField("Nome");
    TextField cognome = new TextField("Cognome");
    DatePicker dataNascita = new DatePicker("Data di Nascita");
    
    Button save = new Button("Salva");
    Button delete = new Button("Elimina");
    Button close = new Button("Annulla");
    
    Binder<Allievo> binder = new BeanValidationBinder<>(Allievo.class);
    
    public AllieveForm() {
        addClassName("allievi-form");
        
        dataNascita.setLocale(new Locale("it", "IT"));
        
        binder.bindInstanceFields(this);
        
        add(nome, cognome, dataNascita, createButtonsLayout());
    }
    
    private HorizontalLayout createButtonsLayout() {
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        close.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        
        save.addClickShortcut(Key.ENTER);
        close.addClickShortcut(Key.ESCAPE);
        
        save.addClickListener(event -> validateAndSave());
        delete.addClickListener(event -> fireEvent(new DeleteEvent(this, allievo)));
        close.addClickListener(event -> fireEvent(new CloseEvent(this)));
        
        binder.addStatusChangeListener(e -> save.setEnabled(binder.isValid()));
        
        return new HorizontalLayout(save, delete, close);
    }
    
    private void validateAndSave() {
        if (binder.isValid()) {
            fireEvent(new SaveEvent(this, allievo));
        }
    }
    
    public void setAllievo(Allievo allievo) {
        this.allievo = allievo;
        binder.readBean(allievo);
    }
    
    // Events
    public static abstract class AllieveFormEvent extends ComponentEvent<AllieveForm> {
        private Allievo allievo;
        
        protected AllieveFormEvent(AllieveForm source, Allievo allievo) {
            super(source, false);
            this.allievo = allievo;
        }
        
        public Allievo getAllievo() {
            return allievo;
        }
    }
    
    public static class SaveEvent extends AllieveFormEvent {
        SaveEvent(AllieveForm source, Allievo allievo) {
            super(source, allievo);
        }
    }
    
    public static class DeleteEvent extends AllieveFormEvent {
        DeleteEvent(AllieveForm source, Allievo allievo) {
            super(source, allievo);
        }
    }
    
    public static class CloseEvent extends AllieveFormEvent {
        CloseEvent(AllieveForm source) {
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
