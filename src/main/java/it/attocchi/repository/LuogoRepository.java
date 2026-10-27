package it.attocchi.repository;

import it.attocchi.entity.Luogo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LuogoRepository extends JpaRepository<Luogo, Long> {
    List<Luogo> findAllByOrderByNomeAsc();
    List<Luogo> findByAttivoTrueOrderByNomeAsc();
    boolean existsByNomeIgnoreCase(String nome);
    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);
}
