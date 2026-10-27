package it.attocchi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Lettera di incarico: l'associazione incarica un insegnante di svolgere un corso (o un servizio) nell'anno scolastico
 * per il compenso concordato (tariffa oraria, importo una tantum o entrambi). I dati sono fissati all'emissione: non
 * cambiano se poi cambiano la tariffa dell'insegnante o il corso. Bilancio e rimborsi restano sui dati dell'insegnante.
 */
@Entity
@Table(name = "lettere_incarico")
public class LetteraIncarico {

    public static final int LUNGHEZZA_NOTA = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "insegnante_id")
    private Insegnante insegnante;

    @NotNull
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "corso_id")
    private Corso corso;

    /** Anno scolastico di validità, copiato dal corso all'emissione. */
    @Column(name = "anno_scolastico", length = 9)
    private String annoScolastico;

    @NotNull
    @Column(name = "data_emissione", nullable = false)
    private LocalDate dataEmissione;

    /** Euro all'ora; facoltativa se c'è l'importo una tantum. */
    @Column(name = "tariffa_oraria", precision = 10, scale = 2)
    private BigDecimal tariffaOraria;

    /** Importo una tantum per l'anno; facoltativo se c'è la tariffa oraria. */
    @Column(name = "importo_una_tantum", precision = 10, scale = 2)
    private BigDecimal importoUnaTantum;

    /** Giorni e orari dell'incarico, es. "martedì e sabato alle 18:00, lezioni da 40 minuti". */
    @Column(name = "giorni_e_orari", length = 500)
    private String giorniEOrari;

    @Column(length = LUNGHEZZA_NOTA)
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

    public Corso getCorso() {
        return corso;
    }

    public void setCorso(Corso corso) {
        this.corso = corso;
    }

    public String getAnnoScolastico() {
        return annoScolastico;
    }

    public void setAnnoScolastico(String annoScolastico) {
        this.annoScolastico = annoScolastico;
    }

    public LocalDate getDataEmissione() {
        return dataEmissione;
    }

    public void setDataEmissione(LocalDate dataEmissione) {
        this.dataEmissione = dataEmissione;
    }

    public BigDecimal getTariffaOraria() {
        return tariffaOraria;
    }

    public void setTariffaOraria(BigDecimal tariffaOraria) {
        this.tariffaOraria = tariffaOraria;
    }

    public BigDecimal getImportoUnaTantum() {
        return importoUnaTantum;
    }

    public void setImportoUnaTantum(BigDecimal importoUnaTantum) {
        this.importoUnaTantum = importoUnaTantum;
    }

    public String getGiorniEOrari() {
        return giorniEOrari;
    }

    public void setGiorniEOrari(String giorniEOrari) {
        this.giorniEOrari = giorniEOrari;
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
        if (!(o instanceof LetteraIncarico other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return LetteraIncarico.class.hashCode();
    }
}
