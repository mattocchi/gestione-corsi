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
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import it.attocchi.entity.User;
import it.attocchi.security.SecurityService;
import it.attocchi.service.DashboardService;
import it.attocchi.service.DashboardService.RiepilogoGenerale;
import it.attocchi.service.DashboardService.RiepilogoGruppi;
import it.attocchi.service.DashboardService.RigaGruppo;
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
        // Riquadri che vanno a capo, uno per riga sugli smartphone
        addClassNames("gap-6");

        String username = securityService.getUsername().orElse("");
        User user = userService.findByUsername(username);
        H1 title = new H1("Ciao " + (user != null ? user.getNome() : username) + "!");
        title.addClassNames("text-[length:clamp(1.5rem,1rem_+_2vw,2rem)] leading-[1.2] m-0");
        add(title);

        if (securityService.hasRole("ADMIN") || securityService.hasRole("SEGRETERIA")) {
            RiepilogoGenerale riepilogo = dashboardService.riepilogoGenerale();
            add(sezione("Riepilogo generale",
                indicatore(VaadinIcon.MUSIC, "Corsi", riepilogo.corsi()),
                indicatore(VaadinIcon.ACADEMY_CAP, "Allievi", riepilogo.allievi()),
                indicatore(VaadinIcon.FAMILY, "Genitori", riepilogo.genitori()),
                indicatore(VaadinIcon.USER_STAR, "Insegnanti", riepilogo.insegnanti())));
            add(pagamenti(pagamentoService.riepilogoCorsiAttivi()));
            add(gruppi(dashboardService.riepilogoGruppi()));
        }
        if (securityService.hasRole("INSEGNANTE")) {
            RiepilogoInsegnante riepilogo = dashboardService.riepilogoInsegnante(username);
            Div sezione = sezione("I miei corsi",
                indicatore(VaadinIcon.MUSIC, "Corsi assegnati", riepilogo.corsi()),
                indicatore(VaadinIcon.CHECK, "Lezioni fatte", riepilogo.lezioniFatte()),
                indicatore(VaadinIcon.CLOCK, "Lezioni da fare", riepilogo.lezioniDaFare()),
                indicatore(VaadinIcon.BAN, "Lezioni non effettuate", riepilogo.lezioniNonEffettuate()));
            // I gruppi dei suoi corsi, solo se ce ne sono
            List<RigaGruppo> gruppi = dashboardService.gruppiDellInsegnante(username);
            if (!gruppi.isEmpty()) {
                sezione.add(tabellaGruppi(gruppi));
            }
            add(sezione);
        }
        if (securityService.hasRole("ALLIEVO")) {
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
        grid.addColumn(RiepilogoPagamenti::corso).setHeader("Corso");
        grid.addColumn(RiepilogoPagamenti::saldati).setHeader("Saldati").setWidth("7rem").setFlexGrow(0);
        grid.addColumn(RiepilogoPagamenti::parziali).setHeader("Parziali").setWidth("7rem").setFlexGrow(0);
        grid.addColumn(RiepilogoPagamenti::senzaPagamenti).setHeader("Senza pagamenti").setWidth("10rem").setFlexGrow(0);
        grid.addColumn(r -> importo(r.incassato(), r.percentualeIncassata())).setHeader("Incassato");
        grid.addColumn(r -> importo(r.daIncassare(), r.percentualeDaIncassare())).setHeader("Da incassare");
        CardMobile.per(grid)
            .titolo(RiepilogoPagamenti::corso)
            .campo("Saldati", RiepilogoPagamenti::saldati)
            .campo("Parziali", RiepilogoPagamenti::parziali)
            .campo("Senza pagamenti", RiepilogoPagamenti::senzaPagamenti)
            .campo("Incassato", r -> importo(r.incassato(), r.percentualeIncassata()))
            .campo("Da incassare", r -> importo(r.daIncassare(), r.percentualeDaIncassare()))
            .applica();
        grid.setItems(riepilogo.subList(0, riepilogo.size() - 1));
        grid.setAllRowsVisible(true);
        grid.setEmptyStateText("Nessun corso attivo");
        grid.addClassNames("mt-4");
        sezione.add(grid);
        return sezione;
    }

    /** Gruppi dei corsi attivi: riquadri con i conteggi e una riga per gruppo. */
    private static Div gruppi(RiepilogoGruppi riepilogo) {
        Div sezione = sezione("Gruppi dei corsi attivi",
            indicatore(VaadinIcon.MUSIC, "Corsi con gruppi", riepilogo.corsiConGruppi()),
            indicatore(VaadinIcon.GROUP, "Gruppi", riepilogo.gruppi()),
            indicatore(VaadinIcon.USER, "Allievi senza gruppo", riepilogo.allieviSenzaGruppo()));
        sezione.add(tabellaGruppi(riepilogo.righe()));
        return sezione;
    }

    /** Una riga per gruppo: corso, gruppo e numero di iscritti. */
    private static Grid<RigaGruppo> tabellaGruppi(List<RigaGruppo> righe) {
        Grid<RigaGruppo> grid = new Grid<>(RigaGruppo.class, false);
        grid.addColumn(RigaGruppo::corso).setHeader("Corso");
        grid.addColumn(r -> Formati.gruppo(r.gruppo())).setHeader("Gruppo");
        grid.addColumn(RigaGruppo::iscritti).setHeader("Iscritti").setWidth("7rem").setFlexGrow(0);
        CardMobile.per(grid)
            .titolo(r -> Formati.gruppo(r.gruppo()))
            .sottotitolo(RigaGruppo::corso)
            .campo("Iscritti", RigaGruppo::iscritti)
            .applica();
        grid.setItems(righe);
        grid.setAllRowsVisible(true);
        grid.setEmptyStateText("Nessun corso attivo con gruppi");
        grid.addClassNames("mt-4");
        return grid;
    }

    private static String importo(BigDecimal importo, int percentuale) {
        return Formati.euro(importo) + " (" + percentuale + "%)";
    }

    /** Sezione con titolo e griglia di riquadri (usata anche dal bilancio degli insegnanti). */
    public static Div sezione(String titolo, Card... indicatori) {
        H2 heading = new H2(titolo);
        heading.addClassNames("text-(length:--aura-font-size-l) mt-0 mx-0 mb-3");
        Div griglia = new Div(indicatori);
        griglia.addClassNames("grid grid-cols-[repeat(auto-fill,minmax(min(100%,11rem),1fr))] gap-4");
        Div sezione = new Div(heading, griglia);
        sezione.addClassNames("w-full");
        return sezione;
    }

    static Card indicatore(VaadinIcon icona, String etichetta, long valore) {
        return indicatore(icona, etichetta, String.valueOf(valore), null);
    }

    /** Riquadro con icona, etichetta, valore e dettaglio facoltativo; l'etichetta è anche il nome accessibile. */
    public static Card indicatore(VaadinIcon icona, String etichetta, String valore, String dettaglio) {
        Card card = new Card();
        Icon icon = icona.create();
        icon.addClassNames("text-(--vaadin-text-color-secondary)");
        card.setHeaderPrefix(icon);
        card.setTitle(etichetta);
        Span numero = new Span(valore);
        numero.addClassNames("font-(--aura-font-weight-semibold) leading-[1.1]");
        card.add(numero);
        if (dettaglio == null) {
            numero.addClassNames("text-[2.25rem]");
        } else {
            // I riquadri con dettaglio mostrano importi in euro: più piccoli dei conteggi, su una riga anche se stretti
            numero.addClassNames("text-[1.6rem] whitespace-nowrap");
            Span testo = new Span(dettaglio);
            testo.addClassNames("block", Stili.TESTO_SECONDARIO);
            card.add(testo);
        }
        card.setAriaLabel(etichetta + ": " + valore + (dettaglio != null ? " (" + dettaglio + ")" : ""));
        return card;
    }
}
