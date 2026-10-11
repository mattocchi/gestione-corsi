package it.attocchi.views.genitore;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import it.attocchi.entity.Corso;
import it.attocchi.entity.Genitore;
import it.attocchi.entity.Presenza;
import it.attocchi.entity.Allievo;
import it.attocchi.entity.User;
import it.attocchi.security.SecurityService;
import it.attocchi.service.CorsoService;
import it.attocchi.service.GenitoreService;
import it.attocchi.service.PagamentoService;
import it.attocchi.service.PagamentoService.Situazione;
import it.attocchi.service.PresenzaService;
import it.attocchi.service.AllievoService;
import it.attocchi.service.UserService;
import it.attocchi.views.AnagraficaFields;
import it.attocchi.views.Formati;
import it.attocchi.views.MainLayout;
import it.attocchi.views.PagamentiAllievoDialog;
import it.attocchi.views.PresenzeAllievoDialog;
import it.attocchi.views.RicevutaView;
import it.attocchi.views.StatoPagamentoUI;
import it.attocchi.views.UtenteFields;
import it.attocchi.views.ViewSupport;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;
import java.util.Map;

@Route(value = "figli", layout = MainLayout.class)
@PageTitle("I Miei Figli")
@Menu(title = "I Miei Figli", order = 40, icon = "vaadin:family")
@RolesAllowed("GENITORE")
public class FigliView extends VerticalLayout {

    private final AllievoService allievoService;
    private final CorsoService corsoService;
    private final PresenzaService presenzaService;
    private final PagamentoService pagamentoService;
    private final UserService userService;

    private final Grid<Allievo> gridFigli = new Grid<>(Allievo.class, false);
    private Genitore genitoreCorrente;

    public FigliView(AllievoService allievoService, CorsoService corsoService,
                     PresenzaService presenzaService, PagamentoService pagamentoService,
                     SecurityService securityService, GenitoreService genitoreService, UserService userService) {
        this.allievoService = allievoService;
        this.corsoService = corsoService;
        this.presenzaService = presenzaService;
        this.pagamentoService = pagamentoService;
        this.userService = userService;

        setSizeFull();
        genitoreCorrente = securityService.getUsername().flatMap(genitoreService::findByUsername).orElse(null);
        configureGrid();

        add(gridFigli);
        expand(gridFigli);
        updateGrid();
    }

    private void configureGrid() {
        gridFigli.addColumn(Allievo::getNomeCompleto).setHeader("Nome").setSortable(true);
        var dataNascita = gridFigli.addColumn(allievo -> Formati.data(allievo.getDataNascita())).setHeader("Data Nascita")
            .setComparator(Allievo::getDataNascita).setAutoWidth(true).setFlexGrow(0);
        gridFigli.addColumn(allievo -> allievo.getCorsi().size()).setHeader("N. Corsi").setAutoWidth(true).setFlexGrow(0);

        ViewSupport.configureActionsColumn(gridFigli.addComponentColumn(allievo -> ViewSupport.actions(
            ViewSupport.iconButton(VaadinIcon.PENCIL, "Modifica", e -> openModificaDialog(allievo)),
            ViewSupport.iconButton(VaadinIcon.BOOK, "Visualizza Dettagli", e -> openDettagliDialog(allievo)))));

        ViewSupport.hideOnMobile(gridFigli, dataNascita);
    }

