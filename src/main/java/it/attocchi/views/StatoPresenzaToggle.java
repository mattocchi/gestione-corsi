package it.attocchi.views;

import com.vaadin.flow.component.button.ButtonVariant;
import it.attocchi.entity.Presenza.StatoPresenza;

import java.util.List;

/**
 * Scelta dello stato di una presenza con un solo tocco: il pulsante selezionato è pieno e
 * colorato (verde Presente, giallo Giustificata, rosso Ingiustificata).
 */
public class StatoPresenzaToggle extends SegmentedControl<StatoPresenza> {

    public StatoPresenzaToggle() {
        super("Stato presenza", List.of(StatoPresenza.values()),
            StatoPresenzaToggle::etichettaBreve, StatoPresenza::getDescrizione, StatoPresenzaToggle::variante);
    }

    private static String etichettaBreve(StatoPresenza stato) {
        return switch (stato) {
            case PRESENTE -> "Presente";
            case ASSENTE_GIUSTIFICATA -> "Giustif.";
            case ASSENTE_INGIUSTIFICATA -> "Ingiust.";
        };
    }

    private static ButtonVariant variante(StatoPresenza stato) {
        return switch (stato) {
            case PRESENTE -> ButtonVariant.SUCCESS;
            case ASSENTE_GIUSTIFICATA -> ButtonVariant.WARNING;
            case ASSENTE_INGIUSTIFICATA -> ButtonVariant.ERROR;
        };
    }
}
