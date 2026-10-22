package it.attocchi.service;

import it.attocchi.entity.Corso;
import it.attocchi.entity.Insegnante;
import it.attocchi.entity.Iscrizione;
import it.attocchi.entity.Lezione;
import it.attocchi.entity.Rimborso;
import it.attocchi.repository.CorsoRepository;
import it.attocchi.repository.InsegnanteRepository;
import it.attocchi.repository.IscrizioneRepository;
import it.attocchi.repository.LezioneRepository;
import it.attocchi.repository.PagamentoRepository;
import it.attocchi.repository.PresenzaRepository;
import it.attocchi.repository.RimborsoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Costi delle lezioni degli insegnanti e mini bilancio per anno scolastico. Le lezioni sono individuali: le ore di
 * una lezione sono gli allievi iscritti alla lezione (le sue presenze, assenti compresi) × la durata del corso;
 * il costo è ore × tariffa oraria attuale. Ogni insegnante matura solo le lezioni a cui era presente
 * ({@code Lezione.insegnantiPresenti}): con due insegnanti presenti la lezione costa per entrambi. Le lezioni non
 * effettuate non costano. Il rimborso forfettario annuo dell'insegnante si somma al costo delle lezioni in ogni anno
 * scolastico in cui ha corsi, maturato per intero dall'inizio dell'anno; non entra nella proposta del rimborso
 * mensile né nel costo dei singoli corsi.
 */
@Service
@Transactional(readOnly = true)
public class BilancioService {

    /** Riga del prospetto dei rimborsi: rimborsato e maturato (lezioni completate e forfait) nell'anno scolastico. */
    public record RigaRimborsi(Insegnante insegnante, BigDecimal rimborsato, BigDecimal maturato) {
        public BigDecimal daRimborsare() {
            return maturato.subtract(rimborsato);
        }
    }

    /** Dati di un corso nel bilancio dell'insegnante. */
    public record BilancioCorso(Corso corso, int iscritti, BigDecimal incassato, BigDecimal incassoPrevisto,
                                BigDecimal costoMaturato, BigDecimal costoPrevisto) {
    }

