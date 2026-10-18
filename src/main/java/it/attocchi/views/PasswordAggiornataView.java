package it.attocchi.views;

import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/** Conferma dopo la nuova password, con il pulsante per andare all'accesso. */
@Route("password-aggiornata")
@PageTitle("Password aggiornata | Gestione Corsi")
@AnonymousAllowed
public class PasswordAggiornataView extends PaginaPubblica {

    public PasswordAggiornataView() {
        super("Password aggiornata");
        Paragraph messaggio = new Paragraph("La tua password è stata cambiata. Ora puoi accedere con la nuova password.");
        messaggio.addClassNames("m-0");
        var accedi = RecuperoPasswordView.tornaAllAccesso(ButtonVariant.PRIMARY);
        accedi.setText("Vai all'accesso");
        contenuto.add(messaggio, accedi);
    }
}
