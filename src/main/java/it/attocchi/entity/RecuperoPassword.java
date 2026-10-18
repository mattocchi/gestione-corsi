package it.attocchi.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Link di recupero password ancora utilizzabile: del token si conserva solo l'hash SHA-256. Il record viene
 * cancellato quando il link è usato o quando l'utente ne chiede uno nuovo.
 */
@Entity
@Table(name = "recupero_password")
public class RecuperoPassword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private LocalDateTime scadenza;

    public RecuperoPassword() {
    }

    public RecuperoPassword(User user, String tokenHash, LocalDateTime scadenza) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.scadenza = scadenza;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public LocalDateTime getScadenza() {
        return scadenza;
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
        if (!(o instanceof RecuperoPassword other)) {
            return false;
        }
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return RecuperoPassword.class.hashCode();
    }
}
