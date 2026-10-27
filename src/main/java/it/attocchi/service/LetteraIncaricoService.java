package it.attocchi.service;

import it.attocchi.entity.Corso;
import it.attocchi.entity.Insegnante;
import it.attocchi.entity.LetteraIncarico;
import it.attocchi.entity.User;
import it.attocchi.repository.CorsoRepository;
import it.attocchi.repository.InsegnanteRepository;
import it.attocchi.repository.LetteraIncaricoRepository;
import it.attocchi.repository.UserRepository;
import it.attocchi.views.Formati;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Lettere di incarico degli insegnanti: le emettono e modificano admin e segreteria (elimina solo l'admin); ogni
 * insegnante vede le proprie, anche degli anni passati. PDF e invio in {@link DocumentiService} ed {@link EmailService}.
 */
@Service
@Transactional(readOnly = true)
public class LetteraIncaricoService {

    private final LetteraIncaricoRepository letteraIncaricoRepository;
    private final InsegnanteRepository insegnanteRepository;
    private final CorsoRepository corsoRepository;
    private final UserRepository userRepository;

    public LetteraIncaricoService(LetteraIncaricoRepository letteraIncaricoRepository,
                                  InsegnanteRepository insegnanteRepository, CorsoRepository corsoRepository,
                                  UserRepository userRepository) {
        this.letteraIncaricoRepository = letteraIncaricoRepository;
        this.insegnanteRepository = insegnanteRepository;
        this.corsoRepository = corsoRepository;
        this.userRepository = userRepository;
    }

    /** Tutte le lettere, dall'anno più recente. Solo admin e segreteria. */
    public List<LetteraIncarico> tutte(String username) {
        verificaStaff(username);
        return letteraIncaricoRepository.findAllInOrdine();
    }

    /** Le lettere dell'insegnante, anche degli anni passati, dalla più recente. */
    public List<LetteraIncarico> dellInsegnante(String username) {
        return letteraIncaricoRepository.findDellInsegnante(username);
    }

    /** La lettera, se l'utente può vederla (staff, o l'insegnante incaricato); altrimenti {@link IllegalArgumentException}. */
    public LetteraIncarico lettera(Long id, String username) {
        LetteraIncarico lettera = letteraIncaricoRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Lettera non trovata"));
        User user = userRepository.findByUsername(username).orElse(null);
        Set<String> ruoli = user != null ? user.getRoles() : Set.of();
        boolean propria = lettera.getInsegnante().getUser() != null
            && username.equals(lettera.getInsegnante().getUser().getUsername());
        if (!propria && !ruoli.contains("ADMIN") && !ruoli.contains("SEGRETERIA")) {
            throw new IllegalArgumentException("Lettera non trovata");
        }
        return lettera;
    }

    /**
     * Nuova lettera proposta dai dati attuali: anno del corso, tariffa oraria e rimborso forfettario annuo
     * dell'insegnante (come una tantum), giorni e orari del corso; data di emissione oggi.
     */
    public LetteraIncarico proposta(Long insegnanteId, Long corsoId) {
        Insegnante insegnante = insegnanteRepository.findById(insegnanteId)
            .orElseThrow(() -> new IllegalArgumentException("Insegnante non trovato: " + insegnanteId));
        Corso corso = corsoRepository.findById(corsoId)
            .orElseThrow(() -> new IllegalArgumentException("Corso non trovato: " + corsoId));
        LetteraIncarico lettera = new LetteraIncarico();
        lettera.setInsegnante(insegnante);
        lettera.setCorso(corso);
        lettera.setAnnoScolastico(corso.getAnnoScolastico());
        lettera.setDataEmissione(LocalDate.now());
        lettera.setTariffaOraria(positivo(insegnante.getTariffaOraria()));
        lettera.setImportoUnaTantum(positivo(insegnante.getRimborsoForfettario()));
        lettera.setGiorniEOrari(giorniEOrari(corso));
        return lettera;
    }

    /** "martedì e sabato alle 18:00, ogni 2 settimane, lezioni da 40 minuti" dai dati del corso. */
    public static String giorniEOrari(Corso corso) {
        StringBuilder testo = new StringBuilder(Formati.giorniEstesi(corso.getGiorniSettimana()));
        if (corso.getOrarioInizio() != null) {
            testo.append(testo.isEmpty() ? "" : " ").append("alle ").append(Formati.ora(corso.getOrarioInizio()));
        }
        if (corso.getCadenzaSettimane() == 2) {
            testo.append(", ogni 2 settimane");
        }
        if (corso.getDurataLezioneMinuti() != null) {
            testo.append(testo.isEmpty() ? "" : ", ").append(corso.isServizio() ? "turni" : "lezioni")
                .append(" da ").append(corso.getDurataLezioneMinuti()).append(" minuti");
        }
        return testo.toString();
    }

    /**
     * Emette o modifica una lettera: insegnante del corso (titolare o secondo), almeno un compenso (tariffa oraria o
     * una tantum, maggiori di zero), data di emissione. Solo admin e segreteria.
     */
    @Transactional
    public LetteraIncarico salva(LetteraIncarico lettera, String username) {
        verificaStaff(username);
        if (lettera.getInsegnante() == null || lettera.getCorso() == null || lettera.getDataEmissione() == null) {
            throw new IllegalArgumentException("Indica insegnante, corso e data di emissione");
        }
        Corso corso = corsoRepository.findById(lettera.getCorso().getId())
            .orElseThrow(() -> new IllegalArgumentException("Corso non trovato"));
        if (!corso.getInsegnanti().contains(lettera.getInsegnante())) {
            throw new IllegalArgumentException(lettera.getInsegnante().getNomeCompleto() + " non è "
                + corso.getTipo().getChiLoSvolge().toLowerCase() + " di " + corso.getNome());
        }
        if (negativo(lettera.getTariffaOraria()) || negativo(lettera.getImportoUnaTantum())) {
            throw new IllegalArgumentException("Gli importi non possono essere negativi");
        }
        if (positivo(lettera.getTariffaOraria()) == null && positivo(lettera.getImportoUnaTantum()) == null) {
            throw new IllegalArgumentException("Indica la tariffa oraria o l'importo una tantum");
        }
        if (lettera.getNota() != null && lettera.getNota().length() > LetteraIncarico.LUNGHEZZA_NOTA) {
            throw new IllegalArgumentException("La nota può avere al massimo " + LetteraIncarico.LUNGHEZZA_NOTA + " caratteri");
        }
        if (lettera.getAnnoScolastico() == null) {
            lettera.setAnnoScolastico(corso.getAnnoScolastico());
        }
        lettera.setTariffaOraria(positivo(lettera.getTariffaOraria()));
        lettera.setImportoUnaTantum(positivo(lettera.getImportoUnaTantum()));
        return letteraIncaricoRepository.save(lettera);
    }

    /** Solo l'admin. */
    @Transactional
    public void elimina(Long id, String username) {
        Set<String> ruoli = userRepository.findByUsername(username).map(User::getRoles).orElse(Set.of());
        if (!ruoli.contains("ADMIN")) {
            throw new IllegalStateException("Solo l'admin può eliminare le lettere di incarico");
        }
        letteraIncaricoRepository.deleteById(id);
    }

    private void verificaStaff(String username) {
        Set<String> ruoli = userRepository.findByUsername(username).map(User::getRoles).orElse(Set.of());
        if (!ruoli.contains("ADMIN") && !ruoli.contains("SEGRETERIA")) {
            throw new IllegalStateException("Le lettere di incarico le gestiscono admin e segreteria");
        }
    }

    private static BigDecimal positivo(BigDecimal importo) {
        return importo != null && importo.signum() > 0 ? importo : null;
    }

    private static boolean negativo(BigDecimal importo) {
        return importo != null && importo.signum() < 0;
    }
}
