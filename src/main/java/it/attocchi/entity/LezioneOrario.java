package it.attocchi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@Table(name = "lezioni_orari")
public class LezioneOrario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "corso_id", nullable = false)
    private Corso corso;

    @NotNull(message = "Il giorno della settimana è obbligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "giorno_settimana", nullable = false)
    private DayOfWeek giornoSettimana;

    @NotNull(message = "L'orario di inizio è obbligatorio")
    @Column(name = "ora_inizio", nullable = false)
    private LocalTime oraInizio;

    @NotNull(message = "L'orario di fine è obbligatorio")
    @Column(name = "ora_fine", nullable = false)
    private LocalTime oraFine;

    // Constructors
    public LezioneOrario() {
    }

    public LezioneOrario(Corso corso, DayOfWeek giornoSettimana, LocalTime oraInizio, LocalTime oraFine) {
        this.corso = corso;
        this.giornoSettimana = giornoSettimana;
        this.oraInizio = oraInizio;
        this.oraFine = oraFine;
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

    public DayOfWeek getGiornoSettimana() {
        return giornoSettimana;
    }

    public void setGiornoSettimana(DayOfWeek giornoSettimana) {
        this.giornoSettimana = giornoSettimana;
    }

    public LocalTime getOraInizio() {
        return oraInizio;
    }

    public void setOraInizio(LocalTime oraInizio) {
        this.oraInizio = oraInizio;
    }

    public LocalTime getOraFine() {
        return oraFine;
    }

    public void setOraFine(LocalTime oraFine) {
        this.oraFine = oraFine;
    }

    public String getGiornoItaliano() {
        return switch (giornoSettimana) {
            case MONDAY -> "Lunedì";
            case TUESDAY -> "Martedì";
            case WEDNESDAY -> "Mercoledì";
            case THURSDAY -> "Giovedì";
            case FRIDAY -> "Venerdì";
            case SATURDAY -> "Sabato";
            case SUNDAY -> "Domenica";
        };
    }

    @Override
    public String toString() {
        return getGiornoItaliano() + " " + oraInizio + " - " + oraFine;
    }
}
