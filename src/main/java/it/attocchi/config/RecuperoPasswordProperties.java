package it.attocchi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Recupero password: {@code url} è l'indirizzo pubblico dell'applicazione usato nel link dell'email ({@code app.url},
 * variabile APP_URL), mai ricavato dalla richiesta; {@code validitaOre} la durata del link
 * ({@code app.password-reset.validita-ore}, variabile PASSWORD_RESET_VALIDITA_ORE).
 */
@ConfigurationProperties(prefix = "app")
public record RecuperoPasswordProperties(String url, PasswordReset passwordReset) {

    public record PasswordReset(int validitaOre) {
    }

    /** Ore di validità del link. */
    public int validitaOre() {
        return passwordReset.validitaOre();
    }

    /** Indirizzo della pagina di una nuova password, senza la barra finale dell'url di base. */
    public String linkNuovaPassword(String token) {
        String base = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        return base + "/nuova-password?token=" + token;
    }
}
