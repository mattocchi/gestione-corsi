package it.attocchi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Anno scolastico scelto nei corsi, nel formato "2026/2027" (da settembre ad agosto). L'elenco è gestito
 * da admin e segreteria; {@link Corso#getAnnoScolastico()} ne conserva il nome.
 */
@Entity
@Table(name = "anni_scolastici")
public class AnnoScolastico {

    private static final Pattern FORMATO = Pattern.compile("(\\d{4})/(\\d{4})");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true, length = 9)
    private String nome;

    /** Solo gli anni attivi si possono scegliere per un corso. */
    @Column(nullable = false)
    private boolean attivo = true;

    public AnnoScolastico() {
    }

    public AnnoScolastico(String nome) {
        this.nome = nome;
    }

    /** Anno scolastico a cui appartiene la data (da settembre è già l'anno successivo). */
    public static String di(LocalDate data) {
        int inizio = data.getMonthValue() >= 9 ? data.getYear() : data.getYear() - 1;
        return inizio + "/" + (inizio + 1);
    }

    /** {@code true} se il nome è nel formato "AAAA/AAAA" con due anni consecutivi. */
    public static boolean valido(String nome) {
        if (nome == null) {
            return false;
        }
        Matcher m = FORMATO.matcher(nome);
        return m.matches() && Integer.parseInt(m.group(2)) == Integer.parseInt(m.group(1)) + 1;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
        if (!(o instanceof AnnoScolastico other)) {
            return false;
        }
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return AnnoScolastico.class.hashCode();
    }
}
