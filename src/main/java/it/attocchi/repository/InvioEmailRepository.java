package it.attocchi.repository;

import it.attocchi.entity.InvioEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvioEmailRepository extends JpaRepository<InvioEmail, Long> {
    List<InvioEmail> findByCorsoIdAndAllievoIdOrderByDataOraDesc(Long corsoId, Long allievoId);
    List<InvioEmail> findAllByOrderByDataOraDesc();
}
