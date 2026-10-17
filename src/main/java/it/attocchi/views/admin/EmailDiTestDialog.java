package it.attocchi.views.admin;

import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.validator.EmailValidator;
import it.attocchi.entity.InvioEmail.Esito;
import it.attocchi.entity.TipoDocumento;
import it.attocchi.service.EmailService;
import it.attocchi.service.EmailService.ConfigurazioneSmtp;
import it.attocchi.service.EmailService.EsitoTest;
import it.attocchi.views.SegmentedControl;
import it.attocchi.views.Stili;
import it.attocchi.views.ViewSupport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Email di test (solo admin) a un indirizzo qualunque, per verificare la configurazione SMTP e la resa dei template
 * nei client di posta: mostra la configurazione in uso (senza password) e l'esito, compreso l'errore SMTP. Il dialog
 * resta aperto per provare più template.
 */
public class EmailDiTestDialog extends Dialog {

    /** Valori del modulo, per validare il destinatario con il Binder. */
    public static class Richiesta {
        private String destinatario = "";

        public String getDestinatario() { return destinatario; }
        public void setDestinatario(String destinatario) { this.destinatario = destinatario; }
    }

    private final EmailField destinatario = new EmailField("Destinatario");
    private final SegmentedControl<Optional<TipoDocumento>> template;
    private final Div esito = new Div();

    public EmailDiTestDialog(EmailService emailService, String username) {
        setHeaderTitle("Email di test");
        setWidth("480px");
        setMaxWidth("100%");
        VerticalLayout content = new VerticalLayout();
        content.addClassNames("p-0");

        content.add(ViewSupport.sezione("Configurazione"));
        ConfigurazioneSmtp smtp = emailService.configurazione();
        if (smtp.reale()) {
            content.add(riga("Server: " + smtp.host() + ":" + smtp.port() + (smtp.starttls() ? " (STARTTLS)" : "")),
                riga("Utente: " + (vuoto(smtp.username()) ? "nessuno (senza autenticazione)" : smtp.username())),
                riga("Mittente: " + (vuoto(smtp.from()) ? "non impostato (MAIL_FROM)" : smtp.from())));
        } else {
            Span simulato = new Span("Server email non configurato (MAIL_HOST): l'invio sarà solo simulato.");
            simulato.addClassNames(Stili.TESTO_SECONDARIO);
            content.add(simulato);
        }

        content.add(ViewSupport.sezione("Messaggio"));
        destinatario.addClassNames("w-full");
        destinatario.setHelperText("Qualunque indirizzo: l'invio non è registrato tra le email inviate");
        List<Optional<TipoDocumento>> opzioni = new ArrayList<>();
        opzioni.add(Optional.empty());
        Arrays.stream(TipoDocumento.values()).map(Optional::of).forEach(opzioni::add);
        template = new SegmentedControl<>("Template", opzioni,
            t -> t.map(d -> d == TipoDocumento.RICEVUTA ? "Ricevuta" : "Conferma").orElse("Solo testo"),
            t -> t.map(d -> d.getTitolo() + " con dati di esempio e PDF allegato").orElse("Messaggio di prova senza allegato"),
            null);
        template.setValue(Optional.empty());
        Span etichetta = new Span("Template");
        etichetta.addClassNames(Stili.TESTO_SECONDARIO);
        Span nota = new Span("Conferma e ricevuta usano dati di esempio (nessun dato reale) e allegano il PDF.");
        nota.addClassNames(Stili.TESTO_SECONDARIO);
        esito.addClassNames("flex flex-wrap items-baseline gap-(--vaadin-gap-s) wrap-anywhere");
        content.add(destinatario, etichetta, template, nota, esito);
        add(content);

        Binder<Richiesta> binder = new Binder<>(Richiesta.class);
        binder.forField(destinatario).asRequired("Inserisci il destinatario")
            .withValidator(new EmailValidator("Indirizzo email non valido"))
            .bind(Richiesta::getDestinatario, Richiesta::setDestinatario);
        Richiesta richiesta = new Richiesta();
        binder.readBean(richiesta);

        Button invia = new Button("Invia", VaadinIcon.ENVELOPE.create(), e -> {
            if (binder.writeBeanIfValid(richiesta)) {
                invia(emailService, richiesta.getDestinatario(), username);
            }
        });
        invia.addThemeVariants(ButtonVariant.PRIMARY);
        getFooter().add(ViewSupport.closeButton(this, "Chiudi"), invia);
    }

    private void invia(EmailService emailService, String a, String username) {
        TipoDocumento tipo = template.getValue() != null ? template.getValue().orElse(null) : null;
        try {
            EsitoTest risultato = emailService.inviaTest(a, tipo, username);
            String testo = switch (risultato.esito()) {
                case INVIATA -> "Inviata a " + a + ": \"" + risultato.oggetto() + "\". Controlla la casella di posta.";
                case SIMULATA -> "Invio simulato a " + a + " (SMTP non configurato).";
                case ERRORE -> "Invio non riuscito: " + risultato.errore();
            };
            Badge badge = switch (risultato.esito()) {
                case INVIATA -> new Badge("Inviata", VaadinIcon.CHECK.create());
                case SIMULATA -> new Badge("Simulata", VaadinIcon.FLASK.create());
                case ERRORE -> new Badge("Errore", VaadinIcon.WARNING.create());
            };
            badge.addThemeVariants(switch (risultato.esito()) {
                case INVIATA -> BadgeVariant.SUCCESS;
                case SIMULATA -> BadgeVariant.CONTRAST;
                case ERRORE -> BadgeVariant.ERROR;
            });
            esito.removeAll();
            esito.add(badge, new Span(testo));
            if (risultato.esito() == Esito.ERRORE) {
                Notification.show("Invio dell'email di test non riuscito");
            }
        } catch (RuntimeException ex) {
            esito.removeAll();
            esito.add(new Span("Invio non possibile: " + ex.getMessage()));
        }
    }

    private static Span riga(String testo) {
        return new Span(testo);
    }

    private static boolean vuoto(String valore) {
        return valore == null || valore.isBlank();
    }
}
