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
import it.attocchi.entity.Genitore;
import it.attocchi.entity.User;
import it.attocchi.service.GenitoreService;
import it.attocchi.views.Stili;
import it.attocchi.views.ViewSupport;

/**
 * Inserimento rapido di un genitore che non esiste ancora, dal campo "Genitore" del dialog dell'allievo: nome e
 * cognome (proposti dal testo scritto), email e cellulare facoltativi. Segnala gli omonimi, che si possono scegliere
 * al posto di un nuovo genitore. Il resto della scheda si completa da Genitori.
 */
public class NuovoGenitoreRapidoDialog extends Dialog {

    private final GenitoreService genitoreService;
    private final SerializableConsumer<Genitore> scegli;
    private final TextField nome = new TextField("Nome");
    private final TextField cognome = new TextField("Cognome");
    private final Div omonimi = new Div();

    /**
     * @param testo  quanto scritto nel campo (anche vuoto): la prima parola diventa il nome, il resto il cognome
     * @param scegli imposta come genitore dell'allievo quello creato o l'omonimo scelto
     */
    public NuovoGenitoreRapidoDialog(GenitoreService genitoreService, String testo, SerializableConsumer<Genitore> scegli) {
        this.genitoreService = genitoreService;
        this.scegli = scegli;
        setHeaderTitle("Nuovo genitore");
        setWidth("480px");
        setMaxWidth("100%");

        String[] parti = testo != null && !testo.isBlank() ? testo.strip().split("\\s+", 2) : new String[0];
        User user = new User();
        user.setNome(parti.length > 0 ? parti[0] : "");
        user.setCognome(parti.length > 1 ? parti[1] : "");

        EmailField email = new EmailField("Email");
        TextField cellulare = new TextField("Cellulare");
        email.setHelperText("Facoltativa: serve per inviare conferme e ricevute");
        cellulare.setHelperText("Facoltativo");
        Binder<User> binder = new Binder<>(User.class);
        binder.forField(nome).asRequired("Inserisci il nome").bind(User::getNome, User::setNome);
        binder.forField(cognome).asRequired("Inserisci il cognome").bind(User::getCognome, User::setCognome);
        binder.forField(email).withValidator(new EmailValidator("Email non valida", true)).bind(User::getEmail, User::setEmail);
        binder.forField(cellulare).bind(User::getTelefono, User::setTelefono);
        binder.readBean(user);

        nome.setValueChangeMode(ValueChangeMode.LAZY);
        cognome.setValueChangeMode(ValueChangeMode.LAZY);
        nome.addValueChangeListener(e -> mostraOmonimi());
        cognome.addValueChangeListener(e -> mostraOmonimi());

        Span nota = new Span("Codice fiscale, indirizzo e accesso si completano poi da Genitori e Gestione Utenti.");
        nota.addClassNames(Stili.TESTO_SECONDARIO, "block mt-3");
        omonimi.addClassNames("flex flex-col gap-2");
        add(new FormLayout(nome, cognome, email, cellulare), omonimi, nota);
        mostraOmonimi();

        Button crea = new Button("Crea", e -> {
            if (!binder.writeBeanIfValid(user)) {
                return;
            }
            Genitore nuovo;
            try {
                nuovo = genitoreService.creaRapido(user.getNome(), user.getCognome(), user.getEmail(), user.getTelefono());
            } catch (RuntimeException ex) {
                Notification.show("Errore nella creazione del genitore: " + ex.getMessage());
                return;
            }
            close();
            scegli.accept(nuovo);
        });
        crea.addThemeVariants(ButtonVariant.PRIMARY);
        getFooter().add(ViewSupport.closeButton(this, "Annulla"), crea);
    }

    /** Avviso con i genitori che hanno già questo nome e cognome, e il pulsante per scegliere quello esistente. */
    private void mostraOmonimi() {
        omonimi.removeAll();
        if (nome.getValue().isBlank() || cognome.getValue().isBlank()) {
            return;
        }
        for (Genitore genitore : genitoreService.omonimi(nome.getValue(), cognome.getValue())) {
            String descrizione = genitore.getNomeCompleto()
                + (genitore.getUser().getEmail() != null ? " – " + genitore.getUser().getEmail() : "");
            Button esistente = new Button("Scegli questo", e -> {
                close();
                scegli.accept(genitore);
            });
            esistente.addThemeVariants(ButtonVariant.TERTIARY);
            Div riga = new Div(new Span("Esiste già un genitore " + descrizione), esistente);
            riga.addClassNames("flex flex-wrap items-center gap-x-2");
            omonimi.add(riga);
        }
    }
}
