package it.attocchi;

import it.attocchi.entity.Allievo;
import it.attocchi.entity.Corso;
import it.attocchi.entity.GruppoCorso;
import it.attocchi.entity.Iscrizione;
import it.attocchi.service.AllievoService;
import it.attocchi.service.CorsoService;
import it.attocchi.service.PresenzaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Conversione all'avvio dei giorni per allievo (tabella {@code iscrizione_giorni} della versione precedente) in gruppi. */
@SpringBootTest
class GruppiInizialiTest {

    @Autowired
    private GruppiIniziali gruppiIniziali;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private CorsoService corsoService;

    @Autowired
    private AllievoService allievoService;

    @Autowired
    private PresenzaService presenzaService;

    @Test
    void giorniPerAllievo_unSoloGiornoDiventaIlGruppoDiQuelGiorno() {
        // Chitarra: martedì e sabato. Chitarra1 e 2 solo il martedì, Chitarra3 martedì e sabato
        Corso chitarra = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Chitarra")).findFirst().orElseThrow();
        corsoService.generaLezioni(chitarra);
        List<Allievo> allievi = List.of(allievo(1), allievo(2), allievo(3));
        jdbc.execute("CREATE TABLE iscrizione_giorni (corso_id BIGINT, allievo_id BIGINT, giorno_settimana VARCHAR(20))");
        try {
            inserisci(chitarra, allievi.get(0), "TUESDAY");
            inserisci(chitarra, allievi.get(1), "TUESDAY");
            inserisci(chitarra, allievi.get(2), "TUESDAY");
            inserisci(chitarra, allievi.get(2), "SATURDAY");

            gruppiIniziali.run();

            List<GruppoCorso> gruppi = corsoService.gruppi(chitarra.getId());
            assertEquals(1, gruppi.size());
            GruppoCorso martedi = gruppi.getFirst();
            assertEquals(DayOfWeek.TUESDAY, martedi.getGiorno());
            assertNull(martedi.getNome());
            assertEquals(chitarra.getOrarioInizio(), martedi.getOrario());
            assertEquals(martedi, iscrizione(chitarra, allievi.get(0)).getGruppo());
            assertEquals(martedi, iscrizione(chitarra, allievi.get(1)).getGruppo());
            assertNull(iscrizione(chitarra, allievi.get(2)).getGruppo());
            assertEquals(Set.of(DayOfWeek.TUESDAY), giorniConPresenza(chitarra, allievi.get(0)));
            assertEquals(Set.of(DayOfWeek.TUESDAY, DayOfWeek.SATURDAY), giorniConPresenza(chitarra, allievi.get(2)));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM iscrizione_giorni", Integer.class));

            // Una seconda esecuzione non cambia nulla
            gruppiIniziali.run();
            assertEquals(1, corsoService.gruppi(chitarra.getId()).size());
        } finally {
            jdbc.execute("DROP TABLE iscrizione_giorni");
            allievi.forEach(a -> corsoService.assegnaGruppo(chitarra.getId(), a.getId(), null));
            corsoService.gruppi(chitarra.getId()).forEach(g -> corsoService.eliminaGruppo(g.getId()));
        }
    }

    private Allievo allievo(int n) {
        return allievoService.findByUsername("allievo_chitarra" + n).orElseThrow();
    }

    private void inserisci(Corso corso, Allievo allievo, String giorno) {
        jdbc.update("INSERT INTO iscrizione_giorni VALUES (?, ?, ?)", corso.getId(), allievo.getId(), giorno);
    }

    private Iscrizione iscrizione(Corso corso, Allievo allievo) {
        return corsoService.findPartecipanti(corso.getId()).stream()
            .filter(i -> i.getAllievo().equals(allievo)).findFirst().orElseThrow();
    }

    private Set<DayOfWeek> giorniConPresenza(Corso corso, Allievo allievo) {
        return presenzaService.findByCorsoAndAllievo(corso.getId(), allievo.getId()).stream()
            .map(p -> p.getLezione().getData().getDayOfWeek()).collect(Collectors.toSet());
    }
}
