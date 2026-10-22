package it.attocchi.service;

import it.attocchi.entity.Allievo;
import it.attocchi.entity.Anagrafica;
import it.attocchi.entity.Genitore;
import it.attocchi.entity.User;
import it.attocchi.repository.AllievoRepository;
import it.attocchi.repository.GenitoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class GenitoreService {

    private final GenitoreRepository genitoreRepository;
    private final UserService userService;
    private final AllievoRepository allievoRepository;

    public GenitoreService(GenitoreRepository genitoreRepository, UserService userService,
                           AllievoRepository allievoRepository) {
        this.genitoreRepository = genitoreRepository;
        this.userService = userService;
        this.allievoRepository = allievoRepository;
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
     * GENITORE; senza username non ha accesso: username generato, password casuale e utente disattivato
     * ({@link UserService#accessoGeneratoSeManca}). Con lo username serve la password. Per uno esistente
     * {@code rawPassword} vuota lascia quella attuale.
     */
    @Transactional
    public Genitore salva(Genitore genitore, User user, String rawPassword) {
        if (genitore.getId() == null) {
            user.getRoles().add("GENITORE");
            rawPassword = userService.accessoGeneratoSeManca(user, rawPassword);
        }
        genitore.setUser(userService.save(user, rawPassword));
        return genitoreRepository.save(genitore);
    }

    /**
     * Salva il genitore come {@link #salva} e ne imposta i figli: gli allievi indicati diventano suoi figli (anche se
     * avevano un altro genitore), quelli tolti restano senza genitore. Restituisce gli allievi spostati da un altro
     * genitore, come "Nome (prima figlio di Altro)".
     */
    @Transactional
    public List<String> salvaConFigli(Genitore genitore, User user, String rawPassword, Collection<Long> figliIds) {
        Genitore salvato = salva(genitore, user, rawPassword);
        List<String> spostati = new ArrayList<>();
        for (Allievo allievo : allievoRepository.findAll()) {
            boolean scelto = figliIds.contains(allievo.getId());
            Genitore attuale = allievo.getGenitore();
            if (scelto && !salvato.equals(attuale)) {
                if (attuale != null) {
                    spostati.add(allievo.getNomeCompleto() + " (prima figlio di " + attuale.getNomeCompleto() + ")");
                }
                allievo.setGenitore(salvato);
            } else if (!scelto && salvato.equals(attuale)) {
                allievo.setGenitore(null);
            }
        }
        return spostati;
    }

    /**
     * Elimina la scheda del genitore ma non il suo utente: i figli restano senza genitore e all'utente si toglie il
     * ruolo GENITORE (se era l'unico, resta senza ruoli). Restituisce quanti figli sono rimasti senza genitore.
     */
    @Transactional
    public int elimina(Long genitoreId) {
        Genitore genitore = genitoreRepository.findById(genitoreId)
            .orElseThrow(() -> new IllegalArgumentException("Genitore non trovato: " + genitoreId));
        List<Allievo> figli = allievoRepository.findAll().stream().filter(a -> genitore.equals(a.getGenitore())).toList();
        figli.forEach(figlio -> figlio.setGenitore(null));
        if (genitore.getUser() != null) {
            genitore.getUser().getRoles().remove("GENITORE");
        }
        genitoreRepository.delete(genitore);
        return figli.size();
    }

    /** Profilo personale del genitore: aggiorna contatti e dati anagrafici (username e ruoli restano invariati). */
    @Transactional
    public Genitore aggiornaProfilo(String username, Genitore dati, User datiUtente) {
        Genitore genitore = genitoreRepository.findByUserUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("Nessun genitore per l'utente " + username));
        userService.aggiornaContatti(username, datiUtente);
        Anagrafica.copia(dati, genitore);
        return genitore;
    }
}
