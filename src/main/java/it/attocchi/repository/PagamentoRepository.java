package it.attocchi.repository;

import it.attocchi.entity.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {

    @Query("SELECT p FROM Pagamento p WHERE p.iscrizione.id.corsoId = :corsoId AND p.iscrizione.id.allievoId = :allievoId ORDER BY p.data, p.id")
    List<Pagamento> findByIscrizione(@Param("corsoId") Long corsoId, @Param("allievoId") Long allievoId);

    @Query("SELECT COUNT(p) > 0 FROM Pagamento p WHERE p.iscrizione.id.corsoId = :corsoId AND p.iscrizione.id.allievoId = :allievoId")
    boolean existsByIscrizione(@Param("corsoId") Long corsoId, @Param("allievoId") Long allievoId);

    @Query("SELECT COUNT(p) > 0 FROM Pagamento p WHERE p.iscrizione.id.corsoId = :corsoId")
    boolean existsByCorso(@Param("corsoId") Long corsoId);

    @Query("SELECT COUNT(p) > 0 FROM Pagamento p WHERE p.iscrizione.id.allievoId = :allievoId")
    boolean existsByAllievo(@Param("allievoId") Long allievoId);

    /** Rate pagate dall'allievo nel periodo (tutti i corsi), con iscrizione e corso, in ordine di data. */
    @Query("SELECT p FROM Pagamento p JOIN FETCH p.iscrizione i JOIN FETCH i.corso "
        + "WHERE i.id.allievoId = :allievoId AND p.data BETWEEN :dal AND :al ORDER BY p.data, p.id")
    List<Pagamento> findDellAllievoNelPeriodo(@Param("allievoId") Long allievoId, @Param("dal") LocalDate dal,
                                              @Param("al") LocalDate al);

    /** Totale pagato da ogni allievo in ogni anno solare (corsi pubblicati): righe [allievoId, anno, somma]. */
    @Query("SELECT p.iscrizione.id.allievoId, YEAR(p.data), SUM(p.importo) FROM Pagamento p "
        + "WHERE p.iscrizione.corso.pubblicato IS NULL OR p.iscrizione.corso.pubblicato = true "
        + "GROUP BY p.iscrizione.id.allievoId, YEAR(p.data)")
    List<Object[]> totaliPerAllievoEAnno();

    /** Totale pagato per ogni partecipante del corso: righe [allievoId, somma]. */
    @Query("SELECT p.iscrizione.id.allievoId, SUM(p.importo) FROM Pagamento p WHERE p.iscrizione.id.corsoId = :corsoId GROUP BY p.iscrizione.id.allievoId")
    List<Object[]> sommaPerAllievo(@Param("corsoId") Long corsoId);

    /** Totale pagato dall'allievo per ogni corso: righe [corsoId, somma]. */
    @Query("SELECT p.iscrizione.id.corsoId, SUM(p.importo) FROM Pagamento p WHERE p.iscrizione.id.allievoId = :allievoId GROUP BY p.iscrizione.id.corsoId")
    List<Object[]> sommaPerCorso(@Param("allievoId") Long allievoId);

    /** Totale pagato da ogni partecipante di ogni corso: righe [corsoId, allievoId, somma]. */
    @Query("SELECT p.iscrizione.id.corsoId, p.iscrizione.id.allievoId, SUM(p.importo) FROM Pagamento p "
        + "GROUP BY p.iscrizione.id.corsoId, p.iscrizione.id.allievoId")
    List<Object[]> sommaPerIscrizione();

    /** Totale pagato da ogni partecipante dei corsi attivi: righe [corsoId, allievoId, somma]. */
    @Query("SELECT p.iscrizione.id.corsoId, p.iscrizione.id.allievoId, SUM(p.importo) FROM Pagamento p "
        + "WHERE p.iscrizione.corso.attivo = true GROUP BY p.iscrizione.id.corsoId, p.iscrizione.id.allievoId")
    List<Object[]> sommaPerIscrizioneCorsiAttivi();
}
