package it.attocchi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Registro di un invio email (riuscito, simulato o fallito): solo metadati, non il corpo. Corso e allievo sono
 * salvati come id e nome, senza chiavi esterne, così il registro resta anche se vengono eliminati.
 */
@Entity
@Table(name = "invii_email")
public class InvioEmail {

    /** Esito dell'invio. */
    public enum Esito {
        INVIATA("Inviata"),
        SIMULATA("Simulata"),
        ERRORE("Errore");

        private final String descrizione;

        Esito(String descrizione) {
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

    /** Testo e non enum nativo di MySQL: un nuovo tipo non richiede di modificare la colonna. */
    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 32)
    private TipoDocumento tipo;

    private Long corsoId;
    private String corsoNome;
    private Long allievoId;
    private String allievoNome;
    /** Riepilogo della famiglia: il genitore ({@code allievoId} è invece l'allievo adulto). */
    private Long genitoreId;
    /** Riepilogo: anno scolastico ({@code null} = corsi senza anno). */
    private String anno;

    @NotNull
    private String destinatario;
    private String destinatarioNome;

    private String oggetto;
    private String allegato;

    /** Username dell'utente che ha inviato. */
    private String inviatoDa;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Esito esito;

    @Column(length = 1000)
    private String errore;

    public Long getId() { return id; }
    public LocalDateTime getDataOra() { return dataOra; }
    public void setDataOra(LocalDateTime dataOra) { this.dataOra = dataOra; }
    public TipoDocumento getTipo() { return tipo; }
    public void setTipo(TipoDocumento tipo) { this.tipo = tipo; }
    public Long getCorsoId() { return corsoId; }
    public void setCorsoId(Long corsoId) { this.corsoId = corsoId; }
    public String getCorsoNome() { return corsoNome; }
    public void setCorsoNome(String corsoNome) { this.corsoNome = corsoNome; }
    public Long getAllievoId() { return allievoId; }
    public void setAllievoId(Long allievoId) { this.allievoId = allievoId; }
    public String getAllievoNome() { return allievoNome; }
    public void setAllievoNome(String allievoNome) { this.allievoNome = allievoNome; }
    public Long getGenitoreId() { return genitoreId; }
    public void setGenitoreId(Long genitoreId) { this.genitoreId = genitoreId; }
    public String getAnno() { return anno; }
    public void setAnno(String anno) { this.anno = anno; }
    public String getDestinatario() { return destinatario; }
    public void setDestinatario(String destinatario) { this.destinatario = destinatario; }
    public String getDestinatarioNome() { return destinatarioNome; }
    public void setDestinatarioNome(String destinatarioNome) { this.destinatarioNome = destinatarioNome; }
    public String getOggetto() { return oggetto; }
    public void setOggetto(String oggetto) { this.oggetto = oggetto; }
    public String getAllegato() { return allegato; }
    public void setAllegato(String allegato) { this.allegato = allegato; }
    public String getInviatoDa() { return inviatoDa; }
    public void setInviatoDa(String inviatoDa) { this.inviatoDa = inviatoDa; }
    public Esito getEsito() { return esito; }
    public void setEsito(Esito esito) { this.esito = esito; }
    public String getErrore() { return errore; }
    public void setErrore(String errore) { this.errore = errore; }

    /**
     * Uguaglianza basata sull'id: due istanze transient (id null) sono diverse.
     * hashCode costante per classe: resta stabile quando l'id viene assegnato al salvataggio.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof InvioEmail other)) {
            return false;
        }
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return InvioEmail.class.hashCode();
    }
}
