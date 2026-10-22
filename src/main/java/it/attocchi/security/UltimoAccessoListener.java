package it.attocchi.security;

import it.attocchi.service.UserService;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/** Registra l'ultimo accesso a ogni login riuscito (il nome autenticato è sempre lo username). */
@Component
public class UltimoAccessoListener {

    private final UserService userService;

    public UltimoAccessoListener(UserService userService) {
        this.userService = userService;
    }

    @EventListener
    public void accessoRiuscito(AuthenticationSuccessEvent event) {
        userService.registraAccesso(event.getAuthentication().getName());
    }
}
