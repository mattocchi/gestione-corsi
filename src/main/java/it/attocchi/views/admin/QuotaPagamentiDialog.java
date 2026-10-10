package it.attocchi.views.admin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import it.attocchi.entity.Corso;
import it.attocchi.entity.Iscrizione;
import it.attocchi.entity.MetodoPagamento;
import it.attocchi.entity.Pagamento;
import it.attocchi.entity.Studente;
import it.attocchi.service.PagamentoService;
import it.attocchi.service.PagamentoService.Situazione;
import it.attocchi.views.Formati;
import it.attocchi.views.RicevutaView;
import it.attocchi.views.SegmentedControl;
import it.attocchi.views.StatoPagamentoUI;
import it.attocchi.views.ViewSupport;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * Quota da pagare e pagamenti di un partecipante a un corso (admin e segreteria).
 * Le modifiche sono salvate subito; la notifica compare solo in caso di errore.
 */
public class QuotaPagamentiDialog extends Dialog {

    private final PagamentoService pagamentoService;
    private final Corso corso;
    private final Studente studente;
    private final Div riepilogo = new Div();
    private final Grid<Pagamento> grid = new Grid<>(Pagamento.class, false);

    public QuotaPagamentiDialog(PagamentoService pagamentoService, Corso corso, Studente studente) {
        this.pagamentoService = pagamentoService;
        this.corso = corso;
        this.studente = studente;

        setHeaderTitle("Quota e pagamenti – " + studente.getNomeCompleto());
        setWidth("700px");
        setMaxWidth("100%");

        Iscrizione iscrizione = pagamentoService.findIscrizione(corso.getId(), studente.getId());

        Span corsoInfo = new Span(corso.getNome() + " · quota piena " + Formati.euro(corso.getCostoTotale()));
        corsoInfo.addClassName("avviso-sola-lettura");

        BigDecimalField quota = new BigDecimalField("Quota personalizzata");
        quota.setLocale(Locale.ITALY);
        quota.setPrefixComponent(new Span("€"));
        quota.setPlaceholder("quota piena");
        quota.setClearButtonVisible(true);
        quota.setHelperText("Vuota = quota piena del corso");
        quota.setValue(iscrizione.getQuotaPersonalizzata());

        TextField motivo = new TextField("Motivo");
        motivo.setPlaceholder("es. fratelli");
        motivo.setValue(iscrizione.getNotaQuota() != null ? iscrizione.getNotaQuota() : "");
        motivo.setEnabled(iscrizione.getQuotaPersonalizzata() != null);

        quota.addValueChangeListener(e -> {
            motivo.setEnabled(e.getValue() != null);
            salvaQuota(e.getValue(), motivo.getValue());
        });
        motivo.addValueChangeListener(e -> salvaQuota(quota.getValue(), e.getValue()));

        FormLayout quotaForm = new FormLayout(quota, motivo);

        Button aggiungi = new Button("Aggiungi pagamento", VaadinIcon.PLUS.create(), e -> openPagamento(new Pagamento()));
        aggiungi.addThemeVariants(ButtonVariant.PRIMARY);
        configureGrid();
        aggiorna();

        VerticalLayout content = new VerticalLayout(corsoInfo, ViewSupport.sezione("Quota"), quotaForm, riepilogo,
            ViewSupport.sezione("Pagamenti"), aggiungi, grid);
        content.setPadding(false);
        add(content);
        Button ricevuta = new Button("Ricevuta", VaadinIcon.PRINT.create());
        getFooter().add(RicevutaView.link(corso.getId(), studente.getId(), ricevuta), ViewSupport.closeButton(this, "Chiudi"));
    }

    private void configureGrid() {
        grid.addColumn(p -> Formati.data(p.getData())).setHeader("Data").setComparator(Pagamento::getData);
        grid.addColumn(p -> Formati.euro(p.getImporto())).setHeader("Importo").setWidth("7rem").setFlexGrow(0);
        var metodo = grid.addColumn(p -> p.getMetodo().getDescrizione()).setHeader("Metodo").setWidth("6.5rem").setFlexGrow(0);
        var nota = grid.addColumn(Pagamento::getNota).setHeader("Nota");
        var azioni = grid.addComponentColumn(p -> {
            Button modifica = ViewSupport.iconButton(VaadinIcon.PENCIL, "Modifica pagamento", e -> openPagamento(p));
            Button elimina = ViewSupport.iconButton(VaadinIcon.TRASH, "Elimina pagamento", e -> confermaEliminazione(p));
            elimina.addThemeVariants(ButtonVariant.ERROR);
            return ViewSupport.actions(modifica, elimina);
        });
        ViewSupport.configureActionsColumn(azioni);
        azioni.setHeader("").setAutoWidth(false).setWidth("6rem");
        grid.setAllRowsVisible(true);
        grid.setEmptyStateText("Nessun pagamento registrato");
        ViewSupport.hideOnMobile(grid, metodo, nota);
    }

