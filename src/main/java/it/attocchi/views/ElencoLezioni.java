package it.attocchi.views;

import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.signals.Signal;
import it.attocchi.entity.Corso;
import it.attocchi.entity.Lezione;
import it.attocchi.entity.Presenza;
import it.attocchi.service.LezioneService;
import it.attocchi.service.PresenzaService;
import it.attocchi.views.insegnante.QuandoLezione;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.function.Consumer;

/**
 * Lista delle lezioni con filtro per stato, badge sulla data, dialog delle presenze, lezioni non effettuate e
 * nuove lezioni: usata da "Le Mie Lezioni" dell'insegnante e dalla vista "Lezioni" di admin e segreteria.
 */
public class ElencoLezioni extends VerticalLayout {

    /** Filtro della griglia per stato della lezione. */
    public enum FiltroLezioni {
        DA_FARE("Da fare", Lezione::isDaFare),
        COMPLETATE("Completate", Lezione::isCompletata),
        NON_EFFETTUATE("Non effettuate", Lezione::isNonEffettuata),
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
    private final SerializableSupplier<List<Corso>> corsi;
    private final String username;
    /** Lezioni di più insegnanti (vista Lezioni di admin e segreteria): filtro per corso e colonna Insegnante. */
    private final boolean tuttiICorsi;
    private final ComboBox<Corso> filtroCorso = new ComboBox<>();
    private List<Lezione> elenco = List.of();

    private final Grid<Lezione> gridLezioni = new Grid<>(Lezione.class, false);
    private final SegmentedControl<FiltroLezioni> filtro = new SegmentedControl<>("Filtra le lezioni per stato",
        List.of(FiltroLezioni.values()), FiltroLezioni::getEtichetta, f -> "Mostra lezioni: " + f.getEtichetta(), null);
    private GridListDataView<Lezione> lezioni;
    private final Signal<Boolean> mobile = ViewSupport.isMobile();

    /**
     * {@code fonte} fornisce le lezioni da mostrare, riletta dopo ogni modifica; {@code corsi} i corsi attivi a cui
     * si possono aggiungere lezioni; {@code username} è l'utente corrente, di cui il service verifica i permessi.
     */
    public ElencoLezioni(LezioneService lezioneService, PresenzaService presenzaService,
                         SerializableSupplier<List<Lezione>> fonte, SerializableSupplier<List<Corso>> corsi,
                         String username) {
        this(lezioneService, presenzaService, fonte, corsi, username, false);
    }

    public ElencoLezioni(LezioneService lezioneService, PresenzaService presenzaService,
                         SerializableSupplier<List<Lezione>> fonte, SerializableSupplier<List<Corso>> corsi,
                         String username, boolean tuttiICorsi) {
        this.tuttiICorsi = tuttiICorsi;
        this.lezioneService = lezioneService;
        this.presenzaService = presenzaService;
        this.fonte = fonte;
        this.corsi = corsi;
        this.username = username;

        setSizeFull();
        setPadding(false);
        configureGrid();

        // Di default solo le lezioni ancora da fare
        filtro.setValue(FiltroLezioni.DA_FARE);
        filtro.getStyle().set("max-width", "32rem");
        filtro.addValueChangeListener(e -> applicaFiltro());

        Button nuova = new Button("Nuova lezione", VaadinIcon.PLUS.create(), e -> apriNuovaLezione(null));
        HorizontalLayout barra = new HorizontalLayout(filtro);
        if (tuttiICorsi) {
            filtroCorso.setPlaceholder("Tutti i corsi");
            filtroCorso.setAriaLabel("Filtra per corso");
            filtroCorso.setClearButtonVisible(true);
            filtroCorso.setItemLabelGenerator(Corso::getNome);
            filtroCorso.setItems(corsi.get());
            filtroCorso.addValueChangeListener(e -> applicaFiltro());
            barra.add(filtroCorso);
        }
        barra.add(nuova);
        barra.setWrap(true);
        barra.setAlignItems(Alignment.CENTER);
        add(barra, gridLezioni);
        expand(gridLezioni);
        updateGrid();
    }

    private void configureGrid() {
        // Solo su mobile: corso e data in un'unica colonna su due righe, per non troncare nulla
        var lezione = gridLezioni.addComponentColumn(this::createRiepilogo)
            .setHeader("Lezione").setComparator(Lezione::getData).setSortable(true);
        var corso = gridLezioni.addColumn(l -> l.getCorso().getNome()).setHeader("Corso").setSortable(true);
        var insegnante = gridLezioni.addColumn(l -> l.getCorso().getInsegnante() != null
            ? l.getCorso().getInsegnante().getNomeCompleto() : "").setHeader("Insegnante").setSortable(true);
        // Data con il badge "Oggi", "Settimana scorsa", … (QuandoLezione)
        var data = gridLezioni.addComponentColumn(this::createData).setHeader("Data")
            .setComparator(Lezione::getData).setSortable(true).setWidth("17rem").setFlexGrow(0);
        var numero = gridLezioni.addColumn(ElencoLezioni::numero).setHeader("N. Lezione")
            .setComparator(Lezione::getNumeroLezione);
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
            insegnante.setVisible(tuttiICorsi && !m);
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
        Badge badge;
        if (lezione.isCompletata()) {
            badge = new Badge("Completata", VaadinIcon.CHECK.create());
            badge.addThemeVariants(BadgeVariant.SUCCESS);
        } else if (lezione.isNonEffettuata()) {
            badge = new Badge("Non effettuata", VaadinIcon.BAN.create());
            badge.addThemeVariants(BadgeVariant.CONTRAST);
            if (lezione.getMotivoNonEffettuata() != null) {
                Tooltip.forComponent(badge).setText(lezione.getMotivoNonEffettuata());
            }
        } else {
            badge = new Badge("Da fare", VaadinIcon.CLOCK.create());
        }
        badge.bindThemeVariant(BadgeVariant.ICON_ONLY, mobile);
        return badge;
    }

