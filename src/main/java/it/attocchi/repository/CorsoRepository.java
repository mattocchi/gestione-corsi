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
    /** Corsi in cui l'insegnante è titolare o secondo insegnante. */
    @Query("SELECT c FROM Corso c LEFT JOIN c.insegnante i1 LEFT JOIN c.secondoInsegnante i2"
        + " WHERE i1.id = :insegnanteId OR i2.id = :insegnanteId")
    List<Corso> findDellInsegnante(@Param("insegnanteId") Long insegnanteId);

    /** Corsi attivi in cui l'utente è titolare o secondo insegnante. */
    @Query("SELECT COUNT(c) FROM Corso c LEFT JOIN c.insegnante i1 LEFT JOIN i1.user u1"
        + " LEFT JOIN c.secondoInsegnante i2 LEFT JOIN i2.user u2"
        + " WHERE c.attivo = true AND (u1.username = :username OR u2.username = :username)")
    long contaAttiviDellInsegnante(@Param("username") String username);
    boolean existsByAnnoScolastico(String annoScolastico);

    @Query("SELECT DISTINCT c.annoScolastico FROM Corso c WHERE c.annoScolastico IS NOT NULL")
    List<String> findAnniScolasticiUsati();

    @Query("SELECT c FROM Corso c JOIN c.iscrizioni i WHERE i.allievo.id = :allievoId")
    List<Corso> findByAllievoId(@Param("allievoId") Long allievoId);

    @Query("SELECT DISTINCT c FROM Corso c LEFT JOIN FETCH c.iscrizioni i LEFT JOIN FETCH i.allievo LEFT JOIN FETCH c.insegnante LEFT JOIN FETCH c.giorniSettimana")
    List<Corso> findAllWithAllievi();
}
