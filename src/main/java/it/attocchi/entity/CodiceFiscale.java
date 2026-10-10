package it.attocchi.entity;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Controllo del codice fiscale delle persone fisiche: formato (anche con omocodia) e carattere di controllo.
 */
public final class CodiceFiscale {

    /** Cognome, nome, anno, mese, giorno, comune, controllo; le cifre possono essere sostituite da lettere (omocodia). */
    private static final Pattern FORMATO = Pattern.compile(
        "[A-Z]{6}[0-9LMNPQRSTUV]{2}[ABCDEHLMPRST][0-9LMNPQRSTUV]{2}[A-Z][0-9LMNPQRSTUV]{3}[A-Z]");

    /** Valore dei caratteri in posizione dispari (1ª, 3ª, …), per A–Z; le cifre 0–9 valgono come A–J. */
    private static final int[] DISPARI = {
        1, 0, 5, 7, 9, 13, 15, 17, 19, 21, 2, 4, 18, 20, 11, 3, 6, 8, 12, 14, 16, 10, 22, 25, 24, 23
    };

    private CodiceFiscale() {
    }

    /** Codice in maiuscolo e senza spazi; {@code null} se vuoto. */
    public static String normalizza(String codice) {
        if (codice == null || codice.isBlank()) {
            return null;
        }
        return codice.replaceAll("\\s", "").toUpperCase(Locale.ROOT);
    }

    /** {@code true} se il codice (anche minuscolo o con spazi) ha formato e carattere di controllo corretti. */
    public static boolean isValido(String codice) {
        String cf = normalizza(codice);
        return cf != null && FORMATO.matcher(cf).matches() && carattereDiControllo(cf.substring(0, 15)) == cf.charAt(15);
    }

    /** Carattere di controllo dei primi 15 caratteri (maiuscoli) del codice. */
    public static char carattereDiControllo(String primi15) {
        int somma = 0;
        for (int i = 0; i < 15; i++) {
            char c = primi15.charAt(i);
            int indice = Character.isDigit(c) ? c - '0' : c - 'A';
            // Posizioni 1-based dispari = indici pari
            somma += i % 2 == 0 ? DISPARI[indice] : indice;
        }
        return (char) ('A' + somma % 26);
    }
}
