package it.attocchi.views;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.signals.Signal;

/**
 * Supporto comune per il layout responsive delle view.
 * <p>
 * Il tema è Aura, quindi le utility responsive di Lumo non sono disponibili: la larghezza
 * della finestra viene letta da {@code Page.windowSizeSignal()}.
 */
public final class ViewSupport {

    /** Sotto questa larghezza (px) le view usano il layout per smartphone. */
    public static final int MOBILE_BREAKPOINT = 640;

    private ViewSupport() {
    }

    /**
     * Signal che vale {@code true} quando la finestra è più stretta del breakpoint mobile.
     * Una larghezza 0 (non ancora nota) è trattata come desktop.
     */
    public static Signal<Boolean> isMobile() {
        return UI.getCurrentOrThrow().getPage().windowSizeSignal()
            .map(size -> size.width() > 0 && size.width() < MOBILE_BREAKPOINT);
    }

    /** Nasconde le colonne indicate quando la finestra è in formato mobile. */
    public static void hideOnMobile(Grid<?> grid, Grid.Column<?>... columns) {
        Signal<Boolean> mobile = isMobile();
        Signal.effect(grid, () -> {
            boolean visible = !mobile.get();
            for (Grid.Column<?> column : columns) {
                column.setVisible(visible);
            }
        });
    }

    /** Pulsante compatto con sola icona; l'etichetta è usata come tooltip e nome accessibile. */
    public static Button iconButton(VaadinIcon icon, String label,
                                    ComponentEventListener<ClickEvent<Button>> listener) {
        Button button = iconButton(icon, label);
        button.addClickListener(listener);
        return button;
    }

    /** Pulsante con sola icona senza listener, ad esempio dentro un {@code Anchor} di download. */
    public static Button iconButton(VaadinIcon icon, String label) {
        Button button = new Button(icon.create());
        button.addThemeVariants(ButtonVariant.TERTIARY);
        button.setAriaLabel(label);
        button.setTooltipText(label);
        return button;
    }

    /** Contenitore per i pulsanti di una colonna "Azioni" che non va a capo. */
    public static HorizontalLayout actions(Button... buttons) {
        HorizontalLayout layout = new HorizontalLayout(buttons);
        layout.setSpacing(false);
        layout.setWrap(false);
        return layout;
    }

    /** Colonna azioni: larghezza adattata al contenuto, fissata sul bordo destro. */
    public static void configureActionsColumn(Grid.Column<?> column) {
        column.setHeader("Azioni").setAutoWidth(true).setFlexGrow(0).setFrozenToEnd(true);
    }

    /**
     * Dialog con titolo, larghezza massima e footer per i pulsanti: su mobile il dialog
     * si adatta alla viewport e i pulsanti del footer restano sempre visibili.
     */
    public static Dialog dialog(String title, String width) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(title);
        dialog.setWidth(width);
        dialog.setMaxWidth("100%");
        return dialog;
    }

    /** Pulsante "Chiudi"/"Annulla" per il footer di un dialog. */
    public static Button closeButton(Dialog dialog, String text) {
        Button button = new Button(text, e -> dialog.close());
        button.addThemeVariants(ButtonVariant.TERTIARY);
        return button;
    }
}
