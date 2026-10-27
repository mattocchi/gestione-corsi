package it.attocchi.views.admin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import it.attocchi.entity.Insegnante;
import it.attocchi.entity.MetodoPagamento;
import it.attocchi.entity.Rimborso;
import it.attocchi.service.BilancioService;
import it.attocchi.service.RimborsoService;
import it.attocchi.views.CardMobile;
import it.attocchi.views.Formati;
import it.attocchi.views.RiepilogoLezioniMese;
import it.attocchi.views.SegmentedControl;
import it.attocchi.views.Stili;
import it.attocchi.views.ViewSupport;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

/**
 * Rimborsi di un insegnante in un anno scolastico: elenco con totale, aggiunta e modifica (admin e segreteria),
 * eliminazione solo per l'admin.
 */
public class RimborsiDialog extends Dialog {

    private final RimborsoService rimborsoService;
    private final BilancioService bilancioService;
    private final Insegnante insegnante;
    private final String annoScolastico;
    private final boolean puoEliminare;
    private final Runnable dopoModifica;
    private final Grid<Rimborso> grid = new Grid<>(Rimborso.class, false);
    private final Span totale = new Span();

    public RimborsiDialog(RimborsoService rimborsoService, BilancioService bilancioService, Insegnante insegnante,
                          String annoScolastico, boolean puoEliminare, Runnable dopoModifica) {
        this.rimborsoService = rimborsoService;
        this.bilancioService = bilancioService;
        this.insegnante = insegnante;
        this.annoScolastico = annoScolastico;
        this.puoEliminare = puoEliminare;
        this.dopoModifica = dopoModifica;

        setHeaderTitle("Rimborsi " + insegnante.getNomeCompleto() + " – " + annoScolastico);
        setWidth("760px");
        setMaxWidth("100%");

        Span tariffa = new Span("Tariffa oraria " + (insegnante.getTariffaOraria() != null
            ? Formati.euro(insegnante.getTariffaOraria()) : "non impostata")
            + (insegnante.getRimborsoForfettario() != null && insegnante.getRimborsoForfettario().signum() > 0
                ? " · forfait annuo " + Formati.euro(insegnante.getRimborsoForfettario()) + " (non compreso nell'importo proposto)" : ""));
        tariffa.addClassNames(Stili.TESTO_SECONDARIO);
        Button aggiungi = new Button("Aggiungi rimborso", VaadinIcon.PLUS.create(), e -> apriRimborso(new Rimborso()));
        aggiungi.addThemeVariants(ButtonVariant.PRIMARY);
        configureGrid();

        VerticalLayout content = new VerticalLayout(tariffa, totale, aggiungi, grid);
        content.addClassNames("p-0");
        add(content);
        getFooter().add(ViewSupport.closeButton(this, "Chiudi"));
        aggiorna();
    }

    private void configureGrid() {
        grid.addColumn(r -> Formati.mese(r.getMese())).setHeader("Mese").setComparator(Rimborso::getMese);
        grid.addColumn(r -> Formati.data(r.getData())).setHeader("Pagato il").setComparator(Rimborso::getData);
        grid.addColumn(r -> Formati.euro(r.getImporto())).setHeader("Importo").setWidth("7.5rem").setFlexGrow(0);
        grid.addColumn(r -> r.getMetodo().getDescrizione()).setHeader("Metodo").setWidth("6.5rem").setFlexGrow(0);
        grid.addColumn(Rimborso::getNota).setHeader("Nota");
        var azioni = grid.addComponentColumn(this::createAzioni);
        ViewSupport.configureActionsColumn(azioni);
        azioni.setHeader("").setAutoWidth(false).setWidth(puoEliminare ? "6rem" : "3.5rem");
        grid.setAllRowsVisible(true);
        grid.setEmptyStateText("Nessun rimborso registrato");
        CardMobile.per(grid)
            .titolo(r -> Formati.mese(r.getMese()))
            .campo("Pagato il", r -> Formati.data(r.getData()))
            .campo("Importo", r -> Formati.euro(r.getImporto()))
            .campo("Metodo", r -> r.getMetodo().getDescrizione())
            .campo("Nota", Rimborso::getNota)
            .azioni(this::createAzioni)
            .applica();
    }

    private HorizontalLayout createAzioni(Rimborso r) {
        Button modifica = ViewSupport.iconButton(VaadinIcon.PENCIL, "Modifica rimborso", e -> apriRimborso(r));
        if (!puoEliminare) {
            return ViewSupport.actions(modifica);
        }
        Button elimina = ViewSupport.iconButton(VaadinIcon.TRASH, "Elimina rimborso", e -> confermaEliminazione(r));
        elimina.addThemeVariants(ButtonVariant.ERROR);
        return ViewSupport.actions(modifica, elimina);
    }

