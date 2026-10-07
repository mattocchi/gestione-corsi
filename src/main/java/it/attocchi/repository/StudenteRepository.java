package it.attocchi.repository;

import it.attocchi.entity.Studente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudenteRepository extends JpaRepository<Studente, Long> {
    Optional<Studente> findByUserId(Long userId);
    Optional<Studente> findByUserUsername(String username);

    @Query("SELECT DISTINCT s FROM Studente s LEFT JOIN FETCH s.corsi WHERE s.genitore.id = :genitoreId")
    List<Studente> findByGenitoreIdWithCorsi(@Param("genitoreId") Long genitoreId);

    @Query("SELECT DISTINCT s FROM Studente s LEFT JOIN FETCH s.corsi LEFT JOIN FETCH s.genitore")
    List<Studente> findAllWithCorsi();
}
