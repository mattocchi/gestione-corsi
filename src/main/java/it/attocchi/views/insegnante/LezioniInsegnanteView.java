package it.attocchi.views.insegnante;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import it.attocchi.entity.Corso;
import it.attocchi.entity.Insegnante;
import it.attocchi.security.SecurityService;
import it.attocchi.service.CorsoService;
import it.attocchi.service.InsegnanteService;
import it.attocchi.service.LezioneService;
import it.attocchi.service.PresenzaService;
import it.attocchi.views.ElencoLezioni;
import it.attocchi.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;

/** Lezioni dei corsi attivi dell'insegnante (il suo registro). */
@Route(value = "lezioni-insegnante", layout = MainLayout.class)
@PageTitle("Le Mie Lezioni")
@Menu(title = "Le Mie Lezioni", order = 20, icon = "vaadin:calendar")
@RolesAllowed("INSEGNANTE")
public class LezioniInsegnanteView extends VerticalLayout implements BeforeEnterObserver {

    private final ElencoLezioni elenco;

    public LezioniInsegnanteView(LezioneService lezioneService, PresenzaService presenzaService,
                                 SecurityService securityService, InsegnanteService insegnanteService,
                                 CorsoService corsoService) {
        addClassNames("size-full");
        Insegnante insegnante = securityService.getUsername().flatMap(insegnanteService::findByUsername).orElse(null);
        elenco = new ElencoLezioni(lezioneService, presenzaService,
            () -> insegnante != null ? lezioneService.findByInsegnante(insegnante.getId()) : List.of(),
            () -> insegnante != null
                ? corsoService.findByInsegnante(insegnante.getId()).stream().filter(Corso::isAttivo).toList() : List.of(),
            securityService.getUsername().orElse(""));
        add(elenco);
        elenco.addClassNames("grow");
    }

    /**
     * Con {@code ?lezione=ID} (da "Gestisci presenze" del calendario o dalla prossima lezione) apre subito le presenze
     * di quella lezione, con {@code &gruppo=…} sulla scheda di quel gruppo.
     */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        elenco.apriPresenze(event.getLocation().getQueryParameters());
    }
}
