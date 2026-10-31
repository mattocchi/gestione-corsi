package it.attocchi.views.admin;

import it.attocchi.views.Stili;
import it.attocchi.security.SessioniUtenti;
import com.vaadin.flow.component.shared.Tooltip;
import it.attocchi.service.RecuperoPasswordService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import it.attocchi.entity.User;
import it.attocchi.security.SecurityService;
import it.attocchi.service.SchedeRuoliService;
import it.attocchi.service.SchedeRuoliService.EliminazioneUtente;
import it.attocchi.service.UserService;
import it.attocchi.views.CardMobile;
import it.attocchi.views.Formati;
import it.attocchi.views.MainLayout;
import it.attocchi.views.RicercaRapida;
import it.attocchi.views.ViewSupport;
import jakarta.annotation.security.RolesAllowed;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Route(value = "utenti", layout = MainLayout.class)
@PageTitle("Utenti")
@Menu(title = "Utenti", order = 12, icon = "vaadin:users")
@RolesAllowed("ADMIN")
public class UtentiView extends VerticalLayout {

    private final UserService userService;
    private final SchedeRuoliService schedeRuoliService;
    private final SecurityService securityService;
    private final Grid<User> grid = new Grid<>(User.class, false);
    private final RicercaRapida ricerca = new RicercaRapida();
    private final Checkbox soloIncongruenze = new Checkbox("Solo incongruenze");
    private final Checkbox soloSospesi = new Checkbox("Solo sospesi per inattività");
    private final Button invitaSelezionati = new Button("Invita selezionati", VaadinIcon.PAPERPLANE.create());
    private final RecuperoPasswordService recuperoPasswordService;
    private final SessioniUtenti sessioniUtenti;
    private GridListDataView<User> utenti;
    private List<User> tutti = List.of();
    private Map<Long, List<String>> incongruenze = Map.of();

    public UtentiView(UserService userService, SchedeRuoliService schedeRuoliService, SecurityService securityService,
                      RecuperoPasswordService recuperoPasswordService, SessioniUtenti sessioniUtenti) {
        this.recuperoPasswordService = recuperoPasswordService;
        this.sessioniUtenti = sessioniUtenti;
        this.userService = userService;
        this.schedeRuoliService = schedeRuoliService;
        this.securityService = securityService;

        addClassNames("size-full");
        configureGrid();

        Button addButton = new Button("Nuovo Utente", e -> openDialog(new User()));
        // Incongruenze tra ruoli e schede: filtro e creazione delle schede che mancano
        soloIncongruenze.setTooltipText("Utenti con un ruolo senza la sua scheda o una scheda senza il ruolo");
        soloIncongruenze.addValueChangeListener(e -> applicaFiltri());
        soloSospesi.addValueChangeListener(e -> applicaFiltri());
        ricerca.addRicercaListener(this::applicaFiltri);
        Button creaSchede = new Button("Crea schede mancanti", VaadinIcon.FILE_ADD.create(), e -> creaSchedeMancanti());
        creaSchede.setTooltipText("Crea la scheda di insegnante, genitore o allievo a chi ha il ruolo ma non la scheda");
        invitaSelezionati.setEnabled(false);
        invitaSelezionati.setTooltipText("Email con il link per impostare la password a chi ha un'email e non è mai entrato");
        invitaSelezionati.addClickListener(e -> invitaSelezionati());
        Button allinea = new Button("Allinea username", VaadinIcon.EXCHANGE.create(), e -> apriAllineamento());
        allinea.setTooltipText("Username generati (nome.cognome) non più uguali a nome e cognome attuali");
        HorizontalLayout toolbar = new HorizontalLayout(addButton, creaSchede, allinea, invitaSelezionati, soloIncongruenze, soloSospesi, ricerca);
        toolbar.addClassNames("flex-wrap w-full items-center");
        add(toolbar, grid);
        grid.addClassNames("grow");

        updateGrid();
    }

