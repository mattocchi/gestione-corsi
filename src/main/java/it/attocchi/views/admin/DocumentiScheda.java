package it.attocchi.views.admin;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import it.attocchi.entity.TipoDocumento;
import it.attocchi.security.SecurityService;
import it.attocchi.service.PubblicazioneDocumentiService;
import it.attocchi.service.PubblicazioneDocumentiService.AnnoAttestazioni;
import it.attocchi.views.Stili;
import it.attocchi.views.ViewSupport;

import java.time.LocalDate;

/**
 * Scheda "Documenti" di Impostazioni (admin e segreteria): quali documenti vedono genitori e allievi adulti. Per ogni
 * tipo l'interruttore e la regola che vale comunque; per le attestazioni gli anni pubblicati (solo anni finiti).
 * Le modifiche si salvano subito.
 */
public class DocumentiScheda extends VerticalLayout {

    private final PubblicazioneDocumentiService pubblicazioneDocumentiService;
    private final String username;
    private final Div anni = new Div();

    public DocumentiScheda(PubblicazioneDocumentiService pubblicazioneDocumentiService, SecurityService securityService) {
        this.pubblicazioneDocumentiService = pubblicazioneDocumentiService;
        this.username = securityService.getUsername().orElse("");
        addClassNames("max-w-3xl");

        Paragraph spiegazione = new Paragraph("Genitori e allievi adulti vedono un documento in Documenti solo se il suo "
            + "tipo è visibile e la regola è rispettata. Admin e segreteria vedono e inviano sempre tutto.");
        spiegazione.addClassNames("m-0", Stili.TESTO_SECONDARIO);
        add(ViewSupport.sezione("Documenti per le famiglie"), spiegazione);
        for (TipoDocumento tipo : PubblicazioneDocumentiService.PER_LE_FAMIGLIE) {
            Checkbox visibile = new Checkbox(tipo.getPlurale(), pubblicazioneDocumentiService.visibile(tipo));
            visibile.setHelperText(regola(tipo));
            visibile.addValueChangeListener(e -> {
                if (!e.isFromClient()) {
                    return;
                }
                try {
                    pubblicazioneDocumentiService.imposta(tipo, e.getValue(), username);
                } catch (RuntimeException ex) {
                    visibile.setValue(e.getOldValue());
                    Notification.show(ex.getMessage());
                }
            });
            add(visibile);
        }

        Paragraph attestazioni = new Paragraph("Pubblica le attestazioni di un anno dopo averne controllato i pagamenti: "
            + "si possono pubblicare dal 1° gennaio dell'anno successivo.");
        attestazioni.addClassNames("m-0", Stili.TESTO_SECONDARIO);
        anni.addClassNames("flex flex-col gap-2");
        add(ViewSupport.sezione("Attestazioni delle spese per anno"), attestazioni, anni);
        aggiornaAnni();
    }

    /** Regola che vale anche con il tipo visibile. */
    static String regola(TipoDocumento tipo) {
        return switch (tipo) {
            case CONFERMA_ISCRIZIONE -> "Quando il corso è pubblicato";
            case RICEVUTA -> "Quando il corso è pubblicato e c'è almeno un pagamento";
            case RIEPILOGO -> "Per ogni anno scolastico con un corso pubblicato";
            case ATTESTAZIONE -> "Solo per gli anni pubblicati qui sotto";
            default -> "";
        };
    }

    private void aggiornaAnni() {
        anni.removeAll();
        LocalDate oggi = LocalDate.now();
        for (AnnoAttestazioni anno : pubblicazioneDocumentiService.anniAttestazioni(oggi)) {
            Checkbox pubblicato = new Checkbox("Attestazioni " + anno.anno(), anno.pubblicato());
            pubblicato.setEnabled(anno.pubblicabile());
            pubblicato.setHelperText(anno.pubblicabile() ? (anno.pubblicato() ? "Pubblicate" : "Non pubblicate")
                : "Pubblicabili dal 1° gennaio " + (anno.anno() + 1));
            pubblicato.addValueChangeListener(e -> {
                if (!e.isFromClient()) {
                    return;
                }
                try {
                    pubblicazioneDocumentiService.pubblicaAttestazioni(anno.anno(), e.getValue(), LocalDate.now(), username);
                } catch (RuntimeException ex) {
                    Notification.show(ex.getMessage());
                }
                aggiornaAnni();
            });
            anni.add(pubblicato);
        }
        if (anni.getComponentCount() == 0) {
            Paragraph nessuno = new Paragraph("Nessun pagamento con bonifico registrato.");
            nessuno.addClassNames("m-0", Stili.TESTO_SECONDARIO);
            anni.add(nessuno);
        }
    }
}
