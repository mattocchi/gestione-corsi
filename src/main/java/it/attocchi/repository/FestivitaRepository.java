package it.attocchi.repository;

import it.attocchi.entity.Festivita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FestivitaRepository extends JpaRepository<Festivita, Long> {

    List<Festivita> findAllByOrderByNomeAsc();

    List<Festivita> findByAttivoTrue();
}
