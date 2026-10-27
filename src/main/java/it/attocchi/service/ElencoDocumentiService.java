package it.attocchi.service;

import it.attocchi.entity.Allievo;
import it.attocchi.entity.Genitore;
import it.attocchi.entity.InvioEmail;
import it.attocchi.entity.InvioEmail.Esito;
import it.attocchi.entity.Iscrizione;
import it.attocchi.entity.IscrizioneId;
import it.attocchi.entity.TipoDocumento;
import it.attocchi.entity.User;
import it.attocchi.repository.AllievoRepository;
import it.attocchi.repository.GenitoreRepository;
import it.attocchi.repository.InvioEmailRepository;
import it.attocchi.repository.IscrizioneRepository;
import it.attocchi.repository.PagamentoRepository;
import it.attocchi.repository.UserRepository;
import it.attocchi.service.EmailService.Destinatario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Elenchi della sezione "Documenti": per admin e segreteria i documenti di ogni iscrizione (conferme, ricevute) e i
 * riepiloghi annuali di famiglie e allievi adulti, con destinatario e ultimo invio; per genitori e allievi adulti i
 * propri documenti. I PDF e gli invii restano in {@link DocumentiService} ed {@link EmailService}, che controllano i
 * permessi.
 */
@Service
@Transactional(readOnly = true)
public class ElencoDocumentiService {

    /** Ordine degli anni scolastici: dal più recente, i corsi senza anno in fondo. */
    private static final Comparator<String> PER_ANNO = Comparator.nullsLast(Comparator.<String>reverseOrder());

    /**
     * Conferma o ricevuta di un'iscrizione per lo staff: {@code destinatario} vuoto se nessuno ha un'email,
     * {@code datiMancanti} l'avviso se mancano i dati del documento (data di nascita; per la ricevuta anche il codice
     * fiscale), {@code ultimoInvio} l'ultima email non in errore di quel documento ({@code null} se mai inviato).
     */
    public record DocumentoIscrizione(TipoDocumento tipo, Iscrizione iscrizione, Optional<Destinatario> destinatario,
                                      String datiMancanti, InvioEmail ultimoInvio) {
    }

    /** Riepilogo annuale della famiglia ({@code genitore}) o dell'allievo adulto ({@code allievo}) per lo staff. */
    public record DocumentoRiepilogo(Genitore genitore, Allievo allievo, String anno, List<String> allievi, int corsi,
                                     Optional<Destinatario> destinatario, InvioEmail ultimoInvio) {

        public String intestatario() {
            return genitore != null ? genitore.getNomeCompleto() : allievo.getNomeCompleto();
        }
    }

    /** Attestazione delle spese di un allievo in un anno solare per lo staff, con il totale pagato nell'anno. */
    public record DocumentoAttestazione(Allievo allievo, int anno, BigDecimal totale, Optional<Destinatario> destinatario,
                                        InvioEmail ultimoInvio) {
    }

    /**
     * Documento di un genitore o di un allievo adulto: conferma o ricevuta di un'iscrizione ({@code iscrizione}) o
     * riepilogo dell'anno ({@code genitore} o {@code allievo}); {@code datiMancanti} come per lo staff.
     */
    public record DocumentoFamiglia(TipoDocumento tipo, Iscrizione iscrizione, Genitore genitore, Allievo allievo,
                                    String anno, String datiMancanti) {
    }

    private record ChiaveRiepilogo(Long genitoreId, Long allievoId, String anno) {
    }

    private final IscrizioneRepository iscrizioneRepository;
    private final PagamentoRepository pagamentoRepository;
    private final InvioEmailRepository invioEmailRepository;
    private final GenitoreRepository genitoreRepository;
    private final AllievoRepository allievoRepository;
    private final UserRepository userRepository;

    public ElencoDocumentiService(IscrizioneRepository iscrizioneRepository, PagamentoRepository pagamentoRepository,
                                  InvioEmailRepository invioEmailRepository, GenitoreRepository genitoreRepository,
                                  AllievoRepository allievoRepository, UserRepository userRepository) {
        this.iscrizioneRepository = iscrizioneRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.invioEmailRepository = invioEmailRepository;
        this.genitoreRepository = genitoreRepository;
        this.allievoRepository = allievoRepository;
        this.userRepository = userRepository;
    }

