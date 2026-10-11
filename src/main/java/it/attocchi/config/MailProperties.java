package it.attocchi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Server SMTP per l'invio delle email ({@code app.mail.*}, da variabili d'ambiente MAIL_*):
 * senza {@code host} l'invio è simulato.
 */
@ConfigurationProperties(prefix = "app.mail")
public record MailProperties(String host, int port, String username, String password, String from, boolean starttls) {

    public boolean configurato() {
        return host != null && !host.isBlank();
    }
}
