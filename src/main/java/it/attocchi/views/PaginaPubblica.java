package it.attocchi.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Pagina accessibile senza login (recupero password): logo e titolo come nella pagina di accesso, sotto un riquadro
 * centrato con il titolo della pagina e il contenuto, largo al massimo come il form di accesso.
 */
public abstract class PaginaPubblica extends VerticalLayout {

    /** A chi serve l'applicazione, sotto il nome nelle pagine senza login. */
    public static final String SOTTOTITOLO = "Lezioni, presenze e pagamenti per insegnanti, segreteria, famiglie e allievi";

    protected final Div contenuto = new Div();
    private final H2 titolo = new H2();

    protected PaginaPubblica(String titoloPagina) {
        addClassNames("size-full items-center justify-center");

        titolo.setText(titoloPagina);
        titolo.addClassNames("text-(length:--aura-font-size-l) m-0");
        contenuto.addClassNames("flex flex-col gap-4 w-full");
        Div riquadro = new Div(titolo, contenuto);
        riquadro.addClassNames("flex flex-col gap-4 w-full max-w-sm p-4");
        add(intestazione());
        add(riquadro);
    }

    /** Logo, nome dell'applicazione e sottotitolo, comuni alla pagina di accesso e alle pagine senza login. */
    static Component[] intestazione() {
        Image logo = new Image(MainLayout.LOGO, MainLayout.LOGO_ALT);
        logo.addClassNames(Stili.LOGO, "w-[min(22rem,85vw)] max-w-full mb-3");
        H1 nome = new H1(MainLayout.NOME_APP);
        // Dimensione fluida tra smartphone e desktop
        nome.addClassNames("text-[length:clamp(1.5rem,1rem_+_2.5vw,2.25rem)] leading-[1.2] text-center m-0");
        Paragraph sottotitolo = new Paragraph(SOTTOTITOLO);
        sottotitolo.addClassNames(Stili.TESTO_SECONDARIO, "text-center max-w-sm m-0");
        return new Component[] {logo, nome, sottotitolo};
    }

    protected void setTitolo(String testo) {
        titolo.setText(testo);
    }
}
