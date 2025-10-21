package it.attocchi.repository;

import it.attocchi.entity.Corso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CorsoRepository extends JpaRepository<Corso, Long> {
    
    List<Corso> findByNomeContainingIgnoreCase(String nome);
    
    List<Corso> findByDocenteId(Long docenteId);
    
    @Query("SELECT c FROM Corso c WHERE c.dataInizio <= :data AND c.dataFine >= :data")
    List<Corso> findCorsiAttiviAllaData(LocalDate data);
    
    List<Corso> findByOrderByNomeAsc();
}
