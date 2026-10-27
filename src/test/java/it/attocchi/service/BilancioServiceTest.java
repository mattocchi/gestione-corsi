package it.attocchi.service;

import it.attocchi.entity.Corso;
import it.attocchi.entity.Insegnante;
import it.attocchi.entity.TipoCorso;
import it.attocchi.entity.Lezione;
import it.attocchi.entity.MetodoPagamento;
import it.attocchi.entity.Pagamento;
import it.attocchi.entity.Rimborso;
import it.attocchi.service.BilancioService.Bilancio;
import it.attocchi.service.BilancioService.BilancioCorso;
import it.attocchi.service.BilancioService.RigaRimborsi;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Bilancio sui dati di DataInitializer: insegnante1 (25 €/h) ha il Corso di Canto 2024/2025 con 7 allievi,
 * 30 lezioni da 40 minuti e quota 600 €.
 */
@SpringBootTest
class BilancioServiceTest {

    @Autowired
    private BilancioService bilancioService;

    @Autowired
    private InsegnanteService insegnanteService;

    @Autowired
    private LezioneService lezioneService;

    @Autowired
    private CorsoService corsoService;

    @Autowired
    private PagamentoService pagamentoService;

    @Autowired
    private RimborsoService rimborsoService;

    @Test
    void oreDonate_soloSenzaTariffaSvolteEPreviste() {
        // Con la tariffa niente ore donate
        assertTrue(bilancioService.oreDonate("insegnante1").isEmpty());
        assertTrue(bilancioService.oreDonateTotali().isEmpty());
        Insegnante insegnante = insegnanteService.findByUsername("insegnante1").orElseThrow();
        Long cantoId = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow().getId();
        Lezione prima = lezioneService.findByCorso(cantoId).getFirst();
        insegnante.setTariffaOraria(null);
        insegnante.setRimborsoForfettario(new BigDecimal("300"));
        insegnanteService.save(insegnante);
        try {
            // Canto: 30 lezioni × 7 allievi × 40 minuti, nessuna ancora svolta; il forfait non conta
            assertEquals(new BilancioService.OreDonate(0, 30 * 7 * 40), bilancioService.oreDonate("insegnante1").orElseThrow());
            lezioneService.completa(prima.getId(), "insegnante1");
            assertEquals(new BilancioService.OreDonate(7 * 40, 30 * 7 * 40), bilancioService.oreDonate("insegnante1").orElseThrow());
            // Il totale per admin e segreteria: il solo insegnante senza tariffa
            assertEquals(new BilancioService.OreDonateTotali(7 * 40, 30 * 7 * 40, 1), bilancioService.oreDonateTotali().orElseThrow());
        } finally {
            Insegnante ripristino = insegnanteService.findByUsername("insegnante1").orElseThrow();
            ripristino.setRimborsoForfettario(null);
            ripristino.setTariffaOraria(new BigDecimal("25.00"));
            insegnanteService.save(ripristino);
            Lezione daRiaprire = lezioneService.findById(prima.getId());
            daRiaprire.setCompletata(false);
            lezioneService.save(daRiaprire);
        }
    }

    @Test
    void costo_minutiPerTariffaOraria() {
        // 7 allievi × 40 minuti = 280 minuti = 4,67 ore × 25 €/h
        assertEquals(new BigDecimal("116.67"), BilancioService.costo(280, new BigDecimal("25")));
        assertEquals(new BigDecimal("3500.00"), BilancioService.costo(30 * 280, new BigDecimal("25")));
        assertEquals(new BigDecimal("0.00"), BilancioService.costo(280, null));
    }

