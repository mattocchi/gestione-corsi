package it.attocchi.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import it.attocchi.entity.CodiceFiscale;
import it.attocchi.entity.IntestazioneDocumenti;
import it.attocchi.entity.User;

/**
 * Sezione "Intestazione dei documenti fiscali" del genitore o dell'allievo adulto: nome, cognome e codice fiscale di
 * chi intesta l'attestazione delle spese (es. il coniuge che detrae), legati a un {@link Binder}. Nome e cognome vuoti
 * usano quelli dell'utente, mostrati come segnaposto.
 */
public class IntestazioneFields<T extends IntestazioneDocumenti> {

    /** Aiuto del codice fiscale del genitore, che serve solo a questo. */
    public static final String AIUTO_GENITORE = "Serve solo per intestare l'attestazione delle spese: è il codice "
        + "fiscale di chi detrae";

    /** Aiuto del codice fiscale dell'allievo adulto, che ha già il proprio. */
    public static final String AIUTO_ALLIEVO = "Solo se l'attestazione va intestata a un'altra persona: vuoto = il "
        + "codice fiscale dell'allievo";

    private final TextField nome = new TextField("Nome dell'intestatario");
    private final TextField cognome = new TextField("Cognome dell'intestatario");
    private final TextField codiceFiscale = new TextField("Codice fiscale dell'intestatario");

    /** @param aiutoCodiceFiscale a cosa serve il codice fiscale, o cosa succede se resta vuoto */
    public IntestazioneFields(Binder<T> binder, User user, String aiutoCodiceFiscale) {
        if (user != null) {
            nome.setPlaceholder(user.getNome());
            cognome.setPlaceholder(user.getCognome());
        }
        nome.setHelperText("Solo se diverso da quello dell'utente");
        cognome.setHelperText("Solo se diverso da quello dell'utente");
        codiceFiscale.setMaxLength(16);
        codiceFiscale.setHelperText(aiutoCodiceFiscale);

        binder.forField(nome).withConverter(DatoFacoltativo::normalizza, v -> v != null ? v : "")
            .bind(IntestazioneDocumenti::getIntestazioneNome, IntestazioneDocumenti::setIntestazioneNome);
        binder.forField(cognome).withConverter(DatoFacoltativo::normalizza, v -> v != null ? v : "")
            .bind(IntestazioneDocumenti::getIntestazioneCognome, IntestazioneDocumenti::setIntestazioneCognome);
        binder.forField(codiceFiscale).withConverter(CodiceFiscale::normalizza, v -> v != null ? v : "")
            .withValidator(v -> v == null || CodiceFiscale.isValido(v), "Codice fiscale non valido")
            .bind(IntestazioneDocumenti::getIntestazioneCodiceFiscale, IntestazioneDocumenti::setIntestazioneCodiceFiscale);
    }

    /** Titolo, spiegazione e campi della sezione. */
    public Component[] getSezione() {
        Span nota = new Span("Dati usati solo nell'attestazione delle spese per la detrazione: indica chi detrae le "
            + "spese, anche se non è l'utente (es. il coniuge). L'indirizzo resta quello dei dati anagrafici.");
        nota.addClassNames(Stili.TESTO_SECONDARIO);
        return new Component[] {ViewSupport.sezione("Intestazione dei documenti fiscali"), nota,
            new FormLayout(nome, cognome, codiceFiscale)};
    }

    /** Testo ripulito dagli spazi, {@code null} se vuoto. */
    private static final class DatoFacoltativo {
        static String normalizza(String valore) {
            return valore == null || valore.isBlank() ? null : valore.strip();
        }
    }
}
