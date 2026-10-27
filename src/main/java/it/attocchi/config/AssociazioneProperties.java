package it.attocchi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Dati dell'associazione riportati su ricevute e conferme di iscrizione ({@code app.associazione.*}); {@code luogo} è
 * la località di "Luogo e data" della dichiarazione delle spese.
 */
@ConfigurationProperties(prefix = "app.associazione")
public record AssociazioneProperties(String nome, String indirizzo, String codiceFiscale, String email, String iban,
                                     String luogo) {
}
