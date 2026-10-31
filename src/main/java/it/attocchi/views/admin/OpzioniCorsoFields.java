package it.attocchi.views.admin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;
import it.attocchi.entity.OpzioneCorso;
import it.attocchi.views.Stili;
import it.attocchi.views.ViewSupport;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Sezione "Opzioni aggiuntive" del dialog del corso: una riga per opzione (descrizione e importo) e "Aggiungi opzione".
 * Le opzioni si salvano con il corso ({@code CorsoService.salva(corso, opzioni)}); togliere un'opzione già salvata è
 * solo per l'admin (il servizio rifiuta quelle attive per qualche iscritto).
 */
class OpzioniCorsoFields extends Div {

    /** Una riga: l'opzione salvata ({@code null} se nuova) con i suoi campi. */
    private record Riga(OpzioneCorso opzione, TextField descrizione, BigDecimalField importo, Div contenitore) {
    }

    private final List<Riga> righe = new ArrayList<>();
    private final Div elenco = new Div();
    private final boolean puoEliminare;

    /** @param puoEliminare {@code true} per l'admin, che può togliere anche le opzioni già salvate */
    OpzioniCorsoFields(List<OpzioneCorso> opzioni, boolean puoEliminare) {
        this.puoEliminare = puoEliminare;
        Span titolo = new Span("Opzioni aggiuntive");
        titolo.addClassNames(Stili.TITOLO_SEZIONE);
        Span aiuto = new Span("Importi una tantum (es. kit) che si sommano alla quota di chi li attiva in Partecipanti.");
        aiuto.addClassNames(Stili.TESTO_SECONDARIO);
        elenco.addClassNames("flex flex-col gap-1");
        Button aggiungi = new Button("Aggiungi opzione", VaadinIcon.PLUS.create(), e -> aggiungi(null).descrizione().focus());
        aggiungi.addClassNames("self-start");
        opzioni.forEach(this::aggiungi);
        add(titolo, aiuto, elenco, aggiungi);
        addClassNames("flex flex-col gap-1 w-full");
    }

    private Riga aggiungi(OpzioneCorso opzione) {
        TextField descrizione = new TextField("Descrizione");
        descrizione.setMaxLength(OpzioneCorso.LUNGHEZZA_DESCRIZIONE);
        descrizione.setPlaceholder("Es. Kit strumentario");
        descrizione.addClassNames("grow");
        BigDecimalField importo = new BigDecimalField("Importo");
        importo.setLocale(Locale.ITALY);
        importo.setPrefixComponent(new Span("€"));
        importo.addClassNames("w-32");
        if (opzione != null) {
            descrizione.setValue(opzione.getDescrizione());
            importo.setValue(opzione.getImporto());
        }
        Div contenitore = new Div(descrizione, importo);
        contenitore.addClassNames("flex flex-wrap items-baseline gap-2");
        Riga riga = new Riga(opzione, descrizione, importo, contenitore);
        // Le nuove si tolgono sempre; quelle salvate solo l'admin
        if (opzione == null || puoEliminare) {
            String nome = opzione != null ? opzione.getDescrizione() : "nuova opzione";
            Button togli = ViewSupport.iconButton(VaadinIcon.TRASH, "Togli " + nome, e -> {
                righe.remove(riga);
                elenco.remove(contenitore);
            });
            togli.addThemeVariants(ButtonVariant.ERROR);
            contenitore.add(togli);
        }
        righe.add(riga);
        elenco.add(contenitore);
        return riga;
    }

    /** Controlla descrizione e importo (maggiore di zero) di ogni riga, mostrando gli errori sui campi. */
    boolean valida() {
        boolean valida = true;
        for (Riga riga : righe) {
            boolean senzaDescrizione = riga.descrizione().getValue().isBlank();
            riga.descrizione().setInvalid(senzaDescrizione);
            riga.descrizione().setErrorMessage(senzaDescrizione ? "Inserisci la descrizione" : null);
            boolean senzaImporto = riga.importo().getValue() == null || riga.importo().getValue().signum() <= 0;
            riga.importo().setInvalid(senzaImporto);
            riga.importo().setErrorMessage(senzaImporto ? "Maggiore di zero" : null);
            valida &= !senzaDescrizione && !senzaImporto;
        }
        return valida;
    }

    /** Opzioni da salvare: quelle salvate con i nuovi valori e le nuove (senza id). */
    List<OpzioneCorso> getOpzioni() {
        List<OpzioneCorso> opzioni = new ArrayList<>();
        for (Riga riga : righe) {
            OpzioneCorso opzione = riga.opzione() != null ? riga.opzione() : new OpzioneCorso();
            opzione.setDescrizione(riga.descrizione().getValue());
            opzione.setImporto(riga.importo().getValue());
            opzioni.add(opzione);
        }
        return opzioni;
    }
}
