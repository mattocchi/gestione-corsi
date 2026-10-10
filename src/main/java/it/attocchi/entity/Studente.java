package it.attocchi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "studenti")
public class Studente implements Anagrafica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    @NotBlank
    private String codiceFiscale;

    @NotNull
    private LocalDate dataNascita;

    private String luogoNascita;

    @Embedded
    private Indirizzo indirizzo = new Indirizzo();

    @OneToMany(mappedBy = "studente")
    private Set<Iscrizione> iscrizioni = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "genitore_id")
    private Genitore genitore;

    // Costruttori
    public Studente() {
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

    /** {@code true} se lo studente non ha nessun campo dell'indirizzo e si mostra quello del genitore. */
    public boolean isIndirizzoDelGenitore() {
        return getIndirizzo().isVuoto() && genitore != null && !genitore.getIndirizzo().isVuoto();
    }

    /** Indirizzo da mostrare: il proprio o, se non ne ha nessun campo, quello del genitore (non copiato). */
    public Indirizzo getIndirizzoEffettivo() {
        return isIndirizzoDelGenitore() ? genitore.getIndirizzo() : getIndirizzo();
    }

    public Set<Iscrizione> getIscrizioni() {
        return iscrizioni;
    }

    /** Corsi a cui lo studente è iscritto, ricavati dalle iscrizioni. */
    public Set<Corso> getCorsi() {
        return iscrizioni.stream().map(Iscrizione::getCorso).collect(Collectors.toSet());
    }

    public Genitore getGenitore() {
        return genitore;
    }

    public void setGenitore(Genitore genitore) {
        this.genitore = genitore;
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
        if (!(o instanceof Studente other)) {
            return false;
        }
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Studente.class.hashCode();
    }
}
