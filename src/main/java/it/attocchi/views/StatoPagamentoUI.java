package it.attocchi.views;

import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import it.attocchi.service.PagamentoService.Situazione;

/** Stato del pagamento di una quota nelle view: badge e colore del pulsante "Quota e pagamenti". */
public final class StatoPagamentoUI {

    private StatoPagamentoUI() {
    }

    /** Badge Saldato (verde), Parziale con percentuale (arancio), Da pagare. */
    public static Badge badge(Situazione situazione) {
        return switch (situazione.stato()) {
            case SALDATO -> {
                Badge badge = new Badge("Saldato", VaadinIcon.CHECK.create());
                badge.addThemeVariants(BadgeVariant.SUCCESS);
                yield badge;
            }
            case PARZIALE -> {
                Badge badge = new Badge("Parziale " + situazione.percentualePagata() + "%", VaadinIcon.HOURGLASS.create());
                badge.addThemeVariants(BadgeVariant.WARNING);
                yield badge;
            }
            case NESSUN_PAGAMENTO -> new Badge("Da pagare", VaadinIcon.CLOCK.create());
        };
    }

    /**
     * Colora il pulsante dei pagamenti secondo lo stato e ne aggiorna il tooltip: su mobile la colonna
     * con il badge è nascosta e lo stato si legge dal colore del pulsante.
     */
    public static void coloraPulsante(Button button, String etichetta, Situazione situazione) {
        switch (situazione.stato()) {
            case SALDATO -> button.addThemeVariants(ButtonVariant.SUCCESS);
            case PARZIALE -> button.addThemeVariants(ButtonVariant.WARNING);
            case NESSUN_PAGAMENTO -> button.addThemeVariants(ButtonVariant.ERROR);
        }
        button.setTooltipText(etichetta + ": " + situazione.stato().getDescrizione()
            + " (residuo " + Formati.euro(situazione.residuo()) + ")");
    }
}
