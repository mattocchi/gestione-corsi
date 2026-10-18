package it.attocchi.repository;

import it.attocchi.entity.RecuperoPassword;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RecuperoPasswordRepository extends JpaRepository<RecuperoPassword, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<RecuperoPassword> findByTokenHash(String tokenHash);

    @Modifying
    @Query("delete from RecuperoPassword r where r.user.id = :userId")
    void deleteByUserId(Long userId);
}
