package it.attocchi.service;

import it.attocchi.service.BilancioService.OreDonateTotali;
import it.attocchi.service.DashboardService.RiepilogoGenerale;
import it.attocchi.service.DashboardService.RiepilogoGruppi;
import it.attocchi.service.PagamentoService.IscrittiCorso;
import it.attocchi.service.PagamentoService.RiepilogoPagamenti;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Dati della stampa "Riepilogo dei corsi attivi" (admin e segreteria): le sezioni della dashboard su pagamenti (con
 * costo insegnanti e margine) e gruppi, gli iscritti di ogni corso attivo e il riepilogo generale (per l'email).
 */
@Service
@Transactional(readOnly = true)
public class RiepilogoCorsiService {

    /**
     * Riepilogo alla data {@code data}. {@code pagamenti} ha una riga per corso attivo e per ultima quella del totale;
     * {@code costi} è il costo previsto degli insegnanti per id del corso (assente senza tariffa).
     */
    public record RiepilogoCorsi(LocalDate data, RiepilogoGenerale generale, List<RiepilogoPagamenti> pagamenti,
                                 Map<Long, BigDecimal> costi, Optional<OreDonateTotali> oreDonate, RiepilogoGruppi gruppi, List<IscrittiCorso> iscritti) {

        public List<RiepilogoPagamenti> corsi() {
            return pagamenti.subList(0, pagamenti.size() - 1);
        }

        public RiepilogoPagamenti totale() {
            return pagamenti.getLast();
        }

        /** Costo del corso, {@code null} se nessun suo insegnante ha la tariffa oraria. */
        public BigDecimal costo(Long corsoId) {
            return costi.get(corsoId);
        }

        /** Quote dovute − costo insegnanti, {@code null} senza tariffa. */
        public BigDecimal margine(RiepilogoPagamenti corso) {
            BigDecimal costo = costo(corso.corsoId());
            return costo != null ? corso.quota().subtract(costo) : null;
        }

        /** Costo previsto dei corsi con tariffa (gli altri sono esclusi dai totali). */
        public BigDecimal costoTotale() {
            return corsi().stream().map(c -> costo(c.corsoId())).filter(c -> c != null).reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        public BigDecimal margineTotale() {
            return corsi().stream().map(this::margine).filter(m -> m != null).reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        public long corsiSenzaTariffa() {
            return corsi().stream().filter(c -> costo(c.corsoId()) == null).count();
        }
    }

    private final DashboardService dashboardService;
    private final PagamentoService pagamentoService;
    private final BilancioService bilancioService;

    public RiepilogoCorsiService(DashboardService dashboardService, PagamentoService pagamentoService,
                                 BilancioService bilancioService) {
        this.dashboardService = dashboardService;
        this.pagamentoService = pagamentoService;
        this.bilancioService = bilancioService;
    }

    /** Riepilogo dei corsi attivi a oggi, con gli stessi conteggi della dashboard. */
    public RiepilogoCorsi riepilogo() {
        LocalDate oggi = LocalDate.now();
        return new RiepilogoCorsi(oggi, dashboardService.riepilogoGenerale(), pagamentoService.riepilogoCorsiAttivi(),
            bilancioService.costiPrevistiPerCorso(), bilancioService.oreDonateTotali(), dashboardService.riepilogoGruppi(),
            pagamentoService.iscrittiPerCorso().stream().filter(c -> c.corso().isAttivo()).toList());
    }
}
