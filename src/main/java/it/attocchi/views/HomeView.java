package it.attocchi.views;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import it.attocchi.entity.GruppoCorso;
import it.attocchi.entity.Lezione;
import it.attocchi.entity.User;
import it.attocchi.security.SecurityService;
import it.attocchi.service.BilancioService;
import it.attocchi.service.BilancioService.OreDonateTotali;
import it.attocchi.service.DashboardService;
import it.attocchi.service.CalendarioService.VoceCalendario;
import it.attocchi.service.DashboardService.RiepilogoGenerale;
import it.attocchi.service.DashboardService.RiepilogoGruppi;
import it.attocchi.service.DashboardService.RigaGruppo;
import it.attocchi.service.DashboardService.RiepilogoInsegnante;
import it.attocchi.service.LezioneService;
import it.attocchi.service.PagamentoService;
import it.attocchi.service.PagamentoService.RiepilogoPagamenti;
import it.attocchi.service.UserService;
import jakarta.annotation.security.PermitAll;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
                    PagamentoService pagamentoService, BilancioService bilancioService, LezioneService lezioneService) {
        // Riquadri che vanno a capo, uno per riga sugli smartphone
        addClassNames("gap-6");

        String username = securityService.getUsername().orElse("");
        User user = userService.findByUsername(username);
        H1 title = new H1("Ciao " + (user != null ? user.getNome() : username) + "!");
        title.addClassNames("text-[length:clamp(1.5rem,1rem_+_2vw,2rem)] leading-[1.2] m-0");
        add(title);

        // Prima dei riepiloghi la prossima lezione di ogni corso: dei corsi dell'insegnante (apre il registro delle
        // presenze), propria dell'allievo e dei figli del genitore (aprono il dettaglio in sola lettura)
        List<Card> prossime = new ArrayList<>();
        Map<Long, List<GruppoCorso>> gruppiPerCorso = lezioneService.gruppiPerCorso();
        if (securityService.hasRole("INSEGNANTE")) {
            dashboardService.prossimeLezioniInsegnante(username)
                .forEach(p -> prossime.add(prossimaLezione(p, gruppiPerCorso,
                    () -> DettaglioLezioneDialog.apriPresenze(p, false))));
        }
        List<VoceCalendario> proprie = new ArrayList<>();
        if (securityService.hasRole("ALLIEVO")) {
            proprie.addAll(dashboardService.prossimeLezioniAllievo(username));
        }
        if (securityService.hasRole("GENITORE")) {
            proprie.addAll(dashboardService.prossimeLezioniDeiFigli(username));
        }
        proprie.forEach(p -> prossime.add(prossimaLezione(p, gruppiPerCorso, () -> DettaglioLezioneDialog.apri(p,
            Formati.numeroLezione(p.lezione(), gruppiPerCorso.get(p.lezione().getCorso().getId())), username, false))));
        if (!prossime.isEmpty()) {
            add(sezione(prossime.size() == 1 ? "Prossima lezione" : "Prossime lezioni", prossime.toArray(Card[]::new)));
        }

        if (securityService.hasRole("ADMIN") || securityService.hasRole("SEGRETERIA")) {
            RiepilogoGenerale riepilogo = dashboardService.riepilogoGenerale();
            List<Card> generale = new ArrayList<>(List.of(
                indicatore(VaadinIcon.MUSIC, "Corsi", riepilogo.corsi()),
                indicatore(VaadinIcon.ACADEMY_CAP, "Allievi", riepilogo.allievi()),
                indicatore(VaadinIcon.CLIPBOARD_USER, "Iscritti ai corsi attivi", riepilogo.iscritti())));
            // Età degli allievi dei corsi attivi (solo di chi ha la data di nascita)
            dashboardService.etaAllievi(LocalDate.now()).ifPresent(eta -> generale.addAll(List.of(
                indicatore(VaadinIcon.CHART, "Età media", eta.media()),
                indicatore(VaadinIcon.CHILD, "Più giovane", eta.minima()),
                indicatore(VaadinIcon.USER, "Più anziano", eta.massima()))));
            generale.add(indicatore(VaadinIcon.FAMILY, "Genitori", riepilogo.genitori()));
            generale.add(indicatore(VaadinIcon.USER_STAR, "Insegnanti", riepilogo.insegnanti()));
            add(sezione("Riepilogo generale", generale.toArray(Card[]::new)));
            add(pagamenti(pagamentoService.riepilogoCorsiAttivi(), bilancioService.costiPrevistiPerCorso(),
                bilancioService.oreDonateTotali()));
            add(gruppi(dashboardService.riepilogoGruppi()));
        }
        if (securityService.hasRole("INSEGNANTE")) {
            RiepilogoInsegnante riepilogo = dashboardService.riepilogoInsegnante(username);
            List<Card> corsi = new ArrayList<>(List.of(
                indicatore(VaadinIcon.MUSIC, "Corsi assegnati", riepilogo.corsi()),
                indicatore(VaadinIcon.CHECK, "Lezioni fatte", riepilogo.lezioniFatte()),
                indicatore(VaadinIcon.CLOCK, "Lezioni da fare", riepilogo.lezioniDaFare()),
                indicatore(VaadinIcon.BAN, "Lezioni non effettuate", riepilogo.lezioniNonEffettuate())));
            // Insegnante senza tariffa oraria: le ore di lezione che sta donando sui corsi attivi (previste, di cui svolte)
            bilancioService.oreDonate(username).ifPresent(ore -> corsi.add(indicatore(VaadinIcon.HEART, "Ore donate",
                Formati.durata(ore.minutiPrevisti()), "di cui " + Formati.durata(ore.minutiSvolti()) + " già svolte")));
            Div sezione = sezione("I miei corsi", corsi.toArray(Card[]::new));
            // I gruppi dei suoi corsi, solo se ce ne sono
            List<RigaGruppo> gruppi = dashboardService.gruppiDellInsegnante(username);
            if (!gruppi.isEmpty()) {
                sezione.add(tabellaGruppi(gruppi));
            }
            add(sezione);
        }
        if (securityService.hasRole("ALLIEVO")) {
            List<Card> corsi = new ArrayList<>();
            corsi.add(indicatore(VaadinIcon.BOOK, "Corsi a cui sono iscritto", dashboardService.corsiAllievo(username)));
            // Allievo adulto (senza genitore): i suoi pagamenti, come per il genitore
            pagamentoService.situazionePropria(username).ifPresent(situazione -> corsi.addAll(pagamentiCard(situazione)));
            add(sezione("I miei corsi", corsi.toArray(Card[]::new)));
        }
        if (securityService.hasRole("GENITORE")) {
            List<Card> figli = new ArrayList<>();
            figli.add(indicatore(VaadinIcon.BOOK, "Corsi dei figli", dashboardService.corsiDeiFigli(username)));
            // Pagamenti di tutti i figli sui corsi attivi
            pagamentoService.situazioneDeiFigli(username).ifPresent(situazione -> figli.addAll(pagamentiCard(situazione)));
            add(sezione("I miei figli", figli.toArray(Card[]::new)));
        }
    }

    /**
     * Pagamenti dei corsi attivi: riquadri con i totali e una riga per corso (l'ultimo elemento è il totale), con il
     * costo previsto degli insegnanti ({@code costi} per id del corso, assente se nessuno ha la tariffa oraria) e il
     * margine = quote dovute − costo; con insegnanti senza tariffa anche il totale delle loro ore donate.
     */
    private static Div pagamenti(List<RiepilogoPagamenti> riepilogo, Map<Long, BigDecimal> costi,
                                 Optional<OreDonateTotali> oreDonate) {
        RiepilogoPagamenti totale = riepilogo.getLast();
        List<RiepilogoPagamenti> corsi = riepilogo.subList(0, riepilogo.size() - 1);
        // Totali di costo e margine solo sui corsi con un costo (almeno un insegnante con tariffa)
        BigDecimal costoTotale = BigDecimal.ZERO;
        BigDecimal margineTotale = BigDecimal.ZERO;
        long senzaTariffa = 0;
        for (RiepilogoPagamenti corso : corsi) {
            BigDecimal costo = costi.get(corso.corsoId());
            if (costo == null) {
                senzaTariffa++;
            } else {
                costoTotale = costoTotale.add(costo);
                margineTotale = margineTotale.add(corso.quota().subtract(costo));
            }
        }
        String esclusi = senzaTariffa == 0 ? null
            : senzaTariffa + (senzaTariffa == 1 ? " corso senza tariffa escluso" : " corsi senza tariffa esclusi");
        List<Card> riquadri = new ArrayList<>(List.of(
            indicatore(VaadinIcon.CHECK, "Saldati", totale.saldati()),
            indicatore(VaadinIcon.HOURGLASS, "Pagamento parziale", totale.parziali()),
            indicatore(VaadinIcon.CLOCK, "Senza pagamenti", totale.senzaPagamenti()),
            indicatore(VaadinIcon.EURO, "Incassato", Formati.euro(totale.incassato()),
                totale.percentualeIncassata() + "% del totale"),
            indicatore(VaadinIcon.WALLET, "Da incassare", Formati.euro(totale.daIncassare()),
                totale.percentualeDaIncassare() + "% del totale"),
            indicatore(VaadinIcon.USER_STAR, "Costo insegnanti previsto", Formati.euro(costoTotale),
                esclusi != null ? esclusi : "lezioni programmate"),
            indicatore(VaadinIcon.SCALE, "Margine previsto", Formati.euro(margineTotale),
                esclusi != null ? esclusi : "quote − costo insegnanti")));
        // Insegnanti senza tariffa: il totale delle ore che donano (previste, di cui svolte)
        oreDonate.ifPresent(ore -> riquadri.add(indicatore(VaadinIcon.HEART, "Ore donate", Formati.durata(ore.minutiPrevisti()),
            "di cui " + Formati.durata(ore.minutiSvolti()) + " già svolte · " + ore.insegnanti()
                + (ore.insegnanti() == 1 ? " insegnante" : " insegnanti"))));
        Div sezione = sezione("Pagamenti dei corsi attivi", riquadri.toArray(Card[]::new));

        Grid<RiepilogoPagamenti> grid = new Grid<>(RiepilogoPagamenti.class, false);
        grid.addColumn(RiepilogoPagamenti::corso).setHeader("Corso");
        grid.addColumn(RiepilogoPagamenti::saldati).setHeader("Saldati").setWidth("7rem").setFlexGrow(0);
        grid.addColumn(RiepilogoPagamenti::parziali).setHeader("Parziali").setWidth("7rem").setFlexGrow(0);
        grid.addColumn(RiepilogoPagamenti::senzaPagamenti).setHeader("Senza pagamenti").setWidth("10rem").setFlexGrow(0);
        grid.addColumn(r -> importo(r.incassato(), r.percentualeIncassata())).setHeader("Incassato");
        grid.addColumn(r -> importo(r.daIncassare(), r.percentualeDaIncassare())).setHeader("Da incassare");
        grid.addColumn(r -> costo(costi, r)).setHeader("Costo insegnanti");
        grid.addComponentColumn(r -> margine(costi, r)).setHeader("Margine").setAutoWidth(true).setFlexGrow(0);
        CardMobile.per(grid)
            .titolo(RiepilogoPagamenti::corso)
            .campo("Saldati", RiepilogoPagamenti::saldati)
            .campo("Parziali", RiepilogoPagamenti::parziali)
            .campo("Senza pagamenti", RiepilogoPagamenti::senzaPagamenti)
            .campo("Incassato", r -> importo(r.incassato(), r.percentualeIncassata()))
            .campo("Da incassare", r -> importo(r.daIncassare(), r.percentualeDaIncassare()))
            .campo("Costo insegnanti", r -> costo(costi, r))
            .campo("Margine", r -> margine(costi, r))
            .applica();
        grid.setItems(corsi);
        grid.setAllRowsVisible(true);
        grid.setEmptyStateText("Nessun corso attivo");
        grid.addClassNames("mt-4");
        sezione.add(grid);
        return sezione;
    }

    /**
     * Riquadro cliccabile (anche con Invio da tastiera) della prossima lezione: corso, data e orario (del gruppo o
     * dell'allievo), numero con il gruppo, allievo per il genitore e "Oggi" o "Domani".
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Card prossimaLezione(VoceCalendario prossima, Map<Long, List<GruppoCorso>> gruppi,
                                        SerializableRunnable apri) {
        Lezione lezione = prossima.lezione();
        Card card = new Card();
        Icon icon = VaadinIcon.CALENDAR_CLOCK.create();
        icon.addClassNames("text-(--vaadin-text-color-secondary)");
        card.setHeaderPrefix(icon);
        card.setTitle(lezione.getCorso().getNome());
        String quando = Formati.data(lezione.getData()) + (prossima.orario() != null ? ", " + Formati.ora(prossima.orario()) : "");
        Span data = new Span(quando);
        data.addClassNames("font-(--aura-font-weight-semibold) text-[1.25rem] leading-[1.2]");
        String numero = "Lezione " + Formati.numeroLezione(prossima, gruppi.get(lezione.getCorso().getId()))
            + (prossima.perChi() != null ? " · " + prossima.perChi() : "");
        Span dettaglio = new Span(numero);
        dettaglio.addClassNames("block", Stili.TESTO_SECONDARIO);
        card.add(data, dettaglio);
        LocalDate oggi = LocalDate.now();
        Span badges = new Span();
        badges.addClassNames("inline-flex flex-wrap gap-1");
        if (lezione.getCorso().isServizio()) {
            badges.add(TipoCorsoUI.badge(lezione.getCorso()));
        }
        if (lezione.getData().equals(oggi) || lezione.getData().equals(oggi.plusDays(1))) {
            Badge badge = new Badge(lezione.getData().equals(oggi) ? "Oggi" : "Domani");
            badge.addThemeVariants(lezione.getData().equals(oggi) ? BadgeVariant.FILLED : BadgeVariant.CONTRAST);
            badges.add(badge);
        }
        if (badges.getComponentCount() > 0) {
            card.setHeaderSuffix(badges);
        }
        card.addClassNames("cursor-pointer");
        card.getElement().setAttribute("role", "button");
        card.getElement().setAttribute("tabindex", "0");
        card.setAriaLabel(lezione.getCorso().getNome() + ": " + quando + ", " + numero);
        ComponentUtil.addListener(card, ClickEvent.class, (ComponentEventListener) e -> apri.run());
        card.getElement().addEventListener("keydown", e -> apri.run()).setFilter("event.key === 'Enter'");
        return card;
    }

    /** Card "Pagato" e "Da pagare" (genitore per i figli, allievo adulto per sé). */
    private static List<Card> pagamentiCard(PagamentoService.Situazione situazione) {
        return List.of(
            indicatore(VaadinIcon.CHECK, "Pagato", Formati.euro(situazione.pagato()), situazione.percentualePagata() + "% della quota"),
            indicatore(VaadinIcon.WALLET, "Da pagare", Formati.euro(situazione.residuo()),
                situazione.residuo().signum() == 0 ? "tutto saldato" : "su " + Formati.euro(situazione.quota()) + " di quota"));
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

    /** Costo previsto degli insegnanti del corso; "Senza tariffa" se nessuno ha la tariffa oraria. */
    private static String costo(Map<Long, BigDecimal> costi, RiepilogoPagamenti corso) {
        BigDecimal costo = costi.get(corso.corsoId());
        return costo != null ? Formati.euro(costo) : "Senza tariffa";
    }

    /** Margine (quote dovute − costo insegnanti) con badge "Attivo" o "Perdita"; vuoto senza tariffa. */
    private static Component margine(Map<Long, BigDecimal> costi, RiepilogoPagamenti corso) {
        BigDecimal costo = costi.get(corso.corsoId());
        if (costo == null) {
            return new Span();
        }
        BigDecimal margine = corso.quota().subtract(costo);
        Badge badge = margine.signum() >= 0
            ? new Badge("Attivo", VaadinIcon.TRENDING_UP.create())
            : new Badge("Perdita", VaadinIcon.TRENDING_DOWN.create());
        badge.addThemeVariants(margine.signum() >= 0 ? BadgeVariant.SUCCESS : BadgeVariant.ERROR);
        Span importo = new Span(Formati.euro(margine));
        Div cella = new Div(importo, badge);
        cella.addClassNames("flex items-center gap-2 whitespace-nowrap");
        return cella;
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
