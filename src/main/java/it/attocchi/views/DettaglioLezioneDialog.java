package it.attocchi.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.QueryParameters;
import it.attocchi.entity.Insegnante;
import it.attocchi.entity.Lezione;
import it.attocchi.views.admin.LezioniView;
import it.attocchi.views.insegnante.LezioniInsegnanteView;

import java.time.LocalTime;
import java.util.stream.Collectors;

/**
 * Dettagli in sola lettura di una lezione (Calendario e prossima lezione della dashboard): numero, data e orario,
 * insegnanti, stato, note; le note per lo staff e "Gestisci presenze" solo ad admin, segreteria e insegnanti del
 * corso (la seconda porta a Lezioni o Le Mie Lezioni con {@code ?lezione=ID}).
 */
public final class DettaglioLezioneDialog {

    private DettaglioLezioneDialog() {
    }

    /**
     * @param perChi   l'allievo per il genitore, {@code null} negli altri casi
     * @param orario   l'orario della lezione dell'allievo; {@code null} = quello della lezione (con la fine)
     * @param numero   il numero della lezione già formattato ({@code Formati.numeroLezione})
     * @param gestione admin o segreteria
     */
    public static void apri(Lezione lezione, String perChi, LocalTime orario, String numero, String username, boolean gestione) {
        Dialog dialog = ViewSupport.dialog(lezione.getCorso().getNome(), "440px");
        String quando = orario != null ? ", " + Formati.ora(orario)
            : lezione.getOrarioInizio() != null ? ", " + Formati.ora(lezione.getOrarioInizio())
                + (lezione.getOrarioFine() != null ? "–" + Formati.ora(lezione.getOrarioFine()) : "") : "";
        VerticalLayout contenuto = new VerticalLayout(
            riga("Lezione", numero + (lezione.getRecuperoDi() != null ? " (recupero)" : "")),
            riga("Data", Formati.data(lezione.getData()) + quando),
            riga("Insegnanti", lezione.getInsegnantiPresenti().isEmpty() ? lezione.getCorso().getNomiInsegnanti()
                : lezione.getInsegnantiPresenti().stream().map(Insegnante::getNomeCompleto).sorted()
                    .collect(Collectors.joining(", "))));
        if (lezione.isSpostata()) {
            contenuto.add(riga("Spostata dal", Formati.data(lezione.getDataOriginale())));
        }
        if (perChi != null) {
            contenuto.add(riga("Allievo", perChi));
        }
        contenuto.add(riga("Stato", badge(lezione)));
        if (lezione.isNonEffettuata() && lezione.getMotivoNonEffettuata() != null) {
            contenuto.add(riga("Motivo", lezione.getMotivoNonEffettuata()));
        }
        boolean staff = gestione || lezione.getCorso().haInsegnante(username);
        // Le note per lo staff solo ad admin, segreteria e insegnanti del corso
        if (lezione.getNotePubbliche() != null) {
            contenuto.add(riga(staff ? "Note pubbliche" : "Note", lezione.getNotePubbliche()));
        }
        if (staff && lezione.getNote() != null) {
            contenuto.add(riga("Note staff", lezione.getNote()));
        }
        contenuto.addClassNames("p-0 gap-2");
        dialog.add(contenuto);
        dialog.getFooter().add(ViewSupport.closeButton(dialog, "Chiudi"));
        if (staff) {
            Button presenze = new Button("Gestisci presenze", VaadinIcon.CLIPBOARD_CHECK.create(), e -> {
                dialog.close();
                apriPresenze(lezione, gestione);
            });
            presenze.addThemeVariants(ButtonVariant.PRIMARY);
            dialog.getFooter().add(presenze);
        }
        dialog.open();
    }

    /** Registro delle presenze della lezione: Lezioni per admin e segreteria, Le Mie Lezioni per l'insegnante. */
    public static void apriPresenze(Lezione lezione, boolean gestione) {
        QueryParameters parametri = QueryParameters.of(ElencoLezioni.PARAMETRO_LEZIONE, String.valueOf(lezione.getId()));
        UI.getCurrent().navigate(gestione ? LezioniView.class : LezioniInsegnanteView.class, parametri);
    }

    private static Badge badge(Lezione lezione) {
        if (lezione.isCompletata()) {
            Badge badge = new Badge("Completata", VaadinIcon.CHECK.create());
            badge.addThemeVariants(BadgeVariant.SUCCESS);
            return badge;
        }
        if (lezione.isNonEffettuata()) {
            Badge badge = new Badge("Non effettuata", VaadinIcon.BAN.create());
            badge.addThemeVariants(BadgeVariant.ERROR);
            return badge;
        }
        return new Badge("Da fare", VaadinIcon.CLOCK.create());
    }

    private static Div riga(String etichetta, String valore) {
        return riga(etichetta, new Span(valore));
    }

    private static Div riga(String etichetta, Component valore) {
        Span nome = new Span(etichetta);
        nome.addClassNames("w-24 shrink-0", Stili.TESTO_SECONDARIO);
        Div riga = new Div(nome, valore);
        riga.addClassNames("flex items-baseline gap-2");
        return riga;
    }
}
