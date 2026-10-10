package it.attocchi.views;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridTester;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.router.RouteNotFoundError;
import it.attocchi.entity.Corso;
import it.attocchi.entity.Genitore;
import it.attocchi.entity.Insegnante;
import it.attocchi.entity.Iscrizione;
import it.attocchi.entity.Lezione;
import it.attocchi.entity.Presenza;
import it.attocchi.entity.Presenza.StatoPresenza;
import it.attocchi.service.InsegnanteService;
import it.attocchi.service.CorsoService;
import it.attocchi.service.LezioneService;
import it.attocchi.service.PresenzaService;
import it.attocchi.views.admin.CorsiView;
import it.attocchi.views.admin.GenitoriView;
import it.attocchi.views.admin.InsegnantiView;
import it.attocchi.views.admin.PartecipantiDialog;
import it.attocchi.views.admin.StudentiView;
import it.attocchi.views.admin.UtentiView;
import it.attocchi.views.insegnante.LezioniInsegnanteView;
import it.attocchi.views.insegnante.LezioniInsegnanteView.FiltroLezioni;
import it.attocchi.views.genitore.FigliView;
import it.attocchi.views.studente.MieiCorsiView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithUserDetails;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test browserless delle view per ruolo, con gli utenti reali creati da DataInitializer.
 * Leggere le celle delle griglie fuori da una transazione verifica anche che i dati
 * mostrati siano caricati dai service (open-in-view è disattivato).
 */
@SpringBootTest
class ViewAccessTest extends SpringBrowserlessTest {

    @Autowired
    private PresenzaService presenzaService;

    @Autowired
    private LezioneService lezioneService;

    @Autowired
    private InsegnanteService insegnanteService;

    @Autowired
    private CorsoService corsoService;

    @Test
    @WithAnonymousUser
    void anonimo_vieneRediretto_alLogin() {
        navigate("", LoginView.class);
    }

    @Test
    @WithUserDetails("admin")
    void admin_menuMostraSoloVociAdmin() {
        navigate(HomeView.class);

        List<String> voci = find(SideNavItem.class).all().stream().map(SideNavItem::getLabel).toList();
        assertTrue(voci.containsAll(List.of("Home", "Gestione Corsi", "Gestione Studenti", "Gestione Utenti", "Insegnanti", "Genitori")), voci.toString());
        assertFalse(voci.contains("Le Mie Lezioni"), voci.toString());
    }

    @Test
    @WithUserDetails("admin")
    void admin_corsi_grigliaEDialogModifica() {
        navigate(CorsiView.class);

        var grid = test(find(Grid.class).single());
        assertEquals(2, grid.size());
        for (int row = 0; row < grid.size(); row++) {
            for (int col = 0; col < 6; col++) {
                grid.getCellText(row, col);
            }
        }

        assertAzioni(grid.getCellComponent(0, 6), "Modifica", "Partecipanti", "Genera Lezioni", "Elimina");

        // Apre il dialog di modifica: i giorni della settimana devono essere già caricati
        Button modifica = (Button) grid.getCellComponent(0, 6).getChildren().findFirst().orElseThrow();
        assertEquals("Modifica", modifica.getElement().getAttribute("aria-label"));
        test(modifica).click();
        CheckboxGroup<?> giorni = find(CheckboxGroup.class).single();
        assertFalse(giorni.getValue().isEmpty());
    }

