package it.attocchi.views;

import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.signals.Signal;
import it.attocchi.entity.Lezione;
import it.attocchi.entity.Presenza;
import it.attocchi.service.LezioneService;
import it.attocchi.service.PresenzaService;
import it.attocchi.views.insegnante.QuandoLezione;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

/**
 * Lista delle lezioni con filtro per stato, badge sulla data e dialog delle presenze: usata da "Le Mie Lezioni"
 * dell'insegnante.
 */
public class ElencoLezioni extends VerticalLayout {

    /** Filtro della griglia per stato della lezione. */
    public enum FiltroLezioni {
        DA_FARE("Da fare", lezione -> !lezione.isCompletata()),
        COMPLETATE("Completate", Lezione::isCompletata),
        TUTTE("Tutte", lezione -> true);

        private final String etichetta;
        private final SerializablePredicate<Lezione> condizione;

        FiltroLezioni(String etichetta, SerializablePredicate<Lezione> condizione) {
            this.etichetta = etichetta;
            this.condizione = condizione;
        }

        public String getEtichetta() {
            return etichetta;
        }
    }

    private final LezioneService lezioneService;
    private final PresenzaService presenzaService;
    private final SerializableSupplier<List<Lezione>> fonte;

    private final Grid<Lezione> gridLezioni = new Grid<>(Lezione.class, false);
    private final SegmentedControl<FiltroLezioni> filtro = new SegmentedControl<>("Filtra le lezioni per stato",
        List.of(FiltroLezioni.values()), FiltroLezioni::getEtichetta, f -> "Mostra lezioni: " + f.getEtichetta(), null);
    private GridListDataView<Lezione> lezioni;
    private final Signal<Boolean> mobile = ViewSupport.isMobile();

    /** {@code fonte} fornisce le lezioni da mostrare, riletta dopo ogni modifica. */
    public ElencoLezioni(LezioneService lezioneService, PresenzaService presenzaService,
                         SerializableSupplier<List<Lezione>> fonte) {
        this.lezioneService = lezioneService;
        this.presenzaService = presenzaService;
        this.fonte = fonte;

        setSizeFull();
        setPadding(false);
        configureGrid();

        // Di default solo le lezioni ancora da fare
        filtro.setValue(FiltroLezioni.DA_FARE);
        filtro.getStyle().set("max-width", "24rem");
        filtro.addValueChangeListener(e -> applicaFiltro());

        add(filtro, gridLezioni);
        expand(gridLezioni);
        updateGrid();
    }

    private void configureGrid() {
        // Solo su mobile: corso e data in un'unica colonna su due righe, per non troncare nulla
        var lezione = gridLezioni.addComponentColumn(this::createRiepilogo)
            .setHeader("Lezione").setComparator(Lezione::getData).setSortable(true);
        var corso = gridLezioni.addColumn(l -> l.getCorso().getNome()).setHeader("Corso").setSortable(true);
        // Data con il badge "Oggi", "Settimana scorsa", … (QuandoLezione)
        var data = gridLezioni.addComponentColumn(this::createData).setHeader("Data")
            .setComparator(Lezione::getData).setSortable(true).setWidth("17rem").setFlexGrow(0);
        var numero = gridLezioni.addColumn(Lezione::getNumeroLezione).setHeader("N. Lezione");
        var orario = gridLezioni.addColumn(Lezione::getOrarioInizio).setHeader("Orario");
        // Stato come badge, non ordinabile (il filtro separa già gli stati). Larghezza fissa: quella automatica
        // verrebbe calcolata sulle sole righe visibili all'apertura ("Da fare") e troncherebbe "Completata".
        var stato = gridLezioni.addComponentColumn(this::createStatoBadge).setFlexGrow(0);
        var azioni = gridLezioni.addComponentColumn(l ->
            ViewSupport.actions(ViewSupport.iconButton(VaadinIcon.CLIPBOARD_CHECK, "Gestisci Presenze", e -> openPresenzeDialog(l))));
        ViewSupport.configureActionsColumn(azioni);
        azioni.setAutoWidth(false);

        // Mobile: Lezione | stato (solo icona) | azioni, senza intestazioni testuali che verrebbero troncate.
        // Desktop: tutte le colonne singole.
        Signal.effect(gridLezioni, () -> {
            boolean m = mobile.get();
            lezione.setVisible(m);
            corso.setVisible(!m);
            data.setVisible(!m);
            numero.setVisible(!m);
            orario.setVisible(!m);
            stato.setHeader(m ? "" : "Stato").setWidth(m ? "3rem" : "8.5rem");
            azioni.setHeader(m ? "" : "Azioni").setWidth(m ? "3.5rem" : "5.5rem");
        });
    }

