package it.attocchi.repository;

import it.attocchi.entity.Rimborso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RimborsoRepository extends JpaRepository<Rimborso, Long> {

    @Query("SELECT r FROM Rimborso r JOIN FETCH r.insegnante i JOIN FETCH i.user WHERE i.id = :insegnanteId "
        + "AND r.meseRiferimento BETWEEN :dal AND :al ORDER BY r.meseRiferimento DESC, r.data DESC")
    List<Rimborso> findByInsegnanteAndPeriodo(@Param("insegnanteId") Long insegnanteId,
                                              @Param("dal") LocalDate dal, @Param("al") LocalDate al);

    @Query("SELECT r FROM Rimborso r JOIN FETCH r.insegnante WHERE r.meseRiferimento BETWEEN :dal AND :al")
    List<Rimborso> findByPeriodo(@Param("dal") LocalDate dal, @Param("al") LocalDate al);
}
