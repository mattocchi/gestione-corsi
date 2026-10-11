package it.attocchi.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.Table;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RouteParameters;
import it.attocchi.entity.Corso;
import it.attocchi.entity.Genitore;
import it.attocchi.entity.Iscrizione;
import it.attocchi.entity.Pagamento;
import it.attocchi.entity.Allievo;
import it.attocchi.security.SecurityService;
import it.attocchi.service.PagamentoService;
import it.attocchi.service.PagamentoService.Ricevuta;
import it.attocchi.service.PagamentoService.Situazione;
import jakarta.annotation.security.RolesAllowed;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

/**
 * Ricevuta stampabile dei pagamenti di un partecipante a un corso (anche non attivo), senza il layout
 * dell'applicazione. Admin e segreteria vedono tutte le ricevute, il genitore solo quelle dei propri figli.
 * Il pulsante "Stampa" usa la stampa del browser (anche per salvare in PDF) e non compare sul foglio.
 */
@Route("ricevuta/:corsoId([0-9]+)/:allievoId([0-9]+)")
@PageTitle("Ricevuta")
@RolesAllowed({"ADMIN", "SEGRETERIA", "GENITORE"})
public class RicevutaView extends Div implements BeforeEnterObserver {

    private final PagamentoService pagamentoService;
    private final SecurityService securityService;

    public RicevutaView(PagamentoService pagamentoService, SecurityService securityService) {
        this.pagamentoService = pagamentoService;
        this.securityService = securityService;
        addClassName("ricevuta");
    }

    /** Parametri della route della ricevuta di un allievo in un corso. */
    public static RouteParameters parametri(Long corsoId, Long allievoId) {
        return new RouteParameters(Map.of("corsoId", String.valueOf(corsoId), "allievoId", String.valueOf(allievoId)));
    }

    /** Link che apre la ricevuta in una nuova scheda, con il componente indicato (es. un pulsante) come contenuto. */
    public static Anchor link(Long corsoId, Long allievoId, Component contenuto) {
        String url = RouteConfiguration.forSessionScope().getUrl(RicevutaView.class, parametri(corsoId, allievoId));
        Anchor anchor = new Anchor(url, "");
        anchor.setTarget(AnchorTarget.BLANK);
        anchor.add(contenuto);
        return anchor;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Optional<Long> corsoId = event.getRouteParameters().getLong("corsoId");
        Optional<Long> allievoId = event.getRouteParameters().getLong("allievoId");
        Ricevuta ricevuta;
        try {
            ricevuta = pagamentoService.ricevuta(corsoId.orElseThrow(), allievoId.orElseThrow());
        } catch (RuntimeException ex) {
            event.rerouteToError(NotFoundException.class);
            return;
        }
        if (!puoVedere(ricevuta.iscrizione().getAllievo())) {
            // Stessa risposta di una ricevuta inesistente: non rivela le iscrizioni di altri
            event.rerouteToError(NotFoundException.class);
            return;
        }
        removeAll();
        mostra(ricevuta);
    }

    private boolean puoVedere(Allievo allievo) {
        if (securityService.hasRole("ADMIN") || securityService.hasRole("SEGRETERIA")) {
            return true;
        }
        Genitore genitore = allievo.getGenitore();
        return genitore != null && securityService.getUsername()
            .map(username -> username.equals(genitore.getUser().getUsername())).orElse(false);
    }

    private void mostra(Ricevuta ricevuta) {
        Iscrizione iscrizione = ricevuta.iscrizione();
        Corso corso = iscrizione.getCorso();
        Allievo allievo = iscrizione.getAllievo();
        Genitore genitore = allievo.getGenitore();
        Situazione situazione = ricevuta.situazione();

        Button stampa = new Button("Stampa", VaadinIcon.PRINT.create(),
            e -> getElement().executeJs("window.print()"));
        stampa.addThemeVariants(ButtonVariant.PRIMARY);
        Div comandi = new Div(stampa);
        comandi.addClassNames("ricevuta-comandi", "no-print");

        Image logo = new Image(MainLayout.LOGO, MainLayout.LOGO_ALT);
        logo.addClassName("ricevuta-logo");
        H1 titolo = new H1("Ricevuta di pagamento");
        Paragraph data = new Paragraph("Emessa il " + Formati.data(LocalDate.now()));
        data.addClassName("ricevuta-data");
        Div intestazione = new Div(logo, new Div(titolo, data));
        intestazione.addClassName("ricevuta-intestazione");

        Div allievoBlocco = blocco("Allievo",
            riga("Nome", allievo.getNomeCompleto()),
            riga("Codice fiscale", allievo.getCodiceFiscale()),
            riga("Indirizzo", Formati.indirizzo(allievo)));
        Div genitoreBlocco = blocco("Genitore",
            riga("Nome", genitore != null ? genitore.getNomeCompleto() : ""),
            riga("Codice fiscale", genitore != null ? genitore.getCodiceFiscale() : ""));
        String quota = Formati.euro(situazione.quota());
        if (iscrizione.getQuotaPersonalizzata() != null) {
            quota += " (quota piena " + Formati.euro(corso.getCostoTotale())
                + (iscrizione.getNotaQuota() != null ? ", " + iscrizione.getNotaQuota() : "") + ")";
        }
        Div corsoBlocco = blocco("Corso",
            riga("Corso", corso.getNome()),
            riga("Anno scolastico", corso.getAnnoScolastico()),
            riga("Insegnante", corso.getInsegnante() != null ? corso.getInsegnante().getNomeCompleto() : ""),
            riga("Quota", quota));

        Div totali = new Div(
            riga("Totale pagato", Formati.euro(situazione.pagato())),
            riga("Residuo da pagare", Formati.euro(situazione.residuo())));
        totali.addClassName("ricevuta-totali");

        add(comandi, intestazione, allievoBlocco, genitoreBlocco, corsoBlocco, new H2("Pagamenti"), pagamenti(ricevuta), totali);
    }

    private static Div blocco(String titolo, Div... righe) {
        Div blocco = new Div(new H2(titolo));
        blocco.add(righe);
        blocco.addClassName("ricevuta-blocco");
        return blocco;
    }

    private static Div riga(String etichetta, String valore) {
        Span label = new Span(etichetta);
        label.addClassName("ricevuta-etichetta");
        Div riga = new Div(label, new Span(valore != null ? valore : ""));
        riga.addClassName("ricevuta-riga");
        return riga;
    }

    private static Div pagamenti(Ricevuta ricevuta) {
        if (ricevuta.pagamenti().isEmpty()) {
            return new Div(new Paragraph("Nessun pagamento registrato."));
        }
        Table table = new Table();
        table.addClassName("ricevuta-tabella");
        table.addHeaderRow("Data", "Importo", "Metodo", "Nota");
        for (Pagamento pagamento : ricevuta.pagamenti()) {
            table.addRow(Formati.data(pagamento.getData()), Formati.euro(pagamento.getImporto()),
                pagamento.getMetodo().getDescrizione(), pagamento.getNota() != null ? pagamento.getNota() : "");
        }
        return new Div(table);
    }
}
