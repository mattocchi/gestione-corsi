package it.attocchi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

/**
 * Luogo in cui si svolgono corsi e lezioni (Aula 1, Palestra, Esterno…), gestito da admin e segreteria. Un luogo
 * non attivo non si può più scegliere, ma resta su corsi e lezioni che lo hanno già.
 */
@Entity
@Table(name = "luoghi")
public class Luogo {

    public static final int LUNGHEZZA_NOME = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true, length = LUNGHEZZA_NOME)
    private String nome;

    @Column(nullable = false)
    private boolean attivo = true;

    public Luogo() {
    }

    public Luogo(String nome) {
        this.nome = nome;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public boolean isAttivo() {
        return attivo;
    }

    public void setAttivo(boolean attivo) {
        this.attivo = attivo;
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
        if (!(o instanceof Luogo other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Luogo.class.hashCode();
    }
}
