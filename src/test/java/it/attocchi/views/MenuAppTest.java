package it.attocchi.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.server.menu.MenuEntry;
import it.attocchi.views.MenuApp.Gruppo;
import it.attocchi.views.MenuApp.Sezione;
import it.attocchi.views.admin.ImpostazioniView;
import it.attocchi.views.admin.CorsiView;
import it.attocchi.views.admin.EmailInviateView;
import it.attocchi.views.admin.GestionePagamentiView;
import it.attocchi.views.admin.UtentiView;
import it.attocchi.views.genitore.FigliView;
import it.attocchi.views.genitore.PagamentiFigliView;
import it.attocchi.views.insegnante.LezioniInsegnanteView;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MenuAppTest {

    private static MenuEntry voce(String titolo, double ordine, Class<? extends Component> vista) {
        return new MenuEntry("/" + titolo.toLowerCase(), titolo, ordine, null, vista);
    }

    private static final MenuEntry HOME = voce("Home", 0, HomeView.class);
    private static final MenuEntry CALENDARIO = voce("Calendario", 1, CalendarioView.class);
    private static final MenuEntry PROFILO = voce("Profilo", 90, ProfiloView.class);
    private static final MenuEntry DOCUMENTI = voce("Documenti", 42, DocumentiView.class);

    private static List<String> titoli(Sezione sezione) {
        return sezione.voci().stream().map(MenuEntry::title).toList();
    }

    @Test
    void staff_gruppiConTitoloEImpostazioniInFondo() {
        List<Sezione> sezioni = MenuApp.organizza(List.of(HOME, CALENDARIO, voce("Corsi", 10, CorsiView.class),
            voce("Pagamenti", 10.5, GestionePagamentiView.class), voce("Cronologia", 10.8, EmailInviateView.class),
            voce("Utenti", 12, UtentiView.class), voce("Impostazioni", 15, ImpostazioniView.class), DOCUMENTI),
            Set.of("ADMIN"));
        assertEquals(List.of(Gruppo.GENERALE, Gruppo.CORSI, Gruppo.PERSONE, Gruppo.ECONOMIA, Gruppo.DOCUMENTI,
            Gruppo.IMPOSTAZIONI), sezioni.stream().map(Sezione::gruppo).toList());
        assertNull(sezioni.getFirst().titolo());
        assertEquals("Corsi e lezioni", sezioni.get(1).titolo());
        // Documenti insieme alla Cronologia, nell'ordine di @Menu
        assertEquals(List.of("Cronologia", "Documenti"), titoli(sezioni.get(4)));
        assertNull(sezioni.get(5).titolo());
        assertEquals(List.of("Impostazioni"), titoli(sezioni.get(5)));
    }

    @Test
    void unSoloRuolo_listaSempliceConDocumentiDopoLeVociDelRuolo() {
        List<Sezione> sezioni = MenuApp.organizza(List.of(HOME, CALENDARIO,
            voce("Le mie lezioni", 20, LezioniInsegnanteView.class), DOCUMENTI, PROFILO), Set.of("INSEGNANTE"));
        assertEquals(List.of("Home", "Calendario", "Le mie lezioni", "Documenti", "Profilo"),
            sezioni.stream().flatMap(s -> titoli(s).stream()).toList());
        sezioni.forEach(s -> assertNull(s.titolo()));
    }

    @Test
    void piuRuoli_unGruppoConTitoloPerRuoloEDocumentiInAlto() {
        List<Sezione> sezioni = MenuApp.organizza(List.of(HOME, CALENDARIO,
            voce("Le mie lezioni", 20, LezioniInsegnanteView.class), voce("I miei figli", 40, FigliView.class),
            voce("Pagamenti", 41, PagamentiFigliView.class), DOCUMENTI, PROFILO), Set.of("INSEGNANTE", "GENITORE"));
        assertEquals(List.of("Home", "Calendario", "Documenti"), titoli(sezioni.getFirst()));
        assertEquals("Insegnante", sezioni.get(1).titolo());
        assertEquals("Famiglia", sezioni.get(2).titolo());
        assertEquals(List.of("I miei figli", "Pagamenti"), titoli(sezioni.get(2)));
        assertEquals(List.of("Profilo"), titoli(sezioni.getLast()));
    }

    @Test
    void staffEGenitore_gruppiDelloStaffPiuFamiglia() {
        List<Sezione> sezioni = MenuApp.organizza(List.of(HOME, voce("Corsi", 10, CorsiView.class),
            voce("I miei figli", 40, FigliView.class), DOCUMENTI), Set.of("SEGRETERIA", "GENITORE"));
        assertEquals(List.of(Gruppo.GENERALE, Gruppo.CORSI, Gruppo.DOCUMENTI, Gruppo.FAMIGLIA),
            sezioni.stream().map(Sezione::gruppo).toList());
        assertEquals("Famiglia", sezioni.getLast().titolo());
    }
}
