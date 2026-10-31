package it.attocchi.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Iscrizione di un allievo (partecipante) a un corso, con l'orario di inizio della sua lezione.
 * <p>
 * Mappata sulla tabella {@code corso_allievi} che prima era la join table del {@code @ManyToMany}
 * Corso–Allievo: in produzione le iscrizioni esistenti restano e {@code ddl-auto=update}
 * aggiunge solo la colonna {@code orario_lezione}.
 */
@Entity
@Table(name = "corso_allievi")
public class Iscrizione {

    /**
     * Ordine dei partecipanti: per gruppo (chi non ne ha in fondo), poi per orario di lezione (chi non ce l'ha
     * dopo), poi in ordine di inserimento. Le iscrizioni senza data (precedenti al 2026-10-22) vengono prima e tra
     * loro seguono l'ordine di creazione dell'allievo.
     */
    public static final Comparator<Iscrizione> ORDINE = Comparator
        .comparing(Iscrizione::getGruppo, Comparator.nullsLast(GruppoCorso.PER_GIORNO.thenComparing(GruppoCorso::getId)))
        .thenComparing(Iscrizione::getOrarioLezione, Comparator.nullsLast(Comparator.naturalOrder()))
        .thenComparing(Iscrizione::getIscrittoIl, Comparator.nullsFirst(Comparator.naturalOrder()))
        .thenComparing(i -> i.getAllievo().getId());

    @EmbeddedId
    private IscrizioneId id = new IscrizioneId();

    @ManyToOne
    @MapsId("corsoId")
    @JoinColumn(name = "corso_id")
    private Corso corso;

    @ManyToOne
    @MapsId("allievoId")
    @JoinColumn(name = "allievo_id")
    private Allievo allievo;

    /** Orario di inizio della lezione del partecipante; {@code null} se non ancora assegnato. */
    private LocalTime orarioLezione;

    /** Quota da pagare se diversa da quella piena del corso; {@code null} = quota piena. */
    @Column(precision = 10, scale = 2)
    private BigDecimal quotaPersonalizzata;

    /** Motivo della quota personalizzata (es. "fratelli"). */
    private String notaQuota;

    /** Quota applicata; {@code null} (iscrizioni precedenti) = dedotta da {@link #quotaPersonalizzata}. */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_quota", length = 20)
    private TipoQuota tipoQuota;

    /**
     * Gruppo del corso a cui appartiene il partecipante, che frequenta solo il giorno del gruppo; {@code null} =
     * tutti i giorni del corso. Si cambia con {@code CorsoService.assegnaGruppo}, che aggiorna le presenze delle
     * lezioni da fare.
     */
    @ManyToOne
    @JoinColumn(name = "gruppo_id")
    private GruppoCorso gruppo;

    /**
     * Opzioni aggiuntive del corso attivate dal partecipante (es. il kit): si sommano alla quota. Si cambiano con
     * {@code CorsoService.impostaOpzione}.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "iscrizione_opzioni",
        joinColumns = {@JoinColumn(name = "corso_id", referencedColumnName = "corso_id"),
            @JoinColumn(name = "allievo_id", referencedColumnName = "allievo_id")},
        inverseJoinColumns = @JoinColumn(name = "opzione_id"))
    private Set<OpzioneCorso> opzioni = new HashSet<>();

    /** Data e ora dell'iscrizione, per l'ordine di inserimento; {@code null} per le iscrizioni precedenti. */
    @Column(name = "iscritto_il")
    private LocalDateTime iscrittoIl;

    // Costruttori
    public Iscrizione() {
    }

    /** Corso e allievo devono essere già salvati (la chiave è formata dai loro id). */
    public Iscrizione(Corso corso, Allievo allievo) {
        this.id = new IscrizioneId(corso.getId(), allievo.getId());
        this.corso = corso;
        this.allievo = allievo;
        this.iscrittoIl = LocalDateTime.now();
    }

    // Getters e Setters
    public IscrizioneId getId() {
        return id;
    }

    public LocalDateTime getIscrittoIl() {
        return iscrittoIl;
    }

    public Corso getCorso() {
        return corso;
    }

    public Allievo getAllievo() {
        return allievo;
    }

    public LocalTime getOrarioLezione() {
        return orarioLezione;
    }

    public void setOrarioLezione(LocalTime orarioLezione) {
        this.orarioLezione = orarioLezione;
    }

    public BigDecimal getQuotaPersonalizzata() {
        return quotaPersonalizzata;
    }

    public void setQuotaPersonalizzata(BigDecimal quotaPersonalizzata) {
        this.quotaPersonalizzata = quotaPersonalizzata;
    }

    public String getNotaQuota() {
        return notaQuota;
    }

    public void setNotaQuota(String notaQuota) {
        this.notaQuota = notaQuota;
    }

    public GruppoCorso getGruppo() {
        return gruppo;
    }

