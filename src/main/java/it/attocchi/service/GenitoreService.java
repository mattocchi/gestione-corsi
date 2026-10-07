package it.attocchi.service;

import it.attocchi.entity.Genitore;
import it.attocchi.repository.GenitoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class GenitoreService {

    private final GenitoreRepository genitoreRepository;

    public GenitoreService(GenitoreRepository genitoreRepository) {
        this.genitoreRepository = genitoreRepository;
    }

    public List<Genitore> findAll() {
        return genitoreRepository.findAll();
    }

    public Optional<Genitore> findByUsername(String username) {
        return genitoreRepository.findByUserUsername(username);
    }

    @Transactional
    public Genitore save(Genitore genitore) {
        return genitoreRepository.save(genitore);
    }
}
