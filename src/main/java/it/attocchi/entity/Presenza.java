package it.attocchi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "presenze", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"lezione_id", "allievo_id"})
})
public class Presenza {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "lezione_id")
    @NotNull
    private Lezione lezione;

    @ManyToOne
    @JoinColumn(name = "allievo_id")
    @NotNull
    private Allievo allievo;

    @Enumerated(EnumType.STRING)
    @NotNull
    private StatoPresenza stato;

    private String note;

    // Costruttori
    public Presenza() {
    }

    public Presenza(Lezione lezione, Allievo allievo, StatoPresenza stato) {
        this.lezione = lezione;
        this.allievo = allievo;
        this.stato = stato;
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Lezione getLezione() {
        return lezione;
    }

    public void setLezione(Lezione lezione) {
        this.lezione = lezione;
    }

    public Allievo getAllievo() {
        return allievo;
    }

    public void setAllievo(Allievo allievo) {
        this.allievo = allievo;
    }

    public StatoPresenza getStato() {
        return stato;
    }

    public void setStato(StatoPresenza stato) {
        this.stato = stato;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    /**
     * Uguaglianza basata sull'id (entity non ancora salvate sono uguali solo a se stesse).
     * hashCode costante per classe: resta stabile quando l'id viene assegnato al salvataggio.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Presenza other)) {
            return false;
        }
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Presenza.class.hashCode();
    }

    public enum StatoPresenza {
        PRESENTE("Presente"),
        ASSENTE_GIUSTIFICATA("Assente giustificata"),
        ASSENTE_INGIUSTIFICATA("Assente ingiustificata");

        private final String descrizione;

        StatoPresenza(String descrizione) {
            this.descrizione = descrizione;
        }

        public String getDescrizione() {
            return descrizione;
        }
    }
}
