package it.attocchi.views;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import it.attocchi.security.SecurityConfig;
import jakarta.servlet.http.Cookie;

import java.util.Arrays;

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

        add(ScorciatoiaHome.banner());
        add(PaginaPubblica.intestazione());
        // La casella sta dentro il form, sotto la password e sopra "Accedi" (prima di "Password dimenticata?"): slot
        // del componente web per i campi in più, che l'API Java del LoginForm non espone
        Checkbox ricordami = ricordami();
        ricordami.getElement().setAttribute("slot", "custom-form-area");
        login.getElement().appendChild(ricordami.getElement());
        add(login);
    }

    /**
     * Casella "Ricordami su questo dispositivo": il form di accesso è nativo e non ha campi in più, quindi la scelta
     * va al server con un cookie tecnico letto al login ({@link SecurityConfig#COOKIE_RICORDAMI}).
     */
    private static Checkbox ricordami() {
        Checkbox ricordami = new Checkbox("Ricordami su questo dispositivo");
        ricordami.setTooltipText("Resti collegato per 30 giorni; non usarlo su un computer condiviso");
        VaadinRequest richiesta = VaadinService.getCurrentRequest();
        Cookie[] cookies = richiesta != null ? richiesta.getCookies() : null;
        ricordami.setValue(cookies != null && Arrays.stream(cookies)
            .anyMatch(c -> SecurityConfig.COOKIE_RICORDAMI.equals(c.getName()) && "1".equals(c.getValue())));
        ricordami.addValueChangeListener(e -> e.getSource().getElement().executeJs("document.cookie = $0",
            SecurityConfig.COOKIE_RICORDAMI + (e.getValue() ? "=1; path=/; SameSite=Lax" : "=; path=/; max-age=0; SameSite=Lax")));
        return ricordami;
    }

    /** Testi del form di accesso in italiano (i default del componente sono in inglese). */
    static LoginI18n italiano() {
        LoginI18n.Form form = new LoginI18n.Form();
        form.setTitle("Accedi");
        form.setUsername("Nome utente o email");
        form.setPassword("Password");
        form.setSubmit("Accedi");
        form.setForgotPassword("Password dimenticata?");

        LoginI18n.ErrorMessage errore = new LoginI18n.ErrorMessage();
        errore.setTitle("Credenziali errate");
        errore.setMessage("Controlla di aver inserito nome utente (o email) e password corretti e riprova. "
            + "Se la tua email è usata da più account, accedi con il nome utente.");
        errore.setUsername("Inserisci il nome utente o l'email");
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
