package it.attocchi.views.admin;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.VaadinService;
import it.attocchi.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Impostazioni usate di rado (admin e segreteria): anni scolastici, luoghi e festività in una pagina a schede. I
 * vecchi indirizzi delle tre pagine ({@code anni-scolastici}, {@code luoghi}, {@code festivita}) aprono la scheda giusta.
 */
@Route(value = ImpostazioniView.PERCORSO, layout = MainLayout.class)
@RouteAlias(value = "anni-scolastici", layout = MainLayout.class)
@RouteAlias(value = "luoghi", layout = MainLayout.class)
@RouteAlias(value = "festivita", layout = MainLayout.class)
@PageTitle("Impostazioni")
@Menu(title = "Impostazioni", order = 15, icon = "vaadin:cog")
@RolesAllowed({"ADMIN", "SEGRETERIA"})
public class ImpostazioniView extends VerticalLayout implements BeforeEnterObserver {

    public static final String PERCORSO = "impostazioni";

    private final TabSheet schede = new TabSheet();
    /** Scheda per indirizzo (quello vecchio della pagina). */
    private final Map<String, Tab> perIndirizzo = new LinkedHashMap<>();

    public ImpostazioniView() {
        addClassNames("size-full");
        aggiungi("anni-scolastici", "Anni scolastici", AnniScolasticiScheda.class);
        aggiungi("luoghi", "Luoghi", LuoghiScheda.class);
        aggiungi("festivita", "Festività", FestivitaScheda.class);
        schede.addClassNames("grow w-full");
        add(schede);
    }

    /** Le schede si creano con le dipendenze di Spring, come le viste. */
    private void aggiungi(String indirizzo, String titolo, Class<? extends Component> classe) {
        Component contenuto = VaadinService.getCurrent().getInstantiator().getOrCreate(classe);
        contenuto.getElement().getClassList().add("h-full");
        perIndirizzo.put(indirizzo, schede.add(titolo, contenuto));
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // "impostazioni" = la prima scheda
        schede.setSelectedTab(perIndirizzo.getOrDefault(event.getLocation().getFirstSegment(),
            perIndirizzo.values().iterator().next()));
    }
}
