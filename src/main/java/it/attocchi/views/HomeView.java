package it.attocchi.views;

import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import it.attocchi.entity.User;
import it.attocchi.security.SecurityService;
import it.attocchi.service.DashboardService;
import it.attocchi.service.DashboardService.RiepilogoGenerale;
import it.attocchi.service.UserService;
import jakarta.annotation.security.PermitAll;

/**
 * Dashboard iniziale: riquadri di riepilogo in funzione dei ruoli dell'utente
 * (chi ha più ruoli vede una sezione per ciascuno).
 */
@PermitAll
@Route(value = "", layout = MainLayout.class)
@PageTitle("Home")
@Menu(title = "Home", order = 0, icon = "vaadin:home")
public class HomeView extends VerticalLayout {

    public HomeView(SecurityService securityService, UserService userService, DashboardService dashboardService) {
        addClassName("dashboard");

        String username = securityService.getUsername().orElse("");
        User user = userService.findByUsername(username);
        H1 title = new H1("Ciao " + (user != null ? user.getNome() : username) + "!");
        title.addClassName("dashboard-title");
        add(title);

        if (securityService.hasRole("ADMIN") || securityService.hasRole("SEGRETERIA")) {
            RiepilogoGenerale riepilogo = dashboardService.riepilogoGenerale();
            add(sezione("Riepilogo generale",
                indicatore(VaadinIcon.MUSIC, "Corsi", riepilogo.corsi()),
                indicatore(VaadinIcon.ACADEMY_CAP, "Allievi", riepilogo.allievi()),
                indicatore(VaadinIcon.FAMILY, "Genitori", riepilogo.genitori()),
                indicatore(VaadinIcon.USER_STAR, "Insegnanti", riepilogo.insegnanti())));
        }
    }

    private static Div sezione(String titolo, Card... indicatori) {
        H2 heading = new H2(titolo);
        heading.addClassName("dashboard-section-title");
        Div griglia = new Div(indicatori);
        griglia.addClassName("dashboard-grid");
        Div sezione = new Div(heading, griglia);
        sezione.addClassName("dashboard-section");
        return sezione;
    }

    /** Riquadro con icona, etichetta e valore; l'etichetta è anche il nome accessibile. */
    static Card indicatore(VaadinIcon icona, String etichetta, long valore) {
        Card card = new Card();
        card.addClassName("dashboard-card");
        Icon icon = icona.create();
        icon.addClassName("dashboard-card-icon");
        card.setHeaderPrefix(icon);
        card.setTitle(etichetta);
        Span numero = new Span(String.valueOf(valore));
        numero.addClassName("dashboard-valore");
        card.add(numero);
        card.setAriaLabel(etichetta + ": " + valore);
        return card;
    }
}
