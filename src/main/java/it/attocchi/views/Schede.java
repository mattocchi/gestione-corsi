package it.attocchi.views;

import com.vaadin.flow.component.AbstractCompositeField;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.function.SerializableFunction;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Filtro principale di una vista come fila di schede ({@link Tabs}): un'opzione per scheda con il nome intero, su
 * smartphone le schede scorrono di lato. Ha un valore come un campo, come {@link SegmentedControl}, che resta per le
 * scelte brevi nei dialog.
 *
 * @param <T> tipo delle opzioni
 */
public class Schede<T> extends AbstractCompositeField<Tabs, Schede<T>, T> {

    private final Map<T, Tab> schede = new LinkedHashMap<>();
    private boolean aggiornando;

    /**
     * @param ariaLabel   nome accessibile delle schede
     * @param opzioni     opzioni nell'ordine di visualizzazione; la prima è selezionata all'inizio
     * @param etichetta   testo della scheda
     * @param descrizione tooltip, o {@code null}
     */
    public Schede(String ariaLabel, Collection<T> opzioni, SerializableFunction<T, String> etichetta,
                  SerializableFunction<T, String> descrizione) {
        // Nessun valore "vuoto": c'è sempre una scheda selezionata (la prima, selezionata da Tabs con autoselect)
        super(null);
        Tabs tabs = getContent();
        tabs.getElement().setAttribute("aria-label", ariaLabel);
        tabs.addClassNames("w-full");
        for (T opzione : opzioni) {
            Tab scheda = new Tab(etichetta.apply(opzione));
            if (descrizione != null) {
                scheda.setTooltipText(descrizione.apply(opzione));
            }
            schede.put(opzione, scheda);
            tabs.add(scheda);
        }
        tabs.addSelectedChangeListener(e -> {
            if (!aggiornando) {
                setModelValue(opzione(tabs.getSelectedTab()), e.isFromClient());
            }
        });
        setModelValue(opzioni.iterator().next(), false);
    }

    /** Scheda di un'opzione (usata nei test). */
    public Tab getScheda(T opzione) {
        return schede.get(opzione);
    }

    private T opzione(Tab scheda) {
        return schede.entrySet().stream().filter(e -> e.getValue() == scheda).map(Map.Entry::getKey)
            .findFirst().orElse(null);
    }

    @Override
    protected void setPresentationValue(T valore) {
        Tab scheda = schede.get(valore);
        if (scheda != null && !Objects.equals(getContent().getSelectedTab(), scheda)) {
            aggiornando = true;
            try {
                getContent().setSelectedTab(scheda);
            } finally {
                aggiornando = false;
            }
        }
    }
}
