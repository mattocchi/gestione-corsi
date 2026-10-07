package it.attocchi.views.docente;

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
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import it.attocchi.entity.Docente;
import it.attocchi.entity.Lezione;
import it.attocchi.entity.Presenza;
import it.attocchi.security.SecurityService;
import it.attocchi.service.DocenteService;
import it.attocchi.service.LezioneService;
import it.attocchi.service.PresenzaService;
import it.attocchi.views.Formati;
import it.attocchi.views.MainLayout;
import it.attocchi.views.SegmentedControl;
import it.attocchi.views.StatoPresenzaToggle;
import it.attocchi.views.ViewSupport;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;
import java.util.function.Consumer;

@Route(value = "lezioni-docente", layout = MainLayout.class)
@PageTitle("Le Mie Lezioni")
@Menu(title = "Le Mie Lezioni", order = 20, icon = "vaadin:calendar")
@RolesAllowed("DOCENTE")
public class LezioniDocenteView extends VerticalLayout {

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

    private final Grid<Lezione> gridLezioni = new Grid<>(Lezione.class, false);
    private final SegmentedControl<FiltroLezioni> filtro = new SegmentedControl<>("Filtra le lezioni per stato",
        List.of(FiltroLezioni.values()), FiltroLezioni::getEtichetta, f -> "Mostra lezioni: " + f.getEtichetta(), null);
    private GridListDataView<Lezione> lezioni;
    private Docente docenteCorrente;

    public LezioniDocenteView(LezioneService lezioneService, PresenzaService presenzaService,
                              SecurityService securityService, DocenteService docenteService) {
        this.lezioneService = lezioneService;
        this.presenzaService = presenzaService;

        setSizeFull();
        docenteCorrente = securityService.getUsername().flatMap(docenteService::findByUsername).orElse(null);
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
        gridLezioni.addColumn(lezione -> lezione.getCorso().getNome()).setHeader("Corso").setSortable(true);
        gridLezioni.addColumn(lezione -> Formati.data(lezione.getData())).setHeader("Data")
            .setComparator(Lezione::getData).setSortable(true).setAutoWidth(true).setFlexGrow(0);
        var numero = gridLezioni.addColumn(Lezione::getNumeroLezione).setHeader("N. Lezione");
        var orario = gridLezioni.addColumn(Lezione::getOrarioInizio).setHeader("Orario");
        var completata = gridLezioni.addColumn(lezione -> lezione.isCompletata() ? "Sì" : "No").setHeader("Completata");

        ViewSupport.configureActionsColumn(gridLezioni.addComponentColumn(lezione ->
            ViewSupport.actions(ViewSupport.iconButton(VaadinIcon.CLIPBOARD_CHECK, "Gestisci Presenze", e -> openPresenzeDialog(lezione)))));

        ViewSupport.hideOnMobile(gridLezioni, numero, orario, completata);
    }

    private void openPresenzeDialog(Lezione lezione) {
        Dialog dialog = ViewSupport.dialog(
            lezione.getCorso().getNome() + " – Lezione " + lezione.getNumeroLezione() + " (" + Formati.data(lezione.getData()) + ")", "800px");

        Grid<Presenza> gridPresenze = new Grid<>(Presenza.class, false);
        // Una sola colonna con nome, stato e note: su schermi stretti gli elementi vanno a capo
        gridPresenze.addComponentColumn(this::createPresenzaRiga).setHeader("Studenti");
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
        Span nome = new Span(presenza.getStudente().getNomeCompleto());
        nome.addClassName("presenza-nome");

        StatoPresenzaToggle stato = new StatoPresenzaToggle();
        stato.setValue(presenza.getStato());
        stato.addValueChangeListener(e -> salva(presenza, p -> p.setStato(e.getValue())));

        TextField note = new TextField();
        note.addClassName("presenza-note");
        note.setAriaLabel("Note per " + presenza.getStudente().getNomeCompleto());
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
                + presenza.getStudente().getNomeCompleto() + ": " + ex.getMessage());
        }
    }

    private void updateGrid() {
        if (docenteCorrente != null) {
            lezioni = gridLezioni.setItems(lezioneService.findByDocente(docenteCorrente.getId()));
            applicaFiltro();
        }
    }

    private void applicaFiltro() {
        if (lezioni != null) {
            lezioni.setFilter(filtro.getValue().condizione);
        }
    }
}
