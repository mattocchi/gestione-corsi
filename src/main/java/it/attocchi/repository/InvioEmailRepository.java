package it.attocchi.repository;

import it.attocchi.entity.InvioEmail;
import it.attocchi.entity.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvioEmailRepository extends JpaRepository<InvioEmail, Long> {
    List<InvioEmail> findByCorsoIdAndAllievoIdOrderByDataOraDesc(Long corsoId, Long allievoId);
    List<InvioEmail> findAllByOrderByDataOraDesc();
    /** Invii del tipo indicato con l'esito diverso da quello indicato (es. non in errore), dal più recente. */
    List<InvioEmail> findByTipoAndEsitoNotOrderByDataOraDesc(TipoDocumento tipo, InvioEmail.Esito esito);
}
