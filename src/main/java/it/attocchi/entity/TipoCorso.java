package it.attocchi.entity;

/**
 * Tipo di un {@link Corso}: un servizio (es. Pulizia) ha tutte le funzioni del corso, cambiano solo le etichette
 * (chi lo svolge è un insegnante, mostrato come "Incaricato").
 */
public enum TipoCorso {
    CORSO("Corso", "Corsi", "Insegnante", "Insegnanti"),
    SERVIZIO("Servizio", "Servizi", "Incaricato", "Incaricati");

    private final String descrizione;
    private final String plurale;
    private final String chiLoSvolge;
    private final String chiLoSvolgonoPlurale;

    TipoCorso(String descrizione, String plurale, String chiLoSvolge, String chiLoSvolgonoPlurale) {
        this.descrizione = descrizione;
        this.plurale = plurale;
        this.chiLoSvolge = chiLoSvolge;
        this.chiLoSvolgonoPlurale = chiLoSvolgonoPlurale;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public String getPlurale() {
        return plurale;
    }

    /** "Insegnante" o "Incaricato". */
    public String getChiLoSvolge() {
        return chiLoSvolge;
    }

    /** "Insegnanti" o "Incaricati". */
    public String getChiLoSvolgonoPlurale() {
        return chiLoSvolgonoPlurale;
    }
}
