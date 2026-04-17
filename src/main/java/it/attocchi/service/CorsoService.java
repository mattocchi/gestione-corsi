package it.attocchi.service;

import it.attocchi.entity.Corso;
import it.attocchi.entity.Lezione;
import it.attocchi.entity.Presenza;
import it.attocchi.entity.Studente;
import it.attocchi.repository.CorsoRepository;
import it.attocchi.repository.LezioneRepository;
import it.attocchi.repository.PresenzaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Service
public class CorsoService {

    private final CorsoRepository corsoRepository;
    private final LezioneRepository lezioneRepository;
    private final PresenzaRepository presenzaRepository;

    public CorsoService(CorsoRepository corsoRepository, LezioneRepository lezioneRepository,
                       PresenzaRepository presenzaRepository) {
        this.corsoRepository = corsoRepository;
        this.lezioneRepository = lezioneRepository;
        this.presenzaRepository = presenzaRepository;
    }

    public List<Corso> findAll() {
        return corsoRepository.findAllWithStudenti();
    }

    public List<Corso> findAllAttivi() {
        return corsoRepository.findByAttivoTrue();
    }

    public Corso findById(Long id) {
        return corsoRepository.findById(id).orElse(null);
    }

    public List<Corso> findByDocente(Long docenteId) {
        return corsoRepository.findByDocenteId(docenteId);
    }

    public List<Corso> findByStudente(Long studenteId) {
        return corsoRepository.findByStudenteId(studenteId);
    }

    @Transactional
    public Corso save(Corso corso) {
        return corsoRepository.save(corso);
    }

    @Transactional
    public void delete(Corso corso) {
        corsoRepository.delete(corso);
    }

    @Transactional
    public void generaLezioni(Corso corso) {
        // Elimina lezioni esistenti non completate
        List<Lezione> lezioniEsistenti = lezioneRepository.findByCorsoIdOrderByDataAsc(corso.getId());
        lezioniEsistenti.stream()
            .filter(l -> !l.isCompletata())
            .forEach(lezioneRepository::delete);

        LocalDate dataCorrente = corso.getDataInizio();
        int numeroLezione = 1;

        while (numeroLezione <= corso.getNumeroLezioni()) {
            DayOfWeek giornoSettimana = dataCorrente.getDayOfWeek();

            if (corso.getGiorniSettimana().contains(giornoSettimana)) {
                Lezione lezione = new Lezione(corso, dataCorrente, numeroLezione);
                lezione.setOrarioInizio(corso.getOrarioInizio());
                if (corso.getOrarioInizio() != null && corso.getDurataLezioneMinuti() != null) {
                    lezione.setOrarioFine(corso.getOrarioInizio().plusMinutes(corso.getDurataLezioneMinuti()));
                }
                lezioneRepository.save(lezione);

                // Crea presenze per tutti gli studenti iscritti
                for (Studente studente : corso.getStudenti()) {
                    Presenza presenza = new Presenza(lezione, studente, Presenza.StatoPresenza.PRESENTE);
                    presenzaRepository.save(presenza);
                }

                numeroLezione++;
            }
            dataCorrente = dataCorrente.plusDays(1);
        }
    }
}
