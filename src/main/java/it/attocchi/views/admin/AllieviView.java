package it.attocchi.views.admin;

import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import it.attocchi.entity.Genitore;
import it.attocchi.entity.Allievo;
import it.attocchi.entity.Corso;
import it.attocchi.entity.User;
import it.attocchi.security.SecurityService;
import it.attocchi.service.GenitoreService;
import it.attocchi.service.AllievoService;
import it.attocchi.service.CorsoService;
import it.attocchi.service.DocumentiService;
import it.attocchi.service.UserService;
import it.attocchi.views.AnagraficaFields;
import it.attocchi.views.CardMobile;
import it.attocchi.views.Formati;
import it.attocchi.views.MainLayout;
import it.attocchi.views.RicercaRapida;
import it.attocchi.views.Stili;
import it.attocchi.views.UtenteFields;
import it.attocchi.views.ViewSupport;
import jakarta.annotation.security.RolesAllowed;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Route(value = "allievi", layout = MainLayout.class)
@PageTitle("Gestione Allievi")
@Menu(title = "Gestione Allievi", order = 11, icon = "vaadin:academy-cap")
@RolesAllowed({"ADMIN", "SEGRETERIA"})
public class AllieviView extends VerticalLayout implements BeforeEnterObserver {

    private final AllievoService allievoService;
    private final UserService userService;
    private final GenitoreService genitoreService;
    /** L'eliminazione degli allievi è riservata all'admin. */
    private final boolean puoEliminare;
    /** Dialog di modifica aperto, chiuso lasciando la pagina (es. Indietro dopo un link a un'altra scheda). */
    private Dialog dialogAperto;

    private final Grid<Allievo> grid = new Grid<>(Allievo.class, false);
    private final RicercaRapida ricerca = new RicercaRapida();
    private final ComboBox<FiltroCorso> filtroCorso = new ComboBox<>();
    private GridListDataView<Allievo> allievi;
    private List<Allievo> tutti = List.of();

    /** Scelta del filtro per corso: un corso, oppure ({@code corso == null}) gli allievi senza corsi. */
    public record FiltroCorso(Corso corso) {

        static final FiltroCorso SENZA_CORSI = new FiltroCorso(null);

        String etichetta() {
            if (corso == null) {
                return "Senza corsi";
            }
            return corso.getAnnoScolastico() != null ? corso.getNome() + " (" + corso.getAnnoScolastico() + ")" : corso.getNome();
        }

        boolean ammette(Allievo allievo) {
            return corso == null ? allievo.getCorsi().isEmpty()
                : allievo.getCorsi().stream().anyMatch(c -> c.getId().equals(corso.getId()));
        }
    }

    public AllieviView(AllievoService allievoService, UserService userService, GenitoreService genitoreService,
                        CorsoService corsoService, SecurityService securityService) {
        this.allievoService = allievoService;
        this.userService = userService;
        this.genitoreService = genitoreService;
        this.puoEliminare = securityService.hasRole("ADMIN");

        addClassNames("size-full");
        configureGrid();

        Button addButton = new Button("Nuovo Allievo", e -> openDialog(new Allievo()));
        // Filtro per corso: prima "Senza corsi", poi i corsi attivi e gli altri, per nome
        filtroCorso.setPlaceholder("Tutti i corsi");
        filtroCorso.setAriaLabel("Filtra per corso");
        filtroCorso.setClearButtonVisible(true);
        filtroCorso.setItemLabelGenerator(FiltroCorso::etichetta);
        filtroCorso.setItems(Stream.concat(Stream.of(FiltroCorso.SENZA_CORSI), corsoService.findAll().stream()
            .sorted(Comparator.comparing((Corso c) -> !c.isAttivo())
                .thenComparing(Corso::getNome, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Corso::getAnnoScolastico, Comparator.nullsLast(Comparator.reverseOrder())))
            .map(FiltroCorso::new)).toList());
        filtroCorso.addClassNames("w-60");
        filtroCorso.addValueChangeListener(e -> applicaFiltri());
        ricerca.addRicercaListener(this::applicaFiltri);
        HorizontalLayout toolbar = new HorizontalLayout(addButton, filtroCorso, ricerca);
        toolbar.addClassNames("flex-wrap w-full items-center");
        add(toolbar, grid);
        grid.addClassNames("grow");

        updateGrid();
    }

