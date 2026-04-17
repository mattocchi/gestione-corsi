package it.attocchi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "presenze", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"lezione_id", "studente_id"})
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
    @JoinColumn(name = "studente_id")
    @NotNull
    private Studente studente;

    @Enumerated(EnumType.STRING)
    @NotNull
    private StatoPresenza stato;

    private String note;

    // Costruttori
    public Presenza() {
    }

    public Presenza(Lezione lezione, Studente studente, StatoPresenza stato) {
        this.lezione = lezione;
        this.studente = studente;
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

    public Studente getStudente() {
        return studente;
    }

    public void setStudente(Studente studente) {
        this.studente = studente;
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
