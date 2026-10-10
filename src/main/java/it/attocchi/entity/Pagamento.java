package it.attocchi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Rata pagata da un partecipante per la quota di un corso.
 */
@Entity
@Table(name = "pagamenti")
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumns({
        @JoinColumn(name = "corso_id", referencedColumnName = "corso_id"),
        @JoinColumn(name = "studente_id", referencedColumnName = "studente_id")
    })
    private Iscrizione iscrizione;

    @NotNull
    private LocalDate data;

    @NotNull
    @Column(precision = 10, scale = 2)
    private BigDecimal importo;

    @NotNull
    @Enumerated(EnumType.STRING)
    private MetodoPagamento metodo = MetodoPagamento.BONIFICO;

    private String nota;

    // Costruttori
    public Pagamento() {
    }

    public Pagamento(Iscrizione iscrizione) {
        this.iscrizione = iscrizione;
        this.data = LocalDate.now();
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public Iscrizione getIscrizione() {
        return iscrizione;
    }

    public void setIscrizione(Iscrizione iscrizione) {
        this.iscrizione = iscrizione;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public BigDecimal getImporto() {
        return importo;
    }

    public void setImporto(BigDecimal importo) {
        this.importo = importo;
    }

    public MetodoPagamento getMetodo() {
        return metodo;
    }

    public void setMetodo(MetodoPagamento metodo) {
        this.metodo = metodo;
    }

    public String getNota() {
        return nota;
    }

    public void setNota(String nota) {
        this.nota = nota;
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
        if (!(o instanceof Pagamento other)) {
            return false;
        }
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Pagamento.class.hashCode();
    }
}
