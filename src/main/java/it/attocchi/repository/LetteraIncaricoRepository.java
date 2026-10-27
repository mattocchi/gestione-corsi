package it.attocchi.repository;

import it.attocchi.entity.LetteraIncarico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LetteraIncaricoRepository extends JpaRepository<LetteraIncarico, Long> {

    @Query("SELECT l FROM LetteraIncarico l ORDER BY l.annoScolastico DESC, l.dataEmissione DESC, l.id DESC")
    List<LetteraIncarico> findAllInOrdine();

    @Query("SELECT l FROM LetteraIncarico l WHERE l.insegnante.user.username = :username "
        + "ORDER BY l.annoScolastico DESC, l.dataEmissione DESC, l.id DESC")
    List<LetteraIncarico> findDellInsegnante(@Param("username") String username);

    boolean existsByInsegnanteId(Long insegnanteId);

    boolean existsByCorsoId(Long corsoId);
}
