package it.attocchi.service;

import it.attocchi.entity.Corso;
import it.attocchi.repository.CorsoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CorsoService {

    private final CorsoRepository corsoRepository;

    public CorsoService(CorsoRepository corsoRepository) {
        this.corsoRepository = corsoRepository;
    }

    public List<Corso> findAll() {
        return corsoRepository.findByOrderByNomeAsc();
    }

    public Optional<Corso> findById(Long id) {
        return corsoRepository.findById(id);
    }

    public Corso save(Corso corso) {
        return corsoRepository.save(corso);
    }

    public void delete(Corso corso) {
        corsoRepository.delete(corso);
    }

    public List<Corso> search(String searchTerm) {
        if (searchTerm == null || searchTerm.isEmpty()) {
            return findAll();
        }
        return corsoRepository.findByNomeContainingIgnoreCase(searchTerm);
    }

    public List<Corso> findCorsiAttivi() {
        return corsoRepository.findCorsiAttiviAllaData(LocalDate.now());
    }

    public List<Corso> findByDocente(Long docenteId) {
        return corsoRepository.findByDocenteId(docenteId);
    }
}
