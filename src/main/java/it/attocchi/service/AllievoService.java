package it.attocchi.service;

import it.attocchi.entity.Anagrafica;
import it.attocchi.entity.Allievo;
import it.attocchi.entity.User;
import it.attocchi.repository.PagamentoRepository;
import it.attocchi.repository.AllievoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AllievoService {

    private final AllievoRepository allievoRepository;
    private final PagamentoRepository pagamentoRepository;
    private final UserService userService;

    public AllievoService(AllievoRepository allievoRepository, PagamentoRepository pagamentoRepository,
                           UserService userService) {
        this.allievoRepository = allievoRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.userService = userService;
    }

    public List<Allievo> findAll() {
        return allievoRepository.findAllWithCorsi();
    }

    public Allievo findById(Long id) {
        return allievoRepository.findById(id).orElse(null);
    }

    public Allievo findByUserId(Long userId) {
        return allievoRepository.findByUserId(userId).orElse(null);
    }

    public Optional<Allievo> findByUsername(String username) {
        return allievoRepository.findByUserUsername(username);
    }

    public List<Allievo> findByGenitore(Long genitoreId) {
        return allievoRepository.findByGenitoreIdWithCorsi(genitoreId);
    }

    @Transactional
    public Allievo save(Allievo allievo) {
        return allievoRepository.save(allievo);
    }

    /**
     * Salva insieme utente e scheda dell'allievo (admin e segreteria). Un allievo nuovo riceve il ruolo
     * ALLIEVO; senza username non ha accesso: username generato da nome e cognome, password casuale e utente
     * disattivato (si attiva da Gestione Utenti). Con lo username serve la password. Per un allievo esistente
     * {@code rawPassword} vuota lascia quella attuale.
     */
    @Transactional
    public Allievo salva(Allievo allievo, User user, String rawPassword) {
        if (allievo.getId() == null) {
            user.getRoles().add("ALLIEVO");
            if (user.getUsername() == null || user.getUsername().isBlank()) {
                user.setUsername(userService.usernameLibero(user.getNome(), user.getCognome()));
                user.setEnabled(false);
                rawPassword = UUID.randomUUID().toString();
            }
        }
        allievo.setUser(userService.save(user, rawPassword));
        return allievoRepository.save(allievo);
    }

    /**
     * Il genitore aggiorna contatti e dati anagrafici di un proprio figlio; username, genitore e corsi restano
     * invariati. Rifiuta gli allievi che non sono figli dell'utente indicato.
     */
    @Transactional
    public Allievo aggiornaDaGenitore(String usernameGenitore, Long allievoId, Allievo dati, User datiUtente) {
        Allievo allievo = allievoRepository.findById(allievoId)
            .orElseThrow(() -> new IllegalArgumentException("Allievo non trovato: " + allievoId));
        if (allievo.getGenitore() == null || !allievo.getGenitore().getUser().getUsername().equals(usernameGenitore)) {
            throw new IllegalArgumentException("L'allievo non è un figlio dell'utente " + usernameGenitore);
        }
        userService.aggiornaContatti(allievo.getUser().getUsername(), datiUtente);
        Anagrafica.copia(dati, allievo);
        return allievo;
    }

    @Transactional
    public void delete(Allievo allievo) {
        if (pagamentoRepository.existsByAllievo(allievo.getId())) {
            throw new IllegalStateException("L'allievo ha pagamenti registrati: eliminali prima di eliminarlo");
        }
        allievoRepository.delete(allievo);
    }
}
