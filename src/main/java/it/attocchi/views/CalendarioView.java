package it.attocchi.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import it.attocchi.entity.Corso;
import it.attocchi.entity.GruppoCorso;
import it.attocchi.entity.Lezione;
import it.attocchi.security.SecurityService;
import it.attocchi.service.CalendarioFestivita;
import it.attocchi.service.CalendarioService;
import it.attocchi.service.CalendarioService.VoceCalendario;
import it.attocchi.service.CorsoService;
import it.attocchi.service.FestivitaService;
import it.attocchi.service.LezioneService;
import jakarta.annotation.security.RolesAllowed;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Calendario mensile delle lezioni secondo il ruolo ({@link CalendarioService}): su desktop una griglia lun–dom con
 * le lezioni di ogni giorno e le festività, su smartphone l'elenco dei giorni del mese. Vaadin non ha un componente
 * calendario: la griglia è fatta con Div e classi Tailwind. Cliccando una lezione si vedono i dettagli e, per chi
 * gestisce le lezioni, "Gestisci presenze" apre il registro esistente su quella lezione.
 */
@Route(value = "calendario", layout = MainLayout.class)
@PageTitle("Calendario")
@Menu(title = "Calendario", order = 1, icon = "vaadin:calendar-user")
@RolesAllowed({"ADMIN", "SEGRETERIA", "INSEGNANTE", "ALLIEVO", "GENITORE"})
public class CalendarioView extends VerticalLayout {

    private final CalendarioService calendarioService;
    private final LezioneService lezioneService;
    private final FestivitaService festivitaService;
    private final String username;
    /** Admin e segreteria: filtro per corso e gestione delle presenze di tutte le lezioni. */
    private final boolean gestione;

    private final H2 titolo = new H2();
    private final ComboBox<Corso> filtroCorso = new ComboBox<>();
    private final Div griglia = new Div();
    private final Div elenco = new Div();
    private YearMonth mese = YearMonth.now();
    private Map<Long, List<GruppoCorso>> gruppiPerCorso = Map.of();

    public CalendarioView(CalendarioService calendarioService, LezioneService lezioneService,
                          FestivitaService festivitaService, CorsoService corsoService, SecurityService securityService) {
        this.calendarioService = calendarioService;
        this.lezioneService = lezioneService;
        this.festivitaService = festivitaService;
        this.username = securityService.getUsername().orElse("");
        this.gestione = securityService.hasRole("ADMIN") || securityService.hasRole("SEGRETERIA");
        addClassNames("size-full");

        Button precedente = ViewSupport.iconButton(VaadinIcon.ANGLE_LEFT, "Mese precedente", e -> vaiA(mese.minusMonths(1)));
        Button successivo = ViewSupport.iconButton(VaadinIcon.ANGLE_RIGHT, "Mese successivo", e -> vaiA(mese.plusMonths(1)));
        Button oggi = new Button("Oggi", e -> vaiA(YearMonth.now()));
        titolo.addClassNames("text-(length:--aura-font-size-xl) m-0 min-w-44 first-letter:uppercase");
        HorizontalLayout barra = new HorizontalLayout(precedente, titolo, successivo, oggi);
        barra.addClassNames("flex-wrap w-full items-center");
        if (gestione) {
            filtroCorso.setPlaceholder("Tutti i corsi");
            filtroCorso.setAriaLabel("Filtra per corso");
            filtroCorso.setClearButtonVisible(true);
            filtroCorso.setItemLabelGenerator(Corso::getNome);
            filtroCorso.setItems(corsoService.findAllAttivi());
            filtroCorso.addClassNames("w-56 max-w-full");
            filtroCorso.addValueChangeListener(e -> aggiorna());
            barra.add(filtroCorso);
        }

        // Griglia lun–dom da 640px in su, elenco dei giorni sotto
        griglia.addClassNames("hidden sm:grid grid-cols-7 gap-px w-full bg-(--vaadin-border-color-secondary)"
            + " border border-(--vaadin-border-color-secondary) rounded-(--vaadin-radius-m) overflow-hidden");
        elenco.addClassNames("flex flex-col gap-4 w-full sm:hidden");
        add(barra, griglia, elenco);
        aggiorna();
    }

    /** Mostra il mese indicato (anche dai test). */
    void vaiA(YearMonth nuovo) {
        mese = nuovo;
        aggiorna();
    }

    private void aggiorna() {
        titolo.setText(Formati.mese(mese));
        List<VoceCalendario> voci = calendarioService.lezioniDelMese(username, mese,
            filtroCorso.getValue() != null ? filtroCorso.getValue().getId() : null);
        gruppiPerCorso = lezioneService.gruppiPerCorso();
        Map<LocalDate, List<VoceCalendario>> perGiorno = voci.stream()
            .collect(Collectors.groupingBy(v -> v.lezione().getData()));
        CalendarioFestivita festivita = festivitaService.calendario();
        riempiGriglia(perGiorno, festivita);
        riempiElenco(perGiorno, festivita);
    }

    private void riempiGriglia(Map<LocalDate, List<VoceCalendario>> perGiorno, CalendarioFestivita festivita) {
        griglia.removeAll();
        for (DayOfWeek giorno : DayOfWeek.values()) {
            Span intestazione = new Span(giorno.getDisplayName(TextStyle.SHORT, ViewSupport.LOCALE));
            intestazione.addClassNames("p-1 text-center bg-(--aura-surface-color)", Stili.TESTO_SECONDARIO);
            griglia.add(intestazione);
        }
        LocalDate primo = mese.atDay(1);
        LocalDate inizio = primo.minusDays(primo.getDayOfWeek().getValue() - 1L);
        LocalDate ultimo = mese.atEndOfMonth();
        LocalDate fine = ultimo.plusDays(7L - ultimo.getDayOfWeek().getValue());
        for (LocalDate data = inizio; !data.isAfter(fine); data = data.plusDays(1)) {
            griglia.add(cella(data, perGiorno.getOrDefault(data, List.of()), festivita));
        }
    }

