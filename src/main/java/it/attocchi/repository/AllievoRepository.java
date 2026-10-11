package it.attocchi.repository;

import it.attocchi.entity.Allievo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AllievoRepository extends JpaRepository<Allievo, Long> {
    Optional<Allievo> findByUserId(Long userId);
    Optional<Allievo> findByUserUsername(String username);

    @Query("SELECT DISTINCT s FROM Allievo s LEFT JOIN FETCH s.iscrizioni i LEFT JOIN FETCH i.corso WHERE s.genitore.id = :genitoreId")
    List<Allievo> findByGenitoreIdWithCorsi(@Param("genitoreId") Long genitoreId);

    @Query("SELECT DISTINCT s FROM Allievo s LEFT JOIN FETCH s.iscrizioni i LEFT JOIN FETCH i.corso LEFT JOIN FETCH s.genitore")
    List<Allievo> findAllWithCorsi();
}
