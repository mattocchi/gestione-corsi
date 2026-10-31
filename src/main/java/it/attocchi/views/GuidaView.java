package it.attocchi.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import it.attocchi.security.SecurityService;
import jakarta.annotation.security.PermitAll;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Guida d'uso per ruolo: per ogni voce del menu a cosa serve e le operazioni principali, con il collegamento alla
 * pagina. I testi sono in {@code src/main/resources/guida/*.md}; con più ruoli una scheda per ruolo.
 */
@Route(value = "guida", layout = MainLayout.class)
@PageTitle("Guida")
@Menu(title = "Guida", order = 95, icon = "vaadin:question-circle-o")
@PermitAll
public class GuidaView extends VerticalLayout {

    public GuidaView(SecurityService securityService) {
        addClassNames("max-w-4xl");
        Map<String, String> guide = new LinkedHashMap<>();
        if (securityService.hasRole("ADMIN") || securityService.hasRole("SEGRETERIA")) {
            guide.put("Staff", "staff");
        }
        if (securityService.hasRole("INSEGNANTE")) {
            guide.put("Insegnante", "insegnante");
        }
        if (securityService.hasRole("GENITORE")) {
            guide.put("Famiglia", "famiglia");
        }
        if (securityService.hasRole("ALLIEVO")) {
            guide.put("Allievo", "allievo");
        }
        if (guide.size() == 1) {
            add(markdown(guide.values().iterator().next()));
        } else {
            TabSheet schede = new TabSheet();
            schede.addClassNames("w-full");
            guide.forEach((titolo, file) -> schede.add(titolo, markdown(file)));
            add(schede);
        }
    }

    /** Testo della guida da {@code guida/<nome>.md}. */
    static String testo(String nome) {
        try (InputStream in = GuidaView.class.getResourceAsStream("/guida/" + nome + ".md")) {
            if (in == null) {
                throw new IllegalStateException("Guida non trovata: " + nome);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static Component markdown(String nome) {
        Markdown markdown = new Markdown(testo(nome));
        markdown.addClassNames("w-full");
        return markdown;
    }
}
