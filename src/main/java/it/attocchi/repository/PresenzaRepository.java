package it.attocchi.repository;

import it.attocchi.entity.Presenza;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PresenzaRepository extends JpaRepository<Presenza, Long> {
    List<Presenza> findByLezioneId(Long lezioneId);
    List<Presenza> findByStudenteId(Long studenteId);
    Optional<Presenza> findByLezioneIdAndStudenteId(Long lezioneId, Long studenteId);

    @Query("SELECT p FROM Presenza p WHERE p.lezione.corso.id = :corsoId AND p.studente.id = :studenteId ORDER BY p.lezione.data")
    List<Presenza> findByCorsoIdAndStudenteId(@Param("corsoId") Long corsoId, @Param("studenteId") Long studenteId);
}
