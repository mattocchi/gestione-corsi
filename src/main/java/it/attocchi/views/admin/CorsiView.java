package it.attocchi.views.admin;

import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import it.attocchi.entity.Corso;
import it.attocchi.entity.TipoCorso;
import it.attocchi.security.SecurityService;
import it.attocchi.service.CorsoService;
import it.attocchi.service.AnnoScolasticoService;
import it.attocchi.views.CardMobile;
import it.attocchi.views.Formati;
import it.attocchi.views.MainLayout;
import it.attocchi.views.RicercaRapida;
import it.attocchi.views.Schede;
import it.attocchi.views.TipoCorsoUI;
import it.attocchi.views.ViewSupport;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;
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
    private final AnnoScolasticoService annoScolasticoService;
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

    public CorsiView(CorsoService corsoService, AnnoScolasticoService annoScolasticoService,
                     SecurityService securityService) {
        this.annoScolasticoService = annoScolasticoService;
        this.corsoService = corsoService;
        this.puoEliminare = securityService.hasRole("ADMIN");

        addClassNames("size-full");
        configureGrid();

        Button addButton = new Button("Nuovo Corso", e -> CorsoView.apri(null, CorsoView.Scheda.DATI));

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
        Button editButton = ViewSupport.iconButton(VaadinIcon.PENCIL, "Modifica", e -> CorsoView.apri(corso.getId(), CorsoView.Scheda.DATI));
        Button partecipantiButton = ViewSupport.iconButton(VaadinIcon.USERS, "Partecipanti",
            e -> CorsoView.apri(corso.getId(), CorsoView.Scheda.ISCRITTI));
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

    /** Copia del corso per l'anno successivo, poi la sua pagina per le modifiche di dettaglio. */
    private void duplica(Corso corso) {
        try {
            Corso copia = corsoService.duplica(corso.getId());
            Notification.show("Copia di " + corso.getNome() + " per il " + copia.getAnnoScolastico()
                + ": non pubblicata, senza partecipanti");
            CorsoView.apri(copia.getId(), CorsoView.Scheda.DATI);
        } catch (RuntimeException ex) {
            Notification.show("Duplicazione non riuscita: " + ex.getMessage());
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
