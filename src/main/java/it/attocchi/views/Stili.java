package it.attocchi.views;

/**
 * Combinazioni di utility Tailwind usate in più punti. Il compilatore Tailwind genera solo le classi che trova scritte
 * nei sorgenti: qui vanno scritte per intero, mai composte a runtime. Colori e tipografia vengono dai token del tema
 * Aura, così tema chiaro e scuro restano coerenti.
 */
public final class Stili {

    /** Testo secondario piccolo: avvisi su contenuti in sola lettura, note, dettagli ed etichette. */
    public static final String TESTO_SECONDARIO = "text-(--vaadin-text-color-secondary) text-(length:--aura-font-size-s)";

    /** Titolo di sezione dentro un dialog ({@link ViewSupport#sezione}). */
    public static final String TITOLO_SEZIONE = "text-(length:--aura-font-size-m) mx-0 mt-2 mb-0";

    /** Riga di valori che va a capo su schermi stretti: riepilogo quota/pagato/residuo e stato. */
    public static final String RIEPILOGO_PAGAMENTI = "flex flex-wrap items-center gap-x-5 gap-y-2 font-(--aura-font-weight-medium)";

    private Stili() {
    }
}
