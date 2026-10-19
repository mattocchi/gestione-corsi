package it.attocchi.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
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

    /** Ordine dei partecipanti: per orario di lezione, quelli senza orario in fondo; a parità per nome. */
    public static final Comparator<Iscrizione> PER_ORARIO = Comparator
        .comparing(Iscrizione::getOrarioLezione, Comparator.nullsLast(Comparator.naturalOrder()))
        .thenComparing(i -> i.getAllievo().getNomeCompleto());

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

    /**
     * Giorni del corso che il partecipante frequenta; vuoto = tutti i giorni del corso. Si cambiano con
     * {@code CorsoService.impostaGiorni}, che aggiorna le presenze delle lezioni da fare.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "iscrizione_giorni", joinColumns = {
        @JoinColumn(name = "corso_id", referencedColumnName = "corso_id"),
        @JoinColumn(name = "allievo_id", referencedColumnName = "allievo_id")})
    @Column(name = "giorno_settimana")
    @Enumerated(EnumType.STRING)
    @BatchSize(size = 50)
    private Set<DayOfWeek> giorni = new HashSet<>();

    // Costruttori
    public Iscrizione() {
    }

    /** Corso e allievo devono essere già salvati (la chiave è formata dai loro id). */
    public Iscrizione(Corso corso, Allievo allievo) {
        this.id = new IscrizioneId(corso.getId(), allievo.getId());
        this.corso = corso;
        this.allievo = allievo;
    }

    // Getters e Setters
    public IscrizioneId getId() {
        return id;
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

    public Set<DayOfWeek> getGiorni() {
        return giorni;
    }

    public void setGiorni(Set<DayOfWeek> giorni) {
        this.giorni = giorni;
    }

    /**
     * Giorni che il partecipante frequenta tra quelli indicati del corso: i suoi giorni ancora del corso o, se non
     * ne ha (o nessuno è più del corso), tutti quelli del corso.
     */
    public Set<DayOfWeek> giorniEffettivi(Set<DayOfWeek> giorniCorso) {
        Set<DayOfWeek> suoi = giorniCorso.isEmpty() ? EnumSet.noneOf(DayOfWeek.class) : EnumSet.copyOf(giorniCorso);
        suoi.retainAll(giorni);
        if (suoi.isEmpty()) {
            return giorniCorso.isEmpty() ? EnumSet.noneOf(DayOfWeek.class) : EnumSet.copyOf(giorniCorso);
        }
        return suoi;
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

    /** {@code true} se frequenta solo alcuni dei giorni del corso. */
    public boolean haGiorniPropri() {
        return !giorniEffettivi().equals(corso.getGiorniSettimana().isEmpty()
            ? EnumSet.noneOf(DayOfWeek.class) : EnumSet.copyOf(corso.getGiorniSettimana()));
    }

    /** Quota da pagare: quella personalizzata se impostata, altrimenti il costo totale del corso. */
    public BigDecimal getQuotaDovuta() {
        if (quotaPersonalizzata != null) {
            return quotaPersonalizzata;
        }
        return corso != null && corso.getCostoTotale() != null ? corso.getCostoTotale() : BigDecimal.ZERO;
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
