package it.attocchi.views.admin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.server.VaadinService;
import it.attocchi.entity.User;
import it.attocchi.security.SecurityService;
import it.attocchi.service.RecuperoPasswordService;
import it.attocchi.views.Formati;
import it.attocchi.views.ViewSupport;

/**
 * Pulsante "Invita ad accedere" delle righe di Gestione Utenti, Gestione Allievi, Genitori e Insegnanti: manda l'email
 * con il link per impostare la password ({@link RecuperoPasswordService#invita}). Attivo solo per chi ha un'email e non
 * è mai entrato; il tooltip dice l'ultimo invito.
 */
public final class InvitoUI {

    private InvitoUI() {
    }

    /** {@code null} se la persona non ha un utente. */
    public static Button pulsante(User user, Runnable dopoInvito) {
        if (user == null) {
            return null;
        }
        Button invita = ViewSupport.iconButton(VaadinIcon.PAPERPLANE, "Invita ad accedere", e -> {
            try {
                servizio(RecuperoPasswordService.class).invita(user.getId(),
                    servizio(SecurityService.class).getUsername().orElse(""));
                Notification.show("Invito inviato a " + user.getEmail());
                dopoInvito.run();
            } catch (RuntimeException ex) {
                Notification.show("Invito non inviato: " + ex.getMessage());
            }
        });
        boolean invitabile = RecuperoPasswordService.invitabile(user);
        invita.setEnabled(invitabile);
        invita.setTooltipText(!invitabile
            ? (user.getUltimoAccesso() != null ? "È già entrato nell'app" : "Senza email: non si può invitare")
            : user.getInvitatoIl() != null ? "Invita di nuovo (ultimo invito " + Formati.dataOra(user.getInvitatoIl()) + ")"
            : "Invita ad accedere");
        return invita;
    }

    private static <T> T servizio(Class<T> tipo) {
        return VaadinService.getCurrent().getInstantiator().getOrCreate(tipo);
    }
}
