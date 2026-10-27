package it.attocchi.views.admin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.VaadinService;
import it.attocchi.entity.User;
import it.attocchi.security.SecurityService;
import it.attocchi.security.SessioniUtenti;
import it.attocchi.service.RecuperoPasswordService;
import it.attocchi.service.RecuperoPasswordService.AnteprimaInvito;
import it.attocchi.service.RecuperoPasswordService.TipoInvito;
import it.attocchi.views.Formati;
import it.attocchi.views.Stili;
import it.attocchi.views.ViewSupport;

import java.util.List;
import java.util.Optional;

/**
 * Pulsante delle righe di Gestione Utenti, Gestione Allievi, Genitori e Insegnanti: "Invita ad accedere" per chi non è
 * mai entrato, "Reimposta accesso" (solo admin) per chi è già entrato ({@link RecuperoPasswordService#invita}). Apre un
 * dialog con destinatario, anteprima del messaggio e invio; dopo una reimpostazione chiude le sessioni dell'utente.
 */
public final class InvitoUI {

    private InvitoUI() {
    }

    /** {@code null} se la persona non ha un utente. */
    public static Button pulsante(User user, Runnable dopoInvito) {
        if (user == null) {
            return null;
        }
        String corrente = servizio(SecurityService.class).getUsername().orElse("");
        Optional<TipoInvito> tipo = servizio(RecuperoPasswordService.class).tipoInvito(user, corrente);
        boolean reimposta = tipo.orElse(null) == TipoInvito.REIMPOSTAZIONE;
        Button pulsante = ViewSupport.iconButton(reimposta ? VaadinIcon.KEY : VaadinIcon.PAPERPLANE,
            reimposta ? "Reimposta accesso" : "Invita ad accedere", e -> apri(user, dopoInvito));
        pulsante.setEnabled(tipo.isPresent());
        String ultimo = user.getInvitatoIl() != null ? " (ultimo invio " + Formati.dataOra(user.getInvitatoIl()) + ")" : "";
        pulsante.setTooltipText(user.getEmail() == null || user.getEmail().isBlank() ? "Senza email: non si può invitare"
            : user.getUsername().equals(corrente) ? "Il tuo account: cambia la password dal profilo"
            : tipo.isEmpty() ? "È già entrato nell'app: solo l'admin può reimpostarne l'accesso"
            : reimposta ? "Reimposta accesso: blocca l'account e invia il link per una nuova password" + ultimo
            : "Invita ad accedere" + ultimo);
        return pulsante;
    }

    /** Dialog di invio: a chi va, cosa succede, anteprima del messaggio e invio. */
    private static void apri(User user, Runnable dopoInvito) {
        RecuperoPasswordService service = servizio(RecuperoPasswordService.class);
        String corrente = servizio(SecurityService.class).getUsername().orElse("");
        AnteprimaInvito anteprima;
        try {
            anteprima = service.anteprimaInvito(user.getId(), corrente);
        } catch (RuntimeException ex) {
            Notification.show("Invio non possibile: " + ex.getMessage());
            return;
        }
        boolean reimposta = anteprima.tipo() == TipoInvito.REIMPOSTAZIONE;
        Dialog dialog = ViewSupport.dialog((reimposta ? "Reimposta accesso – " : "Invita ad accedere – ")
            + user.getNomeCompleto(), "520px");
        Span a = new Span("A: " + user.getNomeCompleto() + " <" + anteprima.destinatario() + ">");
        Span cosa = new Span(reimposta
            ? "L'account viene bloccato subito: la password attuale non vale più e chi è collegato esce. Con il link "
                + "dell'email (valido " + RecuperoPasswordService.VALIDITA_INVITO_GIORNI + " giorni) sceglie una nuova "
                + "password ed entra."
            : "Con il link dell'email (valido " + RecuperoPasswordService.VALIDITA_INVITO_GIORNI + " giorni) sceglie la "
                + "password, l'account si attiva ed entra.");
        cosa.addClassNames(Stili.TESTO_SECONDARIO);
        VerticalLayout contenuto = new VerticalLayout(a, cosa);
        contenuto.addClassNames("p-0");
        Button vedi = new Button("Anteprima", VaadinIcon.EYE.create(), e -> mostraAnteprima(anteprima));
        Button invia = new Button(reimposta ? "Reimposta e invia" : "Invia invito", VaadinIcon.ENVELOPE.create(), e -> {
            try {
                service.invita(user.getId(), corrente);
                if (reimposta) {
                    servizio(SessioniUtenti.class).chiudi(List.of(user.getUsername()));
                }
                dialog.close();
                Notification.show((reimposta ? "Accesso reimpostato, link inviato a " : "Invito inviato a ") + user.getEmail());
                dopoInvito.run();
            } catch (RuntimeException ex) {
                Notification.show("Invio non riuscito: " + ex.getMessage());
            }
        });
        invia.addThemeVariants(ButtonVariant.PRIMARY);
        if (reimposta) {
            invia.addThemeVariants(ButtonVariant.ERROR);
        }
        dialog.add(contenuto);
        dialog.getFooter().add(ViewSupport.closeButton(dialog, "Annulla"), vedi, invia);
        dialog.open();
    }

    private static void mostraAnteprima(AnteprimaInvito anteprima) {
        Dialog dialog = ViewSupport.dialog("Anteprima – " + anteprima.oggetto(), "760px");
        Span nota = new Span("Anteprima: l'email non viene inviata; il link vero si crea all'invio.");
        nota.addClassNames(Stili.TESTO_SECONDARIO);
        // Il testo dell'email in un riquadro isolato dalla pagina (nessuno script)
        IFrame corpo = new IFrame();
        corpo.setSrcdoc(anteprima.corpoHtml());
        corpo.setSandbox();
        corpo.setTitle("Testo dell'email");
        corpo.addClassNames("w-full h-[60vh] border border-(--vaadin-border-color-secondary) rounded-(--vaadin-radius-m)");
        VerticalLayout contenuto = new VerticalLayout(nota, new Span("A: " + anteprima.destinatario()),
            new Span("Oggetto: " + anteprima.oggetto()), corpo);
        contenuto.addClassNames("p-0 gap-2");
        dialog.add(contenuto);
        dialog.getFooter().add(ViewSupport.closeButton(dialog, "Chiudi"));
        dialog.open();
    }

    private static <T> T servizio(Class<T> tipo) {
        return VaadinService.getCurrent().getInstantiator().getOrCreate(tipo);
    }
}
