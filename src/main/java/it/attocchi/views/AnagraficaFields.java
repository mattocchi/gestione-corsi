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
 * di studente, genitore o insegnante. Codice fiscale e provincia sono salvati in maiuscolo.
 */
public class AnagraficaFields<T extends Anagrafica> {

    private final TextField codiceFiscale = new TextField("Codice fiscale");
    private final DatePicker dataNascita = ViewSupport.datePicker("Data di nascita");
    private final TextField luogoNascita = new TextField("Luogo di nascita");
    private final TextField via = new TextField("Indirizzo");
    private final TextField cap = new TextField("CAP");
    private final TextField comune = new TextField("Comune");
    private final TextField provincia = new TextField("Provincia");

    /**
     * @param obbligatori codice fiscale e data di nascita obbligatori (studenti); altrimenti facoltativi,
     *                    ma il codice fiscale se inserito deve essere valido
     */
    public AnagraficaFields(Binder<T> binder, boolean obbligatori) {
        codiceFiscale.setMaxLength(16);
        dataNascita.setMax(LocalDate.now());
        via.setPlaceholder("via e numero civico");
        cap.setMaxLength(5);
        cap.setAllowedCharPattern("[0-9]");
        provincia.setMaxLength(2);
        provincia.setPlaceholder("es. RM");

        var cf = binder.forField(codiceFiscale);
        if (obbligatori) {
            cf.asRequired("Inserisci il codice fiscale");
        }
        cf.withConverter(CodiceFiscale::normalizza, v -> v != null ? v : "")
            .withValidator(v -> v == null || CodiceFiscale.isValido(v), "Codice fiscale non valido")
            .bind(Anagrafica::getCodiceFiscale, Anagrafica::setCodiceFiscale);

        var nascita = binder.forField(dataNascita);
        if (obbligatori) {
            nascita.asRequired("Inserisci la data di nascita");
        }
        nascita.withValidator(d -> d == null || !d.isAfter(LocalDate.now()), "La data di nascita non può essere futura")
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
        return new Component[] {codiceFiscale, dataNascita, luogoNascita, via, cap, comune, provincia};
    }
}
