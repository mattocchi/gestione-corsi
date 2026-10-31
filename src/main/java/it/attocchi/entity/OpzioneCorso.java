package it.attocchi.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Objects;

/**
 * Opzione aggiuntiva di un corso (es. "Kit strumentario" +30 €): un importo una tantum che si somma alla quota solo
 * degli iscritti che la attivano ({@link Iscrizione#getOpzioni()}). L'importo vale per tutti: cambiandolo cambia la
 * quota di chi ha l'opzione.
 */
@Entity
@Table(name = "corso_opzioni")
public class OpzioneCorso {

    /** Lunghezza massima della descrizione. */
    public static final int LUNGHEZZA_DESCRIZIONE = 100;

    /** Ordine di inserimento. */
    public static final Comparator<OpzioneCorso> ORDINE = Comparator.comparing(OpzioneCorso::getId,
        Comparator.nullsLast(Comparator.naturalOrder()));

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "corso_id")
    private Corso corso;

    @Column(nullable = false, length = LUNGHEZZA_DESCRIZIONE)
    private String descrizione;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal importo;

    public OpzioneCorso() {
    }

    public OpzioneCorso(Corso corso, String descrizione, BigDecimal importo) {
        this.corso = corso;
        this.descrizione = descrizione;
        this.importo = importo;
    }

    public Long getId() {
        return id;
    }

    public Corso getCorso() {
        return corso;
    }

    public void setCorso(Corso corso) {
        this.corso = corso;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public BigDecimal getImporto() {
        return importo;
    }

    public void setImporto(BigDecimal importo) {
        this.importo = importo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof OpzioneCorso other && id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
