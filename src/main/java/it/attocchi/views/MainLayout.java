package it.attocchi.views;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.theme.lumo.LumoUtility;
import it.attocchi.security.SecurityService;
import it.attocchi.views.admin.CorsiView;
import it.attocchi.views.admin.StudentiView;
import it.attocchi.views.admin.UtentiView;
import it.attocchi.views.docente.LezioniDocenteView;
import it.attocchi.views.genitore.FigliView;
import it.attocchi.views.studente.MieiCorsiView;

public class MainLayout extends AppLayout {

    private final SecurityService securityService;

    public MainLayout(SecurityService securityService) {
        this.securityService = securityService;
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        H1 logo = new H1("Gestione Corsi Musica");
        logo.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.MEDIUM);

        Button logout = new Button("Logout", e -> securityService.logout());

        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), logo, logout);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(logo);
        header.setWidthFull();
        header.addClassNames(LumoUtility.Padding.Vertical.NONE, LumoUtility.Padding.Horizontal.MEDIUM);

        addToNavbar(header);
    }

    private void createDrawer() {
        VerticalLayout menuLayout = new VerticalLayout();

        if (securityService.hasRole("ADMIN")) {
            menuLayout.add(new RouterLink("Gestione Corsi", CorsiView.class));
            menuLayout.add(new RouterLink("Gestione Studenti", StudentiView.class));
            menuLayout.add(new RouterLink("Gestione Utenti", UtentiView.class));
        }

        if (securityService.hasRole("DOCENTE")) {
            menuLayout.add(new RouterLink("Le Mie Lezioni", LezioniDocenteView.class));
        }

        if (securityService.hasRole("STUDENTE")) {
            menuLayout.add(new RouterLink("I Miei Corsi", MieiCorsiView.class));
        }

        if (securityService.hasRole("GENITORE")) {
            menuLayout.add(new RouterLink("I Miei Figli", FigliView.class));
        }

        addToDrawer(menuLayout);
    }
}
