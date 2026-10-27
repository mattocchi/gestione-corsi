package it.attocchi.views;

import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AttachmentType;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.server.streams.InputStreamDownloadHandler;
import it.attocchi.entity.AperturaDocumento.Modalita;
import it.attocchi.service.DocumentiService.Pdf;

import java.io.ByteArrayInputStream;

/**
 * Pulsante dei PDF con un menu: "Anteprima in una nuova scheda" (il PDF aperto dal browser) o "Scarica PDF". Le voci
 * sono link, non azioni sul server: così la nuova scheda non viene bloccata come popup. Il PDF è generato a ogni
 * richiesta dal fornitore indicato (che controlla i permessi); un errore diventa una risposta 404. Con {@code dopo}
 * ogni PDF generato viene segnalato con la modalità (anteprima o scarico), per la cronologia dei documenti.
 * Famiglie e insegnanti non scaricano: {@link #anteprimaEInvio} sostituisce "Scarica PDF" con "Invia alla mia email".
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
        voci(voce, pdf, null);
        return menu;
    }

    /** Pulsante con la sola icona (azioni nelle righe delle tabelle); {@code etichetta} è il nome accessibile. */
    public static MenuBar soloIcona(VaadinIcon icona, String etichetta, SerializableSupplier<Pdf> pdf) {
        return soloIcona(icona, etichetta, pdf, null);
    }

    /** Come {@link #soloIcona(VaadinIcon, String, SerializableSupplier)}, segnalando a {@code dopo} ogni PDF generato. */
    public static MenuBar soloIcona(VaadinIcon icona, String etichetta, SerializableSupplier<Pdf> pdf,
                                    SerializableBiConsumer<Modalita, Pdf> dopo) {
        MenuBar menu = new MenuBar();
        voci(voceIcona(menu, icona, etichetta), pdf, dopo);
        return menu;
    }

    /**
     * Pulsante con la sola icona per chi non scarica i PDF (famiglie e insegnanti): "Anteprima in una nuova scheda" e
     * "Invia alla mia email", che esegue {@code invia} sul server.
     */
    public static MenuBar anteprimaEInvio(VaadinIcon icona, String etichetta, SerializableSupplier<Pdf> pdf,
                                          SerializableBiConsumer<Modalita, Pdf> dopo, SerializableRunnable invia) {
        MenuBar menu = new MenuBar();
        MenuItem voce = voceIcona(menu, icona, etichetta);
        voce.getSubMenu().addItem(anteprima(pdf, dopo));
        Span email = new Span(VaadinIcon.ENVELOPE.create(), new Span("Invia alla mia email"));
        email.addClassNames("flex items-center gap-2 w-full");
        voce.getSubMenu().addItem(email, e -> invia.run());
        return menu;
    }

    private static MenuItem voceIcona(MenuBar menu, VaadinIcon icona, String etichetta) {
        // Larghezza fissa (icona + freccia del sottomenu di Aura): il menu disegna il pulsante dopo che la colonna
        // "Azioni" ha calcolato la sua larghezza automatica, che altrimenti non lo conterebbe e lo taglierebbe
        menu.addClassNames("flex-none w-[4.5rem]");
        MenuItem voce = menu.addItem(icona.create());
        voce.setAriaLabel(etichetta);
        menu.setTooltipText(voce, etichetta);
        return voce;
    }

    private static void voci(MenuItem voce, SerializableSupplier<Pdf> pdf, SerializableBiConsumer<Modalita, Pdf> dopo) {
        Anchor scarica = new Anchor(handler(pdf, Modalita.SCARICO, dopo), "");
        scarica.add(VaadinIcon.DOWNLOAD.create(), new Span("Scarica PDF"));
        // Il link occupa tutta la voce del menu, testo normale (non sottolineato)
        scarica.addClassNames("flex items-center gap-2 w-full no-underline text-inherit");
        voce.getSubMenu().addItem(anteprima(pdf, dopo));
        voce.getSubMenu().addItem(scarica);
    }

    private static Anchor anteprima(SerializableSupplier<Pdf> pdf, SerializableBiConsumer<Modalita, Pdf> dopo) {
        Anchor anteprima = new Anchor(handler(pdf, Modalita.ANTEPRIMA, dopo).inline(), AttachmentType.INLINE, "");
        anteprima.setTarget("_blank");
        anteprima.add(VaadinIcon.EYE.create(), new Span("Anteprima in una nuova scheda"));
        anteprima.addClassNames("flex items-center gap-2 w-full no-underline text-inherit");
        return anteprima;
    }

    private static InputStreamDownloadHandler handler(SerializableSupplier<Pdf> pdf, Modalita modalita,
                                                      SerializableBiConsumer<Modalita, Pdf> dopo) {
        return DownloadHandler.fromInputStream(evento -> {
            try {
                Pdf documento = pdf.get();
                if (dopo != null) {
                    dopo.accept(modalita, documento);
                }
                return new DownloadResponse(new ByteArrayInputStream(documento.contenuto()), documento.nomeFile(),
                    "application/pdf", documento.contenuto().length);
            } catch (IllegalArgumentException | IllegalStateException ex) {
                return DownloadResponse.error(404);
            }
        });
    }
}
