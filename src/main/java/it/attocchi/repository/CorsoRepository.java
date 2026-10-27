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
    /** Corsi in cui l'insegnante è titolare o facoltativo. */
    @Query("SELECT c FROM Corso c LEFT JOIN c.insegnante i1"
        + " WHERE i1.id = :insegnanteId OR EXISTS (SELECT f.id FROM Corso cf JOIN cf.insegnantiFacoltativi f WHERE cf = c AND f.id = :insegnanteId)")
    List<Corso> findDellInsegnante(@Param("insegnanteId") Long insegnanteId);

    /** Corsi attivi in cui l'utente è titolare o insegnante facoltativo. */
    @Query("SELECT COUNT(c) FROM Corso c LEFT JOIN c.insegnante i1 LEFT JOIN i1.user u1"
        + " WHERE c.attivo = true AND (u1.username = :username OR EXISTS (SELECT f.id FROM Corso cf JOIN cf.insegnantiFacoltativi f JOIN f.user uf WHERE cf = c AND uf.username = :username))")
    long contaAttiviDellInsegnante(@Param("username") String username);
    boolean existsByAnnoScolastico(String annoScolastico);

    @Query("SELECT COUNT(c) > 0 FROM Corso c WHERE c.insegnante.id = :insegnanteId OR EXISTS (SELECT f.id FROM Corso cf JOIN cf.insegnantiFacoltativi f WHERE cf = c AND f.id = :insegnanteId)")
    boolean esisteConInsegnante(@Param("insegnanteId") Long insegnanteId);

    @Query("SELECT DISTINCT c.annoScolastico FROM Corso c WHERE c.annoScolastico IS NOT NULL")
    List<String> findAnniScolasticiUsati();

    @Query("SELECT DISTINCT c.luogo.id FROM Corso c WHERE c.luogo IS NOT NULL")
    List<Long> findLuoghiPredefinitiUsati();

    @Query("SELECT DISTINCT l.id FROM Corso c JOIN c.luoghiAbilitati l")
    List<Long> findLuoghiAbilitatiUsati();

    @Query("SELECT c FROM Corso c JOIN c.iscrizioni i WHERE i.allievo.id = :allievoId")
    List<Corso> findByAllievoId(@Param("allievoId") Long allievoId);

    @Query("SELECT DISTINCT c FROM Corso c LEFT JOIN FETCH c.iscrizioni i LEFT JOIN FETCH i.allievo LEFT JOIN FETCH c.insegnante LEFT JOIN FETCH c.giorniSettimana")
    List<Corso> findAllWithAllievi();
}
