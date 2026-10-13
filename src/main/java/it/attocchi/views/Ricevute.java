package it.attocchi.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import it.attocchi.entity.Allievo;
import it.attocchi.service.DocumentiService;
import it.attocchi.service.DocumentiService.Pdf;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.ByteArrayInputStream;

/**
 * Scarico della ricevuta in PDF (lo stesso documento allegato alle email). I permessi sono verificati da
 * {@link DocumentiService#ricevutaPer}: admin e segreteria tutte, il genitore solo quelle dei propri figli.
 */
public final class Ricevute {

    private Ricevute() {
    }

    /**
     * Link che scarica la ricevuta, con il pulsante indicato come contenuto. Se all'allievo mancano codice fiscale
     * o data di nascita il link non scarica nulla e il pulsante mostra l'avviso.
     */
    public static Anchor link(Long corsoId, Allievo allievo, Button contenuto) {
        String datiMancanti = DocumentiService.avvisoDatiMancanti(allievo);
        if (datiMancanti != null) {
            contenuto.addClickListener(e -> Notification.show(datiMancanti));
            Anchor senzaScarico = new Anchor();
            senzaScarico.add(contenuto);
            return senzaScarico;
        }
        Long allievoId = allievo.getId();
        // Servizio Spring tramite l'Instantiator di Vaadin, per non doverlo passare a ogni vista e dialog
        DocumentiService documenti = VaadinService.getCurrent().getInstantiator().getOrCreate(DocumentiService.class);
        Authentication autenticazione = SecurityContextHolder.getContext().getAuthentication();
        String username = autenticazione != null ? autenticazione.getName() : "";
        DownloadHandler handler = DownloadHandler.fromInputStream(evento -> {
            try {
                Pdf pdf = documenti.ricevutaPer(corsoId, allievoId, username);
                return new DownloadResponse(new ByteArrayInputStream(pdf.contenuto()), pdf.nomeFile(), "application/pdf",
                    pdf.contenuto().length);
            } catch (IllegalArgumentException | IllegalStateException ex) {
                return DownloadResponse.error(404);
            }
        });
        Anchor anchor = new Anchor(handler, "");
        anchor.add(contenuto);
        return anchor;
    }
}
