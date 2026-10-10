package it.attocchi.service;

import it.attocchi.entity.Insegnante;
import it.attocchi.repository.InsegnanteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class InsegnanteService {

    private final InsegnanteRepository insegnanteRepository;

    public InsegnanteService(InsegnanteRepository insegnanteRepository) {
        this.insegnanteRepository = insegnanteRepository;
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
}
