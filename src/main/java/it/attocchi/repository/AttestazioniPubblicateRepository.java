package it.attocchi.repository;

import it.attocchi.entity.AttestazioniPubblicate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AttestazioniPubblicateRepository extends JpaRepository<AttestazioniPubblicate, Long> {
    boolean existsByAnno(int anno);
    Optional<AttestazioniPubblicate> findByAnno(int anno);
}
