package it.attocchi.views;

import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.function.SerializableRunnable;
import it.attocchi.entity.Anagrafica;
import it.attocchi.entity.User;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Campo "Cerca" degli elenchi: filtra mentre si scrive (dopo una breve pausa) nei testi di ogni riga, senza
 * distinguere maiuscole e accenti; con più parole devono comparire tutte, in qualsiasi ordine. Accanto mostra
 * "N di M" quando c'è un testo.
 *
 * <pre>{@code
 * RicercaRapida ricerca = new RicercaRapida();
 * ricerca.collega(grid.setItems(elementi), e -> RicercaRapida.testo(e.getNome(), e.getEmail()));
 * }</pre>
 *
 * Con altri filtri (es. stato e anno) si usa {@link #corrisponde(String)} nel proprio filtro,
 * {@link #addRicercaListener} per riapplicarlo e {@link #mostraConteggio} per il conteggio.
 */
public class RicercaRapida extends Div {

    private final TextField campo = new TextField();
    private final Span conteggio = new Span();
    private SerializableRunnable applica = () -> { };

    public RicercaRapida() {
        campo.setPlaceholder("Cerca");
        campo.setAriaLabel("Cerca nell'elenco");
        campo.setPrefixComponent(VaadinIcon.SEARCH.create());
        campo.setClearButtonVisible(true);
        campo.setValueChangeMode(ValueChangeMode.LAZY);
        campo.addClassNames("w-full sm:w-72");
        campo.addValueChangeListener(e -> applica.run());
        conteggio.addClassNames(Stili.TESTO_SECONDARIO);
        conteggio.setVisible(false);
        addClassNames("flex flex-wrap items-center gap-x-3 w-full sm:w-auto");
        add(campo, conteggio);
    }

    /**
     * Filtra il data view con i testi di ogni riga e aggiorna il conteggio; da richiamare dopo ogni
     * {@code setItems} (un nuovo data view perde il filtro).
     */
    public <T> void collega(GridListDataView<T> righe, SerializableFunction<T, String> testo) {
        int totale = righe.getItemCount();
        applica = () -> {
            righe.setFilter(riga -> corrisponde(testo.apply(riga)));
            mostraConteggio(righe.getItemCount(), totale);
        };
        applica.run();
    }

    /** Azione eseguita quando cambia il testo (per chi combina la ricerca con altri filtri). */
    public void addRicercaListener(SerializableRunnable listener) {
        applica = listener;
    }

    /** {@code true} se il testo indicato contiene tutte le parole cercate (o non si cerca nulla). */
    public boolean corrisponde(String testo) {
        return corrisponde(campo.getValue(), testo);
    }

    /** {@code true} se si sta cercando qualcosa. */
    public boolean isAttiva() {
        return !campo.getValue().isBlank();
    }

    /** "N di M" accanto al campo, solo mentre si cerca. */
    public void mostraConteggio(int trovati, int totale) {
        conteggio.setVisible(isAttiva());
        conteggio.setText(trovati + " di " + totale);
    }

    public TextField getCampo() {
        return campo;
    }

    /** {@code true} se {@code testo} contiene tutte le parole di {@code ricerca}, senza maiuscole né accenti. */
    public static boolean corrisponde(String ricerca, String testo) {
        if (ricerca == null || ricerca.isBlank()) {
            return true;
        }
        String normalizzato = normalizza(testo);
        return Arrays.stream(normalizza(ricerca).split("\\s+")).allMatch(normalizzato::contains);
    }

    /** Unisce i valori di una riga in un unico testo da cercare (i {@code null} sono ignorati). */
    public static String testo(Object... valori) {
        return Arrays.stream(valori).filter(Objects::nonNull).map(Object::toString).collect(Collectors.joining(" "));
    }

    /** Dati dell'utente da cercare: username, nome, cognome, email, telefono e ruoli ({@code null} = nessuno). */
    public static String testo(User user) {
        return user == null ? "" : testo(user.getUsername(), user.getNome(), user.getCognome(), user.getEmail(),
            user.getTelefono(), String.join(" ", user.getRoles()));
    }

    /** Codice fiscale e indirizzo della persona. */
    public static String testo(Anagrafica persona) {
        return testo(persona.getCodiceFiscale(), persona.getIndirizzo().getFormattato());
    }

    private static String normalizza(String testo) {
        if (testo == null) {
            return "";
        }
        return Normalizer.normalize(testo, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ITALIAN).strip();
    }
}
