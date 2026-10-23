package it.attocchi.views.admin;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import it.attocchi.security.SecurityService;
import it.attocchi.service.CorsoService;
import it.attocchi.service.LezioneService;
import it.attocchi.service.PresenzaService;
import it.attocchi.views.ElencoLezioni;
import it.attocchi.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;

/**
 * Lezioni di tutti i corsi attivi per admin e segreteria, con le stesse funzioni di "Le Mie Lezioni"
 * (presenze, completamento, non effettuate, nuove lezioni); eliminare le lezioni aggiunte è solo dell'admin.
 */
@Route(value = "lezioni", layout = MainLayout.class)
@PageTitle("Lezioni")
@Menu(title = "Lezioni", order = 10.2, icon = "vaadin:calendar-clock")
@RolesAllowed({"ADMIN", "SEGRETERIA"})
public class LezioniView extends VerticalLayout implements BeforeEnterObserver {

    private final ElencoLezioni elenco;

    public LezioniView(LezioneService lezioneService, PresenzaService presenzaService, CorsoService corsoService,
                       SecurityService securityService) {
        addClassNames("size-full");
        elenco = new ElencoLezioni(lezioneService, presenzaService, lezioneService::findDeiCorsiAttivi,
            corsoService::findAllAttivi, securityService.getUsername().orElse(""), true);
        add(elenco);
        elenco.addClassNames("grow");
    }

    /** Con {@code ?lezione=ID} (da "Gestisci presenze" del calendario) apre subito le presenze di quella lezione. */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        event.getLocation().getQueryParameters().getSingleParameter(ElencoLezioni.PARAMETRO_LEZIONE)
            .flatMap(ElencoLezioni::idLezione)
            .ifPresent(elenco::apriPresenze);
    }
}
