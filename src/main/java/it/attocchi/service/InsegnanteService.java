package it.attocchi.service;

import it.attocchi.entity.Anagrafica;
import it.attocchi.entity.Insegnante;
import it.attocchi.entity.User;
import it.attocchi.repository.CorsoRepository;
import it.attocchi.repository.InsegnanteRepository;
import it.attocchi.repository.LetteraIncaricoRepository;
import it.attocchi.repository.LezioneRepository;
import it.attocchi.repository.RimborsoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class InsegnanteService {

    private final InsegnanteRepository insegnanteRepository;
    private final UserService userService;
    private final CorsoRepository corsoRepository;
    private final LezioneRepository lezioneRepository;
    private final RimborsoRepository rimborsoRepository;
    private final LetteraIncaricoRepository letteraIncaricoRepository;

    public InsegnanteService(InsegnanteRepository insegnanteRepository, UserService userService,
                             CorsoRepository corsoRepository, LezioneRepository lezioneRepository,
                             RimborsoRepository rimborsoRepository, LetteraIncaricoRepository letteraIncaricoRepository) {
        this.letteraIncaricoRepository = letteraIncaricoRepository;
        this.insegnanteRepository = insegnanteRepository;
        this.userService = userService;
        this.corsoRepository = corsoRepository;
        this.lezioneRepository = lezioneRepository;
        this.rimborsoRepository = rimborsoRepository;
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
     * Motivo per cui la scheda dell'insegnante non si può eliminare (corsi, lezioni o rimborsi collegati); {@code null}
     * se si può.
     */
    public String motivoPerNonEliminare(Long insegnanteId) {
        if (corsoRepository.esisteConInsegnante(insegnanteId)) {
            return "è insegnante di uno o più corsi: cambia prima l'insegnante dei corsi";
        }
        if (!lezioneRepository.findConInsegnantePresente(insegnanteId).isEmpty()) {
            return "risulta presente a delle lezioni";
        }
        if (rimborsoRepository.existsByInsegnanteId(insegnanteId)) {
            return "ha dei rimborsi registrati";
        }
        if (letteraIncaricoRepository.existsByInsegnanteId(insegnanteId)) {
            return "ha delle lettere di incarico";
        }
        return null;
    }

    /**
     * Elimina la scheda dell'insegnante ma non il suo utente, a cui si toglie il ruolo INSEGNANTE; non si può se ha
     * corsi, lezioni o rimborsi ({@link IllegalStateException}).
     */
    @Transactional
    public void elimina(Long insegnanteId) {
        Insegnante insegnante = insegnanteRepository.findById(insegnanteId)
            .orElseThrow(() -> new IllegalArgumentException("Insegnante non trovato: " + insegnanteId));
        String motivo = motivoPerNonEliminare(insegnanteId);
        if (motivo != null) {
            throw new IllegalStateException(insegnante.getNomeCompleto() + " " + motivo);
        }
        if (insegnante.getUser() != null) {
            insegnante.getUser().getRoles().remove("INSEGNANTE");
        }
        insegnanteRepository.delete(insegnante);
    }

    /**
     * Salva insieme utente e scheda dell'insegnante (admin e segreteria). Un insegnante nuovo riceve il ruolo
     * INSEGNANTE; senza username non ha accesso: username generato, password casuale e utente disattivato
     * ({@link UserService#accessoGeneratoSeManca}). Con lo username serve la password. Per uno esistente
     * {@code rawPassword} vuota lascia quella attuale.
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
            rawPassword = userService.accessoGeneratoSeManca(user, rawPassword);
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
