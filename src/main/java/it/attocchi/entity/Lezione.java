package it.attocchi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.BatchSize;

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

    /**
     * Data del calendario del corso di una lezione spostata a mano; {@code null} se non è stata spostata. Decide
     * partecipanti e numero e impedisce a "Genera Lezioni" di ricrearla in quella data.
     */
    @Column(name = "data_originale")
    private LocalDate dataOriginale;

    private LocalTime orarioInizio;

    private LocalTime orarioFine;

    private Integer numeroLezione;

    private String note;

    @OneToMany(mappedBy = "lezione", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Presenza> presenze = new HashSet<>();

    /**
     * Insegnanti presenti alla lezione: nasce con quelli del corso; le lezioni svolte li conservano anche se il corso
     * cambia insegnanti. Serve ai costi (ognuno matura le lezioni a cui era presente).
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "lezione_insegnanti", joinColumns = @JoinColumn(name = "lezione_id"),
        inverseJoinColumns = @JoinColumn(name = "insegnante_id"))
    @BatchSize(size = 50)
    private Set<Insegnante> insegnantiPresenti = new HashSet<>();

    @Column(name = "completata")
    private boolean completata = false;

    /** Lezione programmata che non si è svolta (es. insegnante assente): le sue presenze non contano. */
    @Column(name = "non_effettuata", nullable = false)
    private boolean nonEffettuata = false;

    private String motivoNonEffettuata;

    /** Lezione inserita a mano (recupero o lezione in più), non generata dal calendario del corso. */
    @Column(nullable = false)
    private boolean aggiunta = false;

    /** Lezione non effettuata di cui questa è il recupero; {@code null} se non è un recupero. */
    @ManyToOne
    @JoinColumn(name = "recupero_di_id")
    private Lezione recuperoDi;

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

    public LocalDate getDataOriginale() {
        return dataOriginale;
    }

    public void setDataOriginale(LocalDate dataOriginale) {
        this.dataOriginale = dataOriginale;
    }

    /** Spostata a mano dalla data del calendario del corso. */
    public boolean isSpostata() {
        return dataOriginale != null;
    }

    /** Data che la lezione ha nel calendario del corso: quella originale se è stata spostata. */
    public LocalDate getDataDelCalendario() {
        return dataOriginale != null ? dataOriginale : data;
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

    public boolean isNonEffettuata() {
        return nonEffettuata;
    }

    public void setNonEffettuata(boolean nonEffettuata) {
        this.nonEffettuata = nonEffettuata;
    }

    public String getMotivoNonEffettuata() {
        return motivoNonEffettuata;
    }

    public void setMotivoNonEffettuata(String motivoNonEffettuata) {
        this.motivoNonEffettuata = motivoNonEffettuata;
    }

    public boolean isAggiunta() {
        return aggiunta;
    }

    public void setAggiunta(boolean aggiunta) {
        this.aggiunta = aggiunta;
    }

    public Lezione getRecuperoDi() {
        return recuperoDi;
    }

    public void setRecuperoDi(Lezione recuperoDi) {
        this.recuperoDi = recuperoDi;
    }

    /** Ancora da svolgere: né completata né non effettuata. */
    public Set<Insegnante> getInsegnantiPresenti() {
        return insegnantiPresenti;
    }

    public void setInsegnantiPresenti(Set<Insegnante> insegnantiPresenti) {
        this.insegnantiPresenti = insegnantiPresenti;
    }

    /** {@code true} se l'insegnante era presente alla lezione. */
    public boolean conInsegnante(Long insegnanteId) {
        return insegnantiPresenti.stream().anyMatch(i -> i.getId().equals(insegnanteId));
    }

    public boolean isDaFare() {
        return !completata && !nonEffettuata;
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
        if (!(o instanceof Lezione other)) {
            return false;
        }
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Lezione.class.hashCode();
    }
}
