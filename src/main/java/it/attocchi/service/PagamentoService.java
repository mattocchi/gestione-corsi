package it.attocchi.service;

import it.attocchi.entity.Iscrizione;
import it.attocchi.entity.IscrizioneId;
import it.attocchi.entity.Pagamento;
import it.attocchi.repository.IscrizioneRepository;
import it.attocchi.repository.PagamentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private final PagamentoRepository pagamentoRepository;
    private final IscrizioneRepository iscrizioneRepository;

    public PagamentoService(PagamentoRepository pagamentoRepository, IscrizioneRepository iscrizioneRepository) {
        this.pagamentoRepository = pagamentoRepository;
        this.iscrizioneRepository = iscrizioneRepository;
    }

    public Iscrizione findIscrizione(Long corsoId, Long studenteId) {
        return iscrizione(corsoId, studenteId);
    }

    public List<Pagamento> findPagamenti(Long corsoId, Long studenteId) {
        return pagamentoRepository.findByIscrizione(corsoId, studenteId);
    }

    public Situazione situazione(Long corsoId, Long studenteId) {
        BigDecimal pagato = findPagamenti(corsoId, studenteId).stream()
            .map(Pagamento::getImporto)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Situazione(iscrizione(corsoId, studenteId).getQuotaDovuta(), pagato);
    }

    /** Situazione di ogni partecipante del corso, per id dello studente. */
    public Map<Long, Situazione> situazioniDelCorso(Long corsoId) {
        Map<Long, BigDecimal> pagato = new HashMap<>();
        for (Object[] riga : pagamentoRepository.sommaPerStudente(corsoId)) {
            pagato.put((Long) riga[0], (BigDecimal) riga[1]);
        }
        Map<Long, Situazione> situazioni = new HashMap<>();
        for (Iscrizione iscrizione : iscrizioneRepository.findByCorsoIdWithStudente(corsoId)) {
            Long studenteId = iscrizione.getStudente().getId();
            situazioni.put(studenteId, new Situazione(iscrizione.getQuotaDovuta(), pagato.getOrDefault(studenteId, BigDecimal.ZERO)));
        }
        return situazioni;
    }

    /** Imposta (o con {@code null} toglie) la quota personalizzata del partecipante, con la nota sul motivo. */
    @Transactional
    public void impostaQuota(Long corsoId, Long studenteId, BigDecimal quota, String nota) {
        if (quota != null && quota.signum() < 0) {
            throw new IllegalArgumentException("La quota non può essere negativa");
        }
        Iscrizione iscrizione = iscrizione(corsoId, studenteId);
        iscrizione.setQuotaPersonalizzata(quota);
        iscrizione.setNotaQuota(quota != null && nota != null && !nota.isBlank() ? nota.trim() : null);
    }

    /** Aggiunge o modifica una rata del partecipante indicato. */
    @Transactional
    public Pagamento salva(Long corsoId, Long studenteId, Pagamento pagamento) {
        if (pagamento.getImporto() == null || pagamento.getImporto().signum() <= 0) {
            throw new IllegalArgumentException("L'importo deve essere maggiore di zero");
        }
        if (pagamento.getData() == null || pagamento.getMetodo() == null) {
            throw new IllegalArgumentException("Data e metodo del pagamento sono obbligatori");
        }
        pagamento.setIscrizione(iscrizione(corsoId, studenteId));
        return pagamentoRepository.save(pagamento);
    }

    @Transactional
    public void elimina(Pagamento pagamento) {
        pagamentoRepository.deleteById(pagamento.getId());
    }

    private Iscrizione iscrizione(Long corsoId, Long studenteId) {
        return iscrizioneRepository.findById(new IscrizioneId(corsoId, studenteId))
            .orElseThrow(() -> new IllegalArgumentException("Lo studente " + studenteId + " non è iscritto al corso " + corsoId));
    }
}
