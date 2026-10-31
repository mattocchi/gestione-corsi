package it.attocchi.entity;

/**
 * Documento inviato per email (e scaricabile), con titolo, nome breve e al plurale, e nome del template FreeMarker
 * dell'email ({@code email/}) e, per conferma e ricevuta, della pagina A4 ({@code documenti/}).
 */
public enum TipoDocumento {
    CONFERMA_ISCRIZIONE("Conferma di iscrizione", "Conferma", "Conferme", "conferma"),
    RICEVUTA("Ricevuta di pagamento", "Ricevuta", "Ricevute", "ricevuta"),
    /** Riepilogo annuale dei pagamenti della famiglia o dell'allievo adulto (PDF {@code documenti/riepilogo-famiglia}). */
    RIEPILOGO("Riepilogo pagamenti", "Riepilogo", "Riepiloghi", "riepilogo"),
    /** Spese di un allievo in un anno solare, di tutti i corsi (PDF {@code documenti/attestazione}). */
    ATTESTAZIONE("Attestazione delle spese", "Attestazione", "Attestazioni", "attestazione"),
    /** Incarico di un insegnante per un corso e anno scolastico ({@code LetteraIncarico}, PDF {@code documenti/lettera-incarico}). */
    LETTERA_INCARICO("Lettera di incarico", "Lettera di incarico", "Lettere di incarico", "lettera-incarico"),
    /** Riepilogo dei corsi attivi come nella dashboard, con gli iscritti (solo staff, PDF {@code documenti/riepilogo-corsi}). */
    RIEPILOGO_CORSI("Riepilogo dei corsi attivi", "Riepilogo corsi", "Riepilogo corsi", "riepilogo-corsi");

    private final String titolo;
    private final String breve;
    private final String plurale;
    private final String template;

    TipoDocumento(String titolo, String breve, String plurale, String template) {
        this.titolo = titolo;
        this.breve = breve;
        this.plurale = plurale;
        this.template = template;
    }

    public String getTitolo() {
        return titolo;
    }

    public String getBreve() {
        return breve;
    }

    public String getPlurale() {
        return plurale;
    }

    public String getTemplate() {
        return template;
    }

    /** Documento di una singola iscrizione (conferma o ricevuta), non il riepilogo annuale né l'attestazione. */
    public boolean isDellIscrizione() {
        return this == CONFERMA_ISCRIZIONE || this == RICEVUTA;
    }
}
