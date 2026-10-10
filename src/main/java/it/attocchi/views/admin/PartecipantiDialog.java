package it.attocchi.views.admin;

import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import it.attocchi.entity.Corso;
import it.attocchi.entity.Iscrizione;
import it.attocchi.service.CorsoService;
import it.attocchi.views.ViewSupport;

/**
 * Partecipanti di un corso in ordine di orario di inizio lezione; chi non ha ancora
 * un orario assegnato è in fondo.
 */
public class PartecipantiDialog extends Dialog {

    private final CorsoService corsoService;
    private final Corso corso;
    private final Grid<Iscrizione> grid = new Grid<>(Iscrizione.class, false);

    public PartecipantiDialog(CorsoService corsoService, Corso corso) {
        this.corsoService = corsoService;
        this.corso = corso;

        setHeaderTitle("Partecipanti – " + corso.getNome());
        setWidth("600px");
        setMaxWidth("100%");

        grid.addColumn(PartecipantiDialog::orario).setHeader("Orario").setWidth("7rem").setFlexGrow(0);
        grid.addColumn(iscrizione -> iscrizione.getStudente().getNomeCompleto()).setHeader("Partecipante");
        grid.setAllRowsVisible(true);
        aggiorna();

        add(grid);
        getFooter().add(ViewSupport.closeButton(this, "Chiudi"));
    }

    static String orario(Iscrizione iscrizione) {
        return iscrizione.getOrarioLezione() != null ? iscrizione.getOrarioLezione().toString() : "da assegnare";
    }

    private void aggiorna() {
        grid.setItems(corsoService.findPartecipanti(corso.getId()));
    }
}
