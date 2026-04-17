package it.attocchi.views.docente;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import it.attocchi.entity.Corso;
import it.attocchi.entity.Docente;
import it.attocchi.entity.Lezione;
import it.attocchi.entity.Presenza;
import it.attocchi.entity.User;
import it.attocchi.repository.DocenteRepository;
import it.attocchi.repository.UserRepository;
import it.attocchi.security.SecurityService;
import it.attocchi.service.CorsoService;
import it.attocchi.service.LezioneService;
import it.attocchi.service.PresenzaService;
import it.attocchi.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;

@Route(value = "lezioni-docente", layout = MainLayout.class)
@PageTitle("Le Mie Lezioni")
@RolesAllowed("DOCENTE")
public class LezioniDocenteView extends VerticalLayout {

    private final LezioneService lezioneService;
    private final PresenzaService presenzaService;
    private final SecurityService securityService;
    private final DocenteRepository docenteRepository;
    private final UserRepository userRepository;
    private final CorsoService corsoService;

    private final Grid<Lezione> gridLezioni = new Grid<>(Lezione.class, false);
    private Docente docenteCorrente;

    public LezioniDocenteView(LezioneService lezioneService, PresenzaService presenzaService,
                             SecurityService securityService, DocenteRepository docenteRepository,
                             UserRepository userRepository, CorsoService corsoService) {
        this.lezioneService = lezioneService;
        this.presenzaService = presenzaService;
        this.securityService = securityService;
        this.docenteRepository = docenteRepository;
        this.userRepository = userRepository;
        this.corsoService = corsoService;

        setSizeFull();
        loadDocenteCorrente();
        configureGrid();

        add(gridLezioni);
        updateGrid();
    }

    private void loadDocenteCorrente() {
        String username = securityService.getAuthenticatedUser().getUsername();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user != null) {
            docenteCorrente = docenteRepository.findByUserId(user.getId()).orElse(null);
        }
    }

    private void configureGrid() {
        gridLezioni.addColumn(lezione -> lezione.getCorso().getNome()).setHeader("Corso").setSortable(true);
        gridLezioni.addColumn(Lezione::getData).setHeader("Data").setSortable(true);
        gridLezioni.addColumn(Lezione::getNumeroLezione).setHeader("N. Lezione");
        gridLezioni.addColumn(Lezione::getOrarioInizio).setHeader("Orario");
        gridLezioni.addColumn(lezione -> lezione.isCompletata() ? "Sì" : "No").setHeader("Completata");

        gridLezioni.addComponentColumn(lezione -> {
            Button presenzeButton = new Button("Gestisci Presenze", e -> openPresenzeDialog(lezione));
            presenzeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            return presenzeButton;
        }).setHeader("Azioni");
    }

    private void openPresenzeDialog(Lezione lezione) {
        Dialog dialog = new Dialog();
        dialog.setWidth("800px");

        VerticalLayout layout = new VerticalLayout();
        layout.add(new com.vaadin.flow.component.html.H3("Presenze - " + lezione.getCorso().getNome() + " - Lezione " + lezione.getNumeroLezione()));

        Grid<Presenza> gridPresenze = new Grid<>(Presenza.class, false);
        gridPresenze.addColumn(presenza -> presenza.getStudente().getNomeCompleto()).setHeader("Studente");
        gridPresenze.addColumn(Presenza::getStato).setHeader("Stato");
        gridPresenze.addColumn(Presenza::getNote).setHeader("Note");

        gridPresenze.addComponentColumn(presenza -> {
            ComboBox<Presenza.StatoPresenza> statoCombo = new ComboBox<>();
            statoCombo.setItems(Presenza.StatoPresenza.values());
            statoCombo.setItemLabelGenerator(Presenza.StatoPresenza::getDescrizione);
            statoCombo.setValue(presenza.getStato());
            statoCombo.addValueChangeListener(e -> {
                presenza.setStato(e.getValue());
                presenzaService.save(presenza);
                Notification.show("Presenza aggiornata");
            });

            TextArea note = new TextArea();
            note.setWidth("200px");
            note.setValue(presenza.getNote() != null ? presenza.getNote() : "");
            note.addValueChangeListener(e -> {
                presenza.setNote(e.getValue());
                presenzaService.save(presenza);
            });

            return new HorizontalLayout(statoCombo, note);
        }).setHeader("Modifica");

        List<Presenza> presenze = presenzaService.findByLezione(lezione.getId());
        gridPresenze.setItems(presenze);

        Button completaButton = new Button("Segna Lezione Completata", e -> {
            lezione.setCompletata(true);
            lezioneService.save(lezione);
            dialog.close();
            updateGrid();
            Notification.show("Lezione completata");
        });
        completaButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        completaButton.setEnabled(!lezione.isCompletata());

        Button closeButton = new Button("Chiudi", e -> dialog.close());

        layout.add(gridPresenze, new HorizontalLayout(completaButton, closeButton));
        dialog.add(layout);
        dialog.open();
    }

    private void updateGrid() {
        if (docenteCorrente != null) {
            List<Corso> corsi = corsoService.findByDocente(docenteCorrente.getId());
            List<Lezione> lezioni = corsi.stream()
                .flatMap(corso -> lezioneService.findByCorso(corso.getId()).stream())
                .toList();
            gridLezioni.setItems(lezioni);
        }
    }
}
