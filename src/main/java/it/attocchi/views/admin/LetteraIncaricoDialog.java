package it.attocchi.views.admin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.server.VaadinService;
import it.attocchi.entity.Corso;
import it.attocchi.entity.Insegnante;
import it.attocchi.entity.LetteraIncarico;
import it.attocchi.service.CorsoService;
import it.attocchi.service.LetteraIncaricoService;
import it.attocchi.views.Formati;
import it.attocchi.views.ViewSupport;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Emissione o modifica di una lettera di incarico (admin e segreteria, da Documenti): scelti corso e insegnante si
 * propongono anno, tariffa oraria, una tantum (rimborso forfettario annuo) e giorni e orari, tutti modificabili.
 */
public final class LetteraIncaricoDialog {

    private LetteraIncaricoDialog() {
    }

    public static void apri(LetteraIncarico lettera, String username, Runnable dopoSalvataggio) {
        LetteraIncaricoService service = servizio(LetteraIncaricoService.class);
        boolean nuova = lettera.getId() == null;
        Dialog dialog = ViewSupport.dialog(nuova ? "Nuova lettera di incarico" : "Modifica lettera di incarico", "640px");

        ComboBox<Corso> corso = new ComboBox<>("Corso o servizio");
        List<Corso> corsi = servizio(CorsoService.class).findAll().stream()
            .sorted(Comparator.comparing(Corso::isAttivo).reversed()
                .thenComparing(Corso::getAnnoScolastico, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Corso::getNome, String.CASE_INSENSITIVE_ORDER))
            .toList();
        corso.setItems(corsi);
        corso.setItemLabelGenerator(c -> c.getNome() + (c.getAnnoScolastico() != null ? " (" + c.getAnnoScolastico() + ")" : ""));
        ComboBox<Insegnante> insegnante = new ComboBox<>("Insegnante");
        insegnante.setItemLabelGenerator(Insegnante::getNomeCompleto);
        Span anno = new Span();
        DatePicker emessa = ViewSupport.datePicker("Data di emissione");
        BigDecimalField tariffa = importo("Tariffa oraria (€/ora)");
        BigDecimalField unaTantum = importo("Una tantum (€ per l'anno)");
        TextField giorni = new TextField("Giorni e orari");
        TextArea nota = new TextArea("Nota");
        nota.setMaxLength(LetteraIncarico.LUNGHEZZA_NOTA);
        nota.setHelperText("Facoltativa: condizioni particolari, riportate nella lettera");

        Binder<LetteraIncarico> binder = new Binder<>(LetteraIncarico.class);
        binder.forField(corso).asRequired("Scegli il corso").bind(LetteraIncarico::getCorso, LetteraIncarico::setCorso);
        binder.forField(insegnante).asRequired("Scegli l'insegnante")
            .bind(LetteraIncarico::getInsegnante, LetteraIncarico::setInsegnante);
        binder.forField(emessa).asRequired("Inserisci la data di emissione")
            .bind(LetteraIncarico::getDataEmissione, LetteraIncarico::setDataEmissione);
        binder.forField(tariffa)
            .withValidator(v -> v == null || v.signum() >= 0, "Non può essere negativa")
            .withValidator(v -> positivo(v) || positivo(unaTantum.getValue()), "Indica la tariffa oraria o l'una tantum")
            .bind(LetteraIncarico::getTariffaOraria, LetteraIncarico::setTariffaOraria);
        binder.forField(unaTantum)
            .withValidator(v -> v == null || v.signum() >= 0, "Non può essere negativo")
            .bind(LetteraIncarico::getImportoUnaTantum, LetteraIncarico::setImportoUnaTantum);
        binder.forField(giorni).bind(LetteraIncarico::getGiorniEOrari, LetteraIncarico::setGiorniEOrari);
        binder.forField(nota).bind(LetteraIncarico::getNota, LetteraIncarico::setNota);

        // Gli insegnanti del corso scelto; su una lettera nuova, scelti corso e insegnante si propongono i dati attuali
        corso.addValueChangeListener(e -> {
            Corso scelto = e.getValue();
            insegnante.setItems(scelto != null ? scelto.getInsegnanti() : List.of());
            insegnante.setLabel(scelto != null ? scelto.getTipo().getChiLoSvolge() : "Insegnante");
            if (scelto != null && scelto.getInsegnanti().size() == 1) {
                insegnante.setValue(scelto.getInsegnanti().getFirst());
            }
            anno.setText(scelto != null && scelto.getAnnoScolastico() != null
                ? "Valida per l'anno scolastico " + scelto.getAnnoScolastico() : "");
        });
        insegnante.addValueChangeListener(e -> {
            if (nuova && e.getValue() != null && corso.getValue() != null) {
                LetteraIncarico proposta = service.proposta(e.getValue().getId(), corso.getValue().getId());
                tariffa.setValue(proposta.getTariffaOraria());
                unaTantum.setValue(proposta.getImportoUnaTantum());
                giorni.setValue(Objects.requireNonNullElse(proposta.getGiorniEOrari(), ""));
            }
        });
        binder.readBean(lettera);
        if (lettera.getCorso() != null) {
            insegnante.setItems(lettera.getCorso().getInsegnanti());
            insegnante.setValue(lettera.getInsegnante());
            anno.setText(lettera.getAnnoScolastico() != null ? "Valida per l'anno scolastico " + lettera.getAnnoScolastico() : "");
        }
        if (nuova && emessa.isEmpty()) {
            emessa.setValue(LocalDate.now());
        }

        FormLayout form = new FormLayout(corso, insegnante, emessa, anno, tariffa, unaTantum, giorni, nota);
        form.setColspan(giorni, 2);
        form.setColspan(nota, 2);

        Button salva = new Button("Salva", e -> {
            if (!binder.writeBeanIfValid(lettera)) {
                return;
            }
            try {
                if (nuova) {
                    lettera.setAnnoScolastico(lettera.getCorso().getAnnoScolastico());
                }
                service.salva(lettera, username);
                dialog.close();
                Notification.show("Lettera di incarico salvata");
                dopoSalvataggio.run();
            } catch (RuntimeException ex) {
                Notification.show("Errore nel salvataggio: " + ex.getMessage());
            }
        });
        salva.addThemeVariants(ButtonVariant.PRIMARY);
        dialog.add(form);
        dialog.getFooter().add(ViewSupport.closeButton(dialog, "Annulla"), salva);
        dialog.open();
    }

    /** Compenso della lettera: "25,00 €/ora", "300,00 € una tantum" o entrambi. */
    public static String compenso(LetteraIncarico lettera) {
        String orario = lettera.getTariffaOraria() != null ? Formati.euro(lettera.getTariffaOraria()) + "/ora" : null;
        String fisso = lettera.getImportoUnaTantum() != null ? Formati.euro(lettera.getImportoUnaTantum()) + " una tantum" : null;
        return orario != null && fisso != null ? orario + " + " + fisso : Objects.requireNonNullElse(orario, Objects.requireNonNullElse(fisso, ""));
    }

    private static BigDecimalField importo(String etichetta) {
        BigDecimalField campo = new BigDecimalField(etichetta);
        campo.setLocale(Locale.ITALY);
        campo.setPrefixComponent(new Span("€"));
        campo.setClearButtonVisible(true);
        return campo;
    }

    private static boolean positivo(BigDecimal importo) {
        return importo != null && importo.signum() > 0;
    }

    private static <T> T servizio(Class<T> tipo) {
        return VaadinService.getCurrent().getInstantiator().getOrCreate(tipo);
    }
}
