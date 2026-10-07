package it.attocchi.views.genitore;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;
import it.attocchi.entity.Corso;
import it.attocchi.entity.Genitore;
import it.attocchi.entity.Presenza;
import it.attocchi.entity.Studente;
import it.attocchi.security.SecurityService;
import it.attocchi.service.CorsoService;
import it.attocchi.service.GenitoreService;
import it.attocchi.service.PresenzaService;
import it.attocchi.service.StudenteService;
import it.attocchi.views.Formati;
import it.attocchi.views.MainLayout;
import it.attocchi.views.PresenzeStudenteDialog;
import it.attocchi.views.ViewSupport;
import jakarta.annotation.security.RolesAllowed;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Route(value = "figli", layout = MainLayout.class)
@PageTitle("I Miei Figli")
@Menu(title = "I Miei Figli", order = 40, icon = "vaadin:family")
@RolesAllowed("GENITORE")
public class FigliView extends VerticalLayout {

    private final StudenteService studenteService;
    private final CorsoService corsoService;
    private final PresenzaService presenzaService;

    private final Grid<Studente> gridFigli = new Grid<>(Studente.class, false);
    private Genitore genitoreCorrente;

    public FigliView(StudenteService studenteService, CorsoService corsoService,
                     PresenzaService presenzaService, SecurityService securityService,
                     GenitoreService genitoreService) {
        this.studenteService = studenteService;
        this.corsoService = corsoService;
        this.presenzaService = presenzaService;

        setSizeFull();
        genitoreCorrente = securityService.getUsername().flatMap(genitoreService::findByUsername).orElse(null);
        configureGrid();

        add(gridFigli);
        expand(gridFigli);
        updateGrid();
    }

    private void configureGrid() {
        gridFigli.addColumn(Studente::getNomeCompleto).setHeader("Nome").setSortable(true);
        var dataNascita = gridFigli.addColumn(studente -> Formati.data(studente.getDataNascita())).setHeader("Data Nascita")
            .setComparator(Studente::getDataNascita);
        gridFigli.addColumn(studente -> studente.getCorsi().size()).setHeader("N. Corsi").setAutoWidth(true).setFlexGrow(0);

        ViewSupport.configureActionsColumn(gridFigli.addComponentColumn(studente ->
            ViewSupport.actions(ViewSupport.iconButton(VaadinIcon.BOOK, "Visualizza Dettagli", e -> openDettagliDialog(studente)))));

        ViewSupport.hideOnMobile(gridFigli, dataNascita);
    }

    private void openDettagliDialog(Studente studente) {
        Dialog dialog = ViewSupport.dialog("Corsi di " + studente.getNomeCompleto(), "1000px");

        Grid<Corso> gridCorsi = new Grid<>(Corso.class, false);
        gridCorsi.addColumn(Corso::getNome).setHeader("Corso");
        var docente = gridCorsi.addColumn(corso -> corso.getDocente() != null ? corso.getDocente().getNomeCompleto() : "").setHeader("Docente");
        var numeroLezioni = gridCorsi.addColumn(Corso::getNumeroLezioni).setHeader("N. Lezioni");
        gridCorsi.addColumn(Corso::getCostoTotale).setHeader("Costo Totale").setAutoWidth(true).setFlexGrow(0);

        ViewSupport.configureActionsColumn(gridCorsi.addComponentColumn(corso -> {
            Button presenzeButton = ViewSupport.iconButton(VaadinIcon.CLIPBOARD_CHECK, "Presenze", e -> openPresenzeDialog(studente, corso));

            Anchor download = new Anchor(generateRiepilogo(studente, corso), "");
            download.getElement().setAttribute("download", true);
            download.add(ViewSupport.iconButton(VaadinIcon.DOWNLOAD, "Scarica Riepilogo"));

            HorizontalLayout actions = ViewSupport.actions(presenzeButton);
            actions.add(download);
            return actions;
        }));
        ViewSupport.hideOnMobile(gridCorsi, docente, numeroLezioni);

        gridCorsi.setItems(corsoService.findByStudente(studente.getId()));
        gridCorsi.setAllRowsVisible(true);

        dialog.add(gridCorsi);
        dialog.getFooter().add(ViewSupport.closeButton(dialog, "Chiudi"));
        dialog.open();
    }

    private void openPresenzeDialog(Studente studente, Corso corso) {
        List<Presenza> presenze = presenzaService.findByCorsoAndStudente(corso.getId(), studente.getId());
        new PresenzeStudenteDialog(corso.getNome() + " – " + studente.getNomeCompleto(), presenze).open();
    }

    private DownloadHandler generateRiepilogo(Studente studente, Corso corso) {
        return event -> {
            event.setFileName("riepilogo-" + corso.getNome() + "-" + studente.getNomeCompleto() + ".txt");
            event.setContentType("text/plain; charset=UTF-8");
            StringBuilder sb = new StringBuilder();
            sb.append("RIEPILOGO CORSO\n");
            sb.append("===================\n\n");
            sb.append("Studente: ").append(studente.getNomeCompleto()).append("\n");
            sb.append("Corso: ").append(corso.getNome()).append("\n");
            sb.append("Docente: ").append(corso.getDocente() != null ? corso.getDocente().getNomeCompleto() : "N/D").append("\n");
            sb.append("Data inizio: ").append(Formati.data(corso.getDataInizio())).append("\n");
            sb.append("Numero lezioni: ").append(corso.getNumeroLezioni()).append("\n");
            sb.append("Durata lezione: ").append(corso.getDurataLezioneMinuti()).append(" minuti\n\n");

            BigDecimal costoTotale = corso.getCostoTotale() != null ? corso.getCostoTotale() : BigDecimal.ZERO;
            sb.append("COSTO TOTALE DA PAGARE: € ").append(costoTotale).append("\n\n");

            sb.append("PRESENZE\n");
            sb.append("===================\n");

            List<Presenza> presenze = presenzaService.findByCorsoAndStudente(corso.getId(), studente.getId());
            for (Presenza presenza : presenze) {
                sb.append("Lezione ").append(presenza.getLezione().getNumeroLezione())
                  .append(" - ").append(Formati.data(presenza.getLezione().getData()))
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

            try (OutputStream out = event.getOutputStream()) {
                out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
            }
        };
    }

    private void updateGrid() {
        if (genitoreCorrente != null) {
            List<Studente> figli = studenteService.findByGenitore(genitoreCorrente.getId());
            gridFigli.setItems(figli);
        }
    }
}
