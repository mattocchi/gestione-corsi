package it.attocchi.repository;

import it.attocchi.entity.GruppoCorso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface GruppoCorsoRepository extends JpaRepository<GruppoCorso, Long> {

    List<GruppoCorso> findByCorsoId(Long corsoId);

    @Query("SELECT g.corso.id, COUNT(g) FROM GruppoCorso g GROUP BY g.corso.id")
    List<Object[]> contaPerCorso();

    @Query("SELECT g FROM GruppoCorso g JOIN FETCH g.corso c WHERE c.attivo = true")
    List<GruppoCorso> findDeiCorsiAttivi();
}