    /**
     * Inserimento/modifica: data (default oggi), mese di riferimento tra quelli dell'anno scolastico (default il mese
     * precedente), importo proposto con il maturato del mese, metodo e nota.
     */
    private void apriRimborso(Rimborso rimborso) {
        boolean nuovo = rimborso.getId() == null;
        Dialog dialog = ViewSupport.dialog(nuovo ? "Nuovo rimborso" : "Modifica rimborso", "720px");

        DatePicker data = ViewSupport.datePicker("Pagato il");
        YearMonth primo = YearMonth.from(RimborsoService.inizioAnno(annoScolastico));
        List<YearMonth> mesi = IntStream.range(0, 12).mapToObj(primo::plusMonths).toList();
        ComboBox<YearMonth> mese = new ComboBox<>("Mese di riferimento");
        mese.setItems(mesi);
        mese.setItemLabelGenerator(Formati::mese);
        BigDecimalField importo = new BigDecimalField("Importo");
        importo.setLocale(Locale.ITALY);
        importo.setPrefixComponent(new Span("€"));
        SegmentedControl<MetodoPagamento> metodo = new SegmentedControl<>("Metodo di pagamento",
            List.of(MetodoPagamento.values()), MetodoPagamento::getDescrizione, MetodoPagamento::getDescrizione, null);
        TextField nota = new TextField("Nota");

        Binder<Rimborso> binder = new Binder<>(Rimborso.class);
        binder.forField(data).asRequired("Inserisci la data del pagamento").bind(Rimborso::getData, Rimborso::setData);
        binder.forField(mese).asRequired("Scegli il mese di riferimento").bind(Rimborso::getMese, Rimborso::setMese);
        binder.forField(importo).asRequired("Inserisci l'importo")
            .withValidator(i -> i.signum() > 0, "L'importo deve essere maggiore di zero")
            .bind(Rimborso::getImporto, Rimborso::setImporto);
        binder.forField(metodo).bind(Rimborso::getMetodo, Rimborso::setMetodo);
        binder.forField(nota).bind(Rimborso::getNota, Rimborso::setNota);
        if (nuovo) {
            rimborso.setData(LocalDate.now());
            YearMonth precedente = YearMonth.now().minusMonths(1);
            rimborso.setMese(mesi.contains(precedente) ? precedente : mesi.getFirst());
        }
        binder.readBean(rimborso);
        if (nuovo) {
            // Importo proposto: maturato del mese non ancora rimborsato, aggiornato quando si cambia mese
            proponiImporto(importo, mese.getValue());
            mese.addValueChangeListener(e -> proponiImporto(importo, e.getValue()));
        }
        // Riferimento teorico: le lezioni del mese con ore e preventivo, aggiornate quando si cambia mese
        RiepilogoLezioniMese lezioni = new RiepilogoLezioniMese();
        lezioni.mostra(mese.getValue() != null ? bilancioService.lezioniDelMese(insegnante.getId(), mese.getValue()) : null);
        mese.addValueChangeListener(e -> lezioni.mostra(e.getValue() != null
            ? bilancioService.lezioniDelMese(insegnante.getId(), e.getValue()) : null));

        Button salva = new Button("Salva", e -> {
            if (!binder.writeBeanIfValid(rimborso)) {
                return;
            }
            try {
                rimborsoService.salva(insegnante.getId(), rimborso);
                dialog.close();
                aggiorna();
                dopoModifica.run();
            } catch (RuntimeException ex) {
                Notification.show("Errore nel salvataggio del rimborso: " + ex.getMessage());
            }
        });
        salva.addThemeVariants(ButtonVariant.PRIMARY);

        VerticalLayout form = new VerticalLayout(data, mese, importo, metodo, nota);
        form.addClassNames("p-0");
        data.addClassNames("w-full");
        mese.addClassNames("w-full");
        importo.addClassNames("w-full");
        nota.addClassNames("w-full");
        dialog.add(form, ViewSupport.sezione("Lezioni del mese"), lezioni);
        dialog.getFooter().add(ViewSupport.closeButton(dialog, "Annulla"), salva);
        dialog.open();
    }

    private void proponiImporto(BigDecimalField importo, YearMonth mese) {
        if (mese == null) {
            return;
        }
        BigDecimal proposta = bilancioService.propostaRimborso(insegnante.getId(), mese);
        importo.setValue(proposta.signum() > 0 ? proposta : null);
        importo.setHelperText("Maturato non rimborsato di " + Formati.mese(mese) + ": " + Formati.euro(proposta));
    }

    private void confermaEliminazione(Rimborso rimborso) {
        Dialog conferma = ViewSupport.dialog("Eliminare il rimborso?", "420px");
        conferma.add(new Span(Formati.euro(rimborso.getImporto()) + " di " + Formati.mese(rimborso.getMese())
            + " pagato il " + Formati.data(rimborso.getData())));
        Button elimina = new Button("Elimina", e -> {
            try {
                rimborsoService.elimina(rimborso.getId());
                conferma.close();
                aggiorna();
                dopoModifica.run();
            } catch (RuntimeException ex) {
                Notification.show("Errore nell'eliminazione del rimborso: " + ex.getMessage());
            }
        });
        elimina.addThemeVariants(ButtonVariant.PRIMARY, ButtonVariant.ERROR);
        conferma.getFooter().add(ViewSupport.closeButton(conferma, "Annulla"), elimina);
        conferma.open();
    }

    private void aggiorna() {
        grid.setItems(rimborsoService.rimborsi(insegnante.getId(), annoScolastico));
        totale.setText("Totale rimborsato: " + Formati.euro(rimborsoService.totale(insegnante.getId(), annoScolastico)));
    }
}
