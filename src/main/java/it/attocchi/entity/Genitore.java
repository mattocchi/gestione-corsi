package it.attocchi.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "genitori")
public class Genitore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String codiceFiscale;

    private LocalDate dataNascita;

    private String luogoNascita;

    @Embedded
    private Indirizzo indirizzo = new Indirizzo();

    @OneToMany(mappedBy = "genitore")
    private Set<Studente> figli = new HashSet<>();

    // Costruttori
    public Genitore() {
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

    public String getCodiceFiscale() {
        return codiceFiscale;
    }

    public void setCodiceFiscale(String codiceFiscale) {
        this.codiceFiscale = codiceFiscale;
    }

    public LocalDate getDataNascita() {
        return dataNascita;
    }

    public void setDataNascita(LocalDate dataNascita) {
        this.dataNascita = dataNascita;
    }

    public String getLuogoNascita() {
        return luogoNascita;
    }

    public void setLuogoNascita(String luogoNascita) {
        this.luogoNascita = luogoNascita;
    }

    /** Mai {@code null}: Hibernate lascia l'embedded a {@code null} quando tutte le colonne sono vuote. */
    public Indirizzo getIndirizzo() {
        if (indirizzo == null) {
            indirizzo = new Indirizzo();
        }
        return indirizzo;
    }

    public void setIndirizzo(Indirizzo indirizzo) {
        this.indirizzo = indirizzo;
    }

    public Set<Studente> getFigli() {
        return figli;
    }

    public void setFigli(Set<Studente> figli) {
        this.figli = figli;
    }

    public String getNomeCompleto() {
        return user != null ? user.getNomeCompleto() : "";
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
        if (!(o instanceof Genitore other)) {
            return false;
        }
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Genitore.class.hashCode();
    }
}
