package it.attocchi.views.allievo;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
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
import it.attocchi.service.PagamentoService.Situazione;
import it.attocchi.views.CardMobile;
import it.attocchi.views.Formati;
import it.attocchi.views.MainLayout;
import it.attocchi.views.PresenzeAllievoDialog;
import it.attocchi.views.PrezziCorso;
import it.attocchi.views.ViewSupport;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;
import java.util.Map;

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

        addClassNames("size-full");
        allievoCorrente = securityService.getUsername().flatMap(allievoService::findByUsername).orElse(null);
        configureGrid();

        add(gridCorsi);
        gridCorsi.addClassNames("grow");
        updateGrid();
    }

    private void configureGrid() {
        gridCorsi.addColumn(Corso::getNome).setHeader("Corso").setSortable(true);
        gridCorsi.addColumn(MieiCorsiView::insegnante).setHeader("Insegnanti");
        gridCorsi.addColumn(corso -> Formati.data(corso.getDataInizio())).setHeader("Data Inizio")
            .setComparator(Corso::getDataInizio).setAutoWidth(true).setFlexGrow(0);
        gridCorsi.addColumn(Corso::getNumeroLezioni).setHeader("N. Lezioni");
        gridCorsi.addColumn(Corso::getDurataLezioneMinuti).setHeader("Durata (min)");
        Map<Long, Situazione> situazioni = allievoCorrente != null
            ? pagamentoService.situazioniDelloAllievo(allievoCorrente.getId()) : Map.of();
        PrezziCorso.aggiungiColonne(gridCorsi, situazioni);
        ViewSupport.configureActionsColumn(gridCorsi.addComponentColumn(this::createAzioni));

        CardMobile<Corso> card = CardMobile.per(gridCorsi)
            .titolo(Corso::getNome)
            .campo("Insegnanti", MieiCorsiView::insegnante)
            .campo("Data Inizio", corso -> Formati.data(corso.getDataInizio()))
            .campo("N. Lezioni", Corso::getNumeroLezioni)
            .campo("Durata (min)", Corso::getDurataLezioneMinuti);
        PrezziCorso.aggiungiCampi(card, situazioni).azioni(this::createAzioni).applica();
    }

    private HorizontalLayout createAzioni(Corso corso) {
        return ViewSupport.actions(ViewSupport.iconButton(VaadinIcon.CLIPBOARD_CHECK, "Visualizza Presenze",
            e -> openPresenzeDialog(corso)));
    }

    private static String insegnante(Corso corso) {
        return corso.getNomiInsegnanti();
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
