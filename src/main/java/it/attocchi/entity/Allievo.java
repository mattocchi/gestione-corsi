package it.attocchi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "allievi")
public class Allievo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Il nome è obbligatorio")
    @Column(nullable = false)
    private String nome;

    @NotBlank(message = "Il cognome è obbligatorio")
    @Column(nullable = false)
    private String cognome;

    @NotNull(message = "La data di nascita è obbligatoria")
    @Column(name = "data_nascita", nullable = false)
    private LocalDate dataNascita;

    @ManyToMany(mappedBy = "allievi")
    private Set<Corso> corsi = new HashSet<>();

    @ManyToMany(mappedBy = "allievi")
    private Set<Genitore> genitori = new HashSet<>();

    @OneToMany(mappedBy = "allievo", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Presenza> presenze = new HashSet<>();

    // Constructors
    public Allievo() {
    }

    public Allievo(String nome, String cognome, LocalDate dataNascita) {
        this.nome = nome;
        this.cognome = cognome;
        this.dataNascita = dataNascita;
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

    public String getCognome() {
        return cognome;
    }

    public void setCognome(String cognome) {
        this.cognome = cognome;
    }

    public LocalDate getDataNascita() {
        return dataNascita;
    }

    public void setDataNascita(LocalDate dataNascita) {
        this.dataNascita = dataNascita;
    }

    public Set<Corso> getCorsi() {
        return corsi;
    }

    public void setCorsi(Set<Corso> corsi) {
        this.corsi = corsi;
    }

    public Set<Genitore> getGenitori() {
        return genitori;
    }

    public void setGenitori(Set<Genitore> genitori) {
        this.genitori = genitori;
    }

    public Set<Presenza> getPresenze() {
        return presenze;
    }

    public void setPresenze(Set<Presenza> presenze) {
        this.presenze = presenze;
    }

    public String getNomeCompleto() {
        return nome + " " + cognome;
    }

    @Override
    public String toString() {
        return getNomeCompleto();
    }
}
