package it.attocchi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Un PDF aperto da un genitore o da un allievo adulto in Documenti (anteprima o scarico): solo metadati, con i nomi
 * senza chiavi esterne, così la cronologia resta anche se utenti, allievi o corsi vengono eliminati.
 */
@Entity
@Table(name = "aperture_documenti")
public class AperturaDocumento {

    /** Come è stato aperto il PDF. */
    public enum Modalita {
        ANTEPRIMA("Anteprima"),
        SCARICO("Scaricato");

        private final String descrizione;

        Modalita(String descrizione) {
            this.descrizione = descrizione;
        }

        public String getDescrizione() {
            return descrizione;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private LocalDateTime dataOra;

    @NotNull
    private String username;
    private String utenteNome;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 32)
    private TipoDocumento tipo;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 16)
    private Modalita modalita;

    /** L'allievo, o "Tutti i figli" per il riepilogo della famiglia. */
    private String allievoNome;
    /** Il corso, o la descrizione del documento che ne comprende più d'uno. */
    private String corsoNome;
    /** Anno scolastico, o anno solare per le attestazioni. */
    private String anno;
    private String nomeFile;

    public Long getId() { return id; }
    public LocalDateTime getDataOra() { return dataOra; }
    public void setDataOra(LocalDateTime dataOra) { this.dataOra = dataOra; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getUtenteNome() { return utenteNome; }
    public void setUtenteNome(String utenteNome) { this.utenteNome = utenteNome; }
    public TipoDocumento getTipo() { return tipo; }
    public void setTipo(TipoDocumento tipo) { this.tipo = tipo; }
    public Modalita getModalita() { return modalita; }
    public void setModalita(Modalita modalita) { this.modalita = modalita; }
    public String getAllievoNome() { return allievoNome; }
    public void setAllievoNome(String allievoNome) { this.allievoNome = allievoNome; }
    public String getCorsoNome() { return corsoNome; }
    public void setCorsoNome(String corsoNome) { this.corsoNome = corsoNome; }
    public String getAnno() { return anno; }
    public void setAnno(String anno) { this.anno = anno; }
    public String getNomeFile() { return nomeFile; }
    public void setNomeFile(String nomeFile) { this.nomeFile = nomeFile; }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AperturaDocumento other)) {
            return false;
        }
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return AperturaDocumento.class.hashCode();
    }
}
