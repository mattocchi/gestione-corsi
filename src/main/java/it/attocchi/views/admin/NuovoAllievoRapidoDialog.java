package it.attocchi.views.admin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.SerializableConsumer;
import it.attocchi.entity.Allievo;
import it.attocchi.entity.User;
import it.attocchi.service.AllievoService;
import it.attocchi.views.Stili;
import it.attocchi.views.ViewSupport;

import java.util.List;
import java.util.Set;

/**
 * Inserimento rapido di un allievo che non esiste ancora, dal campo "Aggiungi un allievo" dei partecipanti: nome e
 * cognome (proposti dal testo scritto), email e telefono facoltativi. Segnala gli omonimi, che si possono iscrivere
 * al posto di un nuovo allievo. Il resto della scheda si completa da Gestione Allievi.
 */
public class NuovoAllievoRapidoDialog extends Dialog {

    private final AllievoService allievoService;
    private final Set<Long> giaIscritti;
    private final SerializableConsumer<Allievo> iscrivi;
    private final TextField nome = new TextField("Nome");
    private final TextField cognome = new TextField("Cognome");
    private final Div omonimi = new Div();

    /**
     * @param testo      quanto scritto nel campo: la prima parola diventa il nome, il resto il cognome
     * @param giaIscritti id degli allievi già iscritti al corso (gli omonimi iscritti non si propongono)
     * @param iscrivi    iscrive al corso l'allievo creato o l'omonimo scelto
     */
    public NuovoAllievoRapidoDialog(AllievoService allievoService, String testo, Set<Long> giaIscritti,
                                    SerializableConsumer<Allievo> iscrivi) {
        this.allievoService = allievoService;
        this.giaIscritti = giaIscritti;
        this.iscrivi = iscrivi;
        setHeaderTitle("Nuovo allievo");
        setWidth("480px");
        setMaxWidth("100%");

        String[] parti = testo != null ? testo.strip().split("\\s+", 2) : new String[0];
        User user = new User();
        user.setNome(parti.length > 0 ? parti[0] : "");
        user.setCognome(parti.length > 1 ? parti[1] : "");

        EmailField email = new EmailField("Email");
        TextField telefono = new TextField("Telefono");
        email.setHelperText("Facoltativa");
        telefono.setHelperText("Facoltativo");
        Binder<User> binder = new Binder<>(User.class);
        binder.forField(nome).asRequired("Inserisci il nome").bind(User::getNome, User::setNome);
        binder.forField(cognome).asRequired("Inserisci il cognome").bind(User::getCognome, User::setCognome);
        binder.forField(email).withValidator(new EmailValidator("Email non valida", true)).bind(User::getEmail, User::setEmail);
        binder.forField(telefono).bind(User::getTelefono, User::setTelefono);
        binder.readBean(user);

        nome.setValueChangeMode(ValueChangeMode.LAZY);
        cognome.setValueChangeMode(ValueChangeMode.LAZY);
        nome.addValueChangeListener(e -> mostraOmonimi());
        cognome.addValueChangeListener(e -> mostraOmonimi());

        Span nota = new Span("Codice fiscale, data di nascita, genitore e accesso si completano poi da Allievi.");
        nota.addClassNames(Stili.TESTO_SECONDARIO, "block mt-3");
        omonimi.addClassNames("flex flex-col gap-2");
        FormLayout form = new FormLayout(nome, cognome, email, telefono);
        add(form, omonimi, nota);
        mostraOmonimi();

        Button crea = new Button("Crea e iscrivi", e -> {
            if (!binder.writeBeanIfValid(user)) {
                return;
            }
            Allievo nuovo;
            try {
                nuovo = allievoService.creaRapido(user.getNome(), user.getCognome(), user.getEmail(), user.getTelefono());
            } catch (RuntimeException ex) {
                Notification.show("Errore nella creazione dell'allievo: " + ex.getMessage());
                return;
            }
            close();
            iscrivi.accept(nuovo);
        });
        crea.addThemeVariants(ButtonVariant.PRIMARY);
        getFooter().add(ViewSupport.closeButton(this, "Annulla"), crea);
    }

    /** Avviso con gli allievi che hanno già questo nome e cognome, e il pulsante per iscrivere quelli non iscritti. */
    private void mostraOmonimi() {
        omonimi.removeAll();
        if (nome.getValue().isBlank() || cognome.getValue().isBlank()) {
            return;
        }
        List<Allievo> trovati = allievoService.omonimi(nome.getValue(), cognome.getValue());
        for (Allievo allievo : trovati) {
            String descrizione = allievo.getNomeCompleto()
                + (allievo.getGenitore() != null ? " (genitore " + allievo.getGenitore().getNomeCompleto() + ")" : "")
                + (allievo.getUser().getEmail() != null ? " – " + allievo.getUser().getEmail() : "");
            Span avviso = new Span("Esiste già un allievo " + descrizione);
            Div riga = new Div(avviso);
            riga.addClassNames("flex flex-wrap items-center gap-x-2");
            if (giaIscritti.contains(allievo.getId())) {
                Span iscritto = new Span("già iscritto al corso");
                iscritto.addClassNames(Stili.TESTO_SECONDARIO);
                riga.add(iscritto);
            } else {
                Button esistente = new Button("Iscrivi questo", e -> {
                    close();
                    iscrivi.accept(allievo);
                });
                esistente.addThemeVariants(ButtonVariant.TERTIARY);
                riga.add(esistente);
            }
            omonimi.add(riga);
        }
    }
}
