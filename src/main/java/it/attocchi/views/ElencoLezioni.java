package it.attocchi.views;

import com.vaadin.flow.component.Text;
import it.attocchi.entity.Luogo;
import it.attocchi.service.LuogoService;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.tabs.Tab;
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
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.server.VaadinService;
import it.attocchi.entity.Corso;
import it.attocchi.entity.Insegnante;
import it.attocchi.entity.Lezione;
import it.attocchi.entity.Presenza;
import java.util.TreeMap;
import java.util.LinkedHashMap;
import java.util.Map;
import it.attocchi.entity.GruppoCorso;
import com.vaadin.flow.function.SerializableFunction;
import it.attocchi.service.CalendarioFestivita;
import it.attocchi.service.FestivitaService;
import it.attocchi.service.LezioneService;
import it.attocchi.service.PresenzaService;
import it.attocchi.views.insegnante.QuandoLezione;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
    private Map<Long, List<GruppoCorso>> gruppiPerCorso = Map.of();
    /** Festività attive, rilette con l'elenco: segnalano le lezioni da fare che cadono in un giorno festivo. */
    private CalendarioFestivita festivita;

    private final Grid<Lezione> gridLezioni = new Grid<>(Lezione.class, false);
    private final SegmentedControl<FiltroLezioni> filtro = new SegmentedControl<>("Filtra le lezioni per stato",
        List.of(FiltroLezioni.values()), FiltroLezioni::getEtichetta, f -> "Mostra lezioni: " + f.getEtichetta(), null);
    private GridListDataView<Lezione> lezioni;

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

        addClassNames("size-full p-0");
        configureGrid();

        // Di default solo le lezioni ancora da fare
        filtro.setValue(FiltroLezioni.DA_FARE);
        filtro.addClassNames("max-w-lg");
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
        barra.addClassNames("flex-wrap items-center");
        add(barra, gridLezioni);
        gridLezioni.addClassNames("grow");
        updateGrid();
    }

    private void configureGrid() {
        gridLezioni.addComponentColumn(l -> TipoCorsoUI.nome(l.getCorso())).setHeader("Corso")
            .setComparator(l -> l.getCorso().getNome());
        var insegnante = gridLezioni.addColumn(ElencoLezioni::insegnante).setHeader("Insegnanti").setSortable(true);
        insegnante.setVisible(tuttiICorsi);
        // Data con il badge "Oggi", "Settimana scorsa", … (QuandoLezione)
        gridLezioni.addComponentColumn(this::createData).setHeader("Data")
            .setComparator(Lezione::getData).setSortable(true).setWidth("17rem").setFlexGrow(0);
        gridLezioni.addColumn(this::numero).setHeader("N. Lezione")
            .setComparator(Lezione::getNumeroLezione);
        gridLezioni.addColumn(this::orario).setHeader("Orario");
        gridLezioni.addComponentColumn(ElencoLezioni::createLuogo).setHeader("Luogo")
            .setComparator(l -> l.getLuogoEffettivo() != null ? l.getLuogoEffettivo().getNome() : "");
        // Stato come badge, non ordinabile (il filtro separa già gli stati). Larghezza fissa: quella automatica
        // verrebbe calcolata sulle sole righe visibili all'apertura ("Da fare") e troncherebbe "Completata".
        gridLezioni.addComponentColumn(this::createStatoBadge).setHeader("Stato").setWidth("8.5rem").setFlexGrow(0);
        var azioni = gridLezioni.addComponentColumn(this::createAzioni);
        ViewSupport.configureActionsColumn(azioni);
        azioni.setAutoWidth(false).setWidth("5.5rem");

        CardMobile.per(gridLezioni)
            .titolo(l -> l.getCorso().getNome())
            .stato(this::createStatoBadge)
            .campo("Tipo", l -> TipoCorsoUI.badge(l.getCorso()))
            .campo("Data", this::createData)
            .campo("Insegnanti", l -> tuttiICorsi ? insegnante(l) : null)
            .campo("N. Lezione", this::numero)
            .campo("Orario", this::orario)
            .campo("Luogo", l -> l.getLuogoEffettivo() != null ? createLuogo(l) : null)
            .azioni(this::createAzioni)
            .applica();
    }

    /**
     * Orario della lezione; nei corsi con gruppi, se la lezione è all'orario del corso, quelli dei gruppi del suo
     * giorno ("10:00 · 11:00").
     */
    /** Luogo della lezione, con il badge "Cambiato" se non è quello del corso. */
    private static Span createLuogo(Lezione lezione) {
        Span luogo = new Span(lezione.getLuogoEffettivo() != null ? lezione.getLuogoEffettivo().getNome() : "");
        if (lezione.isLuogoDiverso()) {
            Badge badge = new Badge("Cambiato", VaadinIcon.MAP_MARKER.create());
            badge.addThemeVariants(BadgeVariant.WARNING);
            badge.getElement().setAttribute("title", Formati.luogo(lezione));
            luogo.add(new Text(" "), badge);
        }
        return luogo;
    }

    private String orario(Lezione lezione) {
        if (Objects.equals(lezione.getOrarioInizio(), lezione.getCorso().getOrarioInizio())) {
            DayOfWeek giorno = (lezione.getRecuperoDi() != null ? lezione.getRecuperoDi() : lezione).getDataDelCalendario().getDayOfWeek();
            String orari = gruppiPerCorso.getOrDefault(lezione.getCorso().getId(), List.of()).stream()
                .filter(g -> g.getGiorno() == giorno && g.getOrario() != null)
                .map(GruppoCorso::getOrario).distinct().sorted().map(Formati::ora)
                .collect(Collectors.joining(" · "));
            if (!orari.isEmpty()) {
                return orari;
            }
        }
        return Formati.ora(lezione.getOrarioInizio());
    }

    private HorizontalLayout createAzioni(Lezione lezione) {
        return ViewSupport.actions(ViewSupport.iconButton(VaadinIcon.CLIPBOARD_CHECK, "Gestisci Presenze",
            e -> openPresenzeDialog(lezione)));
    }

    private static String insegnante(Lezione lezione) {
        return lezione.getCorso().getNomiInsegnanti();
    }

    private Div createData(Lezione lezione) {
        Div data = new Div(new Span(Formati.data(lezione.getData())));
        data.addClassNames("flex flex-wrap items-center gap-x-2 gap-y-1");
        QuandoLezione.di(lezione, LocalDate.now()).ifPresent(quando -> {
            Badge badge = new Badge(quando.getEtichetta());
            badge.addThemeVariants(BadgeVariant.SMALL);
            if (quando.getVariante() != null) {
                badge.addThemeVariants(quando.getVariante());
            }
            data.add(badge);
        });
        if (lezione.isSpostata()) {
            Badge badge = new Badge("Spostata", VaadinIcon.CALENDAR_CLOCK.create());
            badge.addThemeVariants(BadgeVariant.SMALL);
            Tooltip.forComponent(badge).setText("Spostata dal " + Formati.data(lezione.getDataOriginale()));
            data.add(badge);
        }
        if (lezione.isDaFare()) {
            festivita.festivita(lezione.getData()).ifPresent(nome -> {
                Badge badge = new Badge(nome, VaadinIcon.FLAG_O.create());
                badge.addThemeVariants(BadgeVariant.SMALL, BadgeVariant.WARNING);
                Tooltip.forComponent(badge).setText(lezione.isSpostata() ? "Festività"
                    : "Festività: con \"Genera Lezioni\" la lezione viene spostata");
                data.add(badge);
            });
        }
        return data;
    }

    private static FestivitaService festivitaService() {
        return VaadinService.getCurrent().getInstantiator().getOrCreate(FestivitaService.class);
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
        return badge;
    }

    /** Parametro {@value #PARAMETRO_LEZIONE} dell'indirizzo che apre le presenze di una lezione. */
    public static final String PARAMETRO_LEZIONE = "lezione";

    /**
     * Parametro {@value #PARAMETRO_GRUPPO} che, con {@link #PARAMETRO_LEZIONE}, apre il registro sulla scheda di un
     * gruppo: il suo id, o {@value #SENZA_GRUPPO} per chi non ne ha.
     */
    public static final String PARAMETRO_GRUPPO = "gruppo";

    public static final String SENZA_GRUPPO = "senza";

    /** Id della lezione dal parametro dell'indirizzo; vuoto se non è un numero. */
    public static Optional<Long> idLezione(String valore) {
        try {
            return Optional.of(Long.valueOf(valore));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    /**
     * Apre le presenze della lezione indicata, se è nell'elenco (es. da "Gestisci presenze" del calendario, con il
     * parametro {@code ?lezione=ID}); altrimenti un avviso.
     */
    public void apriPresenze(Long lezioneId) {
        apriPresenze(lezioneId, null);
    }

    /** Come {@link #apriPresenze(Long)}, sulla scheda del gruppo indicato ({@link #PARAMETRO_GRUPPO}; {@code null} = la prima). */
    public void apriPresenze(Long lezioneId, String scheda) {
        elenco.stream().filter(l -> l.getId().equals(lezioneId)).findFirst().ifPresentOrElse(l -> openPresenzeDialog(l, scheda),
            () -> Notification.show("Lezione non trovata tra le tue lezioni"));
    }

    /** Apre le presenze indicate dai parametri dell'indirizzo ({@code ?lezione=ID&gruppo=…}), se ci sono. */
    public void apriPresenze(QueryParameters parametri) {
        parametri.getSingleParameter(PARAMETRO_LEZIONE).flatMap(ElencoLezioni::idLezione)
            .ifPresent(id -> apriPresenze(id, parametri.getSingleParameter(PARAMETRO_GRUPPO).orElse(null)));
    }

    private void openPresenzeDialog(Lezione lezione) {
        openPresenzeDialog(lezione, null);
    }

    private void openPresenzeDialog(Lezione lezione, String scheda) {
        Dialog dialog = ViewSupport.dialog(
            lezione.getCorso().getNome() + " – Lezione " + Formati.numeroLezione(lezione, gruppiPerCorso.get(lezione.getCorso().getId())) + " (" + Formati.data(lezione.getData()) + ")", "800px");

        dialog.getFooter().add(ViewSupport.closeButton(dialog, "Chiudi"));

        if (lezione.isNonEffettuata()) {
            // Le presenze di una lezione non effettuata non contano: niente registro, solo il ripristino
            Span avviso = new Span("Lezione non effettuata" + (lezione.getMotivoNonEffettuata() != null
                ? ": " + lezione.getMotivoNonEffettuata() : ""));
            avviso.addClassNames(Stili.TESTO_SECONDARIO);
            dialog.add(avviso, createNoteLezione(lezione));
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

        List<Insegnante> insegnanti = lezione.getCorso().getInsegnanti();
        if (insegnanti.size() == 2) {
            dialog.add(ViewSupport.sezione("Insegnanti presenti"), createInsegnantiPresenti(lezione, insegnanti));
        }
        dialog.add(ViewSupport.sezione("Allievi"));

        Grid<Presenza> gridPresenze = new Grid<>(Presenza.class, false);
        gridPresenze.addColumn(p -> p.getAllievo().getNomeCompleto()).setHeader("Allievo");
        // Il gruppo dell'allievo, solo nei corsi che hanno gruppi
        Map<Long, GruppoCorso> gruppi = lezioneService.gruppiDeiPartecipanti(lezione.getCorso().getId());
        SerializableFunction<Presenza, String> gruppo = p -> Formati.gruppo(gruppi.get(p.getAllievo().getId()));
        gridPresenze.addColumn(gruppo::apply).setHeader("Gruppo").setAutoWidth(true).setFlexGrow(0)
            .setVisible(!gruppi.isEmpty());
        gridPresenze.addComponentColumn(this::createStatoPresenza).setHeader("Stato").setWidth("20rem").setFlexGrow(0);
        gridPresenze.addComponentColumn(this::createNotePresenza).setHeader("Note");
        CardMobile.per(gridPresenze)
            .titolo(p -> p.getAllievo().getNomeCompleto())
            .sottotitolo(p -> gruppi.containsKey(p.getAllievo().getId()) ? "Gruppo " + gruppo.apply(p) : null)
            .campo("Stato", this::createStatoPresenza)
            .campo("Note", this::createNotePresenza)
            .applica();
        List<Presenza> presenze = lezioneService.presenzeInOrdine(lezione.getId());
        GridListDataView<Presenza> righe = gridPresenze.setItems(presenze);
        schedePerGruppo(presenze, gruppi, righe, scheda).ifPresent(dialog::add);
        dialog.add(gridPresenze);
        // Luogo e note in fondo, dopo le presenze e prima dei pulsanti
        dialog.add(ViewSupport.sezione("Luogo e note della lezione"), createLuogoLezione(lezione), createNoteLezione(lezione));

        if (!lezione.isCompletata()) {
            Button nonEffettuata = new Button("Non effettuata", VaadinIcon.BAN.create(),
                e -> apriNonEffettuata(lezione, dialog));
            Button completaButton = new Button("Segna Completata",
                e -> esegui(dialog, () -> lezioneService.completa(lezione.getId(), username), "Lezione completata"));
            completaButton.addThemeVariants(ButtonVariant.SUCCESS);
            // Il cambio data solo ad admin e segreteria
            if (lezioneService.puoSpostare(username)) {
                dialog.getFooter().add(new Button("Cambia data", VaadinIcon.CALENDAR.create(),
                    e -> apriCambiaData(lezione, dialog)));
            }
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

        // In un giorno festivo la lezione si può inserire comunque, con un avviso
        Span avvisoFestivita = new Span();
        avvisoFestivita.addClassNames(Stili.TESTO_SECONDARIO);
        avvisoFestivita.setVisible(false);
        CalendarioFestivita calendario = festivitaService().calendario();
        data.addValueChangeListener(e -> {
            var nome = e.getValue() != null ? calendario.festivita(e.getValue()) : Optional.<String>empty();
            avvisoFestivita.setText(nome.map(n -> "È una festività (" + n + "): la lezione si può comunque inserire.").orElse(""));
            avvisoFestivita.setVisible(nome.isPresent());
        });

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
        FormLayout form = new FormLayout(corso, data, orario, avvisoFestivita);
        dialog.add(form);
        dialog.getFooter().add(ViewSupport.closeButton(dialog, "Annulla"), salva);
        dialog.open();
    }

    /**
     * Sposta la lezione da fare a un'altra data e orario: stessi partecipanti e numero; in un giorno festivo un
     * avviso, se il corso ha già una lezione quel giorno il service la rifiuta.
     */
    private void apriCambiaData(Lezione lezione, Dialog dialogLezione) {
        Dialog dialog = ViewSupport.dialog("Cambia data della lezione " + numero(lezione), "480px");
        DatePicker data = ViewSupport.datePicker("Data");
        TimePicker orario = ViewSupport.timePicker("Orario");
        Span avvisoFestivita = new Span();
        avvisoFestivita.addClassNames(Stili.TESTO_SECONDARIO);
        avvisoFestivita.setVisible(false);
        CalendarioFestivita calendario = festivitaService().calendario();
        data.addValueChangeListener(e -> {
            var nome = e.getValue() != null ? calendario.festivita(e.getValue()) : Optional.<String>empty();
            avvisoFestivita.setText(nome.map(n -> "È una festività (" + n + "): la lezione si può comunque spostare.").orElse(""));
            avvisoFestivita.setVisible(nome.isPresent());
        });
        Binder<NuovaLezione> binder = new Binder<>();
        binder.forField(data).asRequired("Inserisci la data della lezione").bind(NuovaLezione::getData, NuovaLezione::setData);
        binder.forField(orario).bind(NuovaLezione::getOrario, NuovaLezione::setOrario);
        NuovaLezione valori = new NuovaLezione();
        valori.setData(lezione.getData());
        valori.setOrario(lezione.getOrarioInizio());
        binder.readBean(valori);

        Span info = new Span(lezione.isSpostata()
            ? "Data del calendario del corso: " + Formati.data(lezione.getDataOriginale()) + "."
            : "Partecipanti e numero della lezione restano gli stessi.");
        info.addClassNames(Stili.TESTO_SECONDARIO);
        Button salva = new Button("Salva", e -> {
            if (!binder.writeBeanIfValid(valori)) {
                return;
            }
            try {
                lezioneService.sposta(lezione.getId(), valori.getData(), valori.getOrario(), username);
            } catch (RuntimeException ex) {
                Notification.show(ex.getMessage());
                return;
            }
            dialog.close();
            dialogLezione.close();
            updateGrid();
            Notification.show("Lezione spostata a " + Formati.data(valori.getData()));
        });
        salva.addThemeVariants(ButtonVariant.PRIMARY);
        dialog.add(new FormLayout(data, orario, avvisoFestivita), info);
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
    private String numero(Lezione lezione) {
        String numero = Formati.numeroLezione(lezione, gruppiPerCorso.get(lezione.getCorso().getId()));
        if (lezione.getRecuperoDi() != null) {
            return numero + " · Recupero";
        }
        return lezione.isAggiunta() ? numero + " · Aggiunta" : numero;
    }

    /** Valori dei dialog "Nuova lezione" e "Cambia data". */
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
        motivo.addClassNames("w-full");
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

    /**
     * Schede che filtrano il registro per gruppo: una per ogni gruppo con allievi alla lezione (in ordine di giorno e
     * orario, con il numero di allievi), "Senza gruppo" se c'è chi non ne ha e "Tutti"; si apre sulla scheda
     * {@code scheda} ({@link #PARAMETRO_GRUPPO}) se c'è, altrimenti sul primo gruppo.
     * Vuoto se la lezione non ha almeno due schede di gruppo (corso senza gruppi o un solo gruppo per tutti).
     */
    private static Optional<Tabs> schedePerGruppo(List<Presenza> presenze, Map<Long, GruppoCorso> gruppi,
                                                  GridListDataView<Presenza> righe, String scheda) {
        Map<GruppoCorso, Long> perGruppo = new TreeMap<>(GruppoCorso.PER_GIORNO.thenComparing(GruppoCorso::getId));
        long senzaGruppo = 0;
        for (Presenza presenza : presenze) {
            GruppoCorso gruppo = gruppi.get(presenza.getAllievo().getId());
            if (gruppo != null) {
                perGruppo.merge(gruppo, 1L, Long::sum);
            } else {
                senzaGruppo++;
            }
        }
        if (perGruppo.size() + (senzaGruppo > 0 ? 1 : 0) < 2) {
            return Optional.empty();
        }
        Tabs schede = new Tabs();
        schede.getElement().setAttribute("aria-label", "Gruppi della lezione");
        Map<Tab, SerializablePredicate<Presenza>> filtri = new LinkedHashMap<>();
        Tab[] iniziale = new Tab[1];
        perGruppo.forEach((gruppo, numero) -> {
            String nome = gruppo.getNome() != null && !gruppo.getNome().isBlank() ? gruppo.getNome() : Formati.giorno(gruppo.getGiorno());
            Tab tab = new Tab(nome + (gruppo.getOrario() != null ? " " + Formati.ora(gruppo.getOrario()) : "") + " (" + numero + ")");
            filtri.put(tab, p -> gruppo.equals(gruppi.get(p.getAllievo().getId())));
            if (String.valueOf(gruppo.getId()).equals(scheda)) {
                iniziale[0] = tab;
            }
        });
        if (senzaGruppo > 0) {
            Tab tab = new Tab("Senza gruppo (" + senzaGruppo + ")");
            filtri.put(tab, p -> !gruppi.containsKey(p.getAllievo().getId()));
            if (SENZA_GRUPPO.equals(scheda)) {
                iniziale[0] = tab;
            }
        }
        filtri.put(new Tab("Tutti (" + presenze.size() + ")"), p -> true);
        filtri.keySet().forEach(schede::add);
        schede.addSelectedChangeListener(e -> righe.setFilter(filtri.get(e.getSelectedTab())));
        Tab selezionata = iniziale[0] != null ? iniziale[0] : filtri.keySet().iterator().next();
        schede.setSelectedTab(selezionata);
        righe.setFilter(filtri.get(selezionata));
        return Optional.of(schede);
    }

    /**
     * Note della lezione per lo staff e per allievi e genitori, salvate subito all'uscita dal campo; in caso di
     * errore un avviso e il campo torna al valore precedente.
     */
    private FormLayout createNoteLezione(Lezione lezione) {
        TextArea staff = areaNote("Note per lo staff", "Visibili solo ad admin, segreteria e insegnanti del corso",
            lezione.getNote());
        TextArea pubbliche = areaNote("Note per allievi e genitori", "Visibili ad allievi e genitori nel calendario",
            lezione.getNotePubbliche());
        for (TextArea area : List.of(staff, pubbliche)) {
            area.addValueChangeListener(e -> {
                if (!e.isFromClient()) {
                    return;
                }
                try {
                    lezioneService.impostaNote(lezione.getId(), staff.getValue(), pubbliche.getValue(), username);
                    // L'elenco non si rilegge: la lezione in memoria tiene le note per la prossima apertura
                    lezione.setNote(staff.getValue().isBlank() ? null : staff.getValue().strip());
                    lezione.setNotePubbliche(pubbliche.getValue().isBlank() ? null : pubbliche.getValue().strip());
                } catch (RuntimeException ex) {
                    Notification.show("Errore nel salvataggio delle note: " + ex.getMessage());
                    area.setValue(e.getOldValue());
                }
            });
        }
        FormLayout form = new FormLayout(staff, pubbliche);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("500px", 2));
        return form;
    }

    /**
     * Luogo della lezione, salvato subito: proposti prima i luoghi abilitati del corso, poi gli altri attivi; vuoto =
     * il luogo del corso. In caso di errore un avviso e il campo torna al valore precedente.
     */
    private ComboBox<Luogo> createLuogoLezione(Lezione lezione) {
        ComboBox<Luogo> campo = new ComboBox<>("Luogo");
        // Come InvioEmailDialog: il service senza passarlo ai costruttori delle due viste
        LuogoService luogoService = VaadinService.getCurrent().getInstantiator().getOrCreate(LuogoService.class);
        campo.setItems(luogoService.sceltePerLezione(lezione));
        campo.setItemLabelGenerator(Luogo::getNome);
        campo.setClearButtonVisible(true);
        Luogo delCorso = lezione.getCorso().getLuogo();
        campo.setPlaceholder(delCorso != null ? delCorso.getNome() : "Nessun luogo");
        campo.setHelperText(delCorso != null ? "Vuoto = luogo del corso (" + delCorso.getNome() + ")"
            : "Il corso non ha un luogo predefinito");
        campo.setValue(lezione.getLuogo());
        campo.addClassNames("w-full sm:w-80");
        campo.addValueChangeListener(e -> {
            if (!e.isFromClient()) {
                return;
            }
            try {
                lezioneService.impostaLuogo(lezione.getId(), e.getValue() != null ? e.getValue().getId() : null, username);
                // Come per le note: la lezione in memoria tiene il luogo e l'elenco ne mostra il nuovo valore
                lezione.setLuogo(e.getValue() == null || e.getValue().equals(delCorso) ? null : e.getValue());
                gridLezioni.getDataProvider().refreshItem(lezione);
            } catch (RuntimeException ex) {
                Notification.show("Errore nel salvataggio del luogo: " + ex.getMessage());
                campo.setValue(e.getOldValue());
            }
        });
        return campo;
    }

    private static TextArea areaNote(String etichetta, String aiuto, String valore) {
        TextArea area = new TextArea(etichetta);
        area.setHelperText(aiuto);
        area.setMaxLength(Lezione.LUNGHEZZA_NOTE);
        area.setValue(valore != null ? valore : "");
        return area;
    }

    /** Scelta degli insegnanti presenti a una lezione di un corso con due insegnanti. */
    private record SceltaInsegnanti(String etichetta, String descrizione, Set<Long> ids) {
    }

    /** "Entrambi / Solo X / Solo Y", salvato subito; in caso di errore torna al valore precedente. */
    private SegmentedControl<SceltaInsegnanti> createInsegnantiPresenti(Lezione lezione, List<Insegnante> insegnanti) {
        Insegnante primo = insegnanti.get(0);
        Insegnante secondo = insegnanti.get(1);
        List<SceltaInsegnanti> scelte = List.of(
            new SceltaInsegnanti("Entrambi", "Presenti entrambi gli insegnanti", Set.of(primo.getId(), secondo.getId())),
            new SceltaInsegnanti("Solo " + breve(primo), "Presente solo " + primo.getNomeCompleto(), Set.of(primo.getId())),
            new SceltaInsegnanti("Solo " + breve(secondo), "Presente solo " + secondo.getNomeCompleto(), Set.of(secondo.getId())));
        SegmentedControl<SceltaInsegnanti> controllo = new SegmentedControl<>("Insegnanti presenti", scelte,
            SceltaInsegnanti::etichetta, SceltaInsegnanti::descrizione, null);
        Set<Long> presenti = lezione.getInsegnantiPresenti().stream().map(Insegnante::getId).collect(Collectors.toSet());
        scelte.stream().filter(scelta -> scelta.ids().equals(presenti)).findFirst().ifPresent(controllo::setValue);
        controllo.addValueChangeListener(e -> {
            if (!e.isFromClient() || e.getValue() == null) {
                return;
            }
            try {
                lezioneService.impostaInsegnantiPresenti(lezione.getId(), e.getValue().ids(), username);
                updateGrid();
            } catch (RuntimeException ex) {
                Notification.show("Errore nel salvataggio degli insegnanti presenti: " + ex.getMessage());
                controllo.setValue(e.getOldValue());
            }
        });
        return controllo;
    }

    /** Cognome dell'insegnante per le etichette brevi (il nome completo se manca). */
    private static String breve(Insegnante insegnante) {
        return insegnante.getUser() != null && insegnante.getUser().getCognome() != null
            ? insegnante.getUser().getCognome() : insegnante.getNomeCompleto();
    }

    private StatoPresenzaToggle createStatoPresenza(Presenza presenza) {
        StatoPresenzaToggle stato = new StatoPresenzaToggle();
        stato.setValue(presenza.getStato());
        stato.addValueChangeListener(e -> salva(presenza, p -> p.setStato(e.getValue())));
        return stato;
    }

    private TextField createNotePresenza(Presenza presenza) {
        TextField note = new TextField();
        note.addClassNames("w-full");
        note.setAriaLabel("Note per " + presenza.getAllievo().getNomeCompleto());
        note.setPlaceholder("Note");
        note.setValue(presenza.getNote() != null ? presenza.getNote() : "");
        note.addValueChangeListener(e -> salva(presenza, p -> p.setNote(e.getValue())));
        return note;
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
        festivita = festivitaService().calendario();
        elenco = fonte.get();
        gruppiPerCorso = lezioneService.gruppiPerCorso();
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
