package it.attocchi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

/**
 * Chiave dell'iscrizione: la coppia (corso, allievo), come la chiave primaria della tabella
 * {@code corso_allievi}.
 */
@Embeddable
public class IscrizioneId implements Serializable {

    @Column(name = "corso_id")
    private Long corsoId;

    @Column(name = "allievo_id")
    private Long allievoId;

    public IscrizioneId() {
    }

    public IscrizioneId(Long corsoId, Long allievoId) {
        this.corsoId = corsoId;
        this.allievoId = allievoId;
    }

    public Long getCorsoId() {
        return corsoId;
    }

    public Long getAllievoId() {
        return allievoId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IscrizioneId other)) {
            return false;
        }
        return Objects.equals(corsoId, other.corsoId) && Objects.equals(allievoId, other.allievoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(corsoId, allievoId);
    }
}
