package it.attocchi.views.admin;

import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableFunction;
import it.attocchi.entity.AperturaDocumento;
import it.attocchi.views.CardMobile;
import it.attocchi.views.Formati;
import it.attocchi.views.ViewSupport;

/**
 * Griglia della cronologia dei documenti aperti da genitori e allievi: data e ora, chi, documento, allievo, corso,
 * anno e modalità (anteprima o scarico). Con {@code elimina} (solo admin) un pulsante Elimina su ogni riga.
 */
public class GrigliaAperture extends Grid<AperturaDocumento> {

    public GrigliaAperture(SerializableConsumer<AperturaDocumento> elimina) {
        super(AperturaDocumento.class, false);
        addColumn(a -> Formati.dataOra(a.getDataOra())).setHeader("Data e ora")
            .setComparator(AperturaDocumento::getDataOra).setWidth("12rem").setFlexGrow(0);
        addColumn(GrigliaAperture::chi).setHeader("Aperto da").setSortable(true);
        addColumn(a -> a.getTipo().getTitolo()).setHeader("Documento").setWidth("11rem").setFlexGrow(0);
        addColumn(AperturaDocumento::getAllievoNome).setHeader("Allievo").setSortable(true);
        addColumn(AperturaDocumento::getCorsoNome).setHeader("Corso").setSortable(true);
        addColumn(AperturaDocumento::getAnno).setHeader("Anno").setAutoWidth(true).setFlexGrow(0);
        addComponentColumn(GrigliaAperture::modalita).setHeader("Modalità").setWidth("9rem").setFlexGrow(0);
        SerializableFunction<AperturaDocumento, HorizontalLayout> azioni = a -> elimina == null ? null
            : ViewSupport.actions(eliminaButton(a, elimina));
        var colonnaAzioni = addComponentColumn(a -> azioni.apply(a));
        ViewSupport.configureActionsColumn(colonnaAzioni);
        colonnaAzioni.setVisible(elimina != null);
        CardMobile.per(this)
            .titolo(a -> a.getTipo().getTitolo() + " – " + a.getAllievoNome())
            .sottotitolo(a -> Formati.dataOra(a.getDataOra()))
            .stato(GrigliaAperture::modalita)
            .campo("Aperto da", GrigliaAperture::chi)
            .campo("Corso", AperturaDocumento::getCorsoNome)
            .campo("Anno", AperturaDocumento::getAnno)
            .azioni(azioni)
            .applica();
        setEmptyStateText("Nessun documento aperto da genitori o allievi");
    }

    private static Button eliminaButton(AperturaDocumento apertura, SerializableConsumer<AperturaDocumento> elimina) {
        Button button = ViewSupport.iconButton(VaadinIcon.TRASH, "Elimina", e -> elimina.accept(apertura));
        button.addThemeVariants(ButtonVariant.ERROR);
        return button;
    }

    /** Nome e username di chi ha aperto il documento. */
    static String chi(AperturaDocumento apertura) {
        return apertura.getUtenteNome() != null ? apertura.getUtenteNome() + " (" + apertura.getUsername() + ")"
            : apertura.getUsername();
    }

    private static Badge modalita(AperturaDocumento apertura) {
        Badge badge = apertura.getModalita() == AperturaDocumento.Modalita.ANTEPRIMA
            ? new Badge(apertura.getModalita().getDescrizione(), VaadinIcon.EYE.create())
            : new Badge(apertura.getModalita().getDescrizione(), VaadinIcon.DOWNLOAD.create());
        badge.addThemeVariants(BadgeVariant.CONTRAST);
        return badge;
    }
}
