package it.attocchi.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "insegnanti")
public class Insegnante implements Anagrafica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String specializzazione;

    private String biografia;

    private String codiceFiscale;

    private LocalDate dataNascita;

    private String luogoNascita;

    @Embedded
    private Indirizzo indirizzo = new Indirizzo();

    /** Tariffa oraria in euro: impostata solo da admin e segreteria, l'insegnante la vede in sola lettura. */
    @Column(precision = 10, scale = 2)
    private BigDecimal tariffaOraria;

    /**
     * Rimborso forfettario annuo in euro, sommato al costo orario in ogni anno scolastico in cui l'insegnante ha
     * corsi: impostato solo da admin e segreteria, l'insegnante lo vede in sola lettura.
     */
    @Column(name = "rimborso_forfettario", precision = 10, scale = 2)
    private BigDecimal rimborsoForfettario;

    @OneToMany(mappedBy = "insegnante")
    private Set<Corso> corsi = new HashSet<>();

    /** Corsi in cui è il secondo insegnante. */
    @OneToMany(mappedBy = "secondoInsegnante")
    private Set<Corso> corsiComeSecondo = new HashSet<>();

    // Costruttori
    public Insegnante() {
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

    public BigDecimal getTariffaOraria() {
        return tariffaOraria;
    }

    public void setTariffaOraria(BigDecimal tariffaOraria) {
        this.tariffaOraria = tariffaOraria;
    }

    public BigDecimal getRimborsoForfettario() {
        return rimborsoForfettario;
    }

    public void setRimborsoForfettario(BigDecimal rimborsoForfettario) {
        this.rimborsoForfettario = rimborsoForfettario;
    }

    public Set<Corso> getCorsi() {
        return corsi;
    }

    public void setCorsi(Set<Corso> corsi) {
        this.corsi = corsi;
    }

    /** Corsi in cui è titolare o secondo insegnante (le collezioni vanno caricate, es. {@code findAllWithCorsi}). */
    public Set<Corso> getTuttiICorsi() {
        Set<Corso> tutti = new HashSet<>(corsi);
        tutti.addAll(corsiComeSecondo);
        return tutti;
    }

    public int getNumeroCorsi() {
        return getTuttiICorsi().size();
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
        if (!(o instanceof Insegnante other)) {
            return false;
        }
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Insegnante.class.hashCode();
    }
}