    private void openPresenzeDialog(Lezione lezione) {
        Dialog dialog = ViewSupport.dialog(
            lezione.getCorso().getNome() + " – Lezione " + lezione.getNumeroLezione() + " (" + Formati.data(lezione.getData()) + ")", "800px");

        dialog.getFooter().add(ViewSupport.closeButton(dialog, "Chiudi"));

        if (lezione.isNonEffettuata()) {
            // Le presenze di una lezione non effettuata non contano: niente registro, solo il ripristino
            Span avviso = new Span("Lezione non effettuata" + (lezione.getMotivoNonEffettuata() != null
                ? ": " + lezione.getMotivoNonEffettuata() : ""));
            avviso.addClassName("avviso-sola-lettura");
            dialog.add(avviso);
            Lezione recupero = elenco.stream().filter(l -> l.getRecuperoDi() != null
                && l.getRecuperoDi().getId().equals(lezione.getId())).findFirst().orElse(null);
            if (recupero != null) {
                dialog.add(new Span("Recuperata il " + Formati.data(recupero.getData())));
            } else {
                Button recupera = new Button("Recupera", VaadinIcon.PLUS.create(), e -> {
                    dialog.close();
                    apriNuovaLezione(lezione);
                });
                recupera.addThemeVariants(ButtonVariant.PRIMARY);
                Button ripristina = new Button("Ripristina", VaadinIcon.ROTATE_LEFT.create(),
                    e -> esegui(dialog, () -> lezioneService.ripristina(lezione.getId(), username), "Lezione ripristinata"));
                dialog.getFooter().add(ripristina, recupera);
            }
            dialog.open();
            return;
        }

        Grid<Presenza> gridPresenze = new Grid<>(Presenza.class, false);
        // Una sola colonna con nome, stato e note: su schermi stretti gli elementi vanno a capo
        gridPresenze.addComponentColumn(this::createPresenzaRiga).setHeader("Allievi");
        gridPresenze.setItems(presenzaService.findByLezione(lezione.getId()));
        dialog.add(gridPresenze);

        if (!lezione.isCompletata()) {
            Button nonEffettuata = new Button("Non effettuata", VaadinIcon.BAN.create(),
                e -> apriNonEffettuata(lezione, dialog));
            Button completaButton = new Button("Segna Completata",
                e -> esegui(dialog, () -> lezioneService.completa(lezione.getId(), username), "Lezione completata"));
            completaButton.addThemeVariants(ButtonVariant.SUCCESS);
            if (lezione.isAggiunta()) {
                // Le lezioni aggiunte a mano non si segnano non effettuate: si eliminano (insegnante o admin)
                if (lezioneService.puoEliminare(lezione.getCorso(), username)) {
                    Button elimina = new Button("Elimina", VaadinIcon.TRASH.create(), e -> confermaEliminazione(lezione, dialog));
                    elimina.addThemeVariants(ButtonVariant.ERROR);
                    dialog.getFooter().add(elimina);
                }
                dialog.getFooter().add(completaButton);
            } else {
                dialog.getFooter().add(nonEffettuata, completaButton);
            }
        }
        dialog.open();
    }

