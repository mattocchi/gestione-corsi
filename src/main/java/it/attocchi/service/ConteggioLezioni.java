package it.attocchi.service;

import it.attocchi.entity.Lezione;
import it.attocchi.entity.Presenza;
import it.attocchi.entity.Presenza.StatoPresenza;

import java.util.Collection;

/**
 * Lezioni di un allievo in un corso, dalle sue presenze: svolte = lezioni completate a cui era atteso (presente o
 * assente), da fare = ancora in calendario, mancanti = previste dal corso meno svolte (mai sotto zero). Presenze e
 * assenze contano solo le svolte; le lezioni non effettuate non contano.
 */
public record ConteggioLezioni(int previste, int svolte, int daFare, int presenze, int giustificate, int ingiustificate) {

    /** @param previste lezioni per partecipante del corso ({@code Corso.numeroLezioni}; {@code null} = svolte + da fare) */
    public static ConteggioLezioni di(Collection<Presenza> presenzeAllievo, Integer previste) {
        int svolte = 0;
        int daFare = 0;
        int presenti = 0;
        int giustificate = 0;
        int ingiustificate = 0;
        for (Presenza presenza : presenzeAllievo) {
            Lezione lezione = presenza.getLezione();
            if (lezione.isDaFare()) {
                daFare++;
            } else if (svolta(lezione)) {
                svolte++;
                if (presenza.getStato() == StatoPresenza.PRESENTE) {
                    presenti++;
                } else if (presenza.getStato() == StatoPresenza.ASSENTE_GIUSTIFICATA) {
                    giustificate++;
                } else if (presenza.getStato() == StatoPresenza.ASSENTE_INGIUSTIFICATA) {
                    ingiustificate++;
                }
            }
        }
        // Senza numero di lezioni (corso con data di fine) le previste sono le lezioni dell'allievo
        return new ConteggioLezioni(previste != null ? previste : svolte + daFare, svolte, daFare, presenti, giustificate,
            ingiustificate);
    }

    /** Lezione completata (e non segnata come non effettuata). */
    public static boolean svolta(Lezione lezione) {
        return lezione.isCompletata() && !lezione.isNonEffettuata();
    }

    public int mancanti() {
        return Math.max(0, previste - svolte);
    }
}
