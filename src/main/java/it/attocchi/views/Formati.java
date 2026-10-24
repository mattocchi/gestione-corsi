package it.attocchi.views;

import it.attocchi.entity.Allievo;
import it.attocchi.entity.Corso;
import it.attocchi.entity.GruppoCorso;
import it.attocchi.entity.Lezione;
import it.attocchi.service.CalendarioService.VoceCalendario;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    private static final DateTimeFormatter DATA_NUMERICA = DateTimeFormatter.ofPattern("dd/MM/yyyy", ITALIANO);
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

    /** Giorni del corso con la cadenza se non è settimanale, es. "sab, ogni 2 settimane". */
    public static String giorniECadenza(Corso corso) {
        String giorni = giorni(corso.getGiorniSettimana());
        if (corso.getCadenzaSettimane() == 1) {
            return giorni;
        }
        String cadenza = "ogni " + corso.getCadenzaSettimane() + " settimane";
        return giorni.isEmpty() ? cadenza : giorni + ", " + cadenza;
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
     * Gruppo del corso: "Martedì 17:00" o, con il nome, "Principianti – mar 17:00"; stringa vuota se {@code null}.
     */
    public static String gruppo(GruppoCorso gruppo) {
        if (gruppo == null) {
            return "";
        }
        String orario = gruppo.getOrario() != null ? " " + ora(gruppo.getOrario()) : "";
        if (gruppo.getNome() != null && !gruppo.getNome().isBlank()) {
            return gruppo.getNome() + " – " + giorno(gruppo.getGiorno()) + orario;
        }
        String giorno = gruppo.getGiorno().getDisplayName(TextStyle.FULL, ITALIANO);
        return Character.toUpperCase(giorno.charAt(0)) + giorno.substring(1) + orario;
    }

    /** Durata in minuti come ore e minuti: "12 h 30 min", "12 h", "45 min", "0 h". */
    public static String durata(long minuti) {
        long ore = minuti / 60;
        long resto = minuti % 60;
        if (ore == 0 && resto > 0) {
            return resto + " min";
        }
        return ore + " h" + (resto > 0 ? " " + resto + " min" : "");
    }

    /** Orario "17:00"; stringa vuota se {@code null}. */
    public static String ora(LocalTime ora) {
        return ora != null ? ora.format(ORA) : "";
    }

    /**
     * Numero della lezione; nei corsi con gruppi (numerati per giorno) con il gruppo di quel giorno, se è uno solo e
     * ha un nome, o il giorno: "5 · Piccoli", "5 · lun". Un recupero usa il giorno della lezione saltata.
     */
    public static String numeroLezione(Lezione lezione, List<GruppoCorso> gruppiDelCorso) {
        String numero = String.valueOf(lezione.getNumeroLezione());
        if (gruppiDelCorso == null || gruppiDelCorso.isEmpty()) {
            return numero;
        }
        DayOfWeek giorno = (lezione.getRecuperoDi() != null ? lezione.getRecuperoDi() : lezione).getData().getDayOfWeek();
        List<GruppoCorso> delGiorno = gruppiDelCorso.stream().filter(g -> g.getGiorno() == giorno).toList();
        boolean nome = delGiorno.size() == 1 && delGiorno.getFirst().getNome() != null && !delGiorno.getFirst().getNome().isBlank();
        return numero + " · " + (nome ? delGiorno.getFirst().getNome() : giorno(giorno));
    }

    /**
     * Numero di una voce del calendario con il suo gruppo: "5 · Grandi" se il gruppo ha un nome, "5 · Senza gruppo"
     * per chi non ne ha, altrimenti come {@link #numeroLezione(Lezione, List)} ("5 · sab").
     */
    public static String numeroLezione(VoceCalendario voce, List<GruppoCorso> gruppiDelCorso) {
        GruppoCorso gruppo = voce.gruppo();
        if (gruppo != null && gruppo.getNome() != null && !gruppo.getNome().isBlank()) {
            return voce.lezione().getNumeroLezione() + " · " + gruppo.getNome();
        }
        if (voce.senzaGruppo()) {
            return voce.lezione().getNumeroLezione() + " · Senza gruppo";
        }
        return numeroLezione(voce.lezione(), gruppiDelCorso);
    }

    /** Il gruppo del partecipante o, senza gruppo, i giorni del corso (es. "mar, sab"). */
    public static String gruppoOGiorni(Corso corso, GruppoCorso gruppo) {
        return gruppo != null ? gruppo(gruppo) : giorni(corso.getGiorniSettimana());
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

    /** Data in cifre, es. "05/03/2012" (data di nascita nei documenti); stringa vuota se nulla. */
    public static String dataNumerica(LocalDate data) {
        return data != null ? DATA_NUMERICA.format(data) : "";
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
