package it.attocchi.service;

import it.attocchi.entity.Corso;
import it.attocchi.entity.Lezione;
import it.attocchi.entity.Presenza;
import it.attocchi.entity.Presenza.StatoPresenza;
import it.attocchi.service.MieiAllieviService.AllievoDelCorso;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * "I miei allievi" sui dati di DataInitializer: Canto (insegnante1, 7 allievi, Canto5 alle 17:00 per primo) e
 * Chitarra (insegnante2, 6 allievi), 30 lezioni previste, nessuna ancora completata.
 */
@SpringBootTest
class MieiAllieviServiceTest {

    @Autowired
    private MieiAllieviService mieiAllieviService;

    @Autowired
    private LezioneService lezioneService;

    @Autowired
    private PresenzaService presenzaService;

    @Autowired
    private CorsoService corsoService;

    @Test
    void soloGliAllieviDeiCorsiDellInsegnante() {
        List<AllievoDelCorso> canto = mieiAllieviService.allieviDellInsegnante("insegnante1");
        assertEquals(7, canto.size());
        assertTrue(canto.stream().allMatch(a -> a.iscrizione().getCorso().getNome().equals("Corso di Canto")));
        // Nell'ordine dei partecipanti: per orario di lezione
        assertEquals("Allievo Canto5", canto.getFirst().iscrizione().getAllievo().getNomeCompleto());
        ConteggioLezioni conteggio = canto.getFirst().conteggio();
        assertEquals(new ConteggioLezioni(30, 0, 30, 0, 0, 0), conteggio);
        assertEquals(30, conteggio.mancanti());

        assertEquals(6, mieiAllieviService.allieviDellInsegnante("insegnante2").size());
        assertTrue(mieiAllieviService.allieviDellInsegnante("admin").isEmpty());
    }

    @Test
    @Transactional
    void svolteDaFareEMancanti_soloSulleLezioniCompletate() {
        List<Lezione> lezioni = lezioneService.findByCorso(canto().getId());
        Lezione prima = lezioni.get(0);
        Lezione seconda = lezioni.get(1);
        Presenza assenza = presenzaService.findByLezione(prima.getId()).stream()
            .filter(p -> p.getAllievo().getNomeCompleto().equals("Allievo Canto5")).findFirst().orElseThrow();
        assenza.setStato(StatoPresenza.ASSENTE_GIUSTIFICATA);
        presenzaService.save(assenza);
        lezioneService.completa(prima.getId(), "insegnante1");
        lezioneService.segnaNonEffettuata(seconda.getId(), null, "insegnante1");

        AllievoDelCorso canto5 = mieiAllieviService.allieviDellInsegnante("insegnante1").getFirst();
        // Una svolta (assente giustificato), una non effettuata che non conta, 28 da fare
        assertEquals(new ConteggioLezioni(30, 1, 28, 0, 1, 0), canto5.conteggio());
        assertEquals(29, canto5.conteggio().mancanti());
        AllievoDelCorso altro = mieiAllieviService.allieviDellInsegnante("insegnante1").get(1);
        assertEquals(new ConteggioLezioni(30, 1, 28, 1, 0, 0), altro.conteggio());
    }

    @Test
    void lezioniDellAllievo_soloPerLInsegnanteDelCorso() {
        AllievoDelCorso canto5 = mieiAllieviService.allieviDellInsegnante("insegnante1").getFirst();
        Long corsoId = canto5.iscrizione().getCorso().getId();
        Long allievoId = canto5.iscrizione().getAllievo().getId();
        List<Presenza> lezioni = mieiAllieviService.lezioniDellAllievo(corsoId, allievoId, "insegnante1");
        assertEquals(30, lezioni.size());
        assertTrue(lezioni.getFirst().getLezione().getData().isBefore(lezioni.getLast().getLezione().getData()));
        assertThrows(IllegalStateException.class, () -> mieiAllieviService.lezioniDellAllievo(corsoId, allievoId, "insegnante2"));
        assertThrows(IllegalStateException.class, () -> mieiAllieviService.lezioniDellAllievo(corsoId, allievoId, "allievo_canto5"));
    }

    private Corso canto() {
        return corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow();
    }
}
