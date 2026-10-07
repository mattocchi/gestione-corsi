package it.attocchi.security;

import com.vaadin.flow.spring.security.AuthenticationContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SecurityService {

    private final AuthenticationContext authenticationContext;

    public SecurityService(AuthenticationContext authenticationContext) {
        this.authenticationContext = authenticationContext;
    }

    public Optional<UserDetails> getAuthenticatedUser() {
        return authenticationContext.getAuthenticatedUser(UserDetails.class);
    }

    public Optional<String> getUsername() {
        return authenticationContext.getPrincipalName();
    }

    public boolean hasRole(String role) {
        return authenticationContext.hasRole(role);
    }

    public void logout() {
        authenticationContext.logout();
    }
}
