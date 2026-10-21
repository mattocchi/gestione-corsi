package it.attocchi.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

    /** Caricati sempre: servono a viste e documenti per i giorni dei partecipanti ({@link Iscrizione#giorniEffettivi()}). */
    @ElementCollection(fetch = FetchType.EAGER)
    @BatchSize(size = 50)
    @CollectionTable(name = "corso_giorni_settimana", joinColumns = @JoinColumn(name = "corso_id"))
    @Column(name = "giorno_settimana")
    @Enumerated(EnumType.STRING)
    private Set<DayOfWeek> giorniSettimana = new HashSet<>();

    private LocalTime orarioInizio;

    /** Settimane tra una lezione e la successiva nello stesso giorno: 1 (ogni settimana) o 2; null = ogni settimana. */
    @Column(name = "cadenza_settimane")
    private Integer cadenzaSettimane;

    private BigDecimal costoTotale;

    /**
     * Prezzo con pre-iscrizione, assegnato a mano (di solito il costo totale scontato del 15%):
     * solo informativo, la quota dovuta resta il costo totale o la quota personalizzata.
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal prezzoPreiscrizione;

    private BigDecimal costoPerLezione;

    @ManyToOne
    @JoinColumn(name = "insegnante_id")
    private Insegnante insegnante;

    /** Secondo insegnante facoltativo, con gli stessi diritti del titolare. */
    @ManyToOne
    @JoinColumn(name = "secondo_insegnante_id")
    private Insegnante secondoInsegnante;

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

    public int getCadenzaSettimane() {
        return cadenzaSettimane != null ? cadenzaSettimane : 1;
    }

    public void setCadenzaSettimane(int cadenzaSettimane) {
        this.cadenzaSettimane = cadenzaSettimane;
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

    public BigDecimal getPrezzoPreiscrizione() {
        return prezzoPreiscrizione;
    }

    public void setPrezzoPreiscrizione(BigDecimal prezzoPreiscrizione) {
        this.prezzoPreiscrizione = prezzoPreiscrizione;
    }

    /** Valore tipico del prezzo con pre-iscrizione: costo totale scontato del 15%. */
    public static BigDecimal prezzoPreiscrizioneTipico(BigDecimal costoTotale) {
        return costoTotale == null ? null : costoTotale.multiply(new BigDecimal("0.85")).setScale(2, RoundingMode.HALF_UP);
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

    public Insegnante getSecondoInsegnante() {
        return secondoInsegnante;
    }

    public void setSecondoInsegnante(Insegnante secondoInsegnante) {
        this.secondoInsegnante = secondoInsegnante;
    }

    /** Titolare e secondo insegnante, in quest'ordine, senza quelli non assegnati. */
    public List<Insegnante> getInsegnanti() {
        List<Insegnante> insegnanti = new ArrayList<>();
        if (insegnante != null) {
            insegnanti.add(insegnante);
        }
        if (secondoInsegnante != null && !secondoInsegnante.equals(insegnante)) {
            insegnanti.add(secondoInsegnante);
        }
        return insegnanti;
    }

    /** Nomi degli insegnanti separati da virgola, es. "Maria Verdi, Luca Bianchi" (vuoto se nessuno). */
    public String getNomiInsegnanti() {
        return String.join(", ", getInsegnanti().stream().map(Insegnante::getNomeCompleto).toList());
    }

    /** {@code true} se l'utente è il titolare o il secondo insegnante del corso. */
    public boolean haInsegnante(String username) {
        return username != null && getInsegnanti().stream()
            .anyMatch(i -> i.getUser() != null && username.equals(i.getUser().getUsername()));
    }

    public Set<Iscrizione> getIscrizioni() {
        return iscrizioni;
    }

    /** Allievi iscritti al corso, ricavati dalle iscrizioni. */
    public Set<Allievo> getAllievi() {
        return iscrizioni.stream().map(Iscrizione::getAllievo).collect(Collectors.toSet());
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
