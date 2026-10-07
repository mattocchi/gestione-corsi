package it.attocchi.service;

import it.attocchi.entity.Docente;
import it.attocchi.repository.DocenteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class DocenteService {

    private final DocenteRepository docenteRepository;

    public DocenteService(DocenteRepository docenteRepository) {
        this.docenteRepository = docenteRepository;
    }

    public List<Docente> findAll() {
        return docenteRepository.findAll();
    }

    public Optional<Docente> findByUsername(String username) {
        return docenteRepository.findByUserUsername(username);
    }

    @Transactional
    public Docente save(Docente docente) {
        return docenteRepository.save(docente);
    }
}
