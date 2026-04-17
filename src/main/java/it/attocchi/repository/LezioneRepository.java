package it.attocchi.repository;

import it.attocchi.entity.Lezione;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LezioneRepository extends JpaRepository<Lezione, Long> {
    List<Lezione> findByCorsoIdOrderByDataAsc(Long corsoId);
    List<Lezione> findByCorsoIdAndDataBetweenOrderByDataAsc(Long corsoId, LocalDate dataInizio, LocalDate dataFine);
    List<Lezione> findByDataBetweenOrderByDataAsc(LocalDate dataInizio, LocalDate dataFine);
}
