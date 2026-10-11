package it.attocchi.repository;

import it.attocchi.entity.Iscrizione;
import it.attocchi.entity.IscrizioneId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IscrizioneRepository extends JpaRepository<Iscrizione, IscrizioneId> {

    @Query("SELECT i FROM Iscrizione i JOIN FETCH i.allievo s JOIN FETCH s.user WHERE i.corso.id = :corsoId")
    List<Iscrizione> findByCorsoIdWithAllievo(@Param("corsoId") Long corsoId);

    @Query("SELECT i FROM Iscrizione i JOIN FETCH i.corso WHERE i.allievo.id = :allievoId")
    List<Iscrizione> findByAllievoIdWithCorso(@Param("allievoId") Long allievoId);

    @Query("SELECT i FROM Iscrizione i JOIN FETCH i.corso c WHERE c.attivo = true")
    List<Iscrizione> findByCorsoAttivo();

    @Query("SELECT COUNT(DISTINCT i.corso.id) FROM Iscrizione i WHERE i.allievo.genitore.user.username = :username")
    long countCorsiFigliByGenitoreUsername(@Param("username") String username);

    long countByAllievoUserUsernameAndCorsoAttivoTrue(String username);
}
