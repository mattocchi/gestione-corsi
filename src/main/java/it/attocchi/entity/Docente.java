package it.attocchi.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "docenti")
public class Docente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String specializzazione;

    private String biografia;

    @OneToMany(mappedBy = "docente")
    private Set<Corso> corsi = new HashSet<>();

    // Costruttori
    public Docente() {
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getSpecializzazione() {
        return specializzazione;
    }

    public void setSpecializzazione(String specializzazione) {
        this.specializzazione = specializzazione;
    }

    public String getBiografia() {
        return biografia;
    }

    public void setBiografia(String biografia) {
        this.biografia = biografia;
    }

    public Set<Corso> getCorsi() {
        return corsi;
    }

    public void setCorsi(Set<Corso> corsi) {
        this.corsi = corsi;
    }

    public String getNomeCompleto() {
        return user != null ? user.getNomeCompleto() : "";
    }
}
