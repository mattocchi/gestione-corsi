package it.attocchi;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Inline;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.server.PWA;
import it.attocchi.views.MainLayout;
import it.attocchi.views.PaginaPubblica;
import com.vaadin.flow.theme.aura.Aura;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * The entry point of the Spring Boot application.
 * <p>
 * The theme stylesheet must be loaded before the application's own styles.
 * <p>
 * {@code @PWA} makes the app installable on the phone's home screen (icon from {@code icons/icon.png},
 * short name of at most 12 characters under the icon). The app doesn't work offline: without a connection
 * the service worker shows {@code offline.html}.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@StyleSheet(Aura.STYLESHEET)
@StyleSheet("styles.css")
@PWA(name = MainLayout.NOME_APP, shortName = MainLayout.NOME_BREVE, description = PaginaPubblica.SOTTOTITOLO,
    themeColor = "#ffffff", backgroundColor = "#ffffff",
    offlinePath = "offline.html", offlineResources = {"images/logo-mosson.png"})
public class Application implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    /** The home-screen shortcut script must be in the head: the browser's install prompt can come before the views. */
    @Override
    public void configurePage(AppShellSettings settings) {
        settings.addInlineFromFile("scorciatoia-home.js", Inline.Wrapping.JAVASCRIPT);
    }

}
