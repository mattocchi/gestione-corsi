package it.attocchi.service;

import it.attocchi.entity.Anagrafica;
import it.attocchi.entity.Allievo;
import it.attocchi.entity.IntestazioneDocumenti;
import it.attocchi.entity.User;
import it.attocchi.repository.PagamentoRepository;
import it.attocchi.repository.AllievoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

    /**
     * {@code true} se l'utente è un allievo senza genitore (adulto): gestisce da sé i pagamenti, come un genitore di
     * sé stesso. La stessa regola manda a lui email e documenti.
     */
    public boolean gestisceDaSe(String username) {
        return username != null && allievoRepository.findByUserUsername(username).map(a -> a.getGenitore() == null).orElse(false);
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
            rawPassword = userService.accessoGeneratoSeManca(user, rawPassword);
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

    /**
     * L'allievo adulto aggiorna l'intestazione della propria attestazione delle spese (unico dato che modifica da sé);
     * rifiutato all'allievo con genitore.
     */
    @Transactional
    public Allievo aggiornaIntestazione(String username, IntestazioneDocumenti dati) {
        Allievo allievo = allievoRepository.findByUserUsername(username)
            .filter(a -> a.getGenitore() == null)
            .orElseThrow(() -> new IllegalArgumentException("Solo l'allievo adulto imposta l'intestazione dei propri documenti"));
        IntestazioneDocumenti.copia(dati, allievo);
        return allievo;
    }

    /**
     * Nuovo allievo con i soli dati essenziali (inserimento rapido dai partecipanti di un corso): nome e cognome
     * obbligatori, email e telefono facoltativi, nessun genitore; come ogni allievo creato senza username ha un
     * utente generato e disattivato. Il resto si completa da Gestione Allievi.
     */
    @Transactional
    public Allievo creaRapido(String nome, String cognome, String email, String telefono) {
        if (nome == null || nome.isBlank() || cognome == null || cognome.isBlank()) {
            throw new IllegalArgumentException("Nome e cognome sono obbligatori");
        }
        User user = new User();
        user.setNome(nome.strip());
        user.setCognome(cognome.strip());
        user.setEmail(email != null && !email.isBlank() ? email.strip() : null);
        user.setTelefono(telefono != null && !telefono.isBlank() ? telefono.strip() : null);
        return salva(new Allievo(), user, null);
    }

    /** Allievi con lo stesso nome e cognome (senza distinguere maiuscole e spazi ai lati), per evitare doppioni. */
    public List<Allievo> omonimi(String nome, String cognome) {
        String n = nome != null ? nome.strip() : "";
        String c = cognome != null ? cognome.strip() : "";
        return allievoRepository.findAllWithCorsi().stream()
            .filter(a -> a.getUser() != null && n.equalsIgnoreCase(Objects.toString(a.getUser().getNome(), "").strip())
                && c.equalsIgnoreCase(Objects.toString(a.getUser().getCognome(), "").strip()))
            .toList();
    }

    @Transactional
    public void delete(Allievo allievo) {
        if (pagamentoRepository.existsByAllievo(allievo.getId())) {
            throw new IllegalStateException("L'allievo ha pagamenti registrati: eliminali prima di eliminarlo");
        }
        allievoRepository.delete(allievo);
    }
}
