package it.attocchi.security;

import it.attocchi.entity.User;
import it.attocchi.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Accesso con lo username oppure con l'email (senza distinguere maiuscole). Un'email vale solo se appartiene
     * a un unico account attivo: se è condivisa da più account serve lo username. Il nome dell'utente autenticato
     * è sempre lo username.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
            .or(() -> perEmail(username))
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        Set<GrantedAuthority> authorities = user.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .collect(Collectors.toSet());

        return new org.springframework.security.core.userdetails.User(
            user.getUsername(),
            user.getPassword(),
            user.isEnabled(),
            true,
            true,
            // Sospeso per inattività = "bloccato": al login lo dice solo con la password giusta (SecurityConfig)
            !user.isSospesoPerInattivita(),
            authorities
        );
    }

    private Optional<User> perEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        // Attivi, o sospesi per inattività (per dire come riattivarsi)
        List<User> account = userRepository.findByEmailIgnoreCase(email.strip()).stream()
            .filter(u -> u.isEnabled() || u.isSospesoPerInattivita()).toList();
        return account.size() == 1 ? Optional.of(account.getFirst()) : Optional.empty();
    }
}
