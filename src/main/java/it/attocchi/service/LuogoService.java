package it.attocchi.service;

import it.attocchi.entity.Luogo;
import it.attocchi.repository.LuogoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Elenco dei luoghi di corsi e lezioni (gestito da admin e segreteria; elimina solo l'admin). */
@Service
@Transactional(readOnly = true)
public class LuogoService {

    private final LuogoRepository luogoRepository;

    public LuogoService(LuogoRepository luogoRepository) {
        this.luogoRepository = luogoRepository;
    }

    /** Tutti i luoghi, in ordine di nome. */
    public List<Luogo> elenco() {
        return luogoRepository.findAllByOrderByNomeAsc();
    }

    /** Luoghi che si possono scegliere per corsi e lezioni. */
    public List<Luogo> attivi() {
        return luogoRepository.findByAttivoTrueOrderByNomeAsc();
    }

    /** Id dei luoghi usati da almeno un corso o una lezione (non eliminabili). */
    public Set<Long> usati() {
        return new HashSet<>();
    }

    /** Crea o rinomina un luogo; il nome è obbligatorio e unico (senza distinguere maiuscole). */
    @Transactional
    public Luogo salva(Luogo luogo) {
        String nome = luogo.getNome() == null ? "" : luogo.getNome().strip();
        if (nome.isEmpty()) {
            throw new IllegalArgumentException("Inserisci il nome del luogo");
        }
        if (nome.length() > Luogo.LUNGHEZZA_NOME) {
            throw new IllegalArgumentException("Il nome può avere al massimo " + Luogo.LUNGHEZZA_NOME + " caratteri");
        }
        boolean doppio = luogo.getId() == null ? luogoRepository.existsByNomeIgnoreCase(nome)
            : luogoRepository.existsByNomeIgnoreCaseAndIdNot(nome, luogo.getId());
        if (doppio) {
            throw new IllegalArgumentException("Il luogo " + nome + " è già presente");
        }
        luogo.setNome(nome);
        return luogoRepository.save(luogo);
    }

    /** Un luogo non attivo non si può più scegliere, ma resta su corsi e lezioni che lo hanno già. */
    @Transactional
    public void impostaAttivo(Long id, boolean attivo) {
        luogoRepository.findById(id).orElseThrow().setAttivo(attivo);
    }

    @Transactional
    public void elimina(Long id) {
        Luogo luogo = luogoRepository.findById(id).orElseThrow();
        if (usati().contains(id)) {
            throw new IllegalStateException("Il luogo " + luogo.getNome() + " è usato da corsi o lezioni: disattivalo");
        }
        luogoRepository.delete(luogo);
    }
}
