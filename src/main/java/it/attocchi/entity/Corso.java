package it.attocchi.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
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

    /** Corso o servizio; null (corsi creati prima del tipo) = corso. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TipoCorso tipo;

    private String descrizione;

    @NotNull
    private LocalDate dataInizio;

    /**
     * Lezioni per partecipante; facoltativo se c'è la data di fine (almeno uno dei due, {@code CorsoService.save}).
     * Con entrambi le lezioni si fermano al primo limite raggiunto.
     */
    private Integer numeroLezioni;

    /** Ultimo giorno in cui generare lezioni (compreso); facoltativo se c'è il numero di lezioni. */
    @Column(name = "data_fine")
    private LocalDate dataFine;

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

    /** Luogo in cui si svolgono di solito le lezioni (facoltativo); una lezione può averne un altro. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "luogo_id")
    private Luogo luogo;

    /** Luoghi proposti per primi sulle lezioni del corso (comprende quello predefinito). Caricati sempre. */
    @ManyToMany(fetch = FetchType.EAGER)
    @BatchSize(size = 50)
    @JoinTable(name = "corso_luoghi", joinColumns = @JoinColumn(name = "corso_id"),
        inverseJoinColumns = @JoinColumn(name = "luogo_id"))
    private Set<Luogo> luoghiAbilitati = new HashSet<>();

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

    /** Massimo numero di insegnanti facoltativi oltre al titolare. */
    public static final int MAX_FACOLTATIVI = 3;

    /**
     * Insegnanti facoltativi (fino a {@link #MAX_FACOLTATIVI}), con gli stessi diritti del titolare, nell'ordine scelto.
     * Caricati sempre: servono a ogni controllo dei permessi ({@link #haInsegnante}).
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SELECT)
    @BatchSize(size = 50)
    @OrderColumn(name = "ordine")
    @JoinTable(name = "corso_insegnanti", joinColumns = @JoinColumn(name = "corso_id"),
        inverseJoinColumns = @JoinColumn(name = "insegnante_id"))
    private List<Insegnante> insegnantiFacoltativi = new ArrayList<>();

    // Solo REMOVE: salvare un corso dal dialog non deve sovrascrivere le iscrizioni,
    // che si gestiscono con i metodi dedicati di CorsoService
    @OneToMany(mappedBy = "corso", cascade = CascadeType.REMOVE)
    private Set<Iscrizione> iscrizioni = new HashSet<>();

    @OneToMany(mappedBy = "corso", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Lezione> lezioni = new HashSet<>();

    @Column(name = "attivo")
    private boolean attivo = true;

    /**
     * Visibile a genitori e allievi; un corso non pubblicato è in preparazione (lo vedono staff e insegnanti). Diverso
     * da {@link #attivo} (archivio). null (corsi creati prima della pubblicazione) = pubblicato; i nuovi nascono no.
     */
    @Column(name = "pubblicato")
    private Boolean pubblicato = false;

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

    public LocalDate getDataFine() {
        return dataFine;
    }

    public void setDataFine(LocalDate dataFine) {
        this.dataFine = dataFine;
    }

    /**
     * Lezioni per partecipante stimate: il numero di lezioni se c'è, altrimenti le date dei giorni del corso (con la
     * cadenza) dalla data di inizio alla data di fine, senza contare le festività.
     */
    public int getLezioniPreviste() {
        if (numeroLezioni != null) {
            return numeroLezioni;
        }
        if (dataInizio == null || dataFine == null || giorniSettimana.isEmpty()) {
            return 0;
        }
        int lezioni = 0;
        for (DayOfWeek giorno : giorniSettimana) {
            LocalDate data = dataInizio.with(TemporalAdjusters.nextOrSame(giorno));
            for (; !data.isAfter(dataFine); data = data.plusWeeks(getCadenzaSettimane())) {
                lezioni++;
            }
        }
        return lezioni;
    }

    public Integer getNumeroLezioni() {
        return numeroLezioni;
    }

    public void setNumeroLezioni(Integer numeroLezioni) {
        this.numeroLezioni = numeroLezioni;
    }

    public Luogo getLuogo() {
        return luogo;
    }

    public void setLuogo(Luogo luogo) {
        this.luogo = luogo;
    }

    public Set<Luogo> getLuoghiAbilitati() {
        return luoghiAbilitati;
    }

    public void setLuoghiAbilitati(Set<Luogo> luoghiAbilitati) {
        this.luoghiAbilitati = new HashSet<>(luoghiAbilitati);
    }

    /** Nome del luogo predefinito, o {@code null}. */
    public String getNomeLuogo() {
        return luogo != null ? luogo.getNome() : null;
    }

    public TipoCorso getTipo() {
        return tipo != null ? tipo : TipoCorso.CORSO;
    }

    public void setTipo(TipoCorso tipo) {
        this.tipo = tipo;
    }

    public boolean isServizio() {
        return getTipo() == TipoCorso.SERVIZIO;
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

    public List<Insegnante> getInsegnantiFacoltativi() {
        return insegnantiFacoltativi;
    }

    public void setInsegnantiFacoltativi(List<Insegnante> insegnantiFacoltativi) {
        this.insegnantiFacoltativi = new ArrayList<>(insegnantiFacoltativi);
    }

    /** Titolare e insegnanti facoltativi, in quest'ordine, senza doppioni. */
    public List<Insegnante> getInsegnanti() {
        List<Insegnante> insegnanti = new ArrayList<>();
        if (insegnante != null) {
            insegnanti.add(insegnante);
        }
        for (Insegnante facoltativo : insegnantiFacoltativi) {
            if (facoltativo != null && !insegnanti.contains(facoltativo)) {
                insegnanti.add(facoltativo);
            }
        }
        return insegnanti;
    }

    /** Nomi degli insegnanti separati da virgola, es. "Maria Verdi, Luca Bianchi" (vuoto se nessuno). */
    public String getNomiInsegnanti() {
        return String.join(", ", getInsegnanti().stream().map(Insegnante::getNomeCompleto).toList());
    }

    /** {@code true} se l'utente è il titolare o uno degli insegnanti facoltativi del corso. */
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

    public boolean isPubblicato() {
        return pubblicato == null || pubblicato;
    }

    public void setPubblicato(boolean pubblicato) {
        this.pubblicato = pubblicato;
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
