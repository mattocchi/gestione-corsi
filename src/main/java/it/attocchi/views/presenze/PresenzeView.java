package it.attocchi.views.presenze;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import it.attocchi.entity.Allievo;
import it.attocchi.entity.Corso;
import it.attocchi.entity.Presenza;
import it.attocchi.service.CorsoService;
import it.attocchi.service.PresenzaService;
import it.attocchi.views.MainLayout;
import org.vaadin.lineawesome.LineAwesomeIcon;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@PageTitle("Gestione Presenze")
@Route(value = "presenze", layout = MainLayout.class)
public class PresenzeView extends Div {

    private final PresenzaService presenzaService;
    private final CorsoService corsoService;
    
    private ComboBox<Corso> corsoComboBox = new ComboBox<>("Corso");
    private DatePicker dataLezione = new DatePicker("Data Lezione");
    private Button caricaButton = new Button("Carica Presenze");
    private Button salvaButton = new Button("Salva Presenze");
    
    private Grid<AllievePresenzaRow> grid = new Grid<>(AllievePresenzaRow.class, false);
    private Map<Long, Presenza.StatoPresenza> statoPresenzeMap = new HashMap<>();
    
    private Corso corsoSelezionato;
    private LocalDate dataSelezionata;
    
    public PresenzeView(PresenzaService presenzaService, CorsoService corsoService) {
        this.presenzaService = presenzaService;
        this.corsoService = corsoService;
        
        addClassName("presenze-view");
        setSizeFull();
        
        configureComponents();
        
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.add(getToolbar(), grid);
        
        add(layout);
        
        salvaButton.setVisible(false);
        grid.setVisible(false);
    }
    
    private void configureComponents() {
        corsoComboBox.setItems(corsoService.findAll());
        corsoComboBox.setItemLabelGenerator(Corso::getNome);
        corsoComboBox.setWidth("300px");
        
        dataLezione.setLocale(new Locale("it", "IT"));
        dataLezione.setValue(LocalDate.now());
        dataLezione.setWidth("200px");
        
        caricaButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        caricaButton.setPrefixComponent(LineAwesomeIcon.CALENDAR_CHECK.create());
        caricaButton.addClickListener(e -> caricaPresenze());
        
        salvaButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        salvaButton.setPrefixComponent(LineAwesomeIcon.SAVE.create());
        salvaButton.addClickListener(e -> salvaPresenze());
        
        configureGrid();
    }
    
    private void configureGrid() {
        grid.addClassName("presenze-grid");
        grid.setSizeFull();
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_WRAP_CELL_CONTENT);

        grid.addColumn(row -> row.allievo.getCognome()).setHeader("Cognome").setSortable(true).setFlexGrow(1);
        grid.addColumn(row -> row.allievo.getNome()).setHeader("Nome").setSortable(true).setFlexGrow(1);

        grid.addComponentColumn(row -> {
            RadioButtonGroup<Presenza.StatoPresenza> radioGroup = new RadioButtonGroup<>();
            radioGroup.setItems(Presenza.StatoPresenza.values());
            radioGroup.setItemLabelGenerator(Presenza.StatoPresenza::getDescrizione);
            radioGroup.setValue(row.stato);

            radioGroup.addValueChangeListener(event -> {
                statoPresenzeMap.put(row.allievo.getId(), event.getValue());
            });

            return radioGroup;
        }).setHeader("Stato").setFlexGrow(2);
    }
    
    private Component getToolbar() {
        HorizontalLayout toolbar = new HorizontalLayout(
            corsoComboBox, 
            dataLezione, 
            caricaButton,
            salvaButton
        );
        toolbar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);
        toolbar.addClassName("toolbar");
        return toolbar;
    }
    
    private void caricaPresenze() {
        corsoSelezionato = corsoComboBox.getValue();
        dataSelezionata = dataLezione.getValue();
        
        if (corsoSelezionato == null) {
            Notification.show("Seleziona un corso")
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        
        if (dataSelezionata == null) {
            Notification.show("Seleziona una data")
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        
        statoPresenzeMap.clear();
        
        var rows = corsoSelezionato.getAllievi().stream()
            .map(allievo -> {
                var presenza = presenzaService.findByCorsoAllievoData(
                    corsoSelezionato.getId(), 
                    allievo.getId(), 
                    dataSelezionata
                );
                
                Presenza.StatoPresenza stato = presenza
                    .map(Presenza::getStato)
                    .orElse(Presenza.StatoPresenza.PRESENTE);
                
                statoPresenzeMap.put(allievo.getId(), stato);
                
                return new AllievePresenzaRow(allievo, stato);
            })
            .toList();
        
        grid.setItems(rows);
        grid.setVisible(true);
        salvaButton.setVisible(true);
        
        Notification.show("Presenze caricate per " + rows.size() + " allievi")
            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
    
    private void salvaPresenze() {
        if (corsoSelezionato == null || dataSelezionata == null) {
            return;
        }
        
        for (Allievo allievo : corsoSelezionato.getAllievi()) {
            Presenza.StatoPresenza stato = statoPresenzeMap.get(allievo.getId());
            if (stato != null) {
                presenzaService.creaOAggiorna(corsoSelezionato, allievo, dataSelezionata, stato);
            }
        }
        
        Notification.show("Presenze salvate con successo")
            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
    
    // Helper class per le righe della grid
    public static class AllievePresenzaRow {
        private final Allievo allievo;
        private Presenza.StatoPresenza stato;
        
        public AllievePresenzaRow(Allievo allievo, Presenza.StatoPresenza stato) {
            this.allievo = allievo;
            this.stato = stato;
        }
        
        public Allievo getAllievo() {
            return allievo;
        }
        
        public Presenza.StatoPresenza getStato() {
            return stato;
        }
        
        public void setStato(Presenza.StatoPresenza stato) {
            this.stato = stato;
        }
    }
}
