package it.attocchi.repository;

import it.attocchi.entity.Insegnante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InsegnanteRepository extends JpaRepository<Insegnante, Long> {
    Optional<Insegnante> findByUserId(Long userId);
    Optional<Insegnante> findByUserUsername(String username);
}
