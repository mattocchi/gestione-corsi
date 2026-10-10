package it.attocchi.repository;

import it.attocchi.entity.Genitore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GenitoreRepository extends JpaRepository<Genitore, Long> {
    Optional<Genitore> findByUserId(Long userId);
    Optional<Genitore> findByUserUsername(String username);

    @Query("SELECT DISTINCT g FROM Genitore g LEFT JOIN FETCH g.figli")
    List<Genitore> findAllWithFigli();
}
