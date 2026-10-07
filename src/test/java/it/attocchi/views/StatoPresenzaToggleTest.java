package it.attocchi.views;

import com.vaadin.flow.component.button.ButtonVariant;
import it.attocchi.entity.Presenza.StatoPresenza;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatoPresenzaToggleTest {

    @Test
    void setValue_evidenziaSoloLoStatoSelezionato() {
        StatoPresenzaToggle toggle = new StatoPresenzaToggle();
        toggle.setValue(StatoPresenza.PRESENTE);

        assertEquals("true", ariaChecked(toggle, StatoPresenza.PRESENTE));
        assertEquals("false", ariaChecked(toggle, StatoPresenza.ASSENTE_GIUSTIFICATA));
        assertTrue(toggle.getPulsante(StatoPresenza.PRESENTE).getThemeNames().contains(ButtonVariant.SUCCESS.getVariantName()));
        assertFalse(toggle.getPulsante(StatoPresenza.ASSENTE_GIUSTIFICATA).getThemeNames().contains(ButtonVariant.PRIMARY.getVariantName()));
    }

    @Test
    void click_cambiaValoreEGeneraEvento() {
        StatoPresenzaToggle toggle = new StatoPresenzaToggle();
        toggle.setValue(StatoPresenza.PRESENTE);
        List<StatoPresenza> eventi = new ArrayList<>();
        toggle.addValueChangeListener(e -> eventi.add(e.getValue()));

        toggle.getPulsante(StatoPresenza.ASSENTE_INGIUSTIFICATA).click();

        assertEquals(StatoPresenza.ASSENTE_INGIUSTIFICATA, toggle.getValue());
        assertEquals(List.of(StatoPresenza.ASSENTE_INGIUSTIFICATA), eventi);
        assertEquals("true", ariaChecked(toggle, StatoPresenza.ASSENTE_INGIUSTIFICATA));
        assertEquals("false", ariaChecked(toggle, StatoPresenza.PRESENTE));
    }

    @Test
    void click_sulloStatoGiaSelezionato_nessunEvento() {
        StatoPresenzaToggle toggle = new StatoPresenzaToggle();
        toggle.setValue(StatoPresenza.PRESENTE);
        List<StatoPresenza> eventi = new ArrayList<>();
        toggle.addValueChangeListener(e -> eventi.add(e.getValue()));

        toggle.getPulsante(StatoPresenza.PRESENTE).click();

        assertTrue(eventi.isEmpty());
    }

    private static String ariaChecked(StatoPresenzaToggle toggle, StatoPresenza stato) {
        return toggle.getPulsante(stato).getElement().getAttribute("aria-checked");
    }
}
