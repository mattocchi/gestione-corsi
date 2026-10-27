package it.attocchi.service;

import it.attocchi.entity.AnnoScolastico;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Elenco degli anni scolastici, popolato all'avvio sui dati di DataInitializer (profilo dev). */
@SpringBootTest
class AnnoScolasticoServiceTest {

    @Autowired
    private AnnoScolasticoService annoScolasticoService;

    @Test
    void avvio_popolaAnniAttorno_aOggiEQuelliDeiCorsi() {
        LocalDate oggi = LocalDate.now();
        List<String> nomi = annoScolasticoService.elenco().stream().map(AnnoScolastico::getNome).toList();
        assertTrue(nomi.containsAll(List.of(AnnoScolastico.di(oggi.minusYears(1)), AnnoScolastico.di(oggi),
            AnnoScolastico.di(oggi.plusYears(1)), "2024/2025")));
        assertTrue(annoScolasticoService.usati().contains("2024/2025"));

        // Già popolato: non aggiunge altro
        int quanti = nomi.size();
        annoScolasticoService.inizializzaSeVuoto(oggi.plusYears(10));
        assertEquals(quanti, annoScolasticoService.elenco().size());
    }

    @Test
    void crea_validaFormatoEUnicita() {
        assertThrows(IllegalArgumentException.class, () -> annoScolasticoService.crea("2030/2032"));
        assertThrows(IllegalArgumentException.class, () -> annoScolasticoService.crea("2024/2025"));

        AnnoScolastico nuovo = annoScolasticoService.crea(" 2040/2041 ");
        try {
            assertEquals("2040/2041", nuovo.getNome());
            assertTrue(nuovo.isAttivo());
            assertTrue(annoScolasticoService.scelteCorso(null).contains("2040/2041"));
        } finally {
            annoScolasticoService.elimina(nuovo.getId());
        }
    }

    @Test
    void disattivato_nonSiSceglieMaRestaSulCorsoCheLoHa() {
        AnnoScolastico nuovo = annoScolasticoService.crea("2041/2042");
        try {
            annoScolasticoService.impostaAttivo(nuovo.getId(), false);
            assertFalse(annoScolasticoService.scelteCorso(null).contains("2041/2042"));
            List<String> scelte = annoScolasticoService.scelteCorso("2041/2042");
            assertTrue(scelte.contains("2041/2042"));
            assertEquals(scelte.stream().sorted().toList(), scelte);
        } finally {
            annoScolasticoService.elimina(nuovo.getId());
        }
    }

    @Test
    void elimina_bloccatoSeUsatoDaUnCorso() {
        AnnoScolastico usato = annoScolasticoService.elenco().stream()
            .filter(a -> a.getNome().equals("2024/2025")).findFirst().orElseThrow();
        assertThrows(IllegalStateException.class, () -> annoScolasticoService.elimina(usato.getId()));
    }

    @Test
    void perFiltro_ancheGliAnniNonAttiviEQuelliDeiDati() {
        AnnoScolastico vecchio = annoScolasticoService.crea("2019/2020");
        annoScolasticoService.impostaAttivo(vecchio.getId(), false);
        try {
            List<String> anni = annoScolasticoService.perFiltro(List.of("2010/2011"));
            assertTrue(anni.contains("2019/2020"), anni.toString());
            assertTrue(anni.contains("2010/2011"), anni.toString());
            assertEquals("2010/2011", anni.getLast());
            assertTrue(annoScolasticoService.nonAttivi().contains("2019/2020"));
            // Per i corsi nuovi solo gli attivi
            assertFalse(annoScolasticoService.scelteCorso(null).contains("2019/2020"));
        } finally {
            annoScolasticoService.elimina(vecchio.getId());
        }
    }
}
