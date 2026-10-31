package it.attocchi.views.admin;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.function.SerializableSupplier;
import it.attocchi.entity.Corso;
import it.attocchi.security.SecurityService;
import it.attocchi.service.CorsoService;
import it.attocchi.service.LezioneService;
import it.attocchi.service.PresenzaService;
import it.attocchi.views.ElencoLezioni;
import it.attocchi.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;

import java.util.Comparator;
import java.util.List;

/**
 * Lezioni di tutti i corsi attivi per admin e segreteria, con le stesse funzioni di "Le mie lezioni"
 * (presenze, completamento, non effettuate, nuove lezioni); eliminare le lezioni aggiunte è solo dell'admin. L'admin
 * trova nel filtro anche i corsi non attivi, per gestirne lezioni e presenze.
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
        SerializableSupplier<List<Corso>> corsi = securityService.hasRole("ADMIN")
            ? () -> corsoService.findAll().stream()
                .sorted(Comparator.comparing((Corso c) -> !c.isAttivo()).thenComparing(Corso::getNome)).toList()
            : corsoService::findAllAttivi;
        elenco = new ElencoLezioni(lezioneService, presenzaService, lezioneService::findDeiCorsiAttivi,
            corsi, securityService.getUsername().orElse(""), true);
        add(elenco);
        elenco.addClassNames("grow");
    }

    /**
     * Con {@code ?lezione=ID} (da "Gestisci presenze" del calendario o dalla prossima lezione) apre subito le presenze
     * di quella lezione, con {@code &gruppo=…} sulla scheda di quel gruppo.
     */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        elenco.mostraStato(event.getLocation().getQueryParameters());
        elenco.apriPresenze(event.getLocation().getQueryParameters());
    }
}
