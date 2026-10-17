package it.attocchi.views;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("login")
@PageTitle("Login | Gestione Corsi")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final LoginForm login = new LoginForm();

    public LoginView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        login.setAction("login");

        Image logo = new Image(MainLayout.LOGO, MainLayout.LOGO_ALT);
        logo.addClassNames(Stili.LOGO, "w-[min(22rem,85vw)] max-w-full mb-3");

        H1 title = new H1("Gestione Corsi Musica");
        // Dimensione fluida tra smartphone e desktop
        title.addClassNames("text-[length:clamp(1.5rem,1rem_+_2.5vw,2.25rem)] leading-[1.2] text-center m-0");
        add(logo, title, login);
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
