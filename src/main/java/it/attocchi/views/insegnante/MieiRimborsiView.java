package it.attocchi.views.insegnante;

import java.util.stream.IntStream;
import java.time.YearMonth;
import it.attocchi.views.RiepilogoLezioniMese;
import it.attocchi.service.BilancioService;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import it.attocchi.entity.AnnoScolastico;
import it.attocchi.entity.Insegnante;
import it.attocchi.entity.Rimborso;
import it.attocchi.security.SecurityService;
import it.attocchi.service.AnnoScolasticoService;
import it.attocchi.service.InsegnanteService;
import it.attocchi.service.RimborsoService;
import it.attocchi.views.CardMobile;
import it.attocchi.views.Formati;
import it.attocchi.views.MainLayout;
import it.attocchi.views.ViewSupport;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;

/** Rimborsi ricevuti dall'insegnante, in sola lettura, per anno scolastico. */
@Route(value = "miei-rimborsi", layout = MainLayout.class)
@PageTitle("I miei rimborsi")
@Menu(title = "I miei rimborsi", order = 21, icon = "vaadin:wallet")
@RolesAllowed("INSEGNANTE")
public class MieiRimborsiView extends VerticalLayout {

    private final RimborsoService rimborsoService;
    private final BilancioService bilancioService;
    private final Insegnante insegnante;
    private final ComboBox<String> filtroAnno;
    private final Grid<Rimborso> grid = new Grid<>(Rimborso.class, false);
    private final Span totale = new Span();

    public MieiRimborsiView(RimborsoService rimborsoService, InsegnanteService insegnanteService,
                            AnnoScolasticoService annoScolasticoService, SecurityService securityService,
                            BilancioService bilancioService) {
        this.rimborsoService = rimborsoService;
        this.bilancioService = bilancioService;
        this.insegnante = securityService.getUsername().flatMap(insegnanteService::findByUsername).orElse(null);
        addClassNames("size-full");

        filtroAnno = ViewSupport.filtroAnnoScolastico(
            annoScolasticoService.elenco().stream().map(AnnoScolastico::getNome).toList());
        filtroAnno.addValueChangeListener(e -> aggiorna());
        Button lezioniDelMese = new Button("Lezioni del mese", VaadinIcon.CALENDAR_CLOCK.create(), e -> apriLezioniDelMese());
        lezioniDelMese.setEnabled(insegnante != null);
        HorizontalLayout barra = new HorizontalLayout(filtroAnno, totale, lezioniDelMese);
        barra.addClassNames("items-center flex-wrap");

        grid.addColumn(r -> Formati.mese(r.getMese())).setHeader("Mese").setComparator(Rimborso::getMese);
        grid.addColumn(r -> Formati.data(r.getData())).setHeader("Pagato il").setComparator(Rimborso::getData);
        grid.addColumn(r -> Formati.euro(r.getImporto())).setHeader("Importo").setWidth("7.5rem").setFlexGrow(0);
        grid.addColumn(r -> r.getMetodo().getDescrizione()).setHeader("Metodo").setWidth("6.5rem").setFlexGrow(0);
        grid.addColumn(Rimborso::getNota).setHeader("Nota");
        grid.setEmptyStateText("Nessun rimborso in questo anno scolastico");
        CardMobile.per(grid)
            .titolo(r -> Formati.mese(r.getMese()))
            .campo("Pagato il", r -> Formati.data(r.getData()))
            .campo("Importo", r -> Formati.euro(r.getImporto()))
            .campo("Metodo", r -> r.getMetodo().getDescrizione())
            .campo("Nota", Rimborso::getNota)
            .applica();

        add(barra, grid);
        grid.addClassNames("grow");
        aggiorna();
    }

    /**
     * Le lezioni di un mese dell'anno scolastico scelto (di default il mese corrente, se è nell'anno), con ore e
     * preventivo: il riferimento del rimborso.
     */
    private void apriLezioniDelMese() {
        String anno = filtroAnno.getValue();
        if (anno == null) {
            Notification.show("Scegli l'anno scolastico");
            return;
        }
        Dialog dialog = ViewSupport.dialog("Lezioni del mese", "720px");
        YearMonth primo = YearMonth.from(RimborsoService.inizioAnno(anno));
        List<YearMonth> mesi = IntStream.range(0, 12).mapToObj(primo::plusMonths).toList();
        ComboBox<YearMonth> mese = new ComboBox<>("Mese");
        mese.setItems(mesi);
        mese.setItemLabelGenerator(Formati::mese);
        RiepilogoLezioniMese lezioni = new RiepilogoLezioniMese();
        mese.addValueChangeListener(e -> lezioni.mostra(e.getValue() != null
            ? bilancioService.lezioniDelMese(insegnante.getId(), e.getValue()) : null));
        mese.setValue(mesi.contains(YearMonth.now()) ? YearMonth.now() : mesi.getFirst());
        dialog.add(mese, lezioni);
        dialog.getFooter().add(ViewSupport.closeButton(dialog, "Chiudi"));
        dialog.open();
    }

    private void aggiorna() {
        String anno = filtroAnno.getValue();
        if (insegnante == null || anno == null) {
            grid.setItems(List.of());
            totale.setText("");
            return;
        }
        grid.setItems(rimborsoService.rimborsi(insegnante.getId(), anno));
        totale.setText("Totale ricevuto: " + Formati.euro(rimborsoService.totale(insegnante.getId(), anno)));
    }
}
