package it.attocchi.repository;

import it.attocchi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    List<User> findByEmailIgnoreCase(String email);
    List<User> findByEmailIgnoreCaseAndEnabledTrue(String email);

    /** Account attivi con l'ultimo accesso prima del limite (chi non è mai entrato non c'è). */
    List<User> findByEnabledTrueAndUltimoAccessoBefore(java.time.LocalDateTime limite);
}
