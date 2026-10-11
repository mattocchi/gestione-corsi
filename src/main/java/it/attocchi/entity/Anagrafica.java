package it.attocchi.entity;

import java.time.LocalDate;

/** Dati anagrafici comuni a allievi, genitori e insegnanti (nome, cognome e contatti sono sull'utente). */
public interface Anagrafica {

    String getCodiceFiscale();

    void setCodiceFiscale(String codiceFiscale);

    LocalDate getDataNascita();

    void setDataNascita(LocalDate dataNascita);

    String getLuogoNascita();

    void setLuogoNascita(String luogoNascita);

    /** Mai {@code null}. */
    Indirizzo getIndirizzo();

    /** Copia i dati anagrafici (indirizzo compreso) da una persona a un'altra. */
    static void copia(Anagrafica da, Anagrafica a) {
        a.setCodiceFiscale(da.getCodiceFiscale());
        a.setDataNascita(da.getDataNascita());
        a.setLuogoNascita(da.getLuogoNascita());
        Indirizzo indirizzo = da.getIndirizzo();
        a.getIndirizzo().setVia(indirizzo.getVia());
        a.getIndirizzo().setCap(indirizzo.getCap());
        a.getIndirizzo().setComune(indirizzo.getComune());
        a.getIndirizzo().setProvincia(indirizzo.getProvincia());
    }
}
