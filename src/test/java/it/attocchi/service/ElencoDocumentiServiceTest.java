package it.attocchi.service;

import it.attocchi.entity.Allievo;
import it.attocchi.entity.Corso;
import it.attocchi.entity.MetodoPagamento;
import it.attocchi.entity.Pagamento;
import it.attocchi.entity.TipoDocumento;
import it.attocchi.repository.InvioEmailRepository;
import it.attocchi.service.ElencoDocumentiService.DocumentoFamiglia;
import it.attocchi.service.ElencoDocumentiService.DocumentoIscrizione;
import it.attocchi.service.ElencoDocumentiService.DocumentoRiepilogo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Elenchi della sezione Documenti sui dati di DataInitializer: Canto (7 iscritti) e Chitarra (6), anno 2024/2025;
 * genitore3 ha Allievo Canto3 (Canto) e Allievo Chitarra1 (Chitarra).
 */
@SpringBootTest
class ElencoDocumentiServiceTest {

    @Autowired
    private ElencoDocumentiService elencoDocumentiService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PagamentoService pagamentoService;

    @Autowired
    private AllievoService allievoService;

    @Autowired
    private CorsoService corsoService;

    @Autowired
    private InvioEmailRepository invioEmailRepository;

    @AfterEach
    void pulisci() {
        invioEmailRepository.deleteAll();
    }

    @Test
    void conferme_ogniIscrizioneConDestinatarioEUltimoInvio() {
        Corso canto = canto();
        Allievo allievo = allievoService.findByUsername("allievo_canto3").orElseThrow();
        List<DocumentoIscrizione> conferme = elencoDocumentiService.documentiIscrizioni(TipoDocumento.CONFERMA_ISCRIZIONE, "segreteria");
        assertEquals(13, conferme.size());
        // Per corso (Canto prima di Chitarra) e nell'ordine dei partecipanti (Canto5 alle 17:00 per primo)
        assertEquals("Allievo Canto5", conferme.getFirst().iscrizione().getAllievo().getNomeCompleto());
        DocumentoIscrizione canto3 = conferma(conferme, allievo);
        assertEquals("genitore3@example.com", canto3.destinatario().orElseThrow().email());
        assertNull(canto3.datiMancanti());
        assertNull(canto3.ultimoInvio());

        emailService.invia(TipoDocumento.CONFERMA_ISCRIZIONE, canto.getId(), allievo.getId(), "segreteria");
        canto3 = conferma(elencoDocumentiService.documentiIscrizioni(TipoDocumento.CONFERMA_ISCRIZIONE, "segreteria"), allievo);
        assertNotNull(canto3.ultimoInvio());
        assertEquals("genitore3@example.com", canto3.ultimoInvio().getDestinatario());

        assertThrows(IllegalStateException.class,
            () -> elencoDocumentiService.documentiIscrizioni(TipoDocumento.CONFERMA_ISCRIZIONE, "genitore3"));
        assertThrows(IllegalArgumentException.class,
            () -> elencoDocumentiService.documentiIscrizioni(TipoDocumento.RIEPILOGO, "segreteria"));
    }

    @Test
    void ricevute_soloLeIscrizioniConPagamenti() {
        Corso canto = canto();
        Allievo allievo = allievoService.findByUsername("allievo_canto3").orElseThrow();
        int prima = elencoDocumentiService.documentiIscrizioni(TipoDocumento.RICEVUTA, "admin").size();
        Pagamento rata = new Pagamento();
        rata.setData(LocalDate.now());
        rata.setImporto(new BigDecimal("100"));
        rata.setMetodo(MetodoPagamento.CASSA);
        pagamentoService.salva(canto.getId(), allievo.getId(), rata);
        try {
            List<DocumentoIscrizione> ricevute = elencoDocumentiService.documentiIscrizioni(TipoDocumento.RICEVUTA, "admin");
            assertEquals(prima + 1, ricevute.size());
            assertTrue(ricevute.stream().allMatch(d -> emailService.haPagamenti(d.iscrizione().getCorso().getId(),
                d.iscrizione().getAllievo().getId())));
            // La famiglia ora ha anche la ricevuta di Canto3
            assertTrue(elencoDocumentiService.documentiDi("genitore3").stream()
                .anyMatch(d -> d.tipo() == TipoDocumento.RICEVUTA && d.iscrizione().getAllievo().getId().equals(allievo.getId())));
        } finally {
            pagamentoService.findPagamenti(canto.getId(), allievo.getId()).forEach(pagamentoService::elimina);
        }
    }

