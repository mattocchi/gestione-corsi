package it.attocchi.entity;

import jakarta.persistence.*;

/** Anno solare le cui attestazioni delle spese sono pubblicate alle famiglie (sempre un anno già finito). */
@Entity
@Table(name = "attestazioni_pubblicate")
public class AttestazioniPubblicate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private int anno;

    public AttestazioniPubblicate() {
    }

    public AttestazioniPubblicate(int anno) {
        this.anno = anno;
    }

    public Long getId() {
        return id;
    }

    public int getAnno() {
        return anno;
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
        if (!(o instanceof AttestazioniPubblicate altro)) {
            return false;
        }
        return id != null && id.equals(altro.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