    @Test
    @WithUserDetails("admin")
    @SuppressWarnings("unchecked")
    void admin_corsi_partecipantiInOrdineDiOrario() {
        navigate(CorsiView.class);
        var grid = test((Grid<Corso>) find(Grid.class).single());
        int canto = riga(grid, "Corso di Canto");

        Button partecipanti = pulsante(grid.getCellComponent(canto, 6), "Partecipanti");
        test(partecipanti).click();

        PartecipantiDialog dialog = find(PartecipantiDialog.class).single();
        var righe = test((Grid<Iscrizione>) find(Grid.class).from(dialog).single());
        assertEquals(List.of("17:00 Studente Canto5", "17:40 Studente Canto4", "18:20 Studente Canto3",
            "19:00 Studente Canto2", "19:40 Studente Canto1",
            "- Studente Canto6", "- Studente Canto7"), ordinePartecipanti(righe));
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_assegnaOrarioLezioneAiPartecipanti() {
        navigate(CorsiView.class);
        var corsi = test((Grid<Corso>) find(Grid.class).single());
        int canto = riga(corsi, "Corso di Canto");
        test(pulsante(corsi.getCellComponent(canto, 6), "Partecipanti")).click();
        PartecipantiDialog dialog = find(PartecipantiDialog.class).single();
        var righe = test((Grid<Iscrizione>) find(Grid.class).from(dialog).single());

        // Canto6 (senza orario, penultimo) alle 16:30: salvato e spostato in cima
        assertEquals("Studente Canto6", righe.getCellText(5, 1));
        test((TimePicker) righe.getCellComponent(5, 0)).setValue(LocalTime.of(16, 30));
        assertEquals("16:30 Studente Canto6", ordinePartecipanti(righe).getFirst());
        assertEquals(LocalTime.of(16, 30), corsoService.findPartecipanti(corsi.getRow(canto).getId()).getFirst().getOrarioLezione());

        // Tolto l'orario torna in fondo (ripristina i dati di test)
        test((TimePicker) righe.getCellComponent(0, 0)).clear();
        assertEquals("- Studente Canto6", ordinePartecipanti(righe).get(5));
    }

    private static List<String> ordinePartecipanti(GridTester<Grid<Iscrizione>, Iscrizione> righe) {
        List<String> ordine = new ArrayList<>();
        for (int row = 0; row < righe.size(); row++) {
            LocalTime orario = ((TimePicker) righe.getCellComponent(row, 0)).getValue();
            ordine.add((orario != null ? orario.toString() : "-") + " " + righe.getCellText(row, 1));
        }
        return ordine;
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_aggiungeERimuovePartecipanti() {
        navigate(CorsiView.class);
        var corsi = test((Grid<Corso>) find(Grid.class).single());
        int chitarra = riga(corsi, "Corso di Chitarra");
        Corso corso = corsi.getRow(chitarra);
        test(pulsante(corsi.getCellComponent(chitarra, 6), "Partecipanti")).click();

        PartecipantiDialog dialog = find(PartecipantiDialog.class).single();
        var partecipanti = test((Grid<Iscrizione>) find(Grid.class).from(dialog).single());
        assertEquals(6, partecipanti.size());

        // Aggiunta: compare nell'elenco e ha le presenze in tutte le lezioni (nessuna completata)
        ComboBox<?> nuovo = find(ComboBox.class).from(dialog).single();
        test(nuovo).selectItem("Studente Canto1");
        test(find(Button.class).from(dialog).withText("Aggiungi").single()).click();
        assertEquals(7, partecipanti.size());
        Long studenteId = corsoService.findPartecipanti(corso.getId()).stream()
            .filter(i -> i.getStudente().getNomeCompleto().equals("Studente Canto1"))
            .findFirst().orElseThrow().getStudente().getId();
        assertEquals(30, presenzaService.findByCorsoAndStudente(corso.getId(), studenteId).size());

        // Rimozione: sparisce dall'elenco con le presenze delle lezioni da fare
        int riga = -1;
        for (int row = 0; row < partecipanti.size(); row++) {
            if (partecipanti.getCellText(row, 1).equals("Studente Canto1")) {
                riga = row;
            }
        }
        test((Button) partecipanti.getCellComponent(riga, 2).getChildren().findFirst().orElseThrow()).click();
        assertEquals(6, partecipanti.size());
        assertTrue(presenzaService.findByCorsoAndStudente(corso.getId(), studenteId).isEmpty());
    }

    private static int riga(GridTester<Grid<Corso>, Corso> grid, String nome) {
        for (int row = 0; row < grid.size(); row++) {
            if (grid.getRow(row).getNome().equals(nome)) {
                return row;
            }
        }
        throw new AssertionError("Corso non trovato: " + nome);
    }

    private static Button pulsante(Component azioni, String etichetta) {
        return azioni.getChildren()
            .filter(c -> c instanceof Button b && etichetta.equals(b.getElement().getAttribute("aria-label")))
            .map(Button.class::cast)
            .findFirst()
            .orElseThrow(() -> new AssertionError("Pulsante non trovato: " + etichetta));
    }

    @Test
    @WithUserDetails("admin")
    void admin_studenti_grigliaConNumeroCorsi() {
        navigate(StudentiView.class);

        var grid = test(find(Grid.class).single());
        assertEquals(13, grid.size());
        for (int row = 0; row < grid.size(); row++) {
            assertEquals("1", grid.getCellText(row, 4));
            grid.getCellText(row, 3);
        }
    }

    @Test
    @WithUserDetails("admin")
    void admin_utenti_griglia() {
        navigate(UtentiView.class);

        assertEquals(26, test(find(Grid.class).single()).size());
    }

    @Test
    @WithUserDetails("segreteria")
    void segreteria_accedeEdNonVedeGestioneUtenti() {
        navigate(HomeView.class);

        List<String> voci = find(SideNavItem.class).all().stream().map(SideNavItem::getLabel).toList();
        assertTrue(voci.contains("Home"), voci.toString());
        assertFalse(voci.contains("Gestione Utenti"), voci.toString());
        navigate("utenti", RouteNotFoundError.class);
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_vedeCorsiEStudentiSenzaElimina() {
        navigate(CorsiView.class);
        var corsi = test((Grid<Corso>) find(Grid.class).single());
        assertEquals(2, corsi.size());
        assertAzioni(corsi.getCellComponent(0, 6), "Modifica", "Partecipanti", "Genera Lezioni");

        navigate(StudentiView.class);
        var studenti = test(find(Grid.class).single());
        assertEquals(13, studenti.size());
        assertAzioni(studenti.getCellComponent(0, 5), "Modifica");
    }

    @Test
    @WithUserDetails("segreteria")
    void segreteria_vedeGliInsegnanti() {
        navigate(InsegnantiView.class);

        var grid = test(find(Grid.class).single());
        assertEquals(2, grid.size());
        for (int row = 0; row < grid.size(); row++) {
            assertEquals("1", grid.getCellText(row, 3));
        }
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_vedeIGenitori() {
        navigate(GenitoriView.class);

        var grid = test((Grid<Genitore>) find(Grid.class).single());
        assertEquals(9, grid.size());
        int genitore3 = -1;
        for (int row = 0; row < grid.size(); row++) {
            grid.getCellText(row, 3);
            if (grid.getRow(row).getUser().getUsername().equals("genitore3")) {
                genitore3 = row;
            }
        }
        assertEquals("Studente Canto3, Studente Chitarra1", grid.getCellText(genitore3, 3));
        assertEquals("2", grid.getCellText(genitore3, 4));
    }

    private static void assertAzioni(Component azioni, String... etichette) {
        assertEquals(List.of(etichette), azioni.getChildren()
            .map(c -> c.getElement().getAttribute("aria-label")).toList());
    }

    @Test
    @WithUserDetails("insegnante1")
    void insegnante_vedeLeProprieLezioni() {
        navigate(LezioniInsegnanteView.class);

        var grid = test(find(Grid.class).single());
        assertEquals(30, grid.size());
        assertEquals("Corso di Canto", grid.getCellText(0, 0));
    }

    @Test
    @WithUserDetails("insegnante1")
    void insegnante_segnaAssenzaGiustificataConUnTocco() {
        navigate(LezioniInsegnanteView.class);
        var lezioni = test(find(Grid.class).single());
        Button gestisciPresenze = (Button) lezioni.getCellComponent(0, 5).getChildren().findFirst().orElseThrow();
        test(gestisciPresenze).click();

        @SuppressWarnings("unchecked")
        Grid<Presenza> gridPresenze = find(Grid.class).all().getLast();
        var presenze = test(gridPresenze);
        Presenza presenza = presenze.getRow(0);
        StatoPresenzaToggle toggle = (StatoPresenzaToggle) presenze.getCellComponent(0, 0).getChildren()
            .filter(StatoPresenzaToggle.class::isInstance).findFirst().orElseThrow();
        assertEquals(StatoPresenza.PRESENTE, toggle.getValue());

        test(toggle.getPulsante(StatoPresenza.ASSENTE_GIUSTIFICATA)).click();

        assertEquals(StatoPresenza.ASSENTE_GIUSTIFICATA, presenzaService.findById(presenza.getId()).getStato());

        // ripristina il dato di test per gli altri test
        test(toggle.getPulsante(StatoPresenza.PRESENTE)).click();
        assertEquals(StatoPresenza.PRESENTE, presenzaService.findById(presenza.getId()).getStato());
    }

    @Test
    @WithUserDetails("insegnante1")
    @SuppressWarnings("unchecked")
    void insegnante_filtraLezioniPerStato() {
        Insegnante insegnante = insegnanteService.findByUsername("insegnante1").orElseThrow();
        Lezione completata = lezioneService.findByInsegnante(insegnante.getId()).getFirst();
        completata.setCompletata(true);
        lezioneService.save(completata);
        try {
            navigate(LezioniInsegnanteView.class);
            var grid = test(find(Grid.class).single());
            SegmentedControl<FiltroLezioni> filtro = find(SegmentedControl.class).single();

            // default: solo le lezioni da fare
            assertEquals(FiltroLezioni.DA_FARE, filtro.getValue());
            assertEquals(29, grid.size());

            test(filtro.getPulsante(FiltroLezioni.COMPLETATE)).click();
            assertEquals(1, grid.size());
            Badge badge = (Badge) grid.getCellComponent(0, 4);
            assertEquals("Completata", badge.getText());
            assertTrue(badge.getThemeNames().contains(BadgeVariant.SUCCESS.getVariantName()));

            test(filtro.getPulsante(FiltroLezioni.TUTTE)).click();
            assertEquals(30, grid.size());

            test(filtro.getPulsante(FiltroLezioni.DA_FARE)).click();
            assertEquals(29, grid.size());
            assertEquals("Da fare", ((Badge) grid.getCellComponent(0, 4)).getText());
        } finally {
            completata.setCompletata(false);
            lezioneService.save(completata);
        }
    }

    @Test
    @WithUserDetails("insegnante1")
    void insegnante_nonAccedeAViewAdmin() {
        navigate("corsi", RouteNotFoundError.class);
    }

    @Test
    @WithUserDetails("genitore3")
    void genitore_vedeIFigliConNumeroCorsi() {
        navigate(FigliView.class);

        var grid = test(find(Grid.class).single());
        assertEquals(2, grid.size());
        assertEquals("1", grid.getCellText(0, 2));
        assertEquals("1", grid.getCellText(1, 2));
    }

    @Test
    @WithUserDetails("studente_canto1")
    void studente_vedeIPropriCorsi() {
        assertInstanceOf(MieiCorsiView.class, navigate(MieiCorsiView.class));

        var grid = test(find(Grid.class).single());
        assertEquals(1, grid.size());
        assertEquals("Corso di Canto", grid.getCellText(0, 0));
    }
}
