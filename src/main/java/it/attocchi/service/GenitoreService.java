package it.attocchi.service;

import it.attocchi.entity.Genitore;
import it.attocchi.repository.GenitoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class GenitoreService {

    private final GenitoreRepository genitoreRepository;

    public GenitoreService(GenitoreRepository genitoreRepository) {
        this.genitoreRepository = genitoreRepository;
    }

    public List<Genitore> findAll() {
        return genitoreRepository.findByOrderByCognomeAscNomeAsc();
    }

    public Optional<Genitore> findById(Long id) {
        return genitoreRepository.findById(id);
    }

    public Genitore save(Genitore genitore) {
        return genitoreRepository.save(genitore);
    }

    public void delete(Genitore genitore) {
        genitoreRepository.delete(genitore);
    }

    public List<Genitore> search(String searchTerm) {
        if (searchTerm == null || searchTerm.isEmpty()) {
            return findAll();
        }
        return genitoreRepository.findByNomeContainingIgnoreCaseOrCognomeContainingIgnoreCase(searchTerm, searchTerm);
    }
}