    /** Dialog di inserimento/modifica di una rata: data (default oggi), importo (default il residuo), metodo, nota. */
    private void openPagamento(Pagamento pagamento) {
        boolean nuovo = pagamento.getId() == null;
        Dialog dialog = ViewSupport.dialog(nuovo ? "Nuovo pagamento" : "Modifica pagamento", "420px");

        DatePicker data = new DatePicker("Data");
        data.setLocale(Locale.ITALY);
        BigDecimalField importo = new BigDecimalField("Importo");
        importo.setLocale(Locale.ITALY);
        importo.setPrefixComponent(new Span("€"));
        SegmentedControl<MetodoPagamento> metodo = new SegmentedControl<>("Metodo di pagamento",
            List.of(MetodoPagamento.values()), MetodoPagamento::getDescrizione, MetodoPagamento::getDescrizione, null);
        TextField nota = new TextField("Nota");
        nota.setPlaceholder("es. riferimento del bonifico");

        Binder<Pagamento> binder = new Binder<>(Pagamento.class);
        binder.forField(data).asRequired("Inserisci la data del pagamento").bind(Pagamento::getData, Pagamento::setData);
        binder.forField(importo).asRequired("Inserisci l'importo")
            .withValidator(i -> i.signum() > 0, "L'importo deve essere maggiore di zero")
            .bind(Pagamento::getImporto, Pagamento::setImporto);
        binder.forField(metodo).bind(Pagamento::getMetodo, Pagamento::setMetodo);
        binder.forField(nota).bind(Pagamento::getNota, Pagamento::setNota);
        if (nuovo) {
            pagamento.setData(LocalDate.now());
            BigDecimal residuo = pagamentoService.situazione(corso.getId(), studente.getId()).residuo();
            pagamento.setImporto(residuo.signum() > 0 ? residuo : null);
        }
        binder.readBean(pagamento);

        Button salva = new Button("Salva", e -> {
            if (!binder.writeBeanIfValid(pagamento)) {
                return;
            }
            try {
                pagamentoService.salva(corso.getId(), studente.getId(), pagamento);
                dialog.close();
                aggiorna();
            } catch (RuntimeException ex) {
                Notification.show("Errore nel salvataggio del pagamento: " + ex.getMessage());
            }
        });
        salva.addThemeVariants(ButtonVariant.PRIMARY);

        VerticalLayout form = new VerticalLayout(data, importo, metodo, nota);
        form.setPadding(false);
        data.setWidthFull();
        importo.setWidthFull();
        nota.setWidthFull();
        dialog.add(form);
        dialog.getFooter().add(ViewSupport.closeButton(dialog, "Annulla"), salva);
        dialog.open();
    }

    private void confermaEliminazione(Pagamento pagamento) {
        Dialog conferma = ViewSupport.dialog("Eliminare il pagamento?", "420px");
        conferma.add(new Span(Formati.euro(pagamento.getImporto()) + " del " + Formati.data(pagamento.getData())
            + " (" + pagamento.getMetodo().getDescrizione() + ")"));
        Button elimina = new Button("Elimina", e -> {
            try {
                pagamentoService.elimina(pagamento);
                conferma.close();
                aggiorna();
            } catch (RuntimeException ex) {
                Notification.show("Errore nell'eliminazione del pagamento: " + ex.getMessage());
            }
        });
        elimina.addThemeVariants(ButtonVariant.PRIMARY, ButtonVariant.ERROR);
        conferma.getFooter().add(ViewSupport.closeButton(conferma, "Annulla"), elimina);
        conferma.open();
    }

    private void aggiorna() {
        grid.setItems(pagamentoService.findPagamenti(corso.getId(), studente.getId()));
        aggiornaRiepilogo();
    }

    private void salvaQuota(BigDecimal valore, String nota) {
        try {
            pagamentoService.impostaQuota(corso.getId(), studente.getId(), valore, nota);
            aggiornaRiepilogo();
        } catch (RuntimeException ex) {
            Notification.show("Errore nel salvataggio della quota: " + ex.getMessage());
        }
    }

    /** Riga con da pagare, pagato, residuo e stato. */
    private void aggiornaRiepilogo() {
        Situazione situazione = pagamentoService.situazione(corso.getId(), studente.getId());
        riepilogo.removeAll();
        riepilogo.addClassName("pagamenti-riepilogo");
        riepilogo.add(new Span("Da pagare " + Formati.euro(situazione.quota())),
            new Span("Pagato " + Formati.euro(situazione.pagato())),
            new Span("Residuo " + Formati.euro(situazione.residuo())),
            StatoPagamentoUI.badge(situazione));
    }
}
