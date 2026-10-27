package it.attocchi.service;

import it.attocchi.entity.AperturaDocumento;
import it.attocchi.entity.AperturaDocumento.Modalita;
import it.attocchi.entity.TipoDocumento;
import it.attocchi.entity.User;
import it.attocchi.repository.AperturaDocumentoRepository;
import it.attocchi.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Cronologia dei PDF aperti da genitori e allievi adulti in Documenti (anteprima o scarico), per admin e segreteria;
 * eliminare una registrazione è solo dell'admin.
 */
@Service
@Transactional(readOnly = true)
public class CronologiaDocumentiService {

    private final AperturaDocumentoRepository aperturaDocumentoRepository;
    private final UserRepository userRepository;

    public CronologiaDocumentiService(AperturaDocumentoRepository aperturaDocumentoRepository, UserRepository userRepository) {
        this.aperturaDocumentoRepository = aperturaDocumentoRepository;
        this.userRepository = userRepository;
    }

    /** Il documento indicato è stato aperto ora dall'utente. */
    @Transactional
    public AperturaDocumento registra(String username, TipoDocumento tipo, Modalita modalita, String allievoNome,
                                      String corsoNome, String anno, String nomeFile) {
        AperturaDocumento apertura = new AperturaDocumento();
        apertura.setDataOra(LocalDateTime.now());
        apertura.setUsername(username);
        apertura.setUtenteNome(userRepository.findByUsername(username).map(User::getNomeCompleto).orElse(null));
        apertura.setTipo(tipo);
        apertura.setModalita(modalita);
        apertura.setAllievoNome(allievoNome);
        apertura.setCorsoNome(corsoNome);
        apertura.setAnno(anno);
        apertura.setNomeFile(nomeFile);
        return aperturaDocumentoRepository.save(apertura);
    }

    /** Tutte le aperture, dalla più recente. */
    public List<AperturaDocumento> tutte() {
        return aperturaDocumentoRepository.findAllByOrderByDataOraDesc();
    }

    /** Solo l'admin. */
    @Transactional
    public void elimina(Long id, String username) {
        boolean admin = userRepository.findByUsername(username).map(u -> u.getRoles().contains("ADMIN")).orElse(false);
        if (!admin) {
            throw new IllegalStateException("Solo l'admin può eliminare la cronologia dei documenti");
        }
        aperturaDocumentoRepository.deleteById(id);
    }
}
