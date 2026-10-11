package it.attocchi.service;

import it.attocchi.entity.Insegnante;
import it.attocchi.entity.MetodoPagamento;
import it.attocchi.entity.Rimborso;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class RimborsoServiceTest {

    @Autowired
    private RimborsoService rimborsoService;

    @Autowired
    private InsegnanteService insegnanteService;

    @Test
    void anniScolastici_daSettembreAdAgosto() {
        assertEquals(LocalDate.of(2026, 9, 1), RimborsoService.inizioAnno("2026/2027"));
        assertEquals(LocalDate.of(2027, 8, 31), RimborsoService.fineAnno("2026/2027"));
    }

    @Test
    void salva_validaEFiltraPerAnnoDelMeseDiRiferimento() {
        Insegnante insegnante = insegnanteService.findByUsername("insegnante2").orElseThrow();
        assertThrows(IllegalArgumentException.class, () -> rimborsoService.salva(insegnante.getId(), rimborso("0", YearMonth.of(2026, 10))));
        assertThrows(IllegalArgumentException.class, () -> rimborsoService.salva(insegnante.getId(), rimborso("10", null)));

        Rimborso agosto = rimborsoService.salva(insegnante.getId(), rimborso("100", YearMonth.of(2026, 8)));
        Rimborso settembre = rimborsoService.salva(insegnante.getId(), rimborso("150.50", YearMonth.of(2026, 9)));
        Rimborso ottobre = rimborso("200", YearMonth.of(2026, 10));
        ottobre.setNota("   ");
        ottobre = rimborsoService.salva(insegnante.getId(), ottobre);
        try {
            List<Rimborso> anno = rimborsoService.rimborsi(insegnante.getId(), "2026/2027");
            assertEquals(List.of(ottobre, settembre), anno);
            assertNull(anno.getFirst().getNota());
            assertEquals(0, new BigDecimal("350.50").compareTo(rimborsoService.totale(insegnante.getId(), "2026/2027")));
            assertEquals(0, new BigDecimal("100").compareTo(rimborsoService.totale(insegnante.getId(), "2025/2026")));
        } finally {
            for (Rimborso r : List.of(agosto, settembre, ottobre)) {
                rimborsoService.elimina(r.getId());
            }
        }
        assertEquals(0, rimborsoService.totale(insegnante.getId(), "2026/2027").signum());
    }

    private static Rimborso rimborso(String importo, YearMonth mese) {
        Rimborso rimborso = new Rimborso();
        rimborso.setData(LocalDate.of(2026, 10, 5));
        rimborso.setMese(mese);
        rimborso.setImporto(new BigDecimal(importo));
        rimborso.setMetodo(MetodoPagamento.BONIFICO);
        return rimborso;
    }
}
