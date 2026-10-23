package it.attocchi.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.OrderedList;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.dom.Element;

/**
 * Invito a creare la scorciatoia dell'app nella schermata Home del telefono ({@code @PWA} su {@code Application}).
 * Due forme: il {@link #banner()} (layout principale e pagina di accesso), che una volta chiuso non ricompare su quel
 * dispositivo, e la {@link #voceMenu()} del menu laterale, sempre disponibile. Restano nascosti finché il browser non
 * dice che è uno smartphone e che l'app non è già aperta dalla scorciatoia (script {@code scorciatoia-home.js}).
 * <p>
 * "Aggiungi" apre l'installazione del browser quando c'è (Android); altrimenti (iPhone, browser senza installazione)
 * mostra le istruzioni per farlo dal menu del browser.
 */
public class ScorciatoiaHome extends Div {

    private static final String IOS = "ios";

    private final boolean banner;
    private String piattaforma;

    private ScorciatoiaHome(boolean banner) {
        this.banner = banner;
        Button aggiungi;
        if (banner) {
            Icon icona = VaadinIcon.MOBILE.create();
            icona.addClassNames("flex-none");
            Span testo = new Span("Aggiungi " + MainLayout.NOME_APP + " alla schermata Home: la apri con un tocco, come un'app.");
            testo.addClassNames("grow min-w-0 text-(length:--aura-font-size-s)");
            aggiungi = new Button("Aggiungi");
            aggiungi.addThemeVariants(ButtonVariant.PRIMARY);
            aggiungi.addClassNames("flex-none");
            Button chiudi = ViewSupport.iconButton(VaadinIcon.CLOSE_SMALL, "Chiudi", e -> chiudi());
            chiudi.addClassNames("flex-none");
            add(icona, testo, aggiungi, chiudi);
            addClassNames("items-center gap-3 p-3 w-full box-border bg-(--aura-surface-color)",
                "border border-solid border-(--vaadin-border-color-secondary) rounded-(--vaadin-radius-m)");
        } else {
            aggiungi = new Button("Aggiungi alla Home", VaadinIcon.MOBILE.create());
            aggiungi.addThemeVariants(ButtonVariant.TERTIARY);
            add(aggiungi);
            addClassNames("px-2");
        }
        mostra(false);

        // Il clic è gestito nel browser (l'installazione va aperta nel gesto dell'utente); il server riceve solo l'esito
        Element pulsante = aggiungi.getElement();
        pulsante.addEventListener("scorciatoia-istruzioni", e -> istruzioni());
        pulsante.addEventListener("scorciatoia-installata", e -> mostra(false));
        addAttachListener(e -> {
            if (e.isInitialAttach()) {
                pulsante.executeJs("this.addEventListener('click', () => window.scorciatoiaHome && scorciatoiaHome.aggiungi(this))");
                verificaDispositivo();
            }
        });
    }

    /** Banner chiudibile, mostrato solo su smartphone finché l'utente non lo chiude. */
    public static ScorciatoiaHome banner() {
        return new ScorciatoiaHome(true);
    }

    /** Voce "Aggiungi alla Home" del menu laterale, mostrata solo su smartphone (anche dopo aver chiuso il banner). */
    public static ScorciatoiaHome voceMenu() {
        return new ScorciatoiaHome(false);
    }

    private void verificaDispositivo() {
        getElement().executeJs("return window.scorciatoiaHome ? [scorciatoiaHome.stato(), scorciatoiaHome.chiuso()].join(':') : 'computer:false'")
            .then(String.class, risposta -> {
                String[] parti = risposta.split(":");
                piattaforma = parti[0];
                boolean daMostrare = piattaforma.equals(IOS) || piattaforma.equals("android");
                mostra(daMostrare && !(banner && Boolean.parseBoolean(parti[1])));
            });
    }

    /** Alterna le classi di visualizzazione: {@code flex} e {@code hidden} insieme non si scavalcano in modo affidabile. */
    private void mostra(boolean visibile) {
        setClassName("flex", visibile && banner);
        setClassName("block", visibile && !banner);
        setClassName("hidden", !visibile);
    }

    private void chiudi() {
        getElement().executeJs("window.scorciatoiaHome && scorciatoiaHome.chiudi()");
        mostra(false);
    }

    private void istruzioni() {
        Dialog dialog = ViewSupport.dialog("Aggiungi alla schermata Home", "28rem");
        OrderedList passi = new OrderedList();
        passi.addClassNames("flex flex-col gap-2 m-0 ps-5");
        Paragraph nota;
        if (IOS.equals(piattaforma)) {
            passi.add(new ListItem("Tocca il pulsante Condividi del browser (il quadrato con la freccia verso l'alto): "
                    + "in Safari è in basso, su iPad e in Chrome è in alto a destra."),
                new ListItem("Scorri le opzioni e scegli \"Aggiungi alla schermata Home\"."),
                new ListItem("Tocca \"Aggiungi\" in alto a destra."));
            nota = new Paragraph("Se l'opzione non c'è, apri questa pagina con Safari e riprova.");
        } else {
            passi.add(new ListItem("Apri il menu del browser (i tre puntini in alto a destra)."),
                new ListItem("Scegli \"Aggiungi a schermata Home\" oppure \"Installa app\"."),
                new ListItem("Conferma con \"Aggiungi\" o \"Installa\"."));
            nota = new Paragraph("Se l'opzione non c'è, apri questa pagina con Chrome e riprova.");
        }
        nota.addClassNames(Stili.TESTO_SECONDARIO, "mt-4 mb-0");
        Paragraph fine = new Paragraph("L'icona \"" + MainLayout.NOME_BREVE + "\" comparirà tra le app del telefono.");
        fine.addClassNames("mt-4 mb-0");
        dialog.add(passi, fine, nota);
        dialog.getFooter().add(ViewSupport.closeButton(dialog, "Ho capito"));
        dialog.open();
    }
}
