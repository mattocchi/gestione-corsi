package it.attocchi.views;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridTester;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextFieldTester;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.router.RouteNotFoundError;
import it.attocchi.entity.Corso;
import it.attocchi.entity.Genitore;
import it.attocchi.entity.Insegnante;
import it.attocchi.entity.Iscrizione;
import it.attocchi.entity.Lezione;
import it.attocchi.entity.Presenza;
import it.attocchi.entity.Studente;
import it.attocchi.entity.User;
import it.attocchi.entity.Presenza.StatoPresenza;
import it.attocchi.service.InsegnanteService;
import it.attocchi.service.CorsoService;
import it.attocchi.service.LezioneService;
import it.attocchi.service.PresenzaService;
import it.attocchi.service.StudenteService;
import it.attocchi.service.UserService;
import it.attocchi.views.admin.CorsiView;
import it.attocchi.views.admin.GenitoriView;
import it.attocchi.views.admin.InsegnantiView;
import it.attocchi.views.admin.PartecipantiDialog;
import it.attocchi.views.admin.StudentiView;
import it.attocchi.views.admin.UtentiView;
import it.attocchi.views.insegnante.LezioniInsegnanteView;
import it.attocchi.views.insegnante.LezioniInsegnanteView.FiltroLezioni;
import it.attocchi.views.genitore.FigliView;
import it.attocchi.views.studente.MieiCorsiView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithUserDetails;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test browserless delle view per ruolo, con gli utenti reali creati da DataInitializer.
 * Leggere le celle delle griglie fuori da una transazione verifica anche che i dati
 * mostrati siano caricati dai service (open-in-view è disattivato).
 */
@SpringBootTest
class ViewAccessTest extends SpringBrowserlessTest {

    /** Colonna delle azioni in Gestione Corsi (desktop). */
    private static final int AZIONI_CORSO = 8;

    @Autowired
    private PresenzaService presenzaService;

    @Autowired
    private LezioneService lezioneService;

    @Autowired
    private InsegnanteService insegnanteService;

    @Autowired
    private CorsoService corsoService;
    @Autowired
    private StudenteService studenteService;

    @Autowired
    private UserService userService;

    @Test
    @WithAnonymousUser
    void anonimo_vieneRediretto_alLogin() {
        navigate("", LoginView.class);
    }

    @Test
    @WithUserDetails("admin")
    void admin_menuMostraSoloVociAdmin() {
        navigate(HomeView.class);

        List<String> voci = find(SideNavItem.class).all().stream().map(SideNavItem::getLabel).toList();
        assertTrue(voci.containsAll(List.of("Home", "Gestione Corsi", "Gestione Studenti", "Gestione Utenti", "Insegnanti", "Genitori")), voci.toString());
        assertFalse(voci.contains("Le Mie Lezioni"), voci.toString());
    }

    @Test
    @WithUserDetails("admin")
    void admin_dashboardConTotali() {
        navigate(HomeView.class);

        assertEquals(List.of("Corsi: 2", "Allievi: 13", "Genitori: 9", "Insegnanti: 2"), indicatori());
    }

    @Test
    @WithUserDetails("segreteria")
    void segreteria_dashboardConTotali() {
        navigate(HomeView.class);

        assertEquals(List.of("Corsi: 2", "Allievi: 13", "Genitori: 9", "Insegnanti: 2"), indicatori());
    }

    @Test
    @WithUserDetails("genitore3")
    void genitore_dashboardConICorsiDeiFigli() {
        navigate(HomeView.class);

        // genitore3: Studente Canto3 (Canto) e Studente Chitarra1 (Chitarra)
        assertEquals(List.of("Corsi dei figli: 2"), indicatori());
    }