    @Test
    void bilancio_effettivoMaturatoETeorico() {
        Insegnante insegnante = insegnanteService.findByUsername("insegnante1").orElseThrow();
        Long cantoId = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow().getId();
        List<Lezione> lezioni = lezioneService.findByCorso(cantoId);
        Lezione completata = lezioni.get(0);
        Lezione saltata = lezioni.get(1);
        Long allievoId = corsoService.findPartecipanti(cantoId).getFirst().getAllievo().getId();

        Bilancio iniziale = bilancioService.bilancio(insegnante.getId(), "2024/2025");
        assertFalse(iniziale.senzaTariffa());
        assertEquals(1, iniziale.corsi().size());
        BilancioCorso canto = iniziale.corsi().getFirst();
        assertEquals(7, canto.iscritti());
        assertEquals(0, new BigDecimal("4200").compareTo(canto.incassoPrevisto()));
        assertEquals(new BigDecimal("3500.00"), canto.costoPrevisto());
        assertEquals(0, iniziale.costoMaturato().signum());
        assertEquals(0, new BigDecimal("700").compareTo(iniziale.marginePrevisto()));
        assertEquals(0, bilancioService.bilancio(insegnante.getId(), "2025/2026").corsi().size());

        lezioneService.completa(completata.getId(), "insegnante1");
        lezioneService.segnaNonEffettuata(saltata.getId(), null, "insegnante1");
        Pagamento rata = new Pagamento();
        rata.setData(LocalDate.of(2024, 10, 10));
        rata.setImporto(new BigDecimal("200"));
        rata.setMetodo(MetodoPagamento.CASSA);
        pagamentoService.salva(cantoId, allievoId, rata);
        YearMonth mese = YearMonth.from(completata.getData());
        Rimborso rimborso = new Rimborso();
        rimborso.setData(LocalDate.of(2024, 11, 5));
        rimborso.setMese(mese);
        rimborso.setImporto(new BigDecimal("100"));
        rimborso.setMetodo(MetodoPagamento.BONIFICO);
        rimborsoService.salva(insegnante.getId(), rimborso);
        try {
            Bilancio bilancio = bilancioService.bilancio(insegnante.getId(), "2024/2025");
            assertEquals(0, new BigDecimal("200").compareTo(bilancio.incassato()));
            assertEquals(0, new BigDecimal("100").compareTo(bilancio.rimborsato()));
            assertEquals(0, new BigDecimal("100").compareTo(bilancio.saldo()));
            assertEquals(new BigDecimal("116.67"), bilancio.costoMaturato());
            assertEquals(0, new BigDecimal("16.67").compareTo(bilancio.daRimborsare()));
            // La lezione non effettuata non costa: 29 lezioni × 280 minuti
            assertEquals(new BigDecimal("3383.33"), bilancio.costoPrevisto());

            RigaRimborsi riga = bilancioService.prospetto("2024/2025").stream()
                .filter(r -> r.insegnante().getId().equals(insegnante.getId())).findFirst().orElseThrow();
            assertEquals(new BigDecimal("116.67"), riga.maturato());
            assertEquals(0, new BigDecimal("16.67").compareTo(riga.daRimborsare()));

            // Proposta: maturato del mese meno quanto già rimborsato per quel mese
            assertEquals(0, new BigDecimal("16.67").compareTo(bilancioService.propostaRimborso(insegnante.getId(), mese)));
            assertEquals(0, bilancioService.propostaRimborso(insegnante.getId(), mese.plusMonths(6)).signum());
        } finally {
            rimborsoService.elimina(rimborso.getId());
            pagamentoService.findPagamenti(cantoId, allievoId).forEach(pagamentoService::elimina);
            lezioneService.ripristina(saltata.getId(), "insegnante1");
            Lezione daRiaprire = lezioneService.findById(completata.getId());
            daRiaprire.setCompletata(false);
            lezioneService.save(daRiaprire);
        }
    }

    @Test
    void servizio_ogniLezioneValeLaSuaDurataSenzaIscritti() {
        Insegnante insegnante = insegnanteService.findByUsername("insegnante1").orElseThrow();
        Corso pulizia = new Corso();
        pulizia.setTipo(TipoCorso.SERVIZIO);
        pulizia.setNome("Pulizia");
        pulizia.setDataInizio(LocalDate.of(2025, 9, 2));
        pulizia.setAnnoScolastico("2025/2026");
        pulizia.setNumeroLezioni(2);
        pulizia.setDurataLezioneMinuti(120);
        pulizia.setGiorniSettimana(new HashSet<>(Set.of(DayOfWeek.TUESDAY)));
        pulizia.setInsegnante(insegnante);
        pulizia = corsoService.save(pulizia);
        try {
            // Senza lezioni generate: stima su numero di lezioni × durata
            assertEquals(new BigDecimal("100.00"), bilancioService.costiPrevistiPerCorso().get(pulizia.getId()));
            corsoService.generaLezioni(pulizia);
            Lezione prima = lezioneService.findByCorso(pulizia.getId()).getFirst();
            lezioneService.completa(prima.getId(), "insegnante1");

            // 25 €/h: 2 lezioni da 2 h previste, una svolta
            Bilancio bilancio = bilancioService.bilancio(insegnante.getId(), "2025/2026");
            assertEquals(new BigDecimal("100.00"), bilancio.costoPrevisto());
            assertEquals(new BigDecimal("50.00"), bilancio.costoMaturato());
            assertEquals(0, new BigDecimal("50.00").compareTo(
                bilancioService.propostaRimborso(insegnante.getId(), YearMonth.from(prima.getData()))));
        } finally {
            corsoService.delete(corsoService.findById(pulizia.getId()));
        }
    }

