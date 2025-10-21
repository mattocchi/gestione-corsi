package it.attocchi.repository;

import it.attocchi.entity.Presenza;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PresenzaRepository extends JpaRepository<Presenza, Long> {
    
    List<Presenza> findByCorsoIdAndDataLezione(Long corsoId, LocalDate dataLezione);
    
    List<Presenza> findByAllievoIdOrderByDataLezioneDesc(Long allievoId);
    
    List<Presenza> findByCorsoIdOrderByDataLezioneDesc(Long corsoId);
    
    Optional<Presenza> findByCorsoIdAndAllievoIdAndDataLezione(Long corsoId, Long allievoId, LocalDate dataLezione);
    
    @Query("SELECT p FROM Presenza p WHERE p.corso.id = :corsoId AND p.dataLezione BETWEEN :dataInizio AND :dataFine ORDER BY p.dataLezione, p.allievo.cognome, p.allievo.nome")
    List<Presenza> findPresenzeByCorsoAndPeriodo(Long corsoId, LocalDate dataInizio, LocalDate dataFine);
}
