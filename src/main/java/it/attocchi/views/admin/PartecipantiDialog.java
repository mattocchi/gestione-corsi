package it.attocchi.views.admin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.timepicker.TimePicker;
import it.attocchi.entity.Corso;
import it.attocchi.entity.Iscrizione;
import it.attocchi.entity.Studente;
import it.attocchi.service.CorsoService;
import it.attocchi.service.PagamentoService;
import it.attocchi.service.StudenteService;
import it.attocchi.views.ViewSupport;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Partecipanti di un corso in ordine di orario di inizio lezione (chi non ha ancora un orario
 * è in fondo), con aggiunta, rimozione e orario modificabile. Le modifiche sono salvate subito
 * e l'elenco si riordina; la notifica compare solo in caso di errore. Per un corso non attivo
 * partecipanti e orari sono in sola lettura.
 */
public class PartecipantiDialog extends Dialog {

    private final CorsoService corsoService;
    private final StudenteService studenteService;
    private final PagamentoService pagamentoService;
    private final Corso corso;
    private final Grid<Iscrizione> grid = new Grid<>(Iscrizione.class, false);
    private final ComboBox<Studente> nuovoPartecipante = new ComboBox<>();

    public PartecipantiDialog(CorsoService corsoService, StudenteService studenteService,
                              PagamentoService pagamentoService, Corso corso) {
        this.corsoService = corsoService;
        this.studenteService = studenteService;
        this.pagamentoService = pagamentoService;
        this.corso = corso;

        setHeaderTitle("Partecipanti – " + corso.getNome());
        setWidth("600px");
        setMaxWidth("100%");

        nuovoPartecipante.setPlaceholder("Aggiungi uno studente");
        nuovoPartecipante.setAriaLabel("Studente da aggiungere");
        nuovoPartecipante.setItemLabelGenerator(Studente::getNomeCompleto);
        nuovoPartecipante.setClearButtonVisible(true);
        Button aggiungi = new Button("Aggiungi", VaadinIcon.PLUS.create(), e -> aggiungi());
        aggiungi.addThemeVariants(ButtonVariant.PRIMARY);
        aggiungi.setEnabled(false);
        nuovoPartecipante.addValueChangeListener(e -> aggiungi.setEnabled(e.getValue() != null));

        HorizontalLayout aggiunta = new HorizontalLayout(nuovoPartecipante, aggiungi);
        aggiunta.setWidthFull();
        aggiunta.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);
        aggiunta.expand(nuovoPartecipante);

        grid.addComponentColumn(this::createOrario).setHeader("Orario").setWidth("9.5rem").setFlexGrow(0);
        grid.addColumn(iscrizione -> iscrizione.getStudente().getNomeCompleto()).setHeader("Partecipante");
        boolean modificabile = corso.isAttivo();
        // Quota e pagamenti sempre disponibili (anche per i corsi non attivi); rimozione solo per i corsi attivi
        var azioni = grid.addComponentColumn(iscrizione -> {
            Button pagamenti = ViewSupport.iconButton(VaadinIcon.EURO, "Quota e pagamenti di " + iscrizione.getStudente().getNomeCompleto(),
                e -> openQuotaPagamenti(iscrizione));
            HorizontalLayout actions = ViewSupport.actions(pagamenti);
            if (modificabile) {
                Button rimuovi = ViewSupport.iconButton(VaadinIcon.TRASH, "Rimuovi " + iscrizione.getStudente().getNomeCompleto(),
                    e -> rimuovi(iscrizione));
                rimuovi.addThemeVariants(ButtonVariant.ERROR);
                actions.add(rimuovi);
            }
            return actions;
        });
        ViewSupport.configureActionsColumn(azioni);
        azioni.setHeader("").setAutoWidth(false).setWidth(modificabile ? "6rem" : "3.5rem");
        // Su schermi stretti il nome va a capo invece di essere troncato
        grid.addClassName("partecipanti-grid");
        grid.setAllRowsVisible(true);
        aggiorna();

        VerticalLayout content = new VerticalLayout(aggiunta, grid);
        if (!modificabile) {
            aggiunta.setVisible(false);
            Span avviso = new Span("Corso non attivo: partecipanti e orari non sono modificabili.");
            avviso.addClassName("avviso-sola-lettura");
            content.addComponentAsFirst(avviso);
        }
        content.setPadding(false);
        add(content);
        getFooter().add(ViewSupport.closeButton(this, "Chiudi"));
    }

    private TimePicker createOrario(Iscrizione iscrizione) {
        TimePicker orario = new TimePicker();
        orario.setLocale(Locale.ITALY);
        orario.setStep(Duration.ofMinutes(5));
        orario.setPlaceholder("da assegnare");
        orario.setClearButtonVisible(true);
        orario.setWidthFull();
        orario.setAriaLabel("Orario lezione di " + iscrizione.getStudente().getNomeCompleto());
        orario.setValue(iscrizione.getOrarioLezione());
        orario.setReadOnly(!corso.isAttivo());
        orario.addValueChangeListener(e -> {
            try {
                corsoService.impostaOrario(corso.getId(), iscrizione.getStudente().getId(), e.getValue());
                aggiorna();
            } catch (RuntimeException ex) {
                Notification.show("Errore nel salvataggio dell'orario di "
                    + iscrizione.getStudente().getNomeCompleto() + ": " + ex.getMessage());
            }
        });
        return orario;
    }

    private void openQuotaPagamenti(Iscrizione iscrizione) {
        QuotaPagamentiDialog dialog = new QuotaPagamentiDialog(pagamentoService, corso, iscrizione.getStudente());
        dialog.addOpenedChangeListener(e -> {
            if (!e.isOpened()) {
                aggiorna();
            }
        });
        dialog.open();
    }

    private void aggiungi() {
        Studente studente = nuovoPartecipante.getValue();
        try {
            corsoService.iscrivi(corso.getId(), studente.getId());
            nuovoPartecipante.clear();
            aggiorna();
        } catch (RuntimeException ex) {
            Notification.show("Errore nell'aggiunta di " + studente.getNomeCompleto() + ": " + ex.getMessage());
        }
    }

    private void rimuovi(Iscrizione iscrizione) {
        try {
            corsoService.rimuoviIscrizione(corso.getId(), iscrizione.getStudente().getId());
            aggiorna();
        } catch (RuntimeException ex) {
            Notification.show("Errore nella rimozione di " + iscrizione.getStudente().getNomeCompleto() + ": " + ex.getMessage());
        }
    }

    /** Ricarica i partecipanti e propone come nuovi solo gli studenti non ancora iscritti. */
    private void aggiorna() {
        List<Iscrizione> partecipanti = corsoService.findPartecipanti(corso.getId());
        grid.setItems(partecipanti);
        Set<Long> iscritti = partecipanti.stream().map(i -> i.getStudente().getId()).collect(Collectors.toSet());
        nuovoPartecipante.setItems(studenteService.findAll().stream()
            .filter(studente -> !iscritti.contains(studente.getId()))
            .sorted(Comparator.comparing(Studente::getNomeCompleto))
            .toList());
    }
}
