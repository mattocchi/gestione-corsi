package it.attocchi.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.springframework.stereotype.Component;

/**
 * Configurazione di ogni UI: lingua italiana indipendentemente da quella del browser (i componenti la usano per numeri,
 * date e ore) e nome dell'applicazione dopo il titolo di ogni pagina nella scheda del browser
 * ("Le Mie Lezioni | Area Corsi Mosson").
 */
@Component
public class LocaleItaliano implements VaadinServiceInitListener {

    static final String SEPARATORE = " | ";

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(e -> {
            UI ui = e.getUI();
            ui.setLocale(ViewSupport.LOCALE);
            // Dopo la risposta del router, che imposta il titolo da @PageTitle
            ui.addAfterNavigationListener(navigazione -> ui.beforeClientResponse(ui, contesto -> {
                String titolo = ui.getInternals().getTitle();
                ui.getPage().setTitle(titoloCompleto(titolo));
            }));
        });
    }

    static String titoloCompleto(String titolo) {
        if (titolo == null || titolo.isBlank()) {
            return MainLayout.NOME_APP;
        }
        return titolo.endsWith(SEPARATORE + MainLayout.NOME_APP) ? titolo : titolo + SEPARATORE + MainLayout.NOME_APP;
    }
}
