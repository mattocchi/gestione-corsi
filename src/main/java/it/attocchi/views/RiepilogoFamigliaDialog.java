package it.attocchi.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import it.attocchi.entity.Allievo;
import it.attocchi.entity.Genitore;
import it.attocchi.service.DocumentiService;
import it.attocchi.service.DocumentiService.Pdf;
import it.attocchi.service.PagamentoService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Objects;

/**
 * Scarico del riepilogo in PDF: della famiglia (anni dei corsi dei figli) o di un allievo adulto senza genitore
 * (anni dei suoi corsi). I permessi sono verificati da {@link DocumentiService#riepilogoFamigliaPer} e
 * {@link DocumentiService#riepilogoAllievoPer}: admin e segreteria tutti, genitore e allievo adulto solo il proprio.
 */
public final class RiepilogoFamigliaDialog {

    /** Valore della scelta per i corsi senza anno scolastico (la ComboBox non ammette {@code null}). */
    private static final String SENZA_ANNO = "";

    private RiepilogoFamigliaDialog() {
    }

    public static void apri(Genitore genitore) {
        PagamentoService pagamenti = servizio(PagamentoService.class);
        DocumentiService documenti = servizio(DocumentiService.class);
        String username = username();
        Long genitoreId = genitore.getId();
        apri("Riepilogo famiglia", pagamenti.anniDellaFamiglia(genitoreId),
            "Nessun figlio di " + genitore.getNomeCompleto() + " è iscritto a un corso.",
            "Corsi dei figli di " + genitore.getNomeCompleto() + " nell'anno scelto, con quota, pagato, residuo e totale della spesa.",
            anno -> documenti.riepilogoFamigliaPer(genitoreId, anno, username));
    }

    /** Riepilogo dei corsi di un allievo adulto (senza genitore), per i suoi Pagamenti. */
    public static void apri(Allievo allievo) {
        PagamentoService pagamenti = servizio(PagamentoService.class);
        DocumentiService documenti = servizio(DocumentiService.class);
        String username = username();
        Long allievoId = allievo.getId();
        apri("Riepilogo pagamenti", pagamenti.anniDellAllievo(allievoId),
            allievo.getNomeCompleto() + " non è iscritto a nessun corso.",
            "Corsi di " + allievo.getNomeCompleto() + " nell'anno scelto, con quota, pagato, residuo e totale della spesa.",
            anno -> documenti.riepilogoAllievoPer(allievoId, anno, username));
    }

    /** Dialog comune: scelta dell'anno scolastico e scarico del PDF ({@code pdf} riceve l'anno, {@code null} = senza anno). */
    private static void apri(String titolo, List<String> anniDisponibili, String nessunCorso, String descrizione,
                             SerializableFunction<String, Pdf> pdf) {
        Dialog dialog = ViewSupport.dialog(titolo, "420px");
        List<String> anni = anniDisponibili.stream()
            .map(anno -> Objects.requireNonNullElse(anno, SENZA_ANNO))
            .toList();
        if (anni.isEmpty()) {
            dialog.add(new Paragraph(nessunCorso));
            dialog.getFooter().add(ViewSupport.closeButton(dialog, "Chiudi"));
            dialog.open();
            return;
        }

        ComboBox<String> anno = ViewSupport.filtroAnnoScolastico(anni);
        anno.setLabel("Anno scolastico");
        anno.setItemLabelGenerator(a -> a.isEmpty() ? "Senza anno scolastico" : a);
        anno.setAllowCustomValue(false);
        anno.setRequired(true);
        anno.addClassNames("w-full");
        Paragraph testo = new Paragraph(descrizione);
        testo.addClassNames(Stili.TESTO_SECONDARIO);
        dialog.add(testo, anno);

        DownloadHandler handler = DownloadHandler.fromInputStream(evento -> {
            String scelto = anno.getValue();
            if (scelto == null) {
                return DownloadResponse.error(404);
            }
            try {
                Pdf documento = pdf.apply(scelto.isEmpty() ? null : scelto);
                return new DownloadResponse(new ByteArrayInputStream(documento.contenuto()), documento.nomeFile(),
                    "application/pdf", documento.contenuto().length);
            } catch (IllegalArgumentException | IllegalStateException ex) {
                return DownloadResponse.error(404);
            }
        });
        Button scarica = new Button("Scarica PDF", VaadinIcon.DOWNLOAD.create());
        scarica.addThemeVariants(ButtonVariant.PRIMARY);
        Anchor link = new Anchor(handler, "");
        link.add(scarica);
        // Senza anno scelto non si scarica
        anno.addValueChangeListener(e -> scarica.setEnabled(e.getValue() != null));

        dialog.getFooter().add(ViewSupport.closeButton(dialog, "Chiudi"), link);
        dialog.open();
    }

    private static <T> T servizio(Class<T> tipo) {
        // Servizi Spring tramite l'Instantiator di Vaadin, come in Ricevute
        return VaadinService.getCurrent().getInstantiator().getOrCreate(tipo);
    }

    private static String username() {
        Authentication autenticazione = SecurityContextHolder.getContext().getAuthentication();
        return autenticazione != null ? autenticazione.getName() : "";
    }
}
