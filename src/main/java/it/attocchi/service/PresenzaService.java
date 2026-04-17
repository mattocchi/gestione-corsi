package it.attocchi.service;

import it.attocchi.entity.Presenza;
import it.attocchi.repository.PresenzaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PresenzaService {

    private final PresenzaRepository presenzaRepository;

    public PresenzaService(PresenzaRepository presenzaRepository) {
        this.presenzaRepository = presenzaRepository;
    }

    public List<Presenza> findAll() {
        return presenzaRepository.findAll();
    }

    public Presenza findById(Long id) {
        return presenzaRepository.findById(id).orElse(null);
    }

    public List<Presenza> findByLezione(Long lezioneId) {
        return presenzaRepository.findByLezioneId(lezioneId);
    }

    public List<Presenza> findByStudente(Long studenteId) {
        return presenzaRepository.findByStudenteId(studenteId);
    }

    public Presenza findByLezioneAndStudente(Long lezioneId, Long studenteId) {
        return presenzaRepository.findByLezioneIdAndStudenteId(lezioneId, studenteId).orElse(null);
    }

    public List<Presenza> findByCorsoAndStudente(Long corsoId, Long studenteId) {
        return presenzaRepository.findByCorsoIdAndStudenteId(corsoId, studenteId);
    }

    @Transactional
    public Presenza save(Presenza presenza) {
        return presenzaRepository.save(presenza);
    }

    @Transactional
    public void delete(Presenza presenza) {
        presenzaRepository.delete(presenza);
    }
}
