package it.attocchi.views.admin;

import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import it.attocchi.entity.AnnoScolastico;
import it.attocchi.entity.Corso;
import it.attocchi.entity.Insegnante;
import it.attocchi.security.SecurityService;
import it.attocchi.service.CorsoService;
import it.attocchi.service.InsegnanteService;
import it.attocchi.service.PagamentoService;
import it.attocchi.service.AllievoService;
import it.attocchi.views.Formati;
import it.attocchi.views.MainLayout;
import it.attocchi.views.SegmentedControl;
import it.attocchi.views.ViewSupport;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Route(value = "corsi", layout = MainLayout.class)
@PageTitle("Gestione Corsi")
@Menu(title = "Gestione Corsi", order = 10, icon = "vaadin:music")
@RolesAllowed({"ADMIN", "SEGRETERIA"})
public class CorsiView extends VerticalLayout {

    /** Filtro della griglia per stato del corso. */
    public enum FiltroStato {
        ATTIVI("Attivi", Corso::isAttivo),
        NON_ATTIVI("Non attivi", corso -> !corso.isAttivo()),
        TUTTI("Tutti", corso -> true);

        private final String etichetta;
        private final SerializablePredicate<Corso> condizione;

        FiltroStato(String etichetta, SerializablePredicate<Corso> condizione) {
            this.etichetta = etichetta;
            this.condizione = condizione;
        }

        public String getEtichetta() {
            return etichetta;
        }

        public boolean ammette(Corso corso) {
            return condizione.test(corso);
        }
    }

    private final CorsoService corsoService;
    private final InsegnanteService insegnanteService;
    private final AllievoService allievoService;
    private final PagamentoService pagamentoService;
    /** L'eliminazione dei corsi è riservata all'admin; la segreteria li gestisce senza eliminarli. */
    private final boolean puoEliminare;

    private final Grid<Corso> grid = new Grid<>(Corso.class, false);
    private final Signal<Boolean> mobile = ViewSupport.isMobile();
    private final SegmentedControl<FiltroStato> filtroStato = new SegmentedControl<>("Filtra i corsi per stato",
        List.of(FiltroStato.values()), FiltroStato::getEtichetta, f -> "Mostra corsi: " + f.getEtichetta(), null);
    private final ComboBox<String> filtroAnno = new ComboBox<>();
    private GridListDataView<Corso> corsi;

    public CorsiView(CorsoService corsoService, InsegnanteService insegnanteService, AllievoService allievoService,
                     PagamentoService pagamentoService, SecurityService securityService) {
        this.pagamentoService = pagamentoService;
        this.corsoService = corsoService;
        this.insegnanteService = insegnanteService;
        this.allievoService = allievoService;
        this.puoEliminare = securityService.hasRole("ADMIN");

        setSizeFull();
        configureGrid();

        Button addButton = new Button("Nuovo Corso", e -> openDialog(new Corso()));

        // Di default solo i corsi attivi, di tutti gli anni
        filtroStato.setValue(FiltroStato.ATTIVI);
        filtroStato.getStyle().set("max-width", "22rem");
        filtroStato.addValueChangeListener(e -> applicaFiltri());
        filtroAnno.setPlaceholder("Tutti gli anni");
        filtroAnno.setAriaLabel("Filtra per anno scolastico");
        filtroAnno.setClearButtonVisible(true);
        filtroAnno.setWidth("11rem");
        filtroAnno.addValueChangeListener(e -> applicaFiltri());

        HorizontalLayout toolbar = new HorizontalLayout(addButton, filtroStato, filtroAnno);
        toolbar.setWrap(true);
        toolbar.setWidthFull();
        toolbar.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        add(toolbar, grid);
        expand(grid);

        updateGrid();
    }

