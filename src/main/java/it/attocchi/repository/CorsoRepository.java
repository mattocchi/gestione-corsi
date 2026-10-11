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
    List<Corso> findByInsegnanteId(Long insegnanteId);
    long countByInsegnanteUserUsernameAndAttivoTrue(String username);
    boolean existsByAnnoScolastico(String annoScolastico);

    @Query("SELECT DISTINCT c.annoScolastico FROM Corso c WHERE c.annoScolastico IS NOT NULL")
    List<String> findAnniScolasticiUsati();

    @Query("SELECT c FROM Corso c JOIN c.iscrizioni i WHERE i.allievo.id = :allievoId")
    List<Corso> findByAllievoId(@Param("allievoId") Long allievoId);

    @Query("SELECT DISTINCT c FROM Corso c LEFT JOIN FETCH c.iscrizioni i LEFT JOIN FETCH i.allievo LEFT JOIN FETCH c.insegnante LEFT JOIN FETCH c.giorniSettimana")
    List<Corso> findAllWithAllievi();
}