    private Div createRiepilogo(Lezione lezione) {
        Div corso = new Div(lezione.getCorso().getNome());
        corso.addClassName("lezione-riepilogo-corso");
        Div riepilogo = new Div(corso, createData(lezione));
        riepilogo.addClassName("lezione-riepilogo");
        return riepilogo;
    }

    private Div createData(Lezione lezione) {
        Div data = new Div(new Span(Formati.data(lezione.getData())));
        data.addClassName("lezione-data");
        QuandoLezione.di(lezione, LocalDate.now()).ifPresent(quando -> {
            Badge badge = new Badge(quando.getEtichetta());
            badge.addThemeVariants(BadgeVariant.SMALL);
            if (quando.getVariante() != null) {
                badge.addThemeVariants(quando.getVariante());
            }
            data.add(badge);
        });
        return data;
    }

    private Badge createStatoBadge(Lezione lezione) {
        Badge badge = lezione.isCompletata()
            ? new Badge("Completata", VaadinIcon.CHECK.create())
            : new Badge("Da fare", VaadinIcon.CLOCK.create());
        if (lezione.isCompletata()) {
            badge.addThemeVariants(BadgeVariant.SUCCESS);
        }
        badge.bindThemeVariant(BadgeVariant.ICON_ONLY, mobile);
        return badge;
    }

    private void openPresenzeDialog(Lezione lezione) {
        Dialog dialog = ViewSupport.dialog(
            lezione.getCorso().getNome() + " – Lezione " + lezione.getNumeroLezione() + " (" + Formati.data(lezione.getData()) + ")", "800px");

        Grid<Presenza> gridPresenze = new Grid<>(Presenza.class, false);
        // Una sola colonna con nome, stato e note: su schermi stretti gli elementi vanno a capo
        gridPresenze.addComponentColumn(this::createPresenzaRiga).setHeader("Allievi");
        gridPresenze.setItems(presenzaService.findByLezione(lezione.getId()));

        Button completaButton = new Button("Segna Completata", e -> {
            lezione.setCompletata(true);
            lezioneService.save(lezione);
            dialog.close();
            updateGrid();
            Notification.show("Lezione completata");
        });
        completaButton.addThemeVariants(ButtonVariant.SUCCESS);
        completaButton.setEnabled(!lezione.isCompletata());

        dialog.add(gridPresenze);
        dialog.getFooter().add(ViewSupport.closeButton(dialog, "Chiudi"), completaButton);
        dialog.open();
    }

    private Div createPresenzaRiga(Presenza presenza) {
        Span nome = new Span(presenza.getAllievo().getNomeCompleto());
        nome.addClassName("presenza-nome");

        StatoPresenzaToggle stato = new StatoPresenzaToggle();
        stato.setValue(presenza.getStato());
        stato.addValueChangeListener(e -> salva(presenza, p -> p.setStato(e.getValue())));

        TextField note = new TextField();
        note.addClassName("presenza-note");
        note.setAriaLabel("Note per " + presenza.getAllievo().getNomeCompleto());
        note.setPlaceholder("Note");
        note.setValue(presenza.getNote() != null ? presenza.getNote() : "");
        note.addValueChangeListener(e -> salva(presenza, p -> p.setNote(e.getValue())));

        Div riga = new Div(nome, stato, note);
        riga.addClassName("presenza-riga");
        return riga;
    }

    /** Salva subito la modifica; il riscontro è lo stato visibile nella riga, la notifica solo in caso di errore. */
    private void salva(Presenza presenza, Consumer<Presenza> modifica) {
        modifica.accept(presenza);
        try {
            presenzaService.save(presenza);
        } catch (RuntimeException ex) {
            Notification.show("Errore nel salvataggio della presenza di "
                + presenza.getAllievo().getNomeCompleto() + ": " + ex.getMessage());
        }
    }

    private void updateGrid() {
        lezioni = gridLezioni.setItems(fonte.get());
        applicaFiltro();
    }

    private void applicaFiltro() {
        if (lezioni != null) {
            lezioni.setFilter(filtro.getValue().condizione);
        }
    }
}
