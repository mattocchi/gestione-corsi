package it.attocchi.views.admin;

import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.signals.Signal;
import it.attocchi.entity.InvioEmail;
import it.attocchi.views.Formati;
import it.attocchi.views.ViewSupport;

/**
 * Griglia del registro degli invii email: data e ora, tipo, (corso e allievo,) destinatario, utente ed esito.
 * Su mobile una colonna riassuntiva e l'esito solo icona.
 */
public class GrigliaInvii extends Grid<InvioEmail> {

    /** {@code conIscrizione}: mostra corso e allievo (registro generale); nel dialog del partecipante non servono. */
    public GrigliaInvii(boolean conIscrizione) {
        super(InvioEmail.class, false);
        Signal<Boolean> mobile = ViewSupport.isMobile();
        var riepilogo = addColumn(LitRenderer.<InvioEmail>of(
                "<div class=\"riepilogo-righe\"><div class=\"riepilogo-righe-titolo\">${item.titolo}</div>"
                    + "<div class=\"riepilogo-righe-dettaglio\">${item.dettaglio}</div></div>")
                .withProperty("titolo", i -> i.getTipo().getTitolo() + (conIscrizione ? " – " + i.getAllievoNome() : ""))
                .withProperty("dettaglio", i -> Formati.dataOra(i.getDataOra()) + " · " + i.getDestinatario()))
            .setHeader("Invio");
        var data = addColumn(i -> Formati.dataOra(i.getDataOra())).setHeader("Data e ora")
            .setComparator(InvioEmail::getDataOra).setWidth("12rem").setFlexGrow(0);
        var tipo = addColumn(i -> i.getTipo().getTitolo()).setHeader("Tipo").setWidth("11rem").setFlexGrow(0);
        var corso = addColumn(InvioEmail::getCorsoNome).setHeader("Corso").setSortable(true);
        var allievo = addColumn(InvioEmail::getAllievoNome).setHeader("Allievo").setSortable(true);
        var destinatario = addColumn(InvioEmail::getDestinatario).setHeader("Destinatario");
        var inviatoDa = addColumn(InvioEmail::getInviatoDa).setHeader("Inviata da").setWidth("8rem").setFlexGrow(0);
        var esito = addComponentColumn(i -> esito(i, mobile)).setHeader("Esito").setWidth("8rem").setFlexGrow(0);
        corso.setVisible(conIscrizione);
        allievo.setVisible(conIscrizione);
        inviatoDa.setVisible(conIscrizione);
        Signal.effect(this, () -> {
            boolean m = mobile.get();
            riepilogo.setVisible(m);
            data.setVisible(!m);
            tipo.setVisible(!m);
            corso.setVisible(conIscrizione && !m);
            allievo.setVisible(conIscrizione && !m);
            destinatario.setVisible(!m);
            inviatoDa.setVisible(conIscrizione && !m);
            esito.setHeader(m ? "" : "Esito").setWidth(m ? "3rem" : "8rem");
        });
        setEmptyStateText("Nessuna email inviata");
    }

    private static Badge esito(InvioEmail invio, Signal<Boolean> mobile) {
        Badge badge = switch (invio.getEsito()) {
            case INVIATA -> new Badge("Inviata", VaadinIcon.CHECK.create());
            case SIMULATA -> new Badge("Simulata", VaadinIcon.FLASK.create());
            case ERRORE -> new Badge("Errore", VaadinIcon.WARNING.create());
        };
        switch (invio.getEsito()) {
            case INVIATA -> badge.addThemeVariants(BadgeVariant.SUCCESS);
            case SIMULATA -> badge.addThemeVariants(BadgeVariant.CONTRAST);
            case ERRORE -> badge.addThemeVariants(BadgeVariant.ERROR);
        }
        if (invio.getErrore() != null) {
            Tooltip.forComponent(badge).setText(invio.getErrore());
        }
        badge.bindThemeVariant(BadgeVariant.ICON_ONLY, mobile);
        return badge;
    }
}
