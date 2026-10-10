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
import it.attocchi.entity.Studente;
import it.attocchi.entity.User;
import it.attocchi.security.SecurityService;
import it.attocchi.service.CorsoService;
import it.attocchi.service.GenitoreService;
import it.attocchi.service.PagamentoService;
import it.attocchi.service.PagamentoService.Situazione;
import it.attocchi.service.PresenzaService;
import it.attocchi.service.StudenteService;
import it.attocchi.service.UserService;
import it.attocchi.views.AnagraficaFields;
import it.attocchi.views.Formati;
import it.attocchi.views.MainLayout;
import it.attocchi.views.PagamentiStudenteDialog;
import it.attocchi.views.PresenzeStudenteDialog;
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

    private final StudenteService studenteService;
    private final CorsoService corsoService;
    private final PresenzaService presenzaService;
    private final PagamentoService pagamentoService;
    private final UserService userService;

    private final Grid<Studente> gridFigli = new Grid<>(Studente.class, false);
    private Genitore genitoreCorrente;

    public FigliView(StudenteService studenteService, CorsoService corsoService,
                     PresenzaService presenzaService, PagamentoService pagamentoService,
                     SecurityService securityService, GenitoreService genitoreService, UserService userService) {
        this.studenteService = studenteService;
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
        gridFigli.addColumn(Studente::getNomeCompleto).setHeader("Nome").setSortable(true);
        var dataNascita = gridFigli.addColumn(studente -> Formati.data(studente.getDataNascita())).setHeader("Data Nascita")
            .setComparator(Studente::getDataNascita).setAutoWidth(true).setFlexGrow(0);
        gridFigli.addColumn(studente -> studente.getCorsi().size()).setHeader("N. Corsi").setAutoWidth(true).setFlexGrow(0);

        ViewSupport.configureActionsColumn(gridFigli.addComponentColumn(studente -> ViewSupport.actions(
            ViewSupport.iconButton(VaadinIcon.PENCIL, "Modifica", e -> openModificaDialog(studente)),
            ViewSupport.iconButton(VaadinIcon.BOOK, "Visualizza Dettagli", e -> openDettagliDialog(studente)))));

        ViewSupport.hideOnMobile(gridFigli, dataNascita);
    }

    /** Contatti e dati anagrafici del figlio; username, genitore e corsi li gestisce la segreteria. */
    private void openModificaDialog(Studente studente) {
        Dialog dialog = ViewSupport.dialog("Dati di " + studente.getNomeCompleto(), "700px");
        User user = studente.getUser();
        UtenteFields utente = new UtenteFields(user, userService);
        Binder<Studente> binder = new Binder<>(Studente.class);
        AnagraficaFields<Studente> anagrafica = new AnagraficaFields<>(binder, true);
        binder.readBean(studente);

        Span indirizzo = new Span("Se l'indirizzo è vuoto si usa il tuo: " + genitoreCorrente.getIndirizzo().getFormattato());
        indirizzo.addClassName("avviso-sola-lettura");
        indirizzo.setVisible(!genitoreCorrente.getIndirizzo().isVuoto());

        VerticalLayout content = new VerticalLayout(
            ViewSupport.sezione("Utente e contatti"), new FormLayout(utente.getComponents()),
            ViewSupport.sezione("Dati anagrafici"), new FormLayout(anagrafica.getComponents()), indirizzo);
        content.setPadding(false);

        Button salva = new Button("Salva", e -> {
            boolean valido = binder.validate().isOk() & utente.validate();
            if (!valido || !binder.writeBeanIfValid(studente) || !utente.writeBeanIfValid(user)) {
                return;
            }
            try {
                studenteService.aggiornaDaGenitore(genitoreCorrente.getUser().getUsername(), studente.getId(), studente, user);
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

    private void openDettagliDialog(Studente studente) {
        Dialog dialog = ViewSupport.dialog("Corsi di " + studente.getNomeCompleto(), "1000px");

        Grid<Corso> gridCorsi = new Grid<>(Corso.class, false);
        gridCorsi.addColumn(Corso::getNome).setHeader("Corso");
        var insegnante = gridCorsi.addColumn(corso -> corso.getInsegnante() != null ? corso.getInsegnante().getNomeCompleto() : "").setHeader("Insegnante");
        var numeroLezioni = gridCorsi.addColumn(Corso::getNumeroLezioni).setHeader("N. Lezioni");
        Map<Long, Situazione> situazioni = pagamentoService.situazioniDelloStudente(studente.getId());
        var pagamento = gridCorsi.addComponentColumn(corso -> StatoPagamentoUI.badge(situazioni.get(corso.getId())))
            .setHeader("Pagamento").setWidth("9rem").setFlexGrow(0);

        ViewSupport.configureActionsColumn(gridCorsi.addComponentColumn(corso -> {
            Button presenzeButton = ViewSupport.iconButton(VaadinIcon.CLIPBOARD_CHECK, "Presenze", e -> openPresenzeDialog(studente, corso));
            Button pagamentiButton = ViewSupport.iconButton(VaadinIcon.EURO, "Pagamenti", e -> openPagamentiDialog(studente, corso));
            StatoPagamentoUI.coloraPulsante(pagamentiButton, "Pagamenti", situazioni.get(corso.getId()));

            HorizontalLayout actions = ViewSupport.actions(presenzeButton, pagamentiButton);
            actions.add(RicevutaView.link(corso.getId(), studente.getId(), ViewSupport.iconButton(VaadinIcon.PRINT, "Ricevuta")));
            return actions;
        }));
        ViewSupport.hideOnMobile(gridCorsi, insegnante, numeroLezioni, pagamento);

        gridCorsi.setItems(corsoService.findByStudente(studente.getId()));
        gridCorsi.setAllRowsVisible(true);

        dialog.add(gridCorsi);
        dialog.getFooter().add(ViewSupport.closeButton(dialog, "Chiudi"));
        dialog.open();
    }

    private void openPresenzeDialog(Studente studente, Corso corso) {
        List<Presenza> presenze = presenzaService.findByCorsoAndStudente(corso.getId(), studente.getId());
        new PresenzeStudenteDialog(corso.getNome() + " – " + studente.getNomeCompleto(), presenze).open();
    }

    private void openPagamentiDialog(Studente studente, Corso corso) {
        new PagamentiStudenteDialog("Pagamenti " + corso.getNome() + " – " + studente.getNomeCompleto(),
            pagamentoService.findIscrizione(corso.getId(), studente.getId()),
            pagamentoService.situazione(corso.getId(), studente.getId()),
            pagamentoService.findPagamenti(corso.getId(), studente.getId())).open();
    }

    private void updateGrid() {
        if (genitoreCorrente != null) {
            List<Studente> figli = studenteService.findByGenitore(genitoreCorrente.getId());
            gridFigli.setItems(figli);
        }
    }
}
