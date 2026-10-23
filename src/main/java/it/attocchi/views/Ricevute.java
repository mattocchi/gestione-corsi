package it.attocchi.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.server.VaadinService;
import it.attocchi.entity.Allievo;
import it.attocchi.service.DocumentiService;
import it.attocchi.service.DocumentiService.Pdf;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;


/**
 * Ricevuta in PDF (lo stesso documento allegato alle email), in anteprima in una nuova scheda o da scaricare
 * ({@link MenuPdf}). I permessi sono verificati da {@link DocumentiService#ricevutaPer}: admin e segreteria tutte, il
 * genitore solo quelle dei propri figli, l'allievo adulto le proprie.
 */
public final class Ricevute {

    private Ricevute() {
    }

    /**
     * Pulsante della ricevuta: con {@code testo} nullo la sola icona della stampante (righe delle tabelle). Se
     * all'allievo mancano codice fiscale o data di nascita il pulsante mostra solo l'avviso.
     */
    public static Component pulsante(Long corsoId, Allievo allievo, String testo) {
        String datiMancanti = DocumentiService.avvisoDatiMancanti(allievo);
        if (datiMancanti != null) {
            Button avviso = testo != null ? new Button(testo, VaadinIcon.PRINT.create()) : ViewSupport.iconButton(VaadinIcon.PRINT, "Ricevuta");
            avviso.addClickListener(e -> Notification.show(datiMancanti));
            return avviso;
        }
        Long allievoId = allievo.getId();
        // Servizio Spring tramite l'Instantiator di Vaadin, per non doverlo passare a ogni vista e dialog
        DocumentiService documenti = VaadinService.getCurrent().getInstantiator().getOrCreate(DocumentiService.class);
        Authentication autenticazione = SecurityContextHolder.getContext().getAuthentication();
        String username = autenticazione != null ? autenticazione.getName() : "";
        SerializableSupplier<Pdf> pdf = () -> documenti.ricevutaPer(corsoId, allievoId, username);
        return testo != null ? MenuPdf.conTesto(testo, VaadinIcon.PRINT, pdf) : MenuPdf.soloIcona(VaadinIcon.PRINT, "Ricevuta", pdf);
    }
}
