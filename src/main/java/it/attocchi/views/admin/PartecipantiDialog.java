package it.attocchi.views.admin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.timepicker.TimePicker;
import it.attocchi.entity.Corso;
import it.attocchi.entity.Iscrizione;
import it.attocchi.entity.Allievo;
import it.attocchi.service.CorsoService;
import it.attocchi.service.PagamentoService;
import it.attocchi.service.PagamentoService.Situazione;
import it.attocchi.service.AllievoService;
import it.attocchi.views.CardMobile;
import it.attocchi.views.StatoPagamentoUI;
import it.attocchi.views.Stili;
import it.attocchi.views.ViewSupport;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
    private final AllievoService allievoService;
    private final PagamentoService pagamentoService;
    private final Corso corso;
    private final Grid<Iscrizione> grid = new Grid<>(Iscrizione.class, false);
    private final ComboBox<Allievo> nuovoPartecipante = new ComboBox<>();
    private Map<Long, Situazione> situazioni = Map.of();

    public PartecipantiDialog(CorsoService corsoService, AllievoService allievoService,
                              PagamentoService pagamentoService, Corso corso) {
        this.corsoService = corsoService;
        this.allievoService = allievoService;
        this.pagamentoService = pagamentoService;
        this.corso = corso;

        setHeaderTitle("Partecipanti – " + corso.getNome());
        setWidth("600px");
        setMaxWidth("100%");

        nuovoPartecipante.setPlaceholder("Aggiungi un allievo");
        nuovoPartecipante.setAriaLabel("Allievo da aggiungere");
        nuovoPartecipante.setItemLabelGenerator(Allievo::getNomeCompleto);
        nuovoPartecipante.setClearButtonVisible(true);
        // Un nome che non corrisponde a nessun allievo apre l'inserimento rapido
        nuovoPartecipante.setAllowCustomValue(true);
        nuovoPartecipante.setHelperText("Non c'è? Scrivi nome e cognome e premi Invio per crearlo");
        nuovoPartecipante.addCustomValueSetListener(e -> nuovoAllievo(e.getDetail()));
        Button aggiungi = new Button("Aggiungi", VaadinIcon.PLUS.create(), e -> aggiungi());
        aggiungi.addThemeVariants(ButtonVariant.PRIMARY);
        aggiungi.setEnabled(false);
        nuovoPartecipante.addValueChangeListener(e -> aggiungi.setEnabled(e.getValue() != null));

        HorizontalLayout aggiunta = new HorizontalLayout(nuovoPartecipante, aggiungi);
        aggiunta.addClassNames("w-full items-baseline");
        nuovoPartecipante.addClassNames("grow");

        grid.addComponentColumn(this::createOrario).setHeader("Orario").setWidth("9.5rem").setFlexGrow(0);
        grid.addColumn(iscrizione -> iscrizione.getAllievo().getNomeCompleto()).setHeader("Partecipante");
        grid.addComponentColumn(iscrizione -> StatoPagamentoUI.badge(situazione(iscrizione)))
            .setHeader("Pagamento").setWidth("9rem").setFlexGrow(0);
        boolean modificabile = corso.isAttivo();
        // Quota e pagamenti sempre disponibili (anche per i corsi non attivi); rimozione solo per i corsi attivi
        var azioni = grid.addComponentColumn(iscrizione -> createAzioni(iscrizione, modificabile));
        ViewSupport.configureActionsColumn(azioni);
        azioni.setHeader("").setAutoWidth(false).setWidth(modificabile ? "6rem" : "3.5rem");
        // Su schermi stretti il nome va a capo invece di essere troncato
        grid.addClassName("partecipanti-grid");
        CardMobile.per(grid)
            .titolo(iscrizione -> iscrizione.getAllievo().getNomeCompleto())
            .stato(iscrizione -> StatoPagamentoUI.badge(situazione(iscrizione)))
            .campo("Orario", this::createOrario)
            .azioni(iscrizione -> createAzioni(iscrizione, modificabile))
            .applica();
        grid.setAllRowsVisible(true);
        aggiorna();

        VerticalLayout content = new VerticalLayout(aggiunta, grid);
        if (!modificabile) {
            aggiunta.setVisible(false);
            Span avviso = new Span("Corso non attivo: partecipanti e orari non sono modificabili.");
            avviso.addClassNames(Stili.TESTO_SECONDARIO);
            content.addComponentAsFirst(avviso);
        }
        content.addClassNames("p-0");
        add(content);
        Button conferme = new Button("Invia conferme", VaadinIcon.ENVELOPE.create(),
            e -> InvioEmailDialog.inviaConferme(corso, corsoService.findPartecipanti(corso.getId()).size(), () -> { }));
        conferme.setTooltipText("Invia per email la conferma di iscrizione a tutti gli iscritti");
        getFooter().add(conferme, ViewSupport.closeButton(this, "Chiudi"));
    }

    private HorizontalLayout createAzioni(Iscrizione iscrizione, boolean modificabile) {
        Button pagamenti = ViewSupport.iconButton(VaadinIcon.EURO, "Quota e pagamenti di " + iscrizione.getAllievo().getNomeCompleto(),
            e -> openQuotaPagamenti(iscrizione));
        StatoPagamentoUI.coloraPulsante(pagamenti, "Quota e pagamenti", situazione(iscrizione));
        HorizontalLayout actions = ViewSupport.actions(pagamenti);
        if (modificabile) {
            Button rimuovi = ViewSupport.iconButton(VaadinIcon.TRASH, "Rimuovi " + iscrizione.getAllievo().getNomeCompleto(),
                e -> rimuovi(iscrizione));
            rimuovi.addThemeVariants(ButtonVariant.ERROR);
            actions.add(rimuovi);
        }
        return actions;
    }

    private TimePicker createOrario(Iscrizione iscrizione) {
        TimePicker orario = ViewSupport.timePicker(null);
        orario.setStep(Duration.ofMinutes(5));
        orario.setPlaceholder("da assegnare");
        orario.setClearButtonVisible(true);
        orario.addClassNames("w-full");
        orario.setAriaLabel("Orario lezione di " + iscrizione.getAllievo().getNomeCompleto());
        orario.setValue(iscrizione.getOrarioLezione());
        orario.setReadOnly(!corso.isAttivo());
        orario.addValueChangeListener(e -> {
            try {
                corsoService.impostaOrario(corso.getId(), iscrizione.getAllievo().getId(), e.getValue());
                aggiorna();
            } catch (RuntimeException ex) {
                Notification.show("Errore nel salvataggio dell'orario di "
                    + iscrizione.getAllievo().getNomeCompleto() + ": " + ex.getMessage());
            }
        });
        return orario;
    }

    private void openQuotaPagamenti(Iscrizione iscrizione) {
        QuotaPagamentiDialog dialog = new QuotaPagamentiDialog(pagamentoService, corso, iscrizione.getAllievo());
        dialog.addOpenedChangeListener(e -> {
            if (!e.isOpened()) {
                aggiorna();
            }
        });
        dialog.open();
    }

    private void aggiungi() {
        iscrivi(nuovoPartecipante.getValue());
    }

    private void iscrivi(Allievo allievo) {
        try {
            corsoService.iscrivi(corso.getId(), allievo.getId());
            nuovoPartecipante.clear();
            aggiorna();
        } catch (RuntimeException ex) {
            Notification.show("Errore nell'aggiunta di " + allievo.getNomeCompleto() + ": " + ex.getMessage());
        }
    }

    /** Inserimento rapido di un allievo che non esiste, con nome e cognome proposti dal testo scritto. */
    private void nuovoAllievo(String testo) {
        nuovoPartecipante.clear();
        if (testo == null || testo.isBlank()) {
            return;
        }
        Set<Long> iscritti = corsoService.findPartecipanti(corso.getId()).stream()
            .map(i -> i.getAllievo().getId()).collect(Collectors.toSet());
        new NuovoAllievoRapidoDialog(allievoService, testo, iscritti, this::iscrivi).open();
    }

    private void rimuovi(Iscrizione iscrizione) {
        try {
            corsoService.rimuoviIscrizione(corso.getId(), iscrizione.getAllievo().getId());
            aggiorna();
        } catch (RuntimeException ex) {
            Notification.show("Errore nella rimozione di " + iscrizione.getAllievo().getNomeCompleto() + ": " + ex.getMessage());
        }
    }

    /** Ricarica i partecipanti e propone come nuovi solo gli allievi non ancora iscritti. */
    private Situazione situazione(Iscrizione iscrizione) {
        return situazioni.getOrDefault(iscrizione.getAllievo().getId(),
            new Situazione(iscrizione.getQuotaDovuta(), BigDecimal.ZERO));
    }

    private void aggiorna() {
        situazioni = pagamentoService.situazioniDelCorso(corso.getId());
        List<Iscrizione> partecipanti = corsoService.findPartecipanti(corso.getId());
        grid.setItems(partecipanti);
        Set<Long> iscritti = partecipanti.stream().map(i -> i.getAllievo().getId()).collect(Collectors.toSet());
        nuovoPartecipante.setItems(allievoService.findAll().stream()
            .filter(allievo -> !iscritti.contains(allievo.getId()))
            .sorted(Comparator.comparing(Allievo::getNomeCompleto))
            .toList());
    }
}
