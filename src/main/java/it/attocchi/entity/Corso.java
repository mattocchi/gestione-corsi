package it.attocchi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "corsi")
public class Corso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Il nome del corso è obbligatorio")
    @Column(nullable = false)
    private String nome;

    @NotNull(message = "La data di inizio è obbligatoria")
    @Column(name = "data_inizio", nullable = false)
    private LocalDate dataInizio;

    @NotNull(message = "La data di fine è obbligatoria")
    @Column(name = "data_fine", nullable = false)
    private LocalDate dataFine;

    @ManyToOne
    @JoinColumn(name = "docente_id")
    private Docente docente;

    @ManyToMany
    @JoinTable(
        name = "corso_allievo",
        joinColumns = @JoinColumn(name = "corso_id"),
        inverseJoinColumns = @JoinColumn(name = "allievo_id")
    )
    private Set<Allievo> allievi = new HashSet<>();

    @OneToMany(mappedBy = "corso", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<LezioneOrario> orariLezioni = new HashSet<>();

    @OneToMany(mappedBy = "corso", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Presenza> presenze = new HashSet<>();

    // Constructors
    public Corso() {
    }

    public Corso(String nome, LocalDate dataInizio, LocalDate dataFine) {
        this.nome = nome;
        this.dataInizio = dataInizio;
        this.dataFine = dataFine;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public LocalDate getDataInizio() {
        return dataInizio;
    }

    public void setDataInizio(LocalDate dataInizio) {
        this.dataInizio = dataInizio;
    }

    public LocalDate getDataFine() {
        return dataFine;
    }

    public void setDataFine(LocalDate dataFine) {
        this.dataFine = dataFine;
    }

    public Docente getDocente() {
        return docente;
    }

    public void setDocente(Docente docente) {
        this.docente = docente;
    }

    public Set<Allievo> getAllievi() {
        return allievi;
    }

    public void setAllievi(Set<Allievo> allievi) {
        this.allievi = allievi;
    }

    public Set<LezioneOrario> getOrariLezioni() {
        return orariLezioni;
    }

    public void setOrariLezioni(Set<LezioneOrario> orariLezioni) {
        this.orariLezioni = orariLezioni;
    }

    public Set<Presenza> getPresenze() {
        return presenze;
    }

    public void setPresenze(Set<Presenza> presenze) {
        this.presenze = presenze;
    }

    @Override
    public String toString() {
        return nome;
    }
}
