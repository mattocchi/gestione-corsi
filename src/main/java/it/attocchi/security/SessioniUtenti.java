package it.attocchi.security;

import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * Sessioni aperte degli utenti (dal {@link SessionRegistry} di {@link SecurityConfig}): quando cambia lo username di un
 * utente le sue sessioni si chiudono, perché il loro principal ha ancora quello vecchio; alla richiesta successiva si
 * torna all'accesso.
 */
@Component
public class SessioniUtenti {

    private final SessionRegistry sessionRegistry;

    public SessioniUtenti(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    /** Chiude ogni sessione degli utenti con questi username; restituisce quante. */
    public int chiudi(Collection<String> usernames) {
        int chiuse = 0;
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            if (principal instanceof UserDetails utente && usernames.contains(utente.getUsername())) {
                for (SessionInformation sessione : sessionRegistry.getAllSessions(principal, false)) {
                    sessione.expireNow();
                    chiuse++;
                }
            }
        }
        return chiuse;
    }
}
