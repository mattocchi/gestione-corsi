package it.attocchi.repository;

import it.attocchi.entity.Lezione;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LezioneRepository extends JpaRepository<Lezione, Long> {

    @Query("SELECT DISTINCT l.luogo.id FROM Lezione l WHERE l.luogo IS NOT NULL")
    List<Long> findLuoghiUsati();

    List<Lezione> findByCorsoIdOrderByDataAsc(Long corsoId);

    /** Lezioni senza insegnanti presenti registrati (create prima dei due insegnanti per corso). */
    @Query("SELECT l FROM Lezione l WHERE l.insegnantiPresenti IS EMPTY AND l.corso.insegnante IS NOT NULL")
    List<Lezione> findSenzaInsegnantiPresenti();
    /** Lezioni dei corsi attivi in cui l'insegnante è titolare o secondo insegnante. */
    @Query("SELECT l FROM Lezione l JOIN l.corso c LEFT JOIN c.insegnante i1 LEFT JOIN c.secondoInsegnante i2"
        + " WHERE c.attivo = true AND (i1.id = :insegnanteId OR i2.id = :insegnanteId) ORDER BY l.data")
    List<Lezione> findDeiCorsiAttiviDellInsegnante(@Param("insegnanteId") Long insegnanteId);
    List<Lezione> findByCorsoAttivoTrueOrderByDataAsc();
    /** Lezioni a cui l'insegnante era (o sarà) presente: base dei suoi costi. */
    @Query("SELECT l FROM Lezione l JOIN l.insegnantiPresenti i WHERE i.id = :insegnanteId ORDER BY l.data")
    List<Lezione> findConInsegnantePresente(@Param("insegnanteId") Long insegnanteId);
    /** Lezioni dei corsi attivi dell'insegnante (titolare o secondo) con lo stato indicato. */
    @Query("SELECT COUNT(l) FROM Lezione l JOIN l.corso c LEFT JOIN c.insegnante i1 LEFT JOIN i1.user u1"
        + " LEFT JOIN c.secondoInsegnante i2 LEFT JOIN i2.user u2 WHERE c.attivo = true"
        + " AND (u1.username = :username OR u2.username = :username)"
        + " AND l.completata = :completata AND l.nonEffettuata = :nonEffettuata")
    long contaDellInsegnante(@Param("username") String username, @Param("completata") boolean completata,
                             @Param("nonEffettuata") boolean nonEffettuata);
    boolean existsByRecuperoDiId(Long lezioneId);

    @Query("SELECT MAX(l.numeroLezione) FROM Lezione l WHERE l.corso.id = :corsoId")
    Integer findMaxNumeroLezione(@Param("corsoId") Long corsoId);
    List<Lezione> findByCorsoIdAndDataBetweenOrderByDataAsc(Long corsoId, LocalDate dataInizio, LocalDate dataFine);
    List<Lezione> findByDataBetweenOrderByDataAsc(LocalDate dataInizio, LocalDate dataFine);
}
