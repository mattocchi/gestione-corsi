package it.attocchi.views;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.server.menu.MenuEntry;
import it.attocchi.security.SecurityService;
import jakarta.annotation.security.PermitAll;

/**
 * Layout principale. Il menu è costruito dalle view annotate con {@code @Menu};
 * {@link MenuConfiguration} restituisce solo le voci accessibili all'utente corrente.
 */
@PermitAll
public class MainLayout extends AppLayout {

    /** Logo dell'associazione (risorsa statica, accessibile anche senza login). */
    public static final String LOGO = "images/logo-mosson.png";
    public static final String LOGO_ALT = "MOSSON Drum & Bugle Corps";

    private final SecurityService securityService;

    public MainLayout(SecurityService securityService) {
        this.securityService = securityService;
        setPrimarySection(Section.DRAWER);
        createHeader();
        addToDrawer(new Scroller(createSideNav()));
    }

    private void createHeader() {
        H1 title = new H1("Gestione Corsi Musica");
        title.addClassName("app-title");

        Button logout = new Button("Logout", e -> securityService.logout());
        logout.addThemeVariants(ButtonVariant.TERTIARY);

        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), title, logout);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(title);
        header.setWidthFull();
        header.addClassName("app-header");

        addToNavbar(header);
    }

    private SideNav createSideNav() {
        SideNav nav = new SideNav();
        MenuConfiguration.getMenuEntries().forEach(entry -> nav.addItem(createSideNavItem(entry)));
        return nav;
    }

    private SideNavItem createSideNavItem(MenuEntry entry) {
        SideNavItem item = new SideNavItem(entry.title(), entry.path());
        item.setMatchNested(true);
        if (entry.icon() != null) {
            item.setPrefixComponent(new Icon(entry.icon()));
        }
        return item;
    }
}
