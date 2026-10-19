package it.attocchi.views;

import it.attocchi.entity.Allievo;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Formati di visualizzazione usati nell'interfaccia.
 */
public final class Formati {

    private static final Locale ITALIANO = Locale.ITALIAN;
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("EEE d MMMM", ITALIANO);
    private static final DateTimeFormatter DATA_CON_ANNO = DateTimeFormatter.ofPattern("EEE d MMMM yyyy", ITALIANO);
    private static final DateTimeFormatter ORA = DateTimeFormatter.ofPattern("HH:mm", ITALIANO);
    private static final DateTimeFormatter MESE = DateTimeFormatter.ofPattern("LLLL yyyy", ITALIANO);

    private Formati() {
    }

    /** Giorno della settimana abbreviato, es. "mar". */
    public static String giorno(DayOfWeek giorno) {
        return giorno.getDisplayName(TextStyle.SHORT, ITALIANO);
    }

    /** Giorni della settimana abbreviati, da lunedì, es. "mar, sab"; stringa vuota se nessuno. */
    public static String giorni(Collection<DayOfWeek> giorni) {
        return giorni.stream().sorted().map(Formati::giorno).collect(Collectors.joining(", "));
    }

    /** Giorni della settimana per esteso, da lunedì, es. "martedì e sabato" (per documenti ed email). */
    public static String giorniEstesi(Collection<DayOfWeek> giorni) {
        List<String> nomi = giorni.stream().sorted().map(g -> g.getDisplayName(TextStyle.FULL, ITALIANO)).toList();
        if (nomi.size() <= 1) {
            return String.join("", nomi);
        }
        return String.join(", ", nomi.subList(0, nomi.size() - 1)) + " e " + nomi.getLast();
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

    /** Data con il giorno della settimana e sempre l'anno, es. "mer 7 ottobre 2026" (documenti PDF). */
    public static String dataCompleta(LocalDate data) {
        return data != null ? DATA_CON_ANNO.format(data) : "";
    }

    /** Data e ora, es. "mer 7 ottobre, 10:35" (anno come in {@link #data(LocalDate)}); stringa vuota se nulla. */
    public static String dataOra(LocalDateTime dataOra) {
        return dataOra != null ? data(dataOra.toLocalDate()) + ", " + ORA.format(dataOra) : "";
    }

    /** Mese con l'anno, es. "ottobre 2026"; stringa vuota se nullo. */
    public static String mese(YearMonth mese) {
        return mese != null ? MESE.format(mese) : "";
    }

    /** Importo in euro nel formato italiano, es. "1.250,50 €"; stringa vuota se nullo. */
    public static String euro(BigDecimal importo) {
        if (importo == null) {
            return "";
        }
        // NumberFormat non è thread-safe: una nuova istanza per chiamata; spazio normale al posto di quello non separabile
        return NumberFormat.getCurrencyInstance(Locale.ITALY).format(importo).replace('\u00a0', ' ');
    }

    /** Indirizzo dell'allievo, o quello del genitore con "(del genitore)" se l'allievo non ne ha. */
    public static String indirizzo(Allievo allievo) {
        String indirizzo = allievo.getIndirizzoEffettivo().getFormattato();
        return allievo.isIndirizzoDelGenitore() ? indirizzo + " (del genitore)" : indirizzo;
    }
}
