package it.attocchi.views.admin;

import java.util.List;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import it.attocchi.entity.AnnoScolastico;
import it.attocchi.security.SecurityService;
import it.attocchi.service.AnnoScolasticoService;
import it.attocchi.service.BilancioService;
import it.attocchi.service.BilancioService.RigaRimborsi;
import it.attocchi.service.RimborsoService;
import it.attocchi.views.CardMobile;
import it.attocchi.views.Formati;
import it.attocchi.views.MainLayout;
import it.attocchi.views.ViewSupport;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;
import java.util.function.Function;

/**
 * Rimborsi spese agli insegnanti per anno scolastico: una riga per insegnante con rimborsato, maturato (lezioni
 * completate) e da rimborsare; dai dialog si registrano i rimborsi (admin e segreteria, eliminazione solo admin)
 * e si vede il mini bilancio dell'insegnante.
 */
@Route(value = "rimborsi", layout = MainLayout.class)
@PageTitle("Rimborsi")
@Menu(title = "Rimborsi", order = 10.7, icon = "vaadin:wallet")
@RolesAllowed({"ADMIN", "SEGRETERIA"})
public class RimborsiView extends VerticalLayout {

    private final BilancioService bilancioService;
    private final RimborsoService rimborsoService;
    private final boolean puoEliminare;
    private final ComboBox<String> filtroAnno;
    private final Grid<RigaRimborsi> grid = new Grid<>(RigaRimborsi.class, false);

    public RimborsiView(BilancioService bilancioService, RimborsoService rimborsoService,
                        AnnoScolasticoService annoScolasticoService, SecurityService securityService) {
        this.bilancioService = bilancioService;
        this.rimborsoService = rimborsoService;
        this.puoEliminare = securityService.hasRole("ADMIN");
        addClassNames("size-full");

        filtroAnno = ViewSupport.filtroAnnoScolastico(
            annoScolasticoService.perFiltro(List.of()), annoScolasticoService.nonAttivi());
        filtroAnno.addValueChangeListener(e -> aggiorna());
        configureGrid();
        add(filtroAnno, grid);
        grid.addClassNames("grow");
        aggiorna();
    }

    private void configureGrid() {
        grid.addColumn(r -> r.insegnante().getNomeCompleto()).setHeader("Insegnante").setSortable(true);
        grid.addColumn(r -> Formati.euro(r.insegnante().getTariffaOraria())).setHeader("Tariffa oraria")
            .setWidth("8rem").setFlexGrow(0);
        euro("Rimborsato", RigaRimborsi::rimborsato);
        euro("Maturato", RigaRimborsi::maturato);
        euro("Da rimborsare", RigaRimborsi::daRimborsare);
        ViewSupport.configureActionsColumn(grid.addComponentColumn(this::createAzioni));

        CardMobile.per(grid)
            .titolo(r -> r.insegnante().getNomeCompleto())
            .campo("Tariffa oraria", r -> Formati.euro(r.insegnante().getTariffaOraria()))
            .campo("Rimborsato", r -> Formati.euro(r.rimborsato()))
            .campo("Maturato", r -> Formati.euro(r.maturato()))
            .campo("Da rimborsare", r -> Formati.euro(r.daRimborsare()))
            .azioni(this::createAzioni)
            .applica();
    }

    private HorizontalLayout createAzioni(RigaRimborsi r) {
        return ViewSupport.actions(
            ViewSupport.iconButton(VaadinIcon.EURO, "Rimborsi", e -> new RimborsiDialog(rimborsoService, bilancioService,
                r.insegnante(), filtroAnno.getValue(), puoEliminare, this::aggiorna).open()),
            ViewSupport.iconButton(VaadinIcon.BAR_CHART, "Bilancio", e -> new BilancioDialog(
                bilancioService.bilancio(r.insegnante().getId(), filtroAnno.getValue())).open()));
    }

    private Grid.Column<RigaRimborsi> euro(String header, Function<RigaRimborsi, BigDecimal> valore) {
        return grid.addColumn(r -> Formati.euro(valore.apply(r))).setHeader(header)
            .setComparator(valore::apply).setWidth("9rem").setFlexGrow(0);
    }

    private void aggiorna() {
        grid.setItems(filtroAnno.getValue() != null ? bilancioService.prospetto(filtroAnno.getValue()) : java.util.List.of());
    }
}
