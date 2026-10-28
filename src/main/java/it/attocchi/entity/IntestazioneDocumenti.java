package it.attocchi.entity;

/**
 * Intestatario dell'attestazione delle spese (genitore o allievo adulto): nome e cognome se diversi da quelli
 * dell'utente (es. il coniuge che detrae le spese) e il codice fiscale da stampare. I campi vuoti usano i dati
 * dell'utente.
 */
public interface IntestazioneDocumenti {

    User getUser();

    String getIntestazioneNome();

    void setIntestazioneNome(String nome);

    String getIntestazioneCognome();

    void setIntestazioneCognome(String cognome);

    /** Codice fiscale dell'intestatario così come inserito ({@code null} se vuoto). */
    String getIntestazioneCodiceFiscale();

    void setIntestazioneCodiceFiscale(String codiceFiscale);

    /** Codice fiscale stampato nell'attestazione, con l'eventuale ripiego sui dati della persona. */
    String getCodiceFiscaleIntestatario();

    /** "Nome Cognome" dell'intestatario: quelli indicati, o in mancanza quelli dell'utente. */
    default String getNomeIntestatario() {
        User user = getUser();
        String nome = pieno(getIntestazioneNome()) ? getIntestazioneNome().strip() : user != null ? user.getNome() : null;
        String cognome = pieno(getIntestazioneCognome()) ? getIntestazioneCognome().strip() : user != null ? user.getCognome() : null;
        return ((nome != null ? nome : "") + " " + (cognome != null ? cognome : "")).strip();
    }

    /** Copia i dati dell'intestazione da una persona a un'altra. */
    static void copia(IntestazioneDocumenti da, IntestazioneDocumenti a) {
        a.setIntestazioneNome(da.getIntestazioneNome());
        a.setIntestazioneCognome(da.getIntestazioneCognome());
        a.setIntestazioneCodiceFiscale(da.getIntestazioneCodiceFiscale());
    }

    private static boolean pieno(String valore) {
        return valore != null && !valore.isBlank();
    }
}
