package it.attocchi.repository;

import it.attocchi.entity.GruppoCorso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GruppoCorsoRepository extends JpaRepository<GruppoCorso, Long> {

    List<GruppoCorso> findByCorsoId(Long corsoId);
}
