package it.attocchi.views.genitore;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import it.attocchi.entity.Corso;
import it.attocchi.entity.Genitore;
import it.attocchi.entity.Presenza;
import it.attocchi.entity.Studente;
import it.attocchi.entity.User;
import it.attocchi.repository.GenitoreRepository;
import it.attocchi.repository.UserRepository;
import it.attocchi.security.SecurityService;
import it.attocchi.service.CorsoService;
import it.attocchi.service.PresenzaService;
import it.attocchi.service.StudenteService;
import it.attocchi.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Route(value = "figli", layout = MainLayout.class)
@PageTitle("I Miei Figli")
@RolesAllowed("GENITORE")
public class FigliView extends VerticalLayout {

    private final StudenteService studenteService;
    private final CorsoService corsoService;
    private final PresenzaService presenzaService;
    private final SecurityService securityService;
    private final GenitoreRepository genitoreRepository;
    private final UserRepository userRepository;

    private final Grid<Studente> gridFigli = new Grid<>(Studente.class, false);
    private Genitore genitoreCorrente;

    public FigliView(StudenteService studenteService, CorsoService corsoService,
                    PresenzaService presenzaService, SecurityService securityService,
                    GenitoreRepository genitoreRepository, UserRepository userRepository) {
        this.studenteService = studenteService;
        this.corsoService = corsoService;
        this.presenzaService = presenzaService;
        this.securityService = securityService;
        this.genitoreRepository = genitoreRepository;
        this.userRepository = userRepository;

        setSizeFull();
        loadGenitoreCorrente();
        configureGrid();

        add(gridFigli);
        updateGrid();
    }

