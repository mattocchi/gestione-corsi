package it.attocchi.service;

import it.attocchi.entity.Corso;
import it.attocchi.entity.Luogo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class LuogoServiceTest {

    @Autowired
    private LuogoService luogoService;

    @Autowired
    private CorsoService corsoService;

    @Test
    void corso_predefinitoSempreAbilitatoELuoghiUsatiNonEliminabili() {
        Luogo esterno = luogoService.salva(new Luogo("Esterno"));
        Corso canto = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow();
        Luogo prima = canto.getLuogo();
        assertEquals("Aula 1", prima.getNome());
        try {
            canto.setLuogo(esterno);
            corsoService.save(canto);
            Corso salvato = corsoService.findById(canto.getId());
            assertEquals(esterno, salvato.getLuogo());
            assertTrue(salvato.getLuoghiAbilitati().contains(esterno));
            assertTrue(luogoService.usati().contains(esterno.getId()));
            assertThrows(IllegalStateException.class, () -> luogoService.elimina(esterno.getId()));
        } finally {
            Corso ripristino = corsoService.findById(canto.getId());
            ripristino.setLuogo(prima);
            ripristino.getLuoghiAbilitati().remove(esterno);
            corsoService.save(ripristino);
            luogoService.elimina(esterno.getId());
        }
    }

    @Test
    void salva_nomeObbligatorioEUnicoSenzaMaiuscole() {
        Luogo sala = luogoService.salva(new Luogo("  Sala prove "));
        try {
            assertEquals("Sala prove", sala.getNome());
            assertThrows(IllegalArgumentException.class, () -> luogoService.salva(new Luogo("sala PROVE")));
            assertThrows(IllegalArgumentException.class, () -> luogoService.salva(new Luogo(" ")));

            // Rinominare con lo stesso nome è permesso
            sala.setNome("Sala Prove");
            assertEquals("Sala Prove", luogoService.salva(sala).getNome());

            luogoService.impostaAttivo(sala.getId(), false);
            assertFalse(luogoService.attivi().stream().anyMatch(l -> l.getId().equals(sala.getId())));
            assertTrue(luogoService.elenco().stream().anyMatch(l -> l.getId().equals(sala.getId())));
        } finally {
            luogoService.elimina(sala.getId());
        }
        assertFalse(luogoService.elenco().stream().anyMatch(l -> l.getId().equals(sala.getId())));
    }
}
