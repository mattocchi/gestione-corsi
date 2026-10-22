package it.attocchi.service;

import it.attocchi.entity.Anagrafica;
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
        if (insegnante.getRimborsoForfettario() != null && insegnante.getRimborsoForfettario().signum() < 0) {
            throw new IllegalArgumentException("Il rimborso forfettario non può essere negativo");
        }
        if (insegnante.getId() == null) {
            user.getRoles().add("INSEGNANTE");
        }
        insegnante.setUser(userService.save(user, rawPassword));
        return insegnanteRepository.save(insegnante);
    }

    /**
     * Profilo personale dell'insegnante: aggiorna contatti, specializzazione, biografia e anagrafica;
     * username e tariffa oraria restano quelli impostati da admin e segreteria.
     */
    @Transactional
    public Insegnante aggiornaProfilo(String username, Insegnante dati, User datiUtente) {
        Insegnante insegnante = insegnanteRepository.findByUserUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("Nessun insegnante per l'utente " + username));
        userService.aggiornaContatti(username, datiUtente);
        insegnante.setSpecializzazione(dati.getSpecializzazione());
        insegnante.setBiografia(dati.getBiografia());
        Anagrafica.copia(dati, insegnante);
        return insegnante;
    }
}
