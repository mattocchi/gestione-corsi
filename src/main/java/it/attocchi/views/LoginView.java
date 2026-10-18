package it.attocchi.views;

import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("login")
@PageTitle("Accesso")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final LoginForm login = new LoginForm();

    public LoginView() {
        addClassNames("size-full items-center justify-center");

        login.setAction("login");
        login.setI18n(italiano());
        login.addForgotPasswordListener(e -> getUI().ifPresent(ui -> ui.navigate(RecuperoPasswordView.class)));

        add(PaginaPubblica.intestazione());
        add(login);
    }

    /** Testi del form di accesso in italiano (i default del componente sono in inglese). */
    static LoginI18n italiano() {
        LoginI18n.Form form = new LoginI18n.Form();
        form.setTitle("Accedi");
        form.setUsername("Nome utente");
        form.setPassword("Password");
        form.setSubmit("Accedi");
        form.setForgotPassword("Password dimenticata?");

        LoginI18n.ErrorMessage errore = new LoginI18n.ErrorMessage();
        errore.setTitle("Nome utente o password errati");
        errore.setMessage("Controlla di aver inserito nome utente e password corretti e riprova.");
        errore.setUsername("Inserisci il nome utente");
        errore.setPassword("Inserisci la password");

        LoginI18n i18n = new LoginI18n();
        i18n.setForm(form);
        i18n.setErrorMessage(errore);
        return i18n;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        if (beforeEnterEvent.getLocation()
            .getQueryParameters()
            .getParameters()
            .containsKey("error")) {
            login.setError(true);
        }
    }
}
