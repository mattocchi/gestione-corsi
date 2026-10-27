package it.attocchi.views.admin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.VaadinService;
import it.attocchi.entity.Corso;
import it.attocchi.service.CorsoService;
import it.attocchi.service.CorsoService.AggiornamentoLezioni;
import it.attocchi.views.Formati;
import it.attocchi.views.Stili;
import it.attocchi.views.ViewSupport;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * "Aggiorna lezioni" di un corso: anteprima di cosa cambia (lezioni da aggiungere e da togliere, quante restano
 * com'erano) e conferma; senza cambiamenti solo un avviso. Usato da Gestione Corsi e dopo le modifiche alle festività.
 */
public final class AggiornaLezioniDialog {

    private AggiornaLezioniDialog() {
    }

    public static void apri(Corso corso, Runnable dopo) {
        CorsoService corsoService = VaadinService.getCurrent().getInstantiator().getOrCreate(CorsoService.class);
        AggiornamentoLezioni anteprima;
        try {
            anteprima = corsoService.anteprimaAggiornamento(corso.getId());
        } catch (RuntimeException ex) {
            Notification.show("Impossibile aggiornare le lezioni: " + ex.getMessage());
            return;
        }
        if (!anteprima.cambia()) {
            Notification.show("Le lezioni di " + corso.getNome() + " sono già aggiornate");
            return;
        }
        Dialog dialog = ViewSupport.dialog("Aggiorna lezioni – " + corso.getNome(), "520px");
        VerticalLayout contenuto = new VerticalLayout();
        contenuto.addClassNames("p-0 gap-2");
        if (!anteprima.aggiunte().isEmpty()) {
            contenuto.add(riga("Da aggiungere (" + anteprima.aggiunte().size() + ")", anteprima.aggiunte()));
        }
        if (!anteprima.tolte().isEmpty()) {
            contenuto.add(riga("Da togliere (" + anteprima.tolte().size() + "): festività o oltre la fine", anteprima.tolte()));
        }
        Span restano = new Span("Restano com'erano, con note e presenze: " + anteprima.invariate() + " lezioni da fare. "
            + "Le lezioni svolte, non effettuate, spostate o aggiunte non cambiano.");
        restano.addClassNames(Stili.TESTO_SECONDARIO);
        contenuto.add(restano);
        Button aggiorna = new Button("Aggiorna", e -> {
            try {
                corsoService.aggiornaLezioni(corso.getId());
                dialog.close();
                Notification.show("Lezioni di " + corso.getNome() + " aggiornate");
                dopo.run();
            } catch (RuntimeException ex) {
                Notification.show("Aggiornamento non riuscito: " + ex.getMessage());
            }
        });
        aggiorna.addThemeVariants(ButtonVariant.PRIMARY);
        dialog.add(contenuto);
        dialog.getFooter().add(ViewSupport.closeButton(dialog, "Annulla"), aggiorna);
        dialog.open();
    }

    /**
     * Dopo una modifica alle festività: se il calendario di qualche corso attivo è cambiato, l'elenco dei corsi con
     * quante lezioni si aggiungono e si tolgono e "Aggiorna tutti"; altrimenti niente.
     */
    public static void proponiPerFestivita() {
        CorsoService corsoService = VaadinService.getCurrent().getInstantiator().getOrCreate(CorsoService.class);
        List<CorsoService.CorsoDaAggiornare> corsi = corsoService.corsiDaAggiornare();
        if (corsi.isEmpty()) {
            return;
        }
        Dialog dialog = ViewSupport.dialog("Aggiornare le lezioni?", "520px");
        VerticalLayout contenuto = new VerticalLayout();
        contenuto.addClassNames("p-0 gap-2");
        Span spiegazione = new Span("Le festività cambiano il calendario di questi corsi attivi:");
        contenuto.add(spiegazione);
        for (CorsoService.CorsoDaAggiornare corso : corsi) {
            AggiornamentoLezioni anteprima = corso.anteprima();
            String cosa = (anteprima.tolte().isEmpty() ? "" : "togliere " + anteprima.tolte().stream().map(Formati::data)
                .collect(Collectors.joining(", ")))
                + (!anteprima.tolte().isEmpty() && !anteprima.aggiunte().isEmpty() ? "; " : "")
                + (anteprima.aggiunte().isEmpty() ? "" : "aggiungere " + anteprima.aggiunte().size()
                    + (anteprima.aggiunte().size() == 1 ? " lezione" : " lezioni"));
            contenuto.add(riga(corso.corso().getNome(), cosa));
        }
        Span nota = new Span("Le lezioni da fare ancora valide restano com'erano, con note e presenze.");
        nota.addClassNames(Stili.TESTO_SECONDARIO);
        contenuto.add(nota);
        Button aggiorna = new Button("Aggiorna tutti", e -> {
            try {
                corsi.forEach(c -> corsoService.aggiornaLezioni(c.corso().getId()));
                dialog.close();
                Notification.show("Lezioni aggiornate: " + corsi.size() + (corsi.size() == 1 ? " corso" : " corsi"));
            } catch (RuntimeException ex) {
                Notification.show("Aggiornamento non riuscito: " + ex.getMessage());
            }
        });
        aggiorna.addThemeVariants(ButtonVariant.PRIMARY);
        dialog.add(contenuto);
        dialog.getFooter().add(ViewSupport.closeButton(dialog, "Non ora"), aggiorna);
        dialog.open();
    }

    private static VerticalLayout riga(String titolo, String testo) {
        Span etichetta = new Span(titolo);
        etichetta.addClassNames("font-(--aura-font-weight-semibold)");
        VerticalLayout riga = new VerticalLayout(etichetta, new Span(testo));
        riga.addClassNames("p-0 gap-0");
        return riga;
    }

    private static VerticalLayout riga(String titolo, List<LocalDate> date) {
        Span etichetta = new Span(titolo);
        etichetta.addClassNames("font-(--aura-font-weight-semibold)");
        Span elenco = new Span(date.stream().map(Formati::data).collect(Collectors.joining(", ")));
        VerticalLayout riga = new VerticalLayout(etichetta, elenco);
        riga.addClassNames("p-0 gap-0");
        return riga;
    }
}
