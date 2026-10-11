package it.attocchi.views.insegnante;

import com.vaadin.flow.component.badge.BadgeVariant;
import it.attocchi.entity.Lezione;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

/**
 * Collocazione di una lezione rispetto a oggi, mostrata come badge nella lista delle lezioni dell'insegnante.
 * Settimane di calendario da lunedì a domenica; le lezioni passate sono segnalate solo se non completate.
 */
public enum QuandoLezione {
    OGGI("Oggi", BadgeVariant.FILLED),
    QUESTA_SETTIMANA("Questa settimana", null),
    SETTIMANA_PROSSIMA("Settimana prossima", BadgeVariant.CONTRAST),
    SETTIMANA_SCORSA("Settimana scorsa", BadgeVariant.WARNING),
    IN_RITARDO("In ritardo", BadgeVariant.ERROR);

    private final String etichetta;
    private final BadgeVariant variante;

    QuandoLezione(String etichetta, BadgeVariant variante) {
        this.etichetta = etichetta;
        this.variante = variante;
    }

    public String getEtichetta() {
        return etichetta;
    }

    /** Variante di colore del badge; {@code null} = colore di default. */
    public BadgeVariant getVariante() {
        return variante;
    }

    public static Optional<QuandoLezione> di(Lezione lezione, LocalDate oggi) {
        LocalDate data = lezione.getData();
        if (data == null) {
            return Optional.empty();
        }
        if (data.equals(oggi)) {
            return Optional.of(OGGI);
        }
        LocalDate lunedi = oggi.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        if (!data.isBefore(lunedi.plusWeeks(2))) {
            return Optional.empty();
        }
        if (!data.isBefore(lunedi.plusWeeks(1))) {
            return Optional.of(SETTIMANA_PROSSIMA);
        }
        if (!data.isBefore(lunedi)) {
            return Optional.of(QUESTA_SETTIMANA);
        }
        if (lezione.isCompletata()) {
            return Optional.empty();
        }
        return Optional.of(!data.isBefore(lunedi.minusWeeks(1)) ? SETTIMANA_SCORSA : IN_RITARDO);
    }
}
