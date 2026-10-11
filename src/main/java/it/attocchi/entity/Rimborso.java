package it.attocchi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Rimborso spese pagato a un insegnante, di solito mensile: il mese di riferimento indica a quale mese si riferisce.
 */
@Entity
@Table(name = "rimborsi")
public class Rimborso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "insegnante_id")
    private Insegnante insegnante;

    /** Data del pagamento. */
    @NotNull
    private LocalDate data;

    /** Mese di riferimento, salvato come primo giorno del mese. */
    @NotNull
    @Column(name = "mese_riferimento")
    private LocalDate meseRiferimento;

    @NotNull
    @Column(precision = 10, scale = 2)
    private BigDecimal importo;

    @NotNull
    @Enumerated(EnumType.STRING)
    private MetodoPagamento metodo = MetodoPagamento.BONIFICO;

    private String nota;

    public Long getId() {
        return id;
    }

    public Insegnante getInsegnante() {
        return insegnante;
    }

    public void setInsegnante(Insegnante insegnante) {
        this.insegnante = insegnante;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public YearMonth getMese() {
        return meseRiferimento != null ? YearMonth.from(meseRiferimento) : null;
    }

    public void setMese(YearMonth mese) {
        this.meseRiferimento = mese != null ? mese.atDay(1) : null;
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
     * Uguaglianza basata sull'id: due istanze transient (id null) sono diverse.
     * hashCode costante per classe: resta stabile quando l'id viene assegnato al salvataggio.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Rimborso other)) {
            return false;
        }
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Rimborso.class.hashCode();
    }
}
