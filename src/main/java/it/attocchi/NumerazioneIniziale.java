package it.attocchi;

import it.attocchi.service.NumerazioneLezioni;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * All'avvio, in ogni profilo, rinumera per giorno della settimana le lezioni dei corsi con gruppi (vedi
 * {@link NumerazioneLezioni}): sistema i numeri delle lezioni generate prima di questa regola. Dopo
 * {@link GruppiIniziali}, che crea i gruppi dai giorni per allievo della versione precedente.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class NumerazioneIniziale implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(NumerazioneIniziale.class);

    private final NumerazioneLezioni numerazioneLezioni;

    public NumerazioneIniziale(NumerazioneLezioni numerazioneLezioni) {
        this.numerazioneLezioni = numerazioneLezioni;
    }

    @Override
    public void run(String... args) {
        int corsi = numerazioneLezioni.rinumeraTutti();
        if (corsi > 0) {
            log.info("Lezioni rinumerate per giorno in {} corsi con gruppi", corsi);
        }
    }
}
