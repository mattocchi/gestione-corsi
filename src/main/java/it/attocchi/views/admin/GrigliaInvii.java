package it.attocchi.views.admin;

import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.shared.Tooltip;
import it.attocchi.entity.InvioEmail;
import it.attocchi.views.CardMobile;
import it.attocchi.views.Formati;

/**
 * Griglia del registro degli invii email: data e ora, tipo, (corso e allievo,) destinatario, utente ed esito.
 * Su mobile una colonna riassuntiva e l'esito solo icona.
 */
public class GrigliaInvii extends Grid<InvioEmail> {

    /** {@code conIscrizione}: mostra corso e allievo (registro generale); nel dialog del partecipante non servono. */
    public GrigliaInvii(boolean conIscrizione) {
        super(InvioEmail.class, false);
        addColumn(i -> Formati.dataOra(i.getDataOra())).setHeader("Data e ora")
            .setComparator(InvioEmail::getDataOra).setWidth("12rem").setFlexGrow(0);
        addColumn(i -> i.getTipo().getTitolo()).setHeader("Tipo").setWidth("11rem").setFlexGrow(0);
        var corso = addColumn(InvioEmail::getCorsoNome).setHeader("Corso").setSortable(true);
        var allievo = addColumn(InvioEmail::getAllievoNome).setHeader("Allievo").setSortable(true);
        addColumn(InvioEmail::getDestinatario).setHeader("Destinatario");
        var inviatoDa = addColumn(InvioEmail::getInviatoDa).setHeader("Inviata da").setWidth("8rem").setFlexGrow(0);
        addComponentColumn(GrigliaInvii::esito).setHeader("Esito").setWidth("8rem").setFlexGrow(0);
        corso.setVisible(conIscrizione);
        allievo.setVisible(conIscrizione);
        inviatoDa.setVisible(conIscrizione);
        CardMobile.per(this)
            .titolo(i -> i.getTipo().getTitolo() + (conIscrizione ? " – " + i.getAllievoNome() : ""))
            .sottotitolo(i -> Formati.dataOra(i.getDataOra()))
            .stato(GrigliaInvii::esito)
            .campo("Corso", i -> conIscrizione ? i.getCorsoNome() : null)
            .campo("Destinatario", InvioEmail::getDestinatario)
            .campo("Inviata da", i -> conIscrizione ? i.getInviatoDa() : null)
            // Il tooltip non si apre al tocco: su mobile l'errore è un campo della card
            .campo("Errore", InvioEmail::getErrore)
            .applica();
        setEmptyStateText("Nessuna email inviata");
    }

    private static Badge esito(InvioEmail invio) {
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
        return badge;
    }
}
