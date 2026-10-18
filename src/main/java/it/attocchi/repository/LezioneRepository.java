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
    List<Lezione> findByCorsoIdOrderByDataAsc(Long corsoId);

    /** Lezioni senza insegnanti presenti registrati (create prima dei due insegnanti per corso). */
    @Query("SELECT l FROM Lezione l WHERE l.insegnantiPresenti IS EMPTY AND l.corso.insegnante IS NOT NULL")
    List<Lezione> findSenzaInsegnantiPresenti();
    List<Lezione> findByCorsoInsegnanteIdAndCorsoAttivoTrueOrderByDataAsc(Long insegnanteId);
    List<Lezione> findByCorsoAttivoTrueOrderByDataAsc();
    List<Lezione> findByCorsoInsegnanteIdOrderByDataAsc(Long insegnanteId);
    long countByCorsoInsegnanteUserUsernameAndCorsoAttivoTrueAndCompletataAndNonEffettuata(String username,
                                                                                         boolean completata, boolean nonEffettuata);
    boolean existsByRecuperoDiId(Long lezioneId);

    @Query("SELECT MAX(l.numeroLezione) FROM Lezione l WHERE l.corso.id = :corsoId")
    Integer findMaxNumeroLezione(@Param("corsoId") Long corsoId);
    List<Lezione> findByCorsoIdAndDataBetweenOrderByDataAsc(Long corsoId, LocalDate dataInizio, LocalDate dataFine);
    List<Lezione> findByDataBetweenOrderByDataAsc(LocalDate dataInizio, LocalDate dataFine);
}
