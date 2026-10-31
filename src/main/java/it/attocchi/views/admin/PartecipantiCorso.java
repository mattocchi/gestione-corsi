package it.attocchi.views.admin;

import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.timepicker.TimePicker;
import it.attocchi.entity.Corso;
import it.attocchi.entity.GruppoCorso;
import it.attocchi.entity.Iscrizione;
import it.attocchi.entity.OpzioneCorso;
import it.attocchi.entity.TipoQuota;
import it.attocchi.entity.Allievo;
import it.attocchi.service.CorsoService;
import it.attocchi.service.PagamentoService;
import it.attocchi.service.PagamentoService.Situazione;
import it.attocchi.service.AllievoService;
import it.attocchi.views.CardMobile;
import it.attocchi.views.Formati;
import it.attocchi.views.StatoPagamentoUI;
import it.attocchi.views.Stili;
import it.attocchi.views.ViewSupport;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Scheda "Iscritti" della pagina del corso ({@link CorsoView}): partecipanti in ordine di orario di inizio lezione (chi non ha ancora un orario
 * è in fondo), con aggiunta, rimozione e orario modificabile. Le modifiche sono salvate subito
 * e l'elenco si riordina; la notifica compare solo in caso di errore. Sopra l'elenco i gruppi del corso (giorno e
 * orario): ogni partecipante può appartenere a uno e frequenta solo quel giorno, senza gruppo tutti i giorni.
 * Eliminare un gruppo è solo per l'admin. Per un corso non attivo partecipanti, orari e gruppi sono in sola lettura
 * (tranne che per l'admin). Se il corso ha opzioni aggiuntive (es. il kit) la colonna "Opzioni" ha una casella per
 * ognuna, salvata subito: l'importo si somma alla quota del partecipante (anche nei corsi non attivi, come le quote).
 * La colonna "Quota" mostra la quota dovuta e se è quella di listino o personalizzata (con la nota).
 * In cima "Invia conferme di iscrizione a tutti" ({@link InvioEmailDialog#inviaConferme}), solo per i corsi pubblicati.
 */
public class PartecipantiCorso extends VerticalLayout {

    private final CorsoService corsoService;
    private final AllievoService allievoService;
    private final PagamentoService pagamentoService;
    private final Corso corso;
    private final Grid<Iscrizione> grid = new Grid<>(Iscrizione.class, false);
    private final ComboBox<Allievo> nuovoPartecipante = new ComboBox<>();
    private final Div gruppiDelCorso = new Div();
    private final Button confermeATutti = new Button("Invia conferme di iscrizione a tutti", VaadinIcon.ENVELOPES.create());
    private final boolean puoEliminareGruppi;
    private final Grid.Column<Iscrizione> colonnaGruppo;
    private final Grid.Column<Iscrizione> colonnaOpzioni;
    private final CardMobile<Iscrizione> card;
    private Map<Long, Situazione> situazioni = Map.of();
    private List<GruppoCorso> gruppi = List.of();
    private List<OpzioneCorso> opzioni = List.of();

    /** @param puoEliminareGruppi {@code true} per l'admin, che può eliminare i gruppi */
    public PartecipantiCorso(CorsoService corsoService, AllievoService allievoService,
                              PagamentoService pagamentoService, Corso corso, boolean puoEliminareGruppi) {
        this.corsoService = corsoService;
        this.puoEliminareGruppi = puoEliminareGruppi;
        this.allievoService = allievoService;
        this.pagamentoService = pagamentoService;
        this.corso = corso;

        nuovoPartecipante.setPlaceholder("Aggiungi un allievo");
        nuovoPartecipante.setAriaLabel("Allievo da aggiungere");
        nuovoPartecipante.setItemLabelGenerator(Allievo::getNomeCompleto);
        nuovoPartecipante.setClearButtonVisible(true);
        // Un nome che non corrisponde a nessun allievo apre l'inserimento rapido
        nuovoPartecipante.setAllowCustomValue(true);
        nuovoPartecipante.setHelperText("Non c'è? Scrivi nome e cognome e premi Invio per crearlo");
        nuovoPartecipante.addCustomValueSetListener(e -> nuovoAllievo(e.getDetail()));
        Button aggiungi = new Button("Aggiungi", VaadinIcon.PLUS.create(), e -> aggiungi());
        aggiungi.addThemeVariants(ButtonVariant.PRIMARY);
        aggiungi.setEnabled(false);
        nuovoPartecipante.addValueChangeListener(e -> aggiungi.setEnabled(e.getValue() != null));

        HorizontalLayout aggiunta = new HorizontalLayout(nuovoPartecipante, aggiungi);
        aggiunta.addClassNames("w-full items-baseline");
        nuovoPartecipante.addClassNames("grow");

        grid.addComponentColumn(this::createOrario).setHeader("Orario").setWidth("9.5rem").setFlexGrow(0);
        grid.addColumn(iscrizione -> iscrizione.getAllievo().getNomeCompleto()).setHeader("Partecipante");
        // Solo se il corso ha dei gruppi
        colonnaGruppo = grid.addComponentColumn(this::createGruppo).setHeader("Gruppo").setWidth("12rem").setFlexGrow(0);
        // Solo se il corso ha opzioni aggiuntive
        colonnaOpzioni = grid.addComponentColumn(this::createOpzioni).setKey("opzioni").setHeader("Opzioni")
            .setAutoWidth(true).setFlexGrow(0);
        grid.addComponentColumn(this::createQuota).setKey("quota").setHeader("Quota").setAutoWidth(true).setFlexGrow(0);
        grid.addComponentColumn(iscrizione -> StatoPagamentoUI.badge(situazione(iscrizione)))
            .setHeader("Pagamento").setWidth("9rem").setFlexGrow(0);
        boolean modificabile = CorsoService.puoModificare(corso);
        // Quota e pagamenti sempre disponibili (anche per i corsi non attivi); rimozione solo per i corsi attivi
        var azioni = grid.addComponentColumn(iscrizione -> createAzioni(iscrizione, modificabile));
        ViewSupport.configureActionsColumn(azioni);
        azioni.setHeader("").setAutoWidth(false).setWidth(modificabile ? "6rem" : "3.5rem");
        // Su schermi stretti il nome va a capo invece di essere troncato
        grid.addClassName("partecipanti-grid");
        card = CardMobile.per(grid)
            .titolo(iscrizione -> iscrizione.getAllievo().getNomeCompleto())
            .stato(iscrizione -> StatoPagamentoUI.badge(situazione(iscrizione)))
            .campo("Orario", this::createOrario)
            .campo("Gruppo", iscrizione -> gruppi.isEmpty() ? null : createGruppo(iscrizione))
            .campo("Opzioni", iscrizione -> opzioni.isEmpty() ? null : createOpzioni(iscrizione))
            .campo("Quota", this::createQuota)
            .azioni(iscrizione -> createAzioni(iscrizione, modificabile));
        card.applica();
        grid.setAllRowsVisible(true);
        gruppiDelCorso.addClassNames("flex flex-col gap-1 w-full");
        aggiorna();

        // Stesso invio di Documenti: chi riceve e chi è saltato, anteprima, invio
        confermeATutti.addClickListener(e -> InvioEmailDialog.inviaConferme(corso, this::aggiorna));
        add(confermeATutti, aggiunta, gruppiDelCorso, grid);
        if (!modificabile) {
            aggiunta.setVisible(false);
            Span avviso = new Span("Corso non attivo: partecipanti e orari non sono modificabili.");
            avviso.addClassNames(Stili.TESTO_SECONDARIO);
            addComponentAsFirst(avviso);
        }
        addClassNames("p-0 max-w-4xl");
    }

    private HorizontalLayout createAzioni(Iscrizione iscrizione, boolean modificabile) {
        Button pagamenti = ViewSupport.iconButton(VaadinIcon.EURO, "Quota e pagamenti di " + iscrizione.getAllievo().getNomeCompleto(),
            e -> openQuotaPagamenti(iscrizione));
        StatoPagamentoUI.coloraPulsante(pagamenti, "Quota e pagamenti", situazione(iscrizione));
        HorizontalLayout actions = ViewSupport.actions(pagamenti);
        if (modificabile) {
            Button rimuovi = ViewSupport.iconButton(VaadinIcon.TRASH, "Rimuovi " + iscrizione.getAllievo().getNomeCompleto(),
                e -> rimuovi(iscrizione));
            rimuovi.addThemeVariants(ButtonVariant.ERROR);
            actions.add(rimuovi);
        }
        return actions;
    }

    private TimePicker createOrario(Iscrizione iscrizione) {
        TimePicker orario = ViewSupport.timePicker(null);
        orario.setStep(Duration.ofMinutes(5));
        orario.setPlaceholder("da assegnare");
        orario.setClearButtonVisible(true);
        orario.addClassNames("w-full");
        orario.setAriaLabel("Orario lezione di " + iscrizione.getAllievo().getNomeCompleto());
        orario.setValue(iscrizione.getOrarioLezione());
        orario.setReadOnly(!CorsoService.puoModificare(corso));
        orario.addValueChangeListener(e -> {
            try {
                corsoService.impostaOrario(corso.getId(), iscrizione.getAllievo().getId(), e.getValue());
                aggiorna();
            } catch (RuntimeException ex) {
                Notification.show("Errore nel salvataggio dell'orario di "
                    + iscrizione.getAllievo().getNomeCompleto() + ": " + ex.getMessage());
            }
        });
        return orario;
    }

    /** Gruppo del partecipante, salvato subito; vuoto = nessun gruppo (tutti i giorni del corso). */
    private ComboBox<GruppoCorso> createGruppo(Iscrizione iscrizione) {
        String nome = iscrizione.getAllievo().getNomeCompleto();
        ComboBox<GruppoCorso> gruppo = new ComboBox<>();
        gruppo.setAriaLabel("Gruppo di " + nome);
        gruppo.setItems(gruppi);
        gruppo.setItemLabelGenerator(Formati::gruppo);
        gruppo.setPlaceholder("Tutti i giorni");
        gruppo.setClearButtonVisible(true);
        gruppo.addClassNames("w-full");
        gruppo.setValue(iscrizione.getGruppo());
        gruppo.setReadOnly(!CorsoService.puoModificare(corso));
        gruppo.addValueChangeListener(e -> {
            if (!e.isFromClient()) {
                return;
            }
            try {
                corsoService.assegnaGruppo(corso.getId(), iscrizione.getAllievo().getId(),
                    e.getValue() != null ? e.getValue().getId() : null);
            } catch (RuntimeException ex) {
                Notification.show("Errore nell'assegnazione del gruppo di " + nome + ": " + ex.getMessage());
            }
            aggiorna();
        });
        return gruppo;
    }

    /**
     * Quota dovuta (opzioni attive comprese) e, sotto, da dove viene: "Prezzo di listino", "Pre-iscrizione" o
     * "Personalizzata" con la nota, e la differenza dal listino; con opzioni attive anche il dettaglio corso + opzioni.
     */
    private Div createQuota(Iscrizione iscrizione) {
        Span importo = new Span(Formati.euro(iscrizione.getQuotaDovuta()));
        importo.addClassNames("font-(--aura-font-weight-medium)");
        Div quota = new Div(importo);
        quota.addClassNames("flex flex-col");
        BigDecimal listino = corso.getCostoTotale() != null ? corso.getCostoTotale() : BigDecimal.ZERO;
        Div origine = new Div();
        origine.addClassNames("flex flex-wrap items-center gap-1");
        TipoQuota tipo = iscrizione.getTipoQuota();
        if (tipo == TipoQuota.PIENA) {
            Span testo = new Span("Prezzo di listino");
            testo.addClassNames(Stili.TESTO_SECONDARIO);
            origine.add(testo);
        } else {
            String nota = tipo == TipoQuota.PERSONALIZZATA ? iscrizione.getNotaQuota() : null;
            Span testo = new Span(tipo.getDescrizione() + (nota != null && !nota.isBlank() ? " · " + nota : ""));
            testo.addClassNames(Stili.TESTO_SECONDARIO);
            origine.add(testo);
            int confronto = iscrizione.getQuotaCorso().compareTo(listino);
            if (confronto != 0) {
                Badge differenza = new Badge((confronto < 0 ? "-" : "+")
                    + Formati.euro(listino.subtract(iscrizione.getQuotaCorso()).abs()));
                differenza.addThemeVariants(confronto < 0 ? BadgeVariant.SUCCESS : BadgeVariant.WARNING);
                origine.add(differenza);
            }
        }
        quota.add(origine);
        if (!iscrizione.getOpzioni().isEmpty()) {
            BigDecimal importoOpzioni = iscrizione.getQuotaDovuta().subtract(iscrizione.getQuotaCorso());
            Span dettaglio = new Span("Corso " + Formati.euro(iscrizione.getQuotaCorso()) + " + opzioni "
                + Formati.euro(importoOpzioni));
            dettaglio.addClassNames(Stili.TESTO_SECONDARIO);
            quota.add(dettaglio);
        }
        return quota;
    }

    /** Una casella per opzione aggiuntiva del corso ("Kit +30,00 €"), salvata subito. */
    private Div createOpzioni(Iscrizione iscrizione) {
        String nome = iscrizione.getAllievo().getNomeCompleto();
        Div caselle = new Div();
        caselle.addClassNames("flex flex-col");
        for (OpzioneCorso opzione : opzioni) {
            Checkbox casella = new Checkbox(opzione.getDescrizione() + " +" + Formati.euro(opzione.getImporto()),
                iscrizione.getOpzioni().contains(opzione));
            casella.setAriaLabel(opzione.getDescrizione() + " per " + nome);
            casella.addValueChangeListener(e -> {
                if (!e.isFromClient()) {
                    return;
                }
                try {
                    corsoService.impostaOpzione(corso.getId(), iscrizione.getAllievo().getId(), opzione.getId(), e.getValue());
                } catch (RuntimeException ex) {
                    Notification.show("Errore nel salvataggio dell'opzione di " + nome + ": " + ex.getMessage());
                }
                aggiorna();
            });
            caselle.add(casella);
        }
        return caselle;
    }

    /** Sezione dei gruppi: uno per riga con il numero di allievi, modifica ed eliminazione; "Nuovo gruppo". */
    private void mostraGruppi(List<Iscrizione> partecipanti) {
        gruppiDelCorso.removeAll();
        boolean modificabile = CorsoService.puoModificare(corso);
        gruppiDelCorso.setVisible(modificabile || !gruppi.isEmpty());
        Span spiegazione = new Span(gruppi.isEmpty()
            ? "Nessun gruppo: tutti frequentano tutti i giorni del corso."
            : "Chi è in un gruppo frequenta solo il suo giorno; senza gruppo tutti i giorni del corso.");
        spiegazione.addClassNames(Stili.TESTO_SECONDARIO);
        gruppiDelCorso.add(ViewSupport.sezione("Gruppi"), spiegazione);
        for (GruppoCorso gruppo : gruppi) {
            long allievi = partecipanti.stream().filter(i -> gruppo.equals(i.getGruppo())).count();
            Span descrizione = new Span(Formati.gruppo(gruppo) + " · " + allievi + (allievi == 1 ? " allievo" : " allievi"));
            HorizontalLayout riga = new HorizontalLayout(descrizione);
            riga.addClassNames("items-center gap-2");
            if (modificabile) {
                riga.add(ViewSupport.iconButton(VaadinIcon.PENCIL, "Modifica il gruppo " + Formati.gruppo(gruppo),
                    e -> new GruppoDialog(corsoService, corso, gruppo, this::aggiorna).open()));
                if (puoEliminareGruppi) {
                    Button elimina = ViewSupport.iconButton(VaadinIcon.TRASH, "Elimina il gruppo " + Formati.gruppo(gruppo),
                        e -> confermaEliminazione(gruppo, allievi));
                    elimina.addThemeVariants(ButtonVariant.ERROR);
                    riga.add(elimina);
                }
            }
            gruppiDelCorso.add(riga);
        }
        if (modificabile) {
            Button nuovo = new Button("Nuovo gruppo", VaadinIcon.PLUS.create(),
                e -> new GruppoDialog(corsoService, corso, new GruppoCorso(), this::aggiorna).open());
            nuovo.addThemeVariants(ButtonVariant.TERTIARY);
            nuovo.addClassNames("self-start");
            gruppiDelCorso.add(nuovo);
        }
    }

    private void confermaEliminazione(GruppoCorso gruppo, long allievi) {
        Dialog conferma = ViewSupport.dialog("Eliminare il gruppo?", "420px");
        conferma.add(new Span(Formati.gruppo(gruppo) + (allievi > 0
            ? ": i suoi " + allievi + " allievi restano senza gruppo e frequentano tutti i giorni del corso."
            : ".")));
        Button elimina = new Button("Elimina", e -> {
            conferma.close();
            try {
                corsoService.eliminaGruppo(gruppo.getId());
            } catch (RuntimeException ex) {
                Notification.show("Errore nell'eliminazione del gruppo: " + ex.getMessage());
            }
            aggiorna();
        });
        elimina.addThemeVariants(ButtonVariant.PRIMARY, ButtonVariant.ERROR);
        conferma.getFooter().add(ViewSupport.closeButton(conferma, "Annulla"), elimina);
        conferma.open();
    }

    private void openQuotaPagamenti(Iscrizione iscrizione) {
        QuotaPagamentiDialog dialog = new QuotaPagamentiDialog(pagamentoService, corso, iscrizione.getAllievo());
        dialog.addOpenedChangeListener(e -> {
            if (!e.isOpened()) {
                aggiorna();
            }
        });
        dialog.open();
    }

    private void aggiungi() {
        iscrivi(nuovoPartecipante.getValue());
    }

    private void iscrivi(Allievo allievo) {
        try {
            corsoService.iscrivi(corso.getId(), allievo.getId());
            nuovoPartecipante.clear();
            aggiorna();
        } catch (RuntimeException ex) {
            Notification.show("Errore nell'aggiunta di " + allievo.getNomeCompleto() + ": " + ex.getMessage());
        }
    }

    /** Inserimento rapido di un allievo che non esiste, con nome e cognome proposti dal testo scritto. */
    private void nuovoAllievo(String testo) {
        nuovoPartecipante.clear();
        if (testo == null || testo.isBlank()) {
            return;
        }
        Set<Long> iscritti = corsoService.findPartecipanti(corso.getId()).stream()
            .map(i -> i.getAllievo().getId()).collect(Collectors.toSet());
        new NuovoAllievoRapidoDialog(allievoService, testo, iscritti, this::iscrivi).open();
    }

    private void rimuovi(Iscrizione iscrizione) {
        try {
            corsoService.rimuoviIscrizione(corso.getId(), iscrizione.getAllievo().getId());
            aggiorna();
        } catch (RuntimeException ex) {
            Notification.show("Errore nella rimozione di " + iscrizione.getAllievo().getNomeCompleto() + ": " + ex.getMessage());
        }
    }

    /** Ricarica i partecipanti e propone come nuovi solo gli allievi non ancora iscritti. */
    private Situazione situazione(Iscrizione iscrizione) {
        return situazioni.getOrDefault(iscrizione.getAllievo().getId(),
            Situazione.di(iscrizione, BigDecimal.ZERO));
    }

    private void aggiorna() {
        situazioni = pagamentoService.situazioniDelCorso(corso.getId());
        List<Iscrizione> partecipanti = corsoService.findPartecipanti(corso.getId());
        gruppi = corsoService.gruppi(corso.getId());
        card.setVisibileSuDesktop(colonnaGruppo, !gruppi.isEmpty());
        opzioni = corsoService.opzioni(corso.getId());
        card.setVisibileSuDesktop(colonnaOpzioni, !opzioni.isEmpty());
        mostraGruppi(partecipanti);
        grid.setItems(partecipanti);
        // Ai corsi non pubblicati non si invia nulla alle famiglie
        confermeATutti.setEnabled(corso.isPubblicato() && !partecipanti.isEmpty());
        confermeATutti.setTooltipText(!corso.isPubblicato() ? "Corso non pubblicato: le conferme non si inviano alle famiglie"
            : partecipanti.isEmpty() ? "Nessun iscritto al corso" : null);
        Set<Long> iscritti = partecipanti.stream().map(i -> i.getAllievo().getId()).collect(Collectors.toSet());
        nuovoPartecipante.setItems(allievoService.findAll().stream()
            .filter(allievo -> !iscritti.contains(allievo.getId()))
            .sorted(Comparator.comparing(Allievo::getNomeCompleto))
            .toList());
    }
}
