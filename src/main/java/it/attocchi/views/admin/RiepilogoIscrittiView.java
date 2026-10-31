package it.attocchi.views.admin;

import java.time.LocalDate;
import it.attocchi.entity.AnnoScolastico;
import it.attocchi.service.AnnoScolasticoService;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.FooterRow;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import it.attocchi.entity.Corso;
import it.attocchi.entity.GruppoCorso;
import it.attocchi.service.BilancioService;
import it.attocchi.service.PagamentoService;
import it.attocchi.service.PagamentoService.IscrittiCorso;
import it.attocchi.service.PagamentoService.RigaPagamento;
import it.attocchi.service.PagamentoService.Situazione;
import it.attocchi.views.CardMobile;
import it.attocchi.views.Formati;
import it.attocchi.views.MainLayout;
import it.attocchi.views.RicercaRapida;
import it.attocchi.views.Schede;
import it.attocchi.views.StatoPagamentoUI;
import it.attocchi.views.Stili;
import it.attocchi.views.ViewSupport;
import it.attocchi.views.admin.CorsiView.FiltroStato;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Iscritti corso per corso con quanto devono pagare e quanto hanno pagato finora, in sola lettura: una
 * {@link TreeGrid} con un livello di raggruppamento (riga del corso con i totali, sotto i suoi iscritti) e il totale
 * generale in fondo. I pagamenti si registrano in Gestione Pagamenti. Per il preventivo la riga del corso ha anche
 * il costo stimato degli insegnanti ({@link BilancioService#costiPrevistiPerCorso()}, solo con la tariffa oraria) e
 * il margine previsto (da pagare meno costo).
 */
@Route(value = "riepilogo-iscritti", layout = MainLayout.class)
@PageTitle("Iscritti")
@Menu(title = "Iscritti", order = 10.6, icon = "vaadin:table")
@RolesAllowed({"ADMIN", "SEGRETERIA"})
public class RiepilogoIscrittiView extends VerticalLayout {

    /** Gruppo del corso con i suoi iscritti mostrati; {@code gruppo} null = gli iscritti senza gruppo. */
    public record Gruppo(GruppoCorso gruppo, List<RigaPagamento> iscritti) {
    }

    /**
     * Riga della griglia: un corso, un suo gruppo (solo nei corsi con gruppi) o un iscritto; l'iscritto tiene il
     * gruppo sotto cui è mostrato ({@code null} nei corsi senza gruppi).
     */
    public record Voce(IscrittiCorso corso, Gruppo gruppo, RigaPagamento iscritto) {

        public boolean isCorso() {
            return gruppo == null && iscritto == null;
        }

        public boolean isGruppo() {
            return gruppo != null && iscritto == null;
        }

        public boolean isIscritto() {
            return iscritto != null;
        }

        Situazione situazione() {
            return iscritto.situazione();
        }

        /** Importo dell'iscritto o totale del corso; nessuno sulla riga del gruppo. */
        BigDecimal importo(Function<Situazione, BigDecimal> importo, Function<IscrittiCorso, BigDecimal> totale) {
            return isCorso() ? totale.apply(corso) : isIscritto() ? importo.apply(situazione()) : null;
        }
    }

    private final PagamentoService pagamentoService;
    private final TreeGrid<Voce> grid = new TreeGrid<>();
    private final Schede<FiltroStato> filtroCorsi = new Schede<>("Filtra per stato del corso",
        List.of(FiltroStato.values()), FiltroStato::getEtichetta, f -> "Corsi: " + f.getEtichetta());
    private final ComboBox<String> filtroAnno;
    private final RicercaRapida ricerca = new RicercaRapida();
    private final List<Voce> corsiMostrati = new ArrayList<>();
    /** Righe sotto ogni corso e gruppo: tutte, o con la ricerca solo quelle che corrispondono. */
    private final Map<Voce, List<Voce>> figli = new HashMap<>();
    private final FooterRow totale;
    private Grid.Column<Voce> colonnaNome;
    private Grid.Column<Voce> colonnaIscritti;
    private Grid.Column<Voce> colonnaQuota;
    private Grid.Column<Voce> colonnaPagato;
    private Grid.Column<Voce> colonnaResiduo;
    private Grid.Column<Voce> colonnaCosto;
    private Grid.Column<Voce> colonnaMargine;
    private Grid.Column<Voce> colonnaCard;
    private List<IscrittiCorso> corsi = List.of();
    /** Costo stimato degli insegnanti per id del corso; manca per i corsi senza tariffa. */
    private Map<Long, BigDecimal> costi = Map.of();

    public RiepilogoIscrittiView(PagamentoService pagamentoService, BilancioService bilancioService) {
        this.pagamentoService = pagamentoService;
        addClassNames("size-full");

        configureGrid();
        totale = grid.appendFooterRow();

        corsi = pagamentoService.iscrittiPerCorso();
        costi = bilancioService.costiPrevistiPerCorso();
        List<String> anni = corsi.stream().map(c -> c.corso().getAnnoScolastico()).filter(Objects::nonNull).distinct()
            .sorted(Comparator.reverseOrder()).toList();
        // Anche gli anni non attivi (archivio): lo storico si consulta
        AnnoScolasticoService annoScolasticoService = VaadinService.getCurrent().getInstantiator()
            .getOrCreate(AnnoScolasticoService.class);
        filtroAnno = ViewSupport.filtroAnnoScolastico(annoScolasticoService.perFiltro(anni), annoScolasticoService.nonAttivi());
        // All'apertura l'anno corrente se ha corsi, altrimenti il più recente con corsi (come prima)
        String corrente = AnnoScolastico.di(LocalDate.now());
        filtroAnno.setValue(anni.contains(corrente) ? corrente : anni.isEmpty() ? null : anni.getFirst());
        filtroAnno.setPlaceholder("Tutti gli anni");
        filtroAnno.setClearButtonVisible(true);
        filtroCorsi.setValue(FiltroStato.ATTIVI);
        filtroCorsi.addValueChangeListener(e -> applicaFiltri());
        filtroAnno.addValueChangeListener(e -> applicaFiltri());
        ricerca.addRicercaListener(this::applicaFiltri);

        Button espandi = new Button("Espandi tutti", VaadinIcon.PLUS_SQUARE_O.create(), e -> grid.expandRecursively(corsiMostrati, 1));
        Button comprimi = new Button("Comprimi tutti", VaadinIcon.MINUS_SQUARE_O.create(), e -> grid.collapse(corsiMostrati));
        espandi.addThemeVariants(ButtonVariant.TERTIARY);
        comprimi.addThemeVariants(ButtonVariant.TERTIARY);
        HorizontalLayout filtri = new HorizontalLayout(filtroAnno, ricerca, espandi, comprimi);
        filtri.addClassNames("flex-wrap w-full items-center");

        add(filtroCorsi, filtri, grid);
        grid.addClassNames("grow");
        applicaFiltri();
    }

    private void configureGrid() {
        colonnaNome = grid.addHierarchyColumn(RiepilogoIscrittiView::nome)
            .setHeader("Corso / Iscritto").setSortable(false).setWidth("14rem").setFlexGrow(2);
        grid.addColumn(v -> v.isCorso() ? Objects.toString(v.corso().corso().getAnnoScolastico(), "") : "")
            .setHeader("Anno").setWidth("7rem").setFlexGrow(0);
        grid.addColumn(v -> v.isIscritto() && v.iscritto().genitore() != null ? v.iscritto().genitore().getNomeCompleto() : "")
            .setHeader("Genitore");
        grid.addColumn(this::giorni).setHeader("Giorni / Gruppo").setAutoWidth(true).setFlexGrow(0);
        colonnaIscritti = grid.addColumn(v -> v.isCorso() ? String.valueOf(v.corso().iscritti().size())
                : v.isGruppo() ? String.valueOf(v.gruppo().iscritti().size()) : "")
            .setHeader("Iscritti").setWidth("6rem").setFlexGrow(0);
        colonnaQuota = euro("Da pagare", Situazione::quota, IscrittiCorso::quota);
        colonnaPagato = euro("Pagato", Situazione::pagato, IscrittiCorso::pagato);
        colonnaResiduo = euro("Residuo", Situazione::residuo, IscrittiCorso::residuo);
        grid.addComponentColumn(this::stato).setHeader("Stato").setWidth("9rem").setFlexGrow(0);
        colonnaCosto = grid.addColumn(this::costo).setHeader("Costo insegnante").setWidth("9rem").setFlexGrow(0)
            .setTextAlign(ColumnTextAlign.END);
        colonnaMargine = grid.addColumn(this::margine).setHeader("Margine previsto").setWidth("9rem").setFlexGrow(0)
            .setTextAlign(ColumnTextAlign.END);
        grid.setPartNameGenerator(v -> v.isCorso() ? "riga-corso" : v.isGruppo() ? "riga-gruppo" : null);

        colonnaCard = CardMobile.per(grid)
            .titolo(RiepilogoIscrittiView::nome)
            .sottotitolo(v -> v.isCorso() ? Objects.toString(v.corso().corso().getAnnoScolastico(), "")
                : v.isIscritto() && v.iscritto().genitore() != null ? "Genitore: " + v.iscritto().genitore().getNomeCompleto() : null)
            .stato(v -> v.isIscritto() ? StatoPagamentoUI.badge(v.situazione()) : null)
            .campo("Giorni", v -> v.isCorso() ? giorni(v) : null)
            .campo("Gruppo", v -> v.isIscritto() && v.gruppo() == null ? giorni(v) : null)
            .campo("Iscritti", v -> v.isCorso() ? v.corso().iscritti().size() + " (" + saldati(v.corso()) + ")"
                : v.isGruppo() ? v.gruppo().iscritti().size() : null)
            .campo("Da pagare", v -> Formati.euro(v.importo(Situazione::quota, IscrittiCorso::quota)))
            .campo("Pagato", v -> Formati.euro(v.importo(Situazione::pagato, IscrittiCorso::pagato)))
            .campo("Residuo", v -> Formati.euro(v.importo(Situazione::residuo, IscrittiCorso::residuo)))
            .campo("Costo insegnante", this::costo)
            .campo("Margine previsto", this::margine)
            .azioni(v -> !figli.getOrDefault(v, List.of()).isEmpty() ? apriChiudi(v) : null)
            .classi(v -> v.isCorso() ? null
                : v.isIscritto() && v.gruppo() != null ? "ms-12 w-[calc(100%-3rem)]" : "ms-6 w-[calc(100%-1.5rem)]")
            .applica();

        // Il pulsante della card del corso cambia testo quando il corso si apre o si chiude
        grid.addExpandListener(e -> e.getItems().forEach(v -> grid.getDataProvider().refreshItem(v)));
        grid.addCollapseListener(e -> e.getItems().forEach(v -> grid.getDataProvider().refreshItem(v)));
        grid.setEmptyStateText("Nessun corso");
    }

    private Grid.Column<Voce> euro(String header, Function<Situazione, BigDecimal> importo,
                                   Function<IscrittiCorso, BigDecimal> totale) {
        return grid.addColumn(v -> Formati.euro(v.importo(importo, totale))).setHeader(header)
            .setWidth("8rem").setFlexGrow(0).setTextAlign(ColumnTextAlign.END);
    }

    /** Nome del corso, del gruppo ("Senza gruppo (tutti i giorni)" per chi non ne ha) o dell'iscritto. */
    private static String nome(Voce voce) {
        if (voce.isCorso()) {
            Corso corso = voce.corso().corso();
            return corso.isServizio() ? corso.getNome() + " (servizio)" : corso.getNome();
        }
        if (voce.isGruppo()) {
            return voce.gruppo().gruppo() != null ? Formati.gruppo(voce.gruppo().gruppo()) : "Senza gruppo (tutti i giorni)";
        }
        return voce.iscritto().allievo().getNomeCompleto();
    }

    private Component stato(Voce voce) {
        if (voce.isIscritto()) {
            return StatoPagamentoUI.badge(voce.situazione());
        }
        if (voce.isGruppo()) {
            return new Span();
        }
        Span saldati = new Span(voce.corso().iscritti().isEmpty() ? "Nessun iscritto" : saldati(voce.corso()));
        saldati.addClassNames(Stili.TESTO_SECONDARIO);
        return saldati;
    }

    /**
     * Giorni del corso, con il numero di gruppi se ne ha (es. "sab · 2 gruppi"), sulla riga del corso; il gruppo
     * (vuoto = tutti i giorni) sulla riga dell'iscritto.
     */
    private String giorni(Voce voce) {
        if (voce.isCorso()) {
            String giorni = Formati.giorni(voce.corso().corso().getGiorniSettimana());
            int gruppi = voce.corso().gruppi().size();
            return gruppi == 0 ? giorni : giorni + " · " + gruppi + (gruppi == 1 ? " gruppo" : " gruppi");
        }
        return voce.isIscritto() ? Formati.gruppo(voce.iscritto().iscrizione().getGruppo()) : "";
    }

    /** Costo stimato degli insegnanti, solo sulla riga del corso; "Senza tariffa" se nessuno ha la tariffa. */
    private String costo(Voce voce) {
        if (!voce.isCorso()) {
            return null;
        }
        BigDecimal costo = costi.get(voce.corso().corso().getId());
        return costo != null ? Formati.euro(costo) : "Senza tariffa";
    }

    /** Da pagare meno il costo stimato, solo sulla riga di un corso con tariffa. */
    private String margine(Voce voce) {
        BigDecimal margine = voce.isCorso() ? margine(voce.corso()) : null;
        return margine != null ? Formati.euro(margine) : null;
    }

    private BigDecimal margine(IscrittiCorso corso) {
        BigDecimal costo = costi.get(corso.corso().getId());
        return costo != null ? corso.quota().subtract(costo) : null;
    }

    private static String saldati(IscrittiCorso corso) {
        return corso.saldati() + (corso.saldati() == 1 ? " saldato" : " saldati");
    }

    private Button apriChiudi(Voce voce) {
        boolean aperto = grid.isExpanded(voce);
        Button button = new Button(aperto ? "Nascondi iscritti" : "Mostra iscritti",
            (aperto ? VaadinIcon.ANGLE_UP : VaadinIcon.ANGLE_DOWN).create(), e -> {
                if (grid.isExpanded(voce)) {
                    grid.collapse(voce);
                } else {
                    grid.expand(voce);
                }
            });
        button.addThemeVariants(ButtonVariant.TERTIARY);
        return button;
    }

    private void applicaFiltri() {
        FiltroStato stato = filtroCorsi.getValue();
        String anno = filtroAnno.getValue();
        corsiMostrati.clear();
        figli.clear();
        int conStatoEAnno = 0;
        for (IscrittiCorso corso : corsi) {
            Corso c = corso.corso();
            if ((stato == null || stato.ammette(c)) && (anno == null || anno.equals(c.getAnnoScolastico()))) {
                conStatoEAnno++;
                // Il corso che corrisponde mostra tutti gli iscritti, altrimenti solo quelli che corrispondono
                boolean corsoTrovato = ricerca.corrisponde(RicercaRapida.testo(c.getNome(), c.getAnnoScolastico(), c.getNomiInsegnanti()));
                List<RigaPagamento> iscritti = corsoTrovato ? corso.iscritti() : corso.iscritti().stream()
                    .filter(r -> ricerca.corrisponde(RicercaRapida.testo(r.allievo().getNomeCompleto(),
                        r.allievo().getUser() != null ? r.allievo().getUser().getEmail() : null,
                        r.genitore() != null ? r.genitore().getNomeCompleto() : null,
                        Formati.gruppo(r.iscrizione().getGruppo()))))
                    .toList();
                if (corsoTrovato || !iscritti.isEmpty()) {
                    Voce voceCorso = new Voce(corso, null, null);
                    corsiMostrati.add(voceCorso);
                    figli.put(voceCorso, righeSotto(corso, iscritti, corsoTrovato));
                }
            }
        }
        ricerca.mostraConteggio(corsiMostrati.size(), conStatoEAnno);
        grid.setItems(corsiMostrati, v -> figli.getOrDefault(v, List.of()));
        grid.expandRecursively(corsiMostrati, 1);
        aggiornaTotale();
    }

    /**
     * Righe sotto il corso: gli iscritti, o nei corsi con gruppi i gruppi (tutti se il corso corrisponde alla
     * ricerca, altrimenti quelli con iscritti trovati) e "Senza gruppo" se qualcuno non ne ha, con i loro iscritti.
     */
    private List<Voce> righeSotto(IscrittiCorso corso, List<RigaPagamento> iscritti, boolean tuttiIGruppi) {
        if (corso.gruppi().isEmpty()) {
            return iscritti.stream().map(r -> new Voce(corso, null, r)).toList();
        }
        List<Gruppo> gruppi = new ArrayList<>();
        corso.gruppi().forEach(g -> gruppi.add(new Gruppo(g, IscrittiCorso.delGruppo(iscritti, g))));
        gruppi.add(new Gruppo(null, IscrittiCorso.delGruppo(iscritti, null)));
        List<Voce> righe = new ArrayList<>();
        for (Gruppo gruppo : gruppi) {
            if (!gruppo.iscritti().isEmpty() || (tuttiIGruppi && gruppo.gruppo() != null)) {
                Voce voceGruppo = new Voce(corso, gruppo, null);
                righe.add(voceGruppo);
                figli.put(voceGruppo, gruppo.iscritti().stream().map(r -> new Voce(corso, gruppo, r)).toList());
            }
        }
        return righe;
    }

    /** Totale generale dei corsi mostrati, nel footer (su mobile nel footer della colonna card). */
    private void aggiornaTotale() {
        List<IscrittiCorso> mostrati = corsiMostrati.stream().map(Voce::corso).toList();
        int iscritti = mostrati.stream().mapToInt(c -> c.iscritti().size()).sum();
        BigDecimal quota = somma(mostrati, IscrittiCorso::quota);
        BigDecimal pagato = somma(mostrati, IscrittiCorso::pagato);
        BigDecimal residuo = somma(mostrati, IscrittiCorso::residuo);
        // Costo e margine solo dei corsi con tariffa
        List<IscrittiCorso> conTariffa = mostrati.stream().filter(c -> costi.containsKey(c.corso().getId())).toList();
        BigDecimal costo = somma(conTariffa, c -> costi.get(c.corso().getId()));
        BigDecimal margine = somma(conTariffa, this::margine);
        totale.getCell(colonnaNome).setText("Totale (" + mostrati.size() + (mostrati.size() == 1 ? " corso)" : " corsi)"));
        totale.getCell(colonnaIscritti).setText(String.valueOf(iscritti));
        totale.getCell(colonnaQuota).setText(Formati.euro(quota));
        totale.getCell(colonnaPagato).setText(Formati.euro(pagato));
        totale.getCell(colonnaResiduo).setText(Formati.euro(residuo));
        totale.getCell(colonnaCosto).setText(Formati.euro(costo));
        totale.getCell(colonnaMargine).setText(Formati.euro(margine));
        totale.getCell(colonnaCard).setText("Totale: " + iscritti + " iscritti · da pagare " + Formati.euro(quota)
            + " · pagato " + Formati.euro(pagato) + " · residuo " + Formati.euro(residuo)
            + " · costo insegnanti " + Formati.euro(costo) + " · margine previsto " + Formati.euro(margine));
    }

    private static BigDecimal somma(List<IscrittiCorso> corsi, Function<IscrittiCorso, BigDecimal> importo) {
        return corsi.stream().map(importo).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
