package it.attocchi.entity;

/**
 * Quota del corso applicata a un'{@link Iscrizione}: piena (il costo totale del corso), pre-iscrizione (il prezzo di
 * pre-iscrizione del corso, che segue se cambia) o personalizzata (importo e motivo dell'iscrizione).
 */
public enum TipoQuota {
    PIENA("Quota piena"),
    PREISCRIZIONE("Pre-iscrizione"),
    PERSONALIZZATA("Personalizzata");

    private final String descrizione;

    TipoQuota(String descrizione) {
        this.descrizione = descrizione;
    }

    public String getDescrizione() {
        return descrizione;
    }
}
