package it.attocchi.service;

import it.attocchi.entity.Studente;
import it.attocchi.entity.User;
import it.attocchi.repository.PagamentoRepository;
import it.attocchi.repository.StudenteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class StudenteService {

    private final StudenteRepository studenteRepository;
    private final PagamentoRepository pagamentoRepository;
    private final UserService userService;

    public StudenteService(StudenteRepository studenteRepository, PagamentoRepository pagamentoRepository,
                           UserService userService) {
        this.studenteRepository = studenteRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.userService = userService;
    }

    public List<Studente> findAll() {
        return studenteRepository.findAllWithCorsi();
    }

    public Studente findById(Long id) {
        return studenteRepository.findById(id).orElse(null);
    }

    public Studente findByUserId(Long userId) {
        return studenteRepository.findByUserId(userId).orElse(null);
    }

    public Optional<Studente> findByUsername(String username) {
        return studenteRepository.findByUserUsername(username);
    }

    public List<Studente> findByGenitore(Long genitoreId) {
        return studenteRepository.findByGenitoreIdWithCorsi(genitoreId);
    }

    @Transactional
    public Studente save(Studente studente) {
        return studenteRepository.save(studente);
    }

    /**
     * Salva insieme utente e scheda dello studente (admin e segreteria). Uno studente nuovo riceve il ruolo
     * STUDENTE e richiede la password; per uno esistente {@code rawPassword} vuota lascia quella attuale.
     */
    @Transactional
    public Studente salva(Studente studente, User user, String rawPassword) {
        if (studente.getId() == null) {
            user.getRoles().add("STUDENTE");
        }
        studente.setUser(userService.save(user, rawPassword));
        return studenteRepository.save(studente);
    }

    @Transactional
    public void delete(Studente studente) {
        if (pagamentoRepository.existsByStudente(studente.getId())) {
            throw new IllegalStateException("Lo studente ha pagamenti registrati: eliminali prima di eliminarlo");
        }
        studenteRepository.delete(studente);
    }
}
