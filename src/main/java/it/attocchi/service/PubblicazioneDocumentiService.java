package it.attocchi.service;

import it.attocchi.entity.AttestazioniPubblicate;
import it.attocchi.entity.PubblicazioneDocumento;
import it.attocchi.entity.TipoDocumento;
import it.attocchi.entity.User;
import it.attocchi.repository.AttestazioniPubblicateRepository;
import it.attocchi.repository.PagamentoRepository;
import it.attocchi.repository.PubblicazioneDocumentoRepository;
import it.attocchi.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Quali documenti vedono le famiglie (genitori e allievi adulti): per ogni tipo un interruttore di admin e segreteria,
 * più la regola del tipo. Conferma e riepilogo annuale: corso pubblicato; ricevuta: corso pubblicato e almeno un
 * pagamento (regole applicate da {@code ElencoDocumentiService} e {@code DocumentiService}); attestazione: solo gli
 * anni pubblicati dallo staff, mai prima del 1° gennaio dell'anno dopo. Admin e segreteria vedono sempre tutto.
 */
@Service
@Transactional(readOnly = true)
public class PubblicazioneDocumentiService {

    /** Tipi di documento delle famiglie, nell'ordine di Impostazioni › Documenti. */
    public static final List<TipoDocumento> PER_LE_FAMIGLIE = List.of(TipoDocumento.CONFERMA_ISCRIZIONE,
        TipoDocumento.RICEVUTA, TipoDocumento.RIEPILOGO, TipoDocumento.ATTESTAZIONE);

    /** Anno solare con pagamenti (o già pubblicato): {@code pubblicabile} = già finito. */
    public record AnnoAttestazioni(int anno, boolean pubblicato, boolean pubblicabile) {
    }

    private final PubblicazioneDocumentoRepository pubblicazioneRepository;
    private final AttestazioniPubblicateRepository attestazioniRepository;
    private final PagamentoRepository pagamentoRepository;
    private final UserRepository userRepository;

    public PubblicazioneDocumentiService(PubblicazioneDocumentoRepository pubblicazioneRepository,
                                         AttestazioniPubblicateRepository attestazioniRepository,
                                         PagamentoRepository pagamentoRepository, UserRepository userRepository) {
        this.pubblicazioneRepository = pubblicazioneRepository;
        this.attestazioniRepository = attestazioniRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.userRepository = userRepository;
    }

    /** Interruttore del tipo: i tipi che non sono delle famiglie non dipendono da questa impostazione. */
    public boolean visibile(TipoDocumento tipo) {
        if (!PER_LE_FAMIGLIE.contains(tipo)) {
            return true;
        }
        return pubblicazioneRepository.findByTipo(tipo).map(PubblicazioneDocumento::isVisibileFamiglie).orElse(false);
    }

    /** Attestazione dell'anno disponibile alle famiglie: tipo visibile, anno finito e pubblicato. */
    public boolean attestazioneDisponibile(int anno, LocalDate oggi) {
        return visibile(TipoDocumento.ATTESTAZIONE) && anno < oggi.getYear() && attestazioniRepository.existsByAnno(anno);
    }

    /** Anni con pagamenti con bonifico (o già pubblicati), dal più recente, con lo stato di pubblicazione. */
    public List<AnnoAttestazioni> anniAttestazioni(LocalDate oggi) {
        Set<Integer> anni = new TreeSet<>(Comparator.reverseOrder());
        pagamentoRepository.totaliPerAllievoEAnno().forEach(riga -> anni.add(((Number) riga[1]).intValue()));
        attestazioniRepository.findAll().forEach(a -> anni.add(a.getAnno()));
        return anni.stream()
            .map(anno -> new AnnoAttestazioni(anno, attestazioniRepository.existsByAnno(anno), anno < oggi.getYear()))
            .toList();
    }

    /** Accende o spegne un tipo per le famiglie (admin e segreteria). */
    @Transactional
    public void imposta(TipoDocumento tipo, boolean visibile, String username) {
        verificaStaff(username);
        if (!PER_LE_FAMIGLIE.contains(tipo)) {
            throw new IllegalArgumentException("Documento non destinato alle famiglie: " + tipo.getTitolo());
        }
        PubblicazioneDocumento pubblicazione = pubblicazioneRepository.findByTipo(tipo)
            .orElseGet(() -> new PubblicazioneDocumento(tipo, visibile));
        pubblicazione.setVisibileFamiglie(visibile);
        pubblicazioneRepository.save(pubblicazione);
    }

    /** Pubblica o ritira le attestazioni di un anno (admin e segreteria); l'anno deve essere finito. */
    @Transactional
    public void pubblicaAttestazioni(int anno, boolean pubblicate, LocalDate oggi, String username) {
        verificaStaff(username);
        if (pubblicate) {
            if (anno >= oggi.getYear()) {
                throw new IllegalArgumentException("Le attestazioni del " + anno + " si possono pubblicare dal 1° gennaio "
                    + (anno + 1));
            }
            if (!attestazioniRepository.existsByAnno(anno)) {
                attestazioniRepository.save(new AttestazioniPubblicate(anno));
            }
        } else {
            attestazioniRepository.findByAnno(anno).ifPresent(attestazioniRepository::delete);
        }
    }

    /**
     * All'avvio: i tipi delle famiglie senza impostazione diventano visibili; la prima volta (nessuna impostazione)
     * pubblica anche le attestazioni degli anni già finiti con pagamenti, così le famiglie vedono quanto vedevano prima.
     */
    @Transactional
    public void inizializza(LocalDate oggi) {
        boolean primaVolta = pubblicazioneRepository.count() == 0;
        for (TipoDocumento tipo : PER_LE_FAMIGLIE) {
            if (pubblicazioneRepository.findByTipo(tipo).isEmpty()) {
                pubblicazioneRepository.save(new PubblicazioneDocumento(tipo, true));
            }
        }
        if (primaVolta) {
            anniAttestazioni(oggi).stream().filter(AnnoAttestazioni::pubblicabile)
                .filter(a -> !a.pubblicato())
                .forEach(a -> attestazioniRepository.save(new AttestazioniPubblicate(a.anno())));
        }
    }

    private void verificaStaff(String username) {
        Set<String> ruoli = userRepository.findByUsername(username).map(User::getRoles).orElse(Set.of());
        if (!ruoli.contains("ADMIN") && !ruoli.contains("SEGRETERIA")) {
            throw new IllegalStateException("Solo admin e segreteria decidono cosa pubblicare alle famiglie");
        }
    }
}
