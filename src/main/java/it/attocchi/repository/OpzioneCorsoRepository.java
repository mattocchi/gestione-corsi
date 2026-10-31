package it.attocchi.repository;

import it.attocchi.entity.OpzioneCorso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OpzioneCorsoRepository extends JpaRepository<OpzioneCorso, Long> {

    List<OpzioneCorso> findByCorsoIdOrderById(Long corsoId);

    /** Iscritti che hanno attiva l'opzione. */
    @Query("SELECT COUNT(i) FROM Iscrizione i JOIN i.opzioni o WHERE o.id = :opzioneId")
    long contaIscritti(@Param("opzioneId") Long opzioneId);
}
