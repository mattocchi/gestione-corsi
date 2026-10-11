package it.attocchi.views.admin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.VaadinService;
import it.attocchi.entity.Allievo;
import it.attocchi.entity.Corso;
import it.attocchi.entity.InvioEmail;
import it.attocchi.entity.TipoDocumento;
import it.attocchi.service.EmailService;
import it.attocchi.service.EmailService.Destinatario;
import it.attocchi.service.EmailService.Riepilogo;
import it.attocchi.views.ViewSupport;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Invio per email della conferma di iscrizione o della ricevuta di un partecipante (admin e segreteria),
 * con il destinatario in evidenza; e conferma dell'invio multiplo delle conferme di un corso.
 */
public final class InvioEmailDialog {

    private InvioEmailDialog() {
    }

    /** Apre il dialog di invio per l'iscrizione; {@code dopoInvio} è eseguito dopo ogni invio. */
    public static void apri(Corso corso, Allievo allievo, Runnable dopoInvio) {
        EmailService emailService = emailService();
        String username = username();
        Dialog dialog = ViewSupport.dialog("Invia email – " + allievo.getNomeCompleto(), "460px");
        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.add(new Span(corso.getNome() + (corso.getAnnoScolastico() != null ? " " + corso.getAnnoScolastico() : "")));

        Optional<Destinatario> destinatario = emailService.destinatario(corso.getId(), allievo.getId());
        Span a = new Span(destinatario.map(d -> "A: " + d.nome() + " <" + d.email() + ">")
            .orElse("Né il genitore né l'allievo hanno un indirizzo email: aggiungilo nella loro scheda per poter inviare."));
        a.addClassName("avviso-sola-lettura");
        content.add(a);
        if (!emailService.invioReale()) {
            Span simulato = new Span("Server email non configurato: l'invio sarà solo simulato e registrato.");
            simulato.addClassName("avviso-sola-lettura");
            content.add(simulato);
        }

        Button conferma = new Button("Invia conferma di iscrizione", VaadinIcon.ENVELOPE.create(),
            e -> invia(emailService, TipoDocumento.CONFERMA_ISCRIZIONE, corso, allievo, username, dialog, dopoInvio));
        conferma.addThemeVariants(ButtonVariant.PRIMARY);
        Button ricevuta = new Button("Invia ricevuta", VaadinIcon.ENVELOPE.create(),
            e -> invia(emailService, TipoDocumento.RICEVUTA, corso, allievo, username, dialog, dopoInvio));
        boolean conPagamenti = emailService.haPagamenti(corso.getId(), allievo.getId());
        conferma.setEnabled(destinatario.isPresent());
        ricevuta.setEnabled(destinatario.isPresent() && conPagamenti);
        if (!conPagamenti) {
            ricevuta.setTooltipText("Nessun pagamento registrato");
        }
        conferma.setWidthFull();
        ricevuta.setWidthFull();
        content.add(conferma, ricevuta);
        dialog.add(content);
        dialog.getFooter().add(ViewSupport.closeButton(dialog, "Chiudi"));
        dialog.open();
    }

    /** Chiede conferma e invia la conferma di iscrizione a tutti gli iscritti del corso con un indirizzo email. */
    public static void inviaConferme(Corso corso, int iscritti, Runnable dopoInvio) {
        EmailService emailService = emailService();
        String username = username();
        Dialog dialog = ViewSupport.dialog("Inviare le conferme di iscrizione?", "460px");
        dialog.add(new Span("La conferma di iscrizione al corso " + corso.getNome() + " sarà inviata per email ai "
            + iscritti + " iscritti (al genitore o, se manca, all'allievo)."
            + (emailService.invioReale() ? "" : " Server email non configurato: l'invio sarà solo simulato.")));
        Button invia = new Button("Invia", VaadinIcon.ENVELOPE.create(), e -> {
            try {
                Riepilogo r = emailService.inviaConferme(corso.getId(), username);
                dialog.close();
                Notification.show("Conferme: " + r.inviate() + " inviate, " + r.simulate() + " simulate, " + r.errori()
                    + " con errore, " + r.senzaEmail() + " senza email");
                dopoInvio.run();
            } catch (RuntimeException ex) {
                Notification.show("Invio non riuscito: " + ex.getMessage());
            }
        });
        invia.addThemeVariants(ButtonVariant.PRIMARY);
        dialog.getFooter().add(ViewSupport.closeButton(dialog, "Annulla"), invia);
        dialog.open();
    }

    private static void invia(EmailService emailService, TipoDocumento tipo, Corso corso, Allievo allievo, String username,
                              Dialog dialog, Runnable dopoInvio) {
        try {
            InvioEmail invio = emailService.invia(tipo, corso.getId(), allievo.getId(), username);
            switch (invio.getEsito()) {
                case INVIATA -> Notification.show(tipo.getTitolo() + " inviata a " + invio.getDestinatario());
                case SIMULATA -> Notification.show(tipo.getTitolo() + ": invio simulato a " + invio.getDestinatario());
                case ERRORE -> Notification.show("Invio non riuscito: " + invio.getErrore());
            }
            dialog.close();
        } catch (RuntimeException ex) {
            Notification.show("Invio non possibile: " + ex.getMessage());
        }
        dopoInvio.run();
    }

    private static EmailService emailService() {
        // Servizio Spring tramite l'Instantiator di Vaadin, come Ricevute: non va passato a ogni vista e dialog
        return VaadinService.getCurrent().getInstantiator().getOrCreate(EmailService.class);
    }

    private static String username() {
        Authentication autenticazione = SecurityContextHolder.getContext().getAuthentication();
        return autenticazione != null ? autenticazione.getName() : "";
    }
}
