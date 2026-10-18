package it.attocchi.views;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import it.attocchi.service.RecuperoPasswordService;

/**
 * "Password dimenticata?": con username o email invia il link per una nuova password. Il messaggio dopo l'invio è
 * sempre lo stesso, che l'account esista o no.
 */
@Route(RecuperoPasswordView.ROUTE)
@PageTitle("Password dimenticata")
@AnonymousAllowed
public class RecuperoPasswordView extends PaginaPubblica {

    public static final String ROUTE = "recupero-password";

    private final RecuperoPasswordService recuperoPasswordService;

    public RecuperoPasswordView(RecuperoPasswordService recuperoPasswordService) {
        super("Password dimenticata?");
        this.recuperoPasswordService = recuperoPasswordService;

        Paragraph spiegazione = new Paragraph("Inserisci il tuo nome utente o l'email del tuo account: ti invieremo "
            + "un link per scegliere una nuova password.");
        spiegazione.addClassNames("m-0");
        TextField account = new TextField("Nome utente o email");
        account.addClassNames("w-full");
        account.setAutofocus(true);
        Button invia = new Button("Invia link", e -> invia(account));
        invia.addThemeVariants(ButtonVariant.PRIMARY);
        invia.addClickShortcut(Key.ENTER);
        contenuto.add(spiegazione, account, invia, tornaAllAccesso(ButtonVariant.TERTIARY));
    }

    private void invia(TextField account) {
        if (account.getValue().isBlank()) {
            account.setErrorMessage("Inserisci il nome utente o l'email");
            account.setInvalid(true);
            return;
        }
        recuperoPasswordService.richiedi(account.getValue());
        setTitolo("Controlla la tua email");
        Paragraph inviata = new Paragraph("Se l'account esiste ed è collegato a un'email attiva, riceverai a breve un "
            + "link per scegliere una nuova password. Il link vale " + recuperoPasswordService.validitaOre()
            + " ore e si può usare una sola volta.");
        inviata.addClassNames("m-0");
        contenuto.removeAll();
        contenuto.add(inviata, tornaAllAccesso(ButtonVariant.PRIMARY));
    }

    static Button tornaAllAccesso(ButtonVariant variante) {
        Button login = new Button("Torna all'accesso", e -> e.getSource().getUI().ifPresent(ui -> ui.navigate(LoginView.class)));
        login.addThemeVariants(variante);
        return login;
    }
}