    @Test
    @WithUserDetails("insegnante1")
    void insegnante_dashboardConCorsiELezioni() {
        Insegnante insegnante = insegnanteService.findByUsername("insegnante1").orElseThrow();
        Lezione completata = lezioneService.findByInsegnante(insegnante.getId()).getFirst();
        completata.setCompletata(true);
        lezioneService.save(completata);
        try {
            // La Home è già aperta all'avvio del test: si esce e si rientra per ricrearla con i dati aggiornati
            navigate(LezioniInsegnanteView.class);
            navigate(HomeView.class);

            assertEquals(List.of("Corsi assegnati: 1", "Lezioni fatte: 1", "Lezioni da fare: 29"), indicatori());
        } finally {
            completata.setCompletata(false);
            lezioneService.save(completata);
        }
    }

    @Test
    @WithUserDetails("studente_canto1")
    void allievo_dashboardConIPropriCorsi() {
        navigate(HomeView.class);

        assertEquals(List.of("Corsi a cui sono iscritto: 1"), indicatori());
    }

    /** Riquadri della dashboard come "etichetta: valore", nell'ordine di visualizzazione. */
    private List<String> indicatori() {
        return find(Card.class).all().stream().map(Card::getAriaLabel).map(o -> o.orElse("")).toList();
    }

    @Test
    @WithUserDetails("admin")
    void admin_corsi_grigliaEDialogModifica() {
        navigate(CorsiView.class);

        var grid = test(find(Grid.class).single());
        assertEquals(2, grid.size());
        for (int row = 0; row < grid.size(); row++) {
            for (int col = 0; col < 6; col++) {
                grid.getCellText(row, col);
            }
        }

        assertAzioni(grid.getCellComponent(0, AZIONI_CORSO), "Modifica", "Partecipanti", "Genera Lezioni", "Elimina");

        // Apre il dialog di modifica: i giorni della settimana devono essere già caricati
        Button modifica = (Button) grid.getCellComponent(0, AZIONI_CORSO).getChildren().findFirst().orElseThrow();
        assertEquals("Modifica", modifica.getElement().getAttribute("aria-label"));
        test(modifica).click();
        CheckboxGroup<?> giorni = find(CheckboxGroup.class).single();
        assertFalse(giorni.getValue().isEmpty());
    }

