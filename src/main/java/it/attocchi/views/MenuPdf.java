package it.attocchi.views;

import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AttachmentType;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.server.streams.InputStreamDownloadHandler;
import it.attocchi.service.DocumentiService.Pdf;

import java.io.ByteArrayInputStream;

/**
 * Pulsante dei PDF con un menu: "Anteprima in una nuova scheda" (il PDF aperto dal browser) o "Scarica PDF". Le voci
 * sono link, non azioni sul server: così la nuova scheda non viene bloccata come popup. Il PDF è generato a ogni
 * richiesta dal fornitore indicato (che controlla i permessi); un errore diventa una risposta 404.
 */
public final class MenuPdf {

    private MenuPdf() {
    }

    /** Pulsante con testo e icona (dialog e barre degli strumenti). */
    public static MenuBar conTesto(String testo, VaadinIcon icona, SerializableSupplier<Pdf> pdf) {
        MenuBar menu = new MenuBar();
        MenuItem voce = menu.addItem(icona.create());
        voce.add(new Span(testo));
        voce.setAriaLabel(testo + ": anteprima o scarica il PDF");
        voci(voce, pdf);
        return menu;
    }

    /** Pulsante con la sola icona (azioni nelle righe delle tabelle); {@code etichetta} è il nome accessibile. */
    public static MenuBar soloIcona(VaadinIcon icona, String etichetta, SerializableSupplier<Pdf> pdf) {
        MenuBar menu = new MenuBar();
        // Larghezza fissa (icona + freccia del sottomenu di Aura): il menu disegna il pulsante dopo che la colonna
        // "Azioni" ha calcolato la sua larghezza automatica, che altrimenti non lo conterebbe e lo taglierebbe
        menu.addClassNames("flex-none w-[4.5rem]");
        MenuItem voce = menu.addItem(icona.create());
        voce.setAriaLabel(etichetta);
        menu.setTooltipText(voce, etichetta);
        voci(voce, pdf);
        return menu;
    }

    private static void voci(MenuItem voce, SerializableSupplier<Pdf> pdf) {
        Anchor anteprima = new Anchor(handler(pdf).inline(), AttachmentType.INLINE, "");
        anteprima.setTarget("_blank");
        anteprima.add(VaadinIcon.EYE.create(), new Span("Anteprima in una nuova scheda"));
        Anchor scarica = new Anchor(handler(pdf), "");
        scarica.add(VaadinIcon.DOWNLOAD.create(), new Span("Scarica PDF"));
        for (Anchor link : new Anchor[] {anteprima, scarica}) {
            // Il link occupa tutta la voce del menu, testo normale (non sottolineato)
            link.addClassNames("flex items-center gap-2 w-full no-underline text-inherit");
            voce.getSubMenu().addItem(link);
        }
    }

    private static InputStreamDownloadHandler handler(SerializableSupplier<Pdf> pdf) {
        return DownloadHandler.fromInputStream(evento -> {
            try {
                Pdf documento = pdf.get();
                return new DownloadResponse(new ByteArrayInputStream(documento.contenuto()), documento.nomeFile(),
                    "application/pdf", documento.contenuto().length);
            } catch (IllegalArgumentException | IllegalStateException ex) {
                return DownloadResponse.error(404);
            }
        });
    }
}
