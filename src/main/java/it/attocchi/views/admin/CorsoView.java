package it.attocchi.views.admin;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import it.attocchi.entity.Corso;
import it.attocchi.security.SecurityService;
import it.attocchi.service.AllievoService;
import it.attocchi.service.AnnoScolasticoService;
import it.attocchi.service.CorsoService;
import it.attocchi.service.InsegnanteService;
import it.attocchi.service.LuogoService;
import it.attocchi.service.PagamentoService;
import it.attocchi.views.MainLayout;
import it.attocchi.views.Schede;
import it.attocchi.views.TipoCorsoUI;
import it.attocchi.views.ViewSupport;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;
import java.util.stream.Stream;

/**
 * Pagina di un corso (admin e segreteria) con le schede "Dati del corso" ({@link DatiCorsoForm}) e "Iscritti"
 * ({@link PartecipantiCorso}): {@code corsi/<id>}, {@code corsi/<id>/iscritti}, {@code corsi/nuovo} per un corso nuovo
 * (Iscritti si attiva dopo il primo salvataggio). In testa "Torna ai corsi", "Aggiorna lezioni", "Duplica" ed
 * "Elimina" (solo admin). Si costruisce solo la scheda aperta; uscendo con modifiche non salvate si chiede conferma.
 */
@Route(value = "corsi/:" + CorsoView.PARAMETRO_CORSO + "(" + CorsoView.NUOVO + "|[0-9]+)/:" + CorsoView.PARAMETRO_SCHEDA
    + "?(" + CorsoView.ISCRITTI + ")", layout = MainLayout.class)
@RolesAllowed({"ADMIN", "SEGRETERIA"})
public class CorsoView extends VerticalLayout implements BeforeEnterObserver, BeforeLeaveObserver, HasDynamicTitle {

    static final String PARAMETRO_CORSO = "corso";
    static final String PARAMETRO_SCHEDA = "scheda";
    static final String NUOVO = "nuovo";
    static final String ISCRITTI = "iscritti";

    /** Schede della pagina. */
    public enum Scheda {
        DATI("Dati del corso"),
        ISCRITTI("Iscritti");

        private final String etichetta;

        Scheda(String etichetta) {
            this.etichetta = etichetta;
        }

        public String getEtichetta() {
            return etichetta;
        }
    }

    private final CorsoService corsoService;
    private final InsegnanteService insegnanteService;
    private final AllievoService allievoService;
    private final PagamentoService pagamentoService;
    private final AnnoScolasticoService annoScolasticoService;
    private final LuogoService luogoService;
    private final boolean puoEliminare;

    private final H2 titolo = new H2();
    private final Div badge = new Div();
    private final HorizontalLayout azioni = new HorizontalLayout();
    private final Schede<Scheda> schede = new Schede<>("Sezioni del corso", List.of(Scheda.values()),
        Scheda::getEtichetta, null);
    private final Div contenuto = new Div();
    private Corso corso;
    private Scheda scheda = Scheda.DATI;
    private DatiCorsoForm form;

    public CorsoView(CorsoService corsoService, InsegnanteService insegnanteService, AllievoService allievoService,
                     PagamentoService pagamentoService, AnnoScolasticoService annoScolasticoService,
                     LuogoService luogoService, SecurityService securityService) {
        this.corsoService = corsoService;
        this.insegnanteService = insegnanteService;
        this.allievoService = allievoService;
        this.pagamentoService = pagamentoService;
        this.annoScolasticoService = annoScolasticoService;
        this.luogoService = luogoService;
        this.puoEliminare = securityService.hasRole("ADMIN");

        Button indietro = new Button("Torna ai corsi", VaadinIcon.ARROW_LEFT.create(),
            e -> UI.getCurrent().navigate(CorsiView.class));
        indietro.addThemeVariants(ButtonVariant.TERTIARY);
        indietro.addClassNames("self-start");
        titolo.addClassNames("m-0 text-(length:--aura-font-size-xl)");
        badge.addClassNames("flex flex-wrap gap-1");
        Div intestazione = new Div(titolo, badge);
        intestazione.addClassNames("flex flex-wrap items-center gap-2 grow");
        azioni.addClassNames("flex-wrap");
        HorizontalLayout testa = new HorizontalLayout(intestazione, azioni);
        testa.addClassNames("flex-wrap w-full items-center");
        // L'altra scheda è un altro indirizzo: Indietro del browser e ricarica la riaprono
        schede.addValueChangeListener(e -> {
            if (e.getValue() != scheda && corso != null && corso.getId() != null) {
                UI.getCurrent().navigate(CorsoView.class, parametri(corso.getId(), e.getValue()));
            }
        });
        contenuto.addClassNames("w-full");
        add(indietro, testa, schede, contenuto);
    }

