package it.attocchi.service;

import it.attocchi.entity.Corso;
import it.attocchi.entity.Iscrizione;
import it.attocchi.entity.Presenza;
import it.attocchi.repository.CorsoRepository;
import it.attocchi.repository.IscrizioneRepository;
import it.attocchi.repository.PresenzaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * "I miei allievi" dell'insegnante: i partecipanti dei suoi corsi attivi (titolare o secondo insegnante), una riga
 * per allievo e corso, con il {@link ConteggioLezioni}; le lezioni di un allievo solo per l'insegnante del corso.
 */
@Service
@Transactional(readOnly = true)
public class MieiAllieviService {

    /** Partecipante di un corso dell'insegnante con le sue lezioni contate. */
    public record AllievoDelCorso(Iscrizione iscrizione, ConteggioLezioni conteggio) {
    }

    private record Chiave(Long corsoId, Long allievoId) {
    }

    private final IscrizioneRepository iscrizioneRepository;
    private final PresenzaRepository presenzaRepository;
    private final CorsoRepository corsoRepository;

    public MieiAllieviService(IscrizioneRepository iscrizioneRepository, PresenzaRepository presenzaRepository,
                              CorsoRepository corsoRepository) {
        this.iscrizioneRepository = iscrizioneRepository;
        this.presenzaRepository = presenzaRepository;
        this.corsoRepository = corsoRepository;
    }

    /** Partecipanti dei corsi attivi dell'insegnante, per nome del corso e nell'ordine dei partecipanti. */
    public List<AllievoDelCorso> allieviDellInsegnante(String username) {
        Map<Chiave, List<Presenza>> presenze = presenzaRepository.findDeiCorsiAttiviDellInsegnante(username).stream()
            .collect(Collectors.groupingBy(p -> new Chiave(p.getLezione().getCorso().getId(), p.getAllievo().getId())));
        return iscrizioneRepository.findDeiCorsiAttiviDellInsegnante(username).stream()
            .sorted(Comparator.comparing((Iscrizione i) -> i.getCorso().getNome(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(i -> i.getCorso().getId())
                .thenComparing(Iscrizione.ORDINE))
            .map(i -> new AllievoDelCorso(i, ConteggioLezioni.di(
                presenze.getOrDefault(new Chiave(i.getCorso().getId(), i.getAllievo().getId()), List.of()),
                i.getCorso().getNumeroLezioni())))
            .toList();
    }

    /** Lezioni (presenze) dell'allievo nel corso, per data; solo per un insegnante del corso, se attivo. */
    public List<Presenza> lezioniDellAllievo(Long corsoId, Long allievoId, String username) {
        Corso corso = corsoRepository.findById(corsoId)
            .orElseThrow(() -> new IllegalArgumentException("Corso non trovato: " + corsoId));
        if (!corso.isAttivo() || !corso.haInsegnante(username)) {
            throw new IllegalStateException("Non puoi vedere gli allievi di questo corso");
        }
        return presenzaRepository.findByCorsoIdAndAllievoId(corsoId, allievoId);
    }
}
