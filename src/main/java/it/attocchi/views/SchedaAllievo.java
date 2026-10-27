package it.attocchi.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import it.attocchi.entity.Allievo;
import it.attocchi.entity.Genitore;
import it.attocchi.entity.User;

/**
 * Scheda dell'allievo in due modalità secondo il genitore (Gestione Allievi, I Miei Figli, profilo dell'allievo):
 * <ul>
 *     <li>con genitore, i dati dell'allievo (nome, nascita, codice fiscale) in evidenza, contatti e indirizzo del
 *     genitore come riferimento in sola lettura e quelli propri dell'allievo in una sezione facoltativa chiusa;</li>
 *     <li>allievo adulto (senza genitore), contatti e indirizzo propri in evidenza: documenti ed email vanno a lui.</li>
 * </ul>
 * I campi sono quelli di {@link UtenteFields} e {@link AnagraficaFields}, che restano legati ai loro binder.
 */
public class SchedaAllievo extends VerticalLayout {

    private final FormLayout contatti;
    private final Div modalita = new Div();
    private final Div riferimento = new Div();
    private final Details datiPropri = new Details("Contatti e indirizzo propri dell'allievo (facoltativi)");
    private final VerticalLayout sezioneContatti = new VerticalLayout();

    /**
     * @param sceltaGenitore campo per scegliere il genitore (staff), oppure {@code null}
     * @param conAccesso     mostra anche username e password (staff)
     */
    public SchedaAllievo(UtenteFields utente, AnagraficaFields<Allievo> anagrafica, Component sceltaGenitore,
                         boolean conAccesso) {
        FormLayout allievo = new FormLayout(utente.getNomeECognome());
        allievo.add(anagrafica.getDatiPersonali());
        contatti = new FormLayout(utente.getContatti());
        contatti.add(anagrafica.getIndirizzo());

        add(ViewSupport.sezione("Allievo"), allievo, ViewSupport.sezione("Genitore"));
        if (sceltaGenitore != null) {
            add(sceltaGenitore);
        }
        modalita.addClassNames(Stili.TESTO_SECONDARIO);
        riferimento.addClassNames("flex flex-col gap-1 p-3 rounded-(--vaadin-radius-m)"
            + " border border-solid border-(--vaadin-border-color-secondary) w-full box-border");
        sezioneContatti.addClassNames("p-0 gap-2");
        add(modalita, sezioneContatti);
        if (conAccesso) {
            add(ViewSupport.sezione("Accesso all'app"), new FormLayout(utente.getAccesso()));
        }
        addClassNames("p-0");
    }

    /** Mostra la scheda con il genitore indicato, o come allievo adulto se {@code null}. */
    public void mostraGenitore(Genitore genitore) {
        sezioneContatti.removeAll();
        riferimento.removeAll();
        datiPropri.removeAll();
        if (genitore == null) {
            modalita.setText("Allievo adulto: documenti, email e pagamenti sono suoi. Inserisci i suoi contatti e "
                + "il suo indirizzo.");
            sezioneContatti.add(ViewSupport.sezione("Contatti e indirizzo"), contatti);
            return;
        }
        modalita.setText("Con genitore: documenti ed email vanno al genitore, i cui contatti e indirizzo valgono come "
            + "riferimento.");
        Span titolo = new Span(VaadinIcon.FAMILY.create(), new Span(genitore.getNomeCompleto()));
        titolo.addClassNames("inline-flex items-center gap-2 font-(--aura-font-weight-semibold)");
        riferimento.add(titolo);
        User user = genitore.getUser();
        aggiungi("Email", user != null ? user.getEmail() : null);
        aggiungi("Telefono", user != null ? user.getTelefono() : null);
        aggiungi("Indirizzo", genitore.getIndirizzo().getFormattato());
        datiPropri.add(contatti);
        datiPropri.setOpened(false);
        datiPropri.addClassNames("w-full");
        sezioneContatti.add(ViewSupport.sezione("Contatti e indirizzo del genitore"), riferimento, datiPropri);
    }

    private void aggiungi(String etichetta, String valore) {
        Span riga = new Span(etichetta + ": " + (valore != null && !valore.isBlank() ? valore : "—"));
        riga.addClassNames(Stili.TESTO_SECONDARIO);
        riferimento.add(riga);
    }
}
