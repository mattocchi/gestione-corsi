package it.attocchi.views.admin;

import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;
import it.attocchi.entity.Corso;
import it.attocchi.entity.Iscrizione;
import it.attocchi.entity.Studente;
import it.attocchi.service.PagamentoService;
import it.attocchi.service.PagamentoService.Situazione;
import it.attocchi.views.Formati;
import it.attocchi.views.ViewSupport;

import java.math.BigDecimal;
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
        aggiornaRiepilogo();

        VerticalLayout content = new VerticalLayout(corsoInfo, sezione("Quota"), quotaForm, riepilogo);
        content.setPadding(false);
        add(content);
        getFooter().add(ViewSupport.closeButton(this, "Chiudi"));
    }

    private static H3 sezione(String titolo) {
        H3 h3 = new H3(titolo);
        h3.addClassName("dialog-section-title");
        return h3;
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
            statoBadge(situazione));
    }

    static Badge statoBadge(Situazione situazione) {
        return switch (situazione.stato()) {
            case SALDATO -> {
                Badge badge = new Badge("Saldato", VaadinIcon.CHECK.create());
                badge.addThemeVariants(BadgeVariant.SUCCESS);
                yield badge;
            }
            case PARZIALE -> {
                Badge badge = new Badge("Parziale " + situazione.percentualePagata() + "%", VaadinIcon.HOURGLASS.create());
                badge.addThemeVariants(BadgeVariant.WARNING);
                yield badge;
            }
            case NESSUN_PAGAMENTO -> new Badge("Da pagare", VaadinIcon.CLOCK.create());
        };
    }
}
