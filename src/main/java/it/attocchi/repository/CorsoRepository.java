package it.attocchi.repository;

import it.attocchi.entity.Corso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CorsoRepository extends JpaRepository<Corso, Long> {
    List<Corso> findByAttivoTrue();
    List<Corso> findByDocenteId(Long docenteId);

    @Query("SELECT c FROM Corso c JOIN c.studenti s WHERE s.id = :studenteId")
    List<Corso> findByStudenteId(@Param("studenteId") Long studenteId);

    @Query("SELECT DISTINCT c FROM Corso c LEFT JOIN FETCH c.studenti LEFT JOIN FETCH c.docente")
    List<Corso> findAllWithStudenti();
}
