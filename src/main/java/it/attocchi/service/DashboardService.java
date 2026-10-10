package it.attocchi.service;

import it.attocchi.repository.CorsoRepository;
import it.attocchi.repository.GenitoreRepository;
import it.attocchi.repository.InsegnanteRepository;
import it.attocchi.repository.IscrizioneRepository;
import it.attocchi.repository.LezioneRepository;
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

    /** Riepilogo dell'insegnante sui propri corsi. */
    public record RiepilogoInsegnante(long corsi, long lezioniFatte, long lezioniDaFare) {
    }

    private final CorsoRepository corsoRepository;
    private final StudenteRepository studenteRepository;
    private final GenitoreRepository genitoreRepository;
    private final InsegnanteRepository insegnanteRepository;
    private final IscrizioneRepository iscrizioneRepository;
    private final LezioneRepository lezioneRepository;

    public DashboardService(CorsoRepository corsoRepository, StudenteRepository studenteRepository,
                            GenitoreRepository genitoreRepository, InsegnanteRepository insegnanteRepository,
                            IscrizioneRepository iscrizioneRepository, LezioneRepository lezioneRepository) {
        this.corsoRepository = corsoRepository;
        this.studenteRepository = studenteRepository;
        this.genitoreRepository = genitoreRepository;
        this.insegnanteRepository = insegnanteRepository;
        this.iscrizioneRepository = iscrizioneRepository;
        this.lezioneRepository = lezioneRepository;
    }

    public RiepilogoGenerale riepilogoGenerale() {
        return new RiepilogoGenerale(corsoRepository.count(), studenteRepository.count(),
            genitoreRepository.count(), insegnanteRepository.count());
    }

    /** Corsi assegnati all'insegnante e lezioni completate / da fare di tutti i suoi corsi. */
    public RiepilogoInsegnante riepilogoInsegnante(String usernameInsegnante) {
        return new RiepilogoInsegnante(corsoRepository.countByInsegnanteUserUsername(usernameInsegnante),
            lezioneRepository.countByCorsoInsegnanteUserUsernameAndCompletata(usernameInsegnante, true),
            lezioneRepository.countByCorsoInsegnanteUserUsernameAndCompletata(usernameInsegnante, false));
    }

    /** Numero di corsi distinti a cui sono iscritti i figli del genitore. */
    public long corsiDeiFigli(String usernameGenitore) {
        return iscrizioneRepository.countCorsiFigliByGenitoreUsername(usernameGenitore);
    }
}
