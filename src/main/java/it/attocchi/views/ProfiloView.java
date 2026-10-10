package it.attocchi.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import it.attocchi.entity.Insegnante;
import it.attocchi.entity.User;
import it.attocchi.security.SecurityService;
import it.attocchi.service.InsegnanteService;
import it.attocchi.service.UserService;
import jakarta.annotation.security.RolesAllowed;

import java.util.Locale;

/**
 * Profilo personale: l'insegnante aggiorna contatti, specializzazione, biografia e dati anagrafici e cambia
 * la password; username e tariffa oraria sono in sola lettura (li gestiscono admin e segreteria).
 */
@Route(value = "profilo", layout = MainLayout.class)
@PageTitle("Il mio profilo")
@Menu(title = "Il mio profilo", order = 90, icon = "vaadin:user-card")
@RolesAllowed("INSEGNANTE")
public class ProfiloView extends VerticalLayout {

    private final UserService userService;
    private final String username;

    public ProfiloView(SecurityService securityService, UserService userService, InsegnanteService insegnanteService) {
        this.userService = userService;
        this.username = securityService.getUsername().orElseThrow();
        setMaxWidth("800px");

        insegnanteService.findByUsername(username).ifPresentOrElse(
            insegnante -> profiloInsegnante(insegnante, insegnanteService),
            () -> add(new Span("Nessuna scheda insegnante collegata a questo utente.")));
        add(ViewSupport.sezione("Cambia password"), cambioPassword());
    }

    private void profiloInsegnante(Insegnante insegnante, InsegnanteService insegnanteService) {
        User user = insegnante.getUser();
        UtenteFields utente = new UtenteFields(user, userService);

        Binder<Insegnante> binder = new Binder<>(Insegnante.class);
        TextField specializzazione = new TextField("Specializzazione");
        TextArea biografia = new TextArea("Biografia");
        BigDecimalField tariffa = new BigDecimalField("Tariffa oraria");
        tariffa.setLocale(Locale.ITALY);
        tariffa.setPrefixComponent(new Span("€"));
        tariffa.setSuffixComponent(new Span("/ora"));
        tariffa.setHelperText("Impostata dalla segreteria");
        tariffa.setValue(insegnante.getTariffaOraria());
        tariffa.setReadOnly(true);
        binder.forField(specializzazione).withNullRepresentation("")
            .bind(Insegnante::getSpecializzazione, Insegnante::setSpecializzazione);
        binder.forField(biografia).withNullRepresentation("").bind(Insegnante::getBiografia, Insegnante::setBiografia);
        AnagraficaFields<Insegnante> anagrafica = new AnagraficaFields<>(binder, false);
        binder.readBean(insegnante);

        Button salva = new Button("Salva", e -> {
            boolean valido = binder.validate().isOk() & utente.validate();
            if (!valido || !binder.writeBeanIfValid(insegnante) || !utente.writeBeanIfValid(user)) {
                return;
            }
            try {
                insegnanteService.aggiornaProfilo(username, insegnante, user);
                Notification.show("Profilo salvato");
            } catch (RuntimeException ex) {
                Notification.show("Errore nel salvataggio del profilo: " + ex.getMessage());
            }
        });
        salva.addThemeVariants(ButtonVariant.PRIMARY);

        FormLayout insegnamento = new FormLayout(specializzazione, tariffa, biografia);
        insegnamento.setColspan(biografia, 2);
        add(ViewSupport.sezione("Utente e contatti"), new FormLayout(utente.getComponents()),
            ViewSupport.sezione("Insegnamento"), insegnamento,
            ViewSupport.sezione("Dati anagrafici"), new FormLayout(anagrafica.getComponents()),
            salva);
    }

    /** Nuova password con conferma. */
    private VerticalLayout cambioPassword() {
        PasswordField nuova = new PasswordField("Nuova password");
        PasswordField conferma = new PasswordField("Conferma password");
        Button cambia = new Button("Cambia password", e -> {
            nuova.setInvalid(nuova.isEmpty());
            nuova.setErrorMessage("Inserisci la nuova password");
            boolean uguali = nuova.getValue().equals(conferma.getValue());
            conferma.setInvalid(!uguali);
            conferma.setErrorMessage("Le password non coincidono");
            if (nuova.isEmpty() || !uguali) {
                return;
            }
            try {
                userService.cambiaPassword(username, nuova.getValue());
                nuova.clear();
                conferma.clear();
                Notification.show("Password cambiata");
            } catch (RuntimeException ex) {
                Notification.show("Errore nel cambio della password: " + ex.getMessage());
            }
        });
        VerticalLayout layout = new VerticalLayout(new FormLayout(nuova, conferma), cambia);
        layout.setPadding(false);
        return layout;
    }
}
