package it.attocchi.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.card.CardVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.signals.Signal;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Righe di una {@link Grid} mostrate come {@link Card} su mobile. La Grid di Vaadin non ha una modalità card nativa:
 * questa classe aggiunge una colonna che rende ogni riga come Card (titolo, stato, campi "Etichetta: valore",
 * azioni nel footer) e, con {@link ViewSupport#isMobile()}, mostra su mobile solo questa colonna e su desktop solo
 * le altre. Dati, filtri e refresh restano quelli della Grid.
 *
 * <pre>{@code
 * CardMobile.per(grid)
 *     .titolo(Corso::getNome)
 *     .stato(this::createStatoBadge)
 *     .campo("Anno", Corso::getAnnoScolastico)
 *     .azioni(this::createActions)
 *     .applica();
 * }</pre>
 *
 * {@link #applica()} va chiamato dopo aver aggiunto tutte le colonne: la visibilità che hanno in quel momento è
 * quella usata su desktop; per cambiarla in seguito usare {@link #setVisibileSuDesktop}.
 */
public class CardMobile<T> implements Serializable {

    private record Campo<T>(String etichetta, SerializableFunction<T, ?> valore) implements Serializable {
    }

    private final Grid<T> grid;
    private final List<Campo<T>> campi = new ArrayList<>();
    private final Map<Grid.Column<T>, Boolean> visibileSuDesktop = new HashMap<>();
    private SerializableFunction<T, String> titolo;
    private SerializableFunction<T, String> sottotitolo;
    private SerializableFunction<T, ? extends Component> stato;
    private SerializableFunction<T, ? extends Component> azioni;
    private Grid.Column<T> colonna;
    private Signal<Boolean> mobile;

    private CardMobile(Grid<T> grid) {
        this.grid = grid;
    }

    public static <T> CardMobile<T> per(Grid<T> grid) {
        return new CardMobile<>(grid);
    }

    /** Titolo della card: il dato principale della riga. */
    public CardMobile<T> titolo(SerializableFunction<T, String> titolo) {
        this.titolo = titolo;
        return this;
    }

    /** Riga secondaria sotto il titolo (vuota = assente). */
    public CardMobile<T> sottotitolo(SerializableFunction<T, String> sottotitolo) {
        this.sottotitolo = sottotitolo;
        return this;
    }

    /** Componente in alto a destra, di solito il badge di stato. */
    public CardMobile<T> stato(SerializableFunction<T, ? extends Component> stato) {
        this.stato = stato;
        return this;
    }

    /**
     * Campo "Etichetta: valore". Il valore può essere un testo, un numero o un componente (badge, link, toggle);
     * {@code null} o testo vuoto non mostrano il campo.
     */
    public CardMobile<T> campo(String etichetta, SerializableFunction<T, ?> valore) {
        campi.add(new Campo<>(etichetta, valore));
        return this;
    }

    /** Azioni nel footer della card, di solito gli stessi pulsanti della colonna "Azioni". */
    public CardMobile<T> azioni(SerializableFunction<T, ? extends Component> azioni) {
        this.azioni = azioni;
        return this;
    }

    /** Aggiunge la colonna card e collega la visibilità delle colonne al formato mobile. */
    public Grid.Column<T> applica() {
        grid.getColumns().forEach(c -> visibileSuDesktop.put(c, c.isVisible()));
        mobile = ViewSupport.isMobile();
        colonna = grid.addComponentColumn(this::creaCard);
        grid.bindClassName("grid-card-mobile", mobile);
        grid.bindThemeName(GridVariant.NO_BORDER.getVariantName(), mobile);
        grid.bindThemeName(GridVariant.NO_ROW_BORDERS.getVariantName(), mobile);
        Signal.effect(grid, () -> {
            boolean m = mobile.get();
            for (Grid.Column<T> c : grid.getColumns()) {
                c.setVisible(c == colonna ? m : !m && visibileSuDesktop.getOrDefault(c, true));
            }
        });
        return colonna;
    }

    /** Cambia la visibilità di una colonna su desktop; su mobile resta nascosta. */
    public void setVisibileSuDesktop(Grid.Column<T> c, boolean visibile) {
        visibileSuDesktop.put(c, visibile);
        c.setVisible(visibile && !mobile.peek());
    }

    private Card creaCard(T item) {
        Card card = new Card();
        card.addThemeVariants(CardVariant.OUTLINED);
        card.addClassName("card-mobile");
        card.setWidthFull();
        if (titolo != null) {
            card.setTitle(titolo.apply(item));
        }
        if (sottotitolo != null) {
            String testo = sottotitolo.apply(item);
            if (testo != null && !testo.isBlank()) {
                card.setSubtitle(testo);
            }
        }
        if (stato != null) {
            card.setHeaderSuffix(stato.apply(item));
        }
        for (Campo<T> campo : campi) {
            Component valore = valore(campo.valore().apply(item));
            if (valore != null) {
                Span etichetta = new Span(campo.etichetta());
                etichetta.addClassName("card-mobile-etichetta");
                Div riga = new Div(etichetta, valore);
                riga.addClassName("card-mobile-campo");
                card.add(riga);
            }
        }
        if (azioni != null) {
            card.addToFooter(azioni.apply(item));
        }
        return card;
    }

    private static Component valore(Object valore) {
        if (valore instanceof Component component) {
            return component;
        }
        if (valore == null || valore.toString().isBlank()) {
            return null;
        }
        Span testo = new Span(valore.toString());
        testo.addClassName("card-mobile-valore");
        return testo;
    }
}