    @Test
    void lezioniDelMese_perGiornoConOreEPreventivo() {
        Insegnante insegnante = insegnanteService.findByUsername("insegnante1").orElseThrow();
        Long cantoId = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow().getId();
        Lezione prima = lezioneService.findByCorso(cantoId).getFirst();
        YearMonth mese = YearMonth.from(prima.getData());
        List<Lezione> delMese = lezioneService.findByCorso(cantoId).stream()
            .filter(l -> YearMonth.from(l.getData()).equals(mese)).toList();
        Lezione saltata = delMese.get(1);

        lezioneService.completa(prima.getId(), "insegnante1");
        lezioneService.segnaNonEffettuata(saltata.getId(), null, "insegnante1");
        try {
            BilancioService.LezioniDelMese lezioni = bilancioService.lezioniDelMese(insegnante.getId(), mese);
            // La non effettuata non compare; 7 allievi × 40 minuti al giorno, 25 €/h
            assertEquals(delMese.size() - 1, lezioni.giorni().size());
            BilancioService.GiornoDiLezioni giorno = lezioni.giorni().getFirst();
            assertEquals(prima.getData(), giorno.data());
            assertEquals("Corso di Canto", giorno.corsi());
            assertEquals(7, giorno.allievi());
            assertEquals(280, giorno.minuti());
            assertTrue(giorno.svolto());
            assertFalse(lezioni.giorni().get(1).svolto());
            assertEquals(280L * (delMese.size() - 1), lezioni.minuti());
            assertEquals(280, lezioni.minutiSvolti());
            assertEquals(new BigDecimal("116.67"), lezioni.maturato());
            assertEquals(BilancioService.costo(lezioni.minuti(), new BigDecimal("25")), lezioni.preventivo());
            assertTrue(bilancioService.lezioniDelMese(insegnante.getId(), mese.minusMonths(3)).giorni().isEmpty());
        } finally {
            lezioneService.ripristina(saltata.getId(), "insegnante1");
            Lezione daRiaprire = lezioneService.findById(prima.getId());
            daRiaprire.setCompletata(false);
            lezioneService.save(daRiaprire);
        }
    }

    @Test
    void forfaitAnnuo_siSommaAlCostoOrarioSoloNegliAnniConCorsi() {
        Insegnante insegnante = insegnanteService.findByUsername("insegnante1").orElseThrow();
        Long cantoId = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow().getId();
        Lezione completata = lezioneService.findByCorso(cantoId).getFirst();
        lezioneService.completa(completata.getId(), "insegnante1");
        insegnante.setRimborsoForfettario(new BigDecimal("300"));
        insegnanteService.save(insegnante);
        try {
            // Tariffa e forfait si sommano: 116,67 € di lezione + 300 € maturati da inizio anno
            Bilancio bilancio = bilancioService.bilancio(insegnante.getId(), "2024/2025");
            assertEquals(0, new BigDecimal("300").compareTo(bilancio.forfait()));
            assertEquals(new BigDecimal("116.67"), bilancio.costoLezioniMaturato());
            assertEquals(new BigDecimal("416.67"), bilancio.costoMaturato());
            assertEquals(new BigDecimal("3800.00"), bilancio.costoPrevisto());
            RigaRimborsi riga = bilancioService.prospetto("2024/2025").stream()
                .filter(r -> r.insegnante().getId().equals(insegnante.getId())).findFirst().orElseThrow();
            assertEquals(new BigDecimal("416.67"), riga.maturato());
            // La proposta mensile resta solo oraria
            assertEquals(0, new BigDecimal("116.67").compareTo(
                bilancioService.propostaRimborso(insegnante.getId(), YearMonth.from(completata.getData()))));
            // Anno senza corsi: niente forfait
            assertEquals(0, bilancioService.bilancio(insegnante.getId(), "2025/2026").costoMaturato().signum());
            assertEquals(0, bilancioService.prospetto("2025/2026").stream()
                .filter(r -> r.insegnante().getId().equals(insegnante.getId())).findFirst().orElseThrow().maturato().signum());

            // Solo il forfait, senza tariffa oraria: vale quello
            insegnante.setTariffaOraria(null);
            insegnanteService.save(insegnante);
            bilancio = bilancioService.bilancio(insegnante.getId(), "2024/2025");
            assertEquals(0, new BigDecimal("300").compareTo(bilancio.costoMaturato()));
            assertEquals(0, new BigDecimal("300").compareTo(bilancio.costoPrevisto()));
        } finally {
            Insegnante ripristino = insegnanteService.findByUsername("insegnante1").orElseThrow();
            ripristino.setRimborsoForfettario(null);
            ripristino.setTariffaOraria(new BigDecimal("25.00"));
            insegnanteService.save(ripristino);
            Lezione daRiaprire = lezioneService.findById(completata.getId());
            daRiaprire.setCompletata(false);
            lezioneService.save(daRiaprire);
        }
    }

