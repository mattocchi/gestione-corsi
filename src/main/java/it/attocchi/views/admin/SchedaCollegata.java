package it.attocchi.views.admin;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.QueryParameters;
import it.attocchi.views.ViewSupport;

import java.util.Optional;
import java.util.function.BooleanSupplier;

/**
 * Link "Apri la scheda di …" dal dialog di modifica di un allievo o di un genitore alla scheda della persona
 * collegata: porta alla sua pagina con {@code ?id=…}, che apre il suo dialog (Indietro torna alla pagina di prima).
 */
final class SchedaCollegata {

    static final String PARAMETRO = "id";

    private SchedaCollegata() {
    }

    /**
     * @param dialog     il dialog da cui si parte, chiuso prima di navigare
     * @param modificato {@code true} se ci sono modifiche non salvate: si chiede conferma prima di lasciarle
     */
    static Button link(String nome, Dialog dialog, BooleanSupplier modificato, Class<? extends Component> vista, Long id) {
        Button link = new Button("Apri la scheda di " + nome, VaadinIcon.ARROW_RIGHT.create(), e -> {
            if (!modificato.getAsBoolean()) {
                vai(dialog, vista, id);
                return;
            }
            Dialog conferma = ViewSupport.dialog("Lasciare le modifiche?", "420px");
            conferma.add(new Span("Le modifiche non salvate a questa scheda andranno perse."));
            Button lascia = new Button("Apri senza salvare", ev -> {
                conferma.close();
                vai(dialog, vista, id);
            });
            lascia.addThemeVariants(ButtonVariant.PRIMARY);
            conferma.getFooter().add(ViewSupport.closeButton(conferma, "Annulla"), lascia);
            conferma.open();
        });
        link.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.SMALL);
        link.setIconAfterText(true);
        return link;
    }

    /** Id della scheda da aprire, dal parametro dell'indirizzo; vuoto se manca o non è un numero. */
    static Optional<Long> id(BeforeEnterEvent event) {
        return event.getLocation().getQueryParameters().getSingleParameter(PARAMETRO).flatMap(valore -> {
            try {
                return Optional.of(Long.valueOf(valore));
            } catch (NumberFormatException ex) {
                return Optional.empty();
            }
        });
    }

    private static void vai(Dialog dialog, Class<? extends Component> vista, Long id) {
        dialog.close();
        UI.getCurrent().navigate(vista, QueryParameters.of(PARAMETRO, String.valueOf(id)));
    }
}
