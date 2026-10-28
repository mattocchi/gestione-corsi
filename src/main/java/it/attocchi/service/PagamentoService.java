package it.attocchi.service;

import it.attocchi.entity.MetodoPagamento;
import it.attocchi.entity.Indirizzo;
import java.time.LocalDate;
import it.attocchi.entity.Allievo;
import it.attocchi.entity.Corso;
import it.attocchi.entity.Genitore;
import it.attocchi.entity.IntestazioneDocumenti;
import it.attocchi.entity.GruppoCorso;
import it.attocchi.entity.Iscrizione;
import it.attocchi.entity.IscrizioneId;
import it.attocchi.entity.Pagamento;
import it.attocchi.repository.AllievoRepository;
import it.attocchi.repository.CorsoRepository;
import it.attocchi.repository.GenitoreRepository;
import it.attocchi.repository.GruppoCorsoRepository;
import it.attocchi.repository.IscrizioneRepository;
import it.attocchi.repository.PagamentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Quote e pagamenti dei partecipanti. Restano modificabili anche nei corsi non attivi
 * (saldi arrivati dopo la chiusura).
 */
@Service
@Transactional(readOnly = true)
public class PagamentoService {

    /** Stato del pagamento della quota di un partecipante. */
    public enum StatoPagamento {
        SALDATO("Saldato"),
        PARZIALE("Parziale"),
        NESSUN_PAGAMENTO("Da pagare");

        private final String descrizione;

        StatoPagamento(String descrizione) {
            this.descrizione = descrizione;
        }

        public String getDescrizione() {
            return descrizione;
        }
    }

    /** Quota dovuta e totale pagato da un partecipante. */
    public record Situazione(BigDecimal quota, BigDecimal pagato) {

        public BigDecimal residuo() {
            return quota.subtract(pagato).max(BigDecimal.ZERO);
        }

        /** Quota 0 o interamente pagata = saldato. */
        public StatoPagamento stato() {
            if (pagato.compareTo(quota) >= 0) {
                return StatoPagamento.SALDATO;
            }
            return pagato.signum() > 0 ? StatoPagamento.PARZIALE : StatoPagamento.NESSUN_PAGAMENTO;
        }

        /** Percentuale pagata della quota (100 se la quota è 0). */
        public int percentualePagata() {
            if (quota.signum() == 0) {
                return 100;
            }
            return pagato.min(quota).multiply(BigDecimal.valueOf(100)).divide(quota, 0, RoundingMode.DOWN).intValue();
        }
    }

    /**
     * Riepilogo dei pagamenti di un corso attivo (o, con {@code corso} "Totale" e {@code corsoId} nullo, di tutti):
     * partecipanti saldati, con pagamento parziale e senza pagamenti; quote dovute, incassato e residuo da incassare.
     */
    public record RiepilogoPagamenti(Long corsoId, String corso, int saldati, int parziali, int senzaPagamenti,
                                     BigDecimal quota, BigDecimal incassato, BigDecimal daIncassare) {

        public int partecipanti() {
            return saldati + parziali + senzaPagamenti;
        }

        /** Percentuale incassata su incassato + da incassare (100 se non c'è nulla da incassare). */
        public int percentualeIncassata() {
            BigDecimal totale = incassato.add(daIncassare);
            if (totale.signum() == 0) {
                return 100;
            }
            return incassato.multiply(BigDecimal.valueOf(100)).divide(totale, 0, RoundingMode.HALF_UP).intValue();
        }

        public int percentualeDaIncassare() {
            return 100 - percentualeIncassata();
        }

        /** @param corsoId {@code null} per la riga del totale */
        static RiepilogoPagamenti di(Long corsoId, String corso, List<Situazione> situazioni) {
            int saldati = 0;
            int parziali = 0;
            int senzaPagamenti = 0;
            BigDecimal quota = BigDecimal.ZERO;
            BigDecimal incassato = BigDecimal.ZERO;
            BigDecimal daIncassare = BigDecimal.ZERO;
            for (Situazione situazione : situazioni) {
                switch (situazione.stato()) {
                    case SALDATO -> saldati++;
                    case PARZIALE -> parziali++;
                    case NESSUN_PAGAMENTO -> senzaPagamenti++;
                }
                quota = quota.add(situazione.quota());
                incassato = incassato.add(situazione.pagato());
                daIncassare = daIncassare.add(situazione.residuo());
            }
            return new RiepilogoPagamenti(corsoId, corso, saldati, parziali, senzaPagamenti, quota, incassato, daIncassare);
        }
    }

