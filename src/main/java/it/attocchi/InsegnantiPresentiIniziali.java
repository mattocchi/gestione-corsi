package it.attocchi;

import it.attocchi.service.LezioneService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * All'avvio (tutti i profili) registra gli insegnanti del corso sulle lezioni che non hanno insegnanti presenti:
 * sostituisce lo script di migrazione per le lezioni create prima dei due insegnanti per corso.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class InsegnantiPresentiIniziali implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(InsegnantiPresentiIniziali.class);

    private final LezioneService lezioneService;

    public InsegnantiPresentiIniziali(LezioneService lezioneService) {
        this.lezioneService = lezioneService;
    }

    @Override
    public void run(String... args) {
        int aggiornate = lezioneService.completaInsegnantiPresenti();
        if (aggiornate > 0) {
            log.info("Registrati gli insegnanti presenti su {} lezioni", aggiornate);
        }
    }
}
