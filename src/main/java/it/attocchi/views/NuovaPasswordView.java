package it.attocchi.views;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import it.attocchi.service.RecuperoPasswordService;
import it.attocchi.service.UserService;

/**
 * Pagina del link ricevuto via email ({@code nuova-password?token=…}): con un link valido si sceglie la nuova password,
 * poi si passa alla pagina di conferma; con un link scaduto, già usato o inventato si può chiederne uno nuovo.
 */
@Route("nuova-password")
@PageTitle("Nuova password")
@AnonymousAllowed
public class NuovaPasswordView extends PaginaPubblica implements BeforeEnterObserver {

    private final RecuperoPasswordService recuperoPasswordService;

    public NuovaPasswordView(RecuperoPasswordService recuperoPasswordService) {
        super("Nuova password");
        this.recuperoPasswordService = recuperoPasswordService;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String token = event.getLocation().getQueryParameters().getSingleParameter("token").orElse(null);
        contenuto.removeAll();
        recuperoPasswordService.verifica(token).ifPresentOrElse(
            username -> mostraForm(token, username),
            this::mostraLinkNonValido);
    }

    private void mostraForm(String token, String username) {
        Paragraph account = new Paragraph("Scegli una nuova password per l'account " + username + ".");
        account.addClassNames("m-0");
        PasswordField nuova = new PasswordField("Nuova password");
        nuova.setHelperText("Almeno " + UserService.PASSWORD_MINIMA + " caratteri");
        nuova.addClassNames("w-full");
        nuova.setAutofocus(true);
        PasswordField conferma = new PasswordField("Conferma password");
        conferma.addClassNames("w-full");
        Button salva = new Button("Salva password", e -> salva(token, nuova, conferma));
        salva.addThemeVariants(ButtonVariant.PRIMARY);
        salva.addClickShortcut(Key.ENTER);
        contenuto.add(account, nuova, conferma, salva);
    }

    private void salva(String token, PasswordField nuova, PasswordField conferma) {
        boolean lunga = nuova.getValue().length() >= UserService.PASSWORD_MINIMA;
        nuova.setErrorMessage("La password deve avere almeno " + UserService.PASSWORD_MINIMA + " caratteri");
        nuova.setInvalid(!lunga);
        boolean uguali = nuova.getValue().equals(conferma.getValue());
        conferma.setErrorMessage("Le password non coincidono");
        conferma.setInvalid(!uguali);
        if (!lunga || !uguali) {
            return;
        }
        try {
            recuperoPasswordService.reimposta(token, nuova.getValue());
        } catch (IllegalStateException ex) {
            // Scaduto o già usato nel frattempo
            contenuto.removeAll();
            mostraLinkNonValido();
            return;
        }
        getUI().ifPresent(ui -> ui.navigate(PasswordAggiornataView.class));
    }

    private void mostraLinkNonValido() {
        setTitolo("Link non valido");
        Paragraph messaggio = new Paragraph("Il link non è valido: è scaduto, è già stato usato oppure ne hai chiesto "
            + "uno più recente. Chiedi un nuovo link per reimpostare la password.");
        messaggio.addClassNames("m-0");
        Button nuovoLink = new Button("Chiedi un nuovo link",
            e -> getUI().ifPresent(ui -> ui.navigate(RecuperoPasswordView.class)));
        nuovoLink.addThemeVariants(ButtonVariant.PRIMARY);
        contenuto.add(messaggio, nuovoLink, RecuperoPasswordView.tornaAllAccesso(ButtonVariant.TERTIARY));
    }
}
