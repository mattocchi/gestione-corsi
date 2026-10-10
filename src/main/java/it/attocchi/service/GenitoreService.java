package it.attocchi.service;

import it.attocchi.entity.Genitore;
import it.attocchi.entity.User;
import it.attocchi.repository.GenitoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class GenitoreService {

    private final GenitoreRepository genitoreRepository;
    private final UserService userService;

    public GenitoreService(GenitoreRepository genitoreRepository, UserService userService) {
        this.genitoreRepository = genitoreRepository;
        this.userService = userService;
    }

    public List<Genitore> findAll() {
        return genitoreRepository.findAll();
    }

    /** Genitori con i figli già caricati (per l'elenco Genitori). */
    public List<Genitore> findAllWithFigli() {
        return genitoreRepository.findAllWithFigli();
    }

    public Optional<Genitore> findByUsername(String username) {
        return genitoreRepository.findByUserUsername(username);
    }

    @Transactional
    public Genitore save(Genitore genitore) {
        return genitoreRepository.save(genitore);
    }

    /**
     * Salva insieme utente e scheda del genitore (admin e segreteria). Un genitore nuovo riceve il ruolo
     * GENITORE e richiede la password; per uno esistente {@code rawPassword} vuota lascia quella attuale.
     */
    @Transactional
    public Genitore salva(Genitore genitore, User user, String rawPassword) {
        if (genitore.getId() == null) {
            user.getRoles().add("GENITORE");
        }
        genitore.setUser(userService.save(user, rawPassword));
        return genitoreRepository.save(genitore);
    }
}