    private void configureGrid() {
        grid.addColumn(Corso::getNome).setHeader("Nome").setSortable(true);
        var anno = grid.addColumn(Corso::getAnnoScolastico).setHeader("Anno").setSortable(true).setWidth("7rem").setFlexGrow(0);
        var insegnante = grid.addColumn(corso -> corso.getInsegnante() != null ? corso.getInsegnante().getNomeCompleto() : "").setHeader("Insegnante");
        var dataInizio = grid.addColumn(corso -> Formati.data(corso.getDataInizio())).setHeader("Data Inizio")
            .setComparator(Corso::getDataInizio).setAutoWidth(true).setFlexGrow(0);
        var numeroLezioni = grid.addColumn(Corso::getNumeroLezioni).setHeader("N. Lezioni");
        var durata = grid.addColumn(Corso::getDurataLezioneMinuti).setHeader("Durata (min)");
        var numeroAllievi = grid.addColumn(corso -> corso.getAllievi().size()).setHeader("N. Allievi");
        var stato = grid.addComponentColumn(this::createStatoBadge).setHeader("Stato").setWidth("8.5rem").setFlexGrow(0);
        Signal.effect(grid, () -> stato.setHeader(mobile.get() ? "" : "Stato").setWidth(mobile.get() ? "3rem" : "8.5rem"));

        ViewSupport.configureActionsColumn(grid.addComponentColumn(corso -> {
            Button editButton = ViewSupport.iconButton(VaadinIcon.PENCIL, "Modifica", e -> openDialog(corso));
            Button partecipantiButton = ViewSupport.iconButton(VaadinIcon.USERS, "Partecipanti",
                e -> openPartecipanti(corso));
            Button generateButton = ViewSupport.iconButton(VaadinIcon.REFRESH, "Genera Lezioni", e -> generaLezioni(corso));
            generateButton.setEnabled(corso.isAttivo());
            HorizontalLayout actions = ViewSupport.actions(editButton, partecipantiButton, generateButton);
            if (puoEliminare) {
                Button deleteButton = ViewSupport.iconButton(VaadinIcon.TRASH, "Elimina", e -> deleteCorso(corso));
                deleteButton.addThemeVariants(ButtonVariant.ERROR);
                actions.add(deleteButton);
            }
            return actions;
        }));

        ViewSupport.hideOnMobile(grid, anno, insegnante, dataInizio, numeroLezioni, durata, numeroAllievi);
    }

    private Badge createStatoBadge(Corso corso) {
        Badge badge = corso.isAttivo()
            ? new Badge("Attivo", VaadinIcon.CHECK.create())
            : new Badge("Non attivo", VaadinIcon.BAN.create());
        if (corso.isAttivo()) {
            badge.addThemeVariants(BadgeVariant.SUCCESS);
        }
        badge.bindThemeVariant(BadgeVariant.ICON_ONLY, mobile);
        return badge;
    }

