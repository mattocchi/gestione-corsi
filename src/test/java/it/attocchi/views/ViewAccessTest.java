package it.attocchi.views;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.RouteNotFoundError;
import it.attocchi.entity.Docente;
import it.attocchi.entity.Lezione;
import it.attocchi.entity.Presenza;
import it.attocchi.entity.Presenza.StatoPresenza;
import it.attocchi.service.DocenteService;
import it.attocchi.service.LezioneService;
import it.attocchi.service.PresenzaService;
import it.attocchi.views.admin.CorsiView;
import it.attocchi.views.admin.StudentiView;
import it.attocchi.views.admin.UtentiView;
import it.attocchi.views.docente.LezioniDocenteView;
import it.attocchi.views.docente.LezioniDocenteView.FiltroLezioni;
import it.attocchi.views.genitore.FigliView;
import it.attocchi.views.studente.MieiCorsiView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithUserDetails;

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
    private DocenteService docenteService;

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
        assertTrue(voci.containsAll(List.of("Home", "Gestione Corsi", "Gestione Studenti", "Gestione Utenti")), voci.toString());
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

        // Apre il dialog di modifica: i giorni della settimana devono essere già caricati
        Button modifica = (Button) grid.getCellComponent(0, 6).getChildren().findFirst().orElseThrow();
        assertEquals("Modifica", modifica.getElement().getAttribute("aria-label"));
        test(modifica).click();
        CheckboxGroup<?> giorni = find(CheckboxGroup.class).single();
        assertFalse(giorni.getValue().isEmpty());
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

        assertEquals(25, test(find(Grid.class).single()).size());
    }

    @Test
    @WithUserDetails("docente1")
    void docente_vedeLeProprieLezioni() {
        navigate(LezioniDocenteView.class);

        var grid = test(find(Grid.class).single());
        assertEquals(30, grid.size());
        assertEquals("Corso di Canto", grid.getCellText(0, 0));
    }

    @Test
    @WithUserDetails("docente1")
    void docente_segnaAssenzaGiustificataConUnTocco() {
        navigate(LezioniDocenteView.class);
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
    @WithUserDetails("docente1")
    @SuppressWarnings("unchecked")
    void docente_filtraLezioniPerStato() {
        Docente docente = docenteService.findByUsername("docente1").orElseThrow();
        Lezione completata = lezioneService.findByDocente(docente.getId()).getFirst();
        completata.setCompletata(true);
        lezioneService.save(completata);
        try {
            navigate(LezioniDocenteView.class);
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
    @WithUserDetails("docente1")
    void docente_nonAccedeAViewAdmin() {
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