    private void loadGenitoreCorrente() {
        String username = securityService.getAuthenticatedUser().getUsername();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user != null) {
            genitoreCorrente = genitoreRepository.findByUserId(user.getId()).orElse(null);
        }
    }

    private void configureGrid() {
        gridFigli.addColumn(Studente::getNomeCompleto).setHeader("Nome").setSortable(true);
        gridFigli.addColumn(Studente::getDataNascita).setHeader("Data Nascita");
        gridFigli.addColumn(studente -> studente.getCorsi().size()).setHeader("N. Corsi");

        gridFigli.addComponentColumn(studente -> {
            Button viewButton = new Button("Visualizza Dettagli", e -> openDettagliDialog(studente));
            viewButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            return viewButton;
        }).setHeader("Azioni");
    }

    private void openDettagliDialog(Studente studente) {
        Dialog dialog = new Dialog();
        dialog.setWidth("1000px");

        VerticalLayout layout = new VerticalLayout();
        layout.add(new H3("Profilo di " + studente.getNomeCompleto()));

        // Grid dei corsi
        Grid<Corso> gridCorsi = new Grid<>(Corso.class, false);
        gridCorsi.addColumn(Corso::getNome).setHeader("Corso");
        gridCorsi.addColumn(corso -> corso.getDocente() != null ? corso.getDocente().getNomeCompleto() : "").setHeader("Docente");
        gridCorsi.addColumn(Corso::getNumeroLezioni).setHeader("N. Lezioni");
        gridCorsi.addColumn(Corso::getCostoTotale).setHeader("Costo Totale");

        gridCorsi.addComponentColumn(corso -> {
            Button presenzeButton = new Button("Presenze", e -> openPresenzeDialog(studente, corso));
            Button downloadButton = new Button("Scarica Riepilogo");

            Anchor download = new Anchor(generateRiepilogo(studente, corso), "");
            download.getElement().setAttribute("download", true);
            download.add(downloadButton);

            return new HorizontalLayout(presenzeButton, download);
        }).setHeader("Azioni");

        List<Corso> corsi = corsoService.findByStudente(studente.getId());
        gridCorsi.setItems(corsi);

        Button closeButton = new Button("Chiudi", e -> dialog.close());

        layout.add(gridCorsi, closeButton);
        dialog.add(layout);
        dialog.open();
    }

    private void openPresenzeDialog(Studente studente, Corso corso) {
        Dialog dialog = new Dialog();
        dialog.setWidth("800px");

        VerticalLayout layout = new VerticalLayout();
        layout.add(new H3("Presenze - " + corso.getNome() + " - " + studente.getNomeCompleto()));

        Grid<Presenza> gridPresenze = new Grid<>(Presenza.class, false);
        gridPresenze.addColumn(presenza -> presenza.getLezione().getData()).setHeader("Data");
        gridPresenze.addColumn(presenza -> presenza.getLezione().getNumeroLezione()).setHeader("N. Lezione");
        gridPresenze.addColumn(presenza -> presenza.getStato().getDescrizione()).setHeader("Stato");
        gridPresenze.addColumn(Presenza::getNote).setHeader("Note");

        List<Presenza> presenze = presenzaService.findByCorsoAndStudente(corso.getId(), studente.getId());
        gridPresenze.setItems(presenze);

        // Calcola statistiche
        long totaleLezioni = presenze.size();
        long presenti = presenze.stream().filter(p -> p.getStato() == Presenza.StatoPresenza.PRESENTE).count();
        long assenzeGiustificate = presenze.stream().filter(p -> p.getStato() == Presenza.StatoPresenza.ASSENTE_GIUSTIFICATA).count();
        long assenzeIngiustificate = presenze.stream().filter(p -> p.getStato() == Presenza.StatoPresenza.ASSENTE_INGIUSTIFICATA).count();

        VerticalLayout stats = new VerticalLayout();
        stats.add(new com.vaadin.flow.component.html.Span("Totale lezioni: " + totaleLezioni));
        stats.add(new com.vaadin.flow.component.html.Span("Presenze: " + presenti));
        stats.add(new com.vaadin.flow.component.html.Span("Assenze giustificate: " + assenzeGiustificate));
        stats.add(new com.vaadin.flow.component.html.Span("Assenze ingiustificate: " + assenzeIngiustificate));

        Button closeButton = new Button("Chiudi", e -> dialog.close());

        layout.add(stats, gridPresenze, closeButton);
        dialog.add(layout);
        dialog.open();
    }

    private StreamResource generateRiepilogo(Studente studente, Corso corso) {
        return new StreamResource("riepilogo-" + corso.getNome() + "-" + studente.getNomeCompleto() + ".txt",
            () -> {
                StringBuilder sb = new StringBuilder();
                sb.append("RIEPILOGO CORSO\n");
                sb.append("===================\n\n");
                sb.append("Studente: ").append(studente.getNomeCompleto()).append("\n");
                sb.append("Corso: ").append(corso.getNome()).append("\n");
                sb.append("Docente: ").append(corso.getDocente() != null ? corso.getDocente().getNomeCompleto() : "N/D").append("\n");
                sb.append("Data inizio: ").append(corso.getDataInizio()).append("\n");
                sb.append("Numero lezioni: ").append(corso.getNumeroLezioni()).append("\n");
                sb.append("Durata lezione: ").append(corso.getDurataLezioneMinuti()).append(" minuti\n\n");

                BigDecimal costoTotale = corso.getCostoTotale() != null ? corso.getCostoTotale() : BigDecimal.ZERO;
                sb.append("COSTO TOTALE DA PAGARE: € ").append(costoTotale).append("\n\n");

                sb.append("PRESENZE\n");
                sb.append("===================\n");

                List<Presenza> presenze = presenzaService.findByCorsoAndStudente(corso.getId(), studente.getId());
                for (Presenza presenza : presenze) {
                    sb.append("Lezione ").append(presenza.getLezione().getNumeroLezione())
                      .append(" - ").append(presenza.getLezione().getData())
                      .append(" - ").append(presenza.getStato().getDescrizione());
                    if (presenza.getNote() != null && !presenza.getNote().isEmpty()) {
                        sb.append(" (").append(presenza.getNote()).append(")");
                    }
                    sb.append("\n");
                }

                long totaleLezioni = presenze.size();
                long presenti = presenze.stream().filter(p -> p.getStato() == Presenza.StatoPresenza.PRESENTE).count();
                long assenzeGiustificate = presenze.stream().filter(p -> p.getStato() == Presenza.StatoPresenza.ASSENTE_GIUSTIFICATA).count();
                long assenzeIngiustificate = presenze.stream().filter(p -> p.getStato() == Presenza.StatoPresenza.ASSENTE_INGIUSTIFICATA).count();

                sb.append("\nSTATISTICHE\n");
                sb.append("===================\n");
                sb.append("Totale lezioni: ").append(totaleLezioni).append("\n");
                sb.append("Presenze: ").append(presenti).append("\n");
                sb.append("Assenze giustificate: ").append(assenzeGiustificate).append("\n");
                sb.append("Assenze ingiustificate: ").append(assenzeIngiustificate).append("\n");

                return new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));
            });
    }

    private void updateGrid() {
        if (genitoreCorrente != null) {
            List<Studente> figli = studenteService.findByGenitore(genitoreCorrente.getId());
            gridFigli.setItems(figli);
        }
    }
}
