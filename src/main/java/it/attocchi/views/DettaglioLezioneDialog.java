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
import it.attocchi.service.CalendarioService.VoceCalendario;
import it.attocchi.views.admin.LezioniView;
import it.attocchi.views.insegnante.LezioniInsegnanteView;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Dettagli in sola lettura di una lezione (Calendario e prossima lezione della dashboard): numero, data e orario,
 * insegnanti, stato, note; le note per lo staff e "Gestisci presenze" solo ad admin, segreteria e insegnanti del
 * corso (la seconda porta a Lezioni o Le Mie Lezioni con {@code ?lezione=ID} e il gruppo della voce).
 */
public final class DettaglioLezioneDialog {

    private DettaglioLezioneDialog() {
    }

    /**
     * Dettagli di una voce del calendario o della prossima lezione: allievo per il genitore, gruppo ("Senza gruppo"
     * per chi non ne ha) e orario della voce; "Gestisci presenze" apre il registro sulla scheda del gruppo.
     *
     * @param numero   il numero della lezione già formattato ({@code Formati.numeroLezione})
     * @param gestione admin o segreteria
     */
    public static void apri(VoceCalendario voce, String numero, String username, boolean gestione) {
        Lezione lezione = voce.lezione();
        String gruppo = voce.gruppo() != null ? Formati.gruppo(voce.gruppo()) : voce.senzaGruppo() ? "Senza gruppo" : null;
        // Allo stesso orario della lezione si mostra anche la fine
        LocalTime orario = Objects.equals(voce.orario(), lezione.getOrarioInizio()) ? null : voce.orario();
        Dialog dialog = ViewSupport.dialog(lezione.getCorso().getNome(), "440px");
        String quando = orario != null ? ", " + Formati.ora(orario)
            : lezione.getOrarioInizio() != null ? ", " + Formati.ora(lezione.getOrarioInizio())
                + (lezione.getOrarioFine() != null ? "–" + Formati.ora(lezione.getOrarioFine()) : "") : "";
        VerticalLayout contenuto = new VerticalLayout(
            riga("Lezione", numero + (lezione.getRecuperoDi() != null ? " (recupero)" : "")),
            riga("Data", Formati.data(lezione.getData()) + quando),
            riga(lezione.getCorso().getTipo().getChiLoSvolgonoPlurale(), lezione.getInsegnantiPresenti().isEmpty() ? lezione.getCorso().getNomiInsegnanti()
                : lezione.getInsegnantiPresenti().stream().map(Insegnante::getNomeCompleto).sorted()
                    .collect(Collectors.joining(", "))));
        if (lezione.getCorso().isServizio()) {
            contenuto.addComponentAsFirst(riga("Tipo", TipoCorsoUI.badge(lezione.getCorso())));
        }
        if (Formati.luogo(lezione) != null) {
            contenuto.add(riga("Luogo", Formati.luogo(lezione)));
        }
        if (lezione.isSpostata()) {
            contenuto.add(riga("Spostata dal", Formati.data(lezione.getDataOriginale())));
        }
        if (gruppo != null) {
            contenuto.add(riga("Gruppo", gruppo));
        }
        if (voce.perChi() != null) {
            contenuto.add(riga("Allievo", voce.perChi()));
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
                apriPresenze(voce, gestione);
            });
            presenze.addThemeVariants(ButtonVariant.PRIMARY);
            dialog.getFooter().add(presenze);
        }
        dialog.open();
    }

    /** Registro delle presenze della lezione della voce, sulla scheda del suo gruppo. */
    public static void apriPresenze(VoceCalendario voce, boolean gestione) {
        apriPresenze(voce.lezione(), schedaDi(voce), gestione);
    }

    /**
     * Registro delle presenze della lezione: Lezioni per admin e segreteria, Le Mie Lezioni per l'insegnante; con
     * {@code scheda} (id del gruppo o {@link ElencoLezioni#SENZA_GRUPPO}) aperto sulla scheda di quel gruppo.
     */
    private static void apriPresenze(Lezione lezione, String scheda, boolean gestione) {
        Map<String, String> valori = new HashMap<>();
        valori.put(ElencoLezioni.PARAMETRO_LEZIONE, String.valueOf(lezione.getId()));
        if (scheda != null) {
            valori.put(ElencoLezioni.PARAMETRO_GRUPPO, scheda);
        }
        UI.getCurrent().navigate(gestione ? LezioniView.class : LezioniInsegnanteView.class, QueryParameters.simple(valori));
    }

    private static String schedaDi(VoceCalendario voce) {
        return voce.gruppo() != null ? String.valueOf(voce.gruppo().getId()) : voce.senzaGruppo() ? ElencoLezioni.SENZA_GRUPPO : null;
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
