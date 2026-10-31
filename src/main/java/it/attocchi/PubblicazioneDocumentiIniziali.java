package it.attocchi;

import it.attocchi.service.PubblicazioneDocumentiService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * All'avvio (tutti i profili) imposta la pubblicazione dei documenti per le famiglie se manca: tipi visibili e, la
 * prima volta, attestazioni degli anni già finiti. Gira dopo {@link DataInitializer} (pagamenti di prova in dev).
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class PubblicazioneDocumentiIniziali implements CommandLineRunner {

    private final PubblicazioneDocumentiService pubblicazioneDocumentiService;

    public PubblicazioneDocumentiIniziali(PubblicazioneDocumentiService pubblicazioneDocumentiService) {
        this.pubblicazioneDocumentiService = pubblicazioneDocumentiService;
    }

    @Override
    public void run(String... args) {
        pubblicazioneDocumentiService.inizializza(LocalDate.now());
    }
}