    @Test
    void riepiloghi_unoPerFamigliaEAnnoConUltimoInvio() {
        List<DocumentoRiepilogo> riepiloghi = elencoDocumentiService.riepiloghi("segreteria");
        DocumentoRiepilogo famiglia3 = riepiloghi.stream()
            .filter(r -> r.genitore() != null && r.genitore().getUser().getUsername().equals("genitore3")).findFirst().orElseThrow();
        assertEquals("2024/2025", famiglia3.anno());
        assertEquals(List.of("Allievo Canto3", "Allievo Chitarra1"), famiglia3.allievi());
        assertEquals(2, famiglia3.corsi());
        assertEquals("genitore3@example.com", famiglia3.destinatario().orElseThrow().email());
        assertNull(famiglia3.ultimoInvio());
        // Nessun allievo è stato iscritto due volte con lo stesso genitore e anno
        assertEquals(riepiloghi.size(), riepiloghi.stream().map(r -> r.intestatario() + r.anno()).distinct().count());

        emailService.inviaRiepilogo(famiglia3.genitore().getId(), null, "2024/2025", "segreteria");
        assertNotNull(elencoDocumentiService.riepiloghi("segreteria").stream()
            .filter(r -> r.genitore() != null && r.genitore().getId().equals(famiglia3.genitore().getId()))
            .findFirst().orElseThrow().ultimoInvio());
        assertThrows(IllegalStateException.class, () -> elencoDocumentiService.riepiloghi("insegnante1"));
    }

    @Test
    void documentiDellaFamiglia_riepilogoEConfermeDeiFigli() {
        List<DocumentoFamiglia> documenti = elencoDocumentiService.documentiDi("genitore3");
        assertEquals(TipoDocumento.RIEPILOGO, documenti.getFirst().tipo());
        assertEquals("2024/2025", documenti.getFirst().anno());
        assertNotNull(documenti.getFirst().genitore());
        List<DocumentoFamiglia> conferme = documenti.stream().filter(d -> d.tipo() == TipoDocumento.CONFERMA_ISCRIZIONE).toList();
        assertEquals(List.of("Allievo Canto3", "Allievo Chitarra1"),
            conferme.stream().map(d -> d.iscrizione().getAllievo().getNomeCompleto()).toList());
        // L'allievo con genitore non ha documenti propri; lo staff senza figli neppure
        assertTrue(elencoDocumentiService.documentiDi("allievo_canto3").isEmpty());
        assertTrue(elencoDocumentiService.documentiDi("segreteria").isEmpty());
    }

    @Test
    void attestazioni_unaPerAllievoEAnnoSolareConUltimoInvio() {
        Corso canto = canto();
        Allievo allievo = allievoService.findByUsername("allievo_canto3").orElseThrow();
        Pagamento rata = new Pagamento();
        rata.setData(LocalDate.of(2025, 3, 10));
        rata.setImporto(new BigDecimal("120"));
        rata.setMetodo(MetodoPagamento.BONIFICO);
        pagamentoService.salva(canto.getId(), allievo.getId(), rata);
        try {
            ElencoDocumentiService.DocumentoAttestazione attestazione = elencoDocumentiService.attestazioni("segreteria").stream()
                .filter(a -> a.allievo().getId().equals(allievo.getId())).findFirst().orElseThrow();
            assertEquals(2025, attestazione.anno());
            assertEquals(0, new BigDecimal("120").compareTo(attestazione.totale()));
            assertEquals("genitore3@example.com", attestazione.destinatario().orElseThrow().email());
            assertNull(attestazione.ultimoInvio());
            assertNull(attestazione.datiMancanti());
            assertTrue(attestazione.inEta());

            emailService.inviaAttestazione(allievo.getId(), 2025, "segreteria");
            assertNotNull(elencoDocumentiService.attestazioni("segreteria").stream()
                .filter(a -> a.allievo().getId().equals(allievo.getId())).findFirst().orElseThrow().ultimoInvio());
            assertThrows(IllegalStateException.class, () -> emailService.inviaAttestazione(allievo.getId(), 2023, "segreteria"));
            assertThrows(IllegalStateException.class, () -> elencoDocumentiService.attestazioni("insegnante1"));

            // Senza luogo di nascita la dichiarazione non si emette né si invia
            allievo.setLuogoNascita(null);
            allievoService.save(allievo);
            try {
                assertEquals("Per la dichiarazione mancano: luogo di nascita dell'allievo", elencoDocumentiService.attestazioni("segreteria")
                    .stream().filter(a -> a.allievo().getId().equals(allievo.getId())).findFirst().orElseThrow().datiMancanti());
                assertThrows(IllegalStateException.class, () -> emailService.inviaAttestazione(allievo.getId(), 2025, "segreteria"));
            } finally {
                allievo.setLuogoNascita("Roma");
                allievoService.save(allievo);
            }

            // Il genitore la trova tra i suoi documenti
            assertTrue(elencoDocumentiService.documentiDi("genitore3").stream().anyMatch(d -> d.tipo() == TipoDocumento.ATTESTAZIONE
                && "2025".equals(d.anno()) && d.allievo().getId().equals(allievo.getId())));
        } finally {
            pagamentoService.findPagamenti(canto.getId(), allievo.getId()).forEach(pagamentoService::elimina);
        }
    }

    private static DocumentoIscrizione conferma(List<DocumentoIscrizione> documenti, Allievo allievo) {
        return documenti.stream().filter(d -> d.iscrizione().getAllievo().getId().equals(allievo.getId())).findFirst().orElseThrow();
    }

    private Corso canto() {
        return corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow();
    }
}
