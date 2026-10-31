package it.attocchi.views;

import java.time.ZoneId;
import java.time.LocalDate;
import com.vaadin.flow.component.html.Span;
import org.springframework.boot.info.BuildProperties;
import org.springframework.beans.factory.ObjectProvider;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.server.menu.MenuEntry;
import it.attocchi.security.SecurityService;
import it.attocchi.service.AllievoService;
import it.attocchi.service.DashboardService;
import it.attocchi.views.allievo.MieiPagamentiView;
import it.attocchi.views.insegnante.LezioniInsegnanteView;
import jakarta.annotation.security.PermitAll;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Layout principale. Il menu è costruito dalle view annotate con {@code @Menu};
 * {@link MenuConfiguration} restituisce solo le voci accessibili all'utente corrente.
 */
@PermitAll
public class MainLayout extends AppLayout implements BeforeEnterObserver, AfterNavigationObserver {

    /** Logo dell'associazione (risorsa statica, accessibile anche senza login). */
    public static final String LOGO = "images/logo-mosson.png";
    public static final String LOGO_ALT = "MOSSON Drum & Bugle Corps";
    /** Nome dell'applicazione: intestazione, pagina di accesso e titolo delle schede del browser. */
    public static final String NOME_APP = "Area Corsi Mosson";
    /** Nome sotto l'icona della scorciatoia sul telefono (massimo 12 caratteri). */
    public static final String NOME_BREVE = "Corsi Mosson";

    /** Attributo di sessione: la prima navigazione dopo il login è già avvenuta. */
    private static final String PAGINA_INIZIALE_DECISA = MainLayout.class.getName() + ".paginaInizialeDecisa";

    private final SecurityService securityService;
    private final DashboardService dashboardService;
    private final AllievoService allievoService;
    /** Versione e data della build (goal build-info nel pom); assente se l'app non è stata costruita con Maven. */
    private final BuildProperties build;
    /** Gruppi richiudibili del menu con i percorsi delle loro voci. */
    private final Map<SideNav, List<String>> richiudibili = new HashMap<>();

    public MainLayout(SecurityService securityService, DashboardService dashboardService, AllievoService allievoService,
                      ObjectProvider<BuildProperties> build) {
        this.build = build.getIfAvailable();
        this.securityService = securityService;
        this.dashboardService = dashboardService;
        this.allievoService = allievoService;
        setPrimarySection(Section.DRAWER);
        createHeader();
        addToDrawer(createDrawer());
    }

    /**
     * Pagina iniziale: alla prima navigazione della sessione (dopo il login) l'insegnante che ha solo quel ruolo
     * e un solo corso attivo va direttamente a "Le mie lezioni" invece della Home. Dopo, la Home resta raggiungibile.
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
        H1 title = new H1(NOME_APP);
        // Il titolo si accorcia con i puntini invece di spingere fuori il pulsante Logout
        title.addClassNames("grow text-(length:--aura-font-size-l) m-0 min-w-0 truncate");

        Button logout = new Button("Logout", e -> securityService.logout());
        logout.addThemeVariants(ButtonVariant.TERTIARY);

        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), title, logout);
        header.addClassNames("w-full items-center px-4 min-w-0");

        // Sotto l'intestazione, su smartphone, l'invito a creare la scorciatoia nella schermata Home
        ScorciatoiaHome scorciatoia = ScorciatoiaHome.banner();
        Div banner = new Div(scorciatoia);
        banner.addClassNames("px-2 pb-2 has-[>.hidden]:hidden");
        Div navbar = new Div(header, banner);
        navbar.addClassNames("flex flex-col w-full min-w-0");

        addToNavbar(navbar);
    }

    /** Menu scorrevole in alto e logo fisso in fondo al drawer (desktop e mobile). */
    private Div createDrawer() {
        Scroller menu = new Scroller(createSideNav());
        menu.addClassNames("flex-auto min-h-0");

        Image logo = new Image(LOGO, LOGO_ALT);
        logo.addClassNames(Stili.LOGO, "flex-none w-[calc(100%_-_2rem)] max-w-56 m-4");

        Div drawer = new Div(menu, ScorciatoiaHome.voceMenu(), logo);
        drawer.addClassNames("flex flex-col h-full min-h-0");
        // Versione e data della release sotto il logo, solo per admin e segreteria
        if (securityService.hasRole("ADMIN") || securityService.hasRole("SEGRETERIA")) {
            Span versione = new Span(versione(build));
            versione.addClassNames(Stili.TESTO_SECONDARIO, "flex-none px-4 pb-3 -mt-2 text-(length:--aura-font-size-xs)");
            drawer.add(versione);
        }
        return drawer;
    }

    /** "Versione 1.1.0 · 27/10/2026" dalla build, o "Versione di sviluppo" senza build-info. */
    static String versione(BuildProperties build) {
        if (build == null || build.getTime() == null) {
            return "Versione di sviluppo";
        }
        return "Versione " + build.getVersion() + " · "
            + Formati.dataNumerica(LocalDate.ofInstant(build.getTime(), ZoneId.of("Europe/Rome")));
    }

    /** Menu a gruppi per ruolo ({@link MenuApp}): un {@link SideNav} per gruppo, con titolo dove serve. */
    private Div createSideNav() {
        // "Pagamenti" dell'allievo solo per chi li gestisce da sé (allievo senza genitore); "Documenti" a chi ne ha:
        // staff, genitori e allievi senza genitore (non all'allievo con genitore: sono del genitore)
        boolean allievoAdulto = allievoService.gestisceDaSe(securityService.getUsername().orElse(null));
        boolean conDocumenti = allievoAdulto || securityService.hasRole("ADMIN") || securityService.hasRole("SEGRETERIA")
            || securityService.hasRole("GENITORE") || securityService.hasRole("INSEGNANTE");
        List<MenuEntry> voci = MenuConfiguration.getMenuEntries().stream()
            .filter(entry -> {
                String percorso = entry.path().replaceFirst("^/", "");
                return (allievoAdulto || !percorso.equals(MieiPagamentiView.PERCORSO))
                    && (conDocumenti || !percorso.equals(DocumentiView.PERCORSO));
            })
            .toList();
        Set<String> ruoli = Stream.of("ADMIN", "SEGRETERIA", "INSEGNANTE", "GENITORE", "ALLIEVO")
            .filter(securityService::hasRole).collect(Collectors.toSet());
        Div menu = new Div();
        menu.addClassNames("flex flex-col gap-4 py-2");
        for (MenuApp.Sezione sezione : MenuApp.organizza(voci, ruoli)) {
            SideNav nav = new SideNav();
            if (sezione.titolo() != null) {
                nav.setLabel(sezione.titolo());
            }
            if (sezione.gruppo().isRichiudibile()) {
                nav.setCollapsible(true);
                nav.setExpanded(false);
                richiudibili.put(nav, sezione.voci().stream().map(v -> v.path().replaceFirst("^/", "")).toList());
            }
            sezione.voci().forEach(voce -> nav.addItem(createSideNavItem(voce)));
            menu.add(nav);
        }
        return menu;
    }

    /** Un gruppo richiudibile si apre da sé quando si arriva su una sua pagina (es. da un collegamento). */
    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        String percorso = event.getLocation().getFirstSegment();
        richiudibili.forEach((nav, percorsi) -> {
            if (percorsi.contains(percorso)) {
                nav.setExpanded(true);
            }
        });
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
