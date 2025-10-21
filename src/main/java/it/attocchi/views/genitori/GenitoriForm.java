package it.attocchi.views.genitori;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.shared.Registration;
import it.attocchi.entity.Allievo;
import it.attocchi.entity.Genitore;

import java.util.List;

public class GenitoriForm extends FormLayout {

    private Genitore genitore;
    
    TextField nome = new TextField("Nome");
    TextField cognome = new TextField("Cognome");
    TextField telefono = new TextField("Telefono");
    EmailField email = new EmailField("Email");
    MultiSelectComboBox<Allievo> allievi = new MultiSelectComboBox<>("Figli");
    
    Button save = new Button("Salva");
    Button delete = new Button("Elimina");
    Button close = new Button("Annulla");
    
    Binder<Genitore> binder = new BeanValidationBinder<>(Genitore.class);
    
    public GenitoriForm(List<Allievo> allAllievi) {
        addClassName("genitori-form");
        
        allievi.setItems(allAllievi);
        allievi.setItemLabelGenerator(Allievo::getNomeCompleto);
        
        binder.forField(nome).bind(Genitore::getNome, Genitore::setNome);
        binder.forField(cognome).bind(Genitore::getCognome, Genitore::setCognome);
        binder.forField(telefono).bind(Genitore::getTelefono, Genitore::setTelefono);
        binder.forField(email).bind(Genitore::getEmail, Genitore::setEmail);
        binder.forField(allievi).bind(
            genitore -> genitore.getAllievi(),
            (genitore, value) -> {
                genitore.getAllievi().clear();
                genitore.getAllievi().addAll(value);
            }
        );
        
        add(nome, cognome, telefono, email, allievi, createButtonsLayout());
    }
    
    private HorizontalLayout createButtonsLayout() {
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        close.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        
        save.addClickShortcut(Key.ENTER);
        close.addClickShortcut(Key.ESCAPE);
        
        save.addClickListener(event -> validateAndSave());
        delete.addClickListener(event -> fireEvent(new DeleteEvent(this, genitore)));
        close.addClickListener(event -> fireEvent(new CloseEvent(this)));
        
        binder.addStatusChangeListener(e -> save.setEnabled(binder.isValid()));
        
        return new HorizontalLayout(save, delete, close);
    }
    
    private void validateAndSave() {
        if (binder.isValid()) {
            fireEvent(new SaveEvent(this, genitore));
        }
    }
    
    public void setGenitore(Genitore genitore) {
        this.genitore = genitore;
        binder.readBean(genitore);
    }
    
    // Events
    public static abstract class GenitoriFormEvent extends ComponentEvent<GenitoriForm> {
        private Genitore genitore;
        
        protected GenitoriFormEvent(GenitoriForm source, Genitore genitore) {
            super(source, false);
            this.genitore = genitore;
        }
        
        public Genitore getGenitore() {
            return genitore;
        }
    }
    
    public static class SaveEvent extends GenitoriFormEvent {
        SaveEvent(GenitoriForm source, Genitore genitore) {
            super(source, genitore);
        }
    }
    
    public static class DeleteEvent extends GenitoriFormEvent {
        DeleteEvent(GenitoriForm source, Genitore genitore) {
            super(source, genitore);
        }
    }
    
    public static class CloseEvent extends GenitoriFormEvent {
        CloseEvent(GenitoriForm source) {
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