    private void configureGrid() {
        grid.addColumn(User::getUsername).setHeader("Username").setSortable(true);
        grid.addColumn(User::getNome).setHeader("Nome").setSortable(true);
        grid.addColumn(User::getCognome).setHeader("Cognome").setSortable(true);
        grid.addColumn(User::getEmail).setHeader("Email");
        grid.addColumn(user -> String.join(", ", user.getRoles())).setHeader("Ruoli");
        grid.addComponentColumn(this::avvisi).setHeader("Schede").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(UtentiView::ultimoAccesso).setHeader("Ultimo accesso")
            .setComparator(Comparator.comparing(User::getUltimoAccesso, Comparator.nullsFirst(Comparator.naturalOrder())))
            .setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(UtentiView::invito).setHeader("Invito")
            .setComparator(Comparator.comparing(User::getInvitatoIl, Comparator.nullsFirst(Comparator.naturalOrder())))
            .setAutoWidth(true).setFlexGrow(0);

        grid.addComponentColumn(UtentiView::stato).setHeader("Stato").setAutoWidth(true).setFlexGrow(0);

        ViewSupport.configureActionsColumn(grid.addComponentColumn(this::createActions));
        // Selezione multipla per invitare più utenti insieme
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        grid.addSelectionListener(e -> invitaSelezionati.setEnabled(!e.getAllSelectedItems().isEmpty()));

        CardMobile.per(grid)
            .titolo(User::getUsername)
            .stato(UtentiView::stato)
            .campo("Nome", User::getNome)
            .campo("Cognome", User::getCognome)
            .campo("Email", User::getEmail)
            .campo("Ruoli", user -> String.join(", ", user.getRoles()))
            .campo("Schede", user -> incongruenze.containsKey(user.getId()) ? avvisi(user) : null)
            .campo("Ultimo accesso", UtentiView::ultimoAccesso)
            .campo("Invito", UtentiView::invito)
            .azioni(this::createActions)
            .applica();
    }

    /** Attivo, Disattivato, o Sospeso per inattività (si riattiva con il recupero password). */
    private static Badge stato(User user) {
        if (user.isEnabled()) {
            Badge badge = new Badge("Attivo", VaadinIcon.CHECK.create());
            badge.addThemeVariants(BadgeVariant.SUCCESS);
            return badge;
        }
        if (user.isSospesoPerInattivita()) {
            Badge badge = new Badge("Sospeso per inattività", VaadinIcon.CLOCK.create());
            badge.addThemeVariants(BadgeVariant.WARNING);
            Tooltip.forComponent(badge).setText("Nessun accesso da più di " + UserService.MESI_INATTIVITA
                + " mesi: si riattiva con «Password dimenticata?» o spuntando Attivo");
            return badge;
        }
        return new Badge("Disattivato", VaadinIcon.BAN.create());
    }

    /**
     * Anteprima degli username generati da allineare (vecchio → nuovo), tutti scelti; la conferma allinea quelli
     * spuntati.
     */
    private void apriAllineamento() {
        List<UserService.UsernameDaAllineare> proposte = userService.usernameDaAllineare();
        if (proposte.isEmpty()) {
            Notification.show("Tutti gli username generati sono già allineati a nome e cognome");
            return;
        }
        Dialog dialog = ViewSupport.dialog("Allinea username", "640px");
        Grid<UserService.UsernameDaAllineare> elenco = new Grid<>();
        elenco.addColumn(p -> p.user().getNomeCompleto()).setHeader("Utente");
        elenco.addColumn(UserService.UsernameDaAllineare::attuale).setHeader("Username attuale");
        elenco.addColumn(UserService.UsernameDaAllineare::proposto).setHeader("Nuovo username");
        CardMobile.per(elenco)
            .titolo(p -> p.user().getNomeCompleto())
            .campo("Username attuale", UserService.UsernameDaAllineare::attuale)
            .campo("Nuovo username", UserService.UsernameDaAllineare::proposto)
            .applica();
        elenco.setItems(proposte);
        elenco.setSelectionMode(Grid.SelectionMode.MULTI);
        proposte.forEach(elenco::select);
        elenco.setAllRowsVisible(true);
        Span nota = new Span("Chi è collegato con questi utenti dovrà rientrare con il nuovo username (o con l'email).");
        nota.addClassNames(Stili.TESTO_SECONDARIO);
        Button conferma = new Button("Allinea", e -> {
            dialog.close();
            allinea(elenco.getSelectedItems().stream().map(p -> p.user().getId()).toList());
        });
        conferma.addThemeVariants(ButtonVariant.PRIMARY);
        elenco.addSelectionListener(e -> conferma.setEnabled(!e.getAllSelectedItems().isEmpty()));
        dialog.add(nota, elenco);
        dialog.getFooter().add(ViewSupport.closeButton(dialog, "Annulla"), conferma);
        dialog.open();
    }

    /** Allinea gli username, chiude le sessioni di chi è cambiato e, se è cambiato il proprio, esce. */
    private void allinea(List<Long> userIds) {
        String corrente = securityService.getUsername().orElse("");
        try {
            Map<String, String> cambiati = userService.allineaUsername(userIds, corrente);
            sessioniUtenti.chiudi(cambiati.keySet());
            if (cambiati.containsKey(corrente)) {
                securityService.logout();
                return;
            }
            Notification.show("Username allineati: " + cambiati.size());
            updateGrid();
        } catch (RuntimeException ex) {
            Notification.show("Allineamento non riuscito: " + ex.getMessage());
        }
    }

