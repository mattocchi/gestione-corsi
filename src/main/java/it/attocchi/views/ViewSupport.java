package it.attocchi.views;

import java.util.Set;
import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.signals.Signal;
import it.attocchi.entity.AnnoScolastico;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * Supporto comune per il layout responsive delle view.
 * <p>
 * Il tema è Aura, quindi le utility responsive di Lumo non sono disponibili: la larghezza
 * della finestra viene letta da {@code Page.windowSizeSignal()}.
 */
public final class ViewSupport {

    /** Sotto questa larghezza (px) le view usano il layout per smartphone. */
    public static final int MOBILE_BREAKPOINT = 640;

    /** Lingua dell'applicazione (impostata sulla UI da {@code LocaleItaliano}). */
    public static final Locale LOCALE = Locale.ITALY;

    /** Calendario in italiano, settimana dal lunedì, data gg/mm/aaaa (accettati anche g/m/aaaa e g/m/aa). */
    private static final DatePicker.DatePickerI18n DATE_PICKER_I18N = new DatePicker.DatePickerI18n()
        .setMonthNames(List.of("gennaio", "febbraio", "marzo", "aprile", "maggio", "giugno",
            "luglio", "agosto", "settembre", "ottobre", "novembre", "dicembre"))
        .setWeekdays(List.of("domenica", "lunedì", "martedì", "mercoledì", "giovedì", "venerdì", "sabato"))
        .setWeekdaysShort(List.of("dom", "lun", "mar", "mer", "gio", "ven", "sab"))
        .setFirstDayOfWeek(1)
        .setToday("Oggi")
        .setCancel("Annulla")
        .setDialogAccessibleName("Calendario")
        .setDateFormats("dd/MM/yyyy", "d/M/yyyy", "d/M/yy")
        .setBadInputErrorMessage("Data non valida: usa il formato gg/mm/aaaa")
        .setMinErrorMessage("Data troppo vecchia")
        .setMaxErrorMessage("Data troppo avanti");

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

    /**
     * Filtro per anno scolastico tra quelli indicati (dal più recente), con l'anno corrente preselezionato
     * se presente, altrimenti il più recente.
     */
    /** Filtro per anno scolastico con "(archivio)" accanto agli anni non attivi. */
    public static ComboBox<String> filtroAnnoScolastico(List<String> anni, Set<String> nonAttivi) {
        ComboBox<String> filtro = filtroAnnoScolastico(anni);
        filtro.setItemLabelGenerator(etichettaAnno(nonAttivi));
        return filtro;
    }

    /** Etichetta di un anno scolastico nei filtri: "2023/2024 (archivio)" se non è più attivo. */
    public static ItemLabelGenerator<String> etichettaAnno(Set<String> nonAttivi) {
        return anno -> nonAttivi.contains(anno) ? anno + " (archivio)" : anno;
    }

    public static ComboBox<String> filtroAnnoScolastico(List<String> anni) {
        ComboBox<String> filtro = new ComboBox<>();
        filtro.setAriaLabel("Filtra per anno scolastico");
        filtro.setItems(anni);
        String corrente = AnnoScolastico.di(LocalDate.now());
        filtro.setValue(anni.contains(corrente) ? corrente : anni.isEmpty() ? null : anni.getFirst());
        filtro.addClassNames("w-40");
        return filtro;
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
        // Stile normale (fondo e bordo), non terziario: si vede che è un pulsante anche senza hover (touch)
        Button button = new Button(icon.create());
        button.setAriaLabel(label);
        button.setTooltipText(label);
        return button;
    }

    /** Contenitore per i pulsanti di una colonna "Azioni" che non va a capo. */
    public static HorizontalLayout actions(Button... buttons) {
        HorizontalLayout layout = new HorizontalLayout(buttons);
        layout.addClassNames("gap-0");
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

    /** Campo data in italiano: usarlo per ogni {@code DatePicker} dell'applicazione. */
    public static DatePicker datePicker(String label) {
        DatePicker datePicker = new DatePicker(label);
        datePicker.setLocale(LOCALE);
        datePicker.setI18n(DATE_PICKER_I18N);
        return datePicker;
    }

    /** Campo ora in italiano (24 ore): usarlo per ogni {@code TimePicker} dell'applicazione. */
    public static TimePicker timePicker(String label) {
        TimePicker timePicker = new TimePicker(label);
        timePicker.setLocale(LOCALE);
        timePicker.setI18n(new TimePicker.TimePickerI18n()
            .setBadInputErrorMessage("Ora non valida: usa il formato hh:mm (es. 14:30)"));
        return timePicker;
    }

    /** Titolo di una sezione all'interno di un dialog. */
    public static H3 sezione(String titolo) {
        H3 h3 = new H3(titolo);
        h3.addClassNames(Stili.TITOLO_SEZIONE);
        return h3;
    }

    /** Pulsante "Chiudi"/"Annulla" per il footer di un dialog. */
    public static Button closeButton(Dialog dialog, String text) {
        Button button = new Button(text, e -> dialog.close());
        button.addThemeVariants(ButtonVariant.TERTIARY);
        return button;
    }
}
