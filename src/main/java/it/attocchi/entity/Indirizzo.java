package it.attocchi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Indirizzo di residenza in formato italiano. Per allievi e genitori riusa le colonne esistenti
 * {@code indirizzo} (via e civico), {@code cap} e {@code citta} (Comune); {@code provincia} è nuova.
 */
@Embeddable
public class Indirizzo {

    /** Via e numero civico. */
    @Column(name = "indirizzo")
    private String via;

    @Column(name = "cap", length = 5)
    private String cap;

    @Column(name = "citta")
    private String comune;

    /** Sigla della provincia, es. "MI". */
    @Column(name = "provincia", length = 2)
    private String provincia;

    public Indirizzo() {
    }

    public Indirizzo(String via, String cap, String comune, String provincia) {
        this.via = via;
        this.cap = cap;
        this.comune = comune;
        this.provincia = provincia;
    }

    /** Nessun campo compilato. */
    public boolean isVuoto() {
        return Stream.of(via, cap, comune, provincia).allMatch(s -> s == null || s.isBlank());
    }

    /** Es. "Via Roma 1, 20121 Milano (MI)"; stringa vuota se l'indirizzo è vuoto. */
    public String getFormattato() {
        String localita = Stream.of(cap, comune).filter(s -> s != null && !s.isBlank()).collect(Collectors.joining(" "));
        if (provincia != null && !provincia.isBlank()) {
            localita = (localita + " (" + provincia + ")").trim();
        }
        return Stream.of(via, localita).filter(s -> s != null && !s.isBlank()).collect(Collectors.joining(", "));
    }

    public String getVia() {
        return via;
    }

    public void setVia(String via) {
        this.via = via;
    }

    public String getCap() {
        return cap;
    }

    public void setCap(String cap) {
        this.cap = cap;
    }

    public String getComune() {
        return comune;
    }

    public void setComune(String comune) {
        this.comune = comune;
    }

    public String getProvincia() {
        return provincia;
    }

    public void setProvincia(String provincia) {
        this.provincia = provincia;
    }
}
