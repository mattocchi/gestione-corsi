package it.attocchi.views;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import it.attocchi.security.SecurityService;
import jakarta.annotation.security.PermitAll;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Home")
@PermitAll
public class HomeView extends VerticalLayout {

    public HomeView(SecurityService securityService) {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        H1 title = new H1("Benvenuto nel Sistema di Gestione Corsi Musica");

        String username = securityService.getAuthenticatedUser() != null
            ? securityService.getAuthenticatedUser().getUsername()
            : "Ospite";

        Paragraph welcome = new Paragraph("Ciao " + username + "! Utilizza il menu laterale per navigare.");

        add(title, welcome);
    }
}
