package it.attocchi.repository;

import it.attocchi.entity.Iscrizione;
import it.attocchi.entity.IscrizioneId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IscrizioneRepository extends JpaRepository<Iscrizione, IscrizioneId> {
}
