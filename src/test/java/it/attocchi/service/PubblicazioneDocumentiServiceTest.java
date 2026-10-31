package it.attocchi.service;

import it.attocchi.entity.TipoDocumento;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class PubblicazioneDocumentiServiceTest {

    private static final LocalDate OGGI = LocalDate.of(2026, 10, 31);

    @Autowired
    private PubblicazioneDocumentiService service;

    @Test
    void allAvvio_tipiDelleFamiglieVisibili() {
        PubblicazioneDocumentiService.PER_LE_FAMIGLIE.forEach(tipo -> assertTrue(service.visibile(tipo), tipo.name()));
        // I documenti che non sono delle famiglie non dipendono dagli interruttori
        assertTrue(service.visibile(TipoDocumento.LETTERA_INCARICO));
    }

    @Test
    void interruttore_soloStaff() {
        try {
            service.imposta(TipoDocumento.RICEVUTA, false, "segreteria");
            assertFalse(service.visibile(TipoDocumento.RICEVUTA));
        } finally {
            service.imposta(TipoDocumento.RICEVUTA, true, "admin");
        }
        assertThrows(IllegalStateException.class, () -> service.imposta(TipoDocumento.RICEVUTA, false, "genitore1"));
        assertThrows(IllegalArgumentException.class, () -> service.imposta(TipoDocumento.RIEPILOGO_CORSI, true, "admin"));
    }

    @Test
    void attestazioni_soloAnniFinitiEPubblicati() {
        try {
            assertFalse(service.attestazioneDisponibile(2020, OGGI));
            service.pubblicaAttestazioni(2020, true, OGGI, "segreteria");
            assertTrue(service.attestazioneDisponibile(2020, OGGI));
            assertTrue(service.anniAttestazioni(OGGI).stream().anyMatch(a -> a.anno() == 2020 && a.pubblicato()));
            // Con il tipo spento nessuna attestazione è disponibile
            service.imposta(TipoDocumento.ATTESTAZIONE, false, "admin");
            assertFalse(service.attestazioneDisponibile(2020, OGGI));
            service.imposta(TipoDocumento.ATTESTAZIONE, true, "admin");
            // L'anno in corso non si pubblica
            IllegalArgumentException errore = assertThrows(IllegalArgumentException.class,
                () -> service.pubblicaAttestazioni(2026, true, OGGI, "admin"));
            assertEquals("Le attestazioni del 2026 si possono pubblicare dal 1° gennaio 2027", errore.getMessage());
            assertThrows(IllegalStateException.class, () -> service.pubblicaAttestazioni(2020, false, OGGI, "genitore1"));
        } finally {
            service.pubblicaAttestazioni(2020, false, OGGI, "admin");
            service.imposta(TipoDocumento.ATTESTAZIONE, true, "admin");
        }
        assertFalse(service.attestazioneDisponibile(2020, OGGI));
    }
}
