package it.attocchi.service;

import it.attocchi.entity.Corso;
import it.attocchi.entity.OpzioneCorso;
import it.attocchi.service.PagamentoService.Situazione;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Opzioni aggiuntive del corso (es. il kit) attivate per iscritto: si sommano alla quota del corso. */
@SpringBootTest
class OpzioniCorsoTest {

    @Autowired
    private CorsoService corsoService;

    @Autowired
    private AllievoService allievoService;

    @Autowired
    private PagamentoService pagamentoService;

    @Test
    @WithMockUser(roles = "SEGRETERIA")
    void opzioni_siSommanoAllaQuotaDiChiLeAttiva() {
        Corso canto = canto();
        Long canto3 = allievoService.findByUsername("allievo_canto3").orElseThrow().getId();
        Long canto4 = allievoService.findByUsername("allievo_canto4").orElseThrow().getId();
        try {
            corsoService.salva(canto, List.of(new OpzioneCorso(null, " Kit ", new BigDecimal("30")),
                new OpzioneCorso(null, "Gita", new BigDecimal("15"))));
            List<OpzioneCorso> opzioni = corsoService.opzioni(canto.getId());
            assertEquals(List.of("Kit", "Gita"), opzioni.stream().map(OpzioneCorso::getDescrizione).toList());
            OpzioneCorso kit = opzioni.getFirst();

            corsoService.impostaOpzione(canto.getId(), canto3, kit.getId(), true);
            Situazione situazione = pagamentoService.situazione(canto.getId(), canto3);
            assertEquals(0, new BigDecimal("630").compareTo(situazione.quota()));
            assertEquals(0, new BigDecimal("600").compareTo(situazione.quotaCorso()));
            assertEquals(List.of(kit), situazione.opzioni());
            // Chi non l'attiva paga la quota del corso
            assertEquals(0, new BigDecimal("600").compareTo(pagamentoService.situazione(canto.getId(), canto4).quota()));

            // Con la quota personalizzata (es. pre-iscrizione) l'opzione si somma
            pagamentoService.impostaQuota(canto.getId(), canto3, new BigDecimal("510"), "Pre-iscrizione");
            assertEquals(0, new BigDecimal("540").compareTo(pagamentoService.situazione(canto.getId(), canto3).quota()));

            // L'importo è quello del corso: cambiandolo cambia per chi l'ha attiva
            List<OpzioneCorso> modificate = new ArrayList<>(corsoService.opzioni(canto.getId()));
            modificate.getFirst().setImporto(new BigDecimal("35"));
            corsoService.salva(corsoService.findById(canto.getId()), modificate);
            assertEquals(0, new BigDecimal("545").compareTo(pagamentoService.situazione(canto.getId(), canto3).quota()));

            // Importo e descrizione obbligatori; la segreteria non elimina
            assertThrows(IllegalArgumentException.class, () -> corsoService.salva(corsoService.findById(canto.getId()),
                List.of(new OpzioneCorso(null, "Senza importo", null))));
            assertThrows(IllegalArgumentException.class, () -> corsoService.salva(corsoService.findById(canto.getId()),
                List.of(new OpzioneCorso(null, " ", BigDecimal.ONE))));
            assertThrows(IllegalStateException.class, () -> corsoService.salva(corsoService.findById(canto.getId()),
                List.of(modificate.getFirst())));
            assertEquals(2, corsoService.opzioni(canto.getId()).size());
        } finally {
            pagamentoService.impostaQuota(canto.getId(), canto3, null, null);
            corsoService.opzioni(canto.getId()).forEach(o -> corsoService.impostaOpzione(canto.getId(), canto3, o.getId(), false));
            eliminaOpzioni(canto);
        }
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_eliminaSoloLeOpzioniNonAttive_eDuplicaLeCopia() {
        Corso canto = canto();
        Long canto3 = allievoService.findByUsername("allievo_canto3").orElseThrow().getId();
        corsoService.salva(canto, List.of(new OpzioneCorso(null, "Kit", new BigDecimal("30"))));
        OpzioneCorso kit = corsoService.opzioni(canto.getId()).getFirst();
        Corso copia = null;
        try {
            corsoService.impostaOpzione(canto.getId(), canto3, kit.getId(), true);
            IllegalStateException attiva = assertThrows(IllegalStateException.class,
                () -> corsoService.salva(corsoService.findById(canto.getId()), List.of()));
            assertTrue(attiva.getMessage().contains("attiva per 1 iscritto"), attiva.getMessage());

            copia = corsoService.duplica(canto.getId());
            List<OpzioneCorso> copiate = corsoService.opzioni(copia.getId());
            assertEquals(1, copiate.size());
            assertEquals("Kit", copiate.getFirst().getDescrizione());
            assertTrue(copiate.getFirst().getId() > kit.getId());
        } finally {
            if (copia != null) {
                // Il corso si elimina con le sue opzioni
                corsoService.delete(corsoService.findById(copia.getId()));
            }
            corsoService.impostaOpzione(canto.getId(), canto3, kit.getId(), false);
            eliminaOpzioni(canto);
        }
        assertTrue(corsoService.opzioni(canto.getId()).isEmpty());
    }

    private void eliminaOpzioni(Corso canto) {
        // Solo l'admin elimina: il test lo imposta per pulire
        SecurityContext prima = SecurityContextHolder.getContext();
        SecurityContext admin = SecurityContextHolder.createEmptyContext();
        admin.setAuthentication(new UsernamePasswordAuthenticationToken("admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        SecurityContextHolder.setContext(admin);
        try {
            corsoService.salva(corsoService.findById(canto.getId()), List.of());
        } finally {
            SecurityContextHolder.setContext(prima);
        }
    }

    private Corso canto() {
        return corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow();
    }
}
