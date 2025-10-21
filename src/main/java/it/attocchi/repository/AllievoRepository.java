package it.attocchi.repository;

import it.attocchi.entity.Allievo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AllievoRepository extends JpaRepository<Allievo, Long> {
    
    List<Allievo> findByNomeContainingIgnoreCaseOrCognomeContainingIgnoreCase(String nome, String cognome);
    
    List<Allievo> findByOrderByCognomeAscNomeAsc();
}
