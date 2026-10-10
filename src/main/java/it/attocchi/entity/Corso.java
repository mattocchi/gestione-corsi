package it.attocchi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "corsi")
public class Corso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String nome;

    private String descrizione;

    @NotNull
    private LocalDate dataInizio;

    @NotNull
    private Integer numeroLezioni;

    @NotNull
    private Integer durataLezioneMinuti;

    @ElementCollection
    @CollectionTable(name = "corso_giorni_settimana", joinColumns = @JoinColumn(name = "corso_id"))
    @Column(name = "giorno_settimana")
    @Enumerated(EnumType.STRING)
    private Set<DayOfWeek> giorniSettimana = new HashSet<>();

    private LocalTime orarioInizio;

    private BigDecimal costoTotale;

    private BigDecimal costoPerLezione;

    @ManyToOne
    @JoinColumn(name = "insegnante_id")
    private Insegnante insegnante;

    // Solo REMOVE: salvare un corso dal dialog non deve sovrascrivere le iscrizioni,
    // che si gestiscono con i metodi dedicati di CorsoService
    @OneToMany(mappedBy = "corso", cascade = CascadeType.REMOVE)
    private Set<Iscrizione> iscrizioni = new HashSet<>();

    @OneToMany(mappedBy = "corso", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Lezione> lezioni = new HashSet<>();

    @Column(name = "attivo")
    private boolean attivo = true;

    /** Anno scolastico nel formato "2026/2027" (vedi {@link AnnoScolastico}). */
    @Column(name = "anno_scolastico", length = 9)
    private String annoScolastico;

    // Costruttori
    public Corso() {
    }

    // Getters e Setters
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

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public LocalDate getDataInizio() {
        return dataInizio;
    }

    public void setDataInizio(LocalDate dataInizio) {
        this.dataInizio = dataInizio;
    }

    public Integer getNumeroLezioni() {
        return numeroLezioni;
    }

    public void setNumeroLezioni(Integer numeroLezioni) {
        this.numeroLezioni = numeroLezioni;
    }

    public Integer getDurataLezioneMinuti() {
        return durataLezioneMinuti;
    }

    public void setDurataLezioneMinuti(Integer durataLezioneMinuti) {
        this.durataLezioneMinuti = durataLezioneMinuti;
    }

    public Set<DayOfWeek> getGiorniSettimana() {
        return giorniSettimana;
    }

    public void setGiorniSettimana(Set<DayOfWeek> giorniSettimana) {
        this.giorniSettimana = giorniSettimana;
    }

    public LocalTime getOrarioInizio() {
        return orarioInizio;
    }

    public void setOrarioInizio(LocalTime orarioInizio) {
        this.orarioInizio = orarioInizio;
    }

    public BigDecimal getCostoTotale() {
        return costoTotale;
    }

    public void setCostoTotale(BigDecimal costoTotale) {
        this.costoTotale = costoTotale;
    }

    public BigDecimal getCostoPerLezione() {
        return costoPerLezione;
    }

    public void setCostoPerLezione(BigDecimal costoPerLezione) {
        this.costoPerLezione = costoPerLezione;
    }

    public Insegnante getInsegnante() {
        return insegnante;
    }

    public void setInsegnante(Insegnante insegnante) {
        this.insegnante = insegnante;
    }

    public Set<Iscrizione> getIscrizioni() {
        return iscrizioni;
    }

    /** Studenti iscritti al corso, ricavati dalle iscrizioni. */
    public Set<Studente> getStudenti() {
        return iscrizioni.stream().map(Iscrizione::getStudente).collect(Collectors.toSet());
    }

    public Set<Lezione> getLezioni() {
        return lezioni;
    }

    public void setLezioni(Set<Lezione> lezioni) {
        this.lezioni = lezioni;
    }

    public String getAnnoScolastico() {
        return annoScolastico;
    }

    public void setAnnoScolastico(String annoScolastico) {
        this.annoScolastico = annoScolastico;
    }

    public boolean isAttivo() {
        return attivo;
    }

    public void setAttivo(boolean attivo) {
        this.attivo = attivo;
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
        if (!(o instanceof Corso other)) {
            return false;
        }
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Corso.class.hashCode();
    }
}
