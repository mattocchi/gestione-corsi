package it.attocchi.views.studente;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import it.attocchi.entity.Corso;
import it.attocchi.entity.Presenza;
import it.attocchi.entity.Studente;
import it.attocchi.security.SecurityService;
import it.attocchi.service.CorsoService;
import it.attocchi.service.PresenzaService;
import it.attocchi.service.StudenteService;
import it.attocchi.views.Formati;
import it.attocchi.views.MainLayout;
import it.attocchi.views.PresenzeStudenteDialog;
import it.attocchi.views.ViewSupport;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;

@Route(value = "miei-corsi", layout = MainLayout.class)
@PageTitle("I Miei Corsi")
@Menu(title = "I Miei Corsi", order = 30, icon = "vaadin:book")
@RolesAllowed("STUDENTE")
public class MieiCorsiView extends VerticalLayout {

    private final CorsoService corsoService;
    private final PresenzaService presenzaService;

    private final Grid<Corso> gridCorsi = new Grid<>(Corso.class, false);
    private Studente studenteCorrente;

    public MieiCorsiView(CorsoService corsoService, PresenzaService presenzaService,
                         SecurityService securityService, StudenteService studenteService) {
        this.corsoService = corsoService;
        this.presenzaService = presenzaService;

        setSizeFull();
        studenteCorrente = securityService.getUsername().flatMap(studenteService::findByUsername).orElse(null);
        configureGrid();

        add(gridCorsi);
        expand(gridCorsi);
        updateGrid();
    }

    private void configureGrid() {
        gridCorsi.addColumn(Corso::getNome).setHeader("Corso").setSortable(true);
        var docente = gridCorsi.addColumn(corso -> corso.getDocente() != null ? corso.getDocente().getNomeCompleto() : "").setHeader("Docente");
        var dataInizio = gridCorsi.addColumn(corso -> Formati.data(corso.getDataInizio())).setHeader("Data Inizio")
            .setComparator(Corso::getDataInizio);
        var numeroLezioni = gridCorsi.addColumn(Corso::getNumeroLezioni).setHeader("N. Lezioni");
        var durata = gridCorsi.addColumn(Corso::getDurataLezioneMinuti).setHeader("Durata (min)");

        ViewSupport.configureActionsColumn(gridCorsi.addComponentColumn(corso ->
            ViewSupport.actions(ViewSupport.iconButton(VaadinIcon.CHECK_SQUARE_O, "Visualizza Presenze", e -> openPresenzeDialog(corso)))));

        ViewSupport.hideOnMobile(gridCorsi, docente, dataInizio, numeroLezioni, durata);
    }

    private void openPresenzeDialog(Corso corso) {
        if (studenteCorrente == null) {
            return;
        }
        List<Presenza> presenze = presenzaService.findByCorsoAndStudente(corso.getId(), studenteCorrente.getId());
        new PresenzeStudenteDialog("Presenze – " + corso.getNome(), presenze).open();
    }

    private void updateGrid() {
        if (studenteCorrente != null) {
            List<Corso> corsi = corsoService.findByStudente(studenteCorrente.getId());
            gridCorsi.setItems(corsi);
        }
    }
}
