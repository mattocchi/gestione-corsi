package it.attocchi.views;

import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.timepicker.TimePicker;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Campi data e ora in italiano creati da {@link ViewSupport}. */
class CampiDataOraTest {

    @Test
    void datePicker_italiano() {
        DatePicker datePicker = ViewSupport.datePicker("Data");
        DatePicker.DatePickerI18n i18n = datePicker.getI18n();
        assertEquals(Locale.ITALY, datePicker.getLocale());
        assertEquals(List.of("dd/MM/yyyy", "d/M/yyyy", "d/M/yy"), i18n.getDateFormats());
        assertEquals(1, i18n.getFirstDayOfWeek());
        assertEquals("ottobre", i18n.getMonthNames().get(9));
        assertEquals("lun", i18n.getWeekdaysShort().get(1));
        assertEquals("Oggi", i18n.getToday());
        assertEquals("Annulla", i18n.getCancel());
    }

    @Test
    void timePicker_italiano() {
        TimePicker timePicker = ViewSupport.timePicker("Ora");
        assertEquals(Locale.ITALY, timePicker.getLocale());
        assertEquals("Ora", timePicker.getLabel());
    }
}
