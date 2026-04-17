package it.attocchi.service;

import it.attocchi.entity.Studente;
import it.attocchi.repository.StudenteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StudenteService {

    private final StudenteRepository studenteRepository;

    public StudenteService(StudenteRepository studenteRepository) {
        this.studenteRepository = studenteRepository;
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

    public List<Studente> findByGenitore(Long genitoreId) {
        return studenteRepository.findByGenitoreId(genitoreId);
    }

    @Transactional
    public Studente save(Studente studente) {
        return studenteRepository.save(studente);
    }

    @Transactional
    public void delete(Studente studente) {
        studenteRepository.delete(studente);
    }
}
