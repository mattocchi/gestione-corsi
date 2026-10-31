package it.attocchi.views.admin;

import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import it.attocchi.entity.Festivita;
import it.attocchi.entity.Festivita.Tipo;
import it.attocchi.security.SecurityService;
import it.attocchi.service.FestivitaService;
import it.attocchi.views.CardMobile;
import it.attocchi.views.Formati;
import it.attocchi.views.SegmentedControl;
import it.attocchi.views.Stili;
import it.attocchi.views.ViewSupport;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Festività saltate da "Genera Lezioni": admin e segreteria le aggiungono, modificano e attivano/disattivano;
 * solo l'admin elimina. In elenco dalla più vicina.
 */
public class FestivitaScheda extends VerticalLayout {

    private final FestivitaService festivitaService;
    private final boolean puoEliminare;
    private final Grid<Festivita> grid = new Grid<>(Festivita.class, false);

    public FestivitaScheda(FestivitaService festivitaService, SecurityService securityService) {
        this.festivitaService = festivitaService;
        this.puoEliminare = securityService.hasRole("ADMIN");
        addClassNames("size-full");

        grid.addColumn(Festivita::getNome).setHeader("Nome").setSortable(true);
        grid.addColumn(FestivitaScheda::quando).setHeader("Quando");
        grid.addColumn(FestivitaScheda::prossima).setHeader("Prossima")
            .setComparator(Comparator.comparing(FestivitaScheda::prossimaData, Comparator.nullsLast(Comparator.naturalOrder())))
            .setWidth("11rem").setFlexGrow(0);
        grid.addComponentColumn(FestivitaScheda::createStatoBadge).setHeader("Stato").setWidth("8.5rem").setFlexGrow(0);
        ViewSupport.configureActionsColumn(grid.addComponentColumn(this::createAzioni));
        CardMobile.per(grid)
            .titolo(Festivita::getNome)
            .stato(FestivitaScheda::createStatoBadge)
            .campo("Quando", FestivitaScheda::quando)
            .campo("Prossima", FestivitaScheda::prossima)
            .azioni(this::createAzioni)
            .applica();

        Span nota = new Span("Le festività attive vengono saltate da \"Genera Lezioni\" in tutti i corsi: "
            + "per spostare lezioni già generate rigenera le lezioni del corso.");
        nota.addClassNames(Stili.TESTO_SECONDARIO);
        Button addButton = new Button("Nuova Festività", e -> openDialog(new Festivita()));
        add(addButton, nota, grid);
        grid.addClassNames("grow");
        updateGrid();
    }

    /** Giorno o periodo leggibile, es. "25 dicembre", "23 dicembre – 6 gennaio", "Lunedì dopo Pasqua". */
    static String quando(Festivita festivita) {
        return switch (festivita.getTipo()) {
            case OGNI_ANNO -> {
                String inizio = giornoMese(festivita.getGiornoInizio(), festivita.getMeseInizio());
                yield festivita.getMeseFine() != null
                    ? inizio + " – " + giornoMese(festivita.getGiornoFine(), festivita.getMeseFine()) + ", ogni anno"
                    : inizio + ", ogni anno";
            }
            case DATE -> festivita.getDataFine() != null
                ? Formati.data(festivita.getDataInizio()) + " – " + Formati.data(festivita.getDataFine())
                : Formati.data(festivita.getDataInizio());
            case PASQUA -> "Domenica di Pasqua, calcolata ogni anno";
            case PASQUETTA -> "Lunedì dopo Pasqua, calcolata ogni anno";
        };
    }

    private static String giornoMese(int giorno, int mese) {
        return giorno + " " + nomeMese(mese);
    }

    private static String nomeMese(int mese) {
        return Month.of(mese).getDisplayName(TextStyle.FULL, ViewSupport.LOCALE);
    }

    private static LocalDate prossimaData(Festivita festivita) {
        return festivita.prossima(LocalDate.now());
    }

