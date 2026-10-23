package it.attocchi.service;

import it.attocchi.entity.GruppoCorso;
import it.attocchi.entity.Lezione;
import it.attocchi.repository.GruppoCorsoRepository;
import it.attocchi.repository.LezioneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Numero delle lezioni nei corsi con gruppi: ogni giorno della settimana ha la sua numerazione (con i gruppi del lunedì
 * e del venerdì la lezione 1 è quella della prima settimana per entrambi), così ogni gruppo conta le sue lezioni. Si
 * ricalcola dalle lezioni in ordine di data: una lezione non effettuata tiene il suo numero, il suo recupero prende lo
 * stesso numero. I corsi senza gruppi tengono il contatore unico di sempre.
 */
@Service
@Transactional(readOnly = true)
public class NumerazioneLezioni {

    private final LezioneRepository lezioneRepository;
    private final GruppoCorsoRepository gruppoRepository;

    public NumerazioneLezioni(LezioneRepository lezioneRepository, GruppoCorsoRepository gruppoRepository) {
        this.lezioneRepository = lezioneRepository;
        this.gruppoRepository = gruppoRepository;
    }

    /** Rinumera le lezioni del corso se ha gruppi (per giorno della settimana); altrimenti non cambia nulla. */
    @Transactional
    public void rinumera(Long corsoId) {
        if (!gruppoRepository.findByCorsoId(corsoId).isEmpty()) {
            numera(corsoId, true);
        }
    }

    /** Dopo l'eliminazione dell'ultimo gruppo: un contatore unico in ordine di data, come per i corsi senza gruppi. */
    @Transactional
    public void rinumeraSenzaGruppi(Long corsoId) {
        if (gruppoRepository.findByCorsoId(corsoId).isEmpty()) {
            numera(corsoId, false);
        }
    }

    /** Rinumera tutti i corsi con gruppi (all'avvio); restituisce quanti corsi ha rinumerato. */
    @Transactional
    public int rinumeraTutti() {
        Set<Long> conGruppi = gruppoRepository.findAll().stream().map(g -> g.getCorso().getId()).collect(Collectors.toSet());
        conGruppi.forEach(corsoId -> numera(corsoId, true));
        return conGruppi.size();
    }

    private void numera(Long corsoId, boolean perGiorno) {
        List<Lezione> lezioni = lezioneRepository.findByCorsoIdOrderByDataAsc(corsoId).stream()
            .sorted(Comparator.comparing(Lezione::getData).thenComparing(Lezione::getId))
            .toList();
        Map<DayOfWeek, Integer> contatori = new EnumMap<>(DayOfWeek.class);
        int contatoreUnico = 0;
        Map<Long, Integer> numeri = new HashMap<>();
        // Prima le lezioni normali, poi i recuperi prendono il numero della lezione saltata
        for (Lezione lezione : lezioni) {
            if (lezione.getRecuperoDi() != null) {
                continue;
            }
            int numero = perGiorno ? contatori.merge(lezione.getData().getDayOfWeek(), 1, Integer::sum) : ++contatoreUnico;
            numeri.put(lezione.getId(), numero);
            lezione.setNumeroLezione(numero);
        }
        for (Lezione lezione : lezioni) {
            if (lezione.getRecuperoDi() != null) {
                lezione.setNumeroLezione(numeri.getOrDefault(lezione.getRecuperoDi().getId(), lezione.getNumeroLezione()));
            }
        }
    }

    /** Gruppi per id del corso (per l'etichetta del numero nelle liste). */
    public Map<Long, List<GruppoCorso>> gruppiPerCorso() {
        return gruppoRepository.findAll().stream().collect(Collectors.groupingBy(g -> g.getCorso().getId()));
    }
}
