package it.attocchi;

import it.attocchi.service.PagamentoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * All'avvio (tutti i profili) le quote personalizzate delle iscrizioni precedenti alla scelta del tipo di quota, se
 * uguali al prezzo di pre-iscrizione del corso e senza motivo (o con motivo "pre-iscrizione"), diventano
 * pre-iscrizione ({@link PagamentoService#convertiPreiscrizioni()}). Le iscrizioni con il tipo salvato non si toccano.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class QuotePreiscrizioneIniziali implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(QuotePreiscrizioneIniziali.class);

    private final PagamentoService pagamentoService;

    public QuotePreiscrizioneIniziali(PagamentoService pagamentoService) {
        this.pagamentoService = pagamentoService;
    }

    @Override
    public void run(String... args) {
        int convertite = pagamentoService.convertiPreiscrizioni();
        if (convertite > 0) {
            log.info("Quote personalizzate convertite in pre-iscrizione: {}", convertite);
        }
    }
}
