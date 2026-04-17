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
    @JoinColumn(name = "docente_id")
    private Docente docente;

    @ManyToMany
    @JoinTable(
        name = "corso_studenti",
        joinColumns = @JoinColumn(name = "corso_id"),
        inverseJoinColumns = @JoinColumn(name = "studente_id")
    )
    private Set<Studente> studenti = new HashSet<>();

    @OneToMany(mappedBy = "corso", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Lezione> lezioni = new HashSet<>();

    @Column(name = "attivo")
    private boolean attivo = true;

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

    public Docente getDocente() {
        return docente;
    }

    public void setDocente(Docente docente) {
        this.docente = docente;
    }

    public Set<Studente> getStudenti() {
        return studenti;
    }

    public void setStudenti(Set<Studente> studenti) {
        this.studenti = studenti;
    }

    public Set<Lezione> getLezioni() {
        return lezioni;
    }

    public void setLezioni(Set<Lezione> lezioni) {
        this.lezioni = lezioni;
    }

    public boolean isAttivo() {
        return attivo;
    }

    public void setAttivo(boolean attivo) {
        this.attivo = attivo;
    }
}
