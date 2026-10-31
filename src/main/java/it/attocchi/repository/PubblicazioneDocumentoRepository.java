package it.attocchi.repository;

import it.attocchi.entity.PubblicazioneDocumento;
import it.attocchi.entity.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PubblicazioneDocumentoRepository extends JpaRepository<PubblicazioneDocumento, Long> {
    Optional<PubblicazioneDocumento> findByTipo(TipoDocumento tipo);
}
