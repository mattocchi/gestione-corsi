package it.attocchi.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
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
 * Scarico del riepilogo della famiglia in PDF: si sceglie l'anno scolastico tra quelli dei corsi dei figli.
 * I permessi sono verificati da {@link DocumentiService#riepilogoFamigliaPer}: admin e segreteria ogni famiglia,
 * il genitore solo la propria.
 */
public final class RiepilogoFamigliaDialog {

    /** Valore della scelta per i corsi senza anno scolastico (la ComboBox non ammette {@code null}). */
    private static final String SENZA_ANNO = "";

    private RiepilogoFamigliaDialog() {
    }

    public static void apri(Genitore genitore) {
        // Servizi Spring tramite l'Instantiator di Vaadin, come in Ricevute
        PagamentoService pagamenti = VaadinService.getCurrent().getInstantiator().getOrCreate(PagamentoService.class);
        DocumentiService documenti = VaadinService.getCurrent().getInstantiator().getOrCreate(DocumentiService.class);
        Authentication autenticazione = SecurityContextHolder.getContext().getAuthentication();
        String username = autenticazione != null ? autenticazione.getName() : "";

        Dialog dialog = ViewSupport.dialog("Riepilogo famiglia", "420px");
        List<String> anni = pagamenti.anniDellaFamiglia(genitore.getId()).stream()
            .map(anno -> Objects.requireNonNullElse(anno, SENZA_ANNO))
            .toList();
        if (anni.isEmpty()) {
            dialog.add(new Paragraph("Nessun figlio di " + genitore.getNomeCompleto() + " è iscritto a un corso."));
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
        Paragraph testo = new Paragraph("Corsi dei figli di " + genitore.getNomeCompleto()
            + " nell'anno scelto, con quota, pagato, residuo e totale della spesa.");
        testo.addClassNames(Stili.TESTO_SECONDARIO);
        dialog.add(testo, anno);

        Long genitoreId = genitore.getId();
        DownloadHandler handler = DownloadHandler.fromInputStream(evento -> {
            String scelto = anno.getValue();
            if (scelto == null) {
                return DownloadResponse.error(404);
            }
            try {
                Pdf pdf = documenti.riepilogoFamigliaPer(genitoreId, scelto.isEmpty() ? null : scelto, username);
                return new DownloadResponse(new ByteArrayInputStream(pdf.contenuto()), pdf.nomeFile(), "application/pdf",
                    pdf.contenuto().length);
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
}