    private Div cella(LocalDate data, List<VoceCalendario> voci, CalendarioFestivita festivita) {
        Span numero = new Span(String.valueOf(data.getDayOfMonth()));
        numero.addClassNames("text-(length:--aura-font-size-s)");
        Div cella = new Div(numero);
        cella.addClassNames("flex flex-col gap-0.5 min-h-28 min-w-0 p-1 bg-(--aura-surface-color)");
        if (!YearMonth.from(data).equals(mese)) {
            cella.addClassNames("opacity-50");
        }
        if (data.equals(LocalDate.now())) {
            numero.addClassNames("font-(--aura-font-weight-semibold) text-(--aura-accent-text-color)");
            cella.getElement().setAttribute("aria-current", "date");
        }
        festivita.festivita(data).ifPresent(nome -> cella.add(festivita(nome)));
        voci.forEach(voce -> cella.add(pulsante(voce, true)));
        return cella;
    }

    private void riempiElenco(Map<LocalDate, List<VoceCalendario>> perGiorno, CalendarioFestivita festivita) {
        elenco.removeAll();
        for (LocalDate data = mese.atDay(1); !data.isAfter(mese.atEndOfMonth()); data = data.plusDays(1)) {
            List<VoceCalendario> voci = perGiorno.getOrDefault(data, List.of());
            var nomeFestivita = festivita.festivita(data);
            if (voci.isEmpty() && nomeFestivita.isEmpty()) {
                continue;
            }
            H3 giorno = new H3(Formati.data(data));
            giorno.addClassNames("text-(length:--aura-font-size-m) m-0");
            Div blocco = new Div(giorno);
            blocco.addClassNames("flex flex-col gap-1");
            nomeFestivita.ifPresent(nome -> blocco.add(festivita(nome)));
            voci.forEach(voce -> blocco.add(pulsante(voce, false)));
            elenco.add(blocco);
        }
        if (elenco.getComponentCount() == 0) {
            Span vuoto = new Span("Nessuna lezione in questo mese");
            vuoto.addClassNames(Stili.TESTO_SECONDARIO);
            elenco.add(vuoto);
        }
    }

    private static Span festivita(String nome) {
        Span festivo = new Span(nome);
        festivo.addClassNames("text-(length:--aura-font-size-xs) text-(--aura-red-text)");
        return festivo;
    }

    /**
     * Lezione come pulsante (orario, corso, gruppo o giorno, figlio) colorato secondo lo stato; apre i dettagli. Nei
     * corsi con gruppi una voce per gruppo, con l'orario del gruppo.
     */
    private Button pulsante(VoceCalendario voce, boolean compatto) {
        Lezione lezione = voce.lezione();
        String gruppo = voce.gruppo() != null ? " · " + nomeBreve(voce.gruppo())
            : voce.senzaGruppo() ? " · Senza gruppo" : gruppoOGiorno(lezione);
        String testo = (voce.orario() != null ? Formati.ora(voce.orario()) + " " : "")
            + lezione.getCorso().getNome() + gruppo
            + (voce.perChi() != null ? " – " + voce.perChi() : "");
        Button pulsante = new Button(testo, e -> dettagli(voce));
        pulsante.addThemeVariants(ButtonVariant.TERTIARY);
        pulsante.addClassNames("justify-start m-0 h-auto min-h-0 py-0.5 px-1 w-full min-w-0",
            compatto ? "text-(length:--aura-font-size-xs)" : "text-(length:--aura-font-size-s)");
        if (lezione.isCompletata()) {
            pulsante.addClassNames("text-(--aura-green-text)");
        } else if (lezione.isNonEffettuata()) {
            pulsante.addClassNames("text-(--aura-red-text) line-through");
        }
        pulsante.setAriaLabel(testo + ", " + stato(lezione) + ", " + Formati.data(lezione.getData()));
        pulsante.setTooltipText(testo + " (" + stato(lezione) + ")");
        return pulsante;
    }

    /** Nome del gruppo, o il suo giorno ("sab") se non ha un nome. */
    private static String nomeBreve(GruppoCorso gruppo) {
        return gruppo.getNome() != null && !gruppo.getNome().isBlank() ? gruppo.getNome() : Formati.giorno(gruppo.getGiorno());
    }

    /** Nei corsi con gruppi " · Piccoli" o " · lun" (la parte dopo il numero), altrimenti vuoto. */
    private String gruppoOGiorno(Lezione lezione) {
        String numero = numero(lezione);
        int separatore = numero.indexOf(" · ");
        return separatore >= 0 ? numero.substring(separatore) : "";
    }

    private String numero(Lezione lezione) {
        return Formati.numeroLezione(lezione, gruppiPerCorso.get(lezione.getCorso().getId()));
    }

    private static String stato(Lezione lezione) {
        return lezione.isCompletata() ? "completata" : lezione.isNonEffettuata() ? "non effettuata" : "da fare";
    }

    /** Dettagli in sola lettura; "Gestisci presenze" per admin, segreteria e gli insegnanti del corso. */
    private void dettagli(VoceCalendario voce) {
        String gruppo = voce.gruppo() != null ? Formati.gruppo(voce.gruppo()) : voce.senzaGruppo() ? "Senza gruppo" : null;
        DettaglioLezioneDialog.apri(voce.lezione(), voce.perChi(), gruppo, voce.gruppo() != null ? voce.orario() : null,
            numero(voce.lezione()), username, gestione);
    }
}
