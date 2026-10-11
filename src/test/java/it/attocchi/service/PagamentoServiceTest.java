package it.attocchi.service;

import it.attocchi.entity.Corso;
import it.attocchi.entity.MetodoPagamento;
import it.attocchi.entity.Pagamento;
import it.attocchi.service.PagamentoService.StatoPagamento;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Quote e rate sui dati di test (Corso di Canto, quota piena 600 €).
 */
@SpringBootTest
class PagamentoServiceTest {

    @Autowired
    private PagamentoService pagamentoService;

    @Autowired
    private CorsoService corsoService;

    @Autowired
    private AllievoService allievoService;

    @Test
    void quotaPersonalizzataERateDeterminanoLaSituazione() {
        Corso canto = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow();
        Long allievo = allievoService.findByUsername("allievo_canto2").orElseThrow().getId();
        try {
            var iniziale = pagamentoService.situazione(canto.getId(), allievo);
            assertEquals(0, new BigDecimal("600").compareTo(iniziale.quota()));
            assertEquals(StatoPagamento.NESSUN_PAGAMENTO, iniziale.stato());

            pagamentoService.impostaQuota(canto.getId(), allievo, new BigDecimal("500"), "fratelli");
            assertEquals("fratelli", pagamentoService.findIscrizione(canto.getId(), allievo).getNotaQuota());

            Pagamento rata = new Pagamento();
            rata.setData(LocalDate.of(2026, 10, 1));
            rata.setImporto(new BigDecimal("200"));
            pagamentoService.salva(canto.getId(), allievo, rata);
            var parziale = pagamentoService.situazione(canto.getId(), allievo);
            assertEquals(StatoPagamento.PARZIALE, parziale.stato());
            assertEquals(0, new BigDecimal("300").compareTo(parziale.residuo()));
            assertEquals(40, parziale.percentualePagata());
            assertEquals(MetodoPagamento.BONIFICO, pagamentoService.findPagamenti(canto.getId(), allievo).getFirst().getMetodo());

            Pagamento saldo = new Pagamento();
            saldo.setData(LocalDate.of(2026, 11, 1));
            saldo.setImporto(new BigDecimal("300"));
            saldo.setMetodo(MetodoPagamento.CASSA);
            pagamentoService.salva(canto.getId(), allievo, saldo);
            assertEquals(StatoPagamento.SALDATO, pagamentoService.situazioniDelCorso(canto.getId()).get(allievo).stato());

            Pagamento negativo = new Pagamento();
            negativo.setData(LocalDate.now());
            negativo.setImporto(new BigDecimal("-1"));
            assertThrows(IllegalArgumentException.class, () -> pagamentoService.salva(canto.getId(), allievo, negativo));
        } finally {
            pagamentoService.findPagamenti(canto.getId(), allievo).forEach(pagamentoService::elimina);
            pagamentoService.impostaQuota(canto.getId(), allievo, null, null);
        }
    }

    @Test
    void righePagamenti_tutteLeIscrizioni_eSoloIFigliPerIlGenitore() {
        var righe = pagamentoService.righePagamenti();
        long iscrizioni = corsoService.findAll().stream().mapToLong(c -> c.getAllievi().size()).sum();
        assertEquals(iscrizioni, righe.size());
        assertEquals("Corso di Canto", righe.getFirst().corso().getNome());

        String genitore = righe.getFirst().genitore().getUser().getUsername();
        var figli = pagamentoService.righePagamentiDeiFigli(genitore);
        assertFalse(figli.isEmpty());
        assertTrue(figli.stream().allMatch(r -> r.genitore().getUser().getUsername().equals(genitore)));
        assertEquals(righe.stream().filter(r -> r.genitore() != null && r.genitore().getUser().getUsername().equals(genitore)).count(),
            figli.size());
    }

    @Test
    void conPagamentiNonSiRimuoveNeSiEliminaNulla() {
        Corso canto = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow();
        var allievo = allievoService.findByUsername("allievo_canto4").orElseThrow();
        Pagamento rata = new Pagamento();
        rata.setData(LocalDate.of(2026, 10, 1));
        rata.setImporto(new BigDecimal("100"));
        pagamentoService.salva(canto.getId(), allievo.getId(), rata);
        try {
            assertThrows(IllegalStateException.class, () -> corsoService.rimuoviIscrizione(canto.getId(), allievo.getId()));
            assertThrows(IllegalStateException.class, () -> corsoService.delete(canto));
            assertThrows(IllegalStateException.class, () -> allievoService.delete(allievo));
            assertEquals(7, corsoService.findPartecipanti(canto.getId()).size());
        } finally {
            pagamentoService.findPagamenti(canto.getId(), allievo.getId()).forEach(pagamentoService::elimina);
        }
    }
}
