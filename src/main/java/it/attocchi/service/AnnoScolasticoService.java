package it.attocchi.service;

import it.attocchi.entity.AnnoScolastico;
import it.attocchi.repository.AnnoScolasticoRepository;
import it.attocchi.repository.CorsoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Comparator;
import java.util.Collection;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Elenco degli anni scolastici che si possono assegnare ai corsi (gestito da admin e segreteria). */
@Service
@Transactional(readOnly = true)
public class AnnoScolasticoService {

    private final AnnoScolasticoRepository annoScolasticoRepository;
    private final CorsoRepository corsoRepository;

    public AnnoScolasticoService(AnnoScolasticoRepository annoScolasticoRepository, CorsoRepository corsoRepository) {
        this.annoScolasticoRepository = annoScolasticoRepository;
        this.corsoRepository = corsoRepository;
    }

    /** Tutti gli anni, dal più recente. */
    public List<AnnoScolastico> elenco() {
        return annoScolasticoRepository.findAllByOrderByNomeDesc();
    }

    /** Anni assegnati ad almeno un corso (non eliminabili). */
    public Set<String> usati() {
        return new HashSet<>(corsoRepository.findAnniScolasticiUsati());
    }

    /**
     * Scelte dei filtri per anno scolastico: tutti gli anni dell'elenco, attivi o no (lo storico si consulta), più
     * quelli presenti nei dati della vista, dal più recente.
     */
    public List<String> perFiltro(Collection<String> anniDeiDati) {
        Set<String> anni = new TreeSet<>(Comparator.reverseOrder());
        annoScolasticoRepository.findAll().forEach(anno -> anni.add(anno.getNome()));
        anniDeiDati.stream().filter(Objects::nonNull).forEach(anni::add);
        return List.copyOf(anni);
    }

    /** Anni dell'elenco non attivi (non si scelgono per i corsi nuovi, restano nei filtri come archivio). */
    public Set<String> nonAttivi() {
        Set<String> nonAttivi = new HashSet<>();
        annoScolasticoRepository.findAll().stream().filter(anno -> !anno.isAttivo()).forEach(anno -> nonAttivi.add(anno.getNome()));
        return nonAttivi;
    }

    /** Scelte per la pagina del corso: gli anni attivi più quello già assegnato al corso, in ordine. */
    public List<String> scelteCorso(String valoreAttuale) {
        Set<String> scelte = new TreeSet<>();
        annoScolasticoRepository.findByAttivoTrue().forEach(anno -> scelte.add(anno.getNome()));
        if (valoreAttuale != null) {
            scelte.add(valoreAttuale);
        }
        return List.copyOf(scelte);
    }

    /** Aggiunge un anno attivo; il nome deve essere "AAAA/AAAA" con anni consecutivi e non ancora presente. */
    @Transactional
    public AnnoScolastico crea(String nome) {
        String valore = nome == null ? null : nome.strip();
        if (!AnnoScolastico.valido(valore)) {
            throw new IllegalArgumentException("Usa il formato AAAA/AAAA con anni consecutivi, es. 2026/2027");
        }
        if (annoScolasticoRepository.existsByNome(valore)) {
            throw new IllegalArgumentException("L'anno scolastico " + valore + " è già presente");
        }
        return annoScolasticoRepository.save(new AnnoScolastico(valore));
    }

    /** Un anno non attivo non si può più scegliere per i corsi, ma resta su quelli che lo hanno già. */
    @Transactional
    public void impostaAttivo(Long id, boolean attivo) {
        annoScolasticoRepository.findById(id).orElseThrow().setAttivo(attivo);
    }

    @Transactional
    public void elimina(Long id) {
        AnnoScolastico anno = annoScolasticoRepository.findById(id).orElseThrow();
        if (corsoRepository.existsByAnnoScolastico(anno.getNome())) {
            throw new IllegalStateException("L'anno scolastico " + anno.getNome() + " è usato da almeno un corso");
        }
        annoScolasticoRepository.delete(anno);
    }

    /**
     * Se l'elenco è vuoto lo popola con l'anno precedente, corrente e successivo rispetto a {@code oggi}
     * e con gli anni già usati dai corsi.
     */
    @Transactional
    public void inizializzaSeVuoto(LocalDate oggi) {
        if (annoScolasticoRepository.count() > 0) {
            return;
        }
        Set<String> anni = new TreeSet<>(List.of(
            AnnoScolastico.di(oggi.minusYears(1)), AnnoScolastico.di(oggi), AnnoScolastico.di(oggi.plusYears(1))));
        anni.addAll(corsoRepository.findAnniScolasticiUsati());
        anni.forEach(nome -> annoScolasticoRepository.save(new AnnoScolastico(nome)));
    }
}
