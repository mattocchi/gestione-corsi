package it.attocchi.service;

import it.attocchi.entity.Corso;
import it.attocchi.entity.Iscrizione;
import it.attocchi.entity.TipoQuota;
import it.attocchi.repository.IscrizioneRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Quota piena, pre-iscrizione (segue il prezzo del corso) o personalizzata. */
@SpringBootTest
class TipoQuotaTest {

    @Autowired
    private CorsoService corsoService;

    @Autowired
    private AllievoService allievoService;

    @Autowired
    private PagamentoService pagamentoService;

    @Autowired
    private IscrizioneRepository iscrizioneRepository;

    @Test
    @WithMockUser(roles = "SEGRETERIA")
    void preiscrizione_segueIlPrezzoDelCorso() {
        Corso canto = canto();
        Long allievoId = allievoService.findByUsername("allievo_canto3").orElseThrow().getId();
        // Senza prezzo di pre-iscrizione non si applica
        assertThrows(IllegalStateException.class,
            () -> pagamentoService.impostaTipoQuota(canto.getId(), allievoId, TipoQuota.PREISCRIZIONE));
        canto.setPrezzoPreiscrizione(new BigDecimal("510"));
        corsoService.save(canto);
        try {
            pagamentoService.impostaQuota(canto.getId(), allievoId, new BigDecimal("400"), "fratelli");
            pagamentoService.impostaTipoQuota(canto.getId(), allievoId, TipoQuota.PREISCRIZIONE);
            Iscrizione iscrizione = pagamentoService.findIscrizione(canto.getId(), allievoId);
            assertEquals(TipoQuota.PREISCRIZIONE, iscrizione.getTipoQuota());
            assertNull(iscrizione.getQuotaPersonalizzata());
            assertNull(iscrizione.getNotaQuota());
            assertEquals("pre-iscrizione", iscrizione.getMotivoQuota());
            assertEquals(0, new BigDecimal("510").compareTo(pagamentoService.situazione(canto.getId(), allievoId).quota()));

            // Cambiando il prezzo nel corso cambia la quota
            Corso modificato = corsoService.findById(canto.getId());
            modificato.setPrezzoPreiscrizione(new BigDecimal("500"));
            corsoService.save(modificato);
            assertEquals(0, new BigDecimal("500").compareTo(pagamentoService.situazione(canto.getId(), allievoId).quota()));

            // Il prezzo non si toglie finché qualcuno è in pre-iscrizione
            Corso senzaPrezzo = corsoService.findById(canto.getId());
            senzaPrezzo.setPrezzoPreiscrizione(null);
            assertThrows(IllegalStateException.class, () -> corsoService.save(senzaPrezzo));

            pagamentoService.impostaTipoQuota(canto.getId(), allievoId, TipoQuota.PIENA);
            assertEquals(TipoQuota.PIENA, pagamentoService.findIscrizione(canto.getId(), allievoId).getTipoQuota());
            assertNull(pagamentoService.findIscrizione(canto.getId(), allievoId).getMotivoQuota());
            assertEquals(0, new BigDecimal("600").compareTo(pagamentoService.situazione(canto.getId(), allievoId).quota()));
        } finally {
            pagamentoService.impostaQuota(canto.getId(), allievoId, null, null);
            Corso ripristino = corsoService.findById(canto.getId());
            ripristino.setPrezzoPreiscrizione(null);
            corsoService.save(ripristino);
        }
    }

    @Test
    @WithMockUser(roles = "SEGRETERIA")
    void convertiPreiscrizioni_soloQuoteUgualiAlPrezzoSenzaTipo() {
        Corso canto = canto();
        Long canto3 = allievoService.findByUsername("allievo_canto3").orElseThrow().getId();
        Long canto4 = allievoService.findByUsername("allievo_canto4").orElseThrow().getId();
        Long canto5 = allievoService.findByUsername("allievo_canto5").orElseThrow().getId();
        canto.setPrezzoPreiscrizione(new BigDecimal("510"));
        corsoService.save(canto);
        try {
            // Come prima di questa versione: quota personalizzata senza tipo salvato
            vecchiaQuota(canto.getId(), canto3, "510", "Pre-iscrizione");
            vecchiaQuota(canto.getId(), canto4, "510", "fratelli");
            vecchiaQuota(canto.getId(), canto5, "500", null);
            pagamentoService.convertiPreiscrizioni();
            assertEquals(TipoQuota.PREISCRIZIONE, pagamentoService.findIscrizione(canto.getId(), canto3).getTipoQuota());
            assertEquals(TipoQuota.PERSONALIZZATA, pagamentoService.findIscrizione(canto.getId(), canto4).getTipoQuota());
            assertEquals("fratelli", pagamentoService.findIscrizione(canto.getId(), canto4).getMotivoQuota());
            assertEquals(TipoQuota.PERSONALIZZATA, pagamentoService.findIscrizione(canto.getId(), canto5).getTipoQuota());
        } finally {
            for (Long id : new Long[] {canto3, canto4, canto5}) {
                pagamentoService.impostaQuota(canto.getId(), id, null, null);
            }
            Corso ripristino = corsoService.findById(canto.getId());
            ripristino.setPrezzoPreiscrizione(null);
            corsoService.save(ripristino);
        }
    }

    private void vecchiaQuota(Long corsoId, Long allievoId, String quota, String nota) {
        Iscrizione iscrizione = pagamentoService.findIscrizione(corsoId, allievoId);
        iscrizione.setTipoQuota(null);
        iscrizione.setQuotaPersonalizzata(new BigDecimal(quota));
        iscrizione.setNotaQuota(nota);
        iscrizioneRepository.save(iscrizione);
    }

    private Corso canto() {
        return corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow();
    }
}
