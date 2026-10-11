package it.attocchi.views.admin;

import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import it.attocchi.entity.InvioEmail;
import it.attocchi.entity.TipoDocumento;
import it.attocchi.service.EmailService;
import it.attocchi.views.MainLayout;
import it.attocchi.views.SegmentedControl;
import jakarta.annotation.security.RolesAllowed;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Registro di tutti gli invii email (conferme di iscrizione e ricevute), con filtri per tipo e testo. */
@Route(value = "email-inviate", layout = MainLayout.class)
@PageTitle("Email inviate")
@Menu(title = "Email inviate", order = 10.8, icon = "vaadin:envelopes")
@RolesAllowed({"ADMIN", "SEGRETERIA"})
public class EmailInviateView extends VerticalLayout {

    private final GrigliaInvii grid = new GrigliaInvii(true);
    private final SegmentedControl<Optional<TipoDocumento>> filtroTipo;
    private final TextField cerca = new TextField();
    private final GridListDataView<InvioEmail> invii;

    public EmailInviateView(EmailService emailService) {
        setSizeFull();
        List<Optional<TipoDocumento>> tipi = new java.util.ArrayList<>();
        tipi.add(Optional.empty());
        Arrays.stream(TipoDocumento.values()).map(Optional::of).forEach(tipi::add);
        filtroTipo = new SegmentedControl<>("Filtra per tipo", tipi,
            t -> t.map(d -> d == TipoDocumento.RICEVUTA ? "Ricevute" : "Conferme").orElse("Tutte"),
            t -> t.map(TipoDocumento::getTitolo).orElse("Tutte le email"), null);
        filtroTipo.setValue(Optional.empty());
        filtroTipo.addValueChangeListener(e -> applicaFiltri());
        cerca.setPlaceholder("Cerca corso, allievo o destinatario");
        cerca.setAriaLabel("Cerca negli invii");
        cerca.setClearButtonVisible(true);
        cerca.setValueChangeMode(ValueChangeMode.LAZY);
        cerca.addValueChangeListener(e -> applicaFiltri());
        cerca.setWidth("18rem");
        cerca.setMaxWidth("100%");
        HorizontalLayout filtri = new HorizontalLayout(filtroTipo, cerca);
        filtri.setWrap(true);
        filtri.setAlignItems(Alignment.CENTER);

        invii = grid.setItems(emailService.tuttiGliInvii());
        add(filtri, grid);
        expand(grid);
    }

    private void applicaFiltri() {
        Optional<TipoDocumento> tipo = filtroTipo.getValue() != null ? filtroTipo.getValue() : Optional.empty();
        String testo = cerca.getValue().strip().toLowerCase(Locale.ITALIAN);
        invii.setFilter(i -> (tipo.isEmpty() || tipo.get() == i.getTipo())
            && (testo.isEmpty() || contiene(i.getCorsoNome(), testo) || contiene(i.getAllievoNome(), testo)
                || contiene(i.getDestinatario(), testo)));
    }

    private static boolean contiene(String valore, String testo) {
        return valore != null && valore.toLowerCase(Locale.ITALIAN).contains(testo);
    }
}
