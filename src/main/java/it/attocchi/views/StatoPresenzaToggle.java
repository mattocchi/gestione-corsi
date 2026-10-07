package it.attocchi.views;

import com.vaadin.flow.component.AbstractCompositeField;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import it.attocchi.entity.Presenza.StatoPresenza;

import java.util.EnumMap;
import java.util.Map;

/**
 * Scelta dello stato di una presenza con un solo tocco: tre pulsanti affiancati,
 * quello selezionato è pieno e colorato. Pensato per l'uso da smartphone al posto
 * di una lista a tendina.
 */
public class StatoPresenzaToggle extends AbstractCompositeField<Div, StatoPresenzaToggle, StatoPresenza> {

    private final Map<StatoPresenza, Button> pulsanti = new EnumMap<>(StatoPresenza.class);

    public StatoPresenzaToggle() {
        super(null);
        Div container = getContent();
        container.addClassName("stato-presenza-toggle");
        container.getElement().setAttribute("role", "radiogroup");
        container.getElement().setAttribute("aria-label", "Stato presenza");

        for (StatoPresenza stato : StatoPresenza.values()) {
            Button pulsante = new Button(etichettaBreve(stato), e -> {
                setPresentationValue(stato);
                setModelValue(stato, e.isFromClient());
            });
            pulsante.getElement().setAttribute("role", "radio");
            pulsante.setAriaLabel(stato.getDescrizione());
            pulsante.setTooltipText(stato.getDescrizione());
            pulsanti.put(stato, pulsante);
            container.add(pulsante);
        }
        setPresentationValue(null);
    }

    /** Pulsante associato a uno stato (usato nei test). */
    Button getPulsante(StatoPresenza stato) {
        return pulsanti.get(stato);
    }

    @Override
    protected void setPresentationValue(StatoPresenza valore) {
        pulsanti.forEach((stato, pulsante) -> {
            boolean selezionato = stato == valore;
            pulsante.getElement().setAttribute("aria-checked", String.valueOf(selezionato));
            pulsante.setThemeVariants(selezionato, ButtonVariant.PRIMARY, variante(stato));
        });
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