    @Test
    @WithUserDetails("admin")
    @SuppressWarnings("unchecked")
    void admin_corsi_partecipantiInOrdineDiOrario() {
        navigate(CorsiView.class);
        var grid = test((Grid<Corso>) find(Grid.class).single());
        int canto = riga(grid, "Corso di Canto");

        Button partecipanti = pulsante(grid.getCellComponent(canto, AZIONI_CORSO), "Partecipanti");
        test(partecipanti).click();

        PartecipantiDialog dialog = find(PartecipantiDialog.class).single();
        var righe = test((Grid<Iscrizione>) find(Grid.class).from(dialog).single());
        assertEquals(List.of("17:00 Studente Canto5", "17:40 Studente Canto4", "18:20 Studente Canto3",
            "19:00 Studente Canto2", "19:40 Studente Canto1",
            "- Studente Canto6", "- Studente Canto7"), ordinePartecipanti(righe));
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_assegnaOrarioLezioneAiPartecipanti() {
        navigate(CorsiView.class);
        var corsi = test((Grid<Corso>) find(Grid.class).single());
        int canto = riga(corsi, "Corso di Canto");
        test(pulsante(corsi.getCellComponent(canto, AZIONI_CORSO), "Partecipanti")).click();
        PartecipantiDialog dialog = find(PartecipantiDialog.class).single();
        var righe = test((Grid<Iscrizione>) find(Grid.class).from(dialog).single());

        // Canto6 (senza orario, penultimo) alle 16:30: salvato e spostato in cima
        assertEquals("Studente Canto6", righe.getCellText(5, 1));
        test((TimePicker) righe.getCellComponent(5, 0)).setValue(LocalTime.of(16, 30));
        assertEquals("16:30 Studente Canto6", ordinePartecipanti(righe).getFirst());
        assertEquals(LocalTime.of(16, 30), corsoService.findPartecipanti(corsi.getRow(canto).getId()).getFirst().getOrarioLezione());

        // Tolto l'orario torna in fondo (ripristina i dati di test)
        test((TimePicker) righe.getCellComponent(0, 0)).clear();
        assertEquals("- Studente Canto6", ordinePartecipanti(righe).get(5));
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_creaCorsoAssegnatoAUnInsegnante() {
        navigate(CorsiView.class);
        var corsi = test((Grid<Corso>) find(Grid.class).single());
        test(find(Button.class).withText("Nuovo Corso").single()).click();

        // Senza insegnante il corso non viene salvato
        test(find(TextField.class).withLabel("Nome").single()).setValue("Corso di Batteria");
        test(find(DatePicker.class).withLabel("Data Inizio").single()).setValue(LocalDate.of(2026, 11, 2));
        test(find(IntegerField.class).withLabel("Numero Lezioni").single()).setValue(10);
        test(find(IntegerField.class).withLabel("Durata Lezione (minuti)").single()).setValue(30);
        ((CheckboxGroup<DayOfWeek>) find(CheckboxGroup.class).single()).setValue(Set.of(DayOfWeek.MONDAY));
        test(find(Button.class).withText("Salva").single()).click();
        assertTrue(find(ComboBox.class).withLabel("Insegnante").single().isInvalid());
        assertEquals(2, corsi.size());

        test(find(ComboBox.class).withLabel("Insegnante").single()).selectItem("Marco Rossi");
        test(find(Button.class).withText("Salva").single()).click();
        Corso nuovo = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Batteria")).findFirst().orElseThrow();
        try {
            assertEquals(3, corsi.size());
            assertEquals("Marco Rossi", nuovo.getInsegnante().getNomeCompleto());
            assertEquals(LocalDate.of(2026, 11, 2), nuovo.getDataInizio());
        } finally {
            corsoService.delete(nuovo);
        }
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_modificaCorsoSenzaPerdereIPartecipanti() {
        navigate(CorsiView.class);
        var corsi = test((Grid<Corso>) find(Grid.class).single());
        int canto = riga(corsi, "Corso di Canto");
        Long corsoId = corsi.getRow(canto).getId();

        test(pulsante(corsi.getCellComponent(canto, AZIONI_CORSO), "Modifica")).click();
        test(find(Button.class).withText("Salva").single()).click();

        assertEquals(7, corsoService.findPartecipanti(corsoId).size());
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_assegnaDataInizioDelCorso() {
        navigate(CorsiView.class);
        var corsi = test((Grid<Corso>) find(Grid.class).single());
        test(find(Button.class).withText("Nuovo Corso").single()).click();
        test(find(TextField.class).withLabel("Nome").single()).setValue("Corso di Tromba");
        test(find(ComboBox.class).withLabel("Insegnante").single()).selectItem("Maria Verdi");
        test(find(IntegerField.class).withLabel("Numero Lezioni").single()).setValue(10);
        test(find(IntegerField.class).withLabel("Durata Lezione (minuti)").single()).setValue(30);

        // Senza data di inizio il corso non viene salvato
        test(find(Button.class).withText("Salva").single()).click();
        assertTrue(find(DatePicker.class).withLabel("Data Inizio").single().isInvalid());
        assertEquals(2, corsi.size());

        test(find(DatePicker.class).withLabel("Data Inizio").single()).setValue(LocalDate.of(2027, 1, 11));
        test(find(Button.class).withText("Salva").single()).click();
        Corso nuovo = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Tromba")).findFirst().orElseThrow();
        try {
            assertEquals(LocalDate.of(2027, 1, 11), nuovo.getDataInizio());
            assertEquals("2026/2027", nuovo.getAnnoScolastico());
            assertTrue(nuovo.isAttivo());
            assertEquals(Formati.data(LocalDate.of(2027, 1, 11)), corsi.getCellText(riga(corsi, "Corso di Tromba"), 3));
        } finally {
            corsoService.delete(nuovo);
        }
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_assegnaCostoTotaleDelCorso() {
        navigate(CorsiView.class);
        var corsi = test((Grid<Corso>) find(Grid.class).single());
        int canto = riga(corsi, "Corso di Canto");
        Long corsoId = corsi.getRow(canto).getId();

        test(pulsante(corsi.getCellComponent(canto, AZIONI_CORSO), "Modifica")).click();
        BigDecimalField costo = find(BigDecimalField.class).withLabel("Costo Totale").single();
        assertEquals(0, new BigDecimal("600.00").compareTo(costo.getValue()));
        new TextFieldTester<>(costo).setValue(new BigDecimal("650.50"));
        test(find(Button.class).withText("Salva").single()).click();
        try {
            assertEquals(0, new BigDecimal("650.50").compareTo(corsoService.findById(corsoId).getCostoTotale()));
        } finally {
            Corso corso = corsoService.findById(corsoId);
            corso.setCostoTotale(new BigDecimal("600.00"));
            corsoService.save(corso);
        }
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_modificaNomeECognomeDiUnoStudente() {
        navigate(StudentiView.class);
        var studenti = test((Grid<Studente>) find(Grid.class).single());
        int riga = -1;
        for (int row = 0; row < studenti.size(); row++) {
            if (studenti.getRow(row).getNomeCompleto().equals("Studente Canto2")) {
                riga = row;
            }
        }
        Long userId = studenti.getRow(riga).getUser().getId();

        test((Button) studenti.getCellComponent(riga, 5).getChildren().findFirst().orElseThrow()).click();
        test(find(TextField.class).withLabel("Nome").single()).setValue("Giulia");
        test(find(TextField.class).withLabel("Cognome").single()).setValue("Neri");
        test(find(Button.class).withText("Salva").single()).click();
        try {
            assertEquals("Giulia Neri", userService.findById(userId).getNomeCompleto());
            assertTrue(IntStream.range(0, studenti.size()).anyMatch(row -> studenti.getCellText(row, 0).equals("Giulia Neri")));
        } finally {
            User user = userService.findById(userId);
            user.setNome("Studente");
            user.setCognome("Canto2");
            userService.save(user, null);
        }
    }

    @Test
    @WithUserDetails("segreteria")
    void segreteria_creaStudenteAssegnatoAUnGenitore() {
        navigate(StudentiView.class);
        var studenti = test(find(Grid.class).single());
        test(find(Button.class).withText("Nuovo Studente").single()).click();
        test(find(TextField.class).withLabel("Username").single()).setValue("studente_nuovo");
        test(find(PasswordField.class).withLabel("Password").single()).setValue("segreta");
        test(find(TextField.class).withLabel("Nome").single()).setValue("Luca");
        test(find(TextField.class).withLabel("Cognome").single()).setValue("Gialli");
        test(find(TextField.class).withLabel("Codice Fiscale").single()).setValue("GLLLCU12A01H501Z");
        test(find(DatePicker.class).withLabel("Data Nascita").single()).setValue(LocalDate.of(2012, 1, 1));

        // Senza genitore lo studente (e il suo utente) non viene creato
        test(find(Button.class).withText("Salva").single()).click();
        assertTrue(find(ComboBox.class).withLabel("Genitore").single().isInvalid());
        assertFalse(userService.existsByUsername("studente_nuovo"));

        test(find(ComboBox.class).withLabel("Genitore").single()).selectItem("Genitore1 Cognome1");
        test(find(Button.class).withText("Salva").single()).click();
        Studente nuovo = studenteService.findByUsername("studente_nuovo").orElseThrow();
        try {
            assertEquals(14, studenti.size());
            assertEquals("Genitore1 Cognome1", nuovo.getGenitore().getNomeCompleto());
            assertTrue(nuovo.getUser().getRoles().contains("STUDENTE"));
        } finally {
            studenteService.delete(nuovo);
            userService.delete(nuovo.getUser());
        }
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_impostaAnnoScolasticoEStatoDelCorso() {
        navigate(CorsiView.class);
        var corsi = test((Grid<Corso>) find(Grid.class).single());
        int canto = riga(corsi, "Corso di Canto");
        Long corsoId = corsi.getRow(canto).getId();
        assertEquals("2024/2025", corsi.getCellText(canto, 1));
        assertEquals("Attivo", ((Badge) corsi.getCellComponent(canto, 7)).getText());

        test(pulsante(corsi.getCellComponent(canto, AZIONI_CORSO), "Modifica")).click();
        test(find(ComboBox.class).withLabel("Anno Scolastico").single()).selectItem("2025/2026");
        test(find(Checkbox.class).withLabel("Attivo").single()).click();
        test(find(Button.class).withText("Salva").single()).click();
        try {
            Corso salvato = corsoService.findById(corsoId);
            assertEquals("2025/2026", salvato.getAnnoScolastico());
            assertFalse(salvato.isAttivo());
        } finally {
            Corso corso = corsoService.findById(corsoId);
            corso.setAnnoScolastico("2024/2025");
            corso.setAttivo(true);
            corsoService.save(corso);
        }
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_filtraCorsiPerStatoEAnno() {
        Corso chitarra = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Chitarra")).findFirst().orElseThrow();
        chitarra.setAttivo(false);
        corsoService.save(chitarra);
        try {
            navigate(CorsiView.class);
            var corsi = test((Grid<Corso>) find(Grid.class).single());
            SegmentedControl<CorsiView.FiltroStato> stato = find(SegmentedControl.class).single();

            // default: solo i corsi attivi
            assertEquals(CorsiView.FiltroStato.ATTIVI, stato.getValue());
            assertEquals(1, corsi.size());
            assertEquals("Corso di Canto", corsi.getRow(0).getNome());

            test(stato.getPulsante(CorsiView.FiltroStato.NON_ATTIVI)).click();
            assertEquals(1, corsi.size());
            assertEquals("Non attivo", ((Badge) corsi.getCellComponent(0, 7)).getText());

            test(stato.getPulsante(CorsiView.FiltroStato.TUTTI)).click();
            assertEquals(2, corsi.size());

            ComboBox<String> anno = find(ComboBox.class).withPlaceholder("Tutti gli anni").single();
            test(anno).selectItem("2024/2025");
            assertEquals(2, corsi.size());
        } finally {
            chitarra = corsoService.findById(chitarra.getId());
            chitarra.setAttivo(true);
            corsoService.save(chitarra);
        }
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_corsoNonAttivoPartecipantiInSolaLettura() {
        Corso chitarra = disattiva("Corso di Chitarra");
        try {
            navigate(CorsiView.class);
            var corsi = test((Grid<Corso>) find(Grid.class).single());
            test(((SegmentedControl<CorsiView.FiltroStato>) find(SegmentedControl.class).single())
                .getPulsante(CorsiView.FiltroStato.NON_ATTIVI)).click();
            assertFalse(pulsante(corsi.getCellComponent(0, AZIONI_CORSO), "Genera Lezioni").isEnabled());

            test(pulsante(corsi.getCellComponent(0, AZIONI_CORSO), "Partecipanti")).click();
            PartecipantiDialog dialog = find(PartecipantiDialog.class).single();
            assertFalse(find(ComboBox.class).from(dialog).exists());
            assertTrue(find(TimePicker.class).from(dialog).all().stream().allMatch(TimePicker::isReadOnly));

            Long studente = studenteService.findByUsername("studente_canto1").orElseThrow().getId();
            assertThrows(IllegalStateException.class, () -> corsoService.iscrivi(chitarra.getId(), studente));
            assertThrows(IllegalStateException.class, () -> corsoService.generaLezioni(chitarra));
        } finally {
            riattiva(chitarra);
        }
    }

    @Test
    @WithUserDetails("insegnante2")
    void insegnante_nonVedeICorsiNonAttivi() {
        Corso chitarra = disattiva("Corso di Chitarra");
        try {
            navigate(LezioniInsegnanteView.class);
            SegmentedControl<FiltroLezioni> filtro = find(SegmentedControl.class).single();
            test(filtro.getPulsante(FiltroLezioni.TUTTE)).click();
            assertEquals(0, test(find(Grid.class).single()).size());

            navigate(HomeView.class);
            assertEquals(List.of("Corsi assegnati: 0", "Lezioni fatte: 0", "Lezioni da fare: 0"), indicatori());

            Presenza presenza = presenzaService.findByCorsoAndStudente(chitarra.getId(),
                studenteService.findByUsername("studente_chitarra1").orElseThrow().getId()).getFirst();
            presenza.setStato(StatoPresenza.ASSENTE_GIUSTIFICATA);
            assertThrows(IllegalStateException.class, () -> presenzaService.save(presenza));
        } finally {
            riattiva(chitarra);
        }
    }

    @Test
    @WithUserDetails("studente_chitarra1")
    void allievo_nonVedeICorsiNonAttivi() {
        Corso chitarra = disattiva("Corso di Chitarra");
        try {
            navigate(MieiCorsiView.class);
            assertEquals(0, test(find(Grid.class).single()).size());
            navigate(HomeView.class);
            assertEquals(List.of("Corsi a cui sono iscritto: 0"), indicatori());
        } finally {
            riattiva(chitarra);
        }
    }

    @Test
    @WithUserDetails("genitore3")
    void genitore_vedeAncheICorsiNonAttivi() {
        Corso chitarra = disattiva("Corso di Chitarra");
        try {
            navigate(FigliView.class);
            var figli = test(find(Grid.class).single());
            assertEquals("1", figli.getCellText(0, 2));
            assertEquals("1", figli.getCellText(1, 2));
        } finally {
            riattiva(chitarra);
        }
    }

    private Corso disattiva(String nome) {
        Corso corso = corsoService.findAll().stream().filter(c -> c.getNome().equals(nome)).findFirst().orElseThrow();
        corso.setAttivo(false);
        return corsoService.save(corso);
    }

    private void riattiva(Corso corso) {
        Corso c = corsoService.findById(corso.getId());
        c.setAttivo(true);
        corsoService.save(c);
    }

    private static List<String> ordinePartecipanti(GridTester<Grid<Iscrizione>, Iscrizione> righe) {
        List<String> ordine = new ArrayList<>();
        for (int row = 0; row < righe.size(); row++) {
            LocalTime orario = ((TimePicker) righe.getCellComponent(row, 0)).getValue();
            ordine.add((orario != null ? orario.toString() : "-") + " " + righe.getCellText(row, 1));
        }
        return ordine;
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_aggiungeERimuovePartecipanti() {
        navigate(CorsiView.class);
        var corsi = test((Grid<Corso>) find(Grid.class).single());
        int chitarra = riga(corsi, "Corso di Chitarra");
        Corso corso = corsi.getRow(chitarra);
        test(pulsante(corsi.getCellComponent(chitarra, AZIONI_CORSO), "Partecipanti")).click();

        PartecipantiDialog dialog = find(PartecipantiDialog.class).single();
        var partecipanti = test((Grid<Iscrizione>) find(Grid.class).from(dialog).single());
        assertEquals(6, partecipanti.size());

        // Aggiunta: compare nell'elenco e ha le presenze in tutte le lezioni (nessuna completata)
        ComboBox<?> nuovo = find(ComboBox.class).from(dialog).single();
        test(nuovo).selectItem("Studente Canto1");
        test(find(Button.class).from(dialog).withText("Aggiungi").single()).click();
        assertEquals(7, partecipanti.size());
        Long studenteId = corsoService.findPartecipanti(corso.getId()).stream()
            .filter(i -> i.getStudente().getNomeCompleto().equals("Studente Canto1"))
            .findFirst().orElseThrow().getStudente().getId();
        assertEquals(30, presenzaService.findByCorsoAndStudente(corso.getId(), studenteId).size());

        // Rimozione: sparisce dall'elenco con le presenze delle lezioni da fare
        int riga = -1;
        for (int row = 0; row < partecipanti.size(); row++) {
            if (partecipanti.getCellText(row, 1).equals("Studente Canto1")) {
                riga = row;
            }
        }
        test((Button) partecipanti.getCellComponent(riga, 2).getChildren().findFirst().orElseThrow()).click();
        assertEquals(6, partecipanti.size());
        assertTrue(presenzaService.findByCorsoAndStudente(corso.getId(), studenteId).isEmpty());
    }

    private static int riga(GridTester<Grid<Corso>, Corso> grid, String nome) {
        for (int row = 0; row < grid.size(); row++) {
            if (grid.getRow(row).getNome().equals(nome)) {
                return row;
            }
        }
        throw new AssertionError("Corso non trovato: " + nome);
    }

    private static Button pulsante(Component azioni, String etichetta) {
        return azioni.getChildren()
            .filter(c -> c instanceof Button b && etichetta.equals(b.getElement().getAttribute("aria-label")))
            .map(Button.class::cast)
            .findFirst()
            .orElseThrow(() -> new AssertionError("Pulsante non trovato: " + etichetta));
    }

    @Test
    @WithUserDetails("admin")
    void admin_studenti_grigliaConNumeroCorsi() {
        navigate(StudentiView.class);

        var grid = test(find(Grid.class).single());
        assertEquals(13, grid.size());
        for (int row = 0; row < grid.size(); row++) {
            assertEquals("1", grid.getCellText(row, 4));
            grid.getCellText(row, 3);
        }
    }

    @Test
    @WithUserDetails("admin")
    void admin_utenti_griglia() {
        navigate(UtentiView.class);

        assertEquals(26, test(find(Grid.class).single()).size());
    }

    @Test
    @WithUserDetails("segreteria")
    void segreteria_accedeEdNonVedeGestioneUtenti() {
        navigate(HomeView.class);

        List<String> voci = find(SideNavItem.class).all().stream().map(SideNavItem::getLabel).toList();
        assertTrue(voci.contains("Home"), voci.toString());
        assertFalse(voci.contains("Gestione Utenti"), voci.toString());
        navigate("utenti", RouteNotFoundError.class);
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_vedeCorsiEStudentiSenzaElimina() {
        navigate(CorsiView.class);
        var corsi = test((Grid<Corso>) find(Grid.class).single());
        assertEquals(2, corsi.size());
        assertAzioni(corsi.getCellComponent(0, AZIONI_CORSO), "Modifica", "Partecipanti", "Genera Lezioni");

        navigate(StudentiView.class);
        var studenti = test(find(Grid.class).single());
        assertEquals(13, studenti.size());
        assertAzioni(studenti.getCellComponent(0, 5), "Modifica");
    }

    @Test
    @WithUserDetails("segreteria")
    void segreteria_vedeGliInsegnanti() {
        navigate(InsegnantiView.class);

        var grid = test(find(Grid.class).single());
        assertEquals(2, grid.size());
        for (int row = 0; row < grid.size(); row++) {
            assertEquals("1", grid.getCellText(row, 3));
        }
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_vedeIGenitori() {
        navigate(GenitoriView.class);

        var grid = test((Grid<Genitore>) find(Grid.class).single());
        assertEquals(9, grid.size());
        int genitore3 = -1;
        for (int row = 0; row < grid.size(); row++) {
            grid.getCellText(row, 3);
            if (grid.getRow(row).getUser().getUsername().equals("genitore3")) {
                genitore3 = row;
            }
        }
        assertEquals("Studente Canto3, Studente Chitarra1", grid.getCellText(genitore3, 3));
        assertEquals("2", grid.getCellText(genitore3, 4));
    }

    private static void assertAzioni(Component azioni, String... etichette) {
        assertEquals(List.of(etichette), azioni.getChildren()
            .map(c -> c.getElement().getAttribute("aria-label")).toList());
    }

    @Test
    @WithUserDetails("insegnante1")
    void insegnante_vedeLeProprieLezioni() {
        navigate(LezioniInsegnanteView.class);

        var grid = test(find(Grid.class).single());
        assertEquals(30, grid.size());
        assertEquals("Corso di Canto", grid.getCellText(0, 0));
    }

    @Test
    @WithUserDetails("insegnante1")
    void insegnante_segnaAssenzaGiustificataConUnTocco() {
        navigate(LezioniInsegnanteView.class);
        var lezioni = test(find(Grid.class).single());
        Button gestisciPresenze = (Button) lezioni.getCellComponent(0, 5).getChildren().findFirst().orElseThrow();
        test(gestisciPresenze).click();

        @SuppressWarnings("unchecked")
        Grid<Presenza> gridPresenze = find(Grid.class).all().getLast();
        var presenze = test(gridPresenze);
        Presenza presenza = presenze.getRow(0);
        StatoPresenzaToggle toggle = (StatoPresenzaToggle) presenze.getCellComponent(0, 0).getChildren()
            .filter(StatoPresenzaToggle.class::isInstance).findFirst().orElseThrow();
        assertEquals(StatoPresenza.PRESENTE, toggle.getValue());

        test(toggle.getPulsante(StatoPresenza.ASSENTE_GIUSTIFICATA)).click();

        assertEquals(StatoPresenza.ASSENTE_GIUSTIFICATA, presenzaService.findById(presenza.getId()).getStato());

        // ripristina il dato di test per gli altri test
        test(toggle.getPulsante(StatoPresenza.PRESENTE)).click();
        assertEquals(StatoPresenza.PRESENTE, presenzaService.findById(presenza.getId()).getStato());
    }

    @Test
    @WithUserDetails("insegnante1")
    @SuppressWarnings("unchecked")
    void insegnante_filtraLezioniPerStato() {
        Insegnante insegnante = insegnanteService.findByUsername("insegnante1").orElseThrow();
        Lezione completata = lezioneService.findByInsegnante(insegnante.getId()).getFirst();
        completata.setCompletata(true);
        lezioneService.save(completata);
        try {
            navigate(LezioniInsegnanteView.class);
            var grid = test(find(Grid.class).single());
            SegmentedControl<FiltroLezioni> filtro = find(SegmentedControl.class).single();

            // default: solo le lezioni da fare
            assertEquals(FiltroLezioni.DA_FARE, filtro.getValue());
            assertEquals(29, grid.size());

            test(filtro.getPulsante(FiltroLezioni.COMPLETATE)).click();
            assertEquals(1, grid.size());
            Badge badge = (Badge) grid.getCellComponent(0, 4);
            assertEquals("Completata", badge.getText());
            assertTrue(badge.getThemeNames().contains(BadgeVariant.SUCCESS.getVariantName()));

            test(filtro.getPulsante(FiltroLezioni.TUTTE)).click();
            assertEquals(30, grid.size());

            test(filtro.getPulsante(FiltroLezioni.DA_FARE)).click();
            assertEquals(29, grid.size());
            assertEquals("Da fare", ((Badge) grid.getCellComponent(0, 4)).getText());
        } finally {
            completata.setCompletata(false);
            lezioneService.save(completata);
        }
    }

    @Test
    @WithUserDetails("insegnante1")
    void insegnante_nonAccedeAViewAdmin() {
        navigate("corsi", RouteNotFoundError.class);
    }

    @Test
    @WithUserDetails("genitore3")
    void genitore_vedeIFigliConNumeroCorsi() {
        navigate(FigliView.class);

        var grid = test(find(Grid.class).single());
        assertEquals(2, grid.size());
        assertEquals("1", grid.getCellText(0, 2));
        assertEquals("1", grid.getCellText(1, 2));
    }

    @Test
    @WithUserDetails("studente_canto1")
    void studente_vedeIPropriCorsi() {
        assertInstanceOf(MieiCorsiView.class, navigate(MieiCorsiView.class));

        var grid = test(find(Grid.class).single());
        assertEquals(1, grid.size());
        assertEquals("Corso di Canto", grid.getCellText(0, 0));
    }
}
