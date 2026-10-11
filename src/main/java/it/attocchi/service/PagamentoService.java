package it.attocchi.service;

import it.attocchi.entity.Corso;
import it.attocchi.entity.Iscrizione;
import it.attocchi.entity.IscrizioneId;
import it.attocchi.entity.Pagamento;
import it.attocchi.repository.CorsoRepository;
import it.attocchi.repository.IscrizioneRepository;
import it.attocchi.repository.PagamentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
     * Riepilogo dei pagamenti di un corso attivo (o, con {@code corso} "Totale", di tutti): partecipanti saldati,
     * con pagamento parziale e senza pagamenti; incassato e residuo da incassare.
     */
    public record RiepilogoPagamenti(String corso, int saldati, int parziali, int senzaPagamenti,
                                     BigDecimal incassato, BigDecimal daIncassare) {

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

        static RiepilogoPagamenti di(String corso, List<Situazione> situazioni) {
            int saldati = 0;
            int parziali = 0;
            int senzaPagamenti = 0;
            BigDecimal incassato = BigDecimal.ZERO;
            BigDecimal daIncassare = BigDecimal.ZERO;
            for (Situazione situazione : situazioni) {
                switch (situazione.stato()) {
                    case SALDATO -> saldati++;
                    case PARZIALE -> parziali++;
                    case NESSUN_PAGAMENTO -> senzaPagamenti++;
                }
                incassato = incassato.add(situazione.pagato());
                daIncassare = daIncassare.add(situazione.residuo());
            }
            return new RiepilogoPagamenti(corso, saldati, parziali, senzaPagamenti, incassato, daIncassare);
        }
    }

    /** Dati della ricevuta di un partecipante: iscrizione (con corso, allievo e genitore), rate e situazione. */
    public record Ricevuta(Iscrizione iscrizione, List<Pagamento> pagamenti, Situazione situazione) {
    }

    private final PagamentoRepository pagamentoRepository;
    private final IscrizioneRepository iscrizioneRepository;
    private final CorsoRepository corsoRepository;

    public PagamentoService(PagamentoRepository pagamentoRepository, IscrizioneRepository iscrizioneRepository,
                            CorsoRepository corsoRepository) {
        this.pagamentoRepository = pagamentoRepository;
        this.iscrizioneRepository = iscrizioneRepository;
        this.corsoRepository = corsoRepository;
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
        perCorso.forEach((corso, situazioni) -> riepilogo.add(RiepilogoPagamenti.di(corso.getNome(), situazioni)));
        riepilogo.add(RiepilogoPagamenti.di("Totale", tutte));
        return riepilogo;
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