    public void setGruppo(GruppoCorso gruppo) {
        this.gruppo = gruppo;
    }

    /**
     * Giorni che il partecipante frequenta tra quelli indicati del corso: il giorno del suo gruppo se è ancora del
     * corso, altrimenti (senza gruppo) tutti quelli del corso.
     */
    public Set<DayOfWeek> giorniEffettivi(Set<DayOfWeek> giorniCorso) {
        if (gruppo != null && giorniCorso.contains(gruppo.getGiorno())) {
            return EnumSet.of(gruppo.getGiorno());
        }
        return giorniCorso.isEmpty() ? EnumSet.noneOf(DayOfWeek.class) : EnumSet.copyOf(giorniCorso);
    }

    /** Giorni effettivi rispetto ai giorni del suo corso (collezione da caricare nella transazione). */
    public Set<DayOfWeek> giorniEffettivi() {
        return giorniEffettivi(corso.getGiorniSettimana());
    }

    /**
     * {@code true} se il partecipante frequenta la lezione di quella data: è uno dei suoi giorni, oppure la data non
     * cade in nessun giorno del corso (lezione straordinaria, per tutti).
     */
    public boolean frequenta(LocalDate data) {
        DayOfWeek giorno = data.getDayOfWeek();
        return !corso.getGiorniSettimana().contains(giorno) || giorniEffettivi().contains(giorno);
    }

    /**
     * Orario della lezione per il partecipante: il suo orario se c'è, altrimenti quello del suo gruppo, altrimenti
     * quello della lezione. Una lezione con un orario diverso da quello del corso (spostata o aggiunta a un'altra ora)
     * ha il suo, scelto per quella data.
     */
    public LocalTime orarioDi(Lezione lezione) {
        if (lezione.getOrarioInizio() != null && !lezione.getOrarioInizio().equals(lezione.getCorso().getOrarioInizio())) {
            return lezione.getOrarioInizio();
        }
        LocalTime abituale = orarioAbituale();
        return abituale != null ? abituale : lezione.getOrarioInizio();
    }

    /** Orario delle lezioni del partecipante: il suo se c'è, altrimenti quello del suo gruppo; {@code null} se nessuno dei due. */
    public LocalTime orarioAbituale() {
        if (orarioLezione != null) {
            return orarioLezione;
        }
        return gruppo != null ? gruppo.getOrario() : null;
    }

    public Set<OpzioneCorso> getOpzioni() {
        return opzioni;
    }

    /** Opzioni attive in ordine di inserimento nel corso. */
    public List<OpzioneCorso> getOpzioniOrdinate() {
        return opzioni.stream().sorted(OpzioneCorso.ORDINE).toList();
    }

    /** Quota applicata: piena, pre-iscrizione o personalizzata. */
    public TipoQuota getTipoQuota() {
        if (tipoQuota != null) {
            return tipoQuota;
        }
        return quotaPersonalizzata != null ? TipoQuota.PERSONALIZZATA : TipoQuota.PIENA;
    }

    /** {@code true} per le iscrizioni precedenti alla scelta del tipo di quota, che lo deducono dalla quota. */
    public boolean isTipoQuotaDedotto() {
        return tipoQuota == null;
    }

    public void setTipoQuota(TipoQuota tipoQuota) {
        this.tipoQuota = tipoQuota;
    }

    /**
     * Quota del corso (senza opzioni): il costo totale, il prezzo di pre-iscrizione del corso (il costo totale se il
     * corso non lo ha più) o la quota personalizzata.
     */
    public BigDecimal getQuotaCorso() {
        BigDecimal piena = corso != null && corso.getCostoTotale() != null ? corso.getCostoTotale() : BigDecimal.ZERO;
        return switch (getTipoQuota()) {
            case PIENA -> piena;
            case PREISCRIZIONE -> corso != null && corso.getPrezzoPreiscrizione() != null ? corso.getPrezzoPreiscrizione() : piena;
            case PERSONALIZZATA -> quotaPersonalizzata != null ? quotaPersonalizzata : piena;
        };
    }

    /** Motivo della quota da mostrare: nessuno per la piena, "pre-iscrizione", o la nota della personalizzata. */
    public String getMotivoQuota() {
        return switch (getTipoQuota()) {
            case PIENA -> null;
            case PREISCRIZIONE -> "pre-iscrizione";
            case PERSONALIZZATA -> notaQuota;
        };
    }

    /** Quota da pagare: quota del corso più le opzioni aggiuntive attive. */
    public BigDecimal getQuotaDovuta() {
        return opzioni.stream().map(OpzioneCorso::getImporto).reduce(getQuotaCorso(), BigDecimal::add);
    }

    /** Uguaglianza sulla chiave (corso, allievo). */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Iscrizione other)) {
            return false;
        }
        return id != null && id.getCorsoId() != null && id.getAllievoId() != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Iscrizione.class.hashCode();
    }
}