    @Test
    void dueInsegnanti_ognunoMaturaLeLezioniACuiEraPresente() {
        Insegnante maria = insegnanteService.findByUsername("insegnante1").orElseThrow();
        Insegnante marco = insegnanteService.findByUsername("insegnante2").orElseThrow();
        var canto = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow();
        canto.setInsegnantiFacoltativi(java.util.List.of(marco));
        corsoService.save(canto);
        Lezione lezione = lezioneService.findByCorso(canto.getId()).getFirst();
        BigDecimal marcoPrima = maturato(marco);
        BigDecimal mariaPrima = maturato(maria);
        // 7 allievi × 40 minuti con la tariffa di ciascuno
        BigDecimal costoMarco = BilancioService.costo(280, marco.getTariffaOraria());
        BigDecimal costoMaria = BilancioService.costo(280, maria.getTariffaOraria());
        try {
            lezioneService.completa(lezione.getId(), "admin");
            assertEquals(marcoPrima.add(costoMarco), maturato(marco));
            assertEquals(mariaPrima.add(costoMaria), maturato(maria));
            assertEquals(2, bilancioService.bilancio(marco.getId(), "2024/2025").corsi().size());

            // Solo Maria presente: Marco non matura la lezione
            Lezione soloMaria = lezioneService.findById(lezione.getId());
            soloMaria.setInsegnantiPresenti(new HashSet<>(List.of(maria)));
            lezioneService.save(soloMaria);
            assertEquals(marcoPrima, maturato(marco));
            assertEquals(mariaPrima.add(costoMaria), maturato(maria));
        } finally {
            Lezione ripristino = lezioneService.findById(lezione.getId());
            ripristino.setCompletata(false);
            lezioneService.save(ripristino);
            canto.setInsegnantiFacoltativi(java.util.List.of());
            corsoService.save(canto);
        }
    }

    @Test
    void costiPrevistiPerCorso_comeNelBilancioSoloConTariffa() {
        Insegnante maria = insegnanteService.findByUsername("insegnante1").orElseThrow();
        var canto = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow();
        var costi = bilancioService.costiPrevistiPerCorso();
        assertEquals(new BigDecimal("3500.00"), costi.get(canto.getId()));
        // Ogni corso ha la somma dei costi previsti dei suoi insegnanti con tariffa, come nei loro bilanci
        for (var corso : corsoService.findAll().stream().filter(c -> c.getAnnoScolastico() != null).toList()) {
            BigDecimal atteso = insegnanteService.findAll().stream()
                .filter(BilancioService::haTariffa)
                .flatMap(i -> bilancioService.bilancio(i.getId(), corso.getAnnoScolastico()).corsi().stream())
                .filter(b -> b.corso().getId().equals(corso.getId()))
                .map(BilancioCorso::costoPrevisto)
                .reduce(null, (a, b) -> a == null ? b : a.add(b));
            assertEquals(atteso, costi.get(corso.getId()), corso.getNome());
        }

        BigDecimal tariffa = maria.getTariffaOraria();
        maria.setTariffaOraria(null);
        insegnanteService.save(maria);
        try {
            assertFalse(bilancioService.costiPrevistiPerCorso().containsKey(canto.getId()));
        } finally {
            Insegnante ripristino = insegnanteService.findByUsername("insegnante1").orElseThrow();
            ripristino.setTariffaOraria(tariffa);
            insegnanteService.save(ripristino);
        }
    }

    private BigDecimal maturato(Insegnante insegnante) {
        return bilancioService.prospetto("2024/2025").stream()
            .filter(r -> r.insegnante().getId().equals(insegnante.getId()))
            .findFirst().orElseThrow().maturato();
    }
}