    /** Dati della ricevuta di un partecipante: iscrizione (con corso, allievo e genitore), rate e situazione. */
    public record Ricevuta(Iscrizione iscrizione, List<Pagamento> pagamenti, Situazione situazione) {
    }

    /** Riga della gestione pagamenti: iscrizione (con corso, allievo e genitore) e sua situazione. */
    public record RigaPagamento(Iscrizione iscrizione, Situazione situazione) {

        public Corso corso() {
            return iscrizione.getCorso();
        }

        public Allievo allievo() {
            return iscrizione.getAllievo();
        }

        /** Genitore dell'allievo, o {@code null} se non assegnato. */
        public Genitore genitore() {
            return iscrizione.getAllievo().getGenitore();
        }
    }

    /**
     * Riepilogo della famiglia per un anno scolastico ({@code null} = corsi senza anno): le iscrizioni dei figli
     * del genitore, in ordine di figlio e corso, con i totali.
     */
    public record RiepilogoFamiglia(Genitore genitore, Allievo allievo, String anno, List<RigaPagamento> righe) {

        /** Riepilogo di un genitore, con i corsi dei figli. */
        public RiepilogoFamiglia(Genitore genitore, String anno, List<RigaPagamento> righe) {
            this(genitore, null, anno, righe);
        }

        /** Chi riceve il riepilogo: il genitore, o l'allievo adulto (senza genitore) per i propri corsi. */
        public String intestatario() {
            return genitore != null ? genitore.getNomeCompleto() : allievo.getNomeCompleto();
        }

        public BigDecimal quota() {
            return somma(righe, Situazione::quota);
        }

        public BigDecimal pagato() {
            return somma(righe, Situazione::pagato);
        }

        public BigDecimal residuo() {
            return somma(righe, Situazione::residuo);
        }

        /** Righe raggruppate per figlio, nell'ordine dell'elenco. */
        public Map<Allievo, List<RigaPagamento>> perFiglio() {
            Map<Allievo, List<RigaPagamento>> perFiglio = new LinkedHashMap<>();
            righe.forEach(riga -> perFiglio.computeIfAbsent(riga.allievo(), a -> new ArrayList<>()).add(riga));
            return perFiglio;
        }

