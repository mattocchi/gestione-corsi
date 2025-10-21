package it.attocchi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "genitori")
public class Genitore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Il nome è obbligatorio")
    @Column(nullable = false)
    private String nome;

    @NotBlank(message = "Il cognome è obbligatorio")
    @Column(nullable = false)
    private String cognome;

    @Column(name = "telefono")
    private String telefono;

    @Column(name = "email")
    private String email;

    @ManyToMany
    @JoinTable(
        name = "genitore_allievo",
        joinColumns = @JoinColumn(name = "genitore_id"),
        inverseJoinColumns = @JoinColumn(name = "allievo_id")
    )
    private Set<Allievo> allievi = new HashSet<>();

    // Constructors
    public Genitore() {
    }

    public Genitore(String nome, String cognome) {
        this.nome = nome;
        this.cognome = cognome;
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

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Set<Allievo> getAllievi() {
        return allievi;
    }

    public void setAllievi(Set<Allievo> allievi) {
        this.allievi = allievi;
    }

    public String getNomeCompleto() {
        return nome + " " + cognome;
    }

    @Override
    public String toString() {
        return getNomeCompleto();
    }
}
