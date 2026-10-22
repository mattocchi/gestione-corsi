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
import it.attocchi.entity.Genitore;
import it.attocchi.entity.Insegnante;
import it.attocchi.entity.Allievo;
import it.attocchi.entity.User;
import it.attocchi.security.SecurityService;
import it.attocchi.service.GenitoreService;
import it.attocchi.service.InsegnanteService;
import it.attocchi.service.AllievoService;
import it.attocchi.service.UserService;
import jakarta.annotation.security.RolesAllowed;

import java.util.Locale;
import java.util.Optional;

/**
 * Profilo personale.
 * <ul>
 *   <li>Insegnante: aggiorna contatti, specializzazione, biografia e dati anagrafici; username e tariffa oraria
 *       sono in sola lettura (li gestiscono admin e segreteria).</li>
 *   <li>Genitore: aggiorna contatti e dati anagrafici (i dati dei figli si modificano da I Miei Figli).</li>
 *   <li>Allievo: sola lettura, con l'indirizzo del genitore se non ne ha uno proprio.</li>
 * </ul>
 * Insegnante e genitore possono cambiare la password.
 */
@Route(value = "profilo", layout = MainLayout.class)
@PageTitle("Il mio profilo")
@Menu(title = "Il mio profilo", order = 90, icon = "vaadin:user-card")
@RolesAllowed({"INSEGNANTE", "GENITORE", "ALLIEVO"})
public class ProfiloView extends VerticalLayout {

    private final UserService userService;
    private final String username;

    public ProfiloView(SecurityService securityService, UserService userService, InsegnanteService insegnanteService,
                       GenitoreService genitoreService, AllievoService allievoService) {
        this.userService = userService;
        this.username = securityService.getUsername().orElseThrow();
        setMaxWidth("800px");

        Optional<Insegnante> insegnante = insegnanteService.findByUsername(username);
        Optional<Genitore> genitore = genitoreService.findByUsername(username);
        Optional<Allievo> allievo = allievoService.findByUsername(username);
        if (insegnante.isPresent()) {
            profiloInsegnante(insegnante.get(), insegnanteService);
        } else if (genitore.isPresent()) {
            profiloGenitore(genitore.get(), genitoreService);
        } else if (allievo.isPresent()) {
            profiloAllievo(allievo.get());
        } else {
            add(new Span("Nessuna scheda anagrafica collegata a questo utente."));
        }
        if (allievo.isEmpty() || insegnante.isPresent() || genitore.isPresent()) {
            add(ViewSupport.sezione("Cambia password"), cambioPassword());
        }
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
        BigDecimalField forfait = new BigDecimalField("Rimborso forfettario annuo");
        forfait.setLocale(Locale.ITALY);
        forfait.setPrefixComponent(new Span("€"));
        forfait.setSuffixComponent(new Span("/anno"));
        forfait.setHelperText("Impostato dalla segreteria");
        forfait.setValue(insegnante.getRimborsoForfettario());
        forfait.setReadOnly(true);
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

        FormLayout insegnamento = new FormLayout(specializzazione, tariffa, forfait, biografia);
        insegnamento.setColspan(biografia, 2);
        add(ViewSupport.sezione("Utente e contatti"), new FormLayout(utente.getComponents()),
            ViewSupport.sezione("Insegnamento"), insegnamento,
            ViewSupport.sezione("Dati anagrafici"), new FormLayout(anagrafica.getComponents()),
            salva);
    }

    private void profiloGenitore(Genitore genitore, GenitoreService genitoreService) {
        User user = genitore.getUser();
        UtenteFields utente = new UtenteFields(user, userService);
        Binder<Genitore> binder = new Binder<>(Genitore.class);
        AnagraficaFields<Genitore> anagrafica = new AnagraficaFields<>(binder, false);
        binder.readBean(genitore);

        Button salva = new Button("Salva", e -> {
            boolean valido = binder.validate().isOk() & utente.validate();
            if (!valido || !binder.writeBeanIfValid(genitore) || !utente.writeBeanIfValid(user)) {
                return;
            }
            try {
                genitoreService.aggiornaProfilo(username, genitore, user);
                Notification.show("Profilo salvato");
            } catch (RuntimeException ex) {
                Notification.show("Errore nel salvataggio del profilo: " + ex.getMessage());
            }
        });
        salva.addThemeVariants(ButtonVariant.PRIMARY);

        add(ViewSupport.sezione("Utente e contatti"), new FormLayout(utente.getComponents()),
            ViewSupport.sezione("Dati anagrafici"), new FormLayout(anagrafica.getComponents()),
            salva);
    }

    /** Profilo dell'allievo in sola lettura; se non ha un indirizzo proprio si mostra quello del genitore. */
    private void profiloAllievo(Allievo allievo) {
        UtenteFields utente = new UtenteFields(allievo.getUser(), userService);
        utente.setReadOnly(true);
        Binder<Allievo> binder = new Binder<>(Allievo.class);
        AnagraficaFields<Allievo> anagrafica = new AnagraficaFields<>(binder, true);
        binder.readBean(allievo);
        binder.setReadOnly(true);

        TextField genitore = new TextField("Genitore");
        genitore.setValue(allievo.getGenitore() != null ? allievo.getGenitore().getNomeCompleto() : "");
        genitore.setReadOnly(true);
        FormLayout utenteForm = new FormLayout(utente.getComponents());
        utenteForm.add(genitore);

        Span avviso = new Span("Per modificare i dati rivolgiti alla segreteria o al genitore.");
        avviso.addClassNames(Stili.TESTO_SECONDARIO);
        add(avviso, ViewSupport.sezione("Utente e genitore"), utenteForm,
            ViewSupport.sezione("Dati anagrafici"), new FormLayout(anagrafica.getComponents()));
        if (allievo.isIndirizzoDelGenitore()) {
            add(new Span("Indirizzo: " + Formati.indirizzo(allievo)));
        }
    }

    /** Nuova password con conferma. */
    private VerticalLayout cambioPassword() {
        PasswordField nuova = new PasswordField("Nuova password");
        nuova.setHelperText("Almeno " + UserService.PASSWORD_MINIMA + " caratteri");
        PasswordField conferma = new PasswordField("Conferma password");
        Button cambia = new Button("Cambia password", e -> {
            boolean lunga = nuova.getValue().length() >= UserService.PASSWORD_MINIMA;
            nuova.setInvalid(!lunga);
            nuova.setErrorMessage("La password deve avere almeno " + UserService.PASSWORD_MINIMA + " caratteri");
            boolean uguali = nuova.getValue().equals(conferma.getValue());
            conferma.setInvalid(!uguali);
            conferma.setErrorMessage("Le password non coincidono");
            if (!lunga || !uguali) {
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
        layout.addClassNames("p-0");
        return layout;
    }
}