    /**
     * Nuova lezione: con {@code saltata} è il suo recupero (stesso corso e numero), altrimenti una lezione in più
     * di uno dei corsi. Data obbligatoria, orario proposto dal corso.
     */
    private void apriNuovaLezione(Lezione saltata) {
        Dialog dialog = ViewSupport.dialog(saltata != null
            ? "Recupero della lezione " + saltata.getNumeroLezione() + " (" + Formati.data(saltata.getData()) + ")"
            : "Nuova lezione", "480px");
        List<Corso> scelte = saltata != null ? List.of(saltata.getCorso()) : corsi.get();
        if (scelte.isEmpty()) {
            Notification.show("Nessun corso attivo a cui aggiungere lezioni");
            return;
        }
        ComboBox<Corso> corso = new ComboBox<>("Corso");
        corso.setItems(scelte);
        corso.setItemLabelGenerator(Corso::getNome);
        corso.setVisible(scelte.size() > 1);
        DatePicker data = ViewSupport.datePicker("Data");
        TimePicker orario = ViewSupport.timePicker("Orario");
        corso.addValueChangeListener(e -> orario.setValue(e.getValue() != null ? e.getValue().getOrarioInizio() : null));
        corso.setValue(scelte.getFirst());
        if (scelte.size() > 1) {
            corso.clear();
        }

        // Il corso non è nel Binder: con un solo corso il campo è nascosto e il Binder non lo scriverebbe
        corso.setRequiredIndicatorVisible(true);
        corso.setErrorMessage("Scegli il corso");
        Binder<NuovaLezione> binder = new Binder<>();
        binder.forField(data).asRequired("Inserisci la data della lezione").bind(NuovaLezione::getData, NuovaLezione::setData);
        binder.forField(orario).bind(NuovaLezione::getOrario, NuovaLezione::setOrario);

        Button salva = new Button("Salva", e -> {
            NuovaLezione nuova = new NuovaLezione();
            corso.setInvalid(corso.isEmpty());
            if (!binder.writeBeanIfValid(nuova) || corso.isEmpty()) {
                return;
            }
            Long corsoId = corso.getValue().getId();
            esegui(dialog, () -> lezioneService.aggiungi(corsoId, nuova.getData(), nuova.getOrario(),
                saltata != null ? saltata.getId() : null, username), saltata != null ? "Recupero inserito" : "Lezione inserita");
        });
        salva.addThemeVariants(ButtonVariant.PRIMARY);
        FormLayout form = new FormLayout(corso, data, orario);
        dialog.add(form);
        dialog.getFooter().add(ViewSupport.closeButton(dialog, "Annulla"), salva);
        dialog.open();
    }

    private void confermaEliminazione(Lezione lezione, Dialog dialogLezione) {
        Dialog conferma = ViewSupport.dialog("Eliminare la lezione?", "420px");
        conferma.add(new Span(lezione.getCorso().getNome() + " – lezione " + lezione.getNumeroLezione()
            + " del " + Formati.data(lezione.getData()) + ", con le sue presenze"));
        Button elimina = new Button("Elimina", e -> {
            conferma.close();
            esegui(dialogLezione, () -> lezioneService.eliminaAggiunta(lezione.getId(), username), "Lezione eliminata");
        });
        elimina.addThemeVariants(ButtonVariant.PRIMARY, ButtonVariant.ERROR);
        conferma.getFooter().add(ViewSupport.closeButton(conferma, "Annulla"), elimina);
        conferma.open();
    }

    /** Numero della lezione, con l'indicazione delle lezioni di recupero e di quelle aggiunte. */
    private static String numero(Lezione lezione) {
        String numero = String.valueOf(lezione.getNumeroLezione());
        if (lezione.getRecuperoDi() != null) {
            return numero + " · Recupero";
        }
        return lezione.isAggiunta() ? numero + " · Aggiunta" : numero;
    }

    /** Valori del dialog "Nuova lezione". */
    private static class NuovaLezione {
        private LocalDate data;
        private LocalTime orario;

        LocalDate getData() { return data; }
        void setData(LocalDate data) { this.data = data; }
        LocalTime getOrario() { return orario; }
        void setOrario(LocalTime orario) { this.orario = orario; }
    }

    /** Conferma con motivo facoltativo prima di segnare la lezione come non effettuata. */
    private void apriNonEffettuata(Lezione lezione, Dialog dialogLezione) {
        Dialog dialog = ViewSupport.dialog("Lezione non effettuata", "420px");
        TextField motivo = new TextField("Motivo");
        motivo.setPlaceholder("es. insegnante assente");
        motivo.setWidthFull();
        Button conferma = new Button("Segna non effettuata", e -> {
            dialog.close();
            esegui(dialogLezione, () -> lezioneService.segnaNonEffettuata(lezione.getId(), motivo.getValue(), username),
                "Lezione segnata come non effettuata");
        });
        conferma.addThemeVariants(ButtonVariant.PRIMARY);
        dialog.add(new Span("Le presenze di questa lezione non verranno conteggiate."), motivo);
        dialog.getFooter().add(ViewSupport.closeButton(dialog, "Annulla"), conferma);
        dialog.open();
    }

    /** Esegue l'operazione, chiude il dialog e aggiorna la lista; in caso di errore mostra il messaggio. */
    private void esegui(Dialog dialog, Runnable operazione, String conferma) {
        try {
            operazione.run();
        } catch (RuntimeException ex) {
            Notification.show(ex.getMessage());
            return;
        }
        dialog.close();
        updateGrid();
        Notification.show(conferma);
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
        elenco = fonte.get();
        lezioni = gridLezioni.setItems(elenco);
        applicaFiltro();
    }

    private void applicaFiltro() {
        if (lezioni != null) {
            Corso corso = filtroCorso.getValue();
            SerializablePredicate<Lezione> stato = filtro.getValue().condizione;
            lezioni.setFilter(l -> stato.test(l) && (corso == null || corso.getId().equals(l.getCorso().getId())));
        }
    }
}
