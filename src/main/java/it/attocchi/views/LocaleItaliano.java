package it.attocchi.views;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.springframework.stereotype.Component;

/**
 * Lingua italiana per ogni UI, indipendentemente dalla lingua del browser: i componenti
 * (numeri, date, ore) la usano come locale predefinito.
 */
@Component
public class LocaleItaliano implements VaadinServiceInitListener {

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(e -> e.getUI().setLocale(ViewSupport.LOCALE));
    }
}