    private void configureGrid() {
        grid.addComponentColumn(AllieviView::nome).setHeader("Nome")
            .setComparator(Allievo::getNomeCompleto);
        grid.addColumn(Allievo::getCodiceFiscale).setHeader("Codice Fiscale");
        grid.addColumn(allievo -> Formati.dataNumerica(allievo.getDataNascita())).setHeader("Data Nascita")
            .setComparator(Allievo::getDataNascita).setAutoWidth(true).setFlexGrow(0);
        // Anni compiuti a oggi
        grid.addColumn(Formati::eta).setHeader("Età")
            .setComparator(Formati.PER_ETA).setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(allievo -> allievo.getGenitore() != null ? allievo.getGenitore().getNomeCompleto() : "")
            .setHeader("Genitore");
        grid.addColumn(Formati::indirizzo).setHeader("Indirizzo");
        grid.addColumn(allievo -> allievo.getCorsi().size()).setHeader("N. Corsi").setAutoWidth(true).setFlexGrow(0);

        ViewSupport.configureActionsColumn(grid.addComponentColumn(this::createActions));

        CardMobile.per(grid)
            .titolo(Allievo::getNomeCompleto)
            .stato(AllieviView::datiMancanti)
            .campo("Codice Fiscale", Allievo::getCodiceFiscale)
            .campo("Data Nascita", allievo -> Formati.dataNumerica(allievo.getDataNascita()))
            .campo("Età", Formati::eta)
            .campo("Genitore", allievo -> allievo.getGenitore() != null ? allievo.getGenitore().getNomeCompleto() : null)
            .campo("Indirizzo", Formati::indirizzo)
            .campo("N. Corsi", allievo -> allievo.getCorsi().size())
            .azioni(this::createActions)
            .applica();
    }

    private HorizontalLayout createActions(Allievo allievo) {
        Button editButton = ViewSupport.iconButton(VaadinIcon.PENCIL, "Modifica", e -> openDialog(allievo));
        HorizontalLayout actions = ViewSupport.actions(editButton);
        if (puoEliminare) {
            Button deleteButton = ViewSupport.iconButton(VaadinIcon.TRASH, "Elimina", e -> deleteAllievo(allievo));
            deleteButton.addThemeVariants(ButtonVariant.ERROR);
            actions.add(deleteButton);
        }
        return actions;
    }

    /** Nome con il badge "Dati mancanti" se manca il codice fiscale (ricevute) o la data di nascita (ogni documento). */
    private static HorizontalLayout nome(Allievo allievo) {
        HorizontalLayout cella = new HorizontalLayout(new Span(allievo.getNomeCompleto()));
        cella.addClassNames("items-center");
        Badge badge = datiMancanti(allievo);
        if (badge != null) {
            cella.add(badge);
        }
        return cella;
    }

    /** Badge "Dati mancanti" con l'elenco nel tooltip, {@code null} se i dati per i documenti ci sono. */
    private static Badge datiMancanti(Allievo allievo) {
        String avviso = DocumentiService.avvisoDatiMancanti(allievo);
        if (avviso == null) {
            return null;
        }
        Badge badge = new Badge("Dati mancanti", VaadinIcon.WARNING.create());
        badge.addThemeVariants(BadgeVariant.WARNING);
        Tooltip.forComponent(badge).setText(avviso);
        return badge;
    }