    /** Ultimo invito ad accedere, vuoto se mai invitato. */
    private static String invito(User user) {
        return user.getInvitatoIl() != null ? Formati.dataOra(user.getInvitatoIl()) : "";
    }

    /**
     * Conferma con il numero di inviti (i selezionati con email mai entrati, gli altri saltati), poi invita; dice
     * quanti sono stati saltati.
     */
    private void invitaSelezionati() {
        List<User> selezionati = List.copyOf(grid.getSelectedItems());
        long invitabili = selezionati.stream().filter(RecuperoPasswordService::invitabile).count();
        if (invitabili == 0) {
            Notification.show("Nessun selezionato si può invitare: servono un'email e nessun accesso");
            return;
        }
        Dialog conferma = ViewSupport.dialog("Invita selezionati", "460px");
        Span testo = new Span("Invito ad accedere per email a " + invitabili + (invitabili == 1 ? " utente" : " utenti")
            + (selezionati.size() > invitabili ? "; " + (selezionati.size() - invitabili)
                + " saltati (senza email o già entrati: per loro \"Reimposta accesso\" sulla riga)." : "."));
        Button invia = new Button("Invia " + invitabili + (invitabili == 1 ? " invito" : " inviti"), e -> {
            conferma.close();
            eseguiInviti(selezionati);
        });
        invia.addThemeVariants(ButtonVariant.PRIMARY);
        conferma.add(testo);
        conferma.getFooter().add(ViewSupport.closeButton(conferma, "Annulla"), invia);
        conferma.open();
    }

    private void eseguiInviti(List<User> selezionati) {
        var esito = recuperoPasswordService.invitaTutti(selezionati.stream().map(User::getId).toList(),
            securityService.getUsername().orElse(""));
        Notification.show("Inviti inviati: " + esito.invitati()
            + (esito.saltati() > 0 ? " · saltati " + esito.saltati() + " (senza email o già entrati)" : ""));
        grid.deselectAll();
        updateGrid();
    }

    private static String ultimoAccesso(User user) {
        return user.getUltimoAccesso() != null ? Formati.dataOra(user.getUltimoAccesso()) : "Mai";
    }

    private HorizontalLayout createActions(User user) {
        Button editButton = ViewSupport.iconButton(VaadinIcon.PENCIL, "Modifica", e -> openDialog(user));
        Button deleteButton = ViewSupport.iconButton(VaadinIcon.TRASH, "Elimina", e -> deleteUser(user));
        deleteButton.addThemeVariants(ButtonVariant.ERROR);
        return ViewSupport.actions(editButton, InvitoUI.pulsante(user, this::updateGrid), deleteButton);
    }

    private void openDialog(User user) {
        Dialog dialog = ViewSupport.dialog(user.getId() == null ? "Nuovo Utente" : "Modifica Utente", "600px");

        FormLayout formLayout = new FormLayout();
        Binder<User> binder = new Binder<>(User.class);

        TextField username = new TextField("Username");
        PasswordField password = new PasswordField("Password");
        TextField nome = new TextField("Nome");
        TextField cognome = new TextField("Cognome");
        TextField email = new TextField("Email");
        TextField telefono = new TextField("Telefono");

        Checkbox attivo = new Checkbox("Attivo");
        attivo.setTooltipText("Solo un utente attivo può accedere all'app");

        CheckboxGroup<String> roles = new CheckboxGroup<>();
        roles.setLabel("Ruoli");
        roles.setItems(Arrays.asList("ADMIN", "SEGRETERIA", "INSEGNANTE", "ALLIEVO", "GENITORE"));

        if (user.getId() != null) {
            password.setPlaceholder("Lascia vuoto per non modificare");
        } else {
            password.setRequiredIndicatorVisible(true);
        }

        // Obbligatori: username (anche i soli spazi contano come vuoto, salvato senza spazi ai lati), nome e cognome
        boolean nuovo = user.getId() == null;
        String usernameAttuale = user.getUsername();
        binder.forField(username).asRequired("Inserisci lo username")
            .withValidator(u -> u != null && !u.isBlank(), "Inserisci lo username")
            .withValidator(u -> u.strip().equals(usernameAttuale) || !userService.existsByUsername(u.strip()), "Username già in uso")
            .bind(User::getUsername, (u, v) -> u.setUsername(v.strip()));
        binder.forField(nome).asRequired("Inserisci il nome").bind(User::getNome, User::setNome);
        binder.forField(cognome).asRequired("Inserisci il cognome").bind(User::getCognome, User::setCognome);
        binder.forField(email).bind(User::getEmail, User::setEmail);
        binder.forField(telefono).bind(User::getTelefono, User::setTelefono);
        binder.forField(attivo).bind(User::isEnabled, User::setEnabled);
        binder.forField(roles).bind(User::getRoles, User::setRoles);

        binder.readBean(user);

        formLayout.add(username, password, nome, cognome, email, telefono, attivo, roles);

        Button saveButton = new Button("Salva", e -> {
            // Errori sui campi, mai un valore mancante che fallisce sul database
            boolean passwordOk = !nuovo || !password.isEmpty();
            password.setInvalid(!passwordOk);
            password.setErrorMessage(passwordOk ? null : "Inserisci la password");
            if (!(binder.validate().isOk() & passwordOk) || !binder.writeBeanIfValid(user)) {
                return;
            }
            try {
                // Con i ruoli Insegnante o Genitore crea anche la scheda che manca, così compare in quegli elenchi
                schedeRuoliService.salvaUtente(user, password.getValue());
                updateGrid();
                dialog.close();
                Notification.show("Utente salvato con successo");
            } catch (Exception ex) {
                Notification.show("Errore nel salvataggio: " + ex.getMessage());
            }
        });
        saveButton.addThemeVariants(ButtonVariant.PRIMARY);

        dialog.add(formLayout);
        dialog.getFooter().add(ViewSupport.closeButton(dialog, "Annulla"), saveButton);
        // Username generato non più attuale: si allinea a nome e cognome salvati
        if (user.getId() != null) {
            userService.proposta(user).ifPresent(proposto -> {
                Button allinea = new Button("Allinea username (" + proposto + ")", VaadinIcon.EXCHANGE.create(), e -> {
                    dialog.close();
                    allinea(List.of(user.getId()));
                });
                allinea.setTooltipText("Lo username generato non corrisponde più a nome e cognome");
                dialog.getFooter().addComponentAsFirst(allinea);
            });
        }
        dialog.open();
    }

