package it.attocchi.views.admin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.function.SerializableRunnable;
import it.attocchi.entity.Corso;
import it.attocchi.entity.GruppoCorso;
import it.attocchi.service.CorsoService;
import it.attocchi.views.Formati;
import it.attocchi.views.SegmentedControl;
import it.attocchi.views.ViewSupport;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.format.TextStyle;
import java.util.List;

/** Nuovo gruppo del corso o modifica di uno esistente: nome facoltativo, giorno tra quelli del corso e orario. */
public class GruppoDialog extends Dialog {

    public GruppoDialog(CorsoService corsoService, Corso corso, GruppoCorso gruppo, SerializableRunnable salvato) {
        boolean nuovo = gruppo.getId() == null;
        setHeaderTitle(nuovo ? "Nuovo gruppo" : "Modifica gruppo");
        setWidth("420px");
        setMaxWidth("100%");

        List<DayOfWeek> giorniCorso = corso.getGiorniSettimana().stream().sorted().toList();
        if (nuovo) {
            gruppo.setGiorno(giorniCorso.isEmpty() ? null : giorniCorso.getFirst());
            gruppo.setOrario(corso.getOrarioInizio());
        }
        TextField nome = new TextField("Nome");
        nome.setHelperText("Facoltativo, es. Principianti");
        SegmentedControl<DayOfWeek> giorno = new SegmentedControl<>("Giorno del gruppo", giorniCorso, Formati::giorno,
            g -> g.getDisplayName(TextStyle.FULL, ViewSupport.LOCALE), null);
        TimePicker orario = ViewSupport.timePicker("Orario");
        orario.setStep(Duration.ofMinutes(5));
        orario.setHelperText("Proposto agli allievi del gruppo");

        Binder<GruppoCorso> binder = new Binder<>(GruppoCorso.class);
        binder.forField(nome).bind(GruppoCorso::getNome, GruppoCorso::setNome);
        binder.forField(giorno).asRequired("Scegli il giorno").bind(GruppoCorso::getGiorno, GruppoCorso::setGiorno);
        binder.forField(orario).asRequired("Indica l'orario").bind(GruppoCorso::getOrario, GruppoCorso::setOrario);
        binder.readBean(gruppo);

        FormLayout form = new FormLayout();
        form.add(nome);
        form.addFormItem(giorno, "Giorno");
        form.add(orario);
        add(form);

        Button salva = new Button("Salva", e -> {
            if (!binder.writeBeanIfValid(gruppo)) {
                return;
            }
            try {
                corsoService.salvaGruppo(corso.getId(), gruppo);
            } catch (RuntimeException ex) {
                Notification.show("Errore nel salvataggio del gruppo: " + ex.getMessage());
                return;
            }
            close();
            salvato.run();
        });
        salva.addThemeVariants(ButtonVariant.PRIMARY);
        getFooter().add(ViewSupport.closeButton(this, "Annulla"), salva);
    }
}
