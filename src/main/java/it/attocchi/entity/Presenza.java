package it.attocchi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Entity
@Table(name = "presenze")
public class Presenza {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "corso_id", nullable = false)
    private Corso corso;

    @ManyToOne
    @JoinColumn(name = "allievo_id", nullable = false)
    private Allievo allievo;

    @NotNull(message = "La data della lezione è obbligatoria")
    @Column(name = "data_lezione", nullable = false)
    private LocalDate dataLezione;

    @NotNull(message = "Lo stato di presenza è obbligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "stato", nullable = false)
    private StatoPresenza stato = StatoPresenza.PRESENTE;

    @Column(name = "note")
    private String note;

    // Constructors
    public Presenza() {
    }

    public Presenza(Corso corso, Allievo allievo, LocalDate dataLezione, StatoPresenza stato) {
        this.corso = corso;
        this.allievo = allievo;
        this.dataLezione = dataLezione;
        this.stato = stato;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Corso getCorso() {
        return corso;
    }

    public void setCorso(Corso corso) {
        this.corso = corso;
    }

    public Allievo getAllievo() {
        return allievo;
    }

    public void setAllievo(Allievo allievo) {
        this.allievo = allievo;
    }

    public LocalDate getDataLezione() {
        return dataLezione;
    }

    public void setDataLezione(LocalDate dataLezione) {
        this.dataLezione = dataLezione;
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
        ASSENTE_GIUSTIFICATO("Assente Giustificato"),
        ASSENTE_INGIUSTIFICATO("Assente Ingiustificato");

        private final String descrizione;

        StatoPresenza(String descrizione) {
            this.descrizione = descrizione;
        }

        public String getDescrizione() {
            return descrizione;
        }

        @Override
        public String toString() {
            return descrizione;
        }
    }
}
