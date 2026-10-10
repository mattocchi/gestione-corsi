package it.attocchi.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Comparator;

/**
 * Iscrizione di uno studente (partecipante) a un corso, con l'orario di inizio della sua lezione.
 * <p>
 * Mappata sulla tabella {@code corso_studenti} che prima era la join table del {@code @ManyToMany}
 * Corso–Studente: in produzione le iscrizioni esistenti restano e {@code ddl-auto=update}
 * aggiunge solo la colonna {@code orario_lezione}.
 */
@Entity
@Table(name = "corso_studenti")
public class Iscrizione {

    /** Ordine dei partecipanti: per orario di lezione, quelli senza orario in fondo; a parità per nome. */
    public static final Comparator<Iscrizione> PER_ORARIO = Comparator
        .comparing(Iscrizione::getOrarioLezione, Comparator.nullsLast(Comparator.naturalOrder()))
        .thenComparing(i -> i.getStudente().getNomeCompleto());

    @EmbeddedId
    private IscrizioneId id = new IscrizioneId();

    @ManyToOne
    @MapsId("corsoId")
    @JoinColumn(name = "corso_id")
    private Corso corso;

    @ManyToOne
    @MapsId("studenteId")
    @JoinColumn(name = "studente_id")
    private Studente studente;

    /** Orario di inizio della lezione del partecipante; {@code null} se non ancora assegnato. */
    private LocalTime orarioLezione;

    /** Quota da pagare se diversa da quella piena del corso; {@code null} = quota piena. */
    @Column(precision = 10, scale = 2)
    private BigDecimal quotaPersonalizzata;

    /** Motivo della quota personalizzata (es. "fratelli"). */
    private String notaQuota;

    // Costruttori
    public Iscrizione() {
    }

    /** Corso e studente devono essere già salvati (la chiave è formata dai loro id). */
    public Iscrizione(Corso corso, Studente studente) {
        this.id = new IscrizioneId(corso.getId(), studente.getId());
        this.corso = corso;
        this.studente = studente;
    }

    // Getters e Setters
    public IscrizioneId getId() {
        return id;
    }

    public Corso getCorso() {
        return corso;
    }

    public Studente getStudente() {
        return studente;
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

    /** Quota da pagare: quella personalizzata se impostata, altrimenti il costo totale del corso. */
    public BigDecimal getQuotaDovuta() {
        if (quotaPersonalizzata != null) {
            return quotaPersonalizzata;
        }
        return corso != null && corso.getCostoTotale() != null ? corso.getCostoTotale() : BigDecimal.ZERO;
    }

    /** Uguaglianza sulla chiave (corso, studente). */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Iscrizione other)) {
            return false;
        }
        return id != null && id.getCorsoId() != null && id.getStudenteId() != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Iscrizione.class.hashCode();
    }
}