        public static BigDecimal somma(List<RigaPagamento> righe, Function<Situazione, BigDecimal> importo) {
            return righe.stream().map(r -> importo.apply(r.situazione())).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    /**
     * Un corso con i suoi iscritti (in ordine di allievo, anche nessuno), i suoi gruppi (per giorno e orario, anche
     * nessuno) e i totali di quota, pagato e residuo.
     */
    public record IscrittiCorso(Corso corso, List<RigaPagamento> iscritti, List<GruppoCorso> gruppi) {

        /** Iscritti del gruppo indicato ({@code null} = quelli senza gruppo), tra quelli dati. */
        public static List<RigaPagamento> delGruppo(List<RigaPagamento> iscritti, GruppoCorso gruppo) {
            return iscritti.stream().filter(r -> Objects.equals(r.iscrizione().getGruppo(), gruppo)).toList();
        }

        public BigDecimal quota() {
            return RiepilogoFamiglia.somma(iscritti, Situazione::quota);
        }

        public BigDecimal pagato() {
            return RiepilogoFamiglia.somma(iscritti, Situazione::pagato);
        }

        public BigDecimal residuo() {
            return RiepilogoFamiglia.somma(iscritti, Situazione::residuo);
        }

        public long saldati() {
            return iscritti.stream().filter(r -> r.situazione().stato() == StatoPagamento.SALDATO).count();
        }
    }

    private static final Comparator<RigaPagamento> PER_ALLIEVO_E_CORSO = Comparator
        .comparing((RigaPagamento r) -> r.allievo().getNomeCompleto(), String.CASE_INSENSITIVE_ORDER)
        .thenComparing(r -> r.allievo().getId())
        .thenComparing(r -> r.corso().getNome(), String.CASE_INSENSITIVE_ORDER);

    private static final Comparator<RigaPagamento> PER_CORSO_E_ALLIEVO = Comparator
        .comparing((RigaPagamento r) -> r.corso().getNome(), String.CASE_INSENSITIVE_ORDER)
        .thenComparing(r -> r.allievo().getNomeCompleto(), String.CASE_INSENSITIVE_ORDER);

    /** Per corso (nome, anno scolastico) e dentro il corso nell'ordine dei partecipanti ({@link Iscrizione#ORDINE}). */
    private static final Comparator<RigaPagamento> PER_CORSO_E_PARTECIPANTE = Comparator
        .comparing((RigaPagamento r) -> r.corso().getNome(), String.CASE_INSENSITIVE_ORDER)
        .thenComparing(r -> r.corso().getAnnoScolastico(), Comparator.nullsLast(Comparator.<String>reverseOrder()))
        .thenComparing(r -> r.corso().getId())
        .thenComparing(RigaPagamento::iscrizione, Iscrizione.ORDINE);

    private final PagamentoRepository pagamentoRepository;
    private final IscrizioneRepository iscrizioneRepository;
    private final CorsoRepository corsoRepository;
    private final GenitoreRepository genitoreRepository;
    private final GruppoCorsoRepository gruppoRepository;
    private final AllievoRepository allievoRepository;

    public PagamentoService(PagamentoRepository pagamentoRepository, IscrizioneRepository iscrizioneRepository,
                            CorsoRepository corsoRepository, GenitoreRepository genitoreRepository,
                            GruppoCorsoRepository gruppoRepository, AllievoRepository allievoRepository) {
        this.gruppoRepository = gruppoRepository;
        this.allievoRepository = allievoRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.iscrizioneRepository = iscrizioneRepository;
        this.corsoRepository = corsoRepository;
        this.genitoreRepository = genitoreRepository;
    }

    public Iscrizione findIscrizione(Long corsoId, Long allievoId) {
        return iscrizione(corsoId, allievoId);
    }

    public List<Pagamento> findPagamenti(Long corsoId, Long allievoId) {
        return pagamentoRepository.findByIscrizione(corsoId, allievoId);
    }

    public Situazione situazione(Long corsoId, Long allievoId) {
        BigDecimal pagato = findPagamenti(corsoId, allievoId).stream()
            .map(Pagamento::getImporto)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Situazione(iscrizione(corsoId, allievoId).getQuotaDovuta(), pagato);
    }

    /** Ricevuta del partecipante: disponibile anche per i corsi non attivi. */
    public Ricevuta ricevuta(Long corsoId, Long allievoId) {
        return new Ricevuta(iscrizione(corsoId, allievoId), findPagamenti(corsoId, allievoId), situazione(corsoId, allievoId));
    }

    /** Situazione di ogni partecipante del corso, per id dell'allievo. */
    public Map<Long, Situazione> situazioniDelCorso(Long corsoId) {
        Map<Long, BigDecimal> pagato = new HashMap<>();
        for (Object[] riga : pagamentoRepository.sommaPerAllievo(corsoId)) {
            pagato.put((Long) riga[0], (BigDecimal) riga[1]);
        }
        Map<Long, Situazione> situazioni = new HashMap<>();
        for (Iscrizione iscrizione : iscrizioneRepository.findByCorsoIdWithAllievo(corsoId)) {
            Long allievoId = iscrizione.getAllievo().getId();
            situazioni.put(allievoId, new Situazione(iscrizione.getQuotaDovuta(), pagato.getOrDefault(allievoId, BigDecimal.ZERO)));
        }
        return situazioni;
    }

    /** Situazione dell'allievo in ognuno dei suoi corsi, per id del corso. */
    public Map<Long, Situazione> situazioniDelloAllievo(Long allievoId) {
        Map<Long, BigDecimal> pagato = new HashMap<>();
        for (Object[] riga : pagamentoRepository.sommaPerCorso(allievoId)) {
            pagato.put((Long) riga[0], (BigDecimal) riga[1]);
        }
        Map<Long, Situazione> situazioni = new HashMap<>();
        for (Iscrizione iscrizione : iscrizioneRepository.findByAllievoIdWithCorso(allievoId)) {
            Long corsoId = iscrizione.getCorso().getId();
            situazioni.put(corsoId, new Situazione(iscrizione.getQuotaDovuta(), pagato.getOrDefault(corsoId, BigDecimal.ZERO)));
        }
        return situazioni;
    }

    /** Riepilogo per ogni corso attivo (in ordine di nome) e, in fondo, il totale di tutti i corsi attivi. */
    public List<RiepilogoPagamenti> riepilogoCorsiAttivi() {
        Map<IscrizioneId, BigDecimal> pagato = new HashMap<>();
        for (Object[] riga : pagamentoRepository.sommaPerIscrizioneCorsiAttivi()) {
            pagato.put(new IscrizioneId((Long) riga[0], (Long) riga[1]), (BigDecimal) riga[2]);
        }
        Map<Corso, List<Situazione>> perCorso = new TreeMap<>(Comparator.comparing(Corso::getNome).thenComparing(Corso::getId));
        // Anche i corsi attivi senza partecipanti hanno la loro riga
        corsoRepository.findByAttivoTrue().forEach(corso -> perCorso.put(corso, new ArrayList<>()));
        List<Situazione> tutte = new ArrayList<>();
        for (Iscrizione iscrizione : iscrizioneRepository.findByCorsoAttivo()) {
            Situazione situazione = new Situazione(iscrizione.getQuotaDovuta(),
                pagato.getOrDefault(iscrizione.getId(), BigDecimal.ZERO));
            perCorso.computeIfAbsent(iscrizione.getCorso(), c -> new ArrayList<>()).add(situazione);
            tutte.add(situazione);
        }
        List<RiepilogoPagamenti> riepilogo = new ArrayList<>();
        perCorso.forEach((corso, situazioni) -> riepilogo.add(RiepilogoPagamenti.di(corso.getId(), corso.getNome(), situazioni)));
        riepilogo.add(RiepilogoPagamenti.di(null, "Totale", tutte));
        return riepilogo;
    }

    /** Una riga per ogni partecipante di ogni corso (anche non attivo), in ordine di corso e allievo. */
    public List<RigaPagamento> righePagamenti() {
        return righe(iscrizioneRepository.findAllWithCorsoAllievoGenitore()).stream().sorted(PER_CORSO_E_PARTECIPANTE).toList();
    }

    /**
     * Quota e pagato di tutti i figli del genitore sui corsi attivi (dashboard); vuoto se non hanno iscrizioni a corsi
     * attivi.
     */
    public Optional<Situazione> situazioneDeiFigli(String usernameGenitore) {
        return somma(righePagamentiDeiFigli(usernameGenitore).stream().filter(riga -> riga.corso().isAttivo())
            .map(RigaPagamento::situazione).toList());
    }

    /**
     * Le righe dell'allievo adulto (senza genitore) indicato dallo username, anche nei corsi non attivi, per corso;
     * vuoto se l'allievo ha un genitore (i pagamenti li vede il genitore).
     */
    public List<RigaPagamento> righePagamentiPropri(String usernameAllievo) {
        return allievoAdulto(usernameAllievo)
            .map(allievo -> righe(pubblicate(iscrizioneRepository.findByAllievoIdWithCorso(allievo.getId()))).stream()
                .sorted(PER_CORSO_E_ALLIEVO).toList())
            .orElse(List.of());
    }

    /** Quota e pagato dell'allievo adulto sui corsi attivi (dashboard); vuoto se ha un genitore o nessun corso attivo. */
    public Optional<Situazione> situazionePropria(String usernameAllievo) {
        return somma(righePagamentiPropri(usernameAllievo).stream().filter(r -> r.corso().isAttivo())
            .map(RigaPagamento::situazione).toList());
    }

    /** Anni scolastici dei corsi dell'allievo, dal più recente; {@code null} in fondo per i corsi senza anno. */
    public List<String> anniDellAllievo(Long allievoId) {
        return pubblicate(iscrizioneRepository.findByAllievoIdWithCorso(allievoId)).stream()
            .map(i -> i.getCorso().getAnnoScolastico())
            .distinct()
            .sorted(Comparator.nullsLast(Comparator.<String>reverseOrder()))
            .toList();
    }

    /** Riepilogo dei corsi dell'allievo per l'anno scolastico indicato ({@code null} = corsi senza anno). */
    /**
     * Spese di un allievo in un anno solare, a prescindere dagli anni scolastici: le rate di tutti i corsi con data
     * nell'anno. Chi paga è il genitore, o l'allievo adulto.
     */
    public record Attestazione(Allievo allievo, int anno, List<Pagamento> pagamenti) {

        public BigDecimal totale() {
            return pagamenti.stream().map(Pagamento::getImporto).reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        /** Il genitore che sostiene le spese, o {@code null} se l'allievo è adulto e paga da sé. */
        public Genitore genitore() {
            return allievo.getGenitore();
        }

        /**
         * {@code true} se l'allievo ha tra 5 e 18 anni in almeno un giorno dell'anno (requisito della detrazione:
         * compie 5 anni entro il 31 dicembre e non ha ancora compiuto 19 anni il 1° gennaio).
         */
        public boolean inEta() {
            LocalDate nascita = allievo.getDataNascita();
            return nascita != null && !nascita.plusYears(5).isAfter(LocalDate.of(anno, 12, 31))
                && nascita.plusYears(19).isAfter(LocalDate.of(anno, 1, 1));
        }

        /**
         * Intestatario ("Spett."): il genitore che sostiene le spese, o l'allievo adulto, con nome, cognome e codice
         * fiscale dell'intestazione dei documenti se indicati ({@link IntestazioneDocumenti}).
         */
        public IntestazioneDocumenti intestatario() {
            return genitore() != null ? genitore() : allievo;
        }

        /**
         * Dati del fac-simile che mancano (vuota se completi): luogo e data di nascita, codice fiscale e residenza
         * dell'allievo (propria o del genitore), codice fiscale dell'intestatario e indirizzo del genitore.
         */
        public List<String> datiMancanti() {
            List<String> mancanti = new ArrayList<>();
            if (vuoto(allievo.getLuogoNascita())) {
                mancanti.add("luogo di nascita dell'allievo");
            }
            if (allievo.getDataNascita() == null) {
                mancanti.add("data di nascita dell'allievo");
            }
            if (vuoto(allievo.getCodiceFiscale())) {
                mancanti.add("codice fiscale dell'allievo");
            }
            if (!completo(allievo.getIndirizzoEffettivo())) {
                mancanti.add("residenza dell'allievo");
            }
            Genitore genitore = genitore();
            if (genitore != null) {
                if (vuoto(genitore.getCodiceFiscaleIntestatario())) {
                    mancanti.add("codice fiscale dell'intestatario");
                }
                if (!completo(genitore.getIndirizzo())) {
                    mancanti.add("indirizzo del genitore");
                }
            }
            return mancanti;
        }

        private static boolean vuoto(String valore) {
            return valore == null || valore.isBlank();
        }

        private static boolean completo(Indirizzo indirizzo) {
            return indirizzo != null && !vuoto(indirizzo.getVia()) && !vuoto(indirizzo.getComune());
        }
    }

    public Attestazione attestazione(Long allievoId, int anno) {
        Allievo allievo = allievoRepository.findById(allievoId)
            .orElseThrow(() -> new IllegalArgumentException("Allievo non trovato: " + allievoId));
        return new Attestazione(allievo, anno, pagamentoRepository.findDellAllievoNelPeriodo(allievoId,
            LocalDate.of(anno, 1, 1), LocalDate.of(anno, 12, 31)).stream()
            // Detraibili: solo bonifici (tracciabili), solo corsi (non servizi) pubblicati
            .filter(p -> p.getIscrizione().getCorso().isPubblicato() && !p.getIscrizione().getCorso().isServizio()
                && p.getMetodo() == MetodoPagamento.BONIFICO)
            .toList());
    }

    /** Anni solari con almeno un pagamento, per allievo (dal più recente). */
    public Map<Long, List<Integer>> anniDiPagamentoPerAllievo() {
        Map<Long, List<Integer>> anni = new HashMap<>();
        for (Object[] riga : pagamentoRepository.totaliPerAllievoEAnno()) {
            anni.computeIfAbsent((Long) riga[0], k -> new ArrayList<>()).add(((Number) riga[1]).intValue());
        }
        anni.values().forEach(elenco -> elenco.sort(Comparator.reverseOrder()));
        return anni;
    }

    public RiepilogoFamiglia riepilogoAllievo(Long allievoId, String anno) {
        Allievo allievo = allievoRepository.findById(allievoId)
            .orElseThrow(() -> new IllegalArgumentException("Allievo non trovato: " + allievoId));
        List<Iscrizione> iscrizioni = pubblicate(iscrizioneRepository.findByAllievoIdWithCorso(allievoId)).stream()
            .filter(i -> Objects.equals(i.getCorso().getAnnoScolastico(), anno))
            .toList();
        return new RiepilogoFamiglia(null, allievo, anno, righe(iscrizioni).stream().sorted(PER_CORSO_E_ALLIEVO).toList());
    }

    private Optional<Allievo> allievoAdulto(String username) {
        return allievoRepository.findByUserUsername(username).filter(a -> a.getGenitore() == null);
    }

    private static Optional<Situazione> somma(List<Situazione> situazioni) {
        if (situazioni.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Situazione(situazioni.stream().map(Situazione::quota).reduce(BigDecimal.ZERO, BigDecimal::add),
            situazioni.stream().map(Situazione::pagato).reduce(BigDecimal.ZERO, BigDecimal::add)));
    }

    /** Le righe dei soli figli del genitore indicato, anche nei corsi non attivi, per corso e nome del figlio. */
    public List<RigaPagamento> righePagamentiDeiFigli(String usernameGenitore) {
        return righe(pubblicate(iscrizioneRepository.findByGenitoreUsernameWithCorsoAllievo(usernameGenitore))).stream()
            .sorted(PER_CORSO_E_ALLIEVO).toList();
    }

    /**
     * Anni scolastici dei corsi dei figli del genitore (attivi o no), dal più recente; {@code null} in fondo se
     * qualche corso non ha l'anno.
     */
    public List<String> anniDellaFamiglia(Long genitoreId) {
        return pubblicate(iscrizioneRepository.findByGenitoreIdWithCorsoAllievo(genitoreId)).stream()
            .map(i -> i.getCorso().getAnnoScolastico())
            .distinct()
            .sorted(Comparator.nullsLast(Comparator.<String>reverseOrder()))
            .toList();
    }

    /** Riepilogo della famiglia per l'anno scolastico indicato ({@code null} = corsi senza anno). */
    public RiepilogoFamiglia riepilogoFamiglia(Long genitoreId, String anno) {
        Genitore genitore = genitoreRepository.findById(genitoreId)
            .orElseThrow(() -> new IllegalArgumentException("Genitore non trovato: " + genitoreId));
        List<Iscrizione> iscrizioni = pubblicate(iscrizioneRepository.findByGenitoreIdWithCorsoAllievo(genitoreId)).stream()
            .filter(i -> Objects.equals(i.getCorso().getAnnoScolastico(), anno))
            .toList();
        return new RiepilogoFamiglia(genitore, anno, righe(iscrizioni).stream().sorted(PER_ALLIEVO_E_CORSO).toList());
    }

    /**
     * Solo le iscrizioni dei corsi pubblicati: ciò che genitori e allievi vedono (e che si invia loro: riepiloghi,
     * attestazioni) non comprende i corsi in preparazione.
     */
    private static List<Iscrizione> pubblicate(List<Iscrizione> iscrizioni) {
        return iscrizioni.stream().filter(i -> i.getCorso().isPubblicato()).toList();
    }

    private List<RigaPagamento> righe(List<Iscrizione> iscrizioni) {
        Map<IscrizioneId, BigDecimal> pagato = new HashMap<>();
        for (Object[] riga : pagamentoRepository.sommaPerIscrizione()) {
            pagato.put(new IscrizioneId((Long) riga[0], (Long) riga[1]), (BigDecimal) riga[2]);
        }
        return iscrizioni.stream()
            .map(i -> new RigaPagamento(i, new Situazione(i.getQuotaDovuta(), pagato.getOrDefault(i.getId(), BigDecimal.ZERO))))
            .toList();
    }

    /**
     * Ogni corso (attivo o no, anche senza iscritti) con i suoi iscritti (in {@link Iscrizione#ORDINE}), quanto
     * hanno pagato finora e i suoi gruppi, in ordine di nome e anno scolastico del corso.
     */
    public List<IscrittiCorso> iscrittiPerCorso() {
        Map<Long, List<RigaPagamento>> perCorso = new HashMap<>();
        for (RigaPagamento riga : righePagamenti()) {
            perCorso.computeIfAbsent(riga.corso().getId(), id -> new ArrayList<>()).add(riga);
        }
        Map<Long, List<GruppoCorso>> gruppi = new HashMap<>();
        gruppoRepository.findAll().stream().sorted(GruppoCorso.PER_GIORNO)
            .forEach(g -> gruppi.computeIfAbsent(g.getCorso().getId(), id -> new ArrayList<>()).add(g));
        return corsoRepository.findAll().stream()
            .sorted(Comparator.comparing(Corso::getNome, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Corso::getAnnoScolastico, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Corso::getId))
            .map(corso -> new IscrittiCorso(corso, perCorso.getOrDefault(corso.getId(), List.of()).stream()
                .sorted(Comparator.comparing(RigaPagamento::iscrizione, Iscrizione.ORDINE)).toList(),
                gruppi.getOrDefault(corso.getId(), List.of())))
            .toList();
    }

    /**
     * Motivi già usati per le quote personalizzate (tutti i corsi), da suggerire: senza doppioni ignorando maiuscole e
     * spazi ai lati (con la grafia più usata), prima i più usati, a parità in ordine alfabetico.
     */
    public List<String> motiviQuota() {
        Map<String, Map<String, Long>> grafiePerMotivo = new HashMap<>();
        for (Object[] riga : iscrizioneRepository.contaMotiviQuota()) {
            String motivo = ((String) riga[0]).strip();
            if (!motivo.isEmpty()) {
                grafiePerMotivo.computeIfAbsent(motivo.toLowerCase(Locale.ITALIAN), k -> new HashMap<>())
                    .merge(motivo, (Long) riga[1], Long::sum);
            }
        }
        record Motivo(String testo, long usi) {
        }
        return grafiePerMotivo.values().stream()
            .map(grafie -> new Motivo(grafie.entrySet().stream()
                .max(Map.Entry.<String, Long>comparingByValue().thenComparing(Map.Entry::getKey, Comparator.reverseOrder()))
                .orElseThrow().getKey(), grafie.values().stream().mapToLong(Long::longValue).sum()))
            .sorted(Comparator.comparingLong(Motivo::usi).reversed()
                .thenComparing(Motivo::testo, String.CASE_INSENSITIVE_ORDER))
            .map(Motivo::testo)
            .toList();
    }

    /** Imposta (o con {@code null} toglie) la quota personalizzata del partecipante, con la nota sul motivo. */
    @Transactional
    public void impostaQuota(Long corsoId, Long allievoId, BigDecimal quota, String nota) {
        if (quota != null && quota.signum() < 0) {
            throw new IllegalArgumentException("La quota non può essere negativa");
        }
        Iscrizione iscrizione = iscrizione(corsoId, allievoId);
        iscrizione.setQuotaPersonalizzata(quota);
        iscrizione.setNotaQuota(quota != null && nota != null && !nota.isBlank() ? nota.trim() : null);
    }

    /** Aggiunge o modifica una rata del partecipante indicato. */
    @Transactional
    public Pagamento salva(Long corsoId, Long allievoId, Pagamento pagamento) {
        if (pagamento.getImporto() == null || pagamento.getImporto().signum() <= 0) {
            throw new IllegalArgumentException("L'importo deve essere maggiore di zero");
        }
        if (pagamento.getData() == null || pagamento.getMetodo() == null) {
            throw new IllegalArgumentException("Data e metodo del pagamento sono obbligatori");
        }
        pagamento.setIscrizione(iscrizione(corsoId, allievoId));
        return pagamentoRepository.save(pagamento);
    }

    @Transactional
    public void elimina(Pagamento pagamento) {
        pagamentoRepository.deleteById(pagamento.getId());
    }

    private Iscrizione iscrizione(Long corsoId, Long allievoId) {
        return iscrizioneRepository.findById(new IscrizioneId(corsoId, allievoId))
            .orElseThrow(() -> new IllegalArgumentException("L'allievo " + allievoId + " non è iscritto al corso " + corsoId));
    }
}
