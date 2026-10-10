package it.attocchi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

/**
 * Chiave dell'iscrizione: la coppia (corso, studente), come la chiave primaria della tabella
 * {@code corso_studenti}.
 */
@Embeddable
public class IscrizioneId implements Serializable {

    @Column(name = "corso_id")
    private Long corsoId;

    @Column(name = "studente_id")
    private Long studenteId;

    public IscrizioneId() {
    }

    public IscrizioneId(Long corsoId, Long studenteId) {
        this.corsoId = corsoId;
        this.studenteId = studenteId;
    }

    public Long getCorsoId() {
        return corsoId;
    }

    public Long getStudenteId() {
        return studenteId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IscrizioneId other)) {
            return false;
        }
        return Objects.equals(corsoId, other.corsoId) && Objects.equals(studenteId, other.studenteId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(corsoId, studenteId);
    }
}
