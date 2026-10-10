package it.attocchi.views;

import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import it.attocchi.entity.User;
import it.attocchi.security.SecurityService;
import it.attocchi.service.DashboardService;
import it.attocchi.service.DashboardService.RiepilogoGenerale;
import it.attocchi.service.DashboardService.RiepilogoInsegnante;
import it.attocchi.service.PagamentoService;
import it.attocchi.service.PagamentoService.RiepilogoPagamenti;
import it.attocchi.service.UserService;
import jakarta.annotation.security.PermitAll;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dashboard iniziale: riquadri di riepilogo in funzione dei ruoli dell'utente
 * (chi ha più ruoli vede una sezione per ciascuno).
 */
@PermitAll
@Route(value = "", layout = MainLayout.class)
@PageTitle("Home")
@Menu(title = "Home", order = 0, icon = "vaadin:home")
public class HomeView extends VerticalLayout {

    public HomeView(SecurityService securityService, UserService userService, DashboardService dashboardService,
                    PagamentoService pagamentoService) {
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
            add(pagamenti(pagamentoService.riepilogoCorsiAttivi()));
        }
        if (securityService.hasRole("INSEGNANTE")) {
            RiepilogoInsegnante riepilogo = dashboardService.riepilogoInsegnante(username);
            add(sezione("I miei corsi",
                indicatore(VaadinIcon.MUSIC, "Corsi assegnati", riepilogo.corsi()),
                indicatore(VaadinIcon.CHECK, "Lezioni fatte", riepilogo.lezioniFatte()),
                indicatore(VaadinIcon.CLOCK, "Lezioni da fare", riepilogo.lezioniDaFare())));
        }
        if (securityService.hasRole("STUDENTE")) {
            add(sezione("I miei corsi",
                indicatore(VaadinIcon.BOOK, "Corsi a cui sono iscritto", dashboardService.corsiAllievo(username))));
        }
        if (securityService.hasRole("GENITORE")) {
            add(sezione("I miei figli",
                indicatore(VaadinIcon.BOOK, "Corsi dei figli", dashboardService.corsiDeiFigli(username))));
        }
    }

    /** Pagamenti dei corsi attivi: riquadri con i totali e una riga per corso (l'ultimo elemento è il totale). */
    private static Div pagamenti(List<RiepilogoPagamenti> riepilogo) {
        RiepilogoPagamenti totale = riepilogo.getLast();
        Div sezione = sezione("Pagamenti dei corsi attivi",
            indicatore(VaadinIcon.CHECK, "Saldati", totale.saldati()),
            indicatore(VaadinIcon.HOURGLASS, "Pagamento parziale", totale.parziali()),
            indicatore(VaadinIcon.CLOCK, "Senza pagamenti", totale.senzaPagamenti()),
            indicatore(VaadinIcon.EURO, "Incassato", Formati.euro(totale.incassato()),
                totale.percentualeIncassata() + "% del totale"),
            indicatore(VaadinIcon.WALLET, "Da incassare", Formati.euro(totale.daIncassare()),
                totale.percentualeDaIncassare() + "% del totale"));

        Grid<RiepilogoPagamenti> grid = new Grid<>(RiepilogoPagamenti.class, false);
        var corso = grid.addColumn(RiepilogoPagamenti::corso).setHeader("Corso");
        // Solo su mobile: corso, conteggi e incassato su tre righe
        var riepilogoMobile = grid.addColumn(LitRenderer.<RiepilogoPagamenti>of(
                "<div class=\"riepilogo-righe\"><div class=\"riepilogo-righe-titolo\">${item.corso}</div>"
                    + "<div class=\"riepilogo-righe-dettaglio\">${item.conteggi}</div>"
                    + "<div class=\"riepilogo-righe-dettaglio\">${item.incassato}</div></div>")
                .withProperty("corso", RiepilogoPagamenti::corso)
                .withProperty("conteggi", r -> r.saldati() + " saldati · " + r.parziali() + " parziali · "
                    + r.senzaPagamenti() + " senza pagamenti")
                .withProperty("incassato", r -> "Incassato " + importo(r.incassato(), r.percentualeIncassata())
                    + " · da incassare " + Formati.euro(r.daIncassare())))
            .setHeader("Corso");
        var saldati = grid.addColumn(RiepilogoPagamenti::saldati).setHeader("Saldati").setWidth("7rem").setFlexGrow(0);
        var parziali = grid.addColumn(RiepilogoPagamenti::parziali).setHeader("Parziali").setWidth("7rem").setFlexGrow(0);
        var senza = grid.addColumn(RiepilogoPagamenti::senzaPagamenti).setHeader("Senza pagamenti").setWidth("10rem").setFlexGrow(0);
        var incassato = grid.addColumn(r -> importo(r.incassato(), r.percentualeIncassata())).setHeader("Incassato");
        var daIncassare = grid.addColumn(r -> importo(r.daIncassare(), r.percentualeDaIncassare())).setHeader("Da incassare");
        ViewSupport.hideOnMobile(grid, corso, saldati, parziali, senza, incassato, daIncassare);
        Signal<Boolean> mobile = ViewSupport.isMobile();
        Signal.effect(grid, () -> riepilogoMobile.setVisible(mobile.get()));
        grid.setItems(riepilogo.subList(0, riepilogo.size() - 1));
        grid.setAllRowsVisible(true);
        grid.setEmptyStateText("Nessun corso attivo");
        grid.addClassName("dashboard-pagamenti");
        sezione.add(grid);
        return sezione;
    }

    private static String importo(BigDecimal importo, int percentuale) {
        return Formati.euro(importo) + " (" + percentuale + "%)";
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

    static Card indicatore(VaadinIcon icona, String etichetta, long valore) {
        return indicatore(icona, etichetta, String.valueOf(valore), null);
    }

    /** Riquadro con icona, etichetta, valore e dettaglio facoltativo; l'etichetta è anche il nome accessibile. */
    static Card indicatore(VaadinIcon icona, String etichetta, String valore, String dettaglio) {
        Card card = new Card();
        card.addClassName("dashboard-card");
        Icon icon = icona.create();
        icon.addClassName("dashboard-card-icon");
        card.setHeaderPrefix(icon);
        card.setTitle(etichetta);
        Span numero = new Span(valore);
        numero.addClassName("dashboard-valore");
        card.add(numero);
        if (dettaglio != null) {
            // I riquadri con dettaglio mostrano importi in euro, più lunghi dei conteggi
            numero.addClassName("dashboard-valore-importo");
            Span testo = new Span(dettaglio);
            testo.addClassName("dashboard-dettaglio");
            card.add(testo);
        }
        card.setAriaLabel(etichetta + ": " + valore + (dettaglio != null ? " (" + dettaglio + ")" : ""));
        return card;
    }
}
