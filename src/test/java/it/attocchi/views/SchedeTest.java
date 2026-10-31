package it.attocchi.views;

import com.vaadin.flow.component.tabs.Tabs;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SchedeTest {

    private static Schede<String> schede() {
        return new Schede<>("Stato", List.of("Attivi", "Non attivi", "Tutti"), s -> s, s -> "Mostra: " + s);
    }

    private static Tabs tabs(Schede<String> schede) {
        return (Tabs) schede.getScheda("Attivi").getParent().orElseThrow();
    }

    @Test
    void allInizio_primaSchedaSelezionata() {
        Schede<String> schede = schede();
        assertEquals("Attivi", schede.getValue());
        assertEquals(schede.getScheda("Attivi"), tabs(schede).getSelectedTab());
        assertEquals("Stato", tabs(schede).getElement().getAttribute("aria-label"));
        assertEquals("Mostra: Tutti", schede.getScheda("Tutti").getTooltip().getText());
    }

    @Test
    void sceltaDellaScheda_cambiaValoreEGeneraEvento() {
        Schede<String> schede = schede();
        List<String> eventi = new ArrayList<>();
        schede.addValueChangeListener(e -> eventi.add(e.getValue()));

        tabs(schede).setSelectedTab(schede.getScheda("Tutti"));

        assertEquals("Tutti", schede.getValue());
        assertEquals(List.of("Tutti"), eventi);
    }

    @Test
    void setValue_selezionaLaSchedaConUnSoloEvento() {
        Schede<String> schede = schede();
        List<String> eventi = new ArrayList<>();
        schede.addValueChangeListener(e -> eventi.add(e.getValue()));

        schede.setValue("Non attivi");

        assertEquals(schede.getScheda("Non attivi"), tabs(schede).getSelectedTab());
        assertEquals(List.of("Non attivi"), eventi);
        assertEquals(1, tabs(schede).getSelectedIndex());
    }
}
