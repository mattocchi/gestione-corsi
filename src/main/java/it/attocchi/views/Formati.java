package it.attocchi.views;

import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Formati di visualizzazione usati nell'interfaccia.
 */
public final class Formati {

    private static final Locale ITALIANO = Locale.ITALIAN;
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("EEE d MMMM", ITALIANO);
    private static final DateTimeFormatter DATA_CON_ANNO = DateTimeFormatter.ofPattern("EEE d MMMM yyyy", ITALIANO);

    private Formati() {
    }

    /**
     * Data leggibile con il giorno della settimana, es. "mer 7 ottobre".
     * L'anno è aggiunto solo se diverso da quello corrente ("gio 3 ottobre 2024").
     */
    public static String data(LocalDate data) {
        return data(data, Year.now());
    }

    static String data(LocalDate data, Year annoCorrente) {
        if (data == null) {
            return "";
        }
        return (data.getYear() == annoCorrente.getValue() ? DATA : DATA_CON_ANNO).format(data);
    }
}
