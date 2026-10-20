package it.attocchi;

import it.attocchi.entity.IscrizioneId;
import it.attocchi.service.CorsoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * All'avvio (tutti i profili) converte in gruppi i giorni per allievo della versione precedente, rimasti nella
 * tabella {@code iscrizione_giorni} (che {@code ddl-auto=update} non elimina), poi svuota la tabella: si esegue una
 * sola volta. Senza la tabella (database nuovo) non fa nulla.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class GruppiIniziali implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(GruppiIniziali.class);

    private final JdbcTemplate jdbc;
    private final CorsoService corsoService;

    public GruppiIniziali(JdbcTemplate jdbc, CorsoService corsoService) {
        this.jdbc = jdbc;
        this.corsoService = corsoService;
    }

    @Override
    public void run(String... args) {
        Map<IscrizioneId, Set<DayOfWeek>> giorni = new HashMap<>();
        try {
            jdbc.query("SELECT corso_id, allievo_id, giorno_settimana FROM iscrizione_giorni", riga -> {
                giorni.computeIfAbsent(new IscrizioneId(riga.getLong(1), riga.getLong(2)), k -> new HashSet<>())
                    .add(DayOfWeek.valueOf(riga.getString(3)));
            });
        } catch (DataAccessException tabellaAssente) {
            return;
        }
        if (giorni.isEmpty()) {
            return;
        }
        int assegnati = corsoService.convertiGiorniInGruppi(giorni);
        jdbc.update("DELETE FROM iscrizione_giorni");
        log.info("Giorni per allievo convertiti in gruppi: {} iscrizioni lette, {} assegnate a un gruppo",
            giorni.size(), assegnati);
    }
}
