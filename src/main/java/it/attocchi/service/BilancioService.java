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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Costi delle lezioni degli insegnanti e mini bilancio per anno scolastico. Le lezioni sono individuali: le ore di
 * una lezione sono gli allievi iscritti alla lezione (le sue presenze, assenti compresi) × la durata del corso;
 * il costo è ore × tariffa oraria attuale. Le lezioni non effettuate non costano.
 */
@Service
@Transactional(readOnly = true)
public class BilancioService {

    /** Riga del prospetto dei rimborsi: rimborsato e maturato (lezioni completate) nell'anno scolastico. */
    public record RigaRimborsi(Insegnante insegnante, BigDecimal rimborsato, BigDecimal maturato) {
        public BigDecimal daRimborsare() {
            return maturato.subtract(rimborsato);
        }
    }

    /** Dati di un corso nel bilancio dell'insegnante. */
    public record BilancioCorso(Corso corso, int iscritti, BigDecimal incassato, BigDecimal incassoPrevisto,
                                BigDecimal costoMaturato, BigDecimal costoPrevisto) {
    }

    /** Mini bilancio dell'insegnante per un anno scolastico. */
    public record Bilancio(Insegnante insegnante, String annoScolastico, List<BilancioCorso> corsi, BigDecimal rimborsato) {

        public boolean senzaTariffa() {
            return insegnante.getTariffaOraria() == null || insegnante.getTariffaOraria().signum() == 0;
        }

        public BigDecimal incassato() {
            return somma(BilancioCorso::incassato);
        }

        public BigDecimal incassoPrevisto() {
            return somma(BilancioCorso::incassoPrevisto);
        }

        public BigDecimal costoMaturato() {
            return somma(BilancioCorso::costoMaturato);
        }

        public BigDecimal costoPrevisto() {
            return somma(BilancioCorso::costoPrevisto);
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
                    i.getTariffaOraria())))
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
        Map<Long, List<Lezione>> lezioniPerCorso = new HashMap<>();
        lezioneRepository.findByCorsoInsegnanteIdOrderByDataAsc(insegnanteId)
            .forEach(l -> lezioniPerCorso.computeIfAbsent(l.getCorso().getId(), k -> new ArrayList<>()).add(l));

        List<BilancioCorso> corsi = new ArrayList<>();
        for (Corso corso : corsoRepository.findByInsegnanteId(insegnanteId)) {
            if (!annoScolastico.equals(corso.getAnnoScolastico())) {
                continue;
            }
            List<Iscrizione> iscrizioni = iscrizioneRepository.findByCorsoIdWithAllievo(corso.getId());
            BigDecimal incassoPrevisto = iscrizioni.stream().map(Iscrizione::getQuotaDovuta).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal incassato = pagamentoRepository.sommaPerAllievo(corso.getId()).stream()
                .map(riga -> (BigDecimal) riga[1]).reduce(BigDecimal.ZERO, BigDecimal::add);
            List<Lezione> lezioni = lezioniPerCorso.getOrDefault(corso.getId(), List.of());
            long minutiMaturati = minuti(lezioni, presenze, Lezione::isCompletata);
            // Senza lezioni generate il costo previsto si stima sul numero di lezioni del corso
            long minutiPrevisti = lezioni.isEmpty()
                ? (long) valore(corso.getNumeroLezioni()) * iscrizioni.size() * valore(corso.getDurataLezioneMinuti())
                : minuti(lezioni, presenze, l -> !l.isNonEffettuata());
            corsi.add(new BilancioCorso(corso, iscrizioni.size(), incassato, incassoPrevisto,
                costo(minutiMaturati, insegnante.getTariffaOraria()), costo(minutiPrevisti, insegnante.getTariffaOraria())));
        }
        corsi.sort(Comparator.comparing(b -> b.corso().getNome()));
        BigDecimal rimborsato = rimborsoRepository.findByInsegnanteAndPeriodo(insegnanteId,
                RimborsoService.inizioAnno(annoScolastico), RimborsoService.fineAnno(annoScolastico))
            .stream().map(Rimborso::getImporto).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Bilancio(insegnante, annoScolastico, corsi, rimborsato);
    }

    /** Minuti di insegnamento delle lezioni dell'insegnante che soddisfano il filtro (mai le non effettuate). */
    private long minutiLezioni(Long insegnanteId, Predicate<Lezione> filtro) {
        return minuti(lezioneRepository.findByCorsoInsegnanteIdOrderByDataAsc(insegnanteId), presenzePerLezione(insegnanteId), filtro);
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