    /** Indirizzo della pagina del corso, sulla scheda indicata. */
    public static RouteParameters parametri(Long corsoId, Scheda scheda) {
        RouteParam corso = new RouteParam(PARAMETRO_CORSO, corsoId != null ? corsoId.toString() : NUOVO);
        return scheda == Scheda.ISCRITTI ? new RouteParameters(corso, new RouteParam(PARAMETRO_SCHEDA, ISCRITTI))
            : new RouteParameters(corso);
    }

    /** Apre la pagina del corso sulla scheda indicata ({@code null} = corso nuovo). */
    public static void apri(Long corsoId, Scheda scheda) {
        UI.getCurrent().navigate(CorsoView.class, parametri(corsoId, scheda));
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String id = event.getRouteParameters().get(PARAMETRO_CORSO).orElse(NUOVO);
        if (NUOVO.equals(id)) {
            corso = new Corso();
        } else {
            corso = corsoService.findById(Long.valueOf(id));
            if (corso == null) {
                Notification.show("Corso non trovato");
                event.forwardTo(CorsiView.class);
                return;
            }
        }
        boolean iscritti = event.getRouteParameters().get(PARAMETRO_SCHEDA).isPresent() && corso.getId() != null;
        scheda = iscritti ? Scheda.ISCRITTI : Scheda.DATI;
        mostra();
    }

    @Override
    public void beforeLeave(BeforeLeaveEvent event) {
        if (form == null || !form.hasChanges()) {
            return;
        }
        BeforeLeaveEvent.ContinueNavigationAction azione = event.postpone();
        Dialog conferma = ViewSupport.dialog("Modifiche non salvate", "420px");
        conferma.add(new Span("I dati del corso hanno modifiche non salvate: uscire senza salvarle?"));
        Button resta = new Button("Resta", e -> {
            conferma.close();
            // La scheda torna a quella aperta
            schede.setValue(scheda);
        });
        resta.addThemeVariants(ButtonVariant.TERTIARY);
        Button esci = new Button("Esci senza salvare", e -> {
            conferma.close();
            form = null;
            azione.proceed();
        });
        esci.addThemeVariants(ButtonVariant.PRIMARY, ButtonVariant.ERROR);
        conferma.getFooter().add(resta, esci);
        conferma.open();
    }

    @Override
    public String getPageTitle() {
        return corso == null || corso.getId() == null ? "Nuovo corso" : corso.getNome();
    }