    private static String prossima(Festivita festivita) {
        LocalDate data = prossimaData(festivita);
        return data != null ? Formati.data(data) : "Passata";
    }

    private static Badge createStatoBadge(Festivita festivita) {
        Badge badge = festivita.isAttivo()
            ? new Badge("Attiva", VaadinIcon.CHECK.create())
            : new Badge("Non attiva", VaadinIcon.BAN.create());
        if (festivita.isAttivo()) {
            badge.addThemeVariants(BadgeVariant.SUCCESS);
        }
        return badge;
    }

    private HorizontalLayout createAzioni(Festivita festivita) {
        Button modifica = ViewSupport.iconButton(VaadinIcon.PENCIL, "Modifica", e -> openDialog(festivita));
        Button attiva = festivita.isAttivo()
            ? ViewSupport.iconButton(VaadinIcon.EYE_SLASH, "Disattiva", e -> impostaAttiva(festivita, false))
            : ViewSupport.iconButton(VaadinIcon.EYE, "Attiva", e -> impostaAttiva(festivita, true));
        HorizontalLayout actions = ViewSupport.actions(modifica, attiva);
        if (puoEliminare) {
            Button elimina = ViewSupport.iconButton(VaadinIcon.TRASH, "Elimina", e -> elimina(festivita));
            elimina.addThemeVariants(ButtonVariant.ERROR);
            actions.add(elimina);
        }
        return actions;
    }

    private void openDialog(Festivita festivita) {
        Dialog dialog = ViewSupport.dialog(festivita.getId() == null ? "Nuova Festività" : "Modifica Festività", "520px");

        TextField nome = new TextField("Nome");
        SegmentedControl<Tipo> tipo = new SegmentedControl<>("Tipo di festività", List.of(Tipo.values()),
            Tipo::getEtichetta, Tipo::getEtichetta, null);

        IntegerField giornoInizio = giorno("Giorno");
        ComboBox<Integer> meseInizio = mese("Mese");
        IntegerField giornoFine = giorno("Fino al giorno");
        ComboBox<Integer> meseFine = mese("Fino al mese");
        giornoFine.setHelperText("Facoltativo, per un periodo");
        DatePicker dataInizio = ViewSupport.datePicker("Dal");
        DatePicker dataFine = ViewSupport.datePicker("Al");
        dataFine.setHelperText("Facoltativo, per un periodo");
        dataFine.setClearButtonVisible(true);
        Span calcolata = new Span("La data viene calcolata ogni anno.");
        calcolata.addClassNames(Stili.TESTO_SECONDARIO);

        // Inizio e fine sempre affiancati (campi corti), anche nel dialog stretto e su mobile
        FormLayout ogniAnno = new FormLayout(giornoInizio, meseInizio, giornoFine, meseFine);
        ogniAnno.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        FormLayout date = new FormLayout(dataInizio, dataFine);
        date.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        tipo.addValueChangeListener(e -> {
            ogniAnno.setVisible(e.getValue() == Tipo.OGNI_ANNO);
            date.setVisible(e.getValue() == Tipo.DATE);
            calcolata.setVisible(e.getValue() == Tipo.PASQUA || e.getValue() == Tipo.PASQUETTA);
        });

        Binder<Festivita> binder = new Binder<>(Festivita.class);
        binder.forField(nome).asRequired("Inserisci il nome della festività").bind(Festivita::getNome, Festivita::setNome);
        binder.forField(tipo).asRequired("Scegli il tipo").bind(Festivita::getTipo, Festivita::setTipo);
        binder.forField(giornoInizio)
            .withValidator(g -> tipo.getValue() != Tipo.OGNI_ANNO || g != null, "Inserisci il giorno")
            .bind(Festivita::getGiornoInizio, Festivita::setGiornoInizio);
        binder.forField(meseInizio)
            .withValidator(m -> tipo.getValue() != Tipo.OGNI_ANNO || m != null, "Scegli il mese")
            .bind(Festivita::getMeseInizio, Festivita::setMeseInizio);
        binder.forField(giornoFine)
            .withValidator(g -> tipo.getValue() != Tipo.OGNI_ANNO || (g == null) == meseFine.isEmpty(),
                "Inserisci giorno e mese di fine, oppure nessuno dei due")
            .bind(Festivita::getGiornoFine, Festivita::setGiornoFine);
        binder.forField(meseFine).bind(Festivita::getMeseFine, Festivita::setMeseFine);
        binder.forField(dataInizio)
            .withValidator(d -> tipo.getValue() != Tipo.DATE || d != null, "Inserisci la data")
            .bind(Festivita::getDataInizio, Festivita::setDataInizio);
        binder.forField(dataFine)
            .withValidator(d -> tipo.getValue() != Tipo.DATE || d == null || dataInizio.isEmpty()
                || !d.isBefore(dataInizio.getValue()), "La data di fine non può precedere quella di inizio")
            .bind(Festivita::getDataFine, Festivita::setDataFine);
        binder.readBean(festivita);
        if (tipo.isEmpty()) {
            tipo.setValue(Tipo.OGNI_ANNO);
        }

        Button salva = new Button("Salva", e -> {
            if (!binder.writeBeanIfValid(festivita)) {
                return;
            }
            try {
                festivitaService.salva(festivita);
            } catch (IllegalArgumentException ex) {
                // Es. 31 aprile: errore sul primo campo del tipo scelto
                if (festivita.getTipo() == Tipo.OGNI_ANNO) {
                    giornoInizio.setErrorMessage(ex.getMessage());
                    giornoInizio.setInvalid(true);
                } else {
                    Notification.show(ex.getMessage());
                }
                return;
            }
            updateGrid();
            dialog.close();
            Notification.show("Festività salvata");
            AggiornaLezioniDialog.proponiPerFestivita();
        });
        salva.addThemeVariants(ButtonVariant.PRIMARY);

        VerticalLayout content = new VerticalLayout(nome, tipo, ogniAnno, date, calcolata);
        content.addClassNames("p-0");
        nome.addClassNames("w-full");
        dialog.add(content);
        dialog.getFooter().add(ViewSupport.closeButton(dialog, "Annulla"), salva);
        dialog.open();
    }

