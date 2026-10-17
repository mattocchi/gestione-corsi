package it.attocchi;

import it.attocchi.service.FestivitaService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * All'avvio (tutti i profili) popola le festività nazionali se l'elenco è vuoto. Gira prima di
 * {@link DataInitializer}, così in dev anche le lezioni dei corsi di prova saltano le festività.
 */
@Component
@Order(-1)
public class FestivitaIniziali implements CommandLineRunner {

    private final FestivitaService festivitaService;

    public FestivitaIniziali(FestivitaService festivitaService) {
        this.festivitaService = festivitaService;
    }

    @Override
    public void run(String... args) {
        festivitaService.inizializzaSeVuoto();
    }
}
