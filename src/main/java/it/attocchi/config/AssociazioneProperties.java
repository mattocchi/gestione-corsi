package it.attocchi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Dati dell'associazione riportati su ricevute e conferme di iscrizione ({@code app.associazione.*}). */
@ConfigurationProperties(prefix = "app.associazione")
public record AssociazioneProperties(String nome, String indirizzo, String codiceFiscale, String email, String iban) {
}
