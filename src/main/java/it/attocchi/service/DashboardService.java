package it.attocchi.service;

import it.attocchi.repository.CorsoRepository;
import it.attocchi.repository.GenitoreRepository;
import it.attocchi.repository.InsegnanteRepository;
import it.attocchi.repository.StudenteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Conteggi di riepilogo per la dashboard iniziale, in funzione del ruolo.
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    /** Riepilogo generale (admin e segreteria). */
    public record RiepilogoGenerale(long corsi, long allievi, long genitori, long insegnanti) {
    }

    private final CorsoRepository corsoRepository;
    private final StudenteRepository studenteRepository;
    private final GenitoreRepository genitoreRepository;
    private final InsegnanteRepository insegnanteRepository;

    public DashboardService(CorsoRepository corsoRepository, StudenteRepository studenteRepository,
                            GenitoreRepository genitoreRepository, InsegnanteRepository insegnanteRepository) {
        this.corsoRepository = corsoRepository;
        this.studenteRepository = studenteRepository;
        this.genitoreRepository = genitoreRepository;
        this.insegnanteRepository = insegnanteRepository;
    }

    public RiepilogoGenerale riepilogoGenerale() {
        return new RiepilogoGenerale(corsoRepository.count(), studenteRepository.count(),
            genitoreRepository.count(), insegnanteRepository.count());
    }
}
