package it.attocchi.views.studente;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import it.attocchi.entity.Corso;
import it.attocchi.entity.Presenza;
import it.attocchi.entity.Studente;
import it.attocchi.entity.User;
import it.attocchi.repository.StudenteRepository;
import it.attocchi.repository.UserRepository;
import it.attocchi.security.SecurityService;
import it.attocchi.service.CorsoService;
import it.attocchi.service.PresenzaService;
import it.attocchi.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;

@Route(value = "miei-corsi", layout = MainLayout.class)
@PageTitle("I Miei Corsi")
@RolesAllowed("STUDENTE")
public class MieiCorsiView extends VerticalLayout {

    private final CorsoService corsoService;
    private final PresenzaService presenzaService;
    private final SecurityService securityService;
    private final StudenteRepository studenteRepository;
    private final UserRepository userRepository;

    private final Grid<Corso> gridCorsi = new Grid<>(Corso.class, false);
    private Studente studenteCorrente;

    public MieiCorsiView(CorsoService corsoService, PresenzaService presenzaService,
                        SecurityService securityService, StudenteRepository studenteRepository,
                        UserRepository userRepository) {
        this.corsoService = corsoService;
        this.presenzaService = presenzaService;
        this.securityService = securityService;
        this.studenteRepository = studenteRepository;
        this.userRepository = userRepository;

        setSizeFull();
        loadStudenteCorrente();
        configureGrid();

        add(gridCorsi);
        updateGrid();
    }

    private void loadStudenteCorrente() {
        String username = securityService.getAuthenticatedUser().getUsername();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user != null) {
            studenteCorrente = studenteRepository.findByUserId(user.getId()).orElse(null);
        }
    }

    private void configureGrid() {
        gridCorsi.addColumn(Corso::getNome).setHeader("Corso").setSortable(true);
        gridCorsi.addColumn(corso -> corso.getDocente() != null ? corso.getDocente().getNomeCompleto() : "").setHeader("Docente");
        gridCorsi.addColumn(Corso::getDataInizio).setHeader("Data Inizio");
        gridCorsi.addColumn(Corso::getNumeroLezioni).setHeader("N. Lezioni");
        gridCorsi.addColumn(Corso::getDurataLezioneMinuti).setHeader("Durata (min)");

        gridCorsi.addComponentColumn(corso -> {
            Button viewButton = new Button("Visualizza Presenze", e -> openPresenzeDialog(corso));
            return viewButton;
        }).setHeader("Azioni");
    }

    private void openPresenzeDialog(Corso corso) {
        Dialog dialog = new Dialog();
        dialog.setWidth("800px");

        VerticalLayout layout = new VerticalLayout();
        layout.add(new H3("Presenze - " + corso.getNome()));

        Grid<Presenza> gridPresenze = new Grid<>(Presenza.class, false);
        gridPresenze.addColumn(presenza -> presenza.getLezione().getData()).setHeader("Data");
        gridPresenze.addColumn(presenza -> presenza.getLezione().getNumeroLezione()).setHeader("N. Lezione");
        gridPresenze.addColumn(presenza -> presenza.getStato().getDescrizione()).setHeader("Stato");
        gridPresenze.addColumn(Presenza::getNote).setHeader("Note");

        if (studenteCorrente != null) {
            List<Presenza> presenze = presenzaService.findByCorsoAndStudente(corso.getId(), studenteCorrente.getId());
            gridPresenze.setItems(presenze);
        }

        Button closeButton = new Button("Chiudi", e -> dialog.close());

        layout.add(gridPresenze, closeButton);
        dialog.add(layout);
        dialog.open();
    }

    private void updateGrid() {
        if (studenteCorrente != null) {
            List<Corso> corsi = corsoService.findByStudente(studenteCorrente.getId());
            gridCorsi.setItems(corsi);
        }
    }
}
