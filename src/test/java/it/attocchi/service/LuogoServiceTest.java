package it.attocchi.service;

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
