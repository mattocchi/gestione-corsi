package it.attocchi.service;

import it.attocchi.entity.User;
import it.attocchi.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void allineaUsername_soloGeneratiNonPiuAttuali() {
        User mario = nuovo("mario.rossi", "Mario", "Rossi");
        User rinominato = nuovo("anna.bianchi", "Anna", "Verdi Neri");
        User conNumero = nuovo("luca.blu2", "Luca", "Blu");
        try {
            // Gli username scelti a mano (admin, genitore1, allievo_canto1) e quelli già allineati non compaiono
            List<UserService.UsernameDaAllineare> proposte = userService.usernameDaAllineare();
            assertEquals(List.of("anna.bianchi"), proposte.stream().map(UserService.UsernameDaAllineare::attuale).toList());
            assertEquals("anna.verdineri", proposte.getFirst().proposto());
            assertEquals(Optional.empty(), userService.proposta(mario));
            assertEquals(Optional.empty(), userService.proposta(conNumero));

            // Il nuovo username è libero: se è già usato prende un numero
            User occupato = nuovo("anna.verdineri", "Anna", "Verdineri");
            try {
                assertThrows(IllegalStateException.class, () -> userService.allineaUsername(List.of(rinominato.getId()), "segreteria"));
                Map<String, String> cambiati = userService.allineaUsername(List.of(rinominato.getId(), mario.getId()), "admin");
                assertEquals(Map.of("anna.bianchi", "anna.verdineri2"), cambiati);
                assertEquals("anna.verdineri2", userRepository.findById(rinominato.getId()).orElseThrow().getUsername());
                assertTrue(userService.usernameDaAllineare().isEmpty());
            } finally {
                userRepository.delete(occupato);
            }
        } finally {
            userRepository.deleteAll(List.of(mario, userRepository.findById(rinominato.getId()).orElseThrow(), conNumero));
        }
    }

    private User nuovo(String username, String nome, String cognome) {
        User user = new User(username, "x", nome, cognome, null);
        return userRepository.save(user);
    }
}
