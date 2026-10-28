package it.attocchi.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import it.attocchi.entity.Anagrafica;
import it.attocchi.entity.CodiceFiscale;

import java.time.LocalDate;
import java.util.Locale;

/**
 * Campi dei dati anagrafici (codice fiscale, nascita, indirizzo in formato italiano) legati a un {@link Binder}
 * di allievo, genitore o insegnante. Codice fiscale e provincia sono salvati in maiuscolo.
 */
public class AnagraficaFields<T extends Anagrafica> {

    private final TextField codiceFiscale = new TextField("Codice fiscale");
    private final DatePicker dataNascita = ViewSupport.datePicker("Data di nascita");
    private final TextField luogoNascita = new TextField("Luogo di nascita");
    private final TextField via = new TextField("Indirizzo");
    private final TextField cap = new TextField("CAP");
    private final TextField comune = new TextField("Comune");
    private final TextField provincia = new TextField("Provincia");
    private final boolean conCodiceFiscale;

    /**
     * Tutti i campi sono facoltativi; il codice fiscale, se inserito, deve essere valido.
     *
     * @param perDocumenti la data di nascita serve per conferme e ricevute, il codice fiscale per le ricevute
     *                     (allievi): lo dice il testo di aiuto dei due campi
     */
    public AnagraficaFields(Binder<T> binder, boolean perDocumenti) {
        this(binder, perDocumenti, true);
    }

    /**
     * Come {@link #AnagraficaFields(Binder, boolean)}; senza {@code conCodiceFiscale} il codice fiscale non c'è (il
     * genitore lo ha nell'intestazione dei documenti fiscali, {@link IntestazioneFields}).
     */
    public AnagraficaFields(Binder<T> binder, boolean perDocumenti, boolean conCodiceFiscale) {
        this.conCodiceFiscale = conCodiceFiscale;
        codiceFiscale.setMaxLength(16);
        dataNascita.setMax(LocalDate.now());
        via.setPlaceholder("via e numero civico");
        cap.setMaxLength(5);
        cap.setAllowedCharPattern("[0-9]");
        provincia.setMaxLength(2);
        provincia.setPlaceholder("es. RM");

        if (perDocumenti) {
            codiceFiscale.setHelperText("Necessario per le ricevute");
            dataNascita.setHelperText("Necessaria per conferme e ricevute");
        }
        if (conCodiceFiscale) {
            binder.forField(codiceFiscale).withConverter(CodiceFiscale::normalizza, v -> v != null ? v : "")
                .withValidator(v -> v == null || CodiceFiscale.isValido(v), "Codice fiscale non valido")
                .bind(Anagrafica::getCodiceFiscale, Anagrafica::setCodiceFiscale);
        }

        binder.forField(dataNascita).withValidator(d -> d == null || !d.isAfter(LocalDate.now()), "La data di nascita non può essere futura")
            .bind(Anagrafica::getDataNascita, Anagrafica::setDataNascita);

        binder.forField(luogoNascita).withNullRepresentation("")
            .bind(Anagrafica::getLuogoNascita, Anagrafica::setLuogoNascita);
        binder.forField(via).withNullRepresentation("")
            .bind(a -> a.getIndirizzo().getVia(), (a, v) -> a.getIndirizzo().setVia(v));
        binder.forField(cap).withNullRepresentation("")
            .withValidator(v -> v == null || v.matches("\\d{5}"), "Il CAP ha 5 cifre")
            .bind(a -> a.getIndirizzo().getCap(), (a, v) -> a.getIndirizzo().setCap(v));
        binder.forField(comune).withNullRepresentation("")
            .bind(a -> a.getIndirizzo().getComune(), (a, v) -> a.getIndirizzo().setComune(v));
        binder.forField(provincia)
            .withConverter(v -> v.isBlank() ? null : v.trim().toUpperCase(Locale.ROOT), v -> v != null ? v : "")
            .withValidator(v -> v == null || v.matches("[A-Z]{2}"), "Sigla di 2 lettere, es. RM")
            .bind(a -> a.getIndirizzo().getProvincia(), (a, v) -> a.getIndirizzo().setProvincia(v));
    }

    public Component[] getComponents() {
        return conCodiceFiscale ? new Component[] {codiceFiscale, dataNascita, luogoNascita, via, cap, comune, provincia}
            : new Component[] {dataNascita, luogoNascita, via, cap, comune, provincia};
    }

    /** Codice fiscale (se c'è), data e luogo di nascita. */
    public Component[] getDatiPersonali() {
        return conCodiceFiscale ? new Component[] {codiceFiscale, dataNascita, luogoNascita}
            : new Component[] {dataNascita, luogoNascita};
    }

    /** Data e luogo di nascita. */
    public Component[] getNascita() {
        return new Component[] {dataNascita, luogoNascita};
    }

    /** Il campo del codice fiscale (con {@code conCodiceFiscale}). */
    public Component getCodiceFiscale() {
        return codiceFiscale;
    }

    /** Via, CAP, comune e provincia. */
    public Component[] getIndirizzo() {
        return new Component[] {via, cap, comune, provincia};
    }
}
