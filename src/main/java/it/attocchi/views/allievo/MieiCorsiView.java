package it.attocchi.views.allievo;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import it.attocchi.entity.Corso;
import it.attocchi.entity.Presenza;
import it.attocchi.entity.Allievo;
import it.attocchi.security.SecurityService;
import it.attocchi.service.CorsoService;
import it.attocchi.service.PresenzaService;
import it.attocchi.service.AllievoService;
import it.attocchi.service.PagamentoService;
import it.attocchi.views.Formati;
import it.attocchi.views.MainLayout;
import it.attocchi.views.PresenzeAllievoDialog;
import it.attocchi.views.PrezziCorso;
import it.attocchi.views.ViewSupport;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;

@Route(value = "miei-corsi", layout = MainLayout.class)
@PageTitle("I Miei Corsi")
@Menu(title = "I Miei Corsi", order = 30, icon = "vaadin:book")
@RolesAllowed("ALLIEVO")
public class MieiCorsiView extends VerticalLayout {

    private final CorsoService corsoService;
    private final PresenzaService presenzaService;
    private final PagamentoService pagamentoService;

    private final Grid<Corso> gridCorsi = new Grid<>(Corso.class, false);
    private Allievo allievoCorrente;

    public MieiCorsiView(CorsoService corsoService, PresenzaService presenzaService,
                         SecurityService securityService, AllievoService allievoService, PagamentoService pagamentoService) {
        this.corsoService = corsoService;
        this.presenzaService = presenzaService;
        this.pagamentoService = pagamentoService;

        setSizeFull();
        allievoCorrente = securityService.getUsername().flatMap(allievoService::findByUsername).orElse(null);
        configureGrid();

        add(gridCorsi);
        expand(gridCorsi);
        updateGrid();
    }

    private void configureGrid() {
        var nome = gridCorsi.addColumn(Corso::getNome).setHeader("Corso").setSortable(true);
        var insegnante = gridCorsi.addColumn(corso -> corso.getInsegnante() != null ? corso.getInsegnante().getNomeCompleto() : "").setHeader("Insegnante");
        var dataInizio = gridCorsi.addColumn(corso -> Formati.data(corso.getDataInizio())).setHeader("Data Inizio")
            .setComparator(Corso::getDataInizio).setAutoWidth(true).setFlexGrow(0);
        var numeroLezioni = gridCorsi.addColumn(Corso::getNumeroLezioni).setHeader("N. Lezioni");
        var durata = gridCorsi.addColumn(Corso::getDurataLezioneMinuti).setHeader("Durata (min)");
        if (allievoCorrente != null) {
            PrezziCorso.aggiungiColonne(gridCorsi, nome, pagamentoService.situazioniDelloAllievo(allievoCorrente.getId()));
        }

        ViewSupport.configureActionsColumn(gridCorsi.addComponentColumn(corso ->
            ViewSupport.actions(ViewSupport.iconButton(VaadinIcon.CLIPBOARD_CHECK, "Visualizza Presenze", e -> openPresenzeDialog(corso)))));

        ViewSupport.hideOnMobile(gridCorsi, insegnante, dataInizio, numeroLezioni, durata);
    }

    private void openPresenzeDialog(Corso corso) {
        if (allievoCorrente == null) {
            return;
        }
        List<Presenza> presenze = presenzaService.findByCorsoAndAllievo(corso.getId(), allievoCorrente.getId());
        new PresenzeAllievoDialog("Presenze – " + corso.getNome(), presenze).open();
    }

    private void updateGrid() {
        if (allievoCorrente != null) {
            List<Corso> corsi = corsoService.findAttiviByAllievo(allievoCorrente.getId());
            gridCorsi.setItems(corsi);
        }
    }
}
