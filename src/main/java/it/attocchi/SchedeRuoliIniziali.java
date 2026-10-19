package it.attocchi;

import it.attocchi.service.SchedeRuoliService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * All'avvio (tutti i profili) crea le schede di insegnante e genitore mancanti per gli utenti che hanno già quel
 * ruolo (assegnato da Gestione Utenti prima che le schede si creassero in automatico). Idempotente.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class SchedeRuoliIniziali implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SchedeRuoliIniziali.class);

    private final SchedeRuoliService schedeRuoliService;

    public SchedeRuoliIniziali(SchedeRuoliService schedeRuoliService) {
        this.schedeRuoliService = schedeRuoliService;
    }

    @Override
    public void run(String... args) {
        int create = schedeRuoliService.completaTutte();
        if (create > 0) {
            log.info("Create {} schede di insegnante o genitore mancanti per utenti con quel ruolo", create);
        }
    }
}
