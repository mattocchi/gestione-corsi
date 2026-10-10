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
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    private StudenteService studenteService;

    @Test
    void quotaPersonalizzataERateDeterminanoLaSituazione() {
        Corso canto = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow();
        Long studente = studenteService.findByUsername("studente_canto2").orElseThrow().getId();
        try {
            var iniziale = pagamentoService.situazione(canto.getId(), studente);
            assertEquals(0, new BigDecimal("600").compareTo(iniziale.quota()));
            assertEquals(StatoPagamento.NESSUN_PAGAMENTO, iniziale.stato());

            pagamentoService.impostaQuota(canto.getId(), studente, new BigDecimal("500"), "fratelli");
            assertEquals("fratelli", pagamentoService.findIscrizione(canto.getId(), studente).getNotaQuota());

            Pagamento rata = new Pagamento();
            rata.setData(LocalDate.of(2026, 10, 1));
            rata.setImporto(new BigDecimal("200"));
            pagamentoService.salva(canto.getId(), studente, rata);
            var parziale = pagamentoService.situazione(canto.getId(), studente);
            assertEquals(StatoPagamento.PARZIALE, parziale.stato());
            assertEquals(0, new BigDecimal("300").compareTo(parziale.residuo()));
            assertEquals(40, parziale.percentualePagata());
            assertEquals(MetodoPagamento.BONIFICO, pagamentoService.findPagamenti(canto.getId(), studente).getFirst().getMetodo());

            Pagamento saldo = new Pagamento();
            saldo.setData(LocalDate.of(2026, 11, 1));
            saldo.setImporto(new BigDecimal("300"));
            saldo.setMetodo(MetodoPagamento.CASSA);
            pagamentoService.salva(canto.getId(), studente, saldo);
            assertEquals(StatoPagamento.SALDATO, pagamentoService.situazioniDelCorso(canto.getId()).get(studente).stato());

            Pagamento negativo = new Pagamento();
            negativo.setData(LocalDate.now());
            negativo.setImporto(new BigDecimal("-1"));
            assertThrows(IllegalArgumentException.class, () -> pagamentoService.salva(canto.getId(), studente, negativo));
        } finally {
            pagamentoService.findPagamenti(canto.getId(), studente).forEach(pagamentoService::elimina);
            pagamentoService.impostaQuota(canto.getId(), studente, null, null);
        }
    }
}
