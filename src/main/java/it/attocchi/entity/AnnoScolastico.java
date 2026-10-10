package it.attocchi.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Anni scolastici nel formato "2026/2027": un anno va da settembre ad agosto.
 */
public final class AnnoScolastico {

    private AnnoScolastico() {
    }

    /** Anno scolastico a cui appartiene la data (da settembre è già l'anno successivo). */
    public static String di(LocalDate data) {
        int inizio = data.getMonthValue() >= 9 ? data.getYear() : data.getYear() - 1;
        return inizio + "/" + (inizio + 1);
    }

    /** Anno precedente, corrente e successivo rispetto a oggi, più l'eventuale valore già assegnato. */
    public static List<String> elenco(LocalDate oggi, String valoreAttuale) {
        String corrente = di(oggi);
        List<String> anni = new ArrayList<>(List.of(di(oggi.minusYears(1)), corrente, di(oggi.plusYears(1))));
        if (valoreAttuale != null && !anni.contains(valoreAttuale)) {
            anni.add(valoreAttuale);
            anni.sort(null);
        }
        return anni;
    }
}