    /** Conferma con le schede che spariscono insieme all'utente; se non si può eliminare, solo il motivo. */
    private void deleteUser(User user) {
        String corrente = securityService.getUsername().orElse("");
        EliminazioneUtente verifica = schedeRuoliService.verificaEliminazione(user.getId(), corrente);
        if (!verifica.possibile()) {
            Notification.show("Impossibile eliminare " + user.getUsername() + ": " + verifica.motivoBlocco());
            return;
        }
        Dialog conferma = ViewSupport.dialog("Eliminare l'utente?", "460px");
        conferma.add(new Span(user.getUsername() + " (" + user.getNomeCompleto() + ")"
            + (verifica.schede().isEmpty() ? "" : ": si eliminano anche " + String.join(", ", verifica.schede())) + "."));
        Button elimina = new Button("Elimina", e -> {
            try {
                schedeRuoliService.eliminaUtente(user.getId(), corrente);
                conferma.close();
                updateGrid();
                Notification.show("Utente eliminato");
            } catch (RuntimeException ex) {
                Notification.show(ex.getMessage());
            }
        });
        elimina.addThemeVariants(ButtonVariant.PRIMARY, ButtonVariant.ERROR);
        conferma.getFooter().add(ViewSupport.closeButton(conferma, "Annulla"), elimina);
        conferma.open();
    }

    private void updateGrid() {
        tutti = userService.findAll();
        incongruenze = schedeRuoliService.incongruenze();
        utenti = grid.setItems(tutti);
        applicaFiltri();
    }

    /** Solo incongruenze e ricerca rapida; "N di M" sugli utenti del primo filtro. */
    private void applicaFiltri() {
        if (utenti != null) {
            SerializablePredicate<User> filtro = user -> (!soloIncongruenze.getValue() || incongruenze.containsKey(user.getId()))
                && (!soloSospesi.getValue() || user.isSospesoPerInattivita());
            utenti.setFilter(user -> filtro.test(user) && ricerca.corrisponde(RicercaRapida.testo(user)));
            ricerca.mostraConteggio(utenti.getItemCount(), (int) tutti.stream().filter(filtro).count());
        }
    }

    /** Un badge di avviso per ogni incongruenza tra ruoli e schede dell'utente; vuoto se è tutto a posto. */
    private Component avvisi(User user) {
        Div avvisi = new Div();
        avvisi.addClassNames("flex flex-wrap gap-1");
        for (String incongruenza : incongruenze.getOrDefault(user.getId(), List.of())) {
            Badge badge = new Badge(incongruenza, VaadinIcon.WARNING.create());
            badge.addThemeVariants(BadgeVariant.WARNING);
            avvisi.add(badge);
        }
        return avvisi;
    }

    private void creaSchedeMancanti() {
        int create = schedeRuoliService.completaTutte();
        updateGrid();
        Notification.show(create == 0 ? "Nessuna scheda da creare"
            : create == 1 ? "Creata 1 scheda mancante" : "Create " + create + " schede mancanti");
    }
}
