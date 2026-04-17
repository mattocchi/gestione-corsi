package it.attocchi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "lezioni")
public class Lezione {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "corso_id")
    @NotNull
    private Corso corso;

    @NotNull
    private LocalDate data;

    private LocalTime orarioInizio;

    private LocalTime orarioFine;

    private Integer numeroLezione;

    private String note;

    @OneToMany(mappedBy = "lezione", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Presenza> presenze = new HashSet<>();

    @Column(name = "completata")
    private boolean completata = false;

    // Costruttori
    public Lezione() {
    }

    public Lezione(Corso corso, LocalDate data, Integer numeroLezione) {
        this.corso = corso;
        this.data = data;
        this.numeroLezione = numeroLezione;
    }

    // Getters e Setters
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

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public LocalTime getOrarioInizio() {
        return orarioInizio;
    }

    public void setOrarioInizio(LocalTime orarioInizio) {
        this.orarioInizio = orarioInizio;
    }

    public LocalTime getOrarioFine() {
        return orarioFine;
    }

    public void setOrarioFine(LocalTime orarioFine) {
        this.orarioFine = orarioFine;
    }

    public Integer getNumeroLezione() {
        return numeroLezione;
    }

    public void setNumeroLezione(Integer numeroLezione) {
        this.numeroLezione = numeroLezione;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Set<Presenza> getPresenze() {
        return presenze;
    }

    public void setPresenze(Set<Presenza> presenze) {
        this.presenze = presenze;
    }

    public boolean isCompletata() {
        return completata;
    }

    public void setCompletata(boolean completata) {
        this.completata = completata;
    }
}