    private static IntegerField giorno(String etichetta) {
        IntegerField giorno = new IntegerField(etichetta);
        giorno.setMin(1);
        giorno.setMax(31);
        giorno.setStepButtonsVisible(true);
        return giorno;
    }

    private static ComboBox<Integer> mese(String etichetta) {
        ComboBox<Integer> mese = new ComboBox<>(etichetta);
        mese.setItems(IntStream.rangeClosed(1, 12).boxed().toList());
        mese.setItemLabelGenerator(FestivitaScheda::nomeMese);
        mese.setClearButtonVisible(true);
        return mese;
    }

    private void impostaAttiva(Festivita festivita, boolean attiva) {
        festivitaService.impostaAttiva(festivita.getId(), attiva);
        updateGrid();
        AggiornaLezioniDialog.proponiPerFestivita();
    }

    private void elimina(Festivita festivita) {
        Dialog conferma = ViewSupport.dialog("Eliminare la festività?", "420px");
        conferma.add(new Span("\"" + festivita.getNome() + "\" non verrà più saltata dalle lezioni dei corsi."));
        Button elimina = new Button("Elimina", e -> {
            festivitaService.elimina(festivita.getId());
            updateGrid();
            conferma.close();
            Notification.show("Festività eliminata");
            AggiornaLezioniDialog.proponiPerFestivita();
        });
        elimina.addThemeVariants(ButtonVariant.PRIMARY, ButtonVariant.ERROR);
        conferma.getFooter().add(ViewSupport.closeButton(conferma, "Annulla"), elimina);
        conferma.open();
    }

    private void updateGrid() {
        List<Festivita> elenco = festivitaService.elenco().stream()
            .sorted(Comparator.comparing(FestivitaScheda::prossimaData, Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();
        grid.setItems(elenco);
    }
}
