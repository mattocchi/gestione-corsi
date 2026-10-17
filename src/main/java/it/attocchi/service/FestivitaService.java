package it.attocchi.service;

import it.attocchi.entity.Festivita;
import it.attocchi.entity.Festivita.Tipo;
import it.attocchi.repository.FestivitaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.MonthDay;
import java.util.List;

/** Festività saltate dalla generazione delle lezioni (gestite da admin e segreteria). */
@Service
@Transactional(readOnly = true)
public class FestivitaService {

    private final FestivitaRepository festivitaRepository;

    public FestivitaService(FestivitaRepository festivitaRepository) {
        this.festivitaRepository = festivitaRepository;
    }

    /** Tutte le festività, per nome. */
    public List<Festivita> elenco() {
        return festivitaRepository.findAllByOrderByNomeAsc();
    }

    /** Calendario delle festività attive, da leggere una volta per ogni elenco o generazione. */
    public CalendarioFestivita calendario() {
        return new CalendarioFestivita(festivitaRepository.findByAttivoTrue());
    }

    /**
     * Salva una festività nuova o modificata. Tiene solo i campi del suo tipo; per "Ogni anno" serve un giorno valido
     * (fine facoltativa ma completa), per "Date precise" la data di inizio (fine facoltativa, non prima dell'inizio).
     */
    @Transactional
    public Festivita salva(Festivita festivita) {
        if (festivita.getNome() == null || festivita.getNome().isBlank()) {
            throw new IllegalArgumentException("Inserisci il nome della festività");
        }
        festivita.setNome(festivita.getNome().strip());
        if (festivita.getTipo() == null) {
            throw new IllegalArgumentException("Scegli il tipo di festività");
        }
        if (festivita.getTipo() != Tipo.OGNI_ANNO) {
            festivita.setGiornoInizio(null);
            festivita.setMeseInizio(null);
            festivita.setGiornoFine(null);
            festivita.setMeseFine(null);
        }
        if (festivita.getTipo() != Tipo.DATE) {
            festivita.setDataInizio(null);
            festivita.setDataFine(null);
        }
        switch (festivita.getTipo()) {
            case OGNI_ANNO -> {
                verificaGiorno(festivita.getGiornoInizio(), festivita.getMeseInizio(), "Inserisci un giorno valido");
                if (festivita.getGiornoFine() != null || festivita.getMeseFine() != null) {
                    verificaGiorno(festivita.getGiornoFine(), festivita.getMeseFine(),
                        "Inserisci giorno e mese di fine validi, oppure nessuno dei due");
                }
            }
            case DATE -> {
                if (festivita.getDataInizio() == null) {
                    throw new IllegalArgumentException("Inserisci la data");
                }
                if (festivita.getDataFine() != null && festivita.getDataFine().isBefore(festivita.getDataInizio())) {
                    throw new IllegalArgumentException("La data di fine non può precedere quella di inizio");
                }
            }
            case PASQUA, PASQUETTA -> {
                // Date calcolate ogni anno
            }
        }
        return festivitaRepository.save(festivita);
    }

    private static void verificaGiorno(Integer giorno, Integer mese, String messaggio) {
        if (giorno == null || mese == null) {
            throw new IllegalArgumentException(messaggio);
        }
        try {
            MonthDay.of(mese, giorno);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException(messaggio);
        }
    }

    @Transactional
    public void impostaAttiva(Long id, boolean attiva) {
        festivitaRepository.findById(id).orElseThrow().setAttivo(attiva);
    }

    @Transactional
    public void elimina(Long id) {
        festivitaRepository.deleteById(id);
    }

    /** Se l'elenco è vuoto lo popola con le festività nazionali italiane. */
    @Transactional
    public void inizializzaSeVuoto() {
        if (festivitaRepository.count() > 0) {
            return;
        }
        festivitaRepository.saveAll(List.of(
            Festivita.ogniAnno("Capodanno", 1, 1),
            Festivita.ogniAnno("Epifania", 6, 1),
            Festivita.calcolata("Pasqua", Tipo.PASQUA),
            Festivita.calcolata("Lunedì dell'Angelo", Tipo.PASQUETTA),
            Festivita.ogniAnno("Festa della Liberazione", 25, 4),
            Festivita.ogniAnno("Festa del Lavoro", 1, 5),
            Festivita.ogniAnno("Festa della Repubblica", 2, 6),
            Festivita.ogniAnno("Ferragosto", 15, 8),
            Festivita.ogniAnno("San Francesco d'Assisi", 4, 10),
            Festivita.ogniAnno("Ognissanti", 1, 11),
            Festivita.ogniAnno("Immacolata Concezione", 8, 12),
            Festivita.ogniAnno("Natale", 25, 12),
            Festivita.ogniAnno("Santo Stefano", 26, 12)));
    }
}
