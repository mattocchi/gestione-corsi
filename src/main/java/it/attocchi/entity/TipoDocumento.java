package it.attocchi.entity;

/** Documento inviato per email (e scaricabile), con titolo e nome del template FreeMarker. */
public enum TipoDocumento {
    CONFERMA_ISCRIZIONE("Conferma di iscrizione", "conferma"),
    RICEVUTA("Ricevuta di pagamento", "ricevuta");

    private final String titolo;
    private final String template;

    TipoDocumento(String titolo, String template) {
        this.titolo = titolo;
        this.template = template;
    }

    public String getTitolo() {
        return titolo;
    }

    public String getTemplate() {
        return template;
    }
}
