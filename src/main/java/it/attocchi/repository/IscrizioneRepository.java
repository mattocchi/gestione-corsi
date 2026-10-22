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

    /** Tutte le iscrizioni con corso, allievo e genitore (e i loro utenti), per la gestione pagamenti. */
    @Query("SELECT i FROM Iscrizione i JOIN FETCH i.corso JOIN FETCH i.allievo a JOIN FETCH a.user "
        + "LEFT JOIN FETCH a.genitore g LEFT JOIN FETCH g.user")
    List<Iscrizione> findAllWithCorsoAllievoGenitore();

    /** Iscrizioni dei figli del genitore, con corso, allievo e genitore. */
    @Query("SELECT i FROM Iscrizione i JOIN FETCH i.corso JOIN FETCH i.allievo a JOIN FETCH a.user "
        + "JOIN FETCH a.genitore g JOIN FETCH g.user u WHERE u.username = :username")
    List<Iscrizione> findByGenitoreUsernameWithCorsoAllievo(@Param("username") String username);

    /** Iscrizioni dei figli del genitore indicato per id, con corso, allievo e genitore. */
    @Query("SELECT i FROM Iscrizione i JOIN FETCH i.corso JOIN FETCH i.allievo a JOIN FETCH a.user "
        + "JOIN FETCH a.genitore g JOIN FETCH g.user WHERE g.id = :genitoreId")
    List<Iscrizione> findByGenitoreIdWithCorsoAllievo(@Param("genitoreId") Long genitoreId);

    @Query("SELECT COUNT(DISTINCT i.corso.id) FROM Iscrizione i WHERE i.allievo.genitore.user.username = :username")
    long countCorsiFigliByGenitoreUsername(@Param("username") String username);

    long countByAllievoUserUsernameAndCorsoAttivoTrue(String username);

    /** Numero di iscritti per corso: righe [id del corso, conteggio]. */
    @Query("SELECT i.corso.id, COUNT(i) FROM Iscrizione i GROUP BY i.corso.id")
    List<Object[]> contaPerCorso();

    /** Motivi delle quote personalizzate con il numero di iscrizioni che li usano. */
    @Query("SELECT i.notaQuota, COUNT(i) FROM Iscrizione i WHERE i.notaQuota IS NOT NULL GROUP BY i.notaQuota")
    List<Object[]> contaMotiviQuota();
}
