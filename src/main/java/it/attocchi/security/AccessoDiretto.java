package it.attocchi.security;

import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

/**
 * Accesso senza passare dal login, dopo che l'utente ha impostato la password dal link d'invito: autentica l'utente
 * nella sessione corrente (con un nuovo id di sessione) e pubblica l'{@link AuthenticationSuccessEvent}, così l'ultimo
 * accesso si registra come per il login normale. Gli account disattivati non entrano.
 */
@Component
public class AccessoDiretto {

    private final UserDetailsService userDetailsService;
    private final ApplicationEventPublisher eventi;
    private final HttpSessionSecurityContextRepository repository = new HttpSessionSecurityContextRepository();

    public AccessoDiretto(UserDetailsService userDetailsService, ApplicationEventPublisher eventi) {
        this.userDetailsService = userDetailsService;
        this.eventi = eventi;
    }

    /** {@code true} se l'utente è entrato; {@code false} se non c'è una richiesta HTTP o l'account non è attivo. */
    public boolean entra(String username) {
        VaadinServletRequest richiesta = VaadinServletRequest.getCurrent();
        VaadinServletResponse risposta = VaadinServletResponse.getCurrent();
        UserDetails utente = userDetailsService.loadUserByUsername(username);
        if (richiesta == null || risposta == null || !utente.isEnabled()) {
            return false;
        }
        HttpServletRequest http = richiesta.getHttpServletRequest();
        // Nuovo id di sessione dopo l'autenticazione (contro la session fixation), come fa il login
        if (http.getSession(false) != null) {
            http.changeSessionId();
        }
        var autenticazione = UsernamePasswordAuthenticationToken.authenticated(utente, null, utente.getAuthorities());
        SecurityContext contesto = SecurityContextHolder.createEmptyContext();
        contesto.setAuthentication(autenticazione);
        SecurityContextHolder.setContext(contesto);
        repository.saveContext(contesto, http, risposta.getHttpServletResponse());
        eventi.publishEvent(new AuthenticationSuccessEvent(autenticazione));
        return true;
    }
}