    private void openDialog(Corso corso) {
        Dialog dialog = ViewSupport.dialog(corso.getId() == null ? "Nuovo Corso" : "Modifica Corso", "600px");

        FormLayout formLayout = new FormLayout();
        Binder<Corso> binder = new Binder<>(Corso.class);

        TextField nome = new TextField("Nome");
        TextArea descrizione = new TextArea("Descrizione");
        DatePicker dataInizio = ViewSupport.datePicker("Data Inizio");
        IntegerField numeroLezioni = new IntegerField("Numero Lezioni");
        IntegerField durataLezioni = new IntegerField("Durata Lezione (minuti)");
        TimePicker orarioInizio = ViewSupport.timePicker("Orario Inizio");
        BigDecimalField costoTotale = new BigDecimalField("Costo Totale");
        costoTotale.setLocale(Locale.ITALY);
        costoTotale.setPrefixComponent(new Span("€"));

        CheckboxGroup<DayOfWeek> giorniSettimana = new CheckboxGroup<>();
        giorniSettimana.setLabel("Giorni Settimana");
        giorniSettimana.setItems(Arrays.asList(DayOfWeek.values()));
        giorniSettimana.setItemLabelGenerator(day -> {
            switch (day) {
                case MONDAY: return "Lunedì";
                case TUESDAY: return "Martedì";
                case WEDNESDAY: return "Mercoledì";
                case THURSDAY: return "Giovedì";
                case FRIDAY: return "Venerdì";
                case SATURDAY: return "Sabato";
                case SUNDAY: return "Domenica";
                default: return day.name();
            }
        });

        ComboBox<String> annoScolastico = new ComboBox<>("Anno Scolastico");
        annoScolastico.setItems(AnnoScolastico.elenco(LocalDate.now(), corso.getAnnoScolastico()));
        // Proposto dalla data di inizio finché non viene scelto
        dataInizio.addValueChangeListener(e -> {
            if (e.getValue() != null && annoScolastico.isEmpty()) {
                annoScolastico.setValue(AnnoScolastico.di(e.getValue()));
            }
        });
        Checkbox attivo = new Checkbox("Attivo");
        attivo.setTooltipText("I corsi non attivi non accettano nuovi partecipanti né lezioni e presenze");

        ComboBox<Insegnante> insegnante = new ComboBox<>("Insegnante");
        insegnante.setItems(insegnanteService.findAll());
        insegnante.setItemLabelGenerator(Insegnante::getNomeCompleto);

        binder.forField(nome).asRequired("Inserisci il nome del corso").bind(Corso::getNome, Corso::setNome);
        binder.forField(descrizione).bind(Corso::getDescrizione, Corso::setDescrizione);
        binder.forField(dataInizio).asRequired("Inserisci la data di inizio del corso").bind(Corso::getDataInizio, Corso::setDataInizio);
        binder.forField(numeroLezioni).asRequired("Inserisci il numero di lezioni")
            .bind(Corso::getNumeroLezioni, Corso::setNumeroLezioni);
        binder.forField(durataLezioni).asRequired("Inserisci la durata della lezione")
            .bind(Corso::getDurataLezioneMinuti, Corso::setDurataLezioneMinuti);
        binder.forField(orarioInizio).bind(Corso::getOrarioInizio, Corso::setOrarioInizio);
        binder.forField(costoTotale)
            .withValidator(costo -> costo == null || costo.signum() >= 0, "Il costo non può essere negativo")
            .bind(Corso::getCostoTotale, Corso::setCostoTotale);
        binder.forField(giorniSettimana).bind(Corso::getGiorniSettimana, Corso::setGiorniSettimana);
        binder.forField(insegnante).asRequired("Scegli l'insegnante del corso").bind(Corso::getInsegnante, Corso::setInsegnante);
        binder.forField(annoScolastico).asRequired("Scegli l'anno scolastico").bind(Corso::getAnnoScolastico, Corso::setAnnoScolastico);
        binder.forField(attivo).bind(Corso::isAttivo, Corso::setAttivo);

        binder.readBean(corso);

        formLayout.add(nome, insegnante, dataInizio, annoScolastico, numeroLezioni, durataLezioni, orarioInizio, costoTotale, attivo, giorniSettimana, descrizione);

        Button saveButton = new Button("Salva", e -> {
            // Campi obbligatori mancanti: gli errori sono mostrati sui campi
            if (!binder.writeBeanIfValid(corso)) {
                return;
            }
            try {
                corsoService.save(corso);
                updateGrid();
                dialog.close();
                Notification.show("Corso salvato con successo");
            } catch (Exception ex) {
                Notification.show("Errore nel salvataggio: " + ex.getMessage());
            }
        });
        saveButton.addThemeVariants(ButtonVariant.PRIMARY);

        formLayout.setColspan(giorniSettimana, 2);
        formLayout.setColspan(descrizione, 2);

        dialog.add(formLayout);
        dialog.getFooter().add(ViewSupport.closeButton(dialog, "Annulla"), saveButton);
        dialog.open();
    }

    private void openPartecipanti(Corso corso) {
        PartecipantiDialog dialog = new PartecipantiDialog(corsoService, allievoService, pagamentoService, corso);
        // Alla chiusura ricarica la griglia: numero di allievi e iscrizioni dei corsi aggiornati
        dialog.addOpenedChangeListener(e -> {
            if (!e.isOpened()) {
                updateGrid();
            }
        });
        dialog.open();
    }

    private void generaLezioni(Corso corso) {
        try {
            corsoService.generaLezioni(corso);
            Notification.show("Lezioni generate con successo");
        } catch (Exception e) {
            Notification.show("Errore nella generazione delle lezioni: " + e.getMessage());
        }
    }

    private void deleteCorso(Corso corso) {
        try {
            corsoService.delete(corso);
            updateGrid();
            Notification.show("Corso eliminato");
        } catch (RuntimeException ex) {
            Notification.show("Impossibile eliminare il corso: " + ex.getMessage());
        }
    }

    private void updateGrid() {
        List<Corso> tutti = corsoService.findAll();
        String annoScelto = filtroAnno.getValue();
        filtroAnno.setItems(tutti.stream().map(Corso::getAnnoScolastico).filter(a -> a != null).distinct().sorted().toList());
        filtroAnno.setValue(annoScelto);
        corsi = grid.setItems(tutti);
        applicaFiltri();
    }

    private void applicaFiltri() {
        if (corsi != null) {
            String anno = filtroAnno.getValue();
            corsi.setFilter(corso -> filtroStato.getValue().ammette(corso)
                && (anno == null || anno.equals(corso.getAnnoScolastico())));
        }
    }
}
