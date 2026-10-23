package it.attocchi.views.insegnante;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import it.attocchi.entity.Corso;
import it.attocchi.entity.Iscrizione;
import it.attocchi.security.SecurityService;
import it.attocchi.service.CorsoService;
import it.attocchi.service.MieiAllieviService;
import it.attocchi.service.MieiAllieviService.AllievoDelCorso;
import it.attocchi.views.CardMobile;
import it.attocchi.views.Formati;
import it.attocchi.views.MainLayout;
import it.attocchi.views.PresenzeAllievoDialog;
import it.attocchi.views.RicercaRapida;
import it.attocchi.views.ViewSupport;
import jakarta.annotation.security.RolesAllowed;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Allievi dei corsi attivi dell'insegnante, in sola lettura: una riga per allievo e corso con le lezioni svolte,
 * da fare e mancanti; cliccando la riga si apre l'elenco delle sue lezioni ({@link PresenzeAllievoDialog}, con le
 * note per lo staff).
 */
@Route(value = "miei-allievi", layout = MainLayout.class)
@PageTitle("I miei allievi")
@Menu(title = "I miei allievi", order = 20.5, icon = "vaadin:academy-cap")
@RolesAllowed("INSEGNANTE")
public class MieiAllieviView extends VerticalLayout {

    private final MieiAllieviService mieiAllieviService;
    private final CorsoService corsoService;
    private final String username;
    private final Grid<AllievoDelCorso> grid = new Grid<>();
    private final ComboBox<Corso> filtroCorso = new ComboBox<>();
    private final RicercaRapida ricerca = new RicercaRapida();
    private List<AllievoDelCorso> tutti = List.of();
    private GridListDataView<AllievoDelCorso> righe;
    private PresenzeAllievoDialog aperto;

    public MieiAllieviView(MieiAllieviService mieiAllieviService, CorsoService corsoService, SecurityService securityService) {
        this.mieiAllieviService = mieiAllieviService;
        this.corsoService = corsoService;
        this.username = securityService.getUsername().orElse(null);
        addClassNames("size-full");

        filtroCorso.setPlaceholder("Tutti i corsi");
        filtroCorso.setAriaLabel("Filtra per corso");
        filtroCorso.setClearButtonVisible(true);
        filtroCorso.setItemLabelGenerator(Corso::getNome);
        filtroCorso.addClassNames("w-60");
        filtroCorso.addValueChangeListener(e -> applicaFiltri());
        ricerca.addRicercaListener(this::applicaFiltri);
        HorizontalLayout barra = new HorizontalLayout(filtroCorso, ricerca);
        barra.addClassNames("flex-wrap w-full items-center");

        grid.addColumn(a -> allievo(a).getAllievo().getNomeCompleto()).setHeader("Allievo")
            .setComparator(a -> allievo(a).getAllievo().getNomeCompleto());
        grid.addColumn(a -> allievo(a).getCorso().getNome()).setHeader("Corso")
            .setComparator(a -> allievo(a).getCorso().getNome());
        grid.addColumn(MieiAllieviView::giorniOGruppo).setHeader("Giorni / Gruppo").setAutoWidth(true);
        grid.addColumn(a -> Formati.ora(allievo(a).getOrarioLezione())).setHeader("Orario")
            .setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(MieiAllieviView::svolte).setHeader("Svolte").setAutoWidth(true).setFlexGrow(0)
            .setComparator(a -> a.conteggio().svolte());
        grid.addColumn(a -> a.conteggio().daFare()).setHeader("Da fare").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(a -> a.conteggio().mancanti()).setHeader("Mancanti").setAutoWidth(true).setFlexGrow(0);
        ViewSupport.configureActionsColumn(grid.addComponentColumn(this::createAzioni));
        grid.setEmptyStateText("Nessun allievo nei tuoi corsi attivi");
        grid.addItemClickListener(e -> apriLezioni(e.getItem()));
        CardMobile.per(grid)
            .titolo(a -> allievo(a).getAllievo().getNomeCompleto())
            .sottotitolo(a -> allievo(a).getCorso().getNome())
            .campo("Giorni / Gruppo", MieiAllieviView::giorniOGruppo)
            .campo("Orario", a -> Formati.ora(allievo(a).getOrarioLezione()))
            .campo("Svolte", MieiAllieviView::svolte)
            .campo("Da fare", a -> a.conteggio().daFare())
            .campo("Mancanti", a -> a.conteggio().mancanti())
            .azioni(this::createAzioni)
            .applica();

        add(barra, grid);
        grid.addClassNames("grow");
        updateGrid();
    }

    private static Iscrizione allievo(AllievoDelCorso riga) {
        return riga.iscrizione();
    }

    private static String giorniOGruppo(AllievoDelCorso riga) {
        return Formati.gruppoOGiorni(riga.iscrizione().getCorso(), riga.iscrizione().getGruppo());
    }

    private static String svolte(AllievoDelCorso riga) {
        return riga.conteggio().svolte() + " su " + riga.conteggio().previste();
    }

    private HorizontalLayout createAzioni(AllievoDelCorso riga) {
        return ViewSupport.actions(ViewSupport.iconButton(VaadinIcon.BOOK, "Lezioni dell'allievo", e -> apriLezioni(riga)));
    }

    /** Dal clic sulla riga o dal pulsante: il clic sul pulsante arriva anche alla riga, il dialog si apre una volta. */
    private void apriLezioni(AllievoDelCorso riga) {
        if (aperto != null && aperto.isOpened()) {
            return;
        }
        Corso corso = riga.iscrizione().getCorso();
        Long allievoId = riga.iscrizione().getAllievo().getId();
        aperto = new PresenzeAllievoDialog(riga.iscrizione().getAllievo().getNomeCompleto() + " – " + corso.getNome(),
            mieiAllieviService.lezioniDellAllievo(corso.getId(), allievoId, username), corsoService.gruppi(corso.getId()),
            corso.getNumeroLezioni(), true);
        aperto.open();
    }

    private void updateGrid() {
        tutti = username != null ? mieiAllieviService.allieviDellInsegnante(username) : List.of();
        // Un corso per voce, per nome
        Map<Long, Corso> corsi = tutti.stream().map(a -> a.iscrizione().getCorso())
            .collect(Collectors.toMap(Corso::getId, Function.identity(), (a, b) -> a));
        filtroCorso.setItems(corsi.values().stream()
            .sorted(Comparator.comparing(Corso::getNome, String.CASE_INSENSITIVE_ORDER)).toList());
        // Con un solo corso il filtro non serve
        filtroCorso.setVisible(corsi.size() > 1);
        righe = grid.setItems(tutti);
        applicaFiltri();
    }

    /** Corso scelto e ricerca rapida (nome dell'allievo, corso, gruppo); "N di M" sugli allievi del corso scelto. */
    private void applicaFiltri() {
        if (righe == null) {
            return;
        }
        Corso scelto = filtroCorso.getValue();
        SerializablePredicate<AllievoDelCorso> delCorso = a -> scelto == null
            || a.iscrizione().getCorso().getId().equals(scelto.getId());
        righe.setFilter(a -> delCorso.test(a) && ricerca.corrisponde(RicercaRapida.testo(
            a.iscrizione().getAllievo().getNomeCompleto(), a.iscrizione().getCorso().getNome(), giorniOGruppo(a))));
        ricerca.mostraConteggio(righe.getItemCount(), (int) tutti.stream().filter(delCorso).count());
    }
}
