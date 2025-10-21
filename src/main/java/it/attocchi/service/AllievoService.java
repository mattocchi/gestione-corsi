package it.attocchi.service;

import it.attocchi.entity.Allievo;
import it.attocchi.repository.AllievoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AllievoService {

    private final AllievoRepository AllievoRepository;

    public AllievoService(AllievoRepository AllievoRepository) {
        this.AllievoRepository = AllievoRepository;
    }

    public List<Allievo> findAll() {
        return AllievoRepository.findByOrderByCognomeAscNomeAsc();
    }

    public Optional<Allievo> findById(Long id) {
        return AllievoRepository.findById(id);
    }

    public Allievo save(Allievo allievo) {
        return AllievoRepository.save(allievo);
    }

    public void delete(Allievo allievo) {
        AllievoRepository.delete(allievo);
    }

    public List<Allievo> search(String searchTerm) {
        if (searchTerm == null || searchTerm.isEmpty()) {
            return findAll();
        }
        return AllievoRepository.findByNomeContainingIgnoreCaseOrCognomeContainingIgnoreCase(searchTerm, searchTerm);
    }
}