    /** Con {@code ?id=…} (link dalla scheda del genitore) apre il dialog di quell'allievo. */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        SchedaCollegata.id(event).flatMap(id -> tutti.stream().filter(a -> a.getId().equals(id)).findFirst())
            .ifPresent(this::openDialog);
    }

    @Override
    protected void onDetach(DetachEvent event) {
        if (dialogAperto != null) {
            dialogAperto.close();
        }
    }

    private void openDialog(Allievo allievo) {
        boolean nuovo = allievo.getId() == null;
        Dialog dialog = ViewSupport.dialog(nuovo ? "Nuovo Allievo" : "Modifica Allievo", "700px");

        // Nome, cognome, contatti e credenziali sono dell'utente collegato all'allievo
        User user = nuovo ? new User() : allievo.getUser();
        UtenteFields utente = new UtenteFields(user, userService, true);

        Binder<Allievo> binder = new Binder<>(Allievo.class);
        ComboBox<Genitore> genitore = new ComboBox<>("Genitore");
        genitore.setItems(genitoreService.findAll());
        genitore.setItemLabelGenerator(Genitore::getNomeCompleto);
        // Facoltativo: gli allievi adulti non hanno genitore (documenti ed email vanno all'allievo)
        genitore.setClearButtonVisible(true);
        genitore.setHelperText("Lascia vuoto per gli allievi adulti; scrivi un nome nuovo e premi Invio per crearlo");
        binder.forField(genitore).bind(Allievo::getGenitore, Allievo::setGenitore);
        // Genitore non ancora presente: si crea al volo scrivendone il nome (Invio) o con il pulsante "+"
        genitore.setAllowCustomValue(true);
        genitore.addCustomValueSetListener(e -> nuovoGenitore(genitore, e.getDetail()));
        Button nuovoGenitore = ViewSupport.iconButton(VaadinIcon.PLUS, "Nuovo genitore", e -> nuovoGenitore(genitore, ""));
        nuovoGenitore.setTooltipText("Nuovo genitore");
        genitore.addClassNames("grow");
        HorizontalLayout sceltaGenitore = new HorizontalLayout(genitore, nuovoGenitore);
        sceltaGenitore.addClassNames("w-full items-baseline gap-2");
        AnagraficaFields<Allievo> anagrafica = new AnagraficaFields<>(binder, true);
        binder.readBean(allievo);

        Span indirizzoGenitore = new Span();
        indirizzoGenitore.addClassNames(Stili.TESTO_SECONDARIO);
        genitore.addValueChangeListener(e -> mostraIndirizzoGenitore(indirizzoGenitore, e.getValue()));
        mostraIndirizzoGenitore(indirizzoGenitore, genitore.getValue());

        FormLayout utenteForm = new FormLayout(utente.getComponents());
        // Link alla scheda del genitore già collegato (non a quello appena scelto nel campo)
        if (!nuovo && allievo.getGenitore() != null) {
            VerticalLayout genitoreECollegamento = new VerticalLayout(sceltaGenitore, SchedaCollegata.link(
                allievo.getGenitore().getNomeCompleto(), dialog, () -> binder.hasChanges() || utente.hasChanges(),
                GenitoriView.class, allievo.getGenitore().getId()));
            genitoreECollegamento.addClassNames("p-0 gap-0 items-start");
            utenteForm.add(genitoreECollegamento);
        } else {
            utenteForm.add(sceltaGenitore);
        }
        VerticalLayout content = new VerticalLayout(
            ViewSupport.sezione("Utente e genitore"), utenteForm,
            ViewSupport.sezione("Dati anagrafici"), new FormLayout(anagrafica.getComponents()), indirizzoGenitore);
        content.addClassNames("p-0");

        Button saveButton = new Button("Salva", e -> {
            // Valida tutto prima di salvare: niente utenti creati per un allievo che poi non si salva
            boolean valido = binder.validate().isOk() & utente.validate();
            if (!valido || !binder.writeBeanIfValid(allievo) || !utente.writeBeanIfValid(user)) {
                return;
            }
            try {
                allievoService.salva(allievo, user, utente.getPassword());
                updateGrid();
                dialog.close();
                Notification.show("Allievo salvato con successo");
            } catch (RuntimeException ex) {
                Notification.show("Errore nel salvataggio: " + ex.getMessage());
            }
        });
        saveButton.addThemeVariants(ButtonVariant.PRIMARY);

        dialog.add(content);
        dialog.getFooter().add(ViewSupport.closeButton(dialog, "Annulla"), saveButton);
        dialogAperto = dialog;
        dialog.open();
    }

    /** Crea un genitore al volo ({@link NuovoGenitoreRapidoDialog}) e lo sceglie nel campo. */
    private void nuovoGenitore(ComboBox<Genitore> campo, String testo) {
        new NuovoGenitoreRapidoDialog(genitoreService, testo, scelto -> {
            campo.setItems(genitoreService.findAll());
            campo.setValue(scelto);
        }).open();
    }

    /** Indirizzo del genitore, mostrato quando l'allievo non ne ha uno proprio. */
    private static void mostraIndirizzoGenitore(Span span, Genitore genitore) {
        String indirizzo = genitore != null ? genitore.getIndirizzo().getFormattato() : "";
        span.setText("Se l'indirizzo è vuoto si mostra quello del genitore: " + indirizzo);
        span.setVisible(!indirizzo.isEmpty());
    }

    private void deleteAllievo(Allievo allievo) {
        try {
            allievoService.delete(allievo);
            updateGrid();
            Notification.show("Allievo eliminato");
        } catch (RuntimeException ex) {
            Notification.show("Impossibile eliminare l'allievo: " + ex.getMessage());
        }
    }

    private void updateGrid() {
        tutti = allievoService.findAll();
        allievi = grid.setItems(tutti);
        applicaFiltri();
    }

    /** Corso scelto e ricerca rapida; "N di M" sugli allievi del corso scelto. */
    private void applicaFiltri() {
        if (allievi != null) {
            FiltroCorso scelto = filtroCorso.getValue();
            SerializablePredicate<Allievo> delCorso = allievo -> scelto == null || scelto.ammette(allievo);
            allievi.setFilter(allievo -> delCorso.test(allievo) && ricerca.corrisponde(RicercaRapida.testo(
                RicercaRapida.testo(allievo.getUser()), RicercaRapida.testo(allievo), Formati.indirizzo(allievo),
                allievo.getGenitore() != null ? allievo.getGenitore().getNomeCompleto() : null,
                allievo.getCorsi().stream().map(Corso::getNome).collect(Collectors.joining(" ")))));
            ricerca.mostraConteggio(allievi.getItemCount(), (int) tutti.stream().filter(delCorso).count());
        }
    }
}