    /**
     * Conferme (ogni iscrizione) o ricevute (le iscrizioni con pagamenti) di tutti i corsi, per anno scolastico (dal
     * più recente), corso e nell'ordine dei partecipanti. Solo admin e segreteria.
     */
    public List<DocumentoIscrizione> documentiIscrizioni(TipoDocumento tipo, String username) {
        verificaStaff(username);
        if (!tipo.isDellIscrizione()) {
            throw new IllegalArgumentException("Non è un documento dell'iscrizione: " + tipo);
        }
        Set<IscrizioneId> conPagamenti = conPagamenti();
        Map<IscrizioneId, InvioEmail> ultimi = new HashMap<>();
        for (InvioEmail invio : invioEmailRepository.findByTipoAndEsitoNotOrderByDataOraDesc(tipo, Esito.ERRORE)) {
            if (invio.getCorsoId() != null && invio.getAllievoId() != null) {
                ultimi.putIfAbsent(new IscrizioneId(invio.getCorsoId(), invio.getAllievoId()), invio);
            }
        }
        return iscrizioneRepository.findAllWithCorsoAllievoGenitore().stream()
            .filter(i -> tipo != TipoDocumento.RICEVUTA || conPagamenti.contains(i.getId()))
            .sorted(Comparator.comparing((Iscrizione i) -> i.getCorso().getAnnoScolastico(), PER_ANNO)
                .thenComparing(i -> i.getCorso().getNome(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(i -> i.getCorso().getId())
                .thenComparing(Iscrizione.ORDINE))
            .map(i -> new DocumentoIscrizione(tipo, i, EmailService.destinatarioDi(i.getAllievo()),
                DocumentiService.avvisoDatiMancanti(i.getAllievo(), tipo), ultimi.get(i.getId())))
            .toList();
    }

    /**
     * Riepiloghi annuali: uno per genitore e anno dei corsi dei figli, uno per allievo adulto (senza genitore) e anno
     * dei suoi corsi; per anno (dal più recente) e intestatario. Solo admin e segreteria.
     */
    public List<DocumentoRiepilogo> riepiloghi(String username) {
        verificaStaff(username);
        Map<ChiaveRiepilogo, List<Iscrizione>> perRiepilogo = new LinkedHashMap<>();
        for (Iscrizione iscrizione : iscrizioneRepository.findAllWithCorsoAllievoGenitore()) {
            Allievo allievo = iscrizione.getAllievo();
            Genitore genitore = allievo.getGenitore();
            ChiaveRiepilogo chiave = new ChiaveRiepilogo(genitore != null ? genitore.getId() : null,
                genitore == null ? allievo.getId() : null, iscrizione.getCorso().getAnnoScolastico());
            perRiepilogo.computeIfAbsent(chiave, k -> new ArrayList<>()).add(iscrizione);
        }
        Map<ChiaveRiepilogo, InvioEmail> ultimi = new HashMap<>();
        for (InvioEmail invio : invioEmailRepository.findByTipoAndEsitoNotOrderByDataOraDesc(TipoDocumento.RIEPILOGO, Esito.ERRORE)) {
            ultimi.putIfAbsent(new ChiaveRiepilogo(invio.getGenitoreId(), invio.getGenitoreId() == null ? invio.getAllievoId() : null,
                invio.getAnno()), invio);
        }
        List<DocumentoRiepilogo> riepiloghi = new ArrayList<>();
        perRiepilogo.forEach((chiave, iscrizioni) -> {
            Allievo primo = iscrizioni.getFirst().getAllievo();
            Genitore genitore = chiave.genitoreId() != null ? primo.getGenitore() : null;
            Allievo adulto = genitore == null ? primo : null;
            List<String> allievi = iscrizioni.stream().map(i -> i.getAllievo().getNomeCompleto())
                .distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
            riepiloghi.add(new DocumentoRiepilogo(genitore, adulto, chiave.anno(), allievi, iscrizioni.size(),
                EmailService.destinatarioRiepilogoDi(genitore, adulto), ultimi.get(chiave)));
        });
        riepiloghi.sort(Comparator.comparing(DocumentoRiepilogo::anno, PER_ANNO)
            .thenComparing(DocumentoRiepilogo::intestatario, String.CASE_INSENSITIVE_ORDER));
        return riepiloghi;
    }

    /**
     * Attestazioni delle spese: una per allievo e anno solare con almeno un pagamento, per anno (dal più recente) e
     * allievo. Solo admin e segreteria.
     */
    public List<DocumentoAttestazione> attestazioni(String username) {
        verificaStaff(username);
        Map<Long, Allievo> allievi = new HashMap<>();
        allievoRepository.findAll().forEach(a -> allievi.put(a.getId(), a));
        Map<String, InvioEmail> ultimi = new HashMap<>();
        for (InvioEmail invio : invioEmailRepository.findByTipoAndEsitoNotOrderByDataOraDesc(TipoDocumento.ATTESTAZIONE, Esito.ERRORE)) {
            ultimi.putIfAbsent(invio.getAllievoId() + "/" + invio.getAnno(), invio);
        }
        List<DocumentoAttestazione> attestazioni = new ArrayList<>();
        for (Object[] riga : pagamentoRepository.totaliPerAllievoEAnno()) {
            Allievo allievo = allievi.get((Long) riga[0]);
            int anno = ((Number) riga[1]).intValue();
            Genitore genitore = allievo.getGenitore();
            attestazioni.add(new DocumentoAttestazione(allievo, anno, (BigDecimal) riga[2],
                EmailService.destinatarioRiepilogoDi(genitore, genitore == null ? allievo : null),
                ultimi.get(allievo.getId() + "/" + anno)));
        }
        attestazioni.sort(Comparator.comparing(DocumentoAttestazione::anno, Comparator.reverseOrder())
            .thenComparing(d -> d.allievo().getNomeCompleto(), String.CASE_INSENSITIVE_ORDER));
        return attestazioni;
    }

    /**
     * Documenti dell'utente: per il genitore quelli dei figli e i riepiloghi della famiglia, per l'allievo senza
     * genitore i propri (chi ha un genitore non ne ha: sono del genitore). Conferma di ogni iscrizione, ricevuta se ci
     * sono pagamenti, riepilogo per ogni anno dei corsi; anche dei corsi non più attivi. Per anno (dal più recente),
     * con il riepilogo per primo, poi per corso, allievo e tipo.
     */
    public List<DocumentoFamiglia> documentiDi(String username) {
        Set<IscrizioneId> conPagamenti = conPagamenti();
        List<DocumentoFamiglia> documenti = new ArrayList<>();
        genitoreRepository.findByUserUsername(username).ifPresent(genitore -> aggiungi(documenti,
            pubblicate(iscrizioneRepository.findByGenitoreUsernameWithCorsoAllievo(username)), genitore, null, conPagamenti));
        allievoRepository.findByUserUsername(username).filter(a -> a.getGenitore() == null).ifPresent(allievo -> aggiungi(documenti,
            pubblicate(iscrizioneRepository.findByAllievoIdWithCorso(allievo.getId())), null, allievo, conPagamenti));
        // Attestazioni delle spese per anno solare: dei figli, o dell'allievo adulto
        Map<Long, Allievo> propri = new HashMap<>();
        documenti.stream().filter(d -> d.iscrizione() != null).map(d -> d.iscrizione().getAllievo())
            .forEach(a -> propri.putIfAbsent(a.getId(), a));
        for (Object[] riga : pagamentoRepository.totaliPerAllievoEAnno()) {
            Allievo allievo = propri.get((Long) riga[0]);
            if (allievo != null) {
                documenti.add(new DocumentoFamiglia(TipoDocumento.ATTESTAZIONE, null, null, allievo,
                    String.valueOf(((Number) riga[1]).intValue()), null));
            }
        }
        documenti.sort(Comparator.comparing(DocumentoFamiglia::anno, PER_ANNO)
            .thenComparing(d -> d.iscrizione() != null)
            .thenComparing(d -> d.iscrizione() != null ? d.iscrizione().getCorso().getNome() : "", String.CASE_INSENSITIVE_ORDER)
            .thenComparing(d -> d.iscrizione() != null ? d.iscrizione().getAllievo().getNomeCompleto() : "", String.CASE_INSENSITIVE_ORDER)
            .thenComparing(DocumentoFamiglia::tipo));
        return documenti;
    }

    private static void aggiungi(List<DocumentoFamiglia> documenti, List<Iscrizione> iscrizioni, Genitore genitore,
                                 Allievo allievo, Set<IscrizioneId> conPagamenti) {
        Set<String> anni = new HashSet<>();
        for (Iscrizione iscrizione : iscrizioni) {
            String anno = iscrizione.getCorso().getAnnoScolastico();
            documenti.add(new DocumentoFamiglia(TipoDocumento.CONFERMA_ISCRIZIONE, iscrizione, null, null, anno,
                DocumentiService.avvisoDatiMancanti(iscrizione.getAllievo(), TipoDocumento.CONFERMA_ISCRIZIONE)));
            if (conPagamenti.contains(iscrizione.getId())) {
                documenti.add(new DocumentoFamiglia(TipoDocumento.RICEVUTA, iscrizione, null, null, anno,
                    DocumentiService.avvisoDatiMancanti(iscrizione.getAllievo(), TipoDocumento.RICEVUTA)));
            }
            if (anni.add(Objects.requireNonNullElse(anno, ""))) {
                documenti.add(new DocumentoFamiglia(TipoDocumento.RIEPILOGO, null, genitore, allievo, anno, null));
            }
        }
    }

    /** Solo i corsi pubblicati: quelli in preparazione le famiglie non li vedono. */
    private static List<Iscrizione> pubblicate(List<Iscrizione> iscrizioni) {
        return iscrizioni.stream().filter(i -> i.getCorso().isPubblicato()).toList();
    }

    private Set<IscrizioneId> conPagamenti() {
        Set<IscrizioneId> chiavi = new HashSet<>();
        for (Object[] riga : pagamentoRepository.sommaPerIscrizione()) {
            chiavi.add(new IscrizioneId((Long) riga[0], (Long) riga[1]));
        }
        return chiavi;
    }

    private void verificaStaff(String username) {
        Set<String> ruoli = userRepository.findByUsername(username).map(User::getRoles).orElse(Set.of());
        if (!ruoli.contains("ADMIN") && !ruoli.contains("SEGRETERIA")) {
            throw new IllegalStateException("Solo admin e segreteria vedono i documenti di tutti");
        }
    }
}
