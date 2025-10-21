package it.attocchi.service;

import it.attocchi.entity.Docente;
import it.attocchi.repository.DocenteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class DocenteService {

    private final DocenteRepository docenteRepository;

    public DocenteService(DocenteRepository docenteRepository) {
        this.docenteRepository = docenteRepository;
    }

    public List<Docente> findAll() {
        return docenteRepository.findByOrderByCognomeAscNomeAsc();
    }

    public Optional<Docente> findById(Long id) {
        return docenteRepository.findById(id);
    }

    public Docente save(Docente docente) {
        return docenteRepository.save(docente);
    }

    public void delete(Docente docente) {
        docenteRepository.delete(docente);
    }

    public List<Docente> search(String searchTerm) {
        if (searchTerm == null || searchTerm.isEmpty()) {
            return findAll();
        }
        return docenteRepository.findByNomeContainingIgnoreCaseOrCognomeContainingIgnoreCase(searchTerm, searchTerm);
    }
}
