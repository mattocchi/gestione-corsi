package it.attocchi.entity;

import java.time.LocalDate;

/** Dati anagrafici comuni a studenti, genitori e insegnanti (nome, cognome e contatti sono sull'utente). */
public interface Anagrafica {

    String getCodiceFiscale();

    void setCodiceFiscale(String codiceFiscale);

    LocalDate getDataNascita();

    void setDataNascita(LocalDate dataNascita);

    String getLuogoNascita();

    void setLuogoNascita(String luogoNascita);

    /** Mai {@code null}. */
    Indirizzo getIndirizzo();
}
