package it.attocchi.service;

import it.attocchi.entity.Allievo;
import it.attocchi.entity.Corso;
import it.attocchi.entity.Presenza;
import it.attocchi.repository.PresenzaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PresenzaService {

    private final PresenzaRepository presenzaRepository;

    public PresenzaService(PresenzaRepository presenzaRepository) {
        this.presenzaRepository = presenzaRepository;
    }

    public List<Presenza> findAll() {
        return presenzaRepository.findAll();
    }

    public Optional<Presenza> findById(Long id) {
        return presenzaRepository.findById(id);
    }

    public Presenza save(Presenza presenza) {
        return presenzaRepository.save(presenza);
    }

    public void delete(Presenza presenza) {
        presenzaRepository.delete(presenza);
    }

    public List<Presenza> findByCorsoAndData(Long corsoId, LocalDate dataLezione) {
        return presenzaRepository.findByCorsoIdAndDataLezione(corsoId, dataLezione);
    }

    public List<Presenza> findByAllievo(Long allievoId) {
        return presenzaRepository.findByAllievoIdOrderByDataLezioneDesc(allievoId);
    }

    public List<Presenza> findByCorso(Long corsoId) {
        return presenzaRepository.findByCorsoIdOrderByDataLezioneDesc(corsoId);
    }

    public Optional<Presenza> findByCorsoAllievoData(Long corsoId, Long allievoId, LocalDate dataLezione) {
        return presenzaRepository.findByCorsoIdAndAllievoIdAndDataLezione(corsoId, allievoId, dataLezione);
    }

    public void creaOAggiorna(Corso corso, Allievo allievo, LocalDate dataLezione, Presenza.StatoPresenza stato) {
        Optional<Presenza> esistente = findByCorsoAllievoData(corso.getId(), allievo.getId(), dataLezione);
        
        if (esistente.isPresent()) {
            Presenza presenza = esistente.get();
            presenza.setStato(stato);
            save(presenza);
        } else {
            Presenza nuovaPresenza = new Presenza(corso, allievo, dataLezione, stato);
            save(nuovaPresenza);
        }
    }

    public List<Presenza> findByCorsoAndPeriodo(Long corsoId, LocalDate dataInizio, LocalDate dataFine) {
        return presenzaRepository.findPresenzeByCorsoAndPeriodo(corsoId, dataInizio, dataFine);
    }
}
