package it.attocchi.repository;

import it.attocchi.entity.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {

    @Query("SELECT p FROM Pagamento p WHERE p.iscrizione.id.corsoId = :corsoId AND p.iscrizione.id.studenteId = :studenteId ORDER BY p.data, p.id")
    List<Pagamento> findByIscrizione(@Param("corsoId") Long corsoId, @Param("studenteId") Long studenteId);

    @Query("SELECT COUNT(p) > 0 FROM Pagamento p WHERE p.iscrizione.id.corsoId = :corsoId AND p.iscrizione.id.studenteId = :studenteId")
    boolean existsByIscrizione(@Param("corsoId") Long corsoId, @Param("studenteId") Long studenteId);

    @Query("SELECT COUNT(p) > 0 FROM Pagamento p WHERE p.iscrizione.id.corsoId = :corsoId")
    boolean existsByCorso(@Param("corsoId") Long corsoId);

    @Query("SELECT COUNT(p) > 0 FROM Pagamento p WHERE p.iscrizione.id.studenteId = :studenteId")
    boolean existsByStudente(@Param("studenteId") Long studenteId);

    /** Totale pagato per ogni partecipante del corso: righe [studenteId, somma]. */
    @Query("SELECT p.iscrizione.id.studenteId, SUM(p.importo) FROM Pagamento p WHERE p.iscrizione.id.corsoId = :corsoId GROUP BY p.iscrizione.id.studenteId")
    List<Object[]> sommaPerStudente(@Param("corsoId") Long corsoId);

    /** Totale pagato dallo studente per ogni corso: righe [corsoId, somma]. */
    @Query("SELECT p.iscrizione.id.corsoId, SUM(p.importo) FROM Pagamento p WHERE p.iscrizione.id.studenteId = :studenteId GROUP BY p.iscrizione.id.corsoId")
    List<Object[]> sommaPerCorso(@Param("studenteId") Long studenteId);
}