    /** Contatti e dati anagrafici del figlio; username, genitore e corsi li gestisce la segreteria. */
    private void openModificaDialog(Allievo allievo) {
        Dialog dialog = ViewSupport.dialog("Dati di " + allievo.getNomeCompleto(), "700px");
        User user = allievo.getUser();
        UtenteFields utente = new UtenteFields(user, userService);
        Binder<Allievo> binder = new Binder<>(Allievo.class);
        AnagraficaFields<Allievo> anagrafica = new AnagraficaFields<>(binder, true);
        binder.readBean(allievo);

        Span indirizzo = new Span("Se l'indirizzo è vuoto si usa il tuo: " + genitoreCorrente.getIndirizzo().getFormattato());
        indirizzo.addClassName("avviso-sola-lettura");
        indirizzo.setVisible(!genitoreCorrente.getIndirizzo().isVuoto());

        VerticalLayout content = new VerticalLayout(
            ViewSupport.sezione("Utente e contatti"), new FormLayout(utente.getComponents()),
            ViewSupport.sezione("Dati anagrafici"), new FormLayout(anagrafica.getComponents()), indirizzo);
        content.setPadding(false);

        Button salva = new Button("Salva", e -> {
            boolean valido = binder.validate().isOk() & utente.validate();
            if (!valido || !binder.writeBeanIfValid(allievo) || !utente.writeBeanIfValid(user)) {
                return;
            }
            try {
                allievoService.aggiornaDaGenitore(genitoreCorrente.getUser().getUsername(), allievo.getId(), allievo, user);
                updateGrid();
                dialog.close();
                Notification.show("Dati salvati");
            } catch (RuntimeException ex) {
                Notification.show("Errore nel salvataggio: " + ex.getMessage());
            }
        });
        salva.addThemeVariants(ButtonVariant.PRIMARY);

        dialog.add(content);
        dialog.getFooter().add(ViewSupport.closeButton(dialog, "Annulla"), salva);
        dialog.open();
    }

    private void openDettagliDialog(Allievo allievo) {
        Dialog dialog = ViewSupport.dialog("Corsi di " + allievo.getNomeCompleto(), "1000px");

        Grid<Corso> gridCorsi = new Grid<>(Corso.class, false);
        gridCorsi.addColumn(Corso::getNome).setHeader("Corso");
        var insegnante = gridCorsi.addColumn(corso -> corso.getInsegnante() != null ? corso.getInsegnante().getNomeCompleto() : "").setHeader("Insegnante");
        var numeroLezioni = gridCorsi.addColumn(Corso::getNumeroLezioni).setHeader("N. Lezioni");
        Map<Long, Situazione> situazioni = pagamentoService.situazioniDelloAllievo(allievo.getId());
        var pagamento = gridCorsi.addComponentColumn(corso -> StatoPagamentoUI.badge(situazioni.get(corso.getId())))
            .setHeader("Pagamento").setWidth("9rem").setFlexGrow(0);

        ViewSupport.configureActionsColumn(gridCorsi.addComponentColumn(corso -> {
            Button presenzeButton = ViewSupport.iconButton(VaadinIcon.CLIPBOARD_CHECK, "Presenze", e -> openPresenzeDialog(allievo, corso));
            Button pagamentiButton = ViewSupport.iconButton(VaadinIcon.EURO, "Pagamenti", e -> openPagamentiDialog(allievo, corso));
            StatoPagamentoUI.coloraPulsante(pagamentiButton, "Pagamenti", situazioni.get(corso.getId()));

            HorizontalLayout actions = ViewSupport.actions(presenzeButton, pagamentiButton);
            actions.add(RicevutaView.link(corso.getId(), allievo.getId(), ViewSupport.iconButton(VaadinIcon.PRINT, "Ricevuta")));
            return actions;
        }));
        ViewSupport.hideOnMobile(gridCorsi, insegnante, numeroLezioni, pagamento);

        gridCorsi.setItems(corsoService.findByAllievo(allievo.getId()));
        gridCorsi.setAllRowsVisible(true);

        dialog.add(gridCorsi);
        dialog.getFooter().add(ViewSupport.closeButton(dialog, "Chiudi"));
        dialog.open();
    }

    private void openPresenzeDialog(Allievo allievo, Corso corso) {
        List<Presenza> presenze = presenzaService.findByCorsoAndAllievo(corso.getId(), allievo.getId());
        new PresenzeAllievoDialog(corso.getNome() + " – " + allievo.getNomeCompleto(), presenze).open();
    }

    private void openPagamentiDialog(Allievo allievo, Corso corso) {
        new PagamentiAllievoDialog("Pagamenti " + corso.getNome() + " – " + allievo.getNomeCompleto(),
            pagamentoService.findIscrizione(corso.getId(), allievo.getId()),
            pagamentoService.situazione(corso.getId(), allievo.getId()),
            pagamentoService.findPagamenti(corso.getId(), allievo.getId())).open();
    }

    private void updateGrid() {
        if (genitoreCorrente != null) {
            List<Allievo> figli = allievoService.findByGenitore(genitoreCorrente.getId());
            gridFigli.setItems(figli);
        }
    }
}
