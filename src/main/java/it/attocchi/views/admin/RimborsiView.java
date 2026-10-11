package it.attocchi.views.admin;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import it.attocchi.entity.AnnoScolastico;
import it.attocchi.security.SecurityService;
import it.attocchi.service.AnnoScolasticoService;
import it.attocchi.service.BilancioService;
import it.attocchi.service.BilancioService.RigaRimborsi;
import it.attocchi.service.RimborsoService;
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
        setSizeFull();

        filtroAnno = ViewSupport.filtroAnnoScolastico(
            annoScolasticoService.elenco().stream().map(AnnoScolastico::getNome).toList());
        filtroAnno.addValueChangeListener(e -> aggiorna());
        configureGrid();
        add(filtroAnno, grid);
        expand(grid);
        aggiorna();
    }

    private void configureGrid() {
        // Su mobile una sola colonna con insegnante e importi
        var riepilogo = grid.addColumn(LitRenderer.<RigaRimborsi>of(
                "<div class=\"riepilogo-righe\"><div class=\"riepilogo-righe-titolo\">${item.insegnante}</div>"
                    + "<div class=\"riepilogo-righe-dettaglio\">${item.dettaglio}</div></div>")
                .withProperty("insegnante", r -> r.insegnante().getNomeCompleto())
                .withProperty("dettaglio", r -> "Rimborsato " + Formati.euro(r.rimborsato())
                    + " · da rimborsare " + Formati.euro(r.daRimborsare())))
            .setHeader("Insegnante");
        var nome = grid.addColumn(r -> r.insegnante().getNomeCompleto()).setHeader("Insegnante").setSortable(true);
        var tariffa = grid.addColumn(r -> Formati.euro(r.insegnante().getTariffaOraria())).setHeader("Tariffa oraria")
            .setWidth("8rem").setFlexGrow(0);
        var rimborsato = euro("Rimborsato", RigaRimborsi::rimborsato);
        var maturato = euro("Maturato", RigaRimborsi::maturato);
        var daRimborsare = euro("Da rimborsare", RigaRimborsi::daRimborsare);
        ViewSupport.configureActionsColumn(grid.addComponentColumn(r -> ViewSupport.actions(
            ViewSupport.iconButton(VaadinIcon.EURO, "Rimborsi", e -> new RimborsiDialog(rimborsoService, bilancioService,
                r.insegnante(), filtroAnno.getValue(), puoEliminare, this::aggiorna).open()),
            ViewSupport.iconButton(VaadinIcon.BAR_CHART, "Bilancio", e -> new BilancioDialog(
                bilancioService.bilancio(r.insegnante().getId(), filtroAnno.getValue())).open()))));

        Signal<Boolean> mobile = ViewSupport.isMobile();
        Signal.effect(grid, () -> riepilogo.setVisible(mobile.get()));
        ViewSupport.hideOnMobile(grid, nome, tariffa, rimborsato, maturato, daRimborsare);
    }

    private Grid.Column<RigaRimborsi> euro(String header, Function<RigaRimborsi, BigDecimal> valore) {
        return grid.addColumn(r -> Formati.euro(valore.apply(r))).setHeader(header)
            .setComparator(valore::apply).setWidth("9rem").setFlexGrow(0);
    }

    private void aggiorna() {
        grid.setItems(filtroAnno.getValue() != null ? bilancioService.prospetto(filtroAnno.getValue()) : java.util.List.of());
    }
}
