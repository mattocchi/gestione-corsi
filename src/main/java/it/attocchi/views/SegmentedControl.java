package it.attocchi.views;

import com.vaadin.flow.component.AbstractCompositeField;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.function.SerializableFunction;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Scelta singola tra poche opzioni con un solo tocco: pulsanti affiancati della stessa
 * larghezza, quello selezionato è pieno (ed eventualmente colorato). Pensato per l'uso da
 * smartphone al posto di liste a tendina; semantica accessibile radiogroup/radio.
 *
 * @param <T> tipo delle opzioni
 */
public class SegmentedControl<T> extends AbstractCompositeField<Div, SegmentedControl<T>, T> {

    private final Map<T, Button> pulsanti = new LinkedHashMap<>();
    private final SerializableFunction<T, ButtonVariant> varianteSelezionata;

    /**
     * @param ariaLabel           nome accessibile del gruppo
     * @param opzioni             opzioni nell'ordine di visualizzazione
     * @param etichetta           testo breve mostrato sul pulsante
     * @param descrizione         testo completo per tooltip e lettori di schermo
     * @param varianteSelezionata colore del pulsante selezionato (oltre a PRIMARY), o {@code null}
     */
    public SegmentedControl(String ariaLabel, Collection<T> opzioni,
                            SerializableFunction<T, String> etichetta,
                            SerializableFunction<T, String> descrizione,
                            SerializableFunction<T, ButtonVariant> varianteSelezionata) {
        super(null);
        this.varianteSelezionata = varianteSelezionata;

        Div container = getContent();
        // Pulsanti della stessa larghezza su tutta la riga, scelta con un tocco
        container.addClassNames("flex gap-1 w-full");
        container.getElement().setAttribute("role", "radiogroup");
        container.getElement().setAttribute("aria-label", ariaLabel);

        for (T opzione : opzioni) {
            Button pulsante = new Button(etichetta.apply(opzione), e -> {
                setPresentationValue(opzione);
                setModelValue(opzione, e.isFromClient());
            });
            pulsante.addClassNames("flex-1 min-w-0 m-0");
            pulsante.getElement().setAttribute("role", "radio");
            pulsante.setAriaLabel(descrizione.apply(opzione));
            pulsante.setTooltipText(descrizione.apply(opzione));
            pulsanti.put(opzione, pulsante);
            container.add(pulsante);
        }
        setPresentationValue(null);
    }

    /** Attiva o disattiva una singola opzione; {@code motivo} (o {@code null}) diventa il tooltip quando è disattivata. */
    public void setOpzioneAttiva(T opzione, boolean attiva, String motivo) {
        Button pulsante = pulsanti.get(opzione);
        pulsante.setEnabled(attiva);
        if (!attiva && motivo != null) {
            pulsante.setTooltipText(motivo);
        }
    }

    /** Pulsante associato a un'opzione (usato nei test). */
    Button getPulsante(T opzione) {
        return pulsanti.get(opzione);
    }

    @Override
    protected void setPresentationValue(T valore) {
        pulsanti.forEach((opzione, pulsante) -> {
            boolean selezionato = opzione.equals(valore);
            pulsante.getElement().setAttribute("aria-checked", String.valueOf(selezionato));
            pulsante.setThemeVariant(ButtonVariant.PRIMARY, selezionato);
            if (varianteSelezionata != null && varianteSelezionata.apply(opzione) != null) {
                pulsante.setThemeVariant(varianteSelezionata.apply(opzione), selezionato);
            }
        });
    }
}
