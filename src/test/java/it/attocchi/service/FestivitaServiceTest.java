package it.attocchi.service;

import it.attocchi.entity.Festivita;
import it.attocchi.entity.Festivita.Tipo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Festività precaricate all'avvio e calendario usato dalla generazione delle lezioni. */
@SpringBootTest
class FestivitaServiceTest {

    @Autowired
    private FestivitaService festivitaService;

    @Test
    void avvio_precaricaLeFestivitaNazionali() {
        List<String> nomi = festivitaService.elenco().stream().map(Festivita::getNome).toList();
        assertTrue(nomi.containsAll(List.of("Natale", "Pasqua", "Lunedì dell'Angelo", "San Francesco d'Assisi")));
        assertEquals(13, nomi.size());

        // Già popolato: non aggiunge altro
        festivitaService.inizializzaSeVuoto();
        assertEquals(13, festivitaService.elenco().size());

        CalendarioFestivita calendario = festivitaService.calendario();
        assertEquals(Optional.of("Natale"), calendario.festivita(LocalDate.of(2026, 12, 25)));
        assertTrue(calendario.festivo(LocalDate.of(2026, 4, 6)));
        assertTrue(calendario.festivo(LocalDate.of(2026, 10, 4)));
        assertFalse(calendario.festivo(LocalDate.of(2026, 10, 5)));
    }

    @Test
    void salva_validaICampiDelTipoEScartaGliAltri() {
        Festivita senzaNome = Festivita.ogniAnno(" ", 1, 1);
        assertThrows(IllegalArgumentException.class, () -> festivitaService.salva(senzaNome));
        assertThrows(IllegalArgumentException.class, () -> festivitaService.salva(Festivita.ogniAnno("X", 31, 4)));

        Festivita senzaData = new Festivita();
        senzaData.setNome("Ponte");
        senzaData.setTipo(Tipo.DATE);
        assertThrows(IllegalArgumentException.class, () -> festivitaService.salva(senzaData));
        senzaData.setDataInizio(LocalDate.of(2026, 11, 3));
        senzaData.setDataFine(LocalDate.of(2026, 11, 2));
        assertThrows(IllegalArgumentException.class, () -> festivitaService.salva(senzaData));

        Festivita ponte = new Festivita();
        ponte.setNome(" Ponte di Ognissanti ");
        ponte.setTipo(Tipo.DATE);
        ponte.setGiornoInizio(5);
        ponte.setDataInizio(LocalDate.of(2026, 11, 2));
        Festivita salvata = festivitaService.salva(ponte);
        try {
            assertEquals("Ponte di Ognissanti", salvata.getNome());
            assertNull(salvata.getGiornoInizio());
            assertTrue(festivitaService.calendario().festivo(LocalDate.of(2026, 11, 2)));

            festivitaService.impostaAttiva(salvata.getId(), false);
            assertFalse(festivitaService.calendario().festivo(LocalDate.of(2026, 11, 2)));
        } finally {
            festivitaService.elimina(salvata.getId());
        }
    }
}
