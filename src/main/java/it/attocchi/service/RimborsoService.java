package it.attocchi.service;

import it.attocchi.entity.Insegnante;
import it.attocchi.entity.Rimborso;
import it.attocchi.repository.InsegnanteRepository;
import it.attocchi.repository.RimborsoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Rimborsi spese agli insegnanti, registrati da admin e segreteria (eliminazione solo admin, controllata dalla view). */
@Service
@Transactional(readOnly = true)
public class RimborsoService {

    private final RimborsoRepository rimborsoRepository;
    private final InsegnanteRepository insegnanteRepository;

    public RimborsoService(RimborsoRepository rimborsoRepository, InsegnanteRepository insegnanteRepository) {
        this.rimborsoRepository = rimborsoRepository;
        this.insegnanteRepository = insegnanteRepository;
    }

    /** Primo giorno dell'anno scolastico "2026/2027" (1 settembre 2026). */
    public static LocalDate inizioAnno(String annoScolastico) {
        return LocalDate.of(Integer.parseInt(annoScolastico.substring(0, 4)), 9, 1);
    }

    /** Ultimo giorno dell'anno scolastico (31 agosto dell'anno successivo). */
    public static LocalDate fineAnno(String annoScolastico) {
        return inizioAnno(annoScolastico).plusYears(1).minusDays(1);
    }

    /** Rimborsi dell'insegnante con mese di riferimento nell'anno scolastico, dal più recente. */
    public List<Rimborso> rimborsi(Long insegnanteId, String annoScolastico) {
        return rimborsoRepository.findByInsegnanteAndPeriodo(insegnanteId, inizioAnno(annoScolastico), fineAnno(annoScolastico));
    }

    /** Totale dei rimborsi dell'insegnante nell'anno scolastico. */
    public BigDecimal totale(Long insegnanteId, String annoScolastico) {
        return rimborsi(insegnanteId, annoScolastico).stream().map(Rimborso::getImporto).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Aggiunge o modifica un rimborso dell'insegnante. */
    @Transactional
    public Rimborso salva(Long insegnanteId, Rimborso rimborso) {
        if (rimborso.getImporto() == null || rimborso.getImporto().signum() <= 0) {
            throw new IllegalArgumentException("L'importo deve essere maggiore di zero");
        }
        if (rimborso.getData() == null || rimborso.getMese() == null || rimborso.getMetodo() == null) {
            throw new IllegalArgumentException("Data, mese di riferimento e metodo sono obbligatori");
        }
        Insegnante insegnante = insegnanteRepository.findById(insegnanteId)
            .orElseThrow(() -> new IllegalArgumentException("Insegnante non trovato: " + insegnanteId));
        rimborso.setInsegnante(insegnante);
        rimborso.setNota(rimborso.getNota() != null && !rimborso.getNota().isBlank() ? rimborso.getNota().strip() : null);
        return rimborsoRepository.save(rimborso);
    }

    @Transactional
    public void elimina(Long rimborsoId) {
        rimborsoRepository.deleteById(rimborsoId);
    }
}
