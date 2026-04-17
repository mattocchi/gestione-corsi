package it.attocchi.repository;

import it.attocchi.entity.Genitore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GenitoreRepository extends JpaRepository<Genitore, Long> {
    Optional<Genitore> findByUserId(Long userId);
}
