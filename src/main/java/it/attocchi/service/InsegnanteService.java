package it.attocchi.service;

import it.attocchi.entity.Insegnante;
import it.attocchi.entity.User;
import it.attocchi.repository.InsegnanteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class InsegnanteService {

    private final InsegnanteRepository insegnanteRepository;
    private final UserService userService;

    public InsegnanteService(InsegnanteRepository insegnanteRepository, UserService userService) {
        this.insegnanteRepository = insegnanteRepository;
        this.userService = userService;
    }

    public List<Insegnante> findAll() {
        return insegnanteRepository.findAll();
    }

    /** Insegnanti con i corsi assegnati già caricati (per l'elenco Insegnanti). */
    public List<Insegnante> findAllWithCorsi() {
        return insegnanteRepository.findAllWithCorsi();
    }

    public Optional<Insegnante> findByUsername(String username) {
        return insegnanteRepository.findByUserUsername(username);
    }

    @Transactional
    public Insegnante save(Insegnante insegnante) {
        return insegnanteRepository.save(insegnante);
    }

    /**
     * Salva insieme utente e scheda dell'insegnante (admin e segreteria). Un insegnante nuovo riceve il ruolo
     * INSEGNANTE e richiede la password; per uno esistente {@code rawPassword} vuota lascia quella attuale.
     */
    @Transactional
    public Insegnante salva(Insegnante insegnante, User user, String rawPassword) {
        if (insegnante.getTariffaOraria() != null && insegnante.getTariffaOraria().signum() < 0) {
            throw new IllegalArgumentException("La tariffa oraria non può essere negativa");
        }
        if (insegnante.getId() == null) {
            user.getRoles().add("INSEGNANTE");
        }
        insegnante.setUser(userService.save(user, rawPassword));
        return insegnanteRepository.save(insegnante);
    }
}
