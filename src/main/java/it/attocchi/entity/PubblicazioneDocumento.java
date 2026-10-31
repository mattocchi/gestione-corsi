package it.attocchi.entity;

import jakarta.persistence.*;

/**
 * Se un tipo di documento è visibile alle famiglie (genitori e allievi adulti), deciso da admin e segreteria in
 * Impostazioni › Documenti. Oltre all'interruttore valgono le regole del tipo ({@code PubblicazioneDocumentiService}).
 */
@Entity
@Table(name = "pubblicazione_documenti")
public class PubblicazioneDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 32)
    private TipoDocumento tipo;

    @Column(nullable = false)
    private boolean visibileFamiglie;

    public PubblicazioneDocumento() {
    }

    public PubblicazioneDocumento(TipoDocumento tipo, boolean visibileFamiglie) {
        this.tipo = tipo;
        this.visibileFamiglie = visibileFamiglie;
    }

    public Long getId() {
        return id;
    }

    public TipoDocumento getTipo() {
        return tipo;
    }

    public boolean isVisibileFamiglie() {
        return visibileFamiglie;
    }

    public void setVisibileFamiglie(boolean visibileFamiglie) {
        this.visibileFamiglie = visibileFamiglie;
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
        if (!(o instanceof PubblicazioneDocumento altro)) {
            return false;
        }
        return id != null && id.equals(altro.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
