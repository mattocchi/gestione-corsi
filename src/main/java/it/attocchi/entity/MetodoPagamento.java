package it.attocchi.entity;

/** Metodo con cui è stata pagata una rata. */
public enum MetodoPagamento {
    BONIFICO("Bonifico"),
    CASSA("Cassa");

    private final String descrizione;

    MetodoPagamento(String descrizione) {
        this.descrizione = descrizione;
    }

    public String getDescrizione() {
        return descrizione;
    }
}
