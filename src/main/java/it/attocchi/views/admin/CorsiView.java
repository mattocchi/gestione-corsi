package it.attocchi.views.admin;

import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.html.Div;
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
import it.attocchi.entity.AnnoScolastico;
import it.attocchi.entity.Corso;
import it.attocchi.entity.Insegnante;
import it.attocchi.entity.Luogo;
import it.attocchi.entity.TipoCorso;
import it.attocchi.security.SecurityService;
import it.attocchi.service.CorsoService;
import it.attocchi.service.InsegnanteService;
import it.attocchi.service.PagamentoService;
import it.attocchi.service.AllievoService;
import it.attocchi.service.AnnoScolasticoService;
import it.attocchi.service.LuogoService;
import it.attocchi.views.CardMobile;
import it.attocchi.views.Formati;
import it.attocchi.views.MainLayout;
import it.attocchi.views.RicercaRapida;
import it.attocchi.views.Schede;
import it.attocchi.views.SegmentedControl;
import it.attocchi.views.TipoCorsoUI;
import it.attocchi.views.Stili;
import it.attocchi.views.ViewSupport;
import jakarta.annotation.security.RolesAllowed;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Route(value = "corsi", layout = MainLayout.class)
@PageTitle("Corsi")
@Menu(title = "Corsi", order = 10, icon = "vaadin:music")
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

    /** Filtro della griglia per tipo: corsi, servizi o entrambi. */
    public enum FiltroTipo {
        TUTTI("Corsi e servizi", null),
        CORSI("Solo corsi", TipoCorso.CORSO),
        SERVIZI("Solo servizi", TipoCorso.SERVIZIO);

        private final String etichetta;
        private final TipoCorso tipo;

        FiltroTipo(String etichetta, TipoCorso tipo) {
            this.etichetta = etichetta;
            this.tipo = tipo;
        }

        public String getEtichetta() {
            return etichetta;
        }

        public boolean ammette(Corso corso) {
            return tipo == null || corso.getTipo() == tipo;
        }
    }

    private final CorsoService corsoService;
    private final InsegnanteService insegnanteService;
    private final AllievoService allievoService;
    private final PagamentoService pagamentoService;
    private final AnnoScolasticoService annoScolasticoService;
    private final LuogoService luogoService;
    /** L'eliminazione dei corsi è riservata all'admin; la segreteria li gestisce senza eliminarli. */
    private final boolean puoEliminare;

    private final Grid<Corso> grid = new Grid<>(Corso.class, false);
    private final Schede<FiltroStato> filtroStato = new Schede<>("Filtra i corsi per stato",
        List.of(FiltroStato.values()), FiltroStato::getEtichetta, f -> "Mostra corsi: " + f.getEtichetta());
    private final Select<FiltroTipo> filtroTipo = new Select<>();
    private final ComboBox<String> filtroAnno = new ComboBox<>();
    private final Checkbox soloNonPubblicati = new Checkbox("Solo non pubblicati");
    private final RicercaRapida ricerca = new RicercaRapida();
    private GridListDataView<Corso> corsi;
    private List<Corso> tutti = List.of();
    private Map<Long, Long> gruppiPerCorso = Map.of();

    public CorsiView(CorsoService corsoService, InsegnanteService insegnanteService, AllievoService allievoService,
                     PagamentoService pagamentoService, AnnoScolasticoService annoScolasticoService,
                     LuogoService luogoService, SecurityService securityService) {
        this.luogoService = luogoService;
        this.pagamentoService = pagamentoService;
        this.annoScolasticoService = annoScolasticoService;
        this.corsoService = corsoService;
        this.insegnanteService = insegnanteService;
        this.allievoService = allievoService;
        this.puoEliminare = securityService.hasRole("ADMIN");

        addClassNames("size-full");
        configureGrid();

        Button addButton = new Button("Nuovo Corso", e -> openDialog(new Corso()));

        // Di default solo i corsi attivi, di tutti gli anni
        filtroStato.setValue(FiltroStato.ATTIVI);
        filtroStato.addValueChangeListener(e -> applicaFiltri());
        filtroTipo.setItems(FiltroTipo.values());
        filtroTipo.setItemLabelGenerator(FiltroTipo::getEtichetta);
        filtroTipo.setAriaLabel("Filtra per tipo");
        filtroTipo.setValue(FiltroTipo.TUTTI);
        filtroTipo.addClassNames("w-44");
        filtroTipo.addValueChangeListener(e -> applicaFiltri());
        filtroAnno.setPlaceholder("Tutti gli anni");
        filtroAnno.setAriaLabel("Filtra per anno scolastico");
        filtroAnno.setClearButtonVisible(true);
        filtroAnno.addClassNames("w-44");
        filtroAnno.addValueChangeListener(e -> applicaFiltri());
        soloNonPubblicati.addValueChangeListener(e -> applicaFiltri());
        ricerca.addRicercaListener(this::applicaFiltri);

        HorizontalLayout toolbar = new HorizontalLayout(addButton, filtroTipo, filtroAnno, soloNonPubblicati, ricerca);
        toolbar.addClassNames("flex-wrap w-full items-center");
        add(filtroStato, toolbar, grid);
        grid.addClassNames("grow");

        updateGrid();
    }

    private void configureGrid() {
        grid.addComponentColumn(TipoCorsoUI::nome).setHeader("Nome").setComparator(Corso::getNome);
        grid.addColumn(Corso::getAnnoScolastico).setHeader("Anno").setSortable(true).setWidth("7rem").setFlexGrow(0);
        grid.addColumn(Corso::getNomiInsegnanti).setHeader("Insegnanti");
        grid.addColumn(corso -> Formati.data(corso.getDataInizio())).setHeader("Data Inizio")
            .setComparator(Corso::getDataInizio).setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(Formati::giorniECadenza).setHeader("Giorni");
        grid.addColumn(Corso::getNomeLuogo).setHeader("Luogo").setSortable(true);
        grid.addColumn(Formati::lezioniCorso).setHeader("Lezioni");
        grid.addColumn(Corso::getDurataLezioneMinuti).setHeader("Durata (min)");
        grid.addColumn(corso -> corso.getAllievi().size()).setHeader("N. Allievi");
        grid.addColumn(this::numeroGruppi).setHeader("Gruppi");
        grid.addComponentColumn(this::createStatoBadge).setHeader("Stato").setWidth("8.5rem").setFlexGrow(0);
        ViewSupport.configureActionsColumn(grid.addComponentColumn(this::createActions));

        CardMobile.per(grid)
            .titolo(Corso::getNome)
            .stato(this::createStatoBadge)
            .campo("Tipo", TipoCorsoUI::badge)
            .campo("Pubblicazione", TipoCorsoUI::badgePubblicazione)
            .campo("Anno", Corso::getAnnoScolastico)
            .campo("Insegnanti", Corso::getNomiInsegnanti)
            .campo("Data Inizio", corso -> Formati.data(corso.getDataInizio()))
            .campo("Giorni", Formati::giorniECadenza)
            .campo("Luogo", Corso::getNomeLuogo)
            .campo("Lezioni", Formati::lezioniCorso)
            .campo("Durata (min)", Corso::getDurataLezioneMinuti)
            .campo("N. Allievi", corso -> corso.getAllievi().size())
            .campo("Gruppi", this::numeroGruppi)
            .azioni(this::createActions)
            .applica();
    }

    private HorizontalLayout createActions(Corso corso) {
        Button editButton = ViewSupport.iconButton(VaadinIcon.PENCIL, "Modifica", e -> openDialog(corso));
        Button partecipantiButton = ViewSupport.iconButton(VaadinIcon.USERS, "Partecipanti",
            e -> openPartecipanti(corso));
        Button generateButton = ViewSupport.iconButton(VaadinIcon.REFRESH, "Aggiorna lezioni",
            e -> AggiornaLezioniDialog.apri(corso, this::updateGrid));
        generateButton.setEnabled(CorsoService.puoModificare(corso));
        Button duplicaButton = ViewSupport.iconButton(VaadinIcon.COPY, "Duplica", e -> duplica(corso));
        duplicaButton.setTooltipText("Duplica per l'anno successivo (senza partecipanti, non pubblicato)");
        HorizontalLayout actions = ViewSupport.actions(editButton, partecipantiButton, generateButton, duplicaButton);
        if (puoEliminare) {
            Button deleteButton = ViewSupport.iconButton(VaadinIcon.TRASH, "Elimina", e -> deleteCorso(corso));
            deleteButton.addThemeVariants(ButtonVariant.ERROR);
            actions.add(deleteButton);
        }
        return actions;
    }

    /** Numero di gruppi del corso; vuoto se non ne ha. */
    private String numeroGruppi(Corso corso) {
        Long numero = gruppiPerCorso.get(corso.getId());
        return numero != null ? numero.toString() : "";
    }

    private Badge createStatoBadge(Corso corso) {
        Badge badge = corso.isAttivo()
            ? new Badge("Attivo", VaadinIcon.CHECK.create())
            : new Badge("Non attivo", VaadinIcon.BAN.create());
        if (corso.isAttivo()) {
            badge.addThemeVariants(BadgeVariant.SUCCESS);
        }
        return badge;
    }

    private void openDialog(Corso corso) {
        Dialog dialog = ViewSupport.dialog(corso.getId() == null ? "Nuovo Corso" : "Modifica Corso", "600px");

        FormLayout formLayout = new FormLayout();
        Binder<Corso> binder = new Binder<>(Corso.class);

        SegmentedControl<TipoCorso> tipo = new SegmentedControl<>("Tipo", List.of(TipoCorso.values()),
            TipoCorso::getDescrizione, t -> t == TipoCorso.SERVIZIO
                ? "Servizio (es. Pulizia): come un corso, svolto da un incaricato" : "Corso", null);
        Span etichettaTipo = new Span("Tipo");
        etichettaTipo.addClassNames(Stili.TESTO_SECONDARIO);
        Div bloccoTipo = new Div(etichettaTipo, tipo);
        bloccoTipo.addClassNames("flex flex-col gap-1");
        TextField nome = new TextField("Nome");
        TextArea descrizione = new TextArea("Descrizione");
        DatePicker dataInizio = ViewSupport.datePicker("Data Inizio");
        IntegerField numeroLezioni = new IntegerField("Numero Lezioni");
        numeroLezioni.setHelperText("Per ogni allievo, nei giorni che frequenta; facoltativo con la data di fine");
        numeroLezioni.setClearButtonVisible(true);
        DatePicker dataFine = ViewSupport.datePicker("Data di fine");
        dataFine.setHelperText("Ultimo giorno di lezione; facoltativa con il numero di lezioni");
        dataFine.setClearButtonVisible(true);
        IntegerField durataLezioni = new IntegerField("Durata Lezione (minuti)");
        TimePicker orarioInizio = ViewSupport.timePicker("Orario Inizio");
        BigDecimalField costoTotale = new BigDecimalField("Costo Totale");
        costoTotale.setLocale(Locale.ITALY);
        costoTotale.setPrefixComponent(new Span("€"));
        BigDecimalField prezzoPreiscrizione = new BigDecimalField("Prezzo con pre-iscrizione");
        prezzoPreiscrizione.setLocale(Locale.ITALY);
        prezzoPreiscrizione.setPrefixComponent(new Span("€"));
        prezzoPreiscrizione.setClearButtonVisible(true);

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

        SegmentedControl<Integer> cadenza = new SegmentedControl<>("Cadenza delle lezioni", List.of(1, 2),
            settimane -> settimane == 1 ? "Ogni settimana" : "Ogni 2 settimane",
            settimane -> settimane == 1 ? "Lezione ogni settimana" : "Lezione ogni 2 settimane, dalla data di inizio",
            null);
        Span aiutoCadenza = new Span("Ogni 2 settimane: le settimane alterne partono dalla data di inizio e una "
            + "festività sposta la lezione alla settimana dopo. Vale da \"Aggiorna lezioni\".");
        aiutoCadenza.addClassNames(Stili.TESTO_SECONDARIO);
        Div bloccoCadenza = new Div(cadenza, aiutoCadenza);
        bloccoCadenza.addClassNames("flex flex-col gap-1");

        ComboBox<String> annoScolastico = new ComboBox<>("Anno Scolastico");
        List<String> anni = annoScolasticoService.scelteCorso(corso.getAnnoScolastico());
        annoScolastico.setItems(anni);
        // Proposto dalla data di inizio finché non viene scelto, se è tra gli anni attivi
        dataInizio.addValueChangeListener(e -> {
            if (e.getValue() != null && annoScolastico.isEmpty() && anni.contains(AnnoScolastico.di(e.getValue()))) {
                annoScolastico.setValue(AnnoScolastico.di(e.getValue()));
            }
        });
        Checkbox pubblicato = new Checkbox("Pubblicato");
        pubblicato.setTooltipText("Visibile a genitori e allievi; finché non è pubblicato ci lavorano solo staff e insegnanti");
        Checkbox attivo = new Checkbox("Attivo");
        attivo.setTooltipText("I corsi non attivi non accettano nuovi partecipanti né lezioni e presenze");

        ComboBox<Insegnante> insegnante = new ComboBox<>("Insegnante");
        insegnante.setItems(insegnanteService.findAll());
        insegnante.setItemLabelGenerator(Insegnante::getNomeCompleto);
        MultiSelectComboBox<Insegnante> altriInsegnanti = new MultiSelectComboBox<>("Altri insegnanti");
        altriInsegnanti.setItems(insegnanteService.findAll());
        altriInsegnanti.setItemLabelGenerator(Insegnante::getNomeCompleto);
        altriInsegnanti.setClearButtonVisible(true);
        altriInsegnanti.setHelperText("Facoltativi, fino a " + Corso.MAX_FACOLTATIVI + ": stessi diritti sul corso");
        // Per i servizi chi lo svolge è un "Incaricato"
        tipo.addValueChangeListener(e -> {
            TipoCorso scelto = e.getValue() != null ? e.getValue() : TipoCorso.CORSO;
            insegnante.setLabel(scelto.getChiLoSvolge());
            altriInsegnanti.setLabel("Altri " + scelto.getChiLoSvolgonoPlurale().toLowerCase());
        });

        MultiSelectComboBox<Luogo> luoghiAbilitati = new MultiSelectComboBox<>("Luoghi abilitati");
        luoghiAbilitati.setItems(luogoService.scelteCorso(corso.getLuoghiAbilitati()));
        luoghiAbilitati.setItemLabelGenerator(Luogo::getNome);
        luoghiAbilitati.setHelperText("Proposti per primi sulle lezioni; si può scegliere comunque ogni luogo attivo");
        ComboBox<Luogo> luogo = new ComboBox<>("Luogo predefinito");
        luogo.setItems(luogoService.scelteCorso(corso.getLuogo() != null ? List.of(corso.getLuogo()) : List.of()));
        luogo.setItemLabelGenerator(Luogo::getNome);
        luogo.setClearButtonVisible(true);
        luogo.setHelperText("Facoltativo: dove si svolgono di solito le lezioni");
        // Il predefinito è anche abilitato
        luogo.addValueChangeListener(e -> {
            if (e.getValue() != null && !luoghiAbilitati.getValue().contains(e.getValue())) {
                luoghiAbilitati.select(e.getValue());
            }
        });

        binder.forField(luogo).bind(Corso::getLuogo, Corso::setLuogo);
        binder.forField(luoghiAbilitati).bind(Corso::getLuoghiAbilitati, Corso::setLuoghiAbilitati);
        binder.forField(tipo).asRequired("Scegli il tipo").bind(Corso::getTipo, Corso::setTipo);
        binder.forField(nome).asRequired("Inserisci il nome del corso").bind(Corso::getNome, Corso::setNome);
        binder.forField(descrizione).bind(Corso::getDescrizione, Corso::setDescrizione);
        binder.forField(dataInizio).asRequired("Inserisci la data di inizio del corso").bind(Corso::getDataInizio, Corso::setDataInizio);
        // Almeno uno tra numero di lezioni e data di fine: con entrambi vale il primo limite raggiunto
        var bindingNumero = binder.forField(numeroLezioni)
            .withValidator(n -> n != null || dataFine.getValue() != null, "Indica il numero di lezioni o la data di fine")
            .withValidator(n -> n == null || n >= 1, "Almeno 1 lezione")
            .bind(Corso::getNumeroLezioni, Corso::setNumeroLezioni);
        binder.forField(dataFine)
            .withValidator(d -> d == null || dataInizio.getValue() == null || !d.isBefore(dataInizio.getValue()),
                "Non può essere prima della data di inizio")
            .bind(Corso::getDataFine, Corso::setDataFine);
        dataFine.addValueChangeListener(e -> bindingNumero.validate());
        binder.forField(durataLezioni).asRequired("Inserisci la durata della lezione")
            .bind(Corso::getDurataLezioneMinuti, Corso::setDurataLezioneMinuti);
        binder.forField(orarioInizio).bind(Corso::getOrarioInizio, Corso::setOrarioInizio);
        binder.forField(costoTotale)
            .withValidator(costo -> costo == null || costo.signum() >= 0, "Il costo non può essere negativo")
            .bind(Corso::getCostoTotale, Corso::setCostoTotale);
        var bindingPreiscrizione = binder.forField(prezzoPreiscrizione)
            .withValidator(prezzo -> prezzo == null || prezzo.signum() >= 0, "Il prezzo non può essere negativo")
            .withValidator(prezzo -> prezzo == null || (costoTotale.getValue() != null && prezzo.compareTo(costoTotale.getValue()) <= 0),
                "Non può superare il costo totale")
            .bind(Corso::getPrezzoPreiscrizione, Corso::setPrezzoPreiscrizione);
        // Aiuto sul valore tipico (sconto 15%), assegnato comunque a mano
        costoTotale.addValueChangeListener(e -> {
            BigDecimal tipico = Corso.prezzoPreiscrizioneTipico(e.getValue());
            prezzoPreiscrizione.setHelperText(tipico != null ? "Di solito " + Formati.euro(tipico) + " (sconto 15%)" : null);
            if (!prezzoPreiscrizione.isEmpty()) {
                bindingPreiscrizione.validate();
            }
        });
        binder.forField(giorniSettimana).bind(Corso::getGiorniSettimana, Corso::setGiorniSettimana);
        binder.forField(cadenza).asRequired("Scegli la cadenza").bind(Corso::getCadenzaSettimane, Corso::setCadenzaSettimane);
        binder.forField(insegnante).asRequired("Scegli l'insegnante del corso").bind(Corso::getInsegnante, Corso::setInsegnante);
        // Nell'ordine di scelta, fino a MAX_FACOLTATIVI e senza il titolare
        binder.forField(altriInsegnanti)
            .withValidator(altri -> altri.size() <= Corso.MAX_FACOLTATIVI, "Al massimo " + Corso.MAX_FACOLTATIVI + " insegnanti")
            .withValidator(altri -> !altri.contains(insegnante.getValue()), "Il titolare non va ripetuto qui")
            .bind(c -> new LinkedHashSet<>(c.getInsegnantiFacoltativi()),
                (c, altri) -> c.setInsegnantiFacoltativi(new ArrayList<>(altri)));
        binder.forField(annoScolastico).asRequired("Scegli l'anno scolastico").bind(Corso::getAnnoScolastico, Corso::setAnnoScolastico);
        binder.forField(attivo).bind(Corso::isAttivo, Corso::setAttivo);
        binder.forField(pubblicato).bind(Corso::isPubblicato, Corso::setPubblicato);

        binder.readBean(corso);

        formLayout.add(bloccoTipo, nome, insegnante, altriInsegnanti, dataInizio, annoScolastico, numeroLezioni, dataFine, durataLezioni, orarioInizio, costoTotale, prezzoPreiscrizione, attivo, pubblicato, giorniSettimana, bloccoCadenza, luogo, luoghiAbilitati, descrizione);

        Button saveButton = new Button("Salva", e -> {
            // Campi obbligatori mancanti: gli errori sono mostrati sui campi
            if (!binder.writeBeanIfValid(corso)) {
                return;
            }
            try {
                Corso salvato = corsoService.save(corso);
                updateGrid();
                dialog.close();
                Notification.show("Corso salvato con successo");
                proponiAggiornamento(salvato);
            } catch (Exception ex) {
                Notification.show("Errore nel salvataggio: " + ex.getMessage());
            }
        });
        saveButton.addThemeVariants(ButtonVariant.PRIMARY);

        formLayout.setColspan(bloccoTipo, 2);
        formLayout.setColspan(giorniSettimana, 2);
        formLayout.setColspan(bloccoCadenza, 2);
        formLayout.setColspan(descrizione, 2);

        dialog.add(formLayout);
        dialog.getFooter().add(ViewSupport.closeButton(dialog, "Annulla"), saveButton);
        dialog.open();
    }

    private void openPartecipanti(Corso corso) {
        PartecipantiDialog dialog = new PartecipantiDialog(corsoService, allievoService, pagamentoService, corso, puoEliminare);
        // Alla chiusura ricarica la griglia: numero di allievi e iscrizioni dei corsi aggiornati
        dialog.addOpenedChangeListener(e -> {
            if (!e.isOpened()) {
                updateGrid();
            }
        });
        dialog.open();
    }

    /** Copia del corso per l'anno successivo, poi il dialog per le modifiche di dettaglio. */
    private void duplica(Corso corso) {
        try {
            Corso copia = corsoService.duplica(corso.getId());
            updateGrid();
            Notification.show("Copia di " + corso.getNome() + " per il " + copia.getAnnoScolastico()
                + ": non pubblicata, senza partecipanti");
            openDialog(corsoService.findById(copia.getId()));
        } catch (RuntimeException ex) {
            Notification.show("Duplicazione non riuscita: " + ex.getMessage());
        }
    }

    /**
     * Dopo il salvataggio: se il corso ha già lezioni e il suo calendario è cambiato (fine, numero di lezioni, giorni,
     * cadenza…) un avviso propone di aggiornarle.
     */
    private void proponiAggiornamento(Corso corso) {
        if (!CorsoService.puoModificare(corso)) {
            return;
        }
        CorsoService.AggiornamentoLezioni anteprima = corsoService.anteprimaAggiornamento(corso.getId());
        boolean haLezioni = anteprima.invariate() > 0 || !anteprima.tolte().isEmpty();
        if (!haLezioni || !anteprima.cambia()) {
            return;
        }
        Notification avviso = new Notification();
        avviso.setDuration(10000);
        Button aggiorna = new Button("Aggiorna lezioni", e -> {
            avviso.close();
            AggiornaLezioniDialog.apri(corso, this::updateGrid);
        });
        aggiorna.addThemeVariants(ButtonVariant.PRIMARY);
        HorizontalLayout riga = new HorizontalLayout(new Span("Il calendario delle lezioni di " + corso.getNome()
            + " è cambiato."), aggiorna);
        riga.addClassNames("items-center");
        avviso.add(riga);
        avviso.open();
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
        tutti = corsoService.findAll();
        gruppiPerCorso = corsoService.numeroGruppi();
        String annoScelto = filtroAnno.getValue();
        // Anche gli anni non attivi (archivio): non si scelgono per i corsi nuovi ma si filtrano
        filtroAnno.setItems(annoScolasticoService.perFiltro(tutti.stream().map(Corso::getAnnoScolastico).toList()));
        filtroAnno.setItemLabelGenerator(ViewSupport.etichettaAnno(annoScolasticoService.nonAttivi()));
        filtroAnno.setValue(annoScelto);
        corsi = grid.setItems(tutti);
        applicaFiltri();
    }

    /** Stato, anno e ricerca rapida (nome, anno, insegnanti, descrizione); "N di M" sui corsi di stato e anno scelti. */
    private void applicaFiltri() {
        if (corsi != null) {
            String anno = filtroAnno.getValue();
            SerializablePredicate<Corso> statoEAnno = corso -> filtroStato.getValue().ammette(corso)
                && filtroTipo.getValue().ammette(corso)
                && (!soloNonPubblicati.getValue() || !corso.isPubblicato())
                && (anno == null || anno.equals(corso.getAnnoScolastico()));
            corsi.setFilter(corso -> statoEAnno.test(corso) && ricerca.corrisponde(RicercaRapida.testo(corso.getNome(),
                corso.getAnnoScolastico(), corso.getNomiInsegnanti(), corso.getDescrizione(), corso.getNomeLuogo())));
            ricerca.mostraConteggio(corsi.getItemCount(), (int) tutti.stream().filter(statoEAnno).count());
        }
    }
}
