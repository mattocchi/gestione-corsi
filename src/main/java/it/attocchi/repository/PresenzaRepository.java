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

    /** Numero di presenze (allievi iscritti) per lezione, per le lezioni a cui l'insegnante era presente. */
    @Query("SELECT p.lezione.id, COUNT(p) FROM Presenza p JOIN p.lezione.insegnantiPresenti i"
        + " WHERE i.id = :insegnanteId GROUP BY p.lezione.id")
    List<Object[]> contaPerLezione(@Param("insegnanteId") Long insegnanteId);
    List<Presenza> findByAllievoId(Long allievoId);
    Optional<Presenza> findByLezioneIdAndAllievoId(Long lezioneId, Long allievoId);

    @Query("SELECT p FROM Presenza p WHERE p.lezione.corso.id = :corsoId AND p.allievo.id = :allievoId ORDER BY p.lezione.data")
    List<Presenza> findByCorsoIdAndAllievoId(@Param("corsoId") Long corsoId, @Param("allievoId") Long allievoId);

    /** Presenze dei corsi attivi in cui l'utente è titolare o secondo insegnante, con la lezione. */
    @Query("SELECT p FROM Presenza p JOIN FETCH p.lezione l JOIN l.corso c LEFT JOIN c.insegnante i1 LEFT JOIN i1.user u1"
        + " LEFT JOIN c.secondoInsegnante i2 LEFT JOIN i2.user u2"
        + " WHERE c.attivo = true AND (u1.username = :username OR u2.username = :username)")
    List<Presenza> findDeiCorsiAttiviDellInsegnante(@Param("username") String username);
}
