package it.attocchi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Comparator;

/**
 * Gruppo di un corso: un giorno della settimana (tra quelli del corso) con un orario di riferimento e un nome
 * facoltativo. Un partecipante può appartenere a un gruppo ({@link Iscrizione#getGruppo()}) e allora frequenta solo
 * quel giorno; senza gruppo frequenta tutti i giorni del corso.
 */
@Entity
@Table(name = "corso_gruppi")
public class GruppoCorso {

    /** Ordine dei gruppi: per giorno, orario e nome. */
    public static final Comparator<GruppoCorso> PER_GIORNO = Comparator
        .comparing(GruppoCorso::getGiorno)
        .thenComparing(GruppoCorso::getOrario, Comparator.nullsLast(Comparator.naturalOrder()))
        .thenComparing(g -> g.getNome() != null ? g.getNome() : "");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "corso_id")
    private Corso corso;

    /** Nome facoltativo (es. "Principianti"). */
    private String nome;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "giorno_settimana")
    private DayOfWeek giorno;

    /** Orario di riferimento, proposto ai partecipanti assegnati al gruppo. */
    private LocalTime orario;

    public GruppoCorso() {
    }

    public GruppoCorso(Corso corso, String nome, DayOfWeek giorno, LocalTime orario) {
        this.corso = corso;
        this.nome = nome;
        this.giorno = giorno;
        this.orario = orario;
    }

    public Long getId() {
        return id;
    }

    public Corso getCorso() {
        return corso;
    }

    public void setCorso(Corso corso) {
        this.corso = corso;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public DayOfWeek getGiorno() {
        return giorno;
    }

    public void setGiorno(DayOfWeek giorno) {
        this.giorno = giorno;
    }

    public LocalTime getOrario() {
        return orario;
    }

    public void setOrario(LocalTime orario) {
        this.orario = orario;
    }

    /**
     * Uguaglianza sull'id: due gruppi non ancora salvati sono uguali solo se sono la stessa istanza;
     * hashCode costante per classe: resta stabile quando l'id viene assegnato al salvataggio.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GruppoCorso other)) {
            return false;
        }
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return GruppoCorso.class.hashCode();
    }
}
