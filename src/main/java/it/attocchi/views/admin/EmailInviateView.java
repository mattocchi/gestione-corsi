package it.attocchi.views.admin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import it.attocchi.entity.AperturaDocumento;
import it.attocchi.entity.InvioEmail;
import it.attocchi.entity.TipoDocumento;
import it.attocchi.security.SecurityService;
import it.attocchi.service.CronologiaDocumentiService;
import it.attocchi.service.EmailService;
import it.attocchi.views.Formati;
import it.attocchi.views.MainLayout;
import it.attocchi.views.ViewSupport;
import jakarta.annotation.security.RolesAllowed;

import java.util.Locale;
import java.util.Optional;

/**
 * Cronologia dei documenti: scheda "Email inviate" (registro di tutti gli invii) e scheda "Documenti aperti" (PDF
 * aperti da genitori e allievi in Documenti, {@link GrigliaAperture}; l'admin può eliminarne le righe), con filtri
 * per tipo e testo in comune; per l'admin anche l'email di test ({@link EmailDiTestDialog}).
 */
@Route(value = "email-inviate", layout = MainLayout.class)
@PageTitle("Cronologia")
@Menu(title = "Cronologia", order = 10.8, icon = "vaadin:envelopes")
@RolesAllowed({"ADMIN", "SEGRETERIA"})
public class EmailInviateView extends VerticalLayout {

    private final GrigliaInvii grid = new GrigliaInvii(true);
    private final Select<TipoDocumento> filtroTipo = new Select<>();
    private final TextField cerca = new TextField();
    private final GridListDataView<InvioEmail> invii;
    private final GrigliaAperture gridAperture;
    private final CronologiaDocumentiService cronologiaDocumentiService;
    private final String username;
    private GridListDataView<AperturaDocumento> aperture;

    public EmailInviateView(EmailService emailService, CronologiaDocumentiService cronologiaDocumentiService,
                            SecurityService securityService) {
        this.cronologiaDocumentiService = cronologiaDocumentiService;
        this.username = securityService.getUsername().orElse("");
        addClassNames("size-full");
        // Nessuna scelta = tutti i tipi
        filtroTipo.setItems(TipoDocumento.values());
        // Chiamato anche per la voce vuota (null)
        filtroTipo.setItemLabelGenerator(t -> t != null ? t.getPlurale() : "Tutti i documenti");
        filtroTipo.setEmptySelectionAllowed(true);
        filtroTipo.setEmptySelectionCaption("Tutti i documenti");
        filtroTipo.setAriaLabel("Filtra per tipo");
        filtroTipo.addClassNames("w-56");
        filtroTipo.addValueChangeListener(e -> applicaFiltri());
        cerca.setPlaceholder("Cerca corso, allievo, destinatario o utente");
        cerca.setAriaLabel("Cerca negli invii");
        cerca.setClearButtonVisible(true);
        cerca.setValueChangeMode(ValueChangeMode.LAZY);
        cerca.addValueChangeListener(e -> applicaFiltri());
        cerca.addClassNames("w-72 max-w-full");
        HorizontalLayout filtri = new HorizontalLayout(filtroTipo, cerca);
        filtri.addClassNames("flex-wrap items-center");
        if (securityService.hasRole("ADMIN")) {
            // Prova della configurazione SMTP e dei template: solo admin, non registrata
            Button emailDiTest = new Button("Email di test", VaadinIcon.ENVELOPE.create(),
                e -> new EmailDiTestDialog(emailService, securityService.getUsername().orElse("")).open());
            filtri.add(emailDiTest);
        }

        invii = grid.setItems(emailService.tuttiGliInvii());
        gridAperture = new GrigliaAperture(securityService.hasRole("ADMIN") ? this::elimina : null);
        aggiornaAperture();
        TabSheet schede = new TabSheet();
        schede.add("Email inviate", grid);
        schede.add("Documenti aperti", gridAperture);
        schede.addClassNames("grow w-full");
        grid.addClassNames("h-full");
        gridAperture.addClassNames("h-full");
        add(filtri, schede);
    }

    private void aggiornaAperture() {
        aperture = gridAperture.setItems(cronologiaDocumentiService.tutte());
        applicaFiltri();
    }

    private void elimina(AperturaDocumento apertura) {
        Dialog conferma = ViewSupport.dialog("Eliminare dalla cronologia?", "420px");
        conferma.add(new Span(apertura.getTipo().getTitolo() + " aperto da " + GrigliaAperture.chi(apertura) + " il "
            + Formati.dataOra(apertura.getDataOra()) + "."));
        Button elimina = new Button("Elimina", e -> {
            try {
                cronologiaDocumentiService.elimina(apertura.getId(), username);
                aggiornaAperture();
                conferma.close();
            } catch (RuntimeException ex) {
                Notification.show(ex.getMessage());
            }
        });
        elimina.addThemeVariants(ButtonVariant.PRIMARY, ButtonVariant.ERROR);
        conferma.getFooter().add(ViewSupport.closeButton(conferma, "Annulla"), elimina);
        conferma.open();
    }

    private void applicaFiltri() {
        Optional<TipoDocumento> tipo = Optional.ofNullable(filtroTipo.getValue());
        String testo = cerca.getValue().strip().toLowerCase(Locale.ITALIAN);
        invii.setFilter(i -> (tipo.isEmpty() || tipo.get() == i.getTipo())
            && (testo.isEmpty() || contiene(i.getCorsoNome(), testo) || contiene(i.getAllievoNome(), testo)
                || contiene(i.getDestinatario(), testo)));
        if (aperture != null) {
            aperture.setFilter(a -> (tipo.isEmpty() || tipo.get() == a.getTipo())
                && (testo.isEmpty() || contiene(a.getCorsoNome(), testo) || contiene(a.getAllievoNome(), testo)
                    || contiene(GrigliaAperture.chi(a), testo)));
        }
    }

    private static boolean contiene(String valore, String testo) {
        return valore != null && valore.toLowerCase(Locale.ITALIAN).contains(testo);
    }
}
