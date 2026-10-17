package it.attocchi.views;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.server.menu.MenuEntry;
import it.attocchi.security.SecurityService;
import it.attocchi.service.DashboardService;
import it.attocchi.views.insegnante.LezioniInsegnanteView;
import jakarta.annotation.security.PermitAll;

/**
 * Layout principale. Il menu è costruito dalle view annotate con {@code @Menu};
 * {@link MenuConfiguration} restituisce solo le voci accessibili all'utente corrente.
 */
@PermitAll
public class MainLayout extends AppLayout implements BeforeEnterObserver {

    /** Logo dell'associazione (risorsa statica, accessibile anche senza login). */
    public static final String LOGO = "images/logo-mosson.png";
    public static final String LOGO_ALT = "MOSSON Drum & Bugle Corps";

    /** Attributo di sessione: la prima navigazione dopo il login è già avvenuta. */
    private static final String PAGINA_INIZIALE_DECISA = MainLayout.class.getName() + ".paginaInizialeDecisa";

    private final SecurityService securityService;
    private final DashboardService dashboardService;

    public MainLayout(SecurityService securityService, DashboardService dashboardService) {
        this.securityService = securityService;
        this.dashboardService = dashboardService;
        setPrimarySection(Section.DRAWER);
        createHeader();
        addToDrawer(createDrawer());
    }

    /**
     * Pagina iniziale: alla prima navigazione della sessione (dopo il login) l'insegnante che ha solo quel ruolo
     * e un solo corso attivo va direttamente a "Le Mie Lezioni" invece della Home. Dopo, la Home resta raggiungibile.
     */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        VaadinSession session = VaadinSession.getCurrent();
        if (session.getAttribute(PAGINA_INIZIALE_DECISA) != null) {
            return;
        }
        session.setAttribute(PAGINA_INIZIALE_DECISA, Boolean.TRUE);
        if (event.getNavigationTarget() == HomeView.class && soloInsegnanteConUnCorso()) {
            event.forwardTo(LezioniInsegnanteView.class);
        }
    }

    private boolean soloInsegnanteConUnCorso() {
        if (!securityService.hasRole("INSEGNANTE") || securityService.hasRole("ADMIN") || securityService.hasRole("SEGRETERIA")
                || securityService.hasRole("ALLIEVO") || securityService.hasRole("GENITORE")) {
            return false;
        }
        return securityService.getUsername().map(dashboardService::corsiInsegnante).orElse(0L) == 1;
    }

    private void createHeader() {
        H1 title = new H1("Gestione Corsi Musica");
        // Il titolo si accorcia con i puntini invece di spingere fuori il pulsante Logout
        title.addClassNames("text-(length:--aura-font-size-l) m-0 min-w-0 truncate");

        Button logout = new Button("Logout", e -> securityService.logout());
        logout.addThemeVariants(ButtonVariant.TERTIARY);

        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), title, logout);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(title);
        header.setWidthFull();
        header.addClassNames("px-4 min-w-0");

        addToNavbar(header);
    }

    /** Menu scorrevole in alto e logo fisso in fondo al drawer (desktop e mobile). */
    private Div createDrawer() {
        Scroller menu = new Scroller(createSideNav());
        menu.addClassNames("flex-auto min-h-0");

        Image logo = new Image(LOGO, LOGO_ALT);
        logo.addClassNames(Stili.LOGO, "flex-none w-[calc(100%_-_2rem)] max-w-56 m-4");

        Div drawer = new Div(menu, logo);
        drawer.addClassNames("flex flex-col h-full min-h-0");
        return drawer;
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
