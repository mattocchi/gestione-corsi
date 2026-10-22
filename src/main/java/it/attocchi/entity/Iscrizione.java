package it.attocchi.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.EnumSet;
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

    /**
     * Gruppo del corso a cui appartiene il partecipante, che frequenta solo il giorno del gruppo; {@code null} =
     * tutti i giorni del corso. Si cambia con {@code CorsoService.assegnaGruppo}, che aggiorna le presenze delle
     * lezioni da fare.
     */
    @ManyToOne
    @JoinColumn(name = "gruppo_id")
    private GruppoCorso gruppo;

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
