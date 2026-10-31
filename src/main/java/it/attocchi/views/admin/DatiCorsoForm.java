package it.attocchi.views.admin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.function.SerializableConsumer;
import it.attocchi.entity.AnnoScolastico;
import it.attocchi.entity.Corso;
import it.attocchi.entity.Insegnante;
import it.attocchi.entity.Luogo;
import it.attocchi.entity.TipoCorso;
import it.attocchi.service.AnnoScolasticoService;
import it.attocchi.service.CorsoService;
import it.attocchi.service.InsegnanteService;
import it.attocchi.service.LuogoService;
import it.attocchi.views.Formati;
import it.attocchi.views.SegmentedControl;
import it.attocchi.views.Stili;
import it.attocchi.views.ViewSupport;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * Scheda "Dati del corso" della pagina del corso ({@link CorsoView}): i dati del corso e le opzioni aggiuntive, salvati
 * insieme con "Salva" ({@code CorsoService.salva(corso, opzioni)}). {@link #hasChanges()} dice se ci sono modifiche non
 * salvate, per chiedere conferma prima di uscire.
 */
class DatiCorsoForm extends Div {

    private final Binder<Corso> binder = new Binder<>(Corso.class);
    private final Corso corso;
    private final OpzioniCorsoFields opzioni;

    /**
     * @param puoEliminare {@code true} per l'admin, che può togliere anche le opzioni già salvate
     * @param salvato      chiamato con il corso salvato
     */
    DatiCorsoForm(Corso corso, CorsoService corsoService, InsegnanteService insegnanteService,
                  AnnoScolasticoService annoScolasticoService, LuogoService luogoService, boolean puoEliminare,
                  SerializableConsumer<Corso> salvato) {

        this.corso = corso;
        FormLayout formLayout = new FormLayout();

        SegmentedControl<TipoCorso> tipo = new SegmentedControl<>("Tipo", List.of(TipoCorso.values()),
            TipoCorso::getDescrizione, t -> t == TipoCorso.SERVIZIO
                ? "Servizio (es. Pulizia): come un corso, svolto da un incaricato" : "Corso", null);
        Span etichettaTipo = new Span("Tipo");
        etichettaTipo.addClassNames(Stili.TESTO_SECONDARIO);
        Div bloccoTipo = new Div(etichettaTipo, tipo);
        bloccoTipo.addClassNames("flex flex-col gap-1");
        TextField nome = new TextField("Nome");
        TextArea descrizione = new TextArea("Descrizione");
        DatePicker dataInizio = ViewSupport.datePicker("Data Inizio");
        IntegerField numeroLezioni = new IntegerField("Numero Lezioni");
        numeroLezioni.setHelperText("Per ogni allievo, nei giorni che frequenta; facoltativo con la data di fine");
        numeroLezioni.setClearButtonVisible(true);
        DatePicker dataFine = ViewSupport.datePicker("Data di fine");
        dataFine.setHelperText("Ultimo giorno di lezione; facoltativa con il numero di lezioni");
        dataFine.setClearButtonVisible(true);
        IntegerField durataLezioni = new IntegerField("Durata Lezione (minuti)");
        TimePicker orarioInizio = ViewSupport.timePicker("Orario Inizio");
        BigDecimalField costoTotale = new BigDecimalField("Costo Totale");
        costoTotale.setLocale(Locale.ITALY);
        costoTotale.setPrefixComponent(new Span("€"));
        BigDecimalField prezzoPreiscrizione = new BigDecimalField("Prezzo con pre-iscrizione");
        prezzoPreiscrizione.setLocale(Locale.ITALY);
        prezzoPreiscrizione.setPrefixComponent(new Span("€"));
        prezzoPreiscrizione.setClearButtonVisible(true);

        CheckboxGroup<DayOfWeek> giorniSettimana = new CheckboxGroup<>();
        giorniSettimana.setLabel("Giorni Settimana");
        giorniSettimana.setItems(Arrays.asList(DayOfWeek.values()));
        giorniSettimana.setItemLabelGenerator(day -> {
            switch (day) {
                case MONDAY: return "Lunedì";
                case TUESDAY: return "Martedì";
                case WEDNESDAY: return "Mercoledì";
                case THURSDAY: return "Giovedì";
                case FRIDAY: return "Venerdì";
                case SATURDAY: return "Sabato";
                case SUNDAY: return "Domenica";
                default: return day.name();
            }
        });

        SegmentedControl<Integer> cadenza = new SegmentedControl<>("Cadenza delle lezioni", List.of(1, 2),
            settimane -> settimane == 1 ? "Ogni settimana" : "Ogni 2 settimane",
            settimane -> settimane == 1 ? "Lezione ogni settimana" : "Lezione ogni 2 settimane, dalla data di inizio",
            null);
        Span aiutoCadenza = new Span("Ogni 2 settimane: le settimane alterne partono dalla data di inizio e una "
            + "festività sposta la lezione alla settimana dopo. Vale da \"Aggiorna lezioni\".");
        aiutoCadenza.addClassNames(Stili.TESTO_SECONDARIO);
        Div bloccoCadenza = new Div(cadenza, aiutoCadenza);
        bloccoCadenza.addClassNames("flex flex-col gap-1");

        ComboBox<String> annoScolastico = new ComboBox<>("Anno Scolastico");
        List<String> anni = annoScolasticoService.scelteCorso(corso.getAnnoScolastico());
        annoScolastico.setItems(anni);
        // Proposto dalla data di inizio finché non viene scelto, se è tra gli anni attivi
        dataInizio.addValueChangeListener(e -> {
            if (e.getValue() != null && annoScolastico.isEmpty() && anni.contains(AnnoScolastico.di(e.getValue()))) {
                annoScolastico.setValue(AnnoScolastico.di(e.getValue()));
            }
        });
        Checkbox pubblicato = new Checkbox("Pubblicato");
        pubblicato.setTooltipText("Visibile a genitori e allievi; finché non è pubblicato ci lavorano solo staff e insegnanti");
        Checkbox attivo = new Checkbox("Attivo");
        attivo.setTooltipText("I corsi non attivi non accettano nuovi partecipanti né lezioni e presenze");

        ComboBox<Insegnante> insegnante = new ComboBox<>("Insegnante");
        insegnante.setItems(insegnanteService.findAll());
        insegnante.setItemLabelGenerator(Insegnante::getNomeCompleto);
        MultiSelectComboBox<Insegnante> altriInsegnanti = new MultiSelectComboBox<>("Altri insegnanti");
        altriInsegnanti.setItems(insegnanteService.findAll());
        altriInsegnanti.setItemLabelGenerator(Insegnante::getNomeCompleto);
        altriInsegnanti.setClearButtonVisible(true);
        altriInsegnanti.setHelperText("Facoltativi, fino a " + Corso.MAX_FACOLTATIVI + ": stessi diritti sul corso");
        // Per i servizi chi lo svolge è un "Incaricato"
        tipo.addValueChangeListener(e -> {
            TipoCorso scelto = e.getValue() != null ? e.getValue() : TipoCorso.CORSO;
            insegnante.setLabel(scelto.getChiLoSvolge());
            altriInsegnanti.setLabel("Altri " + scelto.getChiLoSvolgonoPlurale().toLowerCase());
        });

        MultiSelectComboBox<Luogo> luoghiAbilitati = new MultiSelectComboBox<>("Luoghi abilitati");
        luoghiAbilitati.setItems(luogoService.scelteCorso(corso.getLuoghiAbilitati()));
        luoghiAbilitati.setItemLabelGenerator(Luogo::getNome);
        luoghiAbilitati.setHelperText("Proposti per primi sulle lezioni; si può scegliere comunque ogni luogo attivo");
        ComboBox<Luogo> luogo = new ComboBox<>("Luogo predefinito");
        luogo.setItems(luogoService.scelteCorso(corso.getLuogo() != null ? List.of(corso.getLuogo()) : List.of()));
        luogo.setItemLabelGenerator(Luogo::getNome);
        luogo.setClearButtonVisible(true);
        luogo.setHelperText("Facoltativo: dove si svolgono di solito le lezioni");
        // Il predefinito è anche abilitato
        luogo.addValueChangeListener(e -> {
            if (e.getValue() != null && !luoghiAbilitati.getValue().contains(e.getValue())) {
                luoghiAbilitati.select(e.getValue());
            }
        });

        binder.forField(luogo).bind(Corso::getLuogo, Corso::setLuogo);
        binder.forField(luoghiAbilitati).bind(Corso::getLuoghiAbilitati, Corso::setLuoghiAbilitati);
        binder.forField(tipo).asRequired("Scegli il tipo").bind(Corso::getTipo, Corso::setTipo);
        binder.forField(nome).asRequired("Inserisci il nome del corso").bind(Corso::getNome, Corso::setNome);
        binder.forField(descrizione).bind(Corso::getDescrizione, Corso::setDescrizione);
        binder.forField(dataInizio).asRequired("Inserisci la data di inizio del corso").bind(Corso::getDataInizio, Corso::setDataInizio);
        // Almeno uno tra numero di lezioni e data di fine: con entrambi vale il primo limite raggiunto
        var bindingNumero = binder.forField(numeroLezioni)
            .withValidator(n -> n != null || dataFine.getValue() != null, "Indica il numero di lezioni o la data di fine")
            .withValidator(n -> n == null || n >= 1, "Almeno 1 lezione")
            .bind(Corso::getNumeroLezioni, Corso::setNumeroLezioni);
        binder.forField(dataFine)
            .withValidator(d -> d == null || dataInizio.getValue() == null || !d.isBefore(dataInizio.getValue()),
                "Non può essere prima della data di inizio")
            .bind(Corso::getDataFine, Corso::setDataFine);
        dataFine.addValueChangeListener(e -> bindingNumero.validate());
        binder.forField(durataLezioni).asRequired("Inserisci la durata della lezione")
            .bind(Corso::getDurataLezioneMinuti, Corso::setDurataLezioneMinuti);
        binder.forField(orarioInizio).bind(Corso::getOrarioInizio, Corso::setOrarioInizio);
        binder.forField(costoTotale)
            .withValidator(costo -> costo == null || costo.signum() >= 0, "Il costo non può essere negativo")
            .bind(Corso::getCostoTotale, Corso::setCostoTotale);
        var bindingPreiscrizione = binder.forField(prezzoPreiscrizione)
            .withValidator(prezzo -> prezzo == null || prezzo.signum() >= 0, "Il prezzo non può essere negativo")
            .withValidator(prezzo -> prezzo == null || (costoTotale.getValue() != null && prezzo.compareTo(costoTotale.getValue()) <= 0),
                "Non può superare il costo totale")
            .bind(Corso::getPrezzoPreiscrizione, Corso::setPrezzoPreiscrizione);
        // Aiuto sul valore tipico (sconto 15%), assegnato comunque a mano
        costoTotale.addValueChangeListener(e -> {
            BigDecimal tipico = Corso.prezzoPreiscrizioneTipico(e.getValue());
            prezzoPreiscrizione.setHelperText(tipico != null ? "Di solito " + Formati.euro(tipico) + " (sconto 15%)" : null);
            if (!prezzoPreiscrizione.isEmpty()) {
                bindingPreiscrizione.validate();
            }
        });
        binder.forField(giorniSettimana).bind(Corso::getGiorniSettimana, Corso::setGiorniSettimana);
        binder.forField(cadenza).asRequired("Scegli la cadenza").bind(Corso::getCadenzaSettimane, Corso::setCadenzaSettimane);
        binder.forField(insegnante).asRequired("Scegli l'insegnante del corso").bind(Corso::getInsegnante, Corso::setInsegnante);
        // Nell'ordine di scelta, fino a MAX_FACOLTATIVI e senza il titolare
        binder.forField(altriInsegnanti)
            .withValidator(altri -> altri.size() <= Corso.MAX_FACOLTATIVI, "Al massimo " + Corso.MAX_FACOLTATIVI + " insegnanti")
            .withValidator(altri -> !altri.contains(insegnante.getValue()), "Il titolare non va ripetuto qui")
            .bind(c -> new LinkedHashSet<>(c.getInsegnantiFacoltativi()),
                (c, altri) -> c.setInsegnantiFacoltativi(new ArrayList<>(altri)));
        binder.forField(annoScolastico).asRequired("Scegli l'anno scolastico").bind(Corso::getAnnoScolastico, Corso::setAnnoScolastico);
        binder.forField(attivo).bind(Corso::isAttivo, Corso::setAttivo);
        binder.forField(pubblicato).bind(Corso::isPubblicato, Corso::setPubblicato);

        binder.readBean(corso);
        opzioni = new OpzioniCorsoFields(corso.getId() != null ? corsoService.opzioni(corso.getId()) : List.of(), puoEliminare);

        formLayout.add(bloccoTipo, nome, insegnante, altriInsegnanti, dataInizio, annoScolastico, numeroLezioni, dataFine, durataLezioni, orarioInizio, costoTotale, prezzoPreiscrizione, attivo, pubblicato, giorniSettimana, bloccoCadenza, luogo, luoghiAbilitati, descrizione);

        Button saveButton = new Button("Salva", e -> salva(corsoService, salvato));
        saveButton.addThemeVariants(ButtonVariant.PRIMARY);
        saveButton.addClassNames("self-start");

        formLayout.setColspan(bloccoTipo, 2);
        formLayout.setColspan(giorniSettimana, 2);
        formLayout.setColspan(bloccoCadenza, 2);
        formLayout.setColspan(descrizione, 2);

        add(formLayout, opzioni, saveButton);
        addClassNames("flex flex-col gap-4 w-full max-w-4xl");
    }

    /** Modifiche non ancora salvate, nei dati o nelle opzioni. */
    boolean hasChanges() {
        return binder.hasChanges() || opzioni.hasChanges();
    }

    private void salva(CorsoService corsoService, SerializableConsumer<Corso> salvato) {
        // Campi obbligatori mancanti: gli errori sono mostrati sui campi
        boolean opzioniValide = opzioni.valida();
        if (!binder.writeBeanIfValid(corso) || !opzioniValide) {
            return;
        }
        try {
            salvato.accept(corsoService.salva(corso, opzioni.getOpzioni()));
        } catch (RuntimeException ex) {
            Notification.show("Errore nel salvataggio: " + ex.getMessage());
        }
    }
}