    /**
     * Mini bilancio dell'insegnante per un anno scolastico; {@code forfait} è il rimborso forfettario annuo (zero se
     * non impostato o se l'insegnante non ha corsi nell'anno), compreso nel costo maturato e in quello previsto.
     */
    public record Bilancio(Insegnante insegnante, String annoScolastico, List<BilancioCorso> corsi, BigDecimal forfait,
                           BigDecimal rimborsato) {

        public boolean senzaTariffa() {
            return !haTariffa(insegnante);
        }

        public BigDecimal incassato() {
            return somma(BilancioCorso::incassato);
        }

        public BigDecimal incassoPrevisto() {
            return somma(BilancioCorso::incassoPrevisto);
        }

        /** Costo delle lezioni completate, senza il forfait. */
        public BigDecimal costoLezioniMaturato() {
            return somma(BilancioCorso::costoMaturato);
        }

        /** Costo delle lezioni completate più il forfait annuo. */
        public BigDecimal costoMaturato() {
            return costoLezioniMaturato().add(forfait);
        }

        /** Costo delle lezioni programmate più il forfait annuo. */
        public BigDecimal costoPrevisto() {
            return somma(BilancioCorso::costoPrevisto).add(forfait);
        }

        /** Effettivo: incassato meno rimborsato. */
        public BigDecimal saldo() {
            return incassato().subtract(rimborsato);
        }

        public BigDecimal daRimborsare() {
            return costoMaturato().subtract(rimborsato);
        }

        public BigDecimal marginePrevisto() {
            return incassoPrevisto().subtract(costoPrevisto());
        }

        private BigDecimal somma(java.util.function.Function<BilancioCorso, BigDecimal> valore) {
            return corsi.stream().map(valore).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    private final InsegnanteRepository insegnanteRepository;
    private final CorsoRepository corsoRepository;
    private final LezioneRepository lezioneRepository;
    private final PresenzaRepository presenzaRepository;
    private final IscrizioneRepository iscrizioneRepository;
    private final PagamentoRepository pagamentoRepository;
    private final RimborsoRepository rimborsoRepository;

    public BilancioService(InsegnanteRepository insegnanteRepository, CorsoRepository corsoRepository,
                           LezioneRepository lezioneRepository, PresenzaRepository presenzaRepository,
                           IscrizioneRepository iscrizioneRepository, PagamentoRepository pagamentoRepository,
                           RimborsoRepository rimborsoRepository) {
        this.insegnanteRepository = insegnanteRepository;
        this.corsoRepository = corsoRepository;
        this.lezioneRepository = lezioneRepository;
        this.presenzaRepository = presenzaRepository;
        this.iscrizioneRepository = iscrizioneRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.rimborsoRepository = rimborsoRepository;
    }

    /** Costo di una durata in minuti con la tariffa oraria (0 senza tariffa), arrotondato al centesimo. */
    public static BigDecimal costo(long minuti, BigDecimal tariffaOraria) {
        if (tariffaOraria == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        return tariffaOraria.multiply(BigDecimal.valueOf(minuti)).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    /** Rimborso forfettario annuo dell'insegnante (zero se non impostato). */
    public static BigDecimal forfait(Insegnante insegnante) {
        return insegnante.getRimborsoForfettario() != null ? insegnante.getRimborsoForfettario() : BigDecimal.ZERO;
    }

    /** {@code true} se l'insegnante ha una tariffa oraria maggiore di zero. */
    public static boolean haTariffa(Insegnante insegnante) {
        return insegnante.getTariffaOraria() != null && insegnante.getTariffaOraria().signum() > 0;
    }

    /**
     * Costo previsto degli insegnanti per corso (id del corso → costo), come nel {@link #bilancio}: somma i costi
     * previsti di ogni insegnante con tariffa sui corsi di cui è insegnante o in cui ha lezioni. I corsi senza
     * nessun insegnante con tariffa non compaiono.
     */
    public Map<Long, BigDecimal> costiPrevistiPerCorso() {
        Map<Long, Long> iscritti = new HashMap<>();
        for (Object[] riga : iscrizioneRepository.contaPerCorso()) {
            iscritti.put((Long) riga[0], (Long) riga[1]);
        }
        Map<Long, BigDecimal> costi = new HashMap<>();
        for (Insegnante insegnante : insegnanteRepository.findAll()) {
            if (!haTariffa(insegnante)) {
                continue;
            }
            Map<Long, Long> presenze = presenzePerLezione(insegnante.getId());
            Map<Long, List<Lezione>> lezioniPerCorso = lezioniPerCorso(insegnante.getId());
            for (Corso corso : corsiDaConsiderare(insegnante.getId(), lezioniPerCorso)) {
                costi.merge(corso.getId(), costoPrevisto(corso, lezioniPerCorso.getOrDefault(corso.getId(), List.of()),
                    presenze, iscritti.getOrDefault(corso.getId(), 0L), insegnante.getTariffaOraria()), BigDecimal::add);
            }
        }
        return costi;
    }

    /** Prospetto per anno scolastico: una riga per insegnante, in ordine di nome. */
    public List<RigaRimborsi> prospetto(String annoScolastico) {
        Map<Long, BigDecimal> rimborsato = new HashMap<>();
        for (Rimborso r : rimborsoRepository.findByPeriodo(RimborsoService.inizioAnno(annoScolastico),
                RimborsoService.fineAnno(annoScolastico))) {
            rimborsato.merge(r.getInsegnante().getId(), r.getImporto(), BigDecimal::add);
        }
        return insegnanteRepository.findAll().stream()
            .sorted(Comparator.comparing(Insegnante::getNomeCompleto))
            .map(i -> new RigaRimborsi(i, rimborsato.getOrDefault(i.getId(), BigDecimal.ZERO),
                costo(minutiLezioni(i.getId(), l -> l.isCompletata() && annoScolastico.equals(l.getCorso().getAnnoScolastico())),
                    i.getTariffaOraria()).add(forfaitDellAnno(i, annoScolastico))))
            .toList();
    }

    /**
     * Importo proposto per il rimborso di un mese: costo delle lezioni completate nel mese (tutti i corsi
     * dell'insegnante) meno i rimborsi già registrati per quel mese; mai negativo.
     */
    public BigDecimal propostaRimborso(Long insegnanteId, YearMonth mese) {
        Insegnante insegnante = insegnante(insegnanteId);
        BigDecimal maturato = costo(minutiLezioni(insegnanteId,
            l -> l.isCompletata() && YearMonth.from(l.getData()).equals(mese)), insegnante.getTariffaOraria());
        BigDecimal giaRimborsato = rimborsoRepository.findByInsegnanteAndPeriodo(insegnanteId, mese.atDay(1), mese.atEndOfMonth())
            .stream().map(Rimborso::getImporto).reduce(BigDecimal.ZERO, BigDecimal::add);
        return maturato.subtract(giaRimborsato).max(BigDecimal.ZERO);
    }

    /** Mini bilancio dell'insegnante sui suoi corsi dell'anno scolastico e sui rimborsi dello stesso anno. */
    public Bilancio bilancio(Long insegnanteId, String annoScolastico) {
        Insegnante insegnante = insegnante(insegnanteId);
        Map<Long, Long> presenze = presenzePerLezione(insegnanteId);
        Map<Long, List<Lezione>> lezioniPerCorso = lezioniPerCorso(insegnanteId);
        List<BilancioCorso> corsi = new ArrayList<>();
        for (Corso corso : corsiDaConsiderare(insegnanteId, lezioniPerCorso)) {
            if (!annoScolastico.equals(corso.getAnnoScolastico())) {
                continue;
            }
            List<Iscrizione> iscrizioni = iscrizioneRepository.findByCorsoIdWithAllievo(corso.getId());
            BigDecimal incassoPrevisto = iscrizioni.stream().map(Iscrizione::getQuotaDovuta).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal incassato = pagamentoRepository.sommaPerAllievo(corso.getId()).stream()
                .map(riga -> (BigDecimal) riga[1]).reduce(BigDecimal.ZERO, BigDecimal::add);
            List<Lezione> lezioni = lezioniPerCorso.getOrDefault(corso.getId(), List.of());
            long minutiMaturati = minuti(lezioni, presenze, Lezione::isCompletata);
            corsi.add(new BilancioCorso(corso, iscrizioni.size(), incassato, incassoPrevisto,
                costo(minutiMaturati, insegnante.getTariffaOraria()),
                costoPrevisto(corso, lezioni, presenze, iscrizioni.size(), insegnante.getTariffaOraria())));
        }
        corsi.sort(Comparator.comparing(b -> b.corso().getNome()));
        BigDecimal forfait = corsi.isEmpty() ? BigDecimal.ZERO : forfait(insegnante);
        BigDecimal rimborsato = rimborsoRepository.findByInsegnanteAndPeriodo(insegnanteId,
                RimborsoService.inizioAnno(annoScolastico), RimborsoService.fineAnno(annoScolastico))
            .stream().map(Rimborso::getImporto).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Bilancio(insegnante, annoScolastico, corsi, forfait, rimborsato);
    }

    /**
     * Costo previsto di un insegnante su un corso: le sue lezioni non annullate; senza lezioni generate si stima
     * sul numero di lezioni del corso per ogni iscritto.
     */
    private static BigDecimal costoPrevisto(Corso corso, List<Lezione> lezioni, Map<Long, Long> presenze, long iscritti,
                                            BigDecimal tariffaOraria) {
        long minutiPrevisti = lezioni.isEmpty()
            ? valore(corso.getNumeroLezioni()) * iscritti * valore(corso.getDurataLezioneMinuti())
            : minuti(lezioni, presenze, l -> !l.isNonEffettuata());
        return costo(minutiPrevisti, tariffaOraria);
    }

    /** Forfait annuo dell'insegnante se ha corsi nell'anno scolastico (come nel {@link #bilancio}), altrimenti zero. */
    private BigDecimal forfaitDellAnno(Insegnante insegnante, String annoScolastico) {
        if (forfait(insegnante).signum() == 0) {
            return BigDecimal.ZERO;
        }
        boolean haCorsi = corsiDaConsiderare(insegnante.getId(), lezioniPerCorso(insegnante.getId())).stream()
            .anyMatch(c -> annoScolastico.equals(c.getAnnoScolastico()));
        return haCorsi ? forfait(insegnante) : BigDecimal.ZERO;
    }

    /** Lezioni a cui l'insegnante è presente, per id del corso. */
    private Map<Long, List<Lezione>> lezioniPerCorso(Long insegnanteId) {
        Map<Long, List<Lezione>> lezioniPerCorso = new HashMap<>();
        lezioneRepository.findConInsegnantePresente(insegnanteId)
            .forEach(l -> lezioniPerCorso.computeIfAbsent(l.getCorso().getId(), k -> new ArrayList<>()).add(l));
        return lezioniPerCorso;
    }

    /** I corsi dell'insegnante e quelli di cui non lo è più ma in cui ha lezioni svolte. */
    private Collection<Corso> corsiDaConsiderare(Long insegnanteId, Map<Long, List<Lezione>> lezioniPerCorso) {
        Map<Long, Corso> corsi = new LinkedHashMap<>();
        corsoRepository.findDellInsegnante(insegnanteId).forEach(c -> corsi.put(c.getId(), c));
        lezioniPerCorso.values().forEach(lezioni -> corsi.putIfAbsent(lezioni.getFirst().getCorso().getId(),
            lezioni.getFirst().getCorso()));
        return corsi.values();
    }

    /** Minuti di insegnamento delle lezioni dell'insegnante che soddisfano il filtro (mai le non effettuate). */
    private long minutiLezioni(Long insegnanteId, Predicate<Lezione> filtro) {
        return minuti(lezioneRepository.findConInsegnantePresente(insegnanteId), presenzePerLezione(insegnanteId), filtro);
    }

    private static long minuti(List<Lezione> lezioni, Map<Long, Long> presenze, Predicate<Lezione> filtro) {
        return lezioni.stream()
            .filter(l -> !l.isNonEffettuata() && filtro.test(l))
            .mapToLong(l -> presenze.getOrDefault(l.getId(), 0L) * valore(l.getCorso().getDurataLezioneMinuti()))
            .sum();
    }

    private Map<Long, Long> presenzePerLezione(Long insegnanteId) {
        Map<Long, Long> presenze = new HashMap<>();
        for (Object[] riga : presenzaRepository.contaPerLezione(insegnanteId)) {
            presenze.put((Long) riga[0], (Long) riga[1]);
        }
        return presenze;
    }

    private Insegnante insegnante(Long insegnanteId) {
        return insegnanteRepository.findById(insegnanteId)
            .orElseThrow(() -> new IllegalArgumentException("Insegnante non trovato: " + insegnanteId));
    }

    private static int valore(Integer numero) {
        return numero != null ? numero : 0;
    }
}
