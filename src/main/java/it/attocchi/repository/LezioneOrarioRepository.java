package it.attocchi.repository;

import it.attocchi.entity.LezioneOrario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LezioneOrarioRepository extends JpaRepository<LezioneOrario, Long> {
    
    List<LezioneOrario> findByCorsoId(Long corsoId);
    
    void deleteByCorsoId(Long corsoId);
}
