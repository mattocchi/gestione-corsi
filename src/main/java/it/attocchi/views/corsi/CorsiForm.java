package it.attocchi.views.corsi;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.shared.Registration;
import it.attocchi.entity.Allievo;
import it.attocchi.entity.Corso;
import it.attocchi.entity.Docente;

import java.util.List;
import java.util.Locale;

public class CorsiForm extends FormLayout {

    private Corso corso;
    
    TextField nome = new TextField("Nome Corso");
    DatePicker dataInizio = new DatePicker("Data Inizio");
    DatePicker dataFine = new DatePicker("Data Fine");
    ComboBox<Docente> docente = new ComboBox<>("Docente");
    MultiSelectComboBox<Allievo> allievi = new MultiSelectComboBox<>("Allievi");
    
    Button save = new Button("Salva");
    Button delete = new Button("Elimina");
    Button close = new Button("Annulla");
    
    Binder<Corso> binder = new BeanValidationBinder<>(Corso.class);
    
    public CorsiForm(List<Docente> docenti, List<Allievo> allAllievi) {
        addClassName("corsi-form");
        
        dataInizio.setLocale(new Locale("it", "IT"));
        dataFine.setLocale(new Locale("it", "IT"));
        
        docente.setItems(docenti);
        docente.setItemLabelGenerator(Docente::getNomeCompleto);
        
        allievi.setItems(allAllievi);
        allievi.setItemLabelGenerator(Allievo::getNomeCompleto);
        
        binder.forField(nome).bind(Corso::getNome, Corso::setNome);
        binder.forField(dataInizio).bind(Corso::getDataInizio, Corso::setDataInizio);
        binder.forField(dataFine).bind(Corso::getDataFine, Corso::setDataFine);
        binder.forField(docente).bind(Corso::getDocente, Corso::setDocente);
        binder.forField(allievi).bind(
            corso -> corso.getAllievi(),
            (corso, value) -> {
                corso.getAllievi().clear();
                corso.getAllievi().addAll(value);
            }
        );
        
        add(nome, docente, dataInizio, dataFine, allievi, createButtonsLayout());
    }
    
    private HorizontalLayout createButtonsLayout() {
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        close.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        
        save.addClickShortcut(Key.ENTER);
        close.addClickShortcut(Key.ESCAPE);
        
        save.addClickListener(event -> validateAndSave());
        delete.addClickListener(event -> fireEvent(new DeleteEvent(this, corso)));
        close.addClickListener(event -> fireEvent(new CloseEvent(this)));
        
        binder.addStatusChangeListener(e -> save.setEnabled(binder.isValid()));
        
        return new HorizontalLayout(save, delete, close);
    }
    
    private void validateAndSave() {
        if (binder.isValid()) {
            fireEvent(new SaveEvent(this, corso));
        }
    }
    
    public void setCorso(Corso corso) {
        this.corso = corso;
        binder.readBean(corso);
    }
    
    // Events
    public static abstract class CorsiFormEvent extends ComponentEvent<CorsiForm> {
        private Corso corso;
        
        protected CorsiFormEvent(CorsiForm source, Corso corso) {
            super(source, false);
            this.corso = corso;
        }
        
        public Corso getCorso() {
            return corso;
        }
    }
    
    public static class SaveEvent extends CorsiFormEvent {
        SaveEvent(CorsiForm source, Corso corso) {
            super(source, corso);
        }
    }
    
    public static class DeleteEvent extends CorsiFormEvent {
        DeleteEvent(CorsiForm source, Corso corso) {
            super(source, corso);
        }
    }
    
    public static class CloseEvent extends CorsiFormEvent {
        CloseEvent(CorsiForm source) {
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
