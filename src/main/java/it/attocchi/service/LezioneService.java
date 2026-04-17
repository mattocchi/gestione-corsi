package it.attocchi.service;

import it.attocchi.entity.Lezione;
import it.attocchi.repository.LezioneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class LezioneService {

    private final LezioneRepository lezioneRepository;

    public LezioneService(LezioneRepository lezioneRepository) {
        this.lezioneRepository = lezioneRepository;
    }

    public List<Lezione> findAll() {
        return lezioneRepository.findAll();
    }

    public Lezione findById(Long id) {
        return lezioneRepository.findById(id).orElse(null);
    }

    public List<Lezione> findByCorso(Long corsoId) {
        return lezioneRepository.findByCorsoIdOrderByDataAsc(corsoId);
    }

    public List<Lezione> findByCorsoAndPeriodo(Long corsoId, LocalDate dataInizio, LocalDate dataFine) {
        return lezioneRepository.findByCorsoIdAndDataBetweenOrderByDataAsc(corsoId, dataInizio, dataFine);
    }

    public List<Lezione> findByPeriodo(LocalDate dataInizio, LocalDate dataFine) {
        return lezioneRepository.findByDataBetweenOrderByDataAsc(dataInizio, dataFine);
    }

    @Transactional
    public Lezione save(Lezione lezione) {
        return lezioneRepository.save(lezione);
    }

    @Transactional
    public void delete(Lezione lezione) {
        lezioneRepository.delete(lezione);
    }
}
