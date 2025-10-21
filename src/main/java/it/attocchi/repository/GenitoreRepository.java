package it.attocchi.repository;

import it.attocchi.entity.Genitore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GenitoreRepository extends JpaRepository<Genitore, Long> {
    
    List<Genitore> findByNomeContainingIgnoreCaseOrCognomeContainingIgnoreCase(String nome, String cognome);
    
    List<Genitore> findByOrderByCognomeAscNomeAsc();
}
