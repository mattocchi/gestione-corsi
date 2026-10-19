package it.attocchi.views;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridTester;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.EmailField;
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
import it.attocchi.entity.MetodoPagamento;
import it.attocchi.entity.Pagamento;
import it.attocchi.entity.Presenza;
import it.attocchi.entity.Allievo;
import it.attocchi.entity.AnnoScolastico;
import it.attocchi.entity.Festivita;
import it.attocchi.entity.RecuperoPassword;
import it.attocchi.entity.User;
import it.attocchi.entity.Presenza.StatoPresenza;
import it.attocchi.repository.InsegnanteRepository;
import it.attocchi.service.GenitoreService;
import it.attocchi.service.InsegnanteService;
import it.attocchi.service.CorsoService;
import it.attocchi.service.LezioneService;
import it.attocchi.service.PagamentoService;
import it.attocchi.service.PagamentoService.RiepilogoPagamenti;
import it.attocchi.service.PagamentoService.RigaPagamento;
import it.attocchi.service.PresenzaService;
import it.attocchi.service.AllievoService;
import it.attocchi.service.AnnoScolasticoService;
import it.attocchi.service.FestivitaService;
import it.attocchi.service.RecuperoPasswordService;
import it.attocchi.service.DocumentiService;
import it.attocchi.service.EmailService;
import it.attocchi.repository.InvioEmailRepository;
import it.attocchi.repository.RecuperoPasswordRepository;
import it.attocchi.entity.InvioEmail;
import it.attocchi.entity.TipoDocumento;
import it.attocchi.service.BilancioService;
import it.attocchi.service.RimborsoService;
import it.attocchi.entity.Rimborso;
import it.attocchi.service.UserService;
import it.attocchi.views.admin.AnniScolasticiView;
import it.attocchi.views.admin.FestivitaView;
import it.attocchi.views.admin.CorsiView;
import it.attocchi.views.admin.LezioniView;
import it.attocchi.views.admin.BilancioDialog;
import it.attocchi.views.admin.EmailDiTestDialog;
import it.attocchi.views.admin.EmailInviateView;
import it.attocchi.views.admin.GrigliaInvii;
import it.attocchi.views.admin.RimborsiDialog;
import it.attocchi.views.admin.RimborsiView;
import it.attocchi.views.admin.GenitoriView;
import it.attocchi.views.admin.RiepilogoIscrittiView;
import it.attocchi.views.admin.GestionePagamentiView;
import it.attocchi.views.admin.InsegnantiView;
import it.attocchi.views.admin.InvioEmailDialog;
import it.attocchi.views.admin.PartecipantiDialog;
import it.attocchi.views.admin.QuotaPagamentiDialog;
import it.attocchi.views.admin.AllieviView;
import it.attocchi.views.admin.UtentiView;
import it.attocchi.views.insegnante.LezioniInsegnanteView;
import it.attocchi.views.insegnante.MieiRimborsiView;
import it.attocchi.views.ElencoLezioni.FiltroLezioni;
import it.attocchi.views.genitore.FigliView;
import it.attocchi.views.genitore.PagamentiFigliView;
import it.attocchi.views.allievo.MieiCorsiView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithUserDetails;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    private AllievoService allievoService;

    @Autowired
    private PagamentoService pagamentoService;

    @Autowired
    private UserService userService;

    @Autowired
    private InsegnanteRepository insegnanteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private GenitoreService genitoreService;

    @Autowired
    private AnnoScolasticoService annoScolasticoService;

    @Autowired
    private FestivitaService festivitaService;

    @Autowired
    private RimborsoService rimborsoService;

    @Autowired
    private DocumentiService documentiService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private InvioEmailRepository invioEmailRepository;

    @Autowired
    private RecuperoPasswordRepository recuperoPasswordRepository;

    @Test
    @WithAnonymousUser
    void anonimo_vieneRediretto_alLogin() {
        navigate("", LoginView.class);
    }

    @Test
    @WithAnonymousUser
    void anonimo_chiedeIlLinkEVedeSempreLoStessoMessaggio() {
        try {
            navigate(RecuperoPasswordView.class);
            TextField account = find(TextField.class).withLabel("Nome utente o email").single();
            test(find(Button.class).withText("Invia link").single()).click();
            assertTrue(account.isInvalid());

            test(account).setValue("nessuno");
            test(find(Button.class).withText("Invia link").single()).click();
            assertEquals("Controlla la tua email", find(H2.class).single().getText());
            assertEquals(0, recuperoPasswordRepository.count());

            // Stessa route: la vista si ricrea passando da un'altra pagina
            navigate(LoginView.class);
            navigate(RecuperoPasswordView.class);
            test(find(TextField.class).withLabel("Nome utente o email").single()).setValue("insegnante2");
            test(find(Button.class).withText("Invia link").single()).click();
            assertEquals("Controlla la tua email", find(H2.class).single().getText());
            assertEquals(1, recuperoPasswordRepository.count());
        } finally {
            recuperoPasswordRepository.deleteAll();
        }
    }

    @Test
    @WithAnonymousUser
    void anonimo_reimpostaLaPasswordDalLinkUnaSolaVolta() {
        User user = userService.findByUsername("insegnante2");
        String originale = user.getPassword();
        String token = "token-vista-di-prova";
        recuperoPasswordRepository.save(new RecuperoPassword(user, RecuperoPasswordService.hash(token),
            LocalDateTime.now().plusHours(1)));
        try {
            navigate("nuova-password?token=" + token, NuovaPasswordView.class);
            PasswordField nuova = find(PasswordField.class).withLabel("Nuova password").single();
            PasswordField conferma = find(PasswordField.class).withLabel("Conferma password").single();
            test(nuova).setValue("corta");
            test(conferma).setValue("corta");
            test(find(Button.class).withText("Salva password").single()).click();
            assertTrue(nuova.isInvalid());

            test(nuova).setValue("nuovaPassword1");
            test(conferma).setValue("diversa12");
            test(find(Button.class).withText("Salva password").single()).click();
            assertTrue(conferma.isInvalid());

            test(conferma).setValue("nuovaPassword1");
            test(find(Button.class).withText("Salva password").single()).click();
            assertInstanceOf(PasswordAggiornataView.class, getCurrentView());
            assertTrue(passwordEncoder.matches("nuovaPassword1", userService.findByUsername("insegnante2").getPassword()));

            navigate("nuova-password?token=" + token, NuovaPasswordView.class);
            assertEquals("Link non valido", find(H2.class).single().getText());
        } finally {
            User ripristino = userService.findByUsername("insegnante2");
            ripristino.setPassword(originale);
            userService.save(ripristino, null);
            recuperoPasswordRepository.deleteAll();
        }
    }

    @Test
    @WithUserDetails("admin")
    void admin_menuMostraSoloVociAdmin() {
        navigate(HomeView.class);

        List<String> voci = find(SideNavItem.class).all().stream().map(SideNavItem::getLabel).toList();
        assertTrue(voci.containsAll(List.of("Home", "Gestione Corsi", "Gestione Pagamenti", "Gestione Allievi", "Gestione Utenti", "Insegnanti", "Genitori")), voci.toString());
        assertFalse(voci.contains("Le Mie Lezioni"), voci.toString());
    }

    @Test
    @WithUserDetails("admin")
    void admin_dashboardConTotali() {
        navigate(HomeView.class);

        assertEquals(List.of("Corsi: 2", "Allievi: 13", "Genitori: 9", "Insegnanti: 2"), indicatori().subList(0, 4));
    }

    @Test
    @WithUserDetails("segreteria")
    void segreteria_dashboardConTotali() {
        navigate(HomeView.class);

        assertEquals(List.of("Corsi: 2", "Allievi: 13", "Genitori: 9", "Insegnanti: 2"), indicatori().subList(0, 4));
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_dashboardConRiepilogoPagamenti() {
        Corso canto = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow();
        Allievo allievo = allievoService.findByUsername("allievo_canto3").orElseThrow();
        Pagamento rata = new Pagamento();
        rata.setData(LocalDate.now());
        rata.setImporto(new BigDecimal("200"));
        pagamentoService.salva(canto.getId(), allievo.getId(), rata);
        try {
            // La Home è già aperta: si ricostruisce navigando altrove e tornando
            navigate(CorsiView.class);
            navigate(HomeView.class);
            // 13 partecipanti da 600 € nei due corsi attivi, una rata da 200 €
            assertEquals(List.of("Saldati: 0", "Pagamento parziale: 1", "Senza pagamenti: 12",
                    "Incassato: 200,00 € (3% del totale)", "Da incassare: 7.600,00 € (97% del totale)"),
                indicatori().subList(4, 9));

            var grid = test((Grid<RiepilogoPagamenti>) find(Grid.class).single());
            assertEquals(2, grid.size());
            assertEquals("Corso di Canto", grid.getCellText(0, 0));
            // Indici delle colonne visibili su desktop (la colonna card mobile è nascosta)
            assertEquals("1", grid.getCellText(0, 2));
            assertEquals("6", grid.getCellText(0, 3));
            assertEquals("200,00 € (5%)", grid.getCellText(0, 4));
            assertEquals("Corso di Chitarra", grid.getCellText(1, 0));
            assertEquals("3.600,00 € (100%)", grid.getCellText(1, 5));
        } finally {
            pagamentoService.findPagamenti(canto.getId(), allievo.getId()).forEach(pagamentoService::elimina);
        }
    }

    @Test
    @WithUserDetails("genitore3")
    void genitore_dashboardConICorsiDeiFigli() {
        navigate(HomeView.class);

        // genitore3: Allievo Canto3 (Canto) e Allievo Chitarra1 (Chitarra)
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
            apriLezioni();
            navigate(HomeView.class);

            assertEquals(List.of("Corsi assegnati: 1", "Lezioni fatte: 1", "Lezioni da fare: 29", "Lezioni non effettuate: 0"), indicatori());
        } finally {
            completata.setCompletata(false);
            lezioneService.save(completata);
        }
    }

    @Test
    @WithUserDetails("allievo_canto1")
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
        assertEquals(List.of("17:00 Allievo Canto5", "17:40 Allievo Canto4", "18:20 Allievo Canto3",
            "19:00 Allievo Canto2", "19:40 Allievo Canto1",
            "- Allievo Canto6", "- Allievo Canto7"), ordinePartecipanti(righe));
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
        assertEquals("Allievo Canto6", righe.getCellText(5, 1));
        test((TimePicker) righe.getCellComponent(5, 0)).setValue(LocalTime.of(16, 30));
        assertEquals("16:30 Allievo Canto6", ordinePartecipanti(righe).getFirst());
        assertEquals(LocalTime.of(16, 30), corsoService.findPartecipanti(corsi.getRow(canto).getId()).getFirst().getOrarioLezione());

        // Tolto l'orario torna in fondo (ripristina i dati di test)
        test((TimePicker) righe.getCellComponent(0, 0)).clear();
        assertEquals("- Allievo Canto6", ordinePartecipanti(righe).get(5));
    }

    @Test
    @WithUserDetails("segreteria")
    void campiDataOra_inItaliano() {
        assertEquals(java.util.Locale.ITALY, com.vaadin.flow.component.UI.getCurrent().getLocale());
        navigate(CorsiView.class);
        test(find(Button.class).withText("Nuovo Corso").single()).click();
        DatePicker dataInizio = find(DatePicker.class).withLabel("Data Inizio").single();
        assertEquals("dd/MM/yyyy", dataInizio.getI18n().getDateFormats().getFirst());
        assertEquals(1, dataInizio.getI18n().getFirstDayOfWeek());
        assertEquals(java.util.Locale.ITALY, find(TimePicker.class).withLabel("Orario Inizio").single().getLocale());
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
    void segreteria_assegnaUnSecondoInsegnanteAlCorso() {
        navigate(CorsiView.class);
        var corsi = test((Grid<Corso>) find(Grid.class).single());
        int canto = riga(corsi, "Corso di Canto");
        Long corsoId = corsi.getRow(canto).getId();
        try {
            test(pulsante(corsi.getCellComponent(canto, AZIONI_CORSO), "Modifica")).click();
            ComboBox<Insegnante> secondo = find(ComboBox.class).withLabel("Secondo insegnante").single();
            test(secondo).selectItem("Maria Verdi");
            test(find(Button.class).withText("Salva").single()).click();
            assertTrue(secondo.isInvalid());

            test(secondo).selectItem("Marco Rossi");
            test(find(Button.class).withText("Salva").single()).click();
            assertEquals("Maria Verdi, Marco Rossi", corsoService.findById(corsoId).getNomiInsegnanti());
            assertEquals("Maria Verdi, Marco Rossi", corsi.getCellText(riga(corsi, "Corso di Canto"), 2));
        } finally {
            Corso ripristino = corsoService.findById(corsoId);
            ripristino.setSecondoInsegnante(null);
            corsoService.save(ripristino);
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
    void segreteria_modificaNomeECognomeDiUnAllievo() {
        navigate(AllieviView.class);
        var allievi = test((Grid<Allievo>) find(Grid.class).single());
        int riga = -1;
        for (int row = 0; row < allievi.size(); row++) {
            if (allievi.getRow(row).getNomeCompleto().equals("Allievo Canto2")) {
                riga = row;
            }
        }
        Long userId = allievi.getRow(riga).getUser().getId();

        test((Button) allievi.getCellComponent(riga, 6).getChildren().findFirst().orElseThrow()).click();
        test(find(TextField.class).withLabel("Nome").single()).setValue("Giulia");
        test(find(TextField.class).withLabel("Cognome").single()).setValue("Neri");
        test(find(Button.class).withText("Salva").single()).click();
        try {
            assertEquals("Giulia Neri", userService.findById(userId).getNomeCompleto());
            assertTrue(IntStream.range(0, allievi.size()).anyMatch(row -> allievi.getCellText(row, 0).equals("Giulia Neri")));
        } finally {
            User user = userService.findById(userId);
            user.setNome("Allievo");
            user.setCognome("Canto2");
            userService.save(user, null);
        }
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_creaAllievoSenzaCodiceFiscaleNeDataDiNascita() {
        navigate(AllieviView.class);
        test(find(Button.class).withText("Nuovo Allievo").single()).click();
        test(find(TextField.class).withLabel("Username").single()).setValue("allievo_senza_dati");
        test(find(PasswordField.class).withLabel("Password").single()).setValue("segreta");
        test(find(TextField.class).withLabel("Nome").single()).setValue("Sara");
        test(find(TextField.class).withLabel("Cognome").single()).setValue("Neri");
        test(find(ComboBox.class).withLabel("Genitore").single()).selectItem("Genitore1 Cognome1");
        assertEquals("Necessario per ricevute e conferme", find(TextField.class).withLabel("Codice fiscale").single().getHelperText());
        test(find(Button.class).withText("Salva").single()).click();

        Allievo nuovo = allievoService.findByUsername("allievo_senza_dati").orElseThrow();
        try {
            assertNull(nuovo.getCodiceFiscale());
            assertNull(nuovo.getDataNascita());
            assertEquals(List.of("codice fiscale", "data di nascita"), nuovo.getDatiMancantiPerDocumenti());
            // In elenco il badge "Dati mancanti" accanto al nome
            var allievi = test((Grid<Allievo>) find(Grid.class).single());
            int riga = IntStream.range(0, allievi.size()).filter(r -> allievi.getRow(r).equals(nuovo)).findFirst().orElseThrow();
            Component cella = allievi.getCellComponent(riga, 0);
            assertEquals("Dati mancanti", cella.getChildren().filter(Badge.class::isInstance).map(b -> ((Badge) b).getText())
                .findFirst().orElseThrow());
        } finally {
            allievoService.delete(nuovo);
            userService.delete(nuovo.getUser());
        }
    }

    @Test
    @WithUserDetails("segreteria")
    void segreteria_creaAllievoSenzaUsername_utenteGeneratoEDisattivato() {
        navigate(AllieviView.class);
        test(find(Button.class).withText("Nuovo Allievo").single()).click();
        test(find(TextField.class).withLabel("Nome").single()).setValue("Nicolò");
        test(find(TextField.class).withLabel("Cognome").single()).setValue("D'Amico");
        test(find(ComboBox.class).withLabel("Genitore").single()).selectItem("Genitore1 Cognome1");
        test(find(Button.class).withText("Salva").single()).click();

        Allievo nuovo = allievoService.findByUsername("nicolo.damico").orElseThrow();
        try {
            assertFalse(nuovo.getUser().isEnabled());
            assertTrue(nuovo.getUser().getRoles().contains("ALLIEVO"));
            // Omonimo: username con un numero
            assertEquals("nicolo.damico2", userService.usernameLibero("Nicolo", "Damico"));
        } finally {
            allievoService.delete(nuovo);
            userService.delete(nuovo.getUser());
        }
    }

    @Test
    @WithUserDetails("segreteria")
    void segreteria_conUsernameLaPasswordEObbligatoria() {
        navigate(AllieviView.class);
        test(find(Button.class).withText("Nuovo Allievo").single()).click();
        test(find(TextField.class).withLabel("Username").single()).setValue("allievo_senza_password");
        test(find(TextField.class).withLabel("Nome").single()).setValue("Lia");
        test(find(TextField.class).withLabel("Cognome").single()).setValue("Blu");
        test(find(ComboBox.class).withLabel("Genitore").single()).selectItem("Genitore1 Cognome1");
        test(find(Button.class).withText("Salva").single()).click();
        assertTrue(find(PasswordField.class).withLabel("Password").single().isInvalid());
        assertFalse(userService.existsByUsername("allievo_senza_password"));
    }

    @Test
    @WithUserDetails("segreteria")
    void segreteria_creaAllievoAssegnatoAUnGenitore() {
        navigate(AllieviView.class);
        var allievi = test(find(Grid.class).single());
        test(find(Button.class).withText("Nuovo Allievo").single()).click();
        test(find(TextField.class).withLabel("Username").single()).setValue("allievo_nuovo");
        test(find(PasswordField.class).withLabel("Password").single()).setValue("segreta");
        test(find(TextField.class).withLabel("Nome").single()).setValue("Luca");
        test(find(TextField.class).withLabel("Cognome").single()).setValue("Gialli");
        TextField codiceFiscale = find(TextField.class).withLabel("Codice fiscale").single();
        test(codiceFiscale).setValue("GLLLCU12A01H501Z");
        test(find(DatePicker.class).withLabel("Data di nascita").single()).setValue(LocalDate.of(2012, 1, 1));

        // Senza genitore l'allievo (e il suo utente) non viene creato
        test(find(Button.class).withText("Salva").single()).click();
        assertTrue(find(ComboBox.class).withLabel("Genitore").single().isInvalid());
        // Carattere di controllo del codice fiscale sbagliato
        assertTrue(codiceFiscale.isInvalid());
        assertFalse(userService.existsByUsername("allievo_nuovo"));

        test(find(ComboBox.class).withLabel("Genitore").single()).selectItem("Genitore1 Cognome1");
        test(codiceFiscale).setValue("glllcu12a01h501l");
        test(find(Button.class).withText("Salva").single()).click();
        Allievo nuovo = allievoService.findByUsername("allievo_nuovo").orElseThrow();
        try {
            assertEquals(14, allievi.size());
            assertEquals("Genitore1 Cognome1", nuovo.getGenitore().getNomeCompleto());
            assertEquals("GLLLCU12A01H501L", nuovo.getCodiceFiscale());
            assertTrue(nuovo.getUser().getRoles().contains("ALLIEVO"));
        } finally {
            allievoService.delete(nuovo);
            userService.delete(nuovo.getUser());
        }
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_annoScolasticoDisattivatoNonVienePropostoNeScelto() {
        AnnoScolastico anno = annoScolasticoService.crea("2045/2046");
        annoScolasticoService.impostaAttivo(anno.getId(), false);
        try {
            navigate(CorsiView.class);
            test(find(Button.class).withText("Nuovo Corso").single()).click();
            ComboBox<String> annoScolastico = find(ComboBox.class).withLabel("Anno Scolastico").single();
            test(find(DatePicker.class).withLabel("Data Inizio").single()).setValue(LocalDate.of(2045, 10, 2));
            assertTrue(annoScolastico.isEmpty());
            assertFalse(test(annoScolastico).getSuggestions().contains("2045/2046"));
        } finally {
            annoScolasticoService.elimina(anno.getId());
        }
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_impostaPrezzoConPreiscrizione() {
        navigate(CorsiView.class);
        var corsi = test((Grid<Corso>) find(Grid.class).single());
        int canto = riga(corsi, "Corso di Canto");
        Long corsoId = corsi.getRow(canto).getId();

        test(pulsante(corsi.getCellComponent(canto, AZIONI_CORSO), "Modifica")).click();
        BigDecimalField prezzo = find(BigDecimalField.class).withLabel("Prezzo con pre-iscrizione").single();
        assertEquals("Di solito 510,00 € (sconto 15%)", prezzo.getHelperText());
        test(prezzo).setValue(new BigDecimal("700"));
        test(find(Button.class).withText("Salva").single()).click();
        assertTrue(prezzo.isInvalid());
        assertNull(corsoService.findById(corsoId).getPrezzoPreiscrizione());

        test(prezzo).setValue(new BigDecimal("500"));
        test(find(Button.class).withText("Salva").single()).click();
        try {
            assertEquals(0, new BigDecimal("500").compareTo(corsoService.findById(corsoId).getPrezzoPreiscrizione()));
            // Solo informativo: la quota dovuta resta il costo totale
            Long allievoId = corsoService.findPartecipanti(corsoId).getFirst().getAllievo().getId();
            assertEquals(0, new BigDecimal("600").compareTo(pagamentoService.situazione(corsoId, allievoId).quota()));
        } finally {
            Corso corso = corsoService.findById(corsoId);
            corso.setPrezzoPreiscrizione(null);
            corsoService.save(corso);
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

            Long allievo = allievoService.findByUsername("allievo_canto1").orElseThrow().getId();
            assertThrows(IllegalStateException.class, () -> corsoService.iscrivi(chitarra.getId(), allievo));
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
            apriLezioni();
            SegmentedControl<FiltroLezioni> filtro = find(SegmentedControl.class).single();
            test(filtro.getPulsante(FiltroLezioni.TUTTE)).click();
            assertEquals(0, test(find(Grid.class).single()).size());

            navigate(HomeView.class);
            assertEquals(List.of("Corsi assegnati: 0", "Lezioni fatte: 0", "Lezioni da fare: 0", "Lezioni non effettuate: 0"), indicatori());

            Presenza presenza = presenzaService.findByCorsoAndAllievo(chitarra.getId(),
                allievoService.findByUsername("allievo_chitarra1").orElseThrow().getId()).getFirst();
            presenza.setStato(StatoPresenza.ASSENTE_GIUSTIFICATA);
            assertThrows(IllegalStateException.class, () -> presenzaService.save(presenza));
        } finally {
            riattiva(chitarra);
        }
    }

    @Test
    @WithUserDetails("allievo_chitarra1")
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

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_impostaQuotaPersonalizzata() {
        navigate(CorsiView.class);
        var corsi = test((Grid<Corso>) find(Grid.class).single());
        int canto = riga(corsi, "Corso di Canto");
        Long corsoId = corsi.getRow(canto).getId();
        Long allievoId = allievoService.findByUsername("allievo_canto2").orElseThrow().getId();
        test(pulsante(corsi.getCellComponent(canto, AZIONI_CORSO), "Partecipanti")).click();
        var partecipanti = test((Grid<Iscrizione>) find(Grid.class).from(find(PartecipantiDialog.class).single()).single());
        int riga = IntStream.range(0, partecipanti.size())
            .filter(r -> partecipanti.getCellText(r, 1).equals("Allievo Canto2")).findFirst().orElseThrow();
        test(pulsante(partecipanti.getCellComponent(riga, 3), "Quota e pagamenti di Allievo Canto2")).click();

        QuotaPagamentiDialog dialog = find(QuotaPagamentiDialog.class).single();
        try {
            new TextFieldTester<>(find(BigDecimalField.class).from(dialog).withLabel("Quota personalizzata").single())
                .setValue(new BigDecimal("450"));
            test(find(TextField.class).from(dialog).withLabel("Motivo").single()).setValue("fratelli");

            Iscrizione iscrizione = pagamentoService.findIscrizione(corsoId, allievoId);
            assertEquals(0, new BigDecimal("450").compareTo(iscrizione.getQuotaPersonalizzata()));
            assertEquals("fratelli", iscrizione.getNotaQuota());
            assertTrue(find(Span.class).from(dialog).withText("Da pagare 450,00 €").exists());
        } finally {
            pagamentoService.impostaQuota(corsoId, allievoId, null, null);
        }
    }

    @Test
    @WithUserDetails("segreteria")
    void segreteria_vedePrezzoConPreiscrizioneInQuotaEPagamenti() {
        Corso canto = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow();
        Allievo allievo = allievoService.findByUsername("allievo_canto3").orElseThrow();
        new QuotaPagamentiDialog(pagamentoService, canto, allievo).open();
        assertTrue(find(Span.class).withText("Corso di Canto · quota piena 600,00 €").exists());
        find(QuotaPagamentiDialog.class).single().close();

        canto.setPrezzoPreiscrizione(new BigDecimal("510"));
        new QuotaPagamentiDialog(pagamentoService, canto, allievo).open();
        assertTrue(find(Span.class).withText("Corso di Canto · quota piena 600,00 € · pre-iscrizione 510,00 €").exists());
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_registraModificaEdEliminaUnaRata() {
        Corso canto = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow();
        Allievo allievo = allievoService.findByUsername("allievo_canto3").orElseThrow();
        new QuotaPagamentiDialog(pagamentoService, canto, allievo).open();
        QuotaPagamentiDialog dialog = find(QuotaPagamentiDialog.class).single();
        var rate = test((Grid<Pagamento>) find(Grid.class).from(dialog).all().stream()
            .filter(g -> !(g instanceof GrigliaInvii)).findFirst().orElseThrow());
        try {
            // Nuova rata: data di oggi, importo proposto = residuo, metodo di default Bonifico
            test(find(Button.class).from(dialog).withText("Aggiungi pagamento").single()).click();
            BigDecimalField importo = find(BigDecimalField.class).withLabel("Importo").single();
            assertEquals(0, new BigDecimal("600").compareTo(importo.getValue()));
            SegmentedControl<MetodoPagamento> metodo = find(SegmentedControl.class).single();
            assertEquals(MetodoPagamento.BONIFICO, metodo.getValue());
            new TextFieldTester<>(importo).setValue(new BigDecimal("200"));
            test(metodo.getPulsante(MetodoPagamento.CASSA)).click();
            test(find(Button.class).withText("Salva").single()).click();

            assertEquals(1, rate.size());
            assertEquals("200,00 €", rate.getCellText(0, 1));
            assertEquals("Cassa", rate.getCellText(0, 2));
            assertTrue(find(Span.class).from(dialog).withText("Residuo 400,00 €").exists());

            // Modifica
            test(pulsante(rate.getCellComponent(0, 4), "Modifica pagamento")).click();
            new TextFieldTester<>(find(BigDecimalField.class).withLabel("Importo").single()).setValue(new BigDecimal("250"));
            test(find(Button.class).withText("Salva").single()).click();
            assertEquals("250,00 €", rate.getCellText(0, 1));

            // Eliminazione con conferma
            test(pulsante(rate.getCellComponent(0, 4), "Elimina pagamento")).click();
            test(find(Button.class).withText("Elimina").single()).click();
            assertEquals(0, rate.size());
        } finally {
            pagamentoService.findPagamenti(canto.getId(), allievo.getId()).forEach(pagamentoService::elimina);
        }
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_gestionePagamenti_filtriTotaliEAzioni() {
        Corso canto = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow();
        Allievo allievo = allievoService.findByUsername("allievo_canto3").orElseThrow();
        navigate(GestionePagamentiView.class);
        var righe = test((Grid<RigaPagamento>) find(Grid.class).single());
        assertEquals(pagamentoService.righePagamenti().size(), righe.size());
        assertTrue(find(Span.class).withText(righe.size() + " partecipanti").exists());
        try {
            // Filtro per corso: solo il Corso di Canto, con i totali delle righe filtrate
            ComboBox<Corso> filtroCorso = filtro("Filtra per corso");
            test(filtroCorso).selectItem("Corso di Canto (" + canto.getAnnoScolastico() + ")");
            assertTrue(IntStream.range(0, righe.size()).allMatch(r -> righe.getRow(r).corso().equals(canto)));
            assertTrue(find(Span.class).withText("Da pagare " + Formati.euro(new BigDecimal(600 * righe.size()))).exists());

            // Aggiungi pagamento dalla riga: importo proposto = residuo
            int riga = rigaDi(righe, allievo);
            test(pulsante(righe.getCellComponent(riga, "azioni"), "Aggiungi pagamento")).click();
            BigDecimalField importo = find(BigDecimalField.class).withLabel("Importo").single();
            assertEquals(0, new BigDecimal("600").compareTo(importo.getValue()));
            new TextFieldTester<>(importo).setValue(new BigDecimal("200"));
            test(find(Button.class).withText("Salva").single()).click();
            riga = rigaDi(righe, allievo);
            assertEquals(0, new BigDecimal("200").compareTo(righe.getRow(riga).situazione().pagato()));
            assertEquals("Parziale 33%", ((Badge) righe.getCellComponent(riga, "stato")).getText());

            // Filtro per stato del pagamento e per genitore
            SegmentedControl<ElencoPagamenti.FiltroPagamento> filtroPagamento = find(SegmentedControl.class).all().stream()
                .filter(c -> c.getValue() == ElencoPagamenti.FiltroPagamento.TUTTI).findFirst().orElseThrow();
            test(filtroPagamento.getPulsante(ElencoPagamenti.FiltroPagamento.PARZIALE)).click();
            assertEquals(1, righe.size());
            assertEquals(allievo, righe.getRow(0).allievo());
            test(filtroPagamento.getPulsante(ElencoPagamenti.FiltroPagamento.TUTTI)).click();
            ComboBox<Genitore> filtroGenitore = filtro("Filtra per genitore");
            test(filtroGenitore).selectItem(righe.getRow(rigaDi(righe, allievo)).genitore().getNomeCompleto());
            assertTrue(IntStream.range(0, righe.size())
                .allMatch(r -> righe.getRow(r).genitore().equals(righe.getRow(0).genitore())));

            // Quota personalizzata dal dialog esistente: la riga si aggiorna alla chiusura
            riga = rigaDi(righe, allievo);
            test(pulsante(righe.getCellComponent(riga, "azioni"), "Quota e pagamenti")).click();
            QuotaPagamentiDialog dialog = find(QuotaPagamentiDialog.class).single();
            new TextFieldTester<>(find(BigDecimalField.class).from(dialog).withLabel("Quota personalizzata").single())
                .setValue(new BigDecimal("400"));
            test(find(Button.class).from(dialog).withText("Chiudi").single()).click();
            assertEquals(0, new BigDecimal("200").compareTo(righe.getRow(rigaDi(righe, allievo)).situazione().residuo()));
        } finally {
            pagamentoService.findPagamenti(canto.getId(), allievo.getId()).forEach(pagamentoService::elimina);
            pagamentoService.impostaQuota(canto.getId(), allievo.getId(), null, null);
        }
    }

    @Test
    @WithUserDetails("genitore3")
    @SuppressWarnings("unchecked")
    void genitore_vedeIPagamentiDeiFigliInSolaLettura() {
        Corso canto = disattiva("Corso di Canto");
        Allievo allievo = allievoService.findByUsername("allievo_canto3").orElseThrow();
        Pagamento rata = new Pagamento();
        rata.setData(LocalDate.now());
        rata.setImporto(new BigDecimal("200"));
        pagamentoService.salva(canto.getId(), allievo.getId(), rata);
        try {
            navigate(HomeView.class);
            assertTrue(find(SideNavItem.class).all().stream().anyMatch(i -> "Pagamenti".equals(i.getLabel())));
            navigate(PagamentiFigliView.class);
            var righe = test((Grid<RigaPagamento>) find(Grid.class).single());
            assertTrue(righe.size() > 0);
            assertTrue(IntStream.range(0, righe.size())
                .allMatch(r -> righe.getRow(r).genitore().getUser().getUsername().equals("genitore3")));
            // Anche il corso non attivo, con il dettaglio in sola lettura e la ricevuta
            int riga = rigaDi(righe, allievo);
            assertEquals(canto, righe.getRow(riga).corso());
            assertTrue(find(Span.class).withText("Incassato 200,00 €").exists());
            var azioni = righe.getCellComponent(riga, "azioni");
            assertThrows(AssertionError.class, () -> pulsante(azioni, "Aggiungi pagamento"));
            test(pulsante(azioni, "Pagamenti")).click();
            PagamentiAllievoDialog dialog = find(PagamentiAllievoDialog.class).single();
            assertTrue(find(Span.class).from(dialog).withText("Residuo 400,00 €").exists());
            assertTrue(find(ComboBox.class).all().stream().noneMatch(c -> c.getAriaLabel().filter("Filtra per genitore"::equals).isPresent()
                && c.isVisible()));
        } finally {
            pagamentoService.findPagamenti(canto.getId(), allievo.getId()).forEach(pagamentoService::elimina);
            riattiva(canto);
        }
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_riepilogoIscrittiCorsoPerCorso() {
        Corso canto = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow();
        Allievo allievo = allievoService.findByUsername("allievo_canto3").orElseThrow();
        Pagamento rata = new Pagamento();
        rata.setData(LocalDate.now());
        rata.setImporto(new BigDecimal("600"));
        pagamentoService.salva(canto.getId(), allievo.getId(), rata);
        try {
            navigate(HomeView.class);
            assertTrue(find(SideNavItem.class).all().stream().anyMatch(i -> "Riepilogo Iscritti".equals(i.getLabel())));
            navigate(RiepilogoIscrittiView.class);
            var grid = new com.vaadin.flow.component.treegrid.TreeGridTester<>(
                (com.vaadin.flow.component.treegrid.TreeGrid<RiepilogoIscrittiView.Voce>) find(com.vaadin.flow.component.treegrid.TreeGrid.class).single());

            // Corsi attivi dell'anno più recente, aperti: Canto (7 iscritti) e Chitarra (6)
            assertEquals(15, grid.size());
            assertTrue(grid.getRow(0).isCorso());
            assertTrue(grid.isExpanded(0));
            assertEquals("Corso di Canto", grid.getCellText(0, 0));
            assertEquals("7", grid.getCellText(0, 3));
            assertEquals("4.200,00 €", grid.getCellText(0, 4));
            assertEquals("600,00 €", grid.getCellText(0, 5));
            assertEquals("3.600,00 €", grid.getCellText(0, 6));
            int riga = IntStream.range(1, 8).filter(r -> grid.getRow(r).iscritto().allievo().equals(allievo)).findFirst().orElseThrow();
            assertEquals("600,00 €", grid.getCellText(riga, 5));
            assertEquals("0,00 €", grid.getCellText(riga, 6));
            assertTrue(grid.getRow(8).isCorso());
            assertEquals("Corso di Chitarra", grid.getCellText(8, 0));

            // Comprimi tutti: restano le righe dei corsi
            test(find(Button.class).withText("Comprimi tutti").single()).click();
            assertEquals(2, grid.size());
            grid.expand(1);
            assertEquals(8, grid.size());

            // Corsi non attivi dell'anno: nessuno
            SegmentedControl<CorsiView.FiltroStato> stato = find(SegmentedControl.class).single();
            test(stato.getPulsante(CorsiView.FiltroStato.NON_ATTIVI)).click();
            assertEquals(0, grid.size());
        } finally {
            pagamentoService.findPagamenti(canto.getId(), allievo.getId()).forEach(pagamentoService::elimina);
        }
    }

    @Test
    @WithUserDetails("genitore3")
    void genitore_nonAccedeAlRiepilogoIscritti() {
        navigate("riepilogo-iscritti", RouteNotFoundError.class);
    }

    @Test
    @WithUserDetails("allievo_canto1")
    void allievo_nonAccedeAiPagamenti() {
        navigate("pagamenti-figli", RouteNotFoundError.class);
        navigate("pagamenti", RouteNotFoundError.class);
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_aggiungeEDisattivaAnniScolasticiSenzaElimina() {
        navigate(HomeView.class);
        assertTrue(find(SideNavItem.class).all().stream().anyMatch(v -> v.getLabel().equals("Anni scolastici")));
        navigate(AnniScolasticiView.class);
        var anni = test((Grid<AnnoScolastico>) find(Grid.class).single());
        int quanti = anni.size();
        assertTrue(find(Button.class).all().stream().noneMatch(b -> "Elimina".equals(b.getElement().getAttribute("aria-label"))));

        test(find(Button.class).withText("Nuovo Anno Scolastico").single()).click();
        TextField nome = find(TextField.class).withLabel("Anno Scolastico").single();
        test(nome).setValue("2050/2052");
        test(find(Button.class).withText("Salva").single()).click();
        assertTrue(nome.isInvalid());
        test(nome).setValue("2024/2025");
        test(find(Button.class).withText("Salva").single()).click();
        assertTrue(nome.isInvalid());
        assertEquals("L'anno scolastico 2024/2025 è già presente", nome.getErrorMessage());
        test(nome).setValue("2050/2051");
        test(find(Button.class).withText("Salva").single()).click();

        AnnoScolastico nuovo = annoScolasticoService.elenco().stream()
            .filter(a -> a.getNome().equals("2050/2051")).findFirst().orElseThrow();
        try {
            assertEquals(quanti + 1, anni.size());
            assertEquals("2050/2051", anni.getRow(0).getNome());
            test(pulsante(anni.getCellComponent(0, 3), "Disattiva")).click();
            assertFalse(annoScolasticoService.scelteCorso(null).contains("2050/2051"));
            assertEquals("Non attivo", ((Badge) anni.getCellComponent(0, 1)).getText());
            test(pulsante(anni.getCellComponent(0, 3), "Attiva")).click();
            assertTrue(annoScolasticoService.scelteCorso(null).contains("2050/2051"));
        } finally {
            annoScolasticoService.elimina(nuovo.getId());
        }
    }

    @Test
    @WithUserDetails("admin")
    @SuppressWarnings("unchecked")
    void admin_eliminaSoloAnniScolasticiNonUsati() {
        AnnoScolastico nuovo = annoScolasticoService.crea("2051/2052");
        navigate(AnniScolasticiView.class);
        var anni = test((Grid<AnnoScolastico>) find(Grid.class).single());
        int quanti = anni.size();
        for (int row = 0; row < anni.size(); row++) {
            if (anni.getRow(row).getNome().equals("2024/2025")) {
                assertFalse(pulsante(anni.getCellComponent(row, 3), "Elimina").isEnabled());
                assertEquals("Sì", anni.getCellText(row, 2));
            }
        }
        assertEquals("2051/2052", anni.getRow(0).getNome());
        test(pulsante(anni.getCellComponent(0, 3), "Elimina")).click();
        assertEquals(quanti - 1, anni.size());
        assertFalse(annoScolasticoService.elenco().contains(nuovo));
    }

    @Test
    @WithUserDetails("insegnante1")
    void insegnante_nonAccedeAgliAnniScolastici() {
        navigate("anni-scolastici", RouteNotFoundError.class);
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_aggiungeEDisattivaFestivitaSenzaElimina() {
        navigate(HomeView.class);
        assertTrue(find(SideNavItem.class).all().stream().anyMatch(v -> v.getLabel().equals("Festività")));
        navigate(FestivitaView.class);
        var festivita = test((Grid<Festivita>) find(Grid.class).single());
        int quante = festivita.size();
        assertTrue(find(Button.class).all().stream().noneMatch(b -> "Elimina".equals(b.getElement().getAttribute("aria-label"))));

        // Giorno inesistente: errore sul campo, nulla salvato
        test(find(Button.class).withText("Nuova Festività").single()).click();
        test(find(TextField.class).withLabel("Nome").single()).setValue("Patrono");
        IntegerField giorno = find(IntegerField.class).withLabel("Giorno").single();
        test(giorno).setValue(31);
        test((ComboBox<Integer>) find(ComboBox.class).withLabel("Mese").single()).selectItem("aprile");
        test(find(Button.class).withText("Salva").single()).click();
        assertTrue(giorno.isInvalid());
        assertEquals(quante, festivitaService.elenco().size());

        // Ponte in date precise
        @SuppressWarnings("rawtypes")
        SegmentedControl<Festivita.Tipo> tipo = find(SegmentedControl.class).single();
        test(tipo.getPulsante(Festivita.Tipo.DATE)).click();
        test(find(TextField.class).withLabel("Nome").single()).setValue("Ponte di prova");
        LocalDate ponte = LocalDate.now().plusYears(3);
        test(find(DatePicker.class).withLabel("Dal").single()).setValue(ponte);
        test(find(Button.class).withText("Salva").single()).click();

        Festivita nuova = festivitaService.elenco().stream()
            .filter(f -> f.getNome().equals("Ponte di prova")).findFirst().orElseThrow();
        try {
            assertEquals(quante + 1, festivita.size());
            assertTrue(festivitaService.calendario().festivo(ponte));
            int riga = IntStream.range(0, festivita.size())
                .filter(r -> festivita.getRow(r).getNome().equals("Ponte di prova")).findFirst().orElseThrow();
            test(pulsante(festivita.getCellComponent(riga, 4), "Disattiva")).click();
            assertFalse(festivitaService.calendario().festivo(ponte));
        } finally {
            festivitaService.elimina(nuova.getId());
        }
    }

    @Test
    @WithUserDetails("admin")
    @SuppressWarnings("unchecked")
    void admin_eliminaUnaFestivitaDopoConferma() {
        Festivita ponte = new Festivita();
        ponte.setNome("Da eliminare");
        ponte.setTipo(Festivita.Tipo.DATE);
        ponte.setDataInizio(LocalDate.now().plusYears(4));
        festivitaService.salva(ponte);
        navigate(FestivitaView.class);
        var festivita = test((Grid<Festivita>) find(Grid.class).single());
        int quante = festivita.size();
        int riga = IntStream.range(0, quante)
            .filter(r -> festivita.getRow(r).getNome().equals("Da eliminare")).findFirst().orElseThrow();

        test(pulsante(festivita.getCellComponent(riga, 4), "Elimina")).click();
        test(find(Button.class).withText("Elimina").single()).click();

        assertEquals(quante - 1, festivita.size());
        assertTrue(festivitaService.elenco().stream().noneMatch(f -> f.getNome().equals("Da eliminare")));
    }

    @Test
    @WithAnonymousUser
    void login_mostraNomeESottotitolo() {
        navigate(LoginView.class);
        assertEquals("Area Corsi Mosson", find(H1.class).single().getText());
        assertTrue(find(Paragraph.class).all().stream().anyMatch(p -> p.getText().equals(PaginaPubblica.SOTTOTITOLO)));
    }

    @Test
    @WithUserDetails("admin")
    void titoloScheda_conIlNomeDellApplicazione() {
        // Il suffisso si aggiunge prima della risposta al browser
        navigate(CorsiView.class);
        UI.getCurrent().getInternals().getStateTree().runExecutionsBeforeClientResponse();
        assertEquals("Gestione Corsi | Area Corsi Mosson", UI.getCurrent().getInternals().getTitle());
        navigate(HomeView.class);
        UI.getCurrent().getInternals().getStateTree().runExecutionsBeforeClientResponse();
        assertEquals("Home | Area Corsi Mosson", UI.getCurrent().getInternals().getTitle());
    }

    @Test
    @WithUserDetails("admin")
    void intestazione_mostraIlNomeDellApplicazione() {
        navigate(HomeView.class);
        assertTrue(find(H1.class).all().stream().anyMatch(h -> h.getText().equals("Area Corsi Mosson")));
    }

    @Test
    void login_inItaliano() {
        navigate(LoginView.class);
        assertTrue(find(LoginForm.class).single().isForgotPasswordButtonVisible());
        LoginI18n i18n = LoginView.italiano();
        assertEquals("Nome utente", i18n.getForm().getUsername());
        assertEquals("Accedi", i18n.getForm().getSubmit());
        assertEquals("Password dimenticata?", i18n.getForm().getForgotPassword());
        assertEquals("Nome utente o password errati", i18n.getErrorMessage().getTitle());
    }

    @Test
    @WithUserDetails("insegnante1")
    void insegnante_nonAccedeAlleFestivita() {
        navigate("festivita", RouteNotFoundError.class);
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_registraRimborsiSenzaEliminarli() {
        Insegnante insegnante = insegnanteService.findByUsername("insegnante1").orElseThrow();
        navigate(HomeView.class);
        assertTrue(find(SideNavItem.class).all().stream().anyMatch(v -> v.getLabel().equals("Rimborsi")));
        navigate(RimborsiView.class);
        ComboBox<String> anno = filtro("Filtra per anno scolastico");
        assertEquals(AnnoScolastico.di(LocalDate.now()), anno.getValue());
        test(anno).selectItem("2024/2025");
        var grid = test((Grid<BilancioService.RigaRimborsi>) find(Grid.class).single());
        int riga = IntStream.range(0, grid.size()).filter(r -> grid.getRow(r).insegnante().getId().equals(insegnante.getId()))
            .findFirst().orElseThrow();
        assertEquals("25,00 €", grid.getCellText(riga, 1));
        test(pulsante(grid.getCellComponent(riga, 5), "Rimborsi")).click();

        RimborsiDialog dialog = find(RimborsiDialog.class).single();
        test(find(Button.class).from(dialog).withText("Aggiungi rimborso").single()).click();
        // Mese di riferimento tra quelli dell'anno scolastico; nessuna lezione completata: nessun importo proposto
        ComboBox<java.time.YearMonth> mese = find(ComboBox.class).withLabel("Mese di riferimento").single();
        assertEquals(java.time.YearMonth.of(2024, 9), mese.getValue());
        BigDecimalField importo = find(BigDecimalField.class).withLabel("Importo").single();
        assertNull(importo.getValue());
        test(find(Button.class).withText("Salva").single()).click();
        assertTrue(importo.isInvalid());
        test(mese).selectItem("ottobre 2024");
        test(importo).setValue(new BigDecimal("80"));
        test(find(Button.class).withText("Salva").single()).click();
        try {
            assertTrue(find(Span.class).from(dialog).withText("Totale rimborsato: 80,00 €").exists());
            var rimborsi = test((Grid<Rimborso>) find(Grid.class).from(dialog).single());
            assertEquals("ottobre 2024", rimborsi.getCellText(0, 0));
            assertFalse(find(Button.class).from(dialog).all().stream()
                .anyMatch(b -> "Elimina rimborso".equals(b.getElement().getAttribute("aria-label"))));

            test(pulsante(rimborsi.getCellComponent(0, 5), "Modifica rimborso")).click();
            test(find(BigDecimalField.class).withLabel("Importo").single()).setValue(new BigDecimal("90"));
            test(find(Button.class).withText("Salva").single()).click();
            assertEquals(0, new BigDecimal("90").compareTo(rimborsoService.totale(insegnante.getId(), "2024/2025")));
            // Il prospetto si aggiorna
            assertEquals("90,00 €", grid.getCellText(riga, 2));
        } finally {
            rimborsoService.rimborsi(insegnante.getId(), "2024/2025").forEach(r -> rimborsoService.elimina(r.getId()));
        }
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_vedeIlBilancioDellInsegnante() {
        Insegnante insegnante = insegnanteService.findByUsername("insegnante1").orElseThrow();
        navigate(RimborsiView.class);
        test(this.<String>filtro("Filtra per anno scolastico")).selectItem("2024/2025");
        var grid = test((Grid<BilancioService.RigaRimborsi>) find(Grid.class).single());
        int riga = IntStream.range(0, grid.size()).filter(r -> grid.getRow(r).insegnante().getId().equals(insegnante.getId()))
            .findFirst().orElseThrow();
        test(pulsante(grid.getCellComponent(riga, 5), "Bilancio")).click();

        BilancioDialog dialog = find(BilancioDialog.class).single();
        List<String> riquadri = find(Card.class).from(dialog).all().stream().map(c -> c.getAriaLabel().orElse("")).toList();
        // Canto: 7 allievi × 600 € di quota; 30 lezioni × 7 allievi × 40 minuti a 25 €/h
        assertTrue(riquadri.contains("Incasso previsto: 4.200,00 € (quote assegnate)"), riquadri.toString());
        assertTrue(riquadri.contains("Costo previsto: 3.500,00 € (lezioni programmate)"), riquadri.toString());
        assertTrue(riquadri.contains("Margine previsto: 700,00 € (incasso − costo previsti)"), riquadri.toString());
        var corsi = test((Grid<BilancioService.BilancioCorso>) find(Grid.class).from(dialog).single());
        assertEquals(1, corsi.size());
        assertEquals("Corso di Canto", corsi.getCellText(0, 0));
        assertEquals("7", corsi.getCellText(0, 1));
        assertEquals("3.500,00 €", corsi.getCellText(0, 5));
    }

    @Test
    @WithUserDetails("admin")
    @SuppressWarnings("unchecked")
    void admin_eliminaUnRimborso() {
        Insegnante insegnante = insegnanteService.findByUsername("insegnante2").orElseThrow();
        Rimborso rimborso = new Rimborso();
        rimborso.setData(LocalDate.now());
        rimborso.setMese(java.time.YearMonth.from(LocalDate.now()));
        rimborso.setImporto(new BigDecimal("50"));
        rimborso.setMetodo(MetodoPagamento.CASSA);
        rimborsoService.salva(insegnante.getId(), rimborso);
        navigate(RimborsiView.class);
        var grid = test((Grid<BilancioService.RigaRimborsi>) find(Grid.class).single());
        int riga = IntStream.range(0, grid.size()).filter(r -> grid.getRow(r).insegnante().getId().equals(insegnante.getId()))
            .findFirst().orElseThrow();
        test(pulsante(grid.getCellComponent(riga, 5), "Rimborsi")).click();
        RimborsiDialog dialog = find(RimborsiDialog.class).single();
        var rimborsi = test((Grid<Rimborso>) find(Grid.class).from(dialog).single());
        test(pulsante(rimborsi.getCellComponent(0, 5), "Elimina rimborso")).click();
        test(find(Button.class).withText("Elimina").single()).click();
        assertEquals(0, rimborsi.size());
        assertEquals(0, rimborsoService.totale(insegnante.getId(), AnnoScolastico.di(LocalDate.now())).signum());
    }

    @Test
    @WithUserDetails("insegnante1")
    @SuppressWarnings("unchecked")
    void insegnante_vedeIPropriRimborsiInLettura() {
        Insegnante insegnante = insegnanteService.findByUsername("insegnante1").orElseThrow();
        Insegnante altro = insegnanteService.findByUsername("insegnante2").orElseThrow();
        java.time.YearMonth mese = java.time.YearMonth.from(LocalDate.now());
        Rimborso mio = new Rimborso();
        mio.setData(LocalDate.now());
        mio.setMese(mese);
        mio.setImporto(new BigDecimal("120"));
        mio.setMetodo(MetodoPagamento.BONIFICO);
        rimborsoService.salva(insegnante.getId(), mio);
        Rimborso suo = new Rimborso();
        suo.setData(LocalDate.now());
        suo.setMese(mese);
        suo.setImporto(new BigDecimal("300"));
        suo.setMetodo(MetodoPagamento.BONIFICO);
        rimborsoService.salva(altro.getId(), suo);
        try {
            navigate(HomeView.class);
            assertTrue(find(SideNavItem.class).all().stream().anyMatch(v -> v.getLabel().equals("I miei rimborsi")));
            navigate(MieiRimborsiView.class);
            var grid = test((Grid<Rimborso>) find(Grid.class).single());
            assertEquals(1, grid.size());
            assertEquals(Formati.mese(mese), grid.getCellText(0, 0));
            assertTrue(find(Span.class).withText("Totale ricevuto: 120,00 €").exists());
            assertFalse(find(Button.class).all().stream().anyMatch(b -> b.getText().contains("rimborso")));
        } finally {
            rimborsoService.elimina(mio.getId());
            rimborsoService.elimina(suo.getId());
        }
    }

    @Test
    @WithUserDetails("insegnante1")
    void insegnante_nonAccedeAiRimborsiDiTutti() {
        navigate("rimborsi", RouteNotFoundError.class);
    }

    @SuppressWarnings("unchecked")
    private <T> ComboBox<T> filtro(String ariaLabel) {
        return find(ComboBox.class).all().stream()
            .filter(c -> c.getAriaLabel().filter(ariaLabel::equals).isPresent()).findFirst().orElseThrow();
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_inviaConfermaDaGestionePagamenti_ricevutaSoloConPagamenti() {
        Allievo allievo = allievoService.findByUsername("allievo_canto3").orElseThrow();
        navigate(GestionePagamentiView.class);
        var righe = test((Grid<RigaPagamento>) find(Grid.class).single());
        try {
            test(pulsante(righe.getCellComponent(rigaDi(righe, allievo), "azioni"), "Invia email")).click();
            // Destinatario: il genitore
            assertTrue(find(Span.class).all().stream()
                .anyMatch(sp -> sp.getText().startsWith("A: ") && sp.getText().endsWith("<genitore3@example.com>")));
            assertTrue(find(Span.class).withText("Server email non configurato: l'invio sarà solo simulato e registrato.").exists());
            assertFalse(find(Button.class).withText("Invia ricevuta").single().isEnabled());
            test(find(Button.class).withText("Invia conferma di iscrizione").single()).click();

            List<InvioEmail> invii = emailService.tuttiGliInvii();
            assertEquals(1, invii.size());
            assertEquals(InvioEmail.Esito.SIMULATA, invii.getFirst().getEsito());
            assertEquals("segreteria", invii.getFirst().getInviatoDa());
            assertFalse(find(Button.class).withText("Invia conferma di iscrizione").exists());
        } finally {
            invioEmailRepository.deleteAll();
        }
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_inviaLeConfermeATuttiGliIscritti() {
        navigate(CorsiView.class);
        var corsi = test((Grid<Corso>) find(Grid.class).single());
        test(pulsante(corsi.getCellComponent(riga(corsi, "Corso di Canto"), AZIONI_CORSO), "Partecipanti")).click();
        try {
            test(find(Button.class).withText("Invia conferme").single()).click();
            test(find(Button.class).withText("Invia").single()).click();
            assertEquals(7, emailService.tuttiGliInvii().size());
        } finally {
            invioEmailRepository.deleteAll();
        }
    }

    @Test
    @WithUserDetails("admin")
    @SuppressWarnings("unchecked")
    void admin_vedeIlRegistroDegliInvii() {
        Corso canto = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow();
        Allievo allievo = allievoService.findByUsername("allievo_canto3").orElseThrow();
        emailService.inviaConferme(canto.getId(), "segreteria");
        try {
            // Nel dialog Quota e pagamenti: solo gli invii del partecipante
            new QuotaPagamentiDialog(pagamentoService, canto, allievo).open();
            var inviiPartecipante = test((Grid<InvioEmail>) find(GrigliaInvii.class).single());
            assertEquals(1, inviiPartecipante.size());
            assertEquals("Conferma di iscrizione", inviiPartecipante.getCellText(0, 1));
            assertEquals("Simulata", ((Badge) inviiPartecipante.getCellComponent(0, 3)).getText());
            find(QuotaPagamentiDialog.class).single().close();

            navigate(HomeView.class);
            assertTrue(find(SideNavItem.class).all().stream().anyMatch(v -> v.getLabel().equals("Email inviate")));
            navigate(EmailInviateView.class);
            var registro = test((Grid<InvioEmail>) find(GrigliaInvii.class).single());
            assertEquals(7, registro.size());
            test(find(TextField.class).all().stream().filter(t -> t.getAriaLabel().filter("Cerca negli invii"::equals).isPresent())
                .findFirst().orElseThrow()).setValue("canto3");
            assertEquals(1, registro.size());
            assertEquals("Allievo Canto3", registro.getCellText(0, 3));
            assertEquals("segreteria", registro.getCellText(0, 5));
            SegmentedControl<Optional<TipoDocumento>> tipo = find(SegmentedControl.class).single();
            test(tipo.getPulsante(Optional.of(TipoDocumento.RICEVUTA))).click();
            assertEquals(0, registro.size());
        } finally {
            invioEmailRepository.deleteAll();
        }
    }

    @Test
    @WithUserDetails("segreteria")
    void segreteria_senzaDatiAnagraficiNonInviaDocumenti() {
        Corso canto = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow();
        Allievo allievo = allievoService.findByUsername("allievo_canto3").orElseThrow();
        LocalDate dataNascita = allievo.getDataNascita();
        allievo.setDataNascita(null);
        allievoService.save(allievo);
        try {
            InvioEmailDialog.apri(canto, allievo, () -> { });
            assertFalse(find(Button.class).withText("Invia conferma di iscrizione").single().isEnabled());
            assertTrue(find(Span.class).all().stream().anyMatch(s -> s.getText().startsWith("Manca data di nascita di Allievo Canto3")));
        } finally {
            allievo.setDataNascita(dataNascita);
            allievoService.save(allievo);
        }
    }

    @Test
    @WithUserDetails("admin")
    @SuppressWarnings("unchecked")
    void admin_inviaEmailDiTestConIlTemplateScelto() {
        navigate(EmailInviateView.class);
        test(find(Button.class).withText("Email di test").single()).click();
        assertTrue(find(Span.class).all().stream().anyMatch(s -> s.getText().startsWith("Server email non configurato")));

        // Senza destinatario valido non invia
        test(find(Button.class).withText("Invia").single()).click();
        assertTrue(find(EmailField.class).withLabel("Destinatario").single().isInvalid());

        test(find(EmailField.class).withLabel("Destinatario").single()).setValue("prova@example.org");
        EmailDiTestDialog dialog = find(EmailDiTestDialog.class).single();
        SegmentedControl<Optional<TipoDocumento>> template = find(SegmentedControl.class, dialog).single();
        test(template.getPulsante(Optional.of(TipoDocumento.RICEVUTA))).click();
        test(find(Button.class).withText("Invia").single()).click();
        assertEquals("Simulata", find(Badge.class, dialog).single().getText());
        assertTrue(dialog.isOpened());
        // Il test non finisce nel registro degli invii
        assertTrue(emailService.tuttiGliInvii().isEmpty());
    }

    @Test
    @WithUserDetails("segreteria")
    void segreteria_nonVedeLEmailDiTest() {
        navigate(EmailInviateView.class);
        assertTrue(find(Button.class).withText("Email di test").all().isEmpty());
    }

    @Test
    @WithUserDetails("insegnante1")
    void insegnante_nonAccedeAlRegistroEmail() {
        navigate("email-inviate", RouteNotFoundError.class);
    }

    private static int rigaDi(GridTester<Grid<RigaPagamento>, RigaPagamento> righe, Allievo allievo) {
        return IntStream.range(0, righe.size()).filter(r -> righe.getRow(r).allievo().equals(allievo)).findFirst().orElseThrow();
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
        test(nuovo).selectItem("Allievo Canto1");
        test(find(Button.class).from(dialog).withText("Aggiungi").single()).click();
        assertEquals(7, partecipanti.size());
        Long allievoId = corsoService.findPartecipanti(corso.getId()).stream()
            .filter(i -> i.getAllievo().getNomeCompleto().equals("Allievo Canto1"))
            .findFirst().orElseThrow().getAllievo().getId();
        assertEquals(30, presenzaService.findByCorsoAndAllievo(corso.getId(), allievoId).size());

        // Rimozione: sparisce dall'elenco con le presenze delle lezioni da fare
        int riga = -1;
        for (int row = 0; row < partecipanti.size(); row++) {
            if (partecipanti.getCellText(row, 1).equals("Allievo Canto1")) {
                riga = row;
            }
        }
        test(pulsante(partecipanti.getCellComponent(riga, 3), "Rimuovi Allievo Canto1")).click();
        assertEquals(6, partecipanti.size());
        assertTrue(presenzaService.findByCorsoAndAllievo(corso.getId(), allievoId).isEmpty());
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
    void admin_allievi_grigliaConNumeroCorsi() {
        navigate(AllieviView.class);

        var grid = test(find(Grid.class).single());
        assertEquals(13, grid.size());
        for (int row = 0; row < grid.size(); row++) {
            assertEquals("1", grid.getCellText(row, 5));
            grid.getCellText(row, 3);
            // Nessuno allievo di esempio ha un indirizzo proprio: si mostra quello del genitore
            assertTrue(grid.getCellText(row, 4).matches("Via Roma \\d, 00184 Roma \\(RM\\) \\(del genitore\\)"),
                grid.getCellText(row, 4));
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
    void segreteria_vedeCorsiEAllieviSenzaElimina() {
        navigate(CorsiView.class);
        var corsi = test((Grid<Corso>) find(Grid.class).single());
        assertEquals(2, corsi.size());
        assertAzioni(corsi.getCellComponent(0, AZIONI_CORSO), "Modifica", "Partecipanti", "Genera Lezioni");

        navigate(AllieviView.class);
        var allievi = test(find(Grid.class).single());
        assertEquals(13, allievi.size());
        assertAzioni(allievi.getCellComponent(0, 6), "Modifica");
    }

    @Test
    @WithUserDetails("segreteria")
    void segreteria_vedeGliInsegnanti() {
        navigate(InsegnantiView.class);

        var grid = test(find(Grid.class).single());
        assertEquals(2, grid.size());
        for (int row = 0; row < grid.size(); row++) {
            assertEquals("1", grid.getCellText(row, 4));
        }
        assertAzioni(grid.getCellComponent(0, 5), "Modifica");
    }

    @Test
    @WithUserDetails("segreteria")
    void segreteria_creaUnInsegnanteConAnagraficaETariffa() {
        navigate(InsegnantiView.class);
        test(find(Button.class).withText("Nuovo Insegnante").single()).click();
        Dialog dialog = find(Dialog.class).single();
        test(find(TextField.class).from(dialog).withLabel("Username").single()).setValue("insegnante_test");
        test(find(PasswordField.class).from(dialog).withLabel("Password").single()).setValue("segreta");
        test(find(TextField.class).from(dialog).withLabel("Nome").single()).setValue("Anna");
        test(find(TextField.class).from(dialog).withLabel("Cognome").single()).setValue("Verdi");
        new TextFieldTester<>(find(BigDecimalField.class).from(dialog).withLabel("Tariffa oraria").single())
            .setValue(new BigDecimal("27.50"));
        test(find(TextField.class).from(dialog).withLabel("Provincia").single()).setValue("rm");
        TextField codiceFiscale = find(TextField.class).from(dialog).withLabel("Codice fiscale").single();
        test(codiceFiscale).setValue("RSSMRA85T10A562T");
        Button salva = find(Button.class).from(dialog).withText("Salva").single();
        try {
            // Carattere di controllo sbagliato: il dialog resta aperto con l'errore sul campo
            test(salva).click();
            assertTrue(codiceFiscale.isInvalid());
            assertTrue(dialog.isOpened());

            test(codiceFiscale).setValue("rssmra85t10a562s");
            test(salva).click();
            assertFalse(dialog.isOpened());

            Insegnante insegnante = insegnanteService.findByUsername("insegnante_test").orElseThrow();
            assertEquals("RSSMRA85T10A562S", insegnante.getCodiceFiscale());
            assertEquals("RM", insegnante.getIndirizzo().getProvincia());
            assertEquals(0, new BigDecimal("27.50").compareTo(insegnante.getTariffaOraria()));
            assertTrue(insegnante.getUser().getRoles().contains("INSEGNANTE"));
            assertEquals(3, test(find(Grid.class).single()).size());
        } finally {
            insegnanteService.findByUsername("insegnante_test").ifPresent(i -> {
                insegnanteRepository.delete(i);
                userService.delete(i.getUser());
            });
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
        assertEquals("Allievo Canto3, Allievo Chitarra1", grid.getCellText(genitore3, 3));
        assertEquals("2", grid.getCellText(genitore3, 4));
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_modificaIDatiDiUnGenitore() {
        navigate(GenitoriView.class);
        var grid = test((Grid<Genitore>) find(Grid.class).single());
        int genitore3 = IntStream.range(0, grid.size())
            .filter(r -> grid.getRow(r).getUser().getUsername().equals("genitore3")).findFirst().orElseThrow();
        test(pulsante(grid.getCellComponent(genitore3, 5), "Modifica")).click();

        Dialog dialog = find(Dialog.class).single();
        assertTrue(find(TextField.class).from(dialog).withLabel("Username").single().isReadOnly());
        test(find(TextField.class).from(dialog).withLabel("Telefono").single()).setValue("06 1234567");
        test(find(TextField.class).from(dialog).withLabel("Luogo di nascita").single()).setValue("Napoli");
        test(find(Button.class).from(dialog).withText("Salva").single()).click();
        assertFalse(dialog.isOpened());

        Genitore genitore = genitoreService.findByUsername("genitore3").orElseThrow();
        assertEquals("06 1234567", genitore.getUser().getTelefono());
        assertEquals("Napoli", genitore.getLuogoNascita());
        assertEquals("Via Roma 3", genitore.getIndirizzo().getVia());
        assertTrue(genitore.getUser().getRoles().contains("GENITORE"));
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_scaricaIlRiepilogoDellaFamiglia() {
        navigate(GenitoriView.class);
        var grid = test((Grid<Genitore>) find(Grid.class).single());
        int genitore3 = IntStream.range(0, grid.size())
            .filter(r -> grid.getRow(r).getUser().getUsername().equals("genitore3")).findFirst().orElseThrow();
        test(pulsante(grid.getCellComponent(genitore3, 5), "Riepilogo famiglia")).click();

        Dialog dialog = find(Dialog.class).single();
        assertEquals("2024/2025", find(ComboBox.class).from(dialog).withLabel("Anno scolastico").single().getValue());
        com.vaadin.flow.component.html.Anchor link = find(com.vaadin.flow.component.html.Anchor.class).from(dialog).single();
        assertFalse(link.getHref().isBlank());
        assertTrue(find(Button.class).from(dialog).withText("Scarica PDF").exists());
        test(find(Button.class).from(dialog).withText("Chiudi").single()).click();

        // Genitore senza figli iscritti: solo il messaggio
        int genitore9 = IntStream.range(0, grid.size())
            .filter(r -> grid.getRow(r).getUser().getUsername().equals("genitore9")).findFirst().orElseThrow();
        test(pulsante(grid.getCellComponent(genitore9, 5), "Riepilogo famiglia")).click();
        Dialog vuoto = find(Dialog.class).single();
        assertTrue(find(Paragraph.class).from(vuoto).all().stream().anyMatch(p -> p.getText().contains("è iscritto a un corso")));
        assertFalse(find(Button.class).from(vuoto).withText("Scarica PDF").exists());
    }

    @Test
    @WithUserDetails("genitore3")
    void genitore_scaricaIlProprioRiepilogoDallaPaginaPagamenti() {
        navigate(PagamentiFigliView.class);
        test(find(Button.class).withText("Riepilogo famiglia").single()).click();
        Dialog dialog = find(Dialog.class).single();
        assertEquals("2024/2025", find(ComboBox.class).from(dialog).withLabel("Anno scolastico").single().getValue());
        assertTrue(find(Paragraph.class).from(dialog).all().stream().anyMatch(p -> p.getText().contains("Genitore3 Cognome3")));
        assertFalse(find(com.vaadin.flow.component.html.Anchor.class).from(dialog).single().getHref().isBlank());
    }

    private static void assertAzioni(Component azioni, String... etichette) {
        assertEquals(List.of(etichette), azioni.getChildren()
            .map(c -> c.getElement().getAttribute("aria-label")).toList());
    }

    @Test
    @WithUserDetails("insegnante1")
    void insegnante_vedeLeProprieLezioni() {
        apriLezioni();

        var grid = test(find(Grid.class).single());
        assertEquals(30, grid.size());
        assertEquals("Corso di Canto", grid.getCellText(0, 0));
    }

    @Test
    @WithUserDetails("insegnante1")
    @SuppressWarnings("unchecked")
    void insegnante_vedeBadgeOggiSettimanaProssimaEInRitardo() {
        Insegnante insegnante = insegnanteService.findByUsername("insegnante1").orElseThrow();
        List<Lezione> tutte = lezioneService.findByInsegnante(insegnante.getId());
        Lezione prima = tutte.getFirst();
        Lezione seconda = tutte.get(1);
        LocalDate dataPrima = prima.getData();
        LocalDate dataSeconda = seconda.getData();
        LocalDate oggi = LocalDate.now();
        prima.setData(oggi);
        lezioneService.save(prima);
        seconda.setData(oggi.with(java.time.temporal.TemporalAdjusters.next(java.time.DayOfWeek.MONDAY)));
        lezioneService.save(seconda);
        try {
            apriLezioni();
            var grid = test((Grid<Lezione>) find(Grid.class).single());
            Badge oggiBadge = badgeData(grid, riga(grid, prima));
            assertEquals("Oggi", oggiBadge.getText());
            assertTrue(oggiBadge.getThemeNames().contains(BadgeVariant.FILLED.getVariantName()));
            Badge prossima = badgeData(grid, riga(grid, seconda));
            assertEquals("Settimana prossima", prossima.getText());
            assertTrue(prossima.getThemeNames().contains(BadgeVariant.CONTRAST.getVariantName()));
            // Le lezioni del 2024 ancora da compilare
            Badge ritardo = badgeData(grid, riga(grid, tutte.get(2)));
            assertEquals("In ritardo", ritardo.getText());
            assertTrue(ritardo.getThemeNames().contains(BadgeVariant.ERROR.getVariantName()));
        } finally {
            prima.setData(dataPrima);
            lezioneService.save(prima);
            seconda.setData(dataSeconda);
            lezioneService.save(seconda);
        }
    }

    /**
     * Apre "Le Mie Lezioni" ricreandola: per insegnante1 e insegnante2 (un solo corso) la vista è già aperta
     * all'avvio del test, al posto della Home, con i dati di quel momento.
     */
    private void apriLezioni() {
        navigate(HomeView.class);
        navigate(LezioniInsegnanteView.class);
    }

    @Test
    @WithUserDetails("insegnante1")
    void insegnante_conUnSoloCorso_alLoginApreLeLezioni() {
        assertInstanceOf(LezioniInsegnanteView.class, getCurrentView());
        // Dopo il primo accesso la Home resta raggiungibile
        assertInstanceOf(HomeView.class, navigate(HomeView.class));
    }

    @Test
    @WithUserDetails("insegnante1")
    void insegnante_conPiuCorsi_alLoginApreLaHome() {
        Insegnante insegnante1 = insegnanteService.findByUsername("insegnante1").orElseThrow();
        Corso chitarra = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Chitarra")).findFirst().orElseThrow();
        Insegnante insegnante2 = chitarra.getInsegnante();
        chitarra.setInsegnante(insegnante1);
        corsoService.save(chitarra);
        try {
            // Simula un nuovo login: prima navigazione della sessione verso la Home
            com.vaadin.flow.server.VaadinSession.getCurrent().setAttribute(MainLayout.class.getName() + ".paginaInizialeDecisa", null);
            assertInstanceOf(HomeView.class, navigate(HomeView.class));
        } finally {
            chitarra.setInsegnante(insegnante2);
            corsoService.save(chitarra);
        }
    }

    private static int riga(GridTester<Grid<Lezione>, Lezione> grid, Lezione lezione) {
        return IntStream.range(0, grid.size()).filter(r -> grid.getRow(r).getId().equals(lezione.getId()))
            .findFirst().orElseThrow();
    }

    /** Badge accanto alla data (colonna Data, seconda visibile su desktop). */
    private static Badge badgeData(GridTester<Grid<Lezione>, Lezione> grid, int riga) {
        return grid.getCellComponent(riga, 1).getChildren().filter(Badge.class::isInstance).map(Badge.class::cast)
            .findFirst().orElseThrow();
    }

    @Test
    @WithUserDetails("insegnante1")
    @SuppressWarnings("unchecked")
    void insegnante_segnaLezioneNonEffettuataELaRipristina() {
        apriLezioni();
        var grid = test((Grid<Lezione>) find(Grid.class).single());
        Lezione lezione = grid.getRow(0);
        test(pulsante(grid.getCellComponent(0, 5), "Gestisci Presenze")).click();
        test(find(Button.class).withText("Non effettuata").single()).click();
        test(find(TextField.class).withLabel("Motivo").single()).setValue("insegnante assente");
        test(find(Button.class).withText("Segna non effettuata").single()).click();
        try {
            Lezione salvata = lezioneService.findById(lezione.getId());
            assertTrue(salvata.isNonEffettuata());
            assertEquals("insegnante assente", salvata.getMotivoNonEffettuata());
            assertEquals(29, grid.size());

            SegmentedControl<FiltroLezioni> filtro = find(SegmentedControl.class).single();
            test(filtro.getPulsante(FiltroLezioni.NON_EFFETTUATE)).click();
            assertEquals(1, grid.size());
            assertEquals("Non effettuata", ((Badge) grid.getCellComponent(0, 4)).getText());

            // Il dialog della lezione non effettuata non mostra il registro: solo il ripristino
            test(pulsante(grid.getCellComponent(0, 5), "Gestisci Presenze")).click();
            assertFalse(find(Button.class).withText("Segna Completata").exists());
            test(find(Button.class).withText("Ripristina").single()).click();
            assertEquals(0, grid.size());
            assertTrue(lezioneService.findById(lezione.getId()).isDaFare());
        } finally {
            if (lezioneService.findById(lezione.getId()).isNonEffettuata()) {
                lezioneService.ripristina(lezione.getId(), "insegnante1");
            }
        }
    }

    @Test
    @WithUserDetails("insegnante1")
    @SuppressWarnings("unchecked")
    void insegnante_recuperaLezioneNonEffettuataEAggiungeLezione() {
        Corso canto = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow();
        Lezione saltata = lezioneService.findByCorso(canto.getId()).getFirst();
        lezioneService.segnaNonEffettuata(saltata.getId(), null, "insegnante1");
        try {
            apriLezioni();
            var grid = test((Grid<Lezione>) find(Grid.class).single());
            SegmentedControl<FiltroLezioni> filtro = find(SegmentedControl.class).single();
            test(filtro.getPulsante(FiltroLezioni.NON_EFFETTUATE)).click();
            test(pulsante(grid.getCellComponent(0, 5), "Gestisci Presenze")).click();
            test(find(Button.class).withText("Recupera").single()).click();

            // Un solo corso: la scelta del corso è nascosta, l'orario è quello del corso; la data è obbligatoria
            assertFalse(find(ComboBox.class).withLabel("Corso").exists());
            assertEquals(canto.getOrarioInizio(), find(TimePicker.class).withLabel("Orario").single().getValue());
            test(find(Button.class).withText("Salva").single()).click();
            assertTrue(find(DatePicker.class).withLabel("Data").single().isInvalid());
            test(find(DatePicker.class).withLabel("Data").single()).setValue(LocalDate.now());
            test(find(Button.class).withText("Salva").single()).click();

            test(filtro.getPulsante(FiltroLezioni.DA_FARE)).click();
            int riga = IntStream.range(0, grid.size()).filter(r -> grid.getRow(r).getRecuperoDi() != null).findFirst().orElseThrow();
            Lezione recupero = grid.getRow(riga);
            assertEquals(saltata.getNumeroLezione() + " · Recupero", grid.getCellText(riga, 2));
            assertEquals(7, presenzaService.findByLezione(recupero.getId()).size());

            // Lezione in più dal pulsante sopra la lista
            test(find(Button.class).withText("Nuova lezione").single()).click();
            test(find(DatePicker.class).withLabel("Data").single()).setValue(LocalDate.now().plusDays(1));
            test(find(Button.class).withText("Salva").single()).click();
            int rigaExtra = IntStream.range(0, grid.size())
                .filter(r -> grid.getRow(r).isAggiunta() && grid.getRow(r).getRecuperoDi() == null).findFirst().orElseThrow();
            assertTrue(grid.getCellText(rigaExtra, 2).endsWith(" · Aggiunta"));

            // L'insegnante elimina le lezioni aggiunte non completate
            for (Lezione aggiunta : List.of(grid.getRow(rigaExtra), recupero)) {
                int r = IntStream.range(0, grid.size()).filter(i -> grid.getRow(i).getId().equals(aggiunta.getId())).findFirst().orElseThrow();
                test(pulsante(grid.getCellComponent(r, 5), "Gestisci Presenze")).click();
                test(find(Button.class).withText("Elimina").single()).click();
                test(find(Button.class).withText("Elimina").all().getLast()).click();
                assertNull(lezioneService.findById(aggiunta.getId()));
            }
        } finally {
            lezioneService.findByCorso(canto.getId()).stream().filter(Lezione::isAggiunta)
                .forEach(l -> lezioneService.eliminaAggiunta(l.getId(), "insegnante1"));
            lezioneService.ripristina(saltata.getId(), "insegnante1");
        }
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_gestisceLeLezioniDiTuttiICorsiSenzaEliminare() {
        navigate(HomeView.class);
        assertTrue(find(SideNavItem.class).all().stream().anyMatch(v -> v.getLabel().equals("Lezioni")));
        navigate(LezioniView.class);
        var grid = test((Grid<Lezione>) find(Grid.class).single());
        assertEquals(60, grid.size());
        ComboBox<Corso> filtroCorso = filtro("Filtra per corso");
        test(filtroCorso).selectItem("Corso di Canto");
        assertEquals(30, grid.size());
        assertEquals("Maria Verdi", grid.getCellText(0, 1));

        Lezione lezione = grid.getRow(0);
        test(pulsante(grid.getCellComponent(0, 6), "Gestisci Presenze")).click();
        test(find(Button.class).withText("Non effettuata").single()).click();
        test(find(Button.class).withText("Segna non effettuata").single()).click();
        Lezione recupero = null;
        try {
            assertTrue(lezioneService.findById(lezione.getId()).isNonEffettuata());
            SegmentedControl<FiltroLezioni> filtro = find(SegmentedControl.class).single();
            test(filtro.getPulsante(FiltroLezioni.NON_EFFETTUATE)).click();
            test(pulsante(grid.getCellComponent(0, 6), "Gestisci Presenze")).click();
            test(find(Button.class).withText("Recupera").single()).click();
            test(find(DatePicker.class).withLabel("Data").single()).setValue(LocalDate.now());
            test(find(Button.class).withText("Salva").single()).click();
            recupero = lezioneService.findByCorso(lezione.getCorso().getId()).stream()
                .filter(l -> l.getRecuperoDi() != null).findFirst().orElseThrow();

            // La segreteria non elimina le lezioni aggiunte
            test(filtro.getPulsante(FiltroLezioni.DA_FARE)).click();
            Long idRecupero = recupero.getId();
            int riga = IntStream.range(0, grid.size()).filter(r -> grid.getRow(r).getId().equals(idRecupero)).findFirst().orElseThrow();
            test(pulsante(grid.getCellComponent(riga, 6), "Gestisci Presenze")).click();
            assertFalse(find(Button.class).withText("Elimina").exists());
        } finally {
            if (recupero != null) {
                lezioneService.eliminaAggiunta(recupero.getId(), "admin");
            }
            lezioneService.ripristina(lezione.getId(), "segreteria");
        }
    }

    @Test
    @WithUserDetails("admin")
    @SuppressWarnings("unchecked")
    void admin_aggiungeEdEliminaUnaLezione() {
        navigate(LezioniView.class);
        var grid = test((Grid<Lezione>) find(Grid.class).single());
        test(find(Button.class).withText("Nuova lezione").single()).click();
        // Più corsi: la scelta del corso è obbligatoria
        test(find(DatePicker.class).withLabel("Data").single()).setValue(LocalDate.now());
        test(find(Button.class).withText("Salva").single()).click();
        ComboBox<Corso> corso = find(ComboBox.class).withLabel("Corso").single();
        assertTrue(corso.isInvalid());
        test(corso).selectItem("Corso di Chitarra");
        test(find(Button.class).withText("Salva").single()).click();
        assertEquals(61, grid.size());

        int riga = IntStream.range(0, grid.size()).filter(r -> grid.getRow(r).isAggiunta()).findFirst().orElseThrow();
        Lezione aggiunta = grid.getRow(riga);
        test(pulsante(grid.getCellComponent(riga, 6), "Gestisci Presenze")).click();
        test(find(Button.class).withText("Elimina").single()).click();
        test(find(Button.class).withText("Elimina").all().getLast()).click();
        assertNull(lezioneService.findById(aggiunta.getId()));
        assertEquals(60, grid.size());
    }

    @Test
    @WithUserDetails("admin")
    @SuppressWarnings("unchecked")
    void admin_vedeLeLezioniDaFareInUnaFestivitaEUnAvvisoSullaNuovaLezione() {
        navigate(LezioniView.class);
        Lezione prima = test((Grid<Lezione>) find(Grid.class).single()).getRow(0);
        Festivita chiusura = new Festivita();
        chiusura.setNome("Chiusura di prova");
        chiusura.setTipo(Festivita.Tipo.DATE);
        chiusura.setDataInizio(prima.getData());
        festivitaService.salva(chiusura);
        try {
            navigate(HomeView.class);
            navigate(LezioniView.class);
            var grid = test((Grid<Lezione>) find(Grid.class).single());
            assertEquals(prima, grid.getRow(0));
            // Colonne su desktop: Corso, Insegnante, Data, …
            assertTrue(grid.getCellComponent(0, 2).getChildren()
                .anyMatch(c -> c instanceof Badge badge && badge.getText().contains("Chiusura di prova")));

            test(find(Button.class).withText("Nuova lezione").single()).click();
            test(find(DatePicker.class).withLabel("Data").single()).setValue(prima.getData());
            assertTrue(find(Span.class).all().stream().anyMatch(span -> span.isVisible()
                && span.getText().startsWith("È una festività (Chiusura di prova)")));
        } finally {
            festivitaService.elimina(chiusura.getId());
        }
    }

    @Test
    @WithUserDetails("insegnante1")
    void insegnante_nonAccedeAllaVistaLezioni() {
        navigate("lezioni", RouteNotFoundError.class);
    }

    @Test
    @WithUserDetails("allievo_canto1")
    @SuppressWarnings("unchecked")
    void allievo_vedeLezioneNonEffettuataEsclusaDalleStatistiche() {
        Corso canto = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow();
        Lezione lezione = lezioneService.findByCorso(canto.getId()).getFirst();
        lezioneService.segnaNonEffettuata(lezione.getId(), "festività", "insegnante1");
        try {
            navigate(MieiCorsiView.class);
            var corsi = test(find(Grid.class).single());
            test(pulsante(corsi.getCellComponent(0, 8), "Visualizza Presenze")).click();
            Dialog dialog = find(Dialog.class).single();
            assertTrue(find(Span.class).from(dialog).withText("Totale lezioni: 29").exists());
            var presenze = test((Grid<Presenza>) find(Grid.class).from(dialog).single());
            assertEquals("Lezione non effettuata", presenze.getCellText(0, 2));
        } finally {
            lezioneService.ripristina(lezione.getId(), "insegnante1");
        }
    }

    @Test
    @WithUserDetails("insegnante1")
    @SuppressWarnings("unchecked")
    void insegnante_indicaSeEranoPresentiEntrambiGliInsegnanti() {
        Corso canto = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow();
        Insegnante marco = insegnanteService.findByUsername("insegnante2").orElseThrow();
        canto.setSecondoInsegnante(marco);
        corsoService.save(canto);
        try {
            apriLezioni();
            var lezioni = test((Grid<Lezione>) find(Grid.class).single());
            Lezione lezione = lezioni.getRow(0);
            test((Button) lezioni.getCellComponent(0, 5).getChildren().findFirst().orElseThrow()).click();

            assertTrue(find(Button.class).withText("Entrambi").exists());
            test(find(Button.class).withText("Solo Rossi").single()).click();
            assertEquals(Set.of(marco), lezioneService.findById(lezione.getId()).getInsegnantiPresenti());
        } finally {
            canto.setSecondoInsegnante(null);
            corsoService.save(canto);
        }
    }

    @Test
    @WithUserDetails("insegnante1")
    @SuppressWarnings("unchecked")
    void insegnante_conUnSoloInsegnanteNonVedeLaSceltaDegliInsegnanti() {
        apriLezioni();
        var lezioni = test((Grid<Lezione>) find(Grid.class).single());
        test((Button) lezioni.getCellComponent(0, 5).getChildren().findFirst().orElseThrow()).click();
        assertFalse(find(Button.class).withText("Entrambi").exists());
    }

    @Test
    @WithUserDetails("insegnante1")
    void insegnante_segnaAssenzaGiustificataConUnTocco() {
        apriLezioni();
        var lezioni = test(find(Grid.class).single());
        Button gestisciPresenze = (Button) lezioni.getCellComponent(0, 5).getChildren().findFirst().orElseThrow();
        test(gestisciPresenze).click();

        @SuppressWarnings("unchecked")
        Grid<Presenza> gridPresenze = find(Grid.class).all().getLast();
        var presenze = test(gridPresenze);
        Presenza presenza = presenze.getRow(0);
        // Colonne su desktop: Allievo, Stato, Note
        StatoPresenzaToggle toggle = (StatoPresenzaToggle) presenze.getCellComponent(0, 1);
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
            apriLezioni();
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
    @WithUserDetails("insegnante1")
    void insegnante_aggiornaIlProprioProfiloSenzaTariffa() {
        navigate(HomeView.class);
        assertTrue(find(SideNavItem.class).all().stream().anyMatch(i -> i.getLabel().equals("Il mio profilo")));
        navigate(ProfiloView.class);

        assertTrue(find(TextField.class).withLabel("Username").single().isReadOnly());
        BigDecimalField tariffa = find(BigDecimalField.class).withLabel("Tariffa oraria").single();
        assertTrue(tariffa.isReadOnly());
        assertEquals(0, new BigDecimal("25").compareTo(tariffa.getValue()));

        test(find(TextField.class).withLabel("Telefono").single()).setValue("333 1234567");
        test(find(TextField.class).withLabel("Comune").single()).setValue("Roma");
        test(find(Button.class).withText("Salva").single()).click();

        Insegnante insegnante = insegnanteService.findByUsername("insegnante1").orElseThrow();
        assertEquals("333 1234567", insegnante.getUser().getTelefono());
        assertEquals("Roma", insegnante.getIndirizzo().getComune());
        assertEquals(0, new BigDecimal("25").compareTo(insegnante.getTariffaOraria()));
        assertTrue(insegnante.getUser().getRoles().contains("INSEGNANTE"));

        // Cambio password: almeno 8 caratteri e la conferma deve coincidere
        PasswordField nuova = find(PasswordField.class).withLabel("Nuova password").single();
        PasswordField conferma = find(PasswordField.class).withLabel("Conferma password").single();
        test(nuova).setValue("corta");
        test(conferma).setValue("corta");
        test(find(Button.class).withText("Cambia password").single()).click();
        assertTrue(nuova.isInvalid());
        test(nuova).setValue("nuova-segreta");
        test(conferma).setValue("diversa");
        test(find(Button.class).withText("Cambia password").single()).click();
        assertTrue(conferma.isInvalid());
        try {
            test(conferma).setValue("nuova-segreta");
            test(find(Button.class).withText("Cambia password").single()).click();
            assertTrue(passwordEncoder.matches("nuova-segreta", userService.findByUsername("insegnante1").getPassword()));
        } finally {
            userService.cambiaPassword("insegnante1", "insegnante1");
        }
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
    @WithUserDetails("genitore3")
    @SuppressWarnings("unchecked")
    void genitore_vedeQuotaPagatoResiduoERate() {
        Corso canto = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow();
        Allievo allievo = allievoService.findByUsername("allievo_canto3").orElseThrow();
        Pagamento rata = new Pagamento();
        rata.setData(LocalDate.now());
        rata.setImporto(new BigDecimal("200"));
        rata.setMetodo(MetodoPagamento.CASSA);
        pagamentoService.salva(canto.getId(), allievo.getId(), rata);
        canto.setPrezzoPreiscrizione(new BigDecimal("510"));
        corsoService.save(canto);
        try {
            navigate(FigliView.class);
            var figli = test((Grid<Allievo>) find(Grid.class).single());
            int riga = IntStream.range(0, figli.size())
                .filter(r -> figli.getCellText(r, 0).equals("Allievo Canto3")).findFirst().orElseThrow();
            test(pulsante(figli.getCellComponent(riga, 3), "Visualizza Dettagli")).click();

            var corsi = test((Grid<Corso>) find(Grid.class).from(find(Dialog.class).single()).single());
            assertEquals("Corso di Canto", corsi.getCellText(0, 0));
            // Costo totale, pre-iscrizione e prezzo applicato (quota dovuta)
            assertEquals(List.of("600,00 €", "510,00 €", "600,00 €"),
                List.of(corsi.getCellText(0, 3), corsi.getCellText(0, 4), corsi.getCellText(0, 5)));
            assertEquals("Parziale 33%", ((Badge) corsi.getCellComponent(0, 6)).getText());
            test(pulsante(corsi.getCellComponent(0, 7), "Pagamenti")).click();

            PagamentiAllievoDialog dialog = find(PagamentiAllievoDialog.class).single();
            assertTrue(find(Span.class).from(dialog).withText("Da pagare 600,00 €").exists());
            assertTrue(find(Span.class).from(dialog).withText("Pagato 200,00 €").exists());
            assertTrue(find(Span.class).from(dialog).withText("Residuo 400,00 €").exists());
            var rate = test((Grid<Pagamento>) find(Grid.class).from(dialog).all().stream()
            .filter(g -> !(g instanceof GrigliaInvii)).findFirst().orElseThrow());
            assertEquals(1, rate.size());
            assertEquals("Cassa", rate.getCellText(0, 2));
            // Sola lettura: nessun pulsante di modifica
            assertFalse(find(Button.class).from(dialog).withText("Aggiungi pagamento").exists());
        } finally {
            pagamentoService.findPagamenti(canto.getId(), allievo.getId()).forEach(pagamentoService::elimina);
            Corso corso = corsoService.findById(canto.getId());
            corso.setPrezzoPreiscrizione(null);
            corsoService.save(corso);
        }
    }

    @Test
    @WithUserDetails("allievo_canto1")
    void allievo_vedePrezziDelCorsoEQuotaApplicata() {
        Corso canto = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow();
        Allievo allievo = allievoService.findByUsername("allievo_canto1").orElseThrow();
        canto.setPrezzoPreiscrizione(new BigDecimal("510"));
        corsoService.save(canto);
        pagamentoService.impostaQuota(canto.getId(), allievo.getId(), new BigDecimal("510"), "pre-iscrizione");
        try {
            navigate(MieiCorsiView.class);
            var corsi = test(find(Grid.class).single());
            assertEquals("Corso di Canto", corsi.getCellText(0, 0));
            assertEquals(List.of("600,00 €", "510,00 €", "510,00 €"),
                List.of(corsi.getCellText(0, 5), corsi.getCellText(0, 6), corsi.getCellText(0, 7)));
        } finally {
            pagamentoService.impostaQuota(canto.getId(), allievo.getId(), null, null);
            Corso corso = corsoService.findById(canto.getId());
            corso.setPrezzoPreiscrizione(null);
            corsoService.save(corso);
        }
    }

    @Test
    @WithUserDetails("genitore3")
    void genitore_aggiornaIlProprioProfilo() {
        navigate(ProfiloView.class);
        assertTrue(find(TextField.class).withLabel("Username").single().isReadOnly());
        test(find(TextField.class).withLabel("Luogo di nascita").single()).setValue("Firenze");
        test(find(Button.class).withText("Salva").single()).click();

        Genitore genitore = genitoreService.findByUsername("genitore3").orElseThrow();
        assertEquals("Firenze", genitore.getLuogoNascita());
        assertTrue(genitore.getUser().getRoles().contains("GENITORE"));
        assertTrue(find(Button.class).withText("Cambia password").exists());
    }

    @Test
    @WithUserDetails("genitore3")
    @SuppressWarnings("unchecked")
    void genitore_modificaIDatiDiUnFiglio() {
        navigate(FigliView.class);
        var figli = test((Grid<Allievo>) find(Grid.class).single());
        int riga = IntStream.range(0, figli.size())
            .filter(r -> figli.getCellText(r, 0).equals("Allievo Canto3")).findFirst().orElseThrow();
        test(pulsante(figli.getCellComponent(riga, 3), "Modifica")).click();

        Dialog dialog = find(Dialog.class).single();
        assertTrue(find(TextField.class).from(dialog).withLabel("Username").single().isReadOnly());
        test(find(TextField.class).from(dialog).withLabel("Telefono").single()).setValue("320 7654321");
        test(find(TextField.class).from(dialog).withLabel("Luogo di nascita").single()).setValue("Torino");
        test(find(Button.class).from(dialog).withText("Salva").single()).click();
        assertFalse(dialog.isOpened());

        Allievo allievo = allievoService.findByUsername("allievo_canto3").orElseThrow();
        assertEquals("Torino", allievo.getLuogoNascita());
        assertEquals("320 7654321", allievo.getUser().getTelefono());
        assertEquals("genitore3", allievo.getGenitore().getUser().getUsername());

        // Un genitore non può modificare i figli di un altro
        Allievo altro = allievoService.findByUsername("allievo_canto1").orElseThrow();
        assertThrows(IllegalArgumentException.class,
            () -> allievoService.aggiornaDaGenitore("genitore3", altro.getId(), altro, altro.getUser()));
    }

    @Test
    @WithUserDetails("genitore3")
    @SuppressWarnings("unchecked")
    void genitore_scaricaLaRicevutaInPdfSoloDeiPropriFigli() throws Exception {
        Corso canto = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow();
        Allievo figlio = allievoService.findByUsername("allievo_canto3").orElseThrow();
        Allievo altro = allievoService.findByUsername("allievo_canto1").orElseThrow();
        Pagamento rata = new Pagamento();
        rata.setData(LocalDate.now());
        rata.setImporto(new BigDecimal("200"));
        pagamentoService.salva(canto.getId(), figlio.getId(), rata);
        try {
            navigate(FigliView.class);
            var figli = test((Grid<Allievo>) find(Grid.class).single());
            int riga = IntStream.range(0, figli.size())
                .filter(r -> figli.getCellText(r, 0).equals("Allievo Canto3")).findFirst().orElseThrow();
            test(pulsante(figli.getCellComponent(riga, 3), "Visualizza Dettagli")).click();
            var corsi = test((Grid<Corso>) find(Grid.class).from(find(Dialog.class).single()).single());
            // Il pulsante Ricevuta è dentro un link di download
            com.vaadin.flow.component.html.Anchor link = (com.vaadin.flow.component.html.Anchor) corsi.getCellComponent(0, 7)
                .getChildren().filter(com.vaadin.flow.component.html.Anchor.class::isInstance).findFirst().orElseThrow();
            assertFalse(link.getHref().isBlank());

            var pdf = documentiService.ricevutaPer(canto.getId(), figlio.getId(), "genitore3");
            assertTrue(pdf.nomeFile().startsWith("ricevuta-corso-di-canto-"));
            String testo;
            try (var documento = org.apache.pdfbox.Loader.loadPDF(pdf.contenuto())) {
                testo = new org.apache.pdfbox.text.PDFTextStripper().getText(documento);
            }
            assertTrue(testo.contains("Allievo Canto3"), testo);
            assertTrue(testo.contains("Via Roma 3, 00184 Roma (RM) (del genitore)"), testo);
            assertTrue(testo.contains("200,00 €"), testo);
            assertTrue(testo.contains("400,00 €"), testo);
            // Ricevuta di un allievo che non è suo figlio: come se non esistesse
            assertThrows(IllegalArgumentException.class, () -> documentiService.ricevutaPer(canto.getId(), altro.getId(), "genitore3"));
        } finally {
            pagamentoService.findPagamenti(canto.getId(), figlio.getId()).forEach(pagamentoService::elimina);
        }
    }

    @Test
    void allievo_nonScaricaLaRicevuta() {
        Corso canto = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow();
        Allievo allievo = allievoService.findByUsername("allievo_canto1").orElseThrow();
        assertThrows(IllegalArgumentException.class, () -> documentiService.ricevutaPer(canto.getId(), allievo.getId(), "allievo_canto1"));
        assertThrows(IllegalArgumentException.class, () -> documentiService.ricevutaPer(canto.getId(), -1L, "admin"));
    }

    @Test
    @WithUserDetails("allievo_canto1")
    void allievo_vedeIlProprioProfiloInSolaLettura() {
        navigate(ProfiloView.class);
        assertTrue(find(TextField.class).withLabel("Nome").single().isReadOnly());
        assertTrue(find(TextField.class).withLabel("Codice fiscale").single().isReadOnly());
        assertEquals("Genitore1 Cognome1", find(TextField.class).withLabel("Genitore").single().getValue());
        assertTrue(find(Span.class).withText("Indirizzo: Via Roma 1, 00184 Roma (RM) (del genitore)").exists());
        assertFalse(find(Button.class).withText("Salva").exists());
        assertFalse(find(Button.class).withText("Cambia password").exists());
    }

    @Test
    @WithUserDetails("allievo_canto1")
    void allievo_vedeIPropriCorsi() {
        assertInstanceOf(MieiCorsiView.class, navigate(MieiCorsiView.class));

        var grid = test(find(Grid.class).single());
        assertEquals(1, grid.size());
        assertEquals("Corso di Canto", grid.getCellText(0, 0));
    }
}
