package it.attocchi.repository;

import it.attocchi.entity.AnnoScolastico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnnoScolasticoRepository extends JpaRepository<AnnoScolastico, Long> {
    List<AnnoScolastico> findAllByOrderByNomeDesc();
    List<AnnoScolastico> findByAttivoTrue();
    boolean existsByNome(String nome);
}
