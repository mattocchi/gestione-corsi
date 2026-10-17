package it.attocchi.service;

import it.attocchi.entity.Festivita;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Festività attive in un dato momento: dice se un giorno è festivo e quale festività è. */
public final class CalendarioFestivita {

    private final List<Festivita> attive;

    CalendarioFestivita(List<Festivita> attive) {
        this.attive = List.copyOf(attive);
    }

    /** Nome della festività (o delle festività, separate da virgola) in cui cade la data, se festiva. */
    public Optional<String> festivita(LocalDate data) {
        List<String> nomi = attive.stream().filter(f -> f.comprende(data)).map(Festivita::getNome).toList();
        return nomi.isEmpty() ? Optional.empty() : Optional.of(String.join(", ", nomi));
    }

    public boolean festivo(LocalDate data) {
        return attive.stream().anyMatch(f -> f.comprende(data));
    }
}