    /** Intestazione, azioni e scheda aperta. */
    private void mostra() {
        boolean nuovo = corso.getId() == null;
        titolo.setText(nuovo ? "Nuovo corso" : corso.getNome() + " · " + corso.getAnnoScolastico());
        badge.removeAll();
        Stream.of(TipoCorsoUI.badge(corso), TipoCorsoUI.badgePubblicazione(corso)).filter(b -> b != null).forEach(badge::add);
        azioni.removeAll();
        if (!nuovo) {
            Button aggiorna = new Button("Aggiorna lezioni", VaadinIcon.REFRESH.create(),
                e -> AggiornaLezioniDialog.apri(corso, () -> { }));
            aggiorna.setEnabled(CorsoService.puoModificare(corso));
            Button duplica = new Button("Duplica", VaadinIcon.COPY.create(), e -> duplica());
            duplica.setTooltipText("Duplica per l'anno successivo (senza partecipanti, non pubblicato)");
            azioni.add(aggiorna, duplica);
            if (puoEliminare) {
                Button elimina = new Button("Elimina", VaadinIcon.TRASH.create(), e -> confermaEliminazione());
                elimina.addThemeVariants(ButtonVariant.ERROR);
                azioni.add(elimina);
            }
        }
        schede.getScheda(Scheda.ISCRITTI).setEnabled(!nuovo);
        schede.setValue(scheda);
        contenuto.removeAll();
        if (scheda == Scheda.ISCRITTI) {
            form = null;
            contenuto.add(new PartecipantiCorso(corsoService, allievoService, pagamentoService, corso, puoEliminare));
        } else {
            form = new DatiCorsoForm(corso, corsoService, insegnanteService, annoScolasticoService, luogoService,
                puoEliminare, this::salvato);
            contenuto.add(form);
        }
    }

    private void salvato(Corso salvato) {
        Notification.show("Corso salvato");
        boolean nuovo = corso.getId() == null;
        form = null;
        if (nuovo) {
            // Prende il suo indirizzo: da qui anche la scheda Iscritti
            apri(salvato.getId(), Scheda.DATI);
            return;
        }
        corso = corsoService.findById(salvato.getId());
        mostra();
        proponiAggiornamento(corso);
    }

    /** Copia del corso per l'anno successivo, poi la sua pagina. */
    private void duplica() {
        try {
            Corso copia = corsoService.duplica(corso.getId());
            Notification.show("Copia di " + corso.getNome() + " per il " + copia.getAnnoScolastico()
                + ": non pubblicata, senza partecipanti");
            apri(copia.getId(), Scheda.DATI);
        } catch (RuntimeException ex) {
            Notification.show("Duplicazione non riuscita: " + ex.getMessage());
        }
    }

    private void confermaEliminazione() {
        Dialog conferma = ViewSupport.dialog("Eliminare il corso?", "420px");
        conferma.add(new Span(corso.getNome() + " · " + corso.getAnnoScolastico()
            + ": il corso si elimina con iscritti, lezioni, gruppi e opzioni. Non si può annullare."));
        Button elimina = new Button("Elimina", e -> {
            conferma.close();
            try {
                corsoService.delete(corso);
                Notification.show("Corso eliminato");
                form = null;
                UI.getCurrent().navigate(CorsiView.class);
            } catch (RuntimeException ex) {
                Notification.show("Impossibile eliminare il corso: " + ex.getMessage());
            }
        });
        elimina.addThemeVariants(ButtonVariant.PRIMARY, ButtonVariant.ERROR);
        conferma.getFooter().add(ViewSupport.closeButton(conferma, "Annulla"), elimina);
        conferma.open();
    }

    /**
     * Dopo il salvataggio: se il corso ha già lezioni e il suo calendario è cambiato (fine, numero di lezioni, giorni,
     * cadenza…) un avviso propone di aggiornarle.
     */
    private void proponiAggiornamento(Corso corso) {
        if (!CorsoService.puoModificare(corso)) {
            return;
        }
        CorsoService.AggiornamentoLezioni anteprima = corsoService.anteprimaAggiornamento(corso.getId());
        boolean haLezioni = anteprima.invariate() > 0 || !anteprima.tolte().isEmpty();
        if (!haLezioni || !anteprima.cambia()) {
            return;
        }
        Notification avviso = new Notification();
        avviso.setDuration(10000);
        Button aggiorna = new Button("Aggiorna lezioni", e -> {
            avviso.close();
            AggiornaLezioniDialog.apri(corso, () -> { });
        });
        aggiorna.addThemeVariants(ButtonVariant.PRIMARY);
        HorizontalLayout riga = new HorizontalLayout(new Span("Il calendario delle lezioni di " + corso.getNome()
            + " è cambiato."), aggiorna);
        riga.addClassNames("items-center");
        avviso.add(riga);
        avviso.open();
    }
}
