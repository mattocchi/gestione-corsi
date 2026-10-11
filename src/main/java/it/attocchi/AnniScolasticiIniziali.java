package it.attocchi;

import it.attocchi.service.AnnoScolasticoService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * All'avvio (tutti i profili) popola l'elenco degli anni scolastici se è vuoto. Gira dopo
 * {@link DataInitializer}, così in dev include anche gli anni dei corsi di prova.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class AnniScolasticiIniziali implements CommandLineRunner {

    private final AnnoScolasticoService annoScolasticoService;

    public AnniScolasticiIniziali(AnnoScolasticoService annoScolasticoService) {
        this.annoScolasticoService = annoScolasticoService;
    }

    @Override
    public void run(String... args) {
        annoScolasticoService.inizializzaSeVuoto(LocalDate.now());
    }
}
