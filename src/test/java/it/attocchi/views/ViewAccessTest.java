package it.attocchi.views;

import it.attocchi.entity.LetteraIncarico;
import it.attocchi.service.ElencoDocumentiService;
import java.time.YearMonth;
import com.vaadin.flow.component.tabs.Tabs;
import java.util.stream.Stream;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.ClickEvent;
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
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridTester;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextFieldTester;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.router.RouteNotFoundError;
import it.attocchi.service.ElencoDocumentiService.DocumentoFamiglia;
import it.attocchi.service.ElencoDocumentiService.DocumentoRiepilogo;
import it.attocchi.service.ElencoDocumentiService.DocumentoIscrizione;
import it.attocchi.views.DocumentiView;
import it.attocchi.entity.Corso;
import it.attocchi.entity.GruppoCorso;
import it.attocchi.entity.Genitore;
import it.attocchi.entity.Insegnante;
import it.attocchi.entity.Iscrizione;
import it.attocchi.entity.Lezione;
import it.attocchi.entity.Luogo;
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
import it.attocchi.service.MieiAllieviService.AllievoDelCorso;
import it.attocchi.service.GenitoreService;
import it.attocchi.service.InsegnanteService;
import it.attocchi.service.CorsoService;
import it.attocchi.service.LezioneService;
import it.attocchi.service.PagamentoService;
import it.attocchi.service.DashboardService.RigaGruppo;
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
import it.attocchi.views.admin.LuoghiView;
import it.attocchi.views.admin.LezioniView;
import it.attocchi.views.admin.BilancioDialog;
import it.attocchi.views.admin.EmailDiTestDialog;
import it.attocchi.views.admin.EmailInviateView;
import it.attocchi.views.admin.GrigliaAperture;
import it.attocchi.views.admin.GrigliaInvii;
import it.attocchi.views.admin.RimborsiDialog;
import it.attocchi.views.admin.RimborsiView;
import it.attocchi.views.admin.GenitoriView;
import it.attocchi.views.admin.NuovoAllievoRapidoDialog;
import it.attocchi.views.admin.RiepilogoIscrittiView;
import it.attocchi.views.admin.GestionePagamentiView;
import it.attocchi.views.admin.InsegnantiView;
import it.attocchi.views.admin.InvioEmailDialog;
import it.attocchi.views.admin.PartecipantiDialog;
import it.attocchi.views.admin.GruppoDialog;
import it.attocchi.views.admin.QuotaPagamentiDialog;
import it.attocchi.views.admin.AllieviView;
import it.attocchi.views.admin.UtentiView;
import it.attocchi.views.insegnante.LezioniInsegnanteView;
import it.attocchi.views.insegnante.MieiAllieviView;
import it.attocchi.views.insegnante.MieiRimborsiView;
import it.attocchi.views.ElencoLezioni.FiltroLezioni;
import it.attocchi.views.genitore.FigliView;
import it.attocchi.views.genitore.PagamentiFigliView;
import it.attocchi.views.allievo.MieiCorsiView;
import it.attocchi.views.allievo.MieiPagamentiView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithUserDetails;

import java.math.BigDecimal;
import java.util.Objects;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import com.vaadin.flow.server.StreamResourceRegistry;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;
import it.attocchi.entity.AperturaDocumento;
import it.attocchi.repository.AperturaDocumentoRepository;
import it.attocchi.service.CronologiaDocumentiService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    private static final int AZIONI_CORSO = 11;

    @Autowired
    private PresenzaService presenzaService;

    @Autowired
    private LezioneService lezioneService;

    @Autowired
    private CronologiaDocumentiService cronologiaDocumentiService;

    @Autowired
    private AperturaDocumentoRepository aperturaDocumentoRepository;

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
    private it.attocchi.repository.UserRepository userRepository;

    @Autowired
    private InsegnanteRepository insegnanteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private GenitoreService genitoreService;

    @Autowired
    private it.attocchi.repository.GenitoreRepository genitoreRepository;

    @Autowired
    private AnnoScolasticoService annoScolasticoService;

    @Autowired
    private FestivitaService festivitaService;

    @Autowired
    private it.attocchi.service.LuogoService luogoService;

    @Autowired
    private org.springframework.beans.factory.ObjectProvider<org.springframework.boot.info.BuildProperties> buildProperties;

    @Autowired
    private it.attocchi.service.LetteraIncaricoService letteraIncaricoService;

    @Autowired
    private RimborsoService rimborsoService;

    @Autowired
    private BilancioService bilancioService;

    @Autowired
    private it.attocchi.service.DashboardService dashboardService;

    @Autowired
    private it.attocchi.security.SecurityService securityService;

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
    void admin_vedeVersioneEDataDellaBuildSottoIlLogo() {
        navigate(HomeView.class);
        assertTrue(find(Span.class).from(find(MainLayout.class).single()).all().stream()
            .anyMatch(s -> s.getText().matches("Versione \\S+ · \\d{2}/\\d{2}/\\d{4}")));
        assertEquals("Versione di sviluppo", MainLayout.versione(null));
    }

    @Test
    @WithUserDetails("genitore1")
    void genitore_nonVedeLaVersione() {
        navigate(HomeView.class);
        assertTrue(find(Span.class).from(find(MainLayout.class).single()).all().stream()
            .noneMatch(s -> s.getText().startsWith("Versione")));
    }

    @Test
    @WithUserDetails("admin")
    void admin_menuMostraSoloVociAdmin() {
        navigate(HomeView.class);

        List<String> voci = find(SideNavItem.class).all().stream().map(SideNavItem::getLabel).toList();
        assertTrue(voci.containsAll(List.of("Home", "Corsi", "Pagamenti", "Allievi", "Utenti", "Insegnanti", "Genitori")), voci.toString());
        assertFalse(voci.contains("Le mie lezioni"), voci.toString());
    }

    @Test
    @WithUserDetails("admin")
    void admin_dashboardConTotali() {
        navigate(HomeView.class);

        var eta = dashboardService.etaAllievi(LocalDate.now()).orElseThrow();
        assertEquals(List.of("Corsi: 2", "Allievi: 13", "Iscritti ai corsi attivi: 13", "Età media: " + eta.media(),
            "Più giovane: " + eta.minima(), "Più anziano: " + eta.massima(), "Genitori: 9", "Insegnanti: 2"), indicatori().subList(0, 8));
    }

    @Test
    @WithUserDetails("segreteria")
    void segreteria_dashboardContaGliIscrittiDeiCorsiAttivi() {
        Corso chitarra = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Chitarra")).findFirst().orElseThrow();
        Allievo allievo = allievoService.findByUsername("allievo_canto1").orElseThrow();
        // Un allievo di Canto si iscrive anche a Chitarra: gli allievi restano 13, le iscrizioni diventano 14
        corsoService.iscrivi(chitarra.getId(), allievo.getId());
        try {
            navigate(CorsiView.class);
            navigate(HomeView.class);
            assertEquals(List.of("Allievi: 13", "Iscritti ai corsi attivi: 14"), indicatori().subList(1, 3));

            // Chitarra non attivo: restano le 7 iscrizioni di Canto
            Corso inattivo = corsoService.findById(chitarra.getId());
            inattivo.setAttivo(false);
            corsoService.save(inattivo);
            navigate(CorsiView.class);
            navigate(HomeView.class);
            assertEquals("Iscritti ai corsi attivi: 7", indicatori().get(2));
        } finally {
            Corso attivo = corsoService.findById(chitarra.getId());
            attivo.setAttivo(true);
            corsoService.save(attivo);
            corsoService.rimuoviIscrizione(chitarra.getId(), allievo.getId());
        }
    }

    @Test
    @WithUserDetails("segreteria")
    void segreteria_dashboardConTotali() {
        navigate(HomeView.class);

        var eta = dashboardService.etaAllievi(LocalDate.now()).orElseThrow();
        assertEquals(List.of("Corsi: 2", "Allievi: 13", "Iscritti ai corsi attivi: 13", "Età media: " + eta.media(),
            "Più giovane: " + eta.minima(), "Più anziano: " + eta.massima(), "Genitori: 9", "Insegnanti: 2"), indicatori().subList(0, 8));
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
                indicatori().subList(8, 13));

            var grid = test((Grid<RiepilogoPagamenti>) find(Grid.class).all().getFirst());
            assertEquals(2, grid.size());
            assertEquals("Corso di Canto", grid.getCellText(0, 0));
            // Indici delle colonne visibili su desktop (la colonna card mobile è nascosta)
            assertEquals("1", grid.getCellText(0, 2));
            assertEquals("6", grid.getCellText(0, 3));
            assertEquals("200,00 € (5%)", grid.getCellText(0, 4));
            assertEquals("Corso di Chitarra", grid.getCellText(1, 0));
            assertEquals("3.600,00 € (100%)", grid.getCellText(1, 5));

            // Costo previsto degli insegnanti e margine: Canto 7 × 600 € = 4.200 € di quote, 3.500 € di costo
            var costi = bilancioService.costiPrevistiPerCorso();
            assertEquals(0, new BigDecimal("3500").compareTo(costi.get(canto.getId())));
            assertEquals("3.500,00 €", grid.getCellText(0, 6));
            Component margine = grid.getCellComponent(0, 7);
            assertEquals("700,00 €", ((Span) margine.getChildren().findFirst().orElseThrow()).getText());
            assertEquals("Attivo", ((Badge) margine.getChildren().skip(1).findFirst().orElseThrow()).getText());
            BigDecimal costoTotale = costi.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            assertEquals(List.of("Costo insegnanti previsto: " + Formati.euro(costoTotale) + " (lezioni programmate)",
                    "Margine previsto: " + Formati.euro(new BigDecimal("7800").subtract(costoTotale)) + " (quote − costo insegnanti)"),
                indicatori().subList(13, 15));
        } finally {
            pagamentoService.findPagamenti(canto.getId(), allievo.getId()).forEach(pagamentoService::elimina);
        }
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_dashboardConRiepilogoGruppi() {
        Corso chitarra = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Chitarra")).findFirst().orElseThrow();
        Allievo allievo = allievoService.findByUsername("allievo_chitarra1").orElseThrow();
        navigate(CorsiView.class);
        navigate(HomeView.class);
        assertEquals(List.of("Corsi con gruppi: 0", "Gruppi: 0", "Allievi senza gruppo: 0"), indicatori().subList(15, 18));
        var vuota = test((Grid<RigaGruppo>) find(Grid.class).all().getLast());
        assertEquals(0, vuota.size());

        GruppoCorso sabato = corsoService.salvaGruppo(chitarra.getId(),
            new GruppoCorso(null, "Principianti", DayOfWeek.SATURDAY, LocalTime.of(10, 0)));
        corsoService.salvaGruppo(chitarra.getId(), new GruppoCorso(null, null, DayOfWeek.TUESDAY, LocalTime.of(17, 0)));
        corsoService.assegnaGruppo(chitarra.getId(), allievo.getId(), sabato.getId());
        try {
            navigate(CorsiView.class);
            navigate(HomeView.class);
            // Chitarra ha 6 iscritti: 1 nel gruppo del sabato, 5 senza gruppo
            assertEquals(List.of("Corsi con gruppi: 1", "Gruppi: 2", "Allievi senza gruppo: 5"), indicatori().subList(15, 18));
            var gruppi = test((Grid<RigaGruppo>) find(Grid.class).all().getLast());
            assertEquals(2, gruppi.size());
            assertEquals("Corso di Chitarra", gruppi.getCellText(0, 0));
            assertEquals("Martedì 17:00", gruppi.getCellText(0, 1));
            assertEquals("0", gruppi.getCellText(0, 2));
            assertEquals("Principianti – sab 10:00", gruppi.getCellText(1, 1));
            assertEquals("1", gruppi.getCellText(1, 2));
        } finally {
            corsoService.assegnaGruppo(chitarra.getId(), allievo.getId(), null);
            corsoService.impostaOrario(chitarra.getId(), allievo.getId(), null);
            corsoService.gruppi(chitarra.getId()).forEach(g -> corsoService.eliminaGruppo(g.getId()));
        }
    }

    @Test
    @WithUserDetails("insegnante2")
    @SuppressWarnings("unchecked")
    void insegnante_dashboardConIGruppiDeiSuoiCorsi() {
        Corso chitarra = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Chitarra")).findFirst().orElseThrow();
        apriLezioni();
        navigate(HomeView.class);
        assertFalse(find(Grid.class).exists());

        corsoService.salvaGruppo(chitarra.getId(), new GruppoCorso(null, "Avanzati", DayOfWeek.TUESDAY, LocalTime.of(18, 0)));
        try {
            apriLezioni();
            navigate(HomeView.class);
            var gruppi = test((Grid<RigaGruppo>) find(Grid.class).single());
            assertEquals(1, gruppi.size());
            assertEquals("Avanzati – mar 18:00", gruppi.getCellText(0, 1));
        } finally {
            corsoService.gruppi(chitarra.getId()).forEach(g -> corsoService.eliminaGruppo(g.getId()));
        }
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_gestionePagamenti_colonnaGruppoEOrdineDelCorso() {
        Corso chitarra = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Chitarra")).findFirst().orElseThrow();
        Allievo allievo = allievoService.findByUsername("allievo_chitarra4").orElseThrow();
        GruppoCorso sabato = corsoService.salvaGruppo(chitarra.getId(),
            new GruppoCorso(null, "Piccoli", DayOfWeek.SATURDAY, LocalTime.of(10, 0)));
        corsoService.assegnaGruppo(chitarra.getId(), allievo.getId(), sabato.getId());
        try {
            navigate(GestionePagamentiView.class);
            var righe = test((Grid<RigaPagamento>) find(Grid.class).single());
            test(filtro("Filtra per corso")).selectItem("Corso di Chitarra (" + chitarra.getAnnoScolastico() + ")");
            // Prima il gruppo, poi gli altri in ordine di inserimento
            assertEquals(List.of("Allievo Chitarra4", "Allievo Chitarra1", "Allievo Chitarra2", "Allievo Chitarra3",
                    "Allievo Chitarra5", "Allievo Chitarra6"),
                IntStream.range(0, righe.size()).mapToObj(r -> righe.getRow(r).allievo().getNomeCompleto()).toList());
            assertEquals("Piccoli – sab 10:00", righe.getCellText(0, 3));
            assertEquals("", righe.getCellText(1, 3));
        } finally {
            corsoService.assegnaGruppo(chitarra.getId(), allievo.getId(), null);
            corsoService.impostaOrario(chitarra.getId(), allievo.getId(), null);
            corsoService.gruppi(chitarra.getId()).forEach(g -> corsoService.eliminaGruppo(g.getId()));
        }
    }

    @Test
    @WithUserDetails("genitore3")
    void genitore_dashboardConICorsiDeiFigli() {
        navigate(HomeView.class);

        // genitore3: Allievo Canto3 (Canto) e Allievo Chitarra1 (Chitarra), 600 € ciascuno, nessun pagamento
        assertEquals(List.of("Corsi dei figli: 2", "Pagato: 0,00 € (0% della quota)",
            "Da pagare: 1.200,00 € (su 1.200,00 € di quota)"), indicatori());
    }

    @Test
    @WithUserDetails("allievo_canto1")
    void allievoConGenitore_nonVedeIPagamenti() {
        assertFalse(find(SideNavItem.class).all().stream().anyMatch(i -> i.getLabel().equals("Pagamenti")));
        navigate(MieiPagamentiView.class);
        assertTrue(find(Span.class).withText("I pagamenti dei tuoi corsi sono gestiti dal genitore.").exists());
        assertFalse(find(Grid.class).exists());
    }

    @Test
    @WithUserDetails("allievo_canto1")
    @SuppressWarnings("unchecked")
    void allievoAdulto_vedeIPropriPagamentiComeUnGenitore() {
        Allievo allievo = allievoService.findByUsername("allievo_canto1").orElseThrow();
        Genitore genitore = allievo.getGenitore();
        allievo.setGenitore(null);
        allievoService.save(allievo);
        try {
            // Menu (costruito al login): la voce Pagamenti compare
            MainLayout layout = new MainLayout(securityService, dashboardService, allievoService, buildProperties);
            assertTrue(find(SideNavItem.class).from(layout).all().stream().anyMatch(i -> i.getLabel().equals("Pagamenti")));
            assertTrue(find(SideNavItem.class).from(layout).all().stream().anyMatch(i -> i.getLabel().equals("Documenti")));

            navigate(MieiPagamentiView.class);
            var righe = test((Grid<RigaPagamento>) find(Grid.class).single());
            assertEquals(1, righe.size());
            assertEquals("Corso di Canto", righe.getRow(0).corso().getNome());
            assertFalse(find(Button.class).withText("Riepilogo").exists());

            // I documenti sono in Documenti: il proprio riepilogo e la conferma
            navigate(DocumentiView.class);
            var documenti = test((Grid<DocumentoFamiglia>) find(Grid.class).single());
            assertEquals(2, documenti.size());
            assertEquals("Riepilogo pagamenti", documenti.getCellText(0, 0));
            assertEquals("Allievo Canto1", documenti.getCellText(0, 1));
            assertAnteprimaEInvio(documenti.getCellComponent(0, 5));

            navigate(MieiCorsiView.class);
            navigate(HomeView.class);
            assertEquals(List.of("Corsi a cui sono iscritto: 1", "Pagato: 0,00 € (0% della quota)",
                "Da pagare: 600,00 € (su 600,00 € di quota)"), indicatori());

            navigate(MieiCorsiView.class);
            Grid<Corso> corsi = find(Grid.class).single();
            int colonna = IntStream.range(0, corsi.getColumns().size())
                .filter(i -> "Pagamento".equals(corsi.getColumns().get(i).getHeaderText())).findFirst().orElseThrow();
            assertEquals("Da pagare", ((Badge) test(corsi).getCellComponent(0, colonna)).getText());
        } finally {
            Allievo ripristino = allievoService.findById(allievo.getId());
            ripristino.setGenitore(genitore);
            allievoService.save(ripristino);
        }
    }

    @Test
    @WithUserDetails("genitore3")
    void genitore_dashboardConIPagamentiDeiCorsiAttivi() {
        Corso canto = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow();
        Corso chitarra = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Chitarra")).findFirst().orElseThrow();
        Allievo allievo = allievoService.findByUsername("allievo_canto3").orElseThrow();
        Pagamento rata = new Pagamento();
        rata.setData(LocalDate.now());
        rata.setImporto(new BigDecimal("600"));
        pagamentoService.salva(canto.getId(), allievo.getId(), rata);
        Corso inattivo = corsoService.findById(chitarra.getId());
        inattivo.setAttivo(false);
        corsoService.save(inattivo);
        try {
            // Chitarra non attivo non conta: resta solo Canto, saldato
            navigate(FigliView.class);
            navigate(HomeView.class);
            assertEquals(List.of("Pagato: 600,00 € (100% della quota)", "Da pagare: 0,00 € (tutto saldato)"),
                indicatori().subList(1, 3));
        } finally {
            Corso attivo = corsoService.findById(chitarra.getId());
            attivo.setAttivo(true);
            corsoService.save(attivo);
            pagamentoService.findPagamenti(canto.getId(), allievo.getId()).forEach(pagamentoService::elimina);
        }
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

        assertAzioni(grid.getCellComponent(0, AZIONI_CORSO), "Modifica", "Partecipanti", "Aggiorna lezioni", "Duplica", "Elimina");

        // Apre il dialog di modifica: i giorni della settimana devono essere già caricati
        Button modifica = (Button) grid.getCellComponent(0, AZIONI_CORSO).getChildren().findFirst().orElseThrow();
        assertEquals("Modifica", modifica.getElement().getAttribute("aria-label"));
        test(modifica).click();
        CheckboxGroup<?> giorni = find(CheckboxGroup.class).single();
        assertFalse(giorni.getValue().isEmpty());
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_filtraPerUnAnnoScolasticoNonAttivo() {
        AnnoScolastico anno = annoScolasticoService.elenco().stream().filter(a -> a.getNome().equals("2024/2025")).findFirst().orElseThrow();
        annoScolasticoService.impostaAttivo(anno.getId(), false);
        try {
            navigate(CorsiView.class);
            ComboBox<String> filtro = find(ComboBox.class).all().stream()
                .filter(c -> c.getAriaLabel().filter("Filtra per anno scolastico"::equals).isPresent()).findFirst().orElseThrow();
            assertTrue(filtro.getListDataView().getItems().anyMatch("2024/2025"::equals));
            assertEquals("2024/2025 (archivio)", filtro.getItemLabelGenerator().apply("2024/2025"));
            filtro.setValue("2024/2025");
            assertEquals(2, test((Grid<Corso>) find(Grid.class).single()).size());
        } finally {
            annoScolasticoService.impostaAttivo(anno.getId(), true);
        }
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_duplicaUnCorsoEApreIlDialog() {
        navigate(CorsiView.class);
        var corsi = test((Grid<Corso>) find(Grid.class).single());
        test(pulsante(corsi.getCellComponent(riga(corsi, "Corso di Chitarra"), AZIONI_CORSO), "Duplica")).click();
        Corso copia = corsoService.findAll().stream()
            .filter(c -> c.getNome().equals("Corso di Chitarra") && !c.isPubblicato()).findFirst().orElseThrow();
        try {
            assertEquals("Modifica Corso", find(Dialog.class).single().getHeaderTitle());
            assertFalse(find(Checkbox.class).withLabel("Pubblicato").single().getValue());
        } finally {
            corsoService.delete(corsoService.findById(copia.getId()));
        }
    }

    @Test
    @WithUserDetails("admin")
    @SuppressWarnings("unchecked")
    void admin_aggiornaLezioniConAnteprimaDopoLaDataDiFine() {
        Corso canto = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow();
        List<Lezione> lezioni = lezioneService.findByCorso(canto.getId());
        canto.setDataFine(lezioni.get(24).getData());
        corsoService.save(canto);
        try {
            navigate(CorsiView.class);
            var corsi = test((Grid<Corso>) find(Grid.class).single());
            test(pulsante(corsi.getCellComponent(riga(corsi, "Corso di Canto"), AZIONI_CORSO), "Aggiorna lezioni")).click();
            Dialog dialog = find(Dialog.class).single();
            assertTrue(find(Span.class).from(dialog).all().stream().anyMatch(t -> t.getText().startsWith("Da togliere (5)")));
            test(find(Button.class).from(dialog).withText("Aggiorna").single()).click();
            assertEquals(25, lezioneService.findByCorso(canto.getId()).size());
        } finally {
            Corso ripristino = corsoService.findById(canto.getId());
            ripristino.setDataFine(null);
            corsoService.save(ripristino);
            corsoService.aggiornaLezioni(canto.getId());
        }
        assertEquals(30, lezioneService.findByCorso(canto.getId()).size());
    }

    @Test
    @WithUserDetails("admin")
    @SuppressWarnings("unchecked")
    void admin_corsi_servizioConBadgeEFiltroPerTipo() {
        Corso pulizia = new Corso();
        pulizia.setTipo(it.attocchi.entity.TipoCorso.SERVIZIO);
        pulizia.setNome("Pulizia");
        pulizia.setDataInizio(LocalDate.of(2024, 10, 1));
        pulizia.setAnnoScolastico("2024/2025");
        pulizia.setNumeroLezioni(10);
        pulizia.setDurataLezioneMinuti(120);
        pulizia.setInsegnante(insegnanteService.findByUsername("insegnante1").orElseThrow());
        pulizia = corsoService.save(pulizia);
        try {
            navigate(CorsiView.class);
            var grid = test((Grid<Corso>) find(Grid.class).single());
            assertEquals(3, grid.size());
            int riga = riga(grid, "Pulizia");
            assertTrue(find(Badge.class).from(grid.getCellComponent(riga, 0)).all().stream()
                .anyMatch(b -> "Servizio".equals(b.getText())));
            assertTrue(find(Badge.class).from(grid.getCellComponent(riga(grid, "Corso di Canto"), 0)).all().isEmpty());

            Select<CorsiView.FiltroTipo> filtro = find(Select.class).single();
            filtro.setValue(CorsiView.FiltroTipo.SERVIZI);
            assertEquals(1, grid.size());
            filtro.setValue(CorsiView.FiltroTipo.CORSI);
            assertEquals(2, grid.size());

            // Un corso nuovo nasce non pubblicato: badge e filtro
            filtro.setValue(CorsiView.FiltroTipo.SERVIZI);
            assertTrue(find(Badge.class).from(grid.getCellComponent(0, 0)).all().stream()
                .anyMatch(b -> "Non pubblicato".equals(b.getText())));
            filtro.setValue(CorsiView.FiltroTipo.TUTTI);
            test(find(Checkbox.class).withLabel("Solo non pubblicati").single()).click();
            assertEquals(1, grid.size());
            test(find(Checkbox.class).withLabel("Solo non pubblicati").single()).click();
            filtro.setValue(CorsiView.FiltroTipo.CORSI);

            // Nel dialog del servizio chi lo svolge è l'"Incaricato"
            filtro.setValue(CorsiView.FiltroTipo.SERVIZI);
            test(pulsante(grid.getCellComponent(0, AZIONI_CORSO), "Modifica")).click();
            assertTrue(find(ComboBox.class).all().stream().anyMatch(c -> "Incaricato".equals(c.getLabel())));
        } finally {
            corsoService.delete(corsoService.findById(pulizia.getId()));
        }
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_inserisceRapidamenteUnNuovoAllievoNelCorso() {
        PartecipantiDialog dialog = apriPartecipanti("Corso di Canto");
        var righe = test((Grid<Iscrizione>) find(Grid.class).from(dialog).single());
        assertEquals(7, righe.size());

        scriviNuovoPartecipante(dialog, "Marta Verdi Neri");
        NuovoAllievoRapidoDialog rapido = find(NuovoAllievoRapidoDialog.class).single();
        assertEquals("Marta", find(TextField.class).from(rapido).withLabel("Nome").single().getValue());
        assertEquals("Verdi Neri", find(TextField.class).from(rapido).withLabel("Cognome").single().getValue());
        assertFalse(find(Button.class).from(rapido).withText("Iscrivi questo").exists());
        // Email non valida: non si crea nulla
        test(find(EmailField.class).from(rapido).withLabel("Email").single()).setValue("non-una-email");
        test(find(Button.class).from(rapido).withText("Crea e iscrivi").single()).click();
        assertTrue(rapido.isOpened());
        assertTrue(allievoService.omonimi("Marta", "Verdi Neri").isEmpty());

        test(find(EmailField.class).from(rapido).withLabel("Email").single()).setValue("marta@example.com");
        test(find(Button.class).from(rapido).withText("Crea e iscrivi").single()).click();
        assertFalse(rapido.isOpened());
        Allievo marta = allievoService.omonimi("Marta", "Verdi Neri").getFirst();
        Corso canto = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow();
        try {
            assertEquals(8, righe.size());
            assertTrue(IntStream.range(0, righe.size()).anyMatch(r -> righe.getRow(r).getAllievo().equals(marta)));
            assertNull(marta.getGenitore());
            assertEquals("marta@example.com", marta.getUser().getEmail());
            assertFalse(marta.getUser().isEnabled());
        } finally {
            corsoService.rimuoviIscrizione(canto.getId(), marta.getId());
            // Ricaricato: la copia letta prima ha ancora l'iscrizione appena rimossa
            Allievo daEliminare = allievoService.findById(marta.getId());
            allievoService.delete(daEliminare);
            userService.delete(daEliminare.getUser());
        }
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_inserimentoRapidoProponeLOmonimoEsistente() {
        PartecipantiDialog dialog = apriPartecipanti("Corso di Canto");
        var righe = test((Grid<Iscrizione>) find(Grid.class).from(dialog).single());
        Allievo chitarra1 = allievoService.findByUsername("allievo_chitarra1").orElseThrow();
        Corso canto = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow();

        scriviNuovoPartecipante(dialog, "allievo chitarra1");
        NuovoAllievoRapidoDialog rapido = find(NuovoAllievoRapidoDialog.class).single();
        assertTrue(find(Span.class).from(rapido).all().stream()
            .anyMatch(s -> s.getText().startsWith("Esiste già un allievo Allievo Chitarra1 (genitore Genitore3 Cognome3)")));
        test(find(Button.class).from(rapido).withText("Iscrivi questo").single()).click();
        try {
            assertFalse(rapido.isOpened());
            assertEquals(8, righe.size());
            assertEquals(1, allievoService.omonimi("Allievo", "Chitarra1").size());
        } finally {
            corsoService.rimuoviIscrizione(canto.getId(), chitarra1.getId());
        }

        // Un omonimo già iscritto al corso non si propone
        scriviNuovoPartecipante(dialog, "Allievo Canto1");
        NuovoAllievoRapidoDialog secondo = find(NuovoAllievoRapidoDialog.class).single();
        assertTrue(find(Span.class).from(secondo).withText("già iscritto al corso").exists());
        assertFalse(find(Button.class).from(secondo).withText("Iscrivi questo").exists());
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_creaGruppiEAssegnaIPartecipanti() {
        // Senza gruppi la colonna Gruppo non c'è
        PartecipantiDialog dialog = apriPartecipanti("Corso di Chitarra");
        assertFalse(((Grid<Iscrizione>) find(Grid.class).from(dialog).single()).getColumns().get(2).isVisible());
        Allievo allievo = allievoService.findByUsername("allievo_chitarra1").orElseThrow();
        Corso chitarra = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Chitarra")).findFirst().orElseThrow();
        try {
            test(find(Button.class).from(dialog).withText("Nuovo gruppo").single()).click();
            GruppoDialog nuovo = find(GruppoDialog.class).single();
            test(find(TextField.class).from(nuovo).withLabel("Nome").single()).setValue("Principianti");
            SegmentedControl<DayOfWeek> giorno = find(SegmentedControl.class).from(nuovo).single();
            test(giorno.getPulsante(DayOfWeek.SATURDAY)).click();
            test(find(TimePicker.class).from(nuovo).withLabel("Orario").single()).setValue(LocalTime.of(10, 0));
            test(find(Button.class).from(nuovo).withText("Salva").single()).click();
            assertFalse(nuovo.isOpened());
            assertTrue(find(Span.class).from(dialog).withText("Principianti – sab 10:00 · 0 allievi").exists());
            // La segreteria modifica ma non elimina i gruppi
            assertTrue(find(Button.class).from(dialog).all().stream()
                .anyMatch(b -> "Modifica il gruppo Principianti – sab 10:00".equals(b.getElement().getAttribute("aria-label"))));
            assertFalse(find(Button.class).from(dialog).all().stream()
                .anyMatch(b -> "Elimina il gruppo Principianti – sab 10:00".equals(b.getElement().getAttribute("aria-label"))));

            // Assegnato al gruppo: frequenta solo il sabato e prende l'orario del gruppo
            assertTrue(((Grid<Iscrizione>) find(Grid.class).from(dialog).single()).getColumns().get(2).isVisible());
            test(gruppoDi(dialog, allievo)).selectItem("Principianti – sab 10:00");
            assertEquals("Principianti", iscrizione(chitarra, allievo).getGruppo().getNome());
            assertEquals(LocalTime.of(10, 0), iscrizione(chitarra, allievo).getOrarioLezione());
            assertEquals(Set.of(DayOfWeek.SATURDAY), iscrizione(chitarra, allievo).giorniEffettivi());
            assertTrue(find(Span.class).from(dialog).withText("Principianti – sab 10:00 · 1 allievo").exists());

            // Gestione Corsi mostra il numero di gruppi, vuoto per i corsi senza
            dialog.close();
            navigate(CorsiView.class);
            var corsi = test((Grid<Corso>) find(Grid.class).single());
            assertEquals("1", corsi.getCellText(riga(corsi, "Corso di Chitarra"), 9));
            assertEquals("", corsi.getCellText(riga(corsi, "Corso di Canto"), 9));

            // Nel Riepilogo Iscritti il corso con gruppi ha un livello in più: corso → gruppi → iscritti
            navigate(RiepilogoIscrittiView.class);
            var riepilogo = new com.vaadin.flow.component.treegrid.TreeGridTester<>(
                (com.vaadin.flow.component.treegrid.TreeGrid<RiepilogoIscrittiView.Voce>) find(com.vaadin.flow.component.treegrid.TreeGrid.class).single());
            // Canto (1 + 7 iscritti) senza gruppi; Chitarra: corso, 2 gruppi e 6 iscritti
            assertEquals(17, riepilogo.size());
            int corso = IntStream.range(0, riepilogo.size())
                .filter(r -> riepilogo.getRow(r).isCorso() && riepilogo.getCellText(r, 0).equals("Corso di Chitarra"))
                .findFirst().orElseThrow();
            assertEquals("mar, sab · 1 gruppo", riepilogo.getCellText(corso, 3));
            assertTrue(riepilogo.getRow(corso + 1).isGruppo());
            assertEquals("Principianti – sab 10:00", riepilogo.getCellText(corso + 1, 0));
            assertEquals("1", riepilogo.getCellText(corso + 1, 4));
            assertEquals("", riepilogo.getCellText(corso + 1, 5));
            assertEquals(allievo, riepilogo.getRow(corso + 2).iscritto().allievo());
            assertEquals("Principianti – sab 10:00", riepilogo.getCellText(corso + 2, 3));
            assertEquals("Senza gruppo (tutti i giorni)", riepilogo.getCellText(corso + 3, 0));
            assertEquals("5", riepilogo.getCellText(corso + 3, 4));

            // Comprimi tutti: restano i corsi; Espandi tutti riapre corsi e gruppi
            test(find(Button.class).withText("Comprimi tutti").single()).click();
            assertEquals(2, riepilogo.size());
            test(find(Button.class).withText("Espandi tutti").single()).click();
            assertEquals(17, riepilogo.size());
        } finally {
            corsoService.assegnaGruppo(chitarra.getId(), allievo.getId(), null);
            corsoService.impostaOrario(chitarra.getId(), allievo.getId(), null);
            corsoService.gruppi(chitarra.getId()).forEach(g -> corsoService.eliminaGruppo(g.getId()));
        }
    }

    @Test
    @WithUserDetails("admin")
    void admin_eliminaUnGruppo() {
        Corso chitarra = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Chitarra")).findFirst().orElseThrow();
        Allievo allievo = allievoService.findByUsername("allievo_chitarra1").orElseThrow();
        GruppoCorso gruppo = corsoService.salvaGruppo(chitarra.getId(), new GruppoCorso(null, null, DayOfWeek.TUESDAY, LocalTime.of(17, 0)));
        corsoService.assegnaGruppo(chitarra.getId(), allievo.getId(), gruppo.getId());
        try {
            PartecipantiDialog dialog = apriPartecipanti("Corso di Chitarra");
            Button elimina = find(Button.class).from(dialog).all().stream()
                .filter(b -> "Elimina il gruppo Martedì 17:00".equals(b.getElement().getAttribute("aria-label")))
                .findFirst().orElseThrow();
            test(elimina).click();
            test(find(Button.class).withText("Elimina").last()).click();
            assertTrue(corsoService.gruppi(chitarra.getId()).isEmpty());
            assertNull(iscrizione(chitarra, allievo).getGruppo());
            assertTrue(find(Span.class).from(dialog).withText("Nessun gruppo: tutti frequentano tutti i giorni del corso.").exists());
        } finally {
            corsoService.assegnaGruppo(chitarra.getId(), allievo.getId(), null);
            corsoService.impostaOrario(chitarra.getId(), allievo.getId(), null);
            corsoService.gruppi(chitarra.getId()).forEach(g -> corsoService.eliminaGruppo(g.getId()));
        }
    }

    /** Scelta del gruppo (terza colonna) del partecipante nella griglia dei partecipanti. */
    @SuppressWarnings("unchecked")
    private ComboBox<GruppoCorso> gruppoDi(PartecipantiDialog dialog, Allievo allievo) {
        var righe = test((Grid<Iscrizione>) find(Grid.class).from(dialog).single());
        int riga = IntStream.range(0, righe.size()).filter(r -> righe.getRow(r).getAllievo().equals(allievo)).findFirst().orElseThrow();
        return (ComboBox<GruppoCorso>) righe.getCellComponent(riga, 2);
    }

    private Iscrizione iscrizione(Corso corso, Allievo allievo) {
        return corsoService.findPartecipanti(corso.getId()).stream()
            .filter(i -> i.getAllievo().equals(allievo)).findFirst().orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private PartecipantiDialog apriPartecipanti(String corso) {
        navigate(CorsiView.class);
        var grid = test((Grid<Corso>) find(Grid.class).single());
        test(pulsante(grid.getCellComponent(riga(grid, corso), AZIONI_CORSO), "Partecipanti")).click();
        return find(PartecipantiDialog.class).single();
    }

    /** Testo non presente tra gli allievi scritto nel campo "Aggiungi un allievo" e confermato (Invio). */
    private void scriviNuovoPartecipante(PartecipantiDialog dialog, String testo) {
        ComboBox<Allievo> campo = find(ComboBox.class).from(dialog).all().stream()
            .filter(c -> c.getAriaLabel().filter("Allievo da aggiungere"::equals).isPresent()).findFirst().orElseThrow();
        com.vaadin.flow.component.ComponentUtil.fireEvent(campo,
            new com.vaadin.flow.component.combobox.ComboBoxBase.CustomValueSetEvent<>(campo, true, testo));
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
    void segreteria_assegnaAltriInsegnantiAlCorso() {
        navigate(CorsiView.class);
        var corsi = test((Grid<Corso>) find(Grid.class).single());
        int canto = riga(corsi, "Corso di Canto");
        Long corsoId = corsi.getRow(canto).getId();
        try {
            test(pulsante(corsi.getCellComponent(canto, AZIONI_CORSO), "Modifica")).click();
            MultiSelectComboBox<Insegnante> altri = find(MultiSelectComboBox.class).withLabel("Altri insegnanti").single();
            Insegnante maria = insegnanteService.findByUsername("insegnante1").orElseThrow();
            Insegnante marco = insegnanteService.findByUsername("insegnante2").orElseThrow();
            // Il titolare non va ripetuto tra gli altri
            altri.setValue(Set.of(maria));
            test(find(Button.class).withText("Salva").single()).click();
            assertTrue(altri.isInvalid());

            altri.setValue(Set.of(marco));
            test(find(Button.class).withText("Salva").single()).click();
            assertEquals("Maria Verdi, Marco Rossi", corsoService.findById(corsoId).getNomiInsegnanti());
            assertEquals("Maria Verdi, Marco Rossi", corsi.getCellText(riga(corsi, "Corso di Canto"), 2));
        } finally {
            Corso ripristino = corsoService.findById(corsoId);
            ripristino.setInsegnantiFacoltativi(java.util.List.of());
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
    void segreteria_filtraAllieviPerCorso() {
        Allievo senzaCorsi = allievoService.creaRapido("Zeno", "Senzacorsi", null, null);
        try {
            navigate(AllieviView.class);
            var allievi = test((Grid<Allievo>) find(Grid.class).single());
            ComboBox<AllieviView.FiltroCorso> filtro = find(ComboBox.class).withPlaceholder("Tutti i corsi").single();
            assertEquals(14, allievi.size());
            assertEquals(List.of("Senza corsi", "Corso di Canto (2024/2025)", "Corso di Chitarra (2024/2025)"),
                filtro.getListDataView().getItems().map(f -> filtro.getItemLabelGenerator().apply(f)).toList());

            test(filtro).selectItem("Corso di Chitarra (2024/2025)");
            assertEquals(6, allievi.size());
            assertTrue(IntStream.range(0, allievi.size()).allMatch(r -> allievi.getRow(r).getNomeCompleto().startsWith("Allievo Chitarra")));

            // Si combina con la ricerca rapida
            test(find(TextField.class).withPlaceholder("Cerca").single()).setValue("chitarra3");
            assertEquals(1, allievi.size());
            test(find(TextField.class).withPlaceholder("Cerca").single()).clear();

            test(filtro).selectItem("Senza corsi");
            assertEquals(1, allievi.size());
            assertEquals("Zeno Senzacorsi", allievi.getRow(0).getNomeCompleto());

            filtro.clear();
            assertEquals(14, allievi.size());
        } finally {
            allievoService.delete(allievoService.findById(senzaCorsi.getId()));
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

        test((Button) allievi.getCellComponent(riga, 7).getChildren().findFirst().orElseThrow()).click();
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
    void segreteria_schedaAllievoAdultoOConGenitore() {
        navigate(AllieviView.class);
        test(find(Button.class).withText("Nuovo Allievo").single()).click();
        Dialog dialog = find(Dialog.class).single();
        // Senza genitore: contatti e indirizzo propri in evidenza
        assertTrue(find(H3.class).from(dialog).withText("Contatti e indirizzo").exists());
        // Dati per i documenti: il codice fiscale e, per l'adulto, l'intestatario dell'attestazione in un riquadro chiuso
        assertTrue(find(H3.class).from(dialog).withText("Dati per i documenti").exists());
        var intestatario = find(com.vaadin.flow.component.details.Details.class).from(dialog).single();
        assertEquals("Intesta l'attestazione a un'altra persona", intestatario.getSummaryText());
        assertFalse(intestatario.isOpened());

        test(find(ComboBox.class).withLabel("Genitore").single()).selectItem("Genitore1 Cognome1");
        assertTrue(find(H3.class).from(dialog).withText("Contatti e indirizzo del genitore").exists());
        assertTrue(find(Span.class).from(dialog).withText("Indirizzo: Via Roma 1, 00184 Roma (RM)").exists());
        assertFalse(intestatario.getParent().orElseThrow().isVisible());
        var propri = find(com.vaadin.flow.component.details.Details.class).from(dialog).all().stream()
            .filter(d -> d.getSummaryText().contains("facoltativi")).findFirst().orElseThrow();
        assertFalse(propri.isOpened());
        assertTrue(find(TextField.class).from(propri).withLabel("Email").exists());
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
        assertEquals("Necessario per le ricevute", find(TextField.class).withLabel("Codice fiscale").single().getHelperText());
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
    @SuppressWarnings("unchecked")
    void segreteria_creaIlGenitoreAlVoloDalDialogDellAllievo() {
        navigate(AllieviView.class);
        test(find(Button.class).withText("Nuovo Allievo").single()).click();
        test(find(TextField.class).withLabel("Nome").single()).setValue("Pietro");
        test(find(TextField.class).withLabel("Cognome").single()).setValue("Gialli");
        // Un nome non presente nel campo "Genitore" apre il dialog con nome e cognome già scritti
        ComboBox<Genitore> campo = find(ComboBox.class).withLabel("Genitore").single();
        com.vaadin.flow.component.ComponentUtil.fireEvent(campo,
            new com.vaadin.flow.component.combobox.ComboBoxBase.CustomValueSetEvent<>(campo, true, "Laura Gialli"));
        Dialog nuovoGenitore = find(Dialog.class).all().stream().filter(d -> "Nuovo genitore".equals(d.getHeaderTitle()))
            .findFirst().orElseThrow();
        assertEquals("Laura", find(TextField.class).from(nuovoGenitore).withLabel("Nome").single().getValue());
        assertEquals("Gialli", find(TextField.class).from(nuovoGenitore).withLabel("Cognome").single().getValue());
        test(find(TextField.class).from(nuovoGenitore).withLabel("Cellulare").single()).setValue("333 7654321");
        test(find(Button.class).from(nuovoGenitore).withText("Crea").single()).click();

        Genitore genitore = genitoreService.omonimi("Laura", "Gialli").getFirst();
        Allievo nuovo = null;
        try {
            assertEquals(genitore, campo.getValue());
            assertEquals("333 7654321", genitore.getUser().getTelefono());
            assertFalse(genitore.getUser().isEnabled());
            test(find(Button.class).withText("Salva").single()).click();
            nuovo = allievoService.omonimi("Pietro", "Gialli").getFirst();
            assertEquals(genitore, nuovo.getGenitore());

            // Con il pulsante "+" il dialog si apre vuoto; un omonimo si può scegliere al posto di uno nuovo
            test(find(Button.class).withText("Nuovo Allievo").single()).click();
            test(find(Button.class).all().stream().filter(b -> "Nuovo genitore".equals(b.getElement().getAttribute("aria-label")))
                .findFirst().orElseThrow()).click();
            Dialog vuoto = find(Dialog.class).all().stream().filter(d -> "Nuovo genitore".equals(d.getHeaderTitle()))
                .findFirst().orElseThrow();
            assertEquals("", find(TextField.class).from(vuoto).withLabel("Nome").single().getValue());
            test(find(TextField.class).from(vuoto).withLabel("Nome").single()).setValue("laura");
            test(find(TextField.class).from(vuoto).withLabel("Cognome").single()).setValue("gialli");
            test(find(Button.class).from(vuoto).withText("Scegli questo").single()).click();
            assertEquals(genitore, find(ComboBox.class).withLabel("Genitore").single().getValue());
            assertEquals(1, genitoreService.omonimi("Laura", "Gialli").size());
        } finally {
            if (nuovo != null) {
                allievoService.delete(nuovo);
                userService.delete(nuovo.getUser());
            }
            genitoreService.elimina(genitore.getId());
            userService.delete(genitore.getUser());
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
    void segreteria_creaAllievoAdultoSenzaGenitore() {
        navigate(AllieviView.class);
        test(find(Button.class).withText("Nuovo Allievo").single()).click();
        test(find(TextField.class).withLabel("Nome").single()).setValue("Anna");
        test(find(TextField.class).withLabel("Cognome").single()).setValue("Adulta");
        assertTrue(find(ComboBox.class).withLabel("Genitore").single().getHelperText().startsWith("Lascia vuoto per gli allievi adulti"));
        test(find(Button.class).withText("Salva").single()).click();
        assertFalse(find(Dialog.class).exists());
        Allievo adulta = allievoService.findAll().stream()
            .filter(a -> a.getNomeCompleto().equals("Anna Adulta")).findFirst().orElseThrow();
        try {
            assertNull(adulta.getGenitore());
        } finally {
            allievoService.delete(adulta);
            userService.delete(adulta.getUser());
        }
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

        // Carattere di controllo del codice fiscale sbagliato: l'allievo (e il suo utente) non viene creato
        test(find(Button.class).withText("Salva").single()).click();
        assertFalse(find(ComboBox.class).withLabel("Genitore").single().isInvalid());
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
    void segreteria_impostaCadenzaDelCorso() {
        navigate(CorsiView.class);
        var corsi = test((Grid<Corso>) find(Grid.class).single());
        int canto = riga(corsi, "Corso di Canto");
        Long corsoId = corsi.getRow(canto).getId();
        assertEquals("gio", corsi.getCellText(canto, 4));

        test(pulsante(corsi.getCellComponent(canto, AZIONI_CORSO), "Modifica")).click();
        SegmentedControl<Integer> cadenza = find(SegmentedControl.class).all().stream()
            .filter(c -> c.getElement().getAttribute("aria-label").equals("Cadenza delle lezioni"))
            .findFirst().orElseThrow();
        assertEquals(1, cadenza.getValue());
        test(cadenza.getPulsante(2)).click();
        test(find(Button.class).withText("Salva").single()).click();
        try {
            assertEquals(2, corsoService.findById(corsoId).getCadenzaSettimane());
            assertEquals("gio, ogni 2 settimane", corsi.getCellText(riga(corsi, "Corso di Canto"), 4));
        } finally {
            Corso corso = corsoService.findById(corsoId);
            corso.setCadenzaSettimane(1);
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
        assertEquals("Attivo", ((Badge) corsi.getCellComponent(canto, 10)).getText());

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
            HasValue<?, CorsiView.FiltroStato> stato = filtroStatoCorsi();

            // default: solo i corsi attivi
            assertEquals(CorsiView.FiltroStato.ATTIVI, stato.getValue());
            assertEquals(1, corsi.size());
            assertEquals("Corso di Canto", corsi.getRow(0).getNome());

            stato.setValue(CorsiView.FiltroStato.NON_ATTIVI);
            assertEquals(1, corsi.size());
            assertEquals("Non attivo", ((Badge) corsi.getCellComponent(0, 10)).getText());

            stato.setValue(CorsiView.FiltroStato.TUTTI);
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
            filtroStatoCorsi().setValue(CorsiView.FiltroStato.NON_ATTIVI);
            assertFalse(pulsante(corsi.getCellComponent(0, AZIONI_CORSO), "Aggiorna lezioni").isEnabled());

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
    @WithUserDetails("admin")
    @SuppressWarnings("unchecked")
    void admin_modificaUnCorsoNonAttivo() {
        Corso chitarra = disattiva("Corso di Chitarra");
        Long allievo = allievoService.findByUsername("allievo_canto1").orElseThrow().getId();
        try {
            navigate(CorsiView.class);
            var corsi = test((Grid<Corso>) find(Grid.class).single());
            filtroStatoCorsi().setValue(CorsiView.FiltroStato.NON_ATTIVI);
            assertTrue(pulsante(corsi.getCellComponent(0, AZIONI_CORSO), "Aggiorna lezioni").isEnabled());

            test(pulsante(corsi.getCellComponent(0, AZIONI_CORSO), "Partecipanti")).click();
            PartecipantiDialog dialog = find(PartecipantiDialog.class).single();
            assertTrue(find(ComboBox.class).from(dialog).exists());
            assertTrue(find(TimePicker.class).from(dialog).all().stream().noneMatch(TimePicker::isReadOnly));
            dialog.close();

            corsoService.iscrivi(chitarra.getId(), allievo);
            corsoService.rimuoviIscrizione(chitarra.getId(), allievo);
            Presenza presenza = presenzaService.findByCorsoAndAllievo(chitarra.getId(),
                allievoService.findByUsername("allievo_chitarra1").orElseThrow().getId()).getFirst();
            StatoPresenza stato = presenza.getStato();
            presenza.setStato(StatoPresenza.ASSENTE_GIUSTIFICATA);
            presenzaService.save(presenza);
            presenza.setStato(stato);
            presenzaService.save(presenza);

            // In Lezioni il filtro offre anche il corso non attivo, con le sue lezioni
            navigate(LezioniView.class);
            Schede<FiltroLezioni> filtro = find(Schede.class).single();
            filtro.setValue(FiltroLezioni.TUTTE);
            var lezioni = test(find(Grid.class).single());
            ComboBox<Corso> filtroCorso = find(ComboBox.class).withPlaceholder("Tutti i corsi").single();
            Corso nonAttivo = filtroCorso.getListDataView().getItems()
                .filter(c -> c.getId().equals(chitarra.getId())).findFirst().orElseThrow();
            assertEquals("Corso di Chitarra (non attivo)", filtroCorso.getItemLabelGenerator().apply(nonAttivo));
            test(filtroCorso).selectItem("Corso di Chitarra (non attivo)");
            assertEquals(lezioneService.findByCorso(chitarra.getId()).size(), lezioni.size());
            assertTrue(lezioni.size() > 0);
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
            Schede<FiltroLezioni> filtro = find(Schede.class).single();
            filtro.setValue(FiltroLezioni.TUTTE);
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
            assertEquals("1", figli.getCellText(0, 3));
            assertEquals("1", figli.getCellText(1, 3));
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
            // Motivo nuovo scritto a mano (valore personalizzato della ComboBox)
            ComboBox<String> motivo = find(ComboBox.class).from(dialog).withLabel("Motivo").single();
            com.vaadin.flow.component.ComponentUtil.fireEvent(motivo,
                new com.vaadin.flow.component.combobox.ComboBoxBase.CustomValueSetEvent<>(motivo, true, " fratelli "));

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
    @SuppressWarnings("unchecked")
    void segreteria_motivoQuotaSuggerisceIMotiviGiaUsati() {
        Corso canto = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow();
        Corso chitarra = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Chitarra")).findFirst().orElseThrow();
        Long canto1 = allievoService.findByUsername("allievo_canto1").orElseThrow().getId();
        Long canto2 = allievoService.findByUsername("allievo_canto2").orElseThrow().getId();
        Long chitarra1 = allievoService.findByUsername("allievo_chitarra1").orElseThrow().getId();
        Allievo allievo = allievoService.findByUsername("allievo_canto3").orElseThrow();
        pagamentoService.impostaQuota(canto.getId(), canto1, new BigDecimal("500"), "Borsa di studio");
        pagamentoService.impostaQuota(canto.getId(), canto2, new BigDecimal("450"), "fratelli");
        pagamentoService.impostaQuota(chitarra.getId(), chitarra1, new BigDecimal("450"), "Fratelli ");
        try {
            // Senza doppioni (maiuscole e spazi), prima i più usati
            assertEquals(List.of("Fratelli", "Borsa di studio"), pagamentoService.motiviQuota().stream()
                .filter(m -> List.of("fratelli", "borsa di studio").contains(m.toLowerCase())).toList());

            new QuotaPagamentiDialog(pagamentoService, canto, allievo).open();
            QuotaPagamentiDialog dialog = find(QuotaPagamentiDialog.class).single();
            ComboBox<String> motivo = find(ComboBox.class).from(dialog).withLabel("Motivo").single();
            assertFalse(motivo.isEnabled());
            new TextFieldTester<>(find(BigDecimalField.class).from(dialog).withLabel("Quota personalizzata").single())
                .setValue(new BigDecimal("400"));
            assertTrue(motivo.isEnabled());
            assertTrue(motivo.getListDataView().getItems().toList().containsAll(List.of("Fratelli", "Borsa di studio")));
            test(motivo).selectItem("Borsa di studio");
            assertEquals("Borsa di studio", pagamentoService.findIscrizione(canto.getId(), allievo.getId()).getNotaQuota());
        } finally {
            pagamentoService.impostaQuota(canto.getId(), canto1, null, null);
            pagamentoService.impostaQuota(canto.getId(), canto2, null, null);
            pagamentoService.impostaQuota(chitarra.getId(), chitarra1, null, null);
            pagamentoService.impostaQuota(canto.getId(), allievo.getId(), null, null);
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
            // Ordine dei partecipanti: per orario (Canto5 alle 17:00 … Canto1 alle 19:40), poi senza orario per inserimento
            assertEquals(List.of("Allievo Canto5", "Allievo Canto4", "Allievo Canto3", "Allievo Canto2", "Allievo Canto1",
                    "Allievo Canto6", "Allievo Canto7"),
                IntStream.range(0, righe.size()).mapToObj(r -> righe.getRow(r).allievo().getNomeCompleto()).toList());

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
            Schede<ElencoPagamenti.FiltroPagamento> filtroPagamento = find(Schede.class).single();
            filtroPagamento.setValue(ElencoPagamenti.FiltroPagamento.PARZIALE);
            assertEquals(1, righe.size());
            assertEquals(allievo, righe.getRow(0).allievo());
            filtroPagamento.setValue(ElencoPagamenti.FiltroPagamento.TUTTI);
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
            // Schede per lo stato del pagamento, nessun filtro per stato del corso
            assertFalse(find(Select.class).exists());
            Schede<ElencoPagamenti.FiltroPagamento> stato = find(Schede.class).single();
            stato.setValue(ElencoPagamenti.FiltroPagamento.SALDATO);
            assertEquals(0, righe.size());
            stato.setValue(ElencoPagamenti.FiltroPagamento.TUTTI);
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
            assertTrue(find(SideNavItem.class).all().stream().anyMatch(i -> "Iscritti".equals(i.getLabel())));
            navigate(RiepilogoIscrittiView.class);
            var grid = new com.vaadin.flow.component.treegrid.TreeGridTester<>(
                (com.vaadin.flow.component.treegrid.TreeGrid<RiepilogoIscrittiView.Voce>) find(com.vaadin.flow.component.treegrid.TreeGrid.class).single());

            // Corsi attivi dell'anno più recente, aperti: Canto (7 iscritti) e Chitarra (6)
            assertEquals(15, grid.size());
            assertTrue(grid.getRow(0).isCorso());
            assertTrue(grid.isExpanded(0));
            assertEquals("Corso di Canto", grid.getCellText(0, 0));
            assertEquals("gio", grid.getCellText(0, 3));
            assertEquals("7", grid.getCellText(0, 4));
            assertEquals("4.200,00 €", grid.getCellText(0, 5));
            assertEquals("600,00 €", grid.getCellText(0, 6));
            assertEquals("3.600,00 €", grid.getCellText(0, 7));
            // Preventivo: costo stimato dell'insegnante (30 lezioni × 7 allievi × 40 min a 25 €/h) e margine
            assertEquals("3.500,00 €", grid.getCellText(0, 9));
            assertEquals("700,00 €", grid.getCellText(0, 10));
            assertEquals("", grid.getCellText(1, 9));
            int riga = IntStream.range(1, 8).filter(r -> grid.getRow(r).iscritto().allievo().equals(allievo)).findFirst().orElseThrow();
            assertEquals("600,00 €", grid.getCellText(riga, 6));
            assertEquals("0,00 €", grid.getCellText(riga, 7));
            assertTrue(grid.getRow(8).isCorso());
            assertEquals("Corso di Chitarra", grid.getCellText(8, 0));

            // Comprimi tutti: restano le righe dei corsi
            test(find(Button.class).withText("Comprimi tutti").single()).click();
            assertEquals(2, grid.size());
            grid.expand(1);
            assertEquals(8, grid.size());

            // Corsi non attivi dell'anno: nessuno
            HasValue<?, CorsiView.FiltroStato> stato = filtroStatoCorsi();
            stato.setValue(CorsiView.FiltroStato.NON_ATTIVI);
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
    void segreteria_festivitaNuovaProponeDiAggiornareLeLezioni() {
        Corso canto = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow();
        LocalDate giorno = lezioneService.findByCorso(canto.getId()).get(3).getData();
        navigate(FestivitaView.class);
        test(find(Button.class).withText("Nuova Festività").single()).click();
        @SuppressWarnings("rawtypes")
        SegmentedControl<Festivita.Tipo> tipo = find(SegmentedControl.class).single();
        test(tipo.getPulsante(Festivita.Tipo.DATE)).click();
        test(find(TextField.class).withLabel("Nome").single()).setValue("Chiusura di prova");
        test(find(DatePicker.class).withLabel("Dal").single()).setValue(giorno);
        test(find(Button.class).withText("Salva").single()).click();
        Festivita nuova = festivitaService.elenco().stream()
            .filter(f -> f.getNome().equals("Chiusura di prova")).findFirst().orElseThrow();
        try {
            Dialog proposta = find(Dialog.class).all().stream()
                .filter(d -> "Aggiornare le lezioni?".equals(d.getHeaderTitle())).findFirst().orElseThrow();
            assertTrue(find(Span.class).from(proposta).withText("Corso di Canto").exists());
            test(find(Button.class).from(proposta).withText("Aggiorna tutti").single()).click();
            assertTrue(lezioneService.findByCorso(canto.getId()).stream().noneMatch(l -> l.getData().equals(giorno)));
            assertEquals(30, lezioneService.findByCorso(canto.getId()).size());
        } finally {
            festivitaService.elimina(nuova.getId());
            corsoService.aggiornaLezioni(canto.getId());
        }
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
    @WithUserDetails("insegnante1")
    void insegnante_prossimaLezioneApreIlRegistro() {
        Corso canto = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow();
        apriLezioni();
        navigate(HomeView.class);
        // Le lezioni generate sono passate: nessuna prossima lezione
        assertFalse(find(H2.class).all().stream().anyMatch(h -> h.getText().startsWith("Prossim")));
        Lezione domani = lezioneService.aggiungi(canto.getId(), LocalDate.now().plusDays(1), LocalTime.of(18, 0), null, "insegnante1");
        try {
            apriLezioni();
            navigate(HomeView.class);
            assertTrue(find(H2.class).withText("Prossima lezione").exists());
            Card card = prossimaLezione("Corso di Canto");
            assertTrue(find(Badge.class).from(card).withText("Domani").exists());
            ComponentUtil.fireEvent(card, new ClickEvent<>(card));
            // Il registro delle presenze di quella lezione
            assertTrue(find(Dialog.class).single().getHeaderTitle().startsWith("Corso di Canto – Lezione "));
        } finally {
            lezioneService.eliminaAggiunta(domani.getId(), "insegnante1");
        }
    }

    @Test
    @WithUserDetails("genitore3")
    void genitore_prossimaLezioneDelFiglioApreIlDettaglio() {
        Corso canto = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow();
        Lezione oggi = lezioneService.aggiungi(canto.getId(), LocalDate.now(), LocalTime.of(18, 0), null, "insegnante1");
        try {
            navigate(FigliView.class);
            navigate(HomeView.class);
            Card card = prossimaLezione("Corso di Canto");
            assertTrue(card.getElement().getAttribute("aria-label").contains("Allievo Canto3"));
            assertTrue(find(Badge.class).from(card).withText("Oggi").exists());
            ComponentUtil.fireEvent(card, new ClickEvent<>(card));
            Dialog dettaglio = find(Dialog.class).single();
            assertEquals("Corso di Canto", dettaglio.getHeaderTitle());
            // Sola lettura: niente registro per il genitore
            assertFalse(find(Button.class).from(dettaglio).withText("Gestisci presenze").exists());
        } finally {
            lezioneService.eliminaAggiunta(oggi.getId(), "insegnante1");
        }
    }

    @Test
    @WithUserDetails("insegnante2")
    @SuppressWarnings("unchecked")
    void insegnante_prossimaLezioneUnRiquadroPerGruppo() {
        // Chitarra il prossimo sabato: Piccoli alle 10 (Chitarra1), Grandi alle 11 (Chitarra3), gli altri senza gruppo
        Corso chitarra = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Chitarra")).findFirst().orElseThrow();
        GruppoCorso piccoli = corsoService.salvaGruppo(chitarra.getId(), new GruppoCorso(null, "Piccoli", DayOfWeek.SATURDAY, LocalTime.of(10, 0)));
        GruppoCorso grandi = corsoService.salvaGruppo(chitarra.getId(), new GruppoCorso(null, "Grandi", DayOfWeek.SATURDAY, LocalTime.of(11, 0)));
        List<Allievo> assegnati = List.of("allievo_chitarra1", "allievo_chitarra3").stream()
            .map(u -> allievoService.findByUsername(u).orElseThrow()).toList();
        corsoService.assegnaGruppo(chitarra.getId(), assegnati.get(0).getId(), piccoli.getId());
        corsoService.assegnaGruppo(chitarra.getId(), assegnati.get(1).getId(), grandi.getId());
        Lezione sabato = lezioneService.aggiungi(chitarra.getId(), prossimoSabato(), null, null, "insegnante2");
        try {
            apriLezioni();
            navigate(HomeView.class);
            List<String> riquadri = find(Card.class).all().stream()
                .filter(c -> "button".equals(c.getElement().getAttribute("role")))
                .map(c -> c.getElement().getAttribute("aria-label")).toList();
            String data = Formati.data(sabato.getData());
            int numero = sabato.getNumeroLezione();
            assertEquals(List.of("Corso di Chitarra: " + data + ", 10:00, Lezione " + numero + " · Piccoli · Aula 2",
                "Corso di Chitarra: " + data + ", 11:00, Lezione " + numero + " · Grandi · Aula 2",
                "Corso di Chitarra: " + data + ", 18:00, Lezione " + numero + " · Senza gruppo · Aula 2"), riquadri);

            // Il riquadro dei Grandi apre il registro sulla loro scheda
            Card card = find(Card.class).all().stream()
                .filter(c -> Objects.toString(c.getElement().getAttribute("aria-label"), "").endsWith("· Grandi · Aula 2")).findFirst().orElseThrow();
            ComponentUtil.fireEvent(card, new ClickEvent<>(card));
            Dialog registro = find(Dialog.class).single();
            assertEquals("Grandi 11:00 (1)", find(Tabs.class).from(registro).single().getSelectedTab().getLabel());
            assertEquals(1, test((Grid<Presenza>) find(Grid.class).from(registro).single()).size());
        } finally {
            lezioneService.eliminaAggiunta(sabato.getId(), "insegnante2");
            corsoService.eliminaGruppo(piccoli.getId());
            corsoService.eliminaGruppo(grandi.getId());
            assegnati.forEach(a -> corsoService.impostaOrario(chitarra.getId(), a.getId(), null));
        }
    }

    @Test
    @WithUserDetails("genitore3")
    void genitore_prossimaLezioneDelFiglioConIlSuoGruppo() {
        // Chitarra1, figlio di genitore3, nei Piccoli alle 10 senza un orario suo
        Corso chitarra = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Chitarra")).findFirst().orElseThrow();
        GruppoCorso piccoli = corsoService.salvaGruppo(chitarra.getId(), new GruppoCorso(null, "Piccoli", DayOfWeek.SATURDAY, LocalTime.of(10, 0)));
        GruppoCorso grandi = corsoService.salvaGruppo(chitarra.getId(), new GruppoCorso(null, "Grandi", DayOfWeek.SATURDAY, LocalTime.of(11, 0)));
        Allievo figlio = allievoService.findByUsername("allievo_chitarra1").orElseThrow();
        corsoService.assegnaGruppo(chitarra.getId(), figlio.getId(), piccoli.getId());
        corsoService.impostaOrario(chitarra.getId(), figlio.getId(), null);
        Lezione sabato = lezioneService.aggiungi(chitarra.getId(), prossimoSabato(), null, null, "insegnante2");
        try {
            navigate(FigliView.class);
            navigate(HomeView.class);
            Card card = prossimaLezione("Corso di Chitarra");
            assertEquals("Corso di Chitarra: " + Formati.data(sabato.getData()) + ", 10:00, Lezione " + sabato.getNumeroLezione()
                + " · Piccoli · Allievo Chitarra1 · Aula 2", card.getElement().getAttribute("aria-label"));
            ComponentUtil.fireEvent(card, new ClickEvent<>(card));
            Dialog dettaglio = find(Dialog.class).single();
            assertTrue(find(Span.class).from(dettaglio).withText("Piccoli – sab 10:00").exists());
            assertTrue(find(Span.class).from(dettaglio).withText(Formati.data(sabato.getData()) + ", 10:00").exists());
        } finally {
            lezioneService.eliminaAggiunta(sabato.getId(), "insegnante2");
            corsoService.eliminaGruppo(piccoli.getId());
            corsoService.eliminaGruppo(grandi.getId());
        }
    }

    private static LocalDate prossimoSabato() {
        LocalDate sabato = LocalDate.now().plusDays(1);
        while (sabato.getDayOfWeek() != DayOfWeek.SATURDAY) {
            sabato = sabato.plusDays(1);
        }
        return sabato;
    }

    @Test
    @WithUserDetails("insegnante1")
    void insegnanteSenzaTariffa_vedeLeOreDonate() {
        apriLezioni();
        navigate(HomeView.class);
        // Con la tariffa nessun riquadro
        assertFalse(oreDonate().isPresent());
        Insegnante insegnante = insegnanteService.findByUsername("insegnante1").orElseThrow();
        insegnante.setTariffaOraria(null);
        insegnanteService.save(insegnante);
        try {
            apriLezioni();
            navigate(HomeView.class);
            // Canto: 30 lezioni × 7 allievi × 40 minuti = 140 ore previste in grande, nessuna svolta
            assertEquals("Ore donate: 140 h (di cui 0 h già svolte)", oreDonate().orElseThrow().getElement().getAttribute("aria-label"));
        } finally {
            Insegnante ripristino = insegnanteService.findByUsername("insegnante1").orElseThrow();
            ripristino.setTariffaOraria(new BigDecimal("25.00"));
            insegnanteService.save(ripristino);
        }
    }

    @Test
    @WithUserDetails("segreteria")
    void segreteria_vedeIlTotaleDelleOreDonate() {
        navigate(CorsiView.class);
        navigate(HomeView.class);
        // Tutti gli insegnanti hanno la tariffa: nessun riquadro
        assertFalse(oreDonate().isPresent());
        Insegnante insegnante = insegnanteService.findByUsername("insegnante1").orElseThrow();
        insegnante.setTariffaOraria(null);
        insegnanteService.save(insegnante);
        try {
            navigate(CorsiView.class);
            navigate(HomeView.class);
            // Il solo insegnante senza tariffa: le 140 ore previste di Canto
            assertEquals("Ore donate: 140 h (di cui 0 h già svolte · 1 insegnante)",
                oreDonate().orElseThrow().getElement().getAttribute("aria-label"));
        } finally {
            Insegnante ripristino = insegnanteService.findByUsername("insegnante1").orElseThrow();
            ripristino.setTariffaOraria(new BigDecimal("25.00"));
            insegnanteService.save(ripristino);
        }
    }

    private java.util.Optional<Card> oreDonate() {
        return find(Card.class).all().stream()
            .filter(c -> Objects.toString(c.getElement().getAttribute("aria-label"), "").startsWith("Ore donate")).findFirst();
    }

    /** Riquadro della prossima lezione del corso indicato, in dashboard. */
    private Card prossimaLezione(String corso) {
        return find(Card.class).all().stream()
            .filter(c -> "button".equals(c.getElement().getAttribute("role"))
                && c.getElement().getAttribute("aria-label").startsWith(corso + ":"))
            .findFirst().orElseThrow();
    }

    @Test
    @WithAnonymousUser
    void login_ricordamiNelFormSopraAccedi() {
        navigate(LoginView.class);
        Checkbox ricordami = find(Checkbox.class).withLabel("Ricordami su questo dispositivo").single();
        // Dentro il form, nello slot tra la password e il pulsante di invio (prima di "Password dimenticata?")
        assertTrue(ricordami.getParent().orElseThrow() instanceof LoginForm);
        assertEquals("custom-form-area", ricordami.getElement().getAttribute("slot"));
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
        assertEquals("Corsi | Area Corsi Mosson", UI.getCurrent().getInternals().getTitle());
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
        assertEquals("Nome utente o email", i18n.getForm().getUsername());
        assertEquals("Accedi", i18n.getForm().getSubmit());
        assertEquals("Password dimenticata?", i18n.getForm().getForgotPassword());
        // "Ricordami" non spuntato di default
        Checkbox ricordami = find(Checkbox.class).withLabel("Ricordami su questo dispositivo").single();
        assertFalse(ricordami.getValue());
        assertEquals("Credenziali errate", i18n.getErrorMessage().getTitle());
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_aggiungeLuoghiSenzaElimina() {
        navigate(HomeView.class);
        assertTrue(find(SideNavItem.class).all().stream().anyMatch(v -> v.getLabel().equals("Luoghi")));
        navigate(LuoghiView.class);
        var luoghi = test((Grid<Luogo>) find(Grid.class).single());
        int quanti = luoghi.size();
        assertTrue(find(Button.class).all().stream().noneMatch(b -> "Elimina".equals(b.getElement().getAttribute("aria-label"))));

        test(find(Button.class).withText("Nuovo Luogo").single()).click();
        test(find(TextField.class).withLabel("Nome").single()).setValue("Aula di prova");
        test(find(Button.class).withText("Salva").single()).click();
        Luogo nuovo = luogoService.elenco().stream().filter(l -> l.getNome().equals("Aula di prova")).findFirst().orElseThrow();
        try {
            assertEquals(quanti + 1, luoghi.size());
            // Doppione: errore sul campo, nulla salvato
            test(find(Button.class).withText("Nuovo Luogo").single()).click();
            TextField nome = find(TextField.class).withLabel("Nome").single();
            test(nome).setValue("aula di PROVA");
            test(find(Button.class).withText("Salva").single()).click();
            assertTrue(nome.isInvalid());
            assertEquals(quanti + 1, luogoService.elenco().size());
        } finally {
            luogoService.elimina(nuovo.getId());
        }
    }

    @Test
    @WithUserDetails("insegnante1")
    void insegnante_nonAccedeAiLuoghi() {
        navigate("luoghi", RouteNotFoundError.class);
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
    void insegnante_vedeLeLezioniDelMese() {
        navigate(MieiRimborsiView.class);
        test(find(Button.class).withText("Lezioni del mese").single()).click();
        Dialog dialog = find(Dialog.class).single();
        @SuppressWarnings("unchecked")
        ComboBox<YearMonth> mese = find(ComboBox.class).from(dialog).withLabel("Mese").single();
        mese.setValue(YearMonth.of(2024, 10));
        assertTrue(find(Span.class).from(dialog).all().stream()
            .anyMatch(s -> s.getText().startsWith("Totale di ottobre 2024: ") && s.getText().contains("preventivo")),
            "Totale del mese con il preventivo");
    }

    @Test
    @WithUserDetails("insegnante1")
    void insegnante_nonAccedeAiRimborsiDiTutti() {
        navigate("rimborsi", RouteNotFoundError.class);
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_documenti_conferme_PdfInvioEUltimoInvio() {
        navigate(DocumentiView.class);
        try {
            var documenti = test((Grid<DocumentoIscrizione>) find(Grid.class).single());
            // Conferme di iscrizione dell'anno più recente: tutti i 13 iscritti di Canto e Chitarra
            assertEquals("2024/2025", this.<String>filtro("Filtra per anno scolastico").getValue());
            assertEquals(13, documenti.size());
            assertFalse(find(Button.class).withText("Invia conferme di iscrizione a tutti").exists());

            test(find(TextField.class).withPlaceholder("Cerca").single()).setValue("canto3");
            assertEquals(1, documenti.size());
            assertEquals("genitore3@example.com", documenti.getCellText(0, 3));
            assertEquals("", documenti.getCellText(0, 4));
            test(pulsante(documenti.getCellComponent(0, 6), "Invia per email")).click();
            Dialog dialog = find(Dialog.class).single();
            assertEquals("Invia: Conferma di iscrizione – Allievo Canto3", dialog.getHeaderTitle());
            test(find(Button.class).withText("Anteprima").from(dialog).single()).click();
            Dialog anteprima = find(Dialog.class).all().stream()
                .filter(d -> "Anteprima – Conferma di iscrizione".equals(d.getHeaderTitle())).findFirst().orElseThrow();
            anteprima.close();
            assertTrue(emailService.tuttiGliInvii().isEmpty());
            test(find(Button.class).withText("Invia conferma").single()).click();
            assertEquals(1, emailService.tuttiGliInvii().size());
            // L'elenco si aggiorna con l'ultimo invio (simulato: SMTP non configurato nei test)
            assertTrue(documenti.getCellText(0, 4).endsWith("(simulato)"), documenti.getCellText(0, 4));

            // Scelto un corso, l'invio delle conferme a tutti i suoi iscritti
            test(find(TextField.class).withPlaceholder("Cerca").single()).clear();
            test(this.<Corso>filtro("Filtra per corso")).selectItem("Corso di Canto");
            assertEquals(7, documenti.size());
            test(find(Button.class).withText("Invia conferme di iscrizione a tutti").single()).click();
            assertTrue(find(Button.class).withText("Invia 7 conferme di iscrizione").exists());
        } finally {
            invioEmailRepository.deleteAll();
        }
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_documenti_attestazioniPerAnnoSolare() {
        Corso canto = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow();
        Allievo allievo = allievoService.findByUsername("allievo_canto3").orElseThrow();
        Pagamento rata = new Pagamento();
        rata.setData(LocalDate.of(2025, 2, 3));
        rata.setImporto(new BigDecimal("90"));
        rata.setMetodo(MetodoPagamento.BONIFICO);
        pagamentoService.salva(canto.getId(), allievo.getId(), rata);
        try {
            navigate(DocumentiView.class);
            scegliTipo(TipoDocumento.ATTESTAZIONE);
            var attestazioni = test((Grid<ElencoDocumentiService.DocumentoAttestazione>) find(Grid.class).single());
            test(find(TextField.class).withPlaceholder("Cerca").single()).setValue("canto3");
            assertEquals(1, attestazioni.size());
            assertEquals("Genitore3 Cognome3", attestazioni.getCellText(0, 1));
            assertEquals("2025", attestazioni.getCellText(0, 2));
            assertEquals("90,00 €", attestazioni.getCellText(0, 3));
            test(pulsante(attestazioni.getCellComponent(0, 7), "Invia per email")).click();
            assertTrue(find(Button.class).withText("Anteprima").exists());
        } finally {
            pagamentoService.findPagamenti(canto.getId(), allievo.getId()).forEach(pagamentoService::elimina);
        }
    }

    @Test
    @WithUserDetails("admin")
    @SuppressWarnings("unchecked")
    void admin_documenti_riepiloghiConInvio() {
        navigate(DocumentiView.class);
        scegliTipo(TipoDocumento.RIEPILOGO);
        try {
            var riepiloghi = test((Grid<DocumentoRiepilogo>) find(Grid.class).single());
            test(find(TextField.class).withPlaceholder("Cerca").single()).setValue("chitarra1");
            assertEquals(1, riepiloghi.size());
            assertEquals("Allievo Canto3, Allievo Chitarra1", riepiloghi.getCellText(0, 1));
            assertEquals("2", riepiloghi.getCellText(0, 3));
            assertEquals("genitore3@example.com", riepiloghi.getCellText(0, 4));
            assertFalse(find(ComboBox.class).all().stream().anyMatch(c -> c.getAriaLabel().filter("Filtra per corso"::equals).isPresent()));
            test(pulsante(riepiloghi.getCellComponent(0, 6), "Invia per email")).click();
            assertTrue(find(Dialog.class).single().getHeaderTitle().startsWith("Invia: Riepilogo pagamenti – "));
            test(find(Button.class).withText("Invia riepilogo").single()).click();
            InvioEmail invio = emailService.tuttiGliInvii().getFirst();
            assertEquals(TipoDocumento.RIEPILOGO, invio.getTipo());
            assertEquals("2024/2025", invio.getAnno());
            assertFalse(riepiloghi.getCellText(0, 5).isEmpty());
        } finally {
            invioEmailRepository.deleteAll();
        }
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_documenti_riepilogoCorsiConPdfEInvioAMe() throws IOException {
        navigate(DocumentiView.class);
        scegliTipo(TipoDocumento.RIEPILOGO_CORSI);
        try {
            // Nessun elenco né filtri: un solo documento con PDF (anteprima e scarico) e invio alla propria email
            assertFalse(find(Grid.class).exists());
            assertFalse(find(TextField.class).withPlaceholder("Cerca").exists());
            MenuBar pdf = find(MenuBar.class).single();
            assertLinkPdf(pdf);
            apriLink(find(com.vaadin.flow.component.html.Anchor.class).from(pdf).all().getFirst());
            assertTrue(find(Span.class).withText("Mai inviato per email").exists());
            test(find(Button.class).withText("Invia alla mia email").single()).click();
            assertTrue(find(Paragraph.class).all().stream().anyMatch(p -> p.getText().contains("segreteria@example.com")));
            test(find(Button.class).withText("Invia").single()).click();
            InvioEmail invio = emailService.tuttiGliInvii().getFirst();
            assertEquals(TipoDocumento.RIEPILOGO_CORSI, invio.getTipo());
            assertEquals("segreteria", invio.getInviatoDa());
            assertTrue(find(Span.class).all().stream().anyMatch(s -> s.getText().startsWith("Ultimo invio: ")));
            // Tornando a un altro tipo i filtri ricompaiono
            scegliTipo(TipoDocumento.CONFERMA_ISCRIZIONE);
            assertTrue(find(TextField.class).withPlaceholder("Cerca").exists());
        } finally {
            invioEmailRepository.deleteAll();
        }
    }

    @Test
    @WithUserDetails("genitore3")
    @SuppressWarnings("unchecked")
    void genitore_documenti_riepilogoEConfermeDeiFigliSenzaInvii() {
        navigate(DocumentiView.class);
        var documenti = test((Grid<DocumentoFamiglia>) find(Grid.class).single());
        // Riepilogo della famiglia per primo, poi le conferme dei due figli (nessun pagamento: niente ricevute)
        assertEquals(3, documenti.size());
        assertEquals("Riepilogo pagamenti", documenti.getCellText(0, 0));
        assertEquals("Tutti i figli", documenti.getCellText(0, 1));
        assertEquals("Conferma di iscrizione", documenti.getCellText(1, 0));
        assertEquals("Allievo Canto3", documenti.getCellText(1, 1));
        assertFalse(find(Schede.class).exists());
        assertFalse(find(Button.class).all().stream()
            .anyMatch(b -> "Invia per email".equals(b.getElement().getAttribute("aria-label"))));
        // Un solo anno: niente filtro
        assertFalse(find(ComboBox.class).exists());
        assertTrue(voceDiMenu("Documenti"));
    }

    @Test
    @WithUserDetails("allievo_canto3")
    void allievoConGenitore_documentiNellAreaDelGenitore() {
        assertFalse(voceDiMenu("Documenti"));
        navigate(DocumentiView.class);
        assertTrue(find(Span.class).withText("I documenti dei tuoi corsi sono nell'area del genitore.").exists());
        assertFalse(find(Grid.class).exists());
    }

    @Test
    @WithUserDetails("admin")
    void admin_menuAGruppiConImpostazioniChiuse() {
        navigate(HomeView.class);
        List<SideNav> gruppi = find(SideNav.class).all();
        assertEquals(List.of("Corsi e lezioni", "Persone", "Economia", "Documenti", "Impostazioni"),
            gruppi.stream().map(SideNav::getLabel).filter(java.util.Objects::nonNull).toList());
        SideNav impostazioni = gruppi.stream().filter(g -> "Impostazioni".equals(g.getLabel())).findFirst().orElseThrow();
        assertTrue(impostazioni.isCollapsible());
        assertFalse(impostazioni.isExpanded());
        // Arrivando su una sua pagina il gruppo si apre
        navigate(FestivitaView.class);
        assertTrue(impostazioni.isExpanded());
    }

    @Test
    @WithUserDetails("genitore3")
    void genitore_menuSempliceSenzaTitoli() {
        navigate(HomeView.class);
        assertTrue(find(SideNav.class).all().stream().allMatch(g -> g.getLabel() == null));
        assertTrue(voceDiMenu("Documenti"));
    }

    private boolean voceDiMenu(String titolo) {
        return find(SideNavItem.class).all().stream()
            .anyMatch(voce -> titolo.equals(voce.getLabel()));
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_emetteUnaLetteraDiIncarico() {
        navigate(DocumentiView.class);
        scegliTipo(TipoDocumento.LETTERA_INCARICO);
        test(find(Button.class).withText("Nuova lettera di incarico").single()).click();
        test((ComboBox<Corso>) find(ComboBox.class).withLabel("Corso o servizio").single()).selectItem("Corso di Canto (2024/2025)");
        // Un solo insegnante: scelto da sé, con la sua tariffa e i giorni del corso proposti
        assertEquals("Maria Verdi", ((ComboBox<Insegnante>) find(ComboBox.class).withLabel("Insegnante").single()).getValue().getNomeCompleto());
        assertEquals(0, new BigDecimal("25").compareTo(find(BigDecimalField.class).withLabel("Tariffa oraria (€/ora)").single().getValue()));
        assertEquals("giovedì alle 17:00, lezioni da 40 minuti", find(TextField.class).withLabel("Giorni e orari").single().getValue());
        test(find(Button.class).withText("Salva").single()).click();
        var lettere = test((Grid<LetteraIncarico>) find(Grid.class).single());
        try {
            assertEquals(1, lettere.size());
            assertEquals("25,00 €/ora", lettere.getCellText(0, 3));
        } finally {
            letteraIncaricoService.elimina(lettere.getRow(0).getId(), "admin");
        }
    }

    @Test
    @WithUserDetails("insegnante1")
    void insegnante_vedeLeSueLettereDiIncarico() {
        navigate(HomeView.class);
        assertTrue(find(SideNavItem.class).all().stream().anyMatch(v -> v.getLabel().equals("Documenti")));
        navigate(DocumentiView.class);
        assertTrue(find(H3.class).withText("Lettere di incarico").exists());
        assertFalse(find(Schede.class).exists());
    }

    @SuppressWarnings("unchecked")
    private <T> ComboBox<T> filtro(String ariaLabel) {
        return find(ComboBox.class).all().stream()
            .filter(c -> c.getAriaLabel().filter(ariaLabel::equals).isPresent()).findFirst().orElseThrow();
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_inviaConfermaDaDocumenti_ricevutaSoloConPagamenti() {
        Allievo allievo = allievoService.findByUsername("allievo_canto3").orElseThrow();
        // Gestione Pagamenti non ha più invii né ricevute: sono in Documenti
        navigate(GestionePagamentiView.class);
        var righe = test((Grid<RigaPagamento>) find(Grid.class).single());
        assertThrows(AssertionError.class,
            () -> pulsante(righe.getCellComponent(rigaDi(righe, allievo), "azioni"), "Email: conferma o ricevuta"));
        navigate(DocumentiView.class);
        try {
            // Senza pagamenti l'allievo non è tra le ricevute
            scegliTipo(TipoDocumento.RICEVUTA);
            test(find(TextField.class).withPlaceholder("Cerca").single()).setValue("canto3");
            var ricevute = test((Grid<DocumentoIscrizione>) find(Grid.class).single());
            assertEquals(0, ricevute.size());
            scegliTipo(TipoDocumento.CONFERMA_ISCRIZIONE);
            var conferme = test((Grid<DocumentoIscrizione>) find(Grid.class).single());
            test(pulsante(conferme.getCellComponent(0, 6), "Invia per email")).click();
            // Destinatario: il genitore
            assertTrue(find(Span.class).all().stream()
                .anyMatch(sp -> sp.getText().startsWith("A: ") && sp.getText().endsWith("<genitore3@example.com>")));
            assertTrue(find(Span.class).withText("Server email non configurato: l'invio sarà solo simulato e registrato.").exists());

            // Anteprima della conferma: destinatario, oggetto e testo dell'email, nessun invio
            test(pulsanteAnteprima("Conferma di iscrizione")).click();
            Dialog anteprima = find(Dialog.class).all().stream()
                .filter(d -> "Anteprima – Conferma di iscrizione".equals(d.getHeaderTitle())).findFirst().orElseThrow();
            assertTrue(find(Span.class).from(anteprima)
                .withText("Oggetto: Conferma di iscrizione – Corso di Canto 2024/2025 – Allievo Canto3").exists());
            String corpo = find(com.vaadin.flow.component.html.IFrame.class).from(anteprima).single().getSrcdoc().orElseThrow();
            assertTrue(corpo.contains("Allievo Canto3"));
            assertTrue(emailService.tuttiGliInvii().isEmpty());
            anteprima.close();
            test(find(Button.class).withText("Invia conferma").single()).click();

            List<InvioEmail> invii = emailService.tuttiGliInvii();
            assertEquals(1, invii.size());
            assertEquals(InvioEmail.Esito.SIMULATA, invii.getFirst().getEsito());
            assertEquals("segreteria", invii.getFirst().getInviatoDa());
            assertFalse(find(Button.class).withText("Invia conferma").exists());
        } finally {
            invioEmailRepository.deleteAll();
        }
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_inviaLeConfermeATuttiGliIscritti() {
        // Non più da Partecipanti, ma da Documenti scelto il corso
        navigate(CorsiView.class);
        var corsi = test((Grid<Corso>) find(Grid.class).single());
        test(pulsante(corsi.getCellComponent(riga(corsi, "Corso di Canto"), AZIONI_CORSO), "Partecipanti")).click();
        assertFalse(find(Button.class).withText("Invia conferme di iscrizione a tutti").exists());
        find(Dialog.class).single().close();
        navigate(DocumentiView.class);
        test(this.<Corso>filtro("Filtra per corso")).selectItem("Corso di Canto");
        try {
            test(find(Button.class).withText("Invia conferme di iscrizione a tutti").single()).click();
            // Chi la riceve, con l'anteprima sul primo (Canto5, prima per orario)
            assertTrue(find(Span.class).withText("→ " + genitoreService.findByUsername("genitore5").orElseThrow().getNomeCompleto()
                + " <genitore5@example.com>").exists());
            assertEquals("Anteprima della conferma per Allievo Canto5",
                find(Button.class).withText("Anteprima").single().getTooltip().getText());
            test(find(Button.class).withText("Invia 7 conferme di iscrizione").single()).click();
            assertEquals(7, emailService.tuttiGliInvii().size());
        } finally {
            invioEmailRepository.deleteAll();
        }
    }

    /** Pulsante "Anteprima" del tipo di email indicato, nel dialog di invio del singolo iscritto. */
    private Button pulsanteAnteprima(String tipo) {
        return find(Button.class).all().stream()
            .filter(b -> ("Anteprima: " + tipo).equals(b.getElement().getAttribute("aria-label")))
            .findFirst().orElseThrow();
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
            assertTrue(find(SideNavItem.class).all().stream().anyMatch(v -> v.getLabel().equals("Cronologia")));
            navigate(EmailInviateView.class);
            var registro = test((Grid<InvioEmail>) find(GrigliaInvii.class).single());
            assertEquals(7, registro.size());
            test(find(TextField.class).all().stream().filter(t -> t.getAriaLabel().filter("Cerca negli invii"::equals).isPresent())
                .findFirst().orElseThrow()).setValue("canto3");
            assertEquals(1, registro.size());
            assertEquals("Allievo Canto3", registro.getCellText(0, 3));
            assertEquals("segreteria", registro.getCellText(0, 5));
            Select<TipoDocumento> tipo = find(Select.class).single();
            tipo.setValue(TipoDocumento.RICEVUTA);
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
            InvioEmailDialog.apri(TipoDocumento.CONFERMA_ISCRIZIONE, canto, allievo, () -> { });
            assertFalse(find(Button.class).withText("Invia conferma").single().isEnabled());
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
    @WithUserDetails("admin")
    @SuppressWarnings("unchecked")
    void admin_vedeEdEliminaIDocumentiAperti() {
        cronologiaDocumentiService.registra("genitore3", TipoDocumento.RICEVUTA, AperturaDocumento.Modalita.SCARICO,
            "Allievo Canto3", "Corso di Canto", "2024/2025", "ricevuta.pdf");
        cronologiaDocumentiService.registra("genitore1", TipoDocumento.RIEPILOGO, AperturaDocumento.Modalita.ANTEPRIMA,
            "Tutti i figli", "Tutti i corsi dell'anno", "2024/2025", "riepilogo.pdf");
        try {
            navigate(EmailInviateView.class);
            find(com.vaadin.flow.component.tabs.TabSheet.class).single().setSelectedIndex(1);
            var aperture = test((Grid<AperturaDocumento>) find(GrigliaAperture.class).single());
            assertEquals(2, aperture.size());
            Select<TipoDocumento> tipo = find(Select.class).single();
            tipo.setValue(TipoDocumento.RICEVUTA);
            assertEquals(1, aperture.size());
            assertTrue(aperture.getCellText(0, 1).contains("(genitore3)"));
            assertEquals("Scaricato", ((Badge) aperture.getCellComponent(0, 6)).getText());

            test(pulsante(aperture.getCellComponent(0, 7), "Elimina")).click();
            test(find(Button.class).withText("Elimina").single()).click();
            assertEquals(0, aperture.size());
            assertEquals(1, cronologiaDocumentiService.tutte().size());
        } finally {
            aperturaDocumentoRepository.deleteAll();
        }
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_vedeIDocumentiApertiSenzaEliminarli() {
        AperturaDocumento apertura = cronologiaDocumentiService.registra("genitore3", TipoDocumento.CONFERMA_ISCRIZIONE,
            AperturaDocumento.Modalita.ANTEPRIMA, "Allievo Canto3", "Corso di Canto", "2024/2025", "conferma.pdf");
        try {
            navigate(EmailInviateView.class);
            find(com.vaadin.flow.component.tabs.TabSheet.class).single().setSelectedIndex(1);
            var aperture = test((Grid<AperturaDocumento>) find(GrigliaAperture.class).single());
            assertEquals(1, aperture.size());
            assertFalse(find(GrigliaAperture.class).single().getColumns().get(7).isVisible());
            assertThrows(IllegalStateException.class, () -> cronologiaDocumentiService.elimina(apertura.getId(), "segreteria"));
        } finally {
            aperturaDocumentoRepository.deleteAll();
        }
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

    /** Filtro per stato dei corsi (Gestione Corsi, Gestione Pagamenti, Riepilogo Iscritti): schede o tendina. */
    @SuppressWarnings("unchecked")
    private HasValue<?, CorsiView.FiltroStato> filtroStatoCorsi() {
        return Stream.of(Schede.class, Select.class).flatMap(tipo -> find(tipo).all().stream())
            .map(c -> (HasValue<?, CorsiView.FiltroStato>) c)
            .filter(c -> c.getValue() instanceof CorsiView.FiltroStato).findFirst().orElseThrow();
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
            assertEquals("1", grid.getCellText(row, 6));
            // Data di nascita gg/mm/aaaa ed età in anni compiuti
            Allievo allievo = (Allievo) grid.getRow(row);
            assertEquals(Formati.dataNumerica(allievo.getDataNascita()), grid.getCellText(row, 2));
            assertEquals(String.valueOf(java.time.Period.between(allievo.getDataNascita(), LocalDate.now()).getYears()),
                grid.getCellText(row, 3));
            grid.getCellText(row, 4);
            // Nessuno allievo di esempio ha un indirizzo proprio: si mostra quello del genitore
            assertTrue(grid.getCellText(row, 5).matches("Via Roma \\d, 00184 Roma \\(RM\\) \\(del genitore\\)"),
                grid.getCellText(row, 5));
        }
    }

    @Test
    @WithUserDetails("admin")
    @SuppressWarnings("unchecked")
    void admin_utenti_griglia() {
        User segreteria = userService.findByUsername("segreteria");
        segreteria.setUltimoAccesso(LocalDateTime.of(LocalDate.now().getYear(), 10, 22, 14, 30));
        userService.save(segreteria, null);
        try {
            navigate(UtentiView.class);
            var utenti = test((Grid<User>) find(Grid.class).single());
            assertEquals(26, utenti.size());
            // Ultimo accesso: data e ora, "Mai" per chi non è mai entrato
            int riga = IntStream.range(0, utenti.size()).filter(r -> utenti.getRow(r).getUsername().equals("segreteria"))
                .findFirst().orElseThrow();
            assertEquals(Formati.dataOra(segreteria.getUltimoAccesso()), utenti.getCellText(riga, 6));
            int mai = IntStream.range(0, utenti.size()).filter(r -> utenti.getRow(r).getUsername().equals("genitore9"))
                .findFirst().orElseThrow();
            assertEquals("Mai", utenti.getCellText(mai, 6));
        } finally {
            User ripristino = userService.findByUsername("segreteria");
            ripristino.setUltimoAccesso(null);
            userService.save(ripristino, null);
        }
    }

    @Test
    @WithUserDetails("segreteria")
    void segreteria_accedeEdNonVedeGestioneUtenti() {
        navigate(HomeView.class);

        List<String> voci = find(SideNavItem.class).all().stream().map(SideNavItem::getLabel).toList();
        assertTrue(voci.contains("Home"), voci.toString());
        assertFalse(voci.contains("Utenti"), voci.toString());
        navigate("utenti", RouteNotFoundError.class);
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_vedeCorsiEAllieviSenzaElimina() {
        navigate(CorsiView.class);
        var corsi = test((Grid<Corso>) find(Grid.class).single());
        assertEquals(2, corsi.size());
        assertAzioni(corsi.getCellComponent(0, AZIONI_CORSO), "Modifica", "Partecipanti", "Aggiorna lezioni", "Duplica");

        navigate(AllieviView.class);
        var allievi = test(find(Grid.class).single());
        assertEquals(13, allievi.size());
        assertAzioni(allievi.getCellComponent(0, 7), "Modifica", "Invita ad accedere");
    }

    @Test
    @WithUserDetails("admin")
    @SuppressWarnings("unchecked")
    void admin_eliminaLaSchedaInsegnanteSoloSenzaCollegamenti() {
        Insegnante insegnante1 = insegnanteService.findByUsername("insegnante1").orElseThrow();
        assertNotNull(insegnanteService.motivoPerNonEliminare(insegnante1.getId()));
        assertThrows(IllegalStateException.class, () -> insegnanteService.elimina(insegnante1.getId()));

        User user = new User();
        user.setUsername("ex.insegnante");
        user.setNome("Elio");
        user.setCognome("Senzacorsi");
        user.getRoles().add("SEGRETERIA");
        insegnanteService.salva(new Insegnante(), user, "segreta123");
        try {
            navigate(InsegnantiView.class);
            var grid = test((Grid<Insegnante>) find(Grid.class).single());
            cerca("elio senzacorsi");
            assertEquals(1, grid.size());
            test(pulsante(grid.getCellComponent(0, 6), "Elimina")).click();
            Dialog conferma = find(Dialog.class).all().stream()
                .filter(d -> "Eliminare la scheda insegnante?".equals(d.getHeaderTitle())).findFirst().orElseThrow();
            test(find(Button.class).from(conferma).withText("Elimina").single()).click();

            assertTrue(insegnanteService.findByUsername("ex.insegnante").isEmpty());
            assertEquals(Set.of("SEGRETERIA"), userService.findByUsername("ex.insegnante").getRoles());
            assertTrue(insegnanteService.findByUsername("insegnante1").isPresent());
        } finally {
            insegnanteService.findByUsername("ex.insegnante").ifPresent(insegnanteRepository::delete);
            User ex = userService.findByUsername("ex.insegnante");
            if (ex != null) {
                userService.delete(ex);
            }
        }
    }

    @Test
    @WithUserDetails("segreteria")
    void segreteria_vedeGliInsegnanti() {
        navigate(InsegnantiView.class);

        var grid = test(find(Grid.class).single());
        assertEquals(2, grid.size());
        for (int row = 0; row < grid.size(); row++) {
            // Forfait annuo non impostato nei dati di prova
            assertEquals("", grid.getCellText(row, 4));
            assertEquals("1", grid.getCellText(row, 5));
        }
        assertAzioni(grid.getCellComponent(0, 6), "Modifica", "Invita ad accedere");
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
        new TextFieldTester<>(find(BigDecimalField.class).from(dialog).withLabel("Rimborso forfettario annuo").single())
            .setValue(new BigDecimal("300"));
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
            assertEquals(0, new BigDecimal("300").compareTo(insegnante.getRimborsoForfettario()));
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
    void segreteria_dallaSchedaDellAllievoAQuellaDelGenitoreEViceversa() {
        Allievo canto3 = allievoService.findByUsername("allievo_canto3").orElseThrow();
        Genitore genitore3 = genitoreService.findByUsername("genitore3").orElseThrow();
        // ?id=… apre la scheda dell'allievo, con il link al genitore collegato
        navigate("allievi?id=" + canto3.getId(), AllieviView.class);
        Dialog allievo = find(Dialog.class).single();
        assertEquals("Modifica Allievo", allievo.getHeaderTitle());
        test(find(Button.class).from(allievo).withText("Apri la scheda di Genitore3 Cognome3").single()).click();

        assertInstanceOf(GenitoriView.class, getCurrentView());
        assertFalse(allievo.isOpened());
        Dialog genitore = find(Dialog.class).single();
        assertEquals("Modifica Genitore", genitore.getHeaderTitle());
        assertEquals(genitore3.getUser().getNome(), find(TextField.class).from(genitore).withLabel("Nome").single().getValue());
        // Un link per figlio, in ordine di nome
        assertEquals(List.of("Apri la scheda di Allievo Canto3", "Apri la scheda di Allievo Chitarra1"),
            find(Button.class).from(genitore).all().stream().map(Button::getText).filter(t -> t.startsWith("Apri la scheda")).toList());

        // Con modifiche non salvate chiede conferma
        test(find(TextField.class).from(genitore).withLabel("Telefono").single()).setValue("333 0000000");
        test(find(Button.class).from(genitore).withText("Apri la scheda di Allievo Chitarra1").single()).click();
        assertInstanceOf(GenitoriView.class, getCurrentView());
        Dialog conferma = find(Dialog.class).all().stream()
            .filter(d -> "Lasciare le modifiche?".equals(d.getHeaderTitle())).findFirst().orElseThrow();
        test(find(Button.class).from(conferma).withText("Apri senza salvare").single()).click();
        assertInstanceOf(AllieviView.class, getCurrentView());
        assertEquals("Allievo", find(TextField.class).from(find(Dialog.class).single()).withLabel("Nome").single().getValue());
        assertNotEquals("333 0000000", genitoreService.findByUsername("genitore3").orElseThrow().getUser().getTelefono());

        // Lasciando la pagina (es. Indietro) il dialog della scheda si chiude
        Dialog scheda = find(Dialog.class).single();
        navigate("genitori?id=" + genitore3.getId(), GenitoriView.class);
        assertFalse(scheda.isOpened());
        assertEquals("Modifica Genitore", find(Dialog.class).single().getHeaderTitle());
    }

    @Test
    @WithUserDetails("segreteria")
    void segreteria_nuovoGenitoreSenzaUsername_usernameGeneratoEAccessoDisattivato() {
        navigate(GenitoriView.class);
        test(find(Button.class).withText("Nuovo Genitore").single()).click();
        Dialog dialog = find(Dialog.class).single();
        // Soli spazi = vuoto: niente password richiesta
        test(find(TextField.class).from(dialog).withLabel("Username").single()).setValue("   ");
        test(find(TextField.class).from(dialog).withLabel("Nome").single()).setValue("Marta");
        test(find(TextField.class).from(dialog).withLabel("Cognome").single()).setValue("Spazi");
        test(find(Button.class).from(dialog).withText("Salva").single()).click();
        assertFalse(dialog.isOpened());

        Genitore genitore = genitoreService.findByUsername("marta.spazi").orElseThrow();
        try {
            assertFalse(genitore.getUser().isEnabled());
            assertTrue(genitore.getUser().getRoles().contains("GENITORE"));
        } finally {
            genitoreRepository.delete(genitore);
            userService.delete(genitore.getUser());
        }
    }

    @Test
    @WithUserDetails("segreteria")
    void segreteria_nuovoInsegnanteSenzaUsername_usernameGenerato() {
        Insegnante insegnante = insegnanteService.salva(new Insegnante(), utenteSenzaUsername("Ugo", "Generato"), "");
        try {
            assertEquals("ugo.generato", insegnante.getUser().getUsername());
            assertFalse(insegnante.getUser().isEnabled());
        } finally {
            insegnanteRepository.delete(insegnante);
            userService.delete(insegnante.getUser());
        }
    }

    private static User utenteSenzaUsername(String nome, String cognome) {
        User user = new User();
        user.setNome(nome);
        user.setCognome(cognome);
        return user;
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_associaIFigliDallaSchedaDelGenitore() {
        Genitore genitore3 = genitoreService.findByUsername("genitore3").orElseThrow();
        Genitore genitore1 = genitoreService.findByUsername("genitore1").orElseThrow();
        List<Allievo> figliPrima = allievoService.findByGenitore(genitore3.getId());
        Allievo tolto = figliPrima.getFirst();
        Allievo nuovo = allievoService.findByUsername("allievo_canto1").orElseThrow();
        assertEquals(genitore1, nuovo.getGenitore());
        try {
            navigate(GenitoriView.class);
            var grid = test((Grid<Genitore>) find(Grid.class).single());
            int riga = IntStream.range(0, grid.size()).filter(r -> grid.getRow(r).equals(genitore3)).findFirst().orElseThrow();
            test(pulsante(grid.getCellComponent(riga, 5), "Modifica")).click();
            Dialog dialog = find(Dialog.class).single();
            MultiSelectComboBox<Allievo> figli = find(MultiSelectComboBox.class).from(dialog).withLabel("Figli").single();
            assertEquals(Set.copyOf(figliPrima), figli.getValue());
            assertEquals("Allievo Canto1 (figlio di " + genitore1.getNomeCompleto() + ")", figli.getItemLabelGenerator().apply(nuovo));

            Set<Allievo> scelti = new java.util.HashSet<>(figli.getValue());
            scelti.remove(tolto);
            scelti.add(nuovo);
            figli.setValue(scelti);
            test(find(Button.class).from(dialog).withText("Salva").single()).click();
            assertFalse(dialog.isOpened());

            assertEquals(genitore3, allievoService.findById(nuovo.getId()).getGenitore());
            assertNull(allievoService.findById(tolto.getId()).getGenitore());
            assertEquals(figliPrima.size(), allievoService.findByGenitore(genitore3.getId()).size());
        } finally {
            Allievo a = allievoService.findById(nuovo.getId());
            a.setGenitore(genitore1);
            allievoService.save(a);
            Allievo b = allievoService.findById(tolto.getId());
            b.setGenitore(genitore3);
            allievoService.save(b);
        }
    }

    @Test
    @WithUserDetails("admin")
    @SuppressWarnings("unchecked")
    void admin_eliminaLaSchedaGenitoreMantenendoLUtente() {
        User user = new User();
        user.setUsername("doppio.ruolo");
        user.setNome("Dora");
        user.setCognome("Doppia");
        user.getRoles().add("INSEGNANTE");
        Genitore genitore = genitoreService.salva(new Genitore(), user, "segreta123");
        Allievo figlio = allievoService.creaRapido("Figlio", "Doppia", null, null);
        figlio.setGenitore(genitore);
        allievoService.save(figlio);
        try {
            navigate(GenitoriView.class);
            var grid = test((Grid<Genitore>) find(Grid.class).single());
            cerca("dora doppia");
            assertEquals(1, grid.size());
            test(pulsante(grid.getCellComponent(0, 5), "Elimina")).click();
            Dialog conferma = find(Dialog.class).all().stream()
                .filter(d -> "Eliminare la scheda genitore?".equals(d.getHeaderTitle())).findFirst().orElseThrow();
            assertTrue(find(Span.class).from(conferma).all().stream().anyMatch(sp -> sp.getText().contains("1 figlio resta senza genitore")));
            test(find(Button.class).from(conferma).withText("Elimina").single()).click();

            // Scheda eliminata, utente mantenuto senza il ruolo GENITORE, figlio senza genitore
            assertTrue(genitoreService.findByUsername("doppio.ruolo").isEmpty());
            User rimasto = userService.findByUsername("doppio.ruolo");
            assertNotNull(rimasto);
            assertEquals(Set.of("INSEGNANTE"), rimasto.getRoles());
            assertNull(allievoService.findById(figlio.getId()).getGenitore());
        } finally {
            genitoreService.findByUsername("doppio.ruolo").ifPresent(genitoreRepository::delete);
            Allievo daEliminare = allievoService.findById(figlio.getId());
            daEliminare.setGenitore(null);
            allievoService.save(daEliminare);
            allievoService.delete(daEliminare);
            userService.delete(daEliminare.getUser());
            User doppio = userService.findByUsername("doppio.ruolo");
            if (doppio != null) {
                userService.delete(doppio);
            }
        }
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_modificaIDatiDiUnGenitore() {
        navigate(GenitoriView.class);
        var grid = test((Grid<Genitore>) find(Grid.class).single());
        int genitore3 = IntStream.range(0, grid.size())
            .filter(r -> grid.getRow(r).getUser().getUsername().equals("genitore3")).findFirst().orElseThrow();
        // La segreteria non elimina le schede genitore
        assertFalse(grid.getCellComponent(genitore3, 5).getChildren()
            .anyMatch(c -> "Elimina".equals(c.getElement().getAttribute("aria-label"))));
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
        // In Genitori non c'è più il riepilogo: è in Documenti
        navigate(GenitoriView.class);
        var genitori = test((Grid<Genitore>) find(Grid.class).single());
        assertThrows(AssertionError.class, () -> pulsante(genitori.getCellComponent(0, 5), "Riepilogo famiglia"));

        navigate(DocumentiView.class);
        scegliTipo(TipoDocumento.RIEPILOGO);
        var riepiloghi = test((Grid<DocumentoRiepilogo>) find(Grid.class).single());
        String genitore3 = genitoreService.findByUsername("genitore3").orElseThrow().getNomeCompleto();
        test(find(TextField.class).withPlaceholder("Cerca").single()).setValue(genitore3);
        assertEquals(1, riepiloghi.size());
        assertEquals("2024/2025", riepiloghi.getCellText(0, 2));
        assertLinkPdf(riepiloghi.getCellComponent(0, 6));
        // Genitore senza figli iscritti: nessun riepilogo
        test(find(TextField.class).withPlaceholder("Cerca").single())
            .setValue(genitoreService.findByUsername("genitore9").orElseThrow().getNomeCompleto());
        assertEquals(0, riepiloghi.size());
    }

    @Test
    @WithUserDetails("genitore3")
    @SuppressWarnings("unchecked")
    void genitore_scaricaIlProprioRiepilogoDaDocumenti() {
        navigate(PagamentiFigliView.class);
        assertFalse(find(Button.class).withText("Riepilogo famiglia").exists());
        navigate(DocumentiView.class);
        var documenti = test((Grid<DocumentoFamiglia>) find(Grid.class).single());
        assertEquals("Riepilogo pagamenti", documenti.getCellText(0, 0));
        assertEquals("2024/2025", documenti.getCellText(0, 3));
        assertAnteprimaEInvio(documenti.getCellComponent(0, 5));
    }

    @Test
    @WithUserDetails("genitore3")
    @SuppressWarnings("unchecked")
    void genitore_anteprimaEInvioFinisconoNellaCronologia() throws Exception {
        try {
            navigate(DocumentiView.class);
            var documenti = test((Grid<DocumentoFamiglia>) find(Grid.class).single());
            Component azioni = documenti.getCellComponent(0, 5);
            apriLink(find(com.vaadin.flow.component.html.Anchor.class).from(azioni).single());
            inviaAllaMiaEmail(azioni, "genitore3@example.com");

            List<AperturaDocumento> aperture = cronologiaDocumentiService.tutte();
            assertEquals(2, aperture.size());
            assertEquals(Set.of(AperturaDocumento.Modalita.ANTEPRIMA, AperturaDocumento.Modalita.EMAIL),
                aperture.stream().map(AperturaDocumento::getModalita).collect(Collectors.toSet()));
            AperturaDocumento apertura = aperture.getFirst();
            assertEquals("genitore3", apertura.getUsername());
            assertEquals(TipoDocumento.RIEPILOGO, apertura.getTipo());
            assertEquals("Tutti i figli", apertura.getAllievoNome());
            assertEquals("2024/2025", apertura.getAnno());
            assertTrue(apertura.getNomeFile().endsWith(".pdf"));
            // L'invio è anche in Email inviate, fatto dal genitore stesso
            InvioEmail invio = invioEmailRepository.findAllByOrderByDataOraDesc().getFirst();
            assertEquals("genitore3", invio.getInviatoDa());
            assertEquals("genitore3@example.com", invio.getDestinatario());
        } finally {
            aperturaDocumentoRepository.deleteAll();
            invioEmailRepository.deleteAll();
        }
    }

    @Test
    @WithUserDetails("insegnante1")
    @SuppressWarnings("unchecked")
    void insegnante_anteprimaEInvioDellaLetteraDiIncarico() throws Exception {
        Corso canto = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow();
        Long insegnanteId = insegnanteService.findByUsername("insegnante1").orElseThrow().getId();
        LetteraIncarico lettera = letteraIncaricoService.salva(letteraIncaricoService.proposta(insegnanteId, canto.getId()), "segreteria");
        try {
            navigate(DocumentiView.class);
            var lettere = test((Grid<LetteraIncarico>) find(Grid.class).single());
            Component azioni = lettere.getCellComponent(0, 4);
            assertAnteprimaEInvio(azioni);
            apriLink(find(com.vaadin.flow.component.html.Anchor.class).from(azioni).single());
            inviaAllaMiaEmail(azioni, "insegnante1@example.com");
            assertEquals(Set.of(AperturaDocumento.Modalita.ANTEPRIMA, AperturaDocumento.Modalita.EMAIL),
                cronologiaDocumentiService.tutte().stream().map(AperturaDocumento::getModalita).collect(Collectors.toSet()));
            assertTrue(cronologiaDocumentiService.tutte().stream().allMatch(a -> "insegnante1".equals(a.getUsername())
                && a.getTipo() == TipoDocumento.LETTERA_INCARICO && "Corso di Canto".equals(a.getCorsoNome())));
            assertEquals(TipoDocumento.LETTERA_INCARICO, invioEmailRepository.findAllByOrderByDataOraDesc().getFirst().getTipo());
        } finally {
            aperturaDocumentoRepository.deleteAll();
            invioEmailRepository.deleteAll();
            letteraIncaricoService.elimina(lettera.getId(), "admin");
        }
    }

    /** Sceglie "Invia alla mia email" nel menu del documento e conferma l'invio all'indirizzo atteso. */
    private void inviaAllaMiaEmail(Component azioni, String email) {
        MenuBar menu = find(MenuBar.class).from(azioni).single();
        MenuItem invia = menu.getItems().getFirst().getSubMenu().getItems().stream()
            .filter(v -> v.getElement().getTextRecursively().contains("Invia alla mia email")).findFirst().orElseThrow();
        ComponentUtil.fireEvent(invia, new ClickEvent<>(invia));
        assertTrue(find(Paragraph.class).all().stream().anyMatch(p -> p.getText().contains(email)));
        test(find(Button.class).withText("Invia").single()).click();
    }

    /** Sceglie la scheda del tipo di documento in Documenti (admin e segreteria). */
    @SuppressWarnings("unchecked")
    private void scegliTipo(TipoDocumento tipoDocumento) {
        ((Schede<TipoDocumento>) find(Schede.class).single()).setValue(tipoDocumento);
    }

    /** Esegue il download del link come farebbe il browser (risposta su un buffer). */
    private static void apriLink(com.vaadin.flow.component.html.Anchor link) throws IOException {
        var risorsa = (StreamResourceRegistry.ElementStreamResource) VaadinSession.getCurrent().getResourceRegistry()
            .getResource(URI.create(link.getHref())).orElseThrow();
        VaadinResponse risposta = mock(VaadinResponse.class);
        when(risposta.getOutputStream()).thenReturn(new ByteArrayOutputStream());
        risorsa.getElementRequestHandler().handleRequest(mock(VaadinRequest.class), risposta, VaadinSession.getCurrent(),
            risorsa.getOwner());
    }

    /** Menu PDF di famiglie e insegnanti: solo il link dell'anteprima (inline, nuova scheda) e "Invia alla mia email". */
    private void assertAnteprimaEInvio(Component contenitore) {
        var anteprima = find(com.vaadin.flow.component.html.Anchor.class).from(contenitore).single();
        assertEquals("_blank", anteprima.getTarget().orElse(null));
        assertFalse(anteprima.getElement().hasAttribute("download"));
        assertTrue(find(MenuBar.class).from(contenitore).single().getItems().getFirst().getSubMenu().getItems().stream()
            .anyMatch(v -> v.getElement().getTextRecursively().contains("Invia alla mia email")));
    }

    /** Nel componente un menu PDF con l'anteprima in una nuova scheda (inline) e lo scarico, entrambi con un indirizzo. */
    private void assertLinkPdf(Component contenitore) {
        List<com.vaadin.flow.component.html.Anchor> link = find(com.vaadin.flow.component.html.Anchor.class).from(contenitore).all();
        assertEquals(2, link.size());
        assertTrue(link.stream().allMatch(a -> !a.getHref().isBlank()));
        var anteprima = link.stream().filter(a -> "_blank".equals(a.getTarget().orElse(null))).findFirst().orElseThrow();
        assertFalse(anteprima.getElement().hasAttribute("download"));
        assertTrue(link.stream().anyMatch(a -> a.getElement().hasAttribute("download")));
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_ricercaRapidaInAllieviGenitoriEInsegnanti() {
        navigate(AllieviView.class);
        var allievi = test((Grid<Allievo>) find(Grid.class).single());
        assertEquals(13, allievi.size());
        // Nel genitore collegato: i due figli di genitore3
        cerca("cognome3 GENITORE3");
        assertEquals(2, allievi.size());
        assertTrue(find(Span.class).withText("2 di 13").exists());
        // Nel codice fiscale e nei corsi
        cerca("chitarra1 chitarra");
        assertEquals(1, allievi.size());
        assertEquals("Allievo Chitarra1", allievi.getRow(0).getNomeCompleto());
        cerca("");
        assertEquals(13, allievi.size());

        navigate(GenitoriView.class);
        var genitori = test((Grid<Genitore>) find(Grid.class).single());
        cerca("canto3");
        assertEquals(1, genitori.size());
        assertEquals("genitore3", genitori.getRow(0).getUser().getUsername());

        navigate(InsegnantiView.class);
        var insegnanti = test((Grid<Insegnante>) find(Grid.class).single());
        cerca("chitarra");
        assertEquals(1, insegnanti.size());
        assertEquals("insegnante2", insegnanti.getRow(0).getUser().getUsername());
    }

    @Test
    @WithUserDetails("admin")
    @SuppressWarnings("unchecked")
    void admin_nuovoUtenteSenzaDatiObbligatori_erroriSuiCampi() {
        navigate(UtentiView.class);
        int prima = userService.findAll().size();
        test(find(Button.class).withText("Nuovo Utente").single()).click();
        Dialog dialog = find(Dialog.class).single();
        TextField username = find(TextField.class).from(dialog).withLabel("Username").single();
        PasswordField password = find(PasswordField.class).from(dialog).withLabel("Password").single();
        TextField nome = find(TextField.class).from(dialog).withLabel("Nome").single();
        TextField cognome = find(TextField.class).from(dialog).withLabel("Cognome").single();
        test((CheckboxGroup<String>) find(CheckboxGroup.class).from(dialog).withLabel("Ruoli").single()).selectItem("GENITORE");
        Button salva = find(Button.class).from(dialog).withText("Salva").single();

        // Tutto vuoto: errori sui campi, niente sul database
        test(salva).click();
        assertTrue(username.isInvalid());
        assertTrue(password.isInvalid());
        assertTrue(nome.isInvalid());
        assertTrue(cognome.isInvalid());
        assertTrue(dialog.isOpened());

        // Username di soli spazi o già in uso
        test(password).setValue("segreta123");
        test(nome).setValue("Gino");
        test(cognome).setValue("Senzanome");
        test(username).setValue("   ");
        test(salva).click();
        assertTrue(username.isInvalid());
        test(username).setValue("genitore1");
        test(salva).click();
        assertTrue(username.isInvalid());
        assertTrue(dialog.isOpened());
        assertEquals(prima, userService.findAll().size());

        test(username).setValue(" gino.nuovo ");
        test(salva).click();
        User creato = userService.findByUsername("gino.nuovo");
        try {
            assertFalse(dialog.isOpened());
            assertNotNull(creato);
            assertTrue(genitoreRepository.findByUserId(creato.getId()).isPresent());
        } finally {
            if (creato != null) {
                genitoreRepository.findByUserId(creato.getId()).ifPresent(genitoreRepository::delete);
                userService.delete(creato);
            }
        }
    }

    @Test
    @WithUserDetails("admin")
    @SuppressWarnings("unchecked")
    void admin_eliminaUnUtenteConLaSuaSchedaLibera() {
        Allievo allievo = allievoService.creaRapido("Ivo", "Daeliminare", null, null);
        String username = allievo.getUser().getUsername();
        try {
            navigate(UtentiView.class);
            var utenti = test((Grid<User>) find(Grid.class).single());
            // Allievo iscritto a un corso: niente conferma, solo il motivo
            cerca("allievo_canto1");
            test(pulsante(utenti.getCellComponent(0, 9), "Elimina")).click();
            assertTrue(find(Dialog.class).all().stream().noneMatch(d -> "Eliminare l'utente?".equals(d.getHeaderTitle())));
            assertNotNull(userService.findByUsername("allievo_canto1"));

            cerca("ivo daeliminare");
            assertEquals(1, utenti.size());
            test(pulsante(utenti.getCellComponent(0, 9), "Elimina")).click();
            Dialog conferma = find(Dialog.class).all().stream()
                .filter(d -> "Eliminare l'utente?".equals(d.getHeaderTitle())).findFirst().orElseThrow();
            assertTrue(find(Span.class).from(conferma).all().stream().anyMatch(sp -> sp.getText().contains("si eliminano anche scheda allievo")));
            test(find(Button.class).from(conferma).withText("Elimina").single()).click();
            assertNull(userService.findByUsername(username));
            assertTrue(allievoService.findByUsername(username).isEmpty());
            assertEquals(0, utenti.size());
        } finally {
            allievoService.findByUsername(username).ifPresent(a -> {
                allievoService.delete(a);
                userService.delete(a.getUser());
            });
        }
    }

    @Test
    @WithUserDetails("admin")
    @SuppressWarnings("unchecked")
    void admin_incongruenzeTraRuoliESchede() {
        // genitore9 senza il ruolo GENITORE (la scheda resta) e con il ruolo ALLIEVO senza scheda, salvato senza
        // passare da Gestione Utenti
        User user = userService.findByUsername("genitore9");
        user.getRoles().remove("GENITORE");
        user.getRoles().add("ALLIEVO");
        userService.save(user, null);
        try {
            navigate(UtentiView.class);
            var utenti = test((Grid<User>) find(Grid.class).single());
            int tutti = utenti.size();
            test(find(Checkbox.class).withLabel("Solo incongruenze").single()).click();
            assertEquals(1, utenti.size());
            assertEquals("genitore9", utenti.getRow(0).getUsername());
            assertEquals(List.of("Scheda genitore senza ruolo", "Allievo senza scheda"),
                utenti.getCellComponent(0, 5).getChildren().map(c -> ((Badge) c).getText()).toList());

            // Il pulsante crea la scheda allievo; la scheda genitore senza ruolo resta segnalata
            test(find(Button.class).withText("Crea schede mancanti").single()).click();
            assertTrue(allievoService.findByUsername("genitore9").isPresent());
            assertEquals(List.of("Scheda genitore senza ruolo"),
                utenti.getCellComponent(0, 5).getChildren().map(c -> ((Badge) c).getText()).toList());
            test(find(Checkbox.class).withLabel("Solo incongruenze").single()).click();
            assertEquals(tutti, utenti.size());
        } finally {
            allievoService.findByUsername("genitore9").ifPresent(a -> allievoService.delete(a));
            User ripristino = userService.findByUsername("genitore9");
            ripristino.getRoles().remove("ALLIEVO");
            ripristino.getRoles().add("GENITORE");
            userService.save(ripristino, null);
        }
    }

    @Test
    @WithUserDetails("admin")
    @SuppressWarnings("unchecked")
    void admin_ricercaRapidaNegliUtenti() {
        navigate(UtentiView.class);
        var utenti = test((Grid<User>) find(Grid.class).single());
        int tutti = utenti.size();
        cerca("segreteria");
        assertEquals(1, utenti.size());
        cerca("example.com genitore9");
        assertEquals(1, utenti.size());
        cerca("nessuno-così");
        assertEquals(0, utenti.size());
        cerca("");
        assertEquals(tutti, utenti.size());
    }

    @Test
    @WithUserDetails("admin")
    @SuppressWarnings("unchecked")
    void admin_ruoloGenitoreAssegnatoCreaLaSchedaNellElencoGenitori() {
        Allievo allievo = allievoService.creaRapido("Lia", "Ruolo", null, null);
        try {
            navigate(UtentiView.class);
            var utenti = test((Grid<User>) find(Grid.class).single());
            cerca("lia ruolo");
            assertEquals(1, utenti.size());
            test(pulsante(utenti.getCellComponent(0, 9), "Modifica")).click();
            test((CheckboxGroup<String>) find(CheckboxGroup.class).withLabel("Ruoli").single()).selectItem("GENITORE");
            test(find(Button.class).withText("Salva").single()).click();

            navigate(GenitoriView.class);
            var genitori = test((Grid<Genitore>) find(Grid.class).single());
            assertEquals(10, genitori.size());
            cerca("lia ruolo");
            assertEquals(1, genitori.size());
        } finally {
            genitoreRepository.findByUserId(allievo.getUser().getId()).ifPresent(genitoreRepository::delete);
            Allievo daEliminare = allievoService.findById(allievo.getId());
            allievoService.delete(daEliminare);
            userService.delete(daEliminare.getUser());
        }
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_ricercaRapidaInCorsiERiepilogoIscritti() {
        navigate(CorsiView.class);
        var corsi = test((Grid<Corso>) find(Grid.class).single());
        assertEquals(2, corsi.size());
        // Negli insegnanti del corso
        cerca("rossi");
        assertEquals(1, corsi.size());
        assertEquals("Corso di Chitarra", corsi.getRow(0).getNome());
        assertTrue(find(Span.class).withText("1 di 2").exists());

        navigate(RiepilogoIscrittiView.class);
        var riepilogo = new com.vaadin.flow.component.treegrid.TreeGridTester<>(
            (com.vaadin.flow.component.treegrid.TreeGrid<RiepilogoIscrittiView.Voce>) find(com.vaadin.flow.component.treegrid.TreeGrid.class).single());
        // Un iscritto: solo il suo corso con lui
        cerca("canto3");
        assertEquals(2, riepilogo.size());
        assertEquals("Corso di Canto", riepilogo.getCellText(0, 0));
        assertEquals("Allievo Canto3", riepilogo.getCellText(1, 0));
        // Il genitore: i corsi dei suoi figli
        cerca("genitore3");
        assertEquals(4, riepilogo.size());
        // Il corso: con tutti i suoi iscritti
        cerca("chitarra");
        assertEquals(7, riepilogo.size());
        assertTrue(find(Span.class).withText("1 di 2").exists());
    }

    @Test
    @WithUserDetails("admin")
    @SuppressWarnings("unchecked")
    void admin_allineaGliUsernameGeneratiANomeECognome() {
        User anna = new User("anna.bianchi", "x", "Anna", "Verdi", null);
        anna = userService.save(anna, "segreta123");
        Long id = anna.getId();
        try {
            navigate(UtentiView.class);
            test(find(Button.class).withText("Allinea username").single()).click();
            Dialog dialog = find(Dialog.class).single();
            var proposte = test((Grid<UserService.UsernameDaAllineare>) find(Grid.class).from(dialog).single());
            assertEquals(1, proposte.size());
            assertEquals("anna.verdi", proposte.getCellText(0, 2));
            test(find(Button.class).from(dialog).withText("Allinea").single()).click();
            assertEquals("anna.verdi", userRepository.findById(id).orElseThrow().getUsername());

            // Già allineati: un avviso, nessun dialog
            test(find(Button.class).withText("Allinea username").single()).click();
            assertFalse(find(Dialog.class).exists());
        } finally {
            userRepository.deleteById(id);
        }
    }

    @Test
    @WithUserDetails("admin")
    @SuppressWarnings("unchecked")
    void admin_vedeGliUtentiSospesiPerInattivita() {
        User sospeso = userService.findByUsername("genitore9");
        sospeso.setUltimoAccesso(LocalDateTime.now().minusMonths(8));
        userService.save(sospeso, null);
        userService.sospendiInattivi(LocalDateTime.now());
        try {
            navigate(UtentiView.class);
            var utenti = test((Grid<User>) find(Grid.class).single());
            test(find(Checkbox.class).withLabel("Solo sospesi per inattività").single()).click();
            assertEquals(1, utenti.size());
            assertEquals("Sospeso per inattività", ((Badge) utenti.getCellComponent(0, 8)).getText());
        } finally {
            User ripristino = userService.findByUsername("genitore9");
            ripristino.setEnabled(true);
            ripristino.setUltimoAccesso(null);
            userService.save(ripristino, null);
        }
    }

    @Test
    @WithUserDetails("admin")
    @SuppressWarnings("unchecked")
    void admin_reimpostaLAccessoDiUnUtenteGiaEntrato() {
        User insegnante = userService.findByUsername("insegnante2");
        insegnante.setUltimoAccesso(LocalDateTime.now().minusDays(1));
        userService.save(insegnante, null);
        try {
            navigate(UtentiView.class);
            var utenti = test((Grid<User>) find(Grid.class).single());
            cerca("insegnante2");
            test(pulsante(utenti.getCellComponent(0, 9), "Reimposta accesso")).click();
            Dialog dialog = find(Dialog.class).single();
            test(find(Button.class).from(dialog).withText("Anteprima").single()).click();
            assertTrue(find(Dialog.class).all().stream()
                .anyMatch(d -> "Anteprima – Il tuo accesso all'Area Corsi è stato reimpostato".equals(d.getHeaderTitle())));
            assertTrue(userService.findByUsername("insegnante2").isEnabled());

            test(find(Button.class).from(dialog).withText("Reimposta e invia").single()).click();
            assertFalse(userService.findByUsername("insegnante2").isEnabled());
        } finally {
            User ripristino = userService.findByUsername("insegnante2");
            ripristino.setEnabled(true);
            ripristino.setUltimoAccesso(null);
            ripristino.setInvitatoIl(null);
            userService.save(ripristino, "insegnante2");
            recuperoPasswordRepository.deleteAll();
        }
    }

    @Test
    @WithUserDetails("admin")
    @SuppressWarnings("unchecked")
    void admin_invitaGliUtentiSelezionati() {
        navigate(UtentiView.class);
        var utenti = test((Grid<User>) find(Grid.class).single());
        Button invitaSelezionati = find(Button.class).withText("Invita selezionati").single();
        assertFalse(invitaSelezionati.isEnabled());
        cerca("allievo_canto1");
        User allievo = utenti.getRow(0);
        try {
            utenti.select(0);
            assertTrue(invitaSelezionati.isEnabled());
            test(invitaSelezionati).click();
            test(find(Button.class).withText("Invia 1 invito").single()).click();
            assertNotNull(userService.findByUsername("allievo_canto1").getInvitatoIl());
            // La colonna "Invito" mostra quando
            cerca("allievo_canto1");
            assertFalse(utenti.getCellText(0, 7).isEmpty());
        } finally {
            User ripristino = userService.findByUsername("allievo_canto1");
            ripristino.setInvitatoIl(null);
            userService.save(ripristino, null);
            recuperoPasswordRepository.deleteAll();
        }
    }

    private void cerca(String testo) {
        test(find(TextField.class).all().stream().filter(t -> t.getAriaLabel().filter("Cerca nell'elenco"::equals).isPresent())
            .findFirst().orElseThrow()).setValue(testo);
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
     * Apre "Le mie lezioni" ricreandola: per insegnante1 e insegnante2 (un solo corso) la vista è già aperta
     * all'avvio del test, al posto della Home, con i dati di quel momento.
     */
    private void apriLezioni() {
        navigate(HomeView.class);
        navigate(LezioniInsegnanteView.class);
    }

    @Test
    @WithUserDetails("insegnante2")
    @SuppressWarnings("unchecked")
    void insegnante_numeroDellaLezioneConGruppoOGiorno() {
        Corso chitarra = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Chitarra")).findFirst().orElseThrow();
        GruppoCorso martedi = corsoService.salvaGruppo(chitarra.getId(), new GruppoCorso(null, null, DayOfWeek.TUESDAY, LocalTime.of(17, 0)));
        GruppoCorso sabato = corsoService.salvaGruppo(chitarra.getId(), new GruppoCorso(null, "Principianti", DayOfWeek.SATURDAY, LocalTime.of(10, 0)));
        List<Allievo> allievi = IntStream.rangeClosed(1, 6)
            .mapToObj(n -> allievoService.findByUsername("allievo_chitarra" + n).orElseThrow()).toList();
        try {
            for (int n = 0; n < 6; n++) {
                corsoService.assegnaGruppo(chitarra.getId(), allievi.get(n).getId(), (n < 3 ? martedi : sabato).getId());
            }
            corsoService.generaLezioni(chitarra);
            apriLezioni();
            Grid<Lezione> griglia = find(Grid.class).all().stream()
                .filter(g -> g.getColumns().stream().anyMatch(c -> "N. Lezione".equals(((Grid.Column<?>) c).getHeaderText())))
                .findFirst().orElseThrow();
            List<Grid.Column<Lezione>> visibili = griglia.getColumns().stream().filter(Grid.Column::isVisible).toList();
            int numero = IntStream.range(0, visibili.size()).filter(i -> "N. Lezione".equals(visibili.get(i).getHeaderText()))
                .findFirst().orElseThrow();
            var lezioni = test(griglia);
            List<String> numeri = IntStream.range(0, lezioni.size()).mapToObj(r -> lezioni.getCellText(r, numero)).toList();
            assertTrue(numeri.contains("1 · mar"), numeri.toString());
            assertTrue(numeri.contains("1 · Principianti"), numeri.toString());
        } finally {
            allievi.forEach(a -> {
                corsoService.assegnaGruppo(chitarra.getId(), a.getId(), null);
                corsoService.impostaOrario(chitarra.getId(), a.getId(), null);
            });
            corsoService.gruppi(chitarra.getId()).forEach(g -> corsoService.eliminaGruppo(g.getId()));
            corsoService.generaLezioni(chitarra);
        }
    }

    @Test
    @WithUserDetails("admin")
    void lezioni_conIlParametroApreLePresenzeDellaLezione() {
        Corso canto = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow();
        Lezione lezione = lezioneService.findByCorso(canto.getId()).get(2);
        navigate("lezioni?lezione=" + lezione.getId(), LezioniView.class);
        String titolo = "Corso di Canto – Lezione " + lezione.getNumeroLezione() + " (" + Formati.data(lezione.getData()) + ")";
        assertTrue(find(Dialog.class).all().stream().anyMatch(d -> titolo.equals(d.getHeaderTitle())));
    }

    @Test
    @WithUserDetails("admin")
    @SuppressWarnings("unchecked")
    void admin_settimanaTipoDeiCorsiAttivi() {
        navigate(CalendarioView.class);
        CalendarioView calendario = find(CalendarioView.class).single();
        calendario.mostra(CalendarioView.Vista.SETTIMANA);
        // Niente mese da scorrere; voci in sola lettura (nessun pulsante), nella griglia e nell'elenco mobile
        assertFalse(find(Button.class).withText("Oggi").exists());
        List<String> orari = find(Span.class).all().stream().map(Span::getText)
            .filter(t -> t.matches("\\d\\d:\\d\\d–\\d\\d:\\d\\d")).toList();
        assertEquals(List.of("18:00–18:40", "17:00–17:40", "18:00–18:40", "18:00–18:40", "17:00–17:40", "18:00–18:40"), orari);
        assertEquals(2 * 2, find(Span.class).withText("Corso di Chitarra").all().size());
        assertTrue(find(H3.class).withText("giovedì").exists());
        assertTrue(find(Button.class).all().stream().noneMatch(b -> b.getText().contains("Corso di")));

        // Filtro per corso anche qui
        test((ComboBox<Corso>) find(ComboBox.class).withPlaceholder("Tutti i corsi").single()).selectItem("Corso di Canto");
        assertEquals(2, find(Span.class).withText("Corso di Canto").all().size());
        assertFalse(find(Span.class).withText("Corso di Chitarra").exists());

        // Di nuovo il mese
        calendario.mostra(CalendarioView.Vista.MESE);
        assertTrue(find(Button.class).withText("Oggi").exists());
        assertFalse(find(Span.class).withText("Corso di Canto").exists());
    }

    @Test
    @WithUserDetails("admin")
    @SuppressWarnings("unchecked")
    void admin_calendarioDelMeseConFiltroDettagliEPresenze() {
        assertTrue(find(SideNavItem.class).all().stream().anyMatch(i -> i.getLabel().equals("Calendario")));
        navigate(CalendarioView.class);
        CalendarioView calendario = find(CalendarioView.class).single();
        // Ottobre 2024: Canto il giovedì (5), Chitarra martedì e sabato (9); griglia ed elenco mobile
        calendario.vaiA(java.time.YearMonth.of(2024, 10));
        assertTrue(find(H2.class).withText("ottobre 2024").exists());
        assertEquals(2 * 14, lezioniNelCalendario(null).size());
        test((ComboBox<Corso>) find(ComboBox.class).withPlaceholder("Tutti i corsi").single()).selectItem("Corso di Canto");
        assertEquals(2 * 5, lezioniNelCalendario("Corso di Canto").size());

        test(lezioniNelCalendario("Corso di Canto").getFirst()).click();
        Dialog dettagli = find(Dialog.class).all().stream()
            .filter(d -> "Corso di Canto".equals(d.getHeaderTitle())).findFirst().orElseThrow();
        test(find(Button.class).from(dettagli).withText("Gestisci presenze").single()).click();
        assertInstanceOf(LezioniView.class, getCurrentView());
        assertTrue(find(Dialog.class).all().stream().anyMatch(d -> d.getHeaderTitle().startsWith("Corso di Canto – Lezione 1 ")));
    }

    @Test
    @WithUserDetails("allievo_canto1")
    void allievo_calendarioSoloLeSueLezioniSenzaPresenze() {
        navigate(CalendarioView.class);
        find(CalendarioView.class).single().vaiA(java.time.YearMonth.of(2024, 10));
        assertEquals(2 * 5, lezioniNelCalendario(null).size());
        assertFalse(find(ComboBox.class).withPlaceholder("Tutti i corsi").exists());
        test(lezioniNelCalendario(null).getFirst()).click();
        assertFalse(find(Button.class).withText("Gestisci presenze").exists());
    }

    @Test
    @WithUserDetails("insegnante2")
    void insegnante_nelCalendarioUnaVocePerGruppoConIlSuoOrario() {
        Corso chitarra = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Chitarra")).findFirst().orElseThrow();
        GruppoCorso piccoli = corsoService.salvaGruppo(chitarra.getId(), new GruppoCorso(null, "Piccoli", DayOfWeek.SATURDAY, LocalTime.of(10, 0)));
        Allievo allievo = allievoService.findByUsername("allievo_chitarra1").orElseThrow();
        corsoService.assegnaGruppo(chitarra.getId(), allievo.getId(), piccoli.getId());
        try {
            navigate(CalendarioView.class);
            find(CalendarioView.class).single().vaiA(java.time.YearMonth.of(2024, 10));
            List<Button> voci = lezioniNelCalendario("Corso di Chitarra");
            // I 4 sabati di ottobre: il gruppo alle 10 e gli altri all'orario della lezione (griglia ed elenco)
            assertEquals(4 * 2, voci.stream().filter(b -> b.getText().equals("10:00 Corso di Chitarra · Piccoli")).count());
            assertTrue(voci.stream().anyMatch(b -> b.getText().endsWith("Corso di Chitarra · Senza gruppo")));
            test(voci.stream().filter(b -> b.getText().equals("10:00 Corso di Chitarra · Piccoli")).findFirst().orElseThrow()).click();
            List<String> testi = find(Span.class).from(find(Dialog.class).single()).all().stream().map(Span::getText).toList();
            assertTrue(testi.contains("Piccoli – sab 10:00"), testi.toString());
        } finally {
            corsoService.eliminaGruppo(piccoli.getId());
            corsoService.impostaOrario(chitarra.getId(), allievo.getId(), null);
        }
    }

    @Test
    @WithUserDetails("insegnante2")
    @SuppressWarnings("unchecked")
    void insegnante_registroDellaLezioneConUnaSchedaPerGruppo() {
        // Chitarra il sabato: Piccoli alle 10 (Chitarra1, Chitarra2), Grandi alle 11 (Chitarra3), Chitarra4..6 senza gruppo
        Corso chitarra = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Chitarra")).findFirst().orElseThrow();
        GruppoCorso piccoli = corsoService.salvaGruppo(chitarra.getId(), new GruppoCorso(null, "Piccoli", DayOfWeek.SATURDAY, LocalTime.of(10, 0)));
        GruppoCorso grandi = corsoService.salvaGruppo(chitarra.getId(), new GruppoCorso(null, "Grandi", DayOfWeek.SATURDAY, LocalTime.of(11, 0)));
        List<Allievo> assegnati = List.of("allievo_chitarra1", "allievo_chitarra2", "allievo_chitarra3").stream()
            .map(u -> allievoService.findByUsername(u).orElseThrow()).toList();
        corsoService.assegnaGruppo(chitarra.getId(), assegnati.get(0).getId(), piccoli.getId());
        corsoService.assegnaGruppo(chitarra.getId(), assegnati.get(1).getId(), piccoli.getId());
        corsoService.assegnaGruppo(chitarra.getId(), assegnati.get(2).getId(), grandi.getId());
        try {
            Lezione sabato = lezioneService.findByCorso(chitarra.getId()).stream()
                .filter(l -> l.getData().getDayOfWeek() == DayOfWeek.SATURDAY).findFirst().orElseThrow();
            Lezione martedi = lezioneService.findByCorso(chitarra.getId()).stream()
                .filter(l -> l.getData().getDayOfWeek() == DayOfWeek.TUESDAY).findFirst().orElseThrow();
            apriLezioni();
            // Orario delle lezioni: il sabato quelli dei due gruppi, il martedì quello del corso
            Grid<Lezione> griglia = find(Grid.class).all().stream()
                .filter(g -> g.getColumns().stream().anyMatch(c -> "Orario".equals(((Grid.Column<?>) c).getHeaderText())))
                .findFirst().orElseThrow();
            List<Grid.Column<Lezione>> visibili = griglia.getColumns().stream().filter(Grid.Column::isVisible).toList();
            int colonna = IntStream.range(0, visibili.size()).filter(i -> "Orario".equals(visibili.get(i).getHeaderText()))
                .findFirst().orElseThrow();
            var elenco = test(griglia);
            List<String> orari = IntStream.range(0, elenco.size()).mapToObj(r -> elenco.getCellText(r, colonna)).distinct().toList();
            assertEquals(Set.of("10:00 · 11:00", "18:00"), Set.copyOf(orari));
            find(ElencoLezioni.class).single().apriPresenze(sabato.getId());
            Dialog registro = find(Dialog.class).single();
            Tabs schede = find(Tabs.class).from(registro).single();
            assertEquals(List.of("Piccoli 10:00 (2)", "Grandi 11:00 (1)", "Senza gruppo (3)", "Tutti (6)"),
                find(Tab.class).from(schede).all().stream().map(Tab::getLabel).toList());
            // Si apre sul primo gruppo, poi si cambia scheda
            var presenze = test((Grid<Presenza>) find(Grid.class).from(registro).single());
            assertEquals(2, presenze.size());
            schede.setSelectedIndex(1);
            assertEquals(1, presenze.size());
            assertEquals("Allievo Chitarra3", presenze.getRow(0).getAllievo().getNomeCompleto());
            schede.setSelectedIndex(3);
            assertEquals(6, presenze.size());
            registro.close();

            // Il martedì ci sono solo quelli senza gruppo: niente schede
            find(ElencoLezioni.class).single().apriPresenze(martedi.getId());
            assertFalse(find(Tabs.class).from(find(Dialog.class).single()).exists());
        } finally {
            corsoService.eliminaGruppo(piccoli.getId());
            corsoService.eliminaGruppo(grandi.getId());
            assegnati.forEach(a -> corsoService.impostaOrario(chitarra.getId(), a.getId(), null));
        }
    }

    /** Pulsanti delle lezioni nel calendario (griglia ed elenco), eventualmente di un corso. */
    @Test
    @WithUserDetails("allievo_canto1")
    void allievo_nelCalendarioVedeSoloLeNotePubblicheDellaLezione() {
        Corso canto = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow();
        Lezione prima = lezioneService.findByCorso(canto.getId()).getFirst();
        lezioneService.impostaNote(prima.getId(), "Nota interna", "Portare lo spartito", "admin");
        try {
            navigate(CalendarioView.class);
            find(CalendarioView.class).single().vaiA(java.time.YearMonth.of(2024, 10));
            test(lezioniNelCalendario(null).getFirst()).click();
            List<String> testi = find(Span.class).from(find(Dialog.class).single()).all().stream().map(Span::getText).toList();
            assertTrue(testi.contains("Portare lo spartito"));
            assertFalse(testi.contains("Nota interna"));
        } finally {
            lezioneService.impostaNote(prima.getId(), null, null, "admin");
        }
    }

    @Test
    @WithUserDetails("insegnante1")
    @SuppressWarnings("unchecked")
    void insegnante_scriveLeNoteDellaLezione() {
        apriLezioni();
        var grid = test((Grid<Lezione>) find(Grid.class).single());
        Lezione lezione = grid.getRow(0);
        test(pulsante(grid.getCellComponent(0, 6), "Gestisci Presenze")).click();
        try {
            test(find(TextArea.class).withLabel("Note per lo staff").single()).setValue("Ripassare la scala");
            test(find(TextArea.class).withLabel("Note per allievi e genitori").single()).setValue("Portare lo spartito");
            Lezione salvata = lezioneService.findById(lezione.getId());
            assertEquals("Ripassare la scala", salvata.getNote());
            assertEquals("Portare lo spartito", salvata.getNotePubbliche());
            // Riaperta, il dialog mostra le note salvate
            find(Dialog.class).single().close();
            test(pulsante(grid.getCellComponent(0, 6), "Gestisci Presenze")).click();
            assertEquals("Ripassare la scala", find(TextArea.class).withLabel("Note per lo staff").single().getValue());
        } finally {
            lezioneService.impostaNote(lezione.getId(), null, null, "admin");
        }
    }

    @Test
    @WithUserDetails("insegnante1")
    @SuppressWarnings("unchecked")
    void insegnante_cambiaIlLuogoDellaLezione() {
        apriLezioni();
        var grid = test((Grid<Lezione>) find(Grid.class).single());
        Lezione lezione = grid.getRow(0);
        test(pulsante(grid.getCellComponent(0, 6), "Gestisci Presenze")).click();
        try {
            ComboBox<Luogo> luogo = find(ComboBox.class).withLabel("Luogo").single();
            assertEquals("Aula 1", luogo.getPlaceholder());
            test(luogo).selectItem("Palestra");
            assertEquals("Palestra", lezioneService.findById(lezione.getId()).getLuogo().getNome());
            // L'elenco mostra il nuovo luogo con il badge "Cambiato" (colonna Luogo, dopo Orario)
            assertTrue(find(Badge.class).from(grid.getCellComponent(0, 4)).all().stream()
                .anyMatch(b -> "Cambiato".equals(b.getText())));
        } finally {
            lezioneService.impostaLuogo(lezione.getId(), null, "admin");
        }
    }

    private List<Button> lezioniNelCalendario(String corso) {
        return find(Button.class).from(find(CalendarioView.class).single()).all().stream()
            .filter(b -> b.getElement().getAttribute("aria-label") != null
                && b.getElement().getAttribute("aria-label").contains("Corso di")
                && (corso == null || b.getElement().getAttribute("aria-label").contains(corso)))
            .toList();
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
    void insegnante_vedeIlGruppoDegliAllieviNellePresenze() {
        Corso canto = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow();
        Allievo allievo = allievoService.findByUsername("allievo_canto1").orElseThrow();
        GruppoCorso gruppo = corsoService.salvaGruppo(canto.getId(), new GruppoCorso(null, "Avanzati", DayOfWeek.THURSDAY, LocalTime.of(19, 0)));
        corsoService.assegnaGruppo(canto.getId(), allievo.getId(), gruppo.getId());
        try {
            apriLezioni();
            var grid = test((Grid<Lezione>) find(Grid.class).single());
            test(pulsante(grid.getCellComponent(0, 6), "Gestisci Presenze")).click();
            Grid<Presenza> presenze = (Grid<Presenza>) find(Grid.class).from(find(Dialog.class).single()).single();
            assertEquals("Gruppo", presenze.getColumns().get(1).getHeaderText());
            assertTrue(presenze.getColumns().get(1).isVisible());
            var righe = test(presenze);
            int riga = IntStream.range(0, righe.size()).filter(r -> righe.getRow(r).getAllievo().equals(allievo)).findFirst().orElseThrow();
            assertEquals("Avanzati – gio 19:00", righe.getCellText(riga, 1));
        } finally {
            corsoService.assegnaGruppo(canto.getId(), allievo.getId(), null);
            corsoService.eliminaGruppo(gruppo.getId());
        }
    }

    @Test
    @WithUserDetails("insegnante1")
    @SuppressWarnings("unchecked")
    void insegnante_nonVedeCambiaData() {
        apriLezioni();
        var grid = test((Grid<Lezione>) find(Grid.class).single());
        test(pulsante(grid.getCellComponent(0, 6), "Gestisci Presenze")).click();
        assertTrue(find(Button.class).withText("Segna Completata").exists());
        assertFalse(find(Button.class).withText("Cambia data").exists());
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_lezioniDiUnServizioConBadge() {
        Corso pulizia = new Corso();
        pulizia.setTipo(it.attocchi.entity.TipoCorso.SERVIZIO);
        pulizia.setNome("Pulizia");
        pulizia.setDataInizio(LocalDate.of(2024, 10, 1));
        pulizia.setAnnoScolastico("2024/2025");
        pulizia.setNumeroLezioni(2);
        pulizia.setDurataLezioneMinuti(120);
        pulizia.setGiorniSettimana(new java.util.HashSet<>(Set.of(DayOfWeek.TUESDAY)));
        pulizia.setInsegnante(insegnanteService.findByUsername("insegnante1").orElseThrow());
        pulizia = corsoService.save(pulizia);
        try {
            corsoService.generaLezioni(pulizia);
            navigate(LezioniView.class);
            var grid = test((Grid<Lezione>) find(Grid.class).single());
            Long id = pulizia.getId();
            int riga = IntStream.range(0, grid.size()).filter(r -> grid.getRow(r).getCorso().getId().equals(id))
                .findFirst().orElseThrow();
            assertTrue(find(Badge.class).from(grid.getCellComponent(riga, 0)).all().stream()
                .anyMatch(b -> "Servizio".equals(b.getText())));
        } finally {
            corsoService.delete(corsoService.findById(pulizia.getId()));
        }
    }

    @Test
    @WithUserDetails("segreteria")
    @SuppressWarnings("unchecked")
    void segreteria_cambiaLaDataDiUnaLezione() {
        navigate(LezioniView.class);
        var grid = test((Grid<Lezione>) find(Grid.class).single());
        Lezione lezione = grid.getRow(0);
        LocalDate nuova = lezione.getData().plusDays(1);
        navigate("lezioni?lezione=" + lezione.getId(), LezioniView.class);
        test(find(Button.class).withText("Cambia data").single()).click();
        assertEquals(lezione.getData(), find(DatePicker.class).withLabel("Data").single().getValue());
        test(find(DatePicker.class).withLabel("Data").single()).setValue(nuova);
        test(find(TimePicker.class).withLabel("Orario").single()).setValue(LocalTime.of(16, 0));
        test(find(Button.class).withText("Salva").single()).click();
        try {
            Lezione spostata = lezioneService.findById(lezione.getId());
            assertEquals(nuova, spostata.getData());
            assertEquals(lezione.getData(), spostata.getDataOriginale());
            assertEquals(LocalTime.of(16, 0), spostata.getOrarioInizio());
            assertFalse(find(Button.class).withText("Cambia data").exists());
            // Colonne su desktop: Corso, Insegnante, Data, …
            var elenco = test((Grid<Lezione>) find(Grid.class).single());
            int riga = IntStream.range(0, elenco.size()).filter(r -> elenco.getRow(r).equals(lezione)).findFirst().orElseThrow();
            assertTrue(elenco.getCellComponent(riga, 2).getChildren()
                .anyMatch(c -> c instanceof Badge b && "Spostata".equals(b.getText())));
        } finally {
            lezioneService.sposta(lezione.getId(), lezione.getData(), lezione.getOrarioInizio(), "segreteria");
        }
    }

    @Test
    @WithUserDetails("insegnante1")
    @SuppressWarnings("unchecked")
    void insegnante_segnaLezioneNonEffettuataELaRipristina() {
        apriLezioni();
        var grid = test((Grid<Lezione>) find(Grid.class).single());
        Lezione lezione = grid.getRow(0);
        test(pulsante(grid.getCellComponent(0, 6), "Gestisci Presenze")).click();
        test(find(Button.class).withText("Non effettuata").single()).click();
        test(find(TextField.class).withLabel("Motivo").single()).setValue("insegnante assente");
        test(find(Button.class).withText("Segna non effettuata").single()).click();
        try {
            Lezione salvata = lezioneService.findById(lezione.getId());
            assertTrue(salvata.isNonEffettuata());
            assertEquals("insegnante assente", salvata.getMotivoNonEffettuata());
            assertEquals(29, grid.size());

            Schede<FiltroLezioni> filtro = find(Schede.class).single();
            filtro.setValue(FiltroLezioni.NON_EFFETTUATE);
            assertEquals(1, grid.size());
            assertEquals("Non effettuata", ((Badge) grid.getCellComponent(0, 5)).getText());

            // Il dialog della lezione non effettuata non mostra il registro: solo il ripristino
            test(pulsante(grid.getCellComponent(0, 6), "Gestisci Presenze")).click();
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
            Schede<FiltroLezioni> filtro = find(Schede.class).single();
            filtro.setValue(FiltroLezioni.NON_EFFETTUATE);
            test(pulsante(grid.getCellComponent(0, 6), "Gestisci Presenze")).click();
            test(find(Button.class).withText("Recupera").single()).click();

            // Un solo corso: la scelta del corso è nascosta, l'orario è quello del corso; la data è obbligatoria
            assertFalse(find(ComboBox.class).withLabel("Corso").exists());
            assertEquals(canto.getOrarioInizio(), find(TimePicker.class).withLabel("Orario").single().getValue());
            test(find(Button.class).withText("Salva").single()).click();
            assertTrue(find(DatePicker.class).withLabel("Data").single().isInvalid());
            test(find(DatePicker.class).withLabel("Data").single()).setValue(LocalDate.now());
            test(find(Button.class).withText("Salva").single()).click();

            filtro.setValue(FiltroLezioni.DA_FARE);
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
                test(pulsante(grid.getCellComponent(r, 6), "Gestisci Presenze")).click();
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
        test(pulsante(grid.getCellComponent(0, 7), "Gestisci Presenze")).click();
        test(find(Button.class).withText("Non effettuata").single()).click();
        test(find(Button.class).withText("Segna non effettuata").single()).click();
        Lezione recupero = null;
        try {
            assertTrue(lezioneService.findById(lezione.getId()).isNonEffettuata());
            Schede<FiltroLezioni> filtro = find(Schede.class).single();
            filtro.setValue(FiltroLezioni.NON_EFFETTUATE);
            test(pulsante(grid.getCellComponent(0, 7), "Gestisci Presenze")).click();
            test(find(Button.class).withText("Recupera").single()).click();
            test(find(DatePicker.class).withLabel("Data").single()).setValue(LocalDate.now());
            test(find(Button.class).withText("Salva").single()).click();
            recupero = lezioneService.findByCorso(lezione.getCorso().getId()).stream()
                .filter(l -> l.getRecuperoDi() != null).findFirst().orElseThrow();

            // La segreteria non elimina le lezioni aggiunte
            filtro.setValue(FiltroLezioni.DA_FARE);
            Long idRecupero = recupero.getId();
            int riga = IntStream.range(0, grid.size()).filter(r -> grid.getRow(r).getId().equals(idRecupero)).findFirst().orElseThrow();
            test(pulsante(grid.getCellComponent(riga, 7), "Gestisci Presenze")).click();
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
        test(pulsante(grid.getCellComponent(riga, 7), "Gestisci Presenze")).click();
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
    @WithUserDetails("insegnante1")
    @SuppressWarnings("unchecked")
    void insegnante_vedeISuoiAllieviELeLoroLezioni() {
        navigate(MieiAllieviView.class);
        var allievi = test((Grid<AllievoDelCorso>) find(Grid.class).single());
        // Solo i 7 allievi di Canto, nell'ordine dei partecipanti; con un solo corso niente filtro per corso
        assertEquals(7, allievi.size());
        assertEquals("Allievo Canto5", allievi.getCellText(0, 0));
        assertEquals("Corso di Canto", allievi.getCellText(0, 1));
        assertEquals("0 su 30", allievi.getCellText(0, 4));
        assertEquals("30", allievi.getCellText(0, 5));
        assertEquals("30", allievi.getCellText(0, 6));
        assertFalse(find(ComboBox.class).exists());

        test(find(TextField.class).withPlaceholder("Cerca").single()).setValue("canto3");
        assertEquals(1, allievi.size());
        assertEquals("Allievo Canto3", allievi.getCellText(0, 0));

        test(pulsante(allievi.getCellComponent(0, 7), "Lezioni dell'allievo")).click();
        Dialog dialog = find(Dialog.class).single();
        assertEquals("Allievo Canto3 – Corso di Canto", dialog.getHeaderTitle());
        assertTrue(find(Span.class).from(dialog).withText("Mancanti: 30").exists());
        Grid<Presenza> lezioni = find(Grid.class).from(dialog).single();
        assertEquals(30, test(lezioni).size());
        // L'orario di ogni lezione è quello dell'allievo
        assertEquals("18:20", test(lezioni).getCellText(0, 1));
        // L'insegnante vede anche le note per lo staff
        assertTrue(lezioni.getColumns().stream()
            .anyMatch(c -> c.isVisible() && "Note per lo staff".equals(c.getHeaderText())));
    }

    @Test
    @WithUserDetails("allievo_canto1")
    void allievo_nonAccedeAiMieiAllievi() {
        navigate("miei-allievi", RouteNotFoundError.class);
    }

    @Test
    @WithUserDetails("segreteria")
    void segreteria_nonAccedeAiMieiAllievi() {
        navigate("miei-allievi", RouteNotFoundError.class);
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
            test(pulsante(corsi.getCellComponent(0, 10), "Visualizza Presenze")).click();
            Dialog dialog = find(Dialog.class).single();
            // La non effettuata non conta: 29 da fare, nessuna svolta, 30 mancanti
            assertTrue(find(Span.class).from(dialog).withText("Da fare: 29").exists());
            assertTrue(find(Span.class).from(dialog).withText("Svolte: 0 su 30").exists());
            assertTrue(find(Span.class).from(dialog).withText("Mancanti: 30").exists());
            assertTrue(find(Span.class).from(dialog).withText("Presenze: 0").exists());
            Grid<Presenza> gridPresenze = find(Grid.class).from(dialog).single();
            var presenze = test(gridPresenze);
            assertEquals("Lezione non effettuata", presenze.getCellText(0, 3));
            // Le lezioni future non risultano "Presente" e le note per lo staff restano nascoste
            assertEquals("Da fare", presenze.getCellText(1, 3));
            assertFalse(gridPresenze.getColumns().stream()
                .anyMatch(c -> c.isVisible() && "Note per lo staff".equals(c.getHeaderText())));
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
        canto.setInsegnantiFacoltativi(java.util.List.of(marco));
        corsoService.save(canto);
        try {
            apriLezioni();
            var lezioni = test((Grid<Lezione>) find(Grid.class).single());
            Lezione lezione = lezioni.getRow(0);
            test((Button) lezioni.getCellComponent(0, 6).getChildren().findFirst().orElseThrow()).click();

            assertTrue(find(Button.class).withText("Entrambi").exists());
            test(find(Button.class).withText("Solo Rossi").single()).click();
            assertEquals(Set.of(marco), lezioneService.findById(lezione.getId()).getInsegnantiPresenti());
        } finally {
            canto.setInsegnantiFacoltativi(java.util.List.of());
            corsoService.save(canto);
        }
    }

    @Test
    @WithUserDetails("insegnante1")
    @SuppressWarnings("unchecked")
    void insegnante_conTreInsegnantiSceglieIPresentiConLeCaselle() {
        Corso canto = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Canto")).findFirst().orElseThrow();
        Insegnante maria = insegnanteService.findByUsername("insegnante1").orElseThrow();
        Insegnante marco = insegnanteService.findByUsername("insegnante2").orElseThrow();
        User utente = utenteSenzaUsername("Ugo", "Terzo");
        utente.setUsername("ugo.terzo");
        Insegnante ugo = insegnanteService.salva(new Insegnante(), utente, "segreta123");
        canto.setInsegnantiFacoltativi(java.util.List.of(marco, ugo));
        corsoService.save(canto);
        try {
            apriLezioni();
            var lezioni = test((Grid<Lezione>) find(Grid.class).single());
            Lezione lezione = lezioni.getRow(0);
            test((Button) lezioni.getCellComponent(0, 6).getChildren().findFirst().orElseThrow()).click();

            assertFalse(find(Button.class).withText("Entrambi").exists());
            CheckboxGroup<Insegnante> caselle = find(CheckboxGroup.class).all().stream()
                .filter(c -> c.getAriaLabel().filter("Insegnanti presenti"::equals).isPresent()).findFirst().orElseThrow();
            assertEquals(Set.of(maria, marco, ugo), caselle.getValue());
            test(caselle).deselectItem("Marco Rossi");
            assertEquals(Set.of(maria, ugo), lezioneService.findById(lezione.getId()).getInsegnantiPresenti());
        } finally {
            Corso ripristino = corsoService.findById(canto.getId());
            ripristino.setInsegnantiFacoltativi(java.util.List.of());
            corsoService.save(ripristino);
            insegnanteRepository.delete(ugo);
            userService.delete(ugo.getUser());
        }
    }

    @Test
    @WithUserDetails("insegnante1")
    @SuppressWarnings("unchecked")
    void insegnante_conUnSoloInsegnanteNonVedeLaSceltaDegliInsegnanti() {
        apriLezioni();
        var lezioni = test((Grid<Lezione>) find(Grid.class).single());
        test((Button) lezioni.getCellComponent(0, 6).getChildren().findFirst().orElseThrow()).click();
        assertFalse(find(Button.class).withText("Entrambi").exists());
    }

    @Test
    @WithUserDetails("insegnante1")
    void insegnante_segnaAssenzaGiustificataConUnTocco() {
        apriLezioni();
        var lezioni = test(find(Grid.class).single());
        Button gestisciPresenze = (Button) lezioni.getCellComponent(0, 6).getChildren().findFirst().orElseThrow();
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
            Schede<FiltroLezioni> filtro = find(Schede.class).single();

            // default: solo le lezioni da fare
            assertEquals(FiltroLezioni.DA_FARE, filtro.getValue());
            assertEquals(29, grid.size());

            filtro.setValue(FiltroLezioni.COMPLETATE);
            assertEquals(1, grid.size());
            Badge badge = (Badge) grid.getCellComponent(0, 5);
            assertEquals("Completata", badge.getText());
            assertTrue(badge.getThemeNames().contains(BadgeVariant.SUCCESS.getVariantName()));

            filtro.setValue(FiltroLezioni.TUTTE);
            assertEquals(30, grid.size());

            filtro.setValue(FiltroLezioni.DA_FARE);
            assertEquals(29, grid.size());
            assertEquals("Da fare", ((Badge) grid.getCellComponent(0, 5)).getText());
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
    @WithUserDetails("segreteria")
    void segreteria_aggiornaIlProprioProfilo() {
        navigate(HomeView.class);
        assertTrue(voceDiMenu("Il mio profilo"));
        navigate(ProfiloView.class);
        assertTrue(find(TextField.class).withLabel("Username").single().isReadOnly());
        TextField ruoli = find(TextField.class).withLabel("Ruoli").single();
        assertTrue(ruoli.isReadOnly());
        assertEquals("SEGRETERIA", ruoli.getValue());
        String telefono = userService.findByUsername("segreteria").getTelefono();
        try {
            test(find(TextField.class).withLabel("Telefono").single()).setValue("333 7654321");
            test(find(Button.class).withText("Salva").single()).click();
            assertEquals("333 7654321", userService.findByUsername("segreteria").getTelefono());
            assertTrue(find(Button.class).withText("Cambia password").exists());
        } finally {
            User user = userService.findByUsername("segreteria");
            user.setTelefono(telefono);
            userService.aggiornaContatti("segreteria", user);
        }
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
        assertTrue(find(BigDecimalField.class).withLabel("Rimborso forfettario annuo").single().isReadOnly());

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
        assertEquals("1", grid.getCellText(0, 3));
        assertEquals("1", grid.getCellText(1, 3));
    }

    @Test
    @WithUserDetails("genitore3")
    @SuppressWarnings("unchecked")
    void genitore_vedeIDatiMancantiDeiFigli() {
        Allievo canto3 = allievoService.findByUsername("allievo_canto3").orElseThrow();
        String codiceFiscale = canto3.getCodiceFiscale();
        canto3.setCodiceFiscale(null);
        allievoService.save(canto3);
        try {
            navigate(FigliView.class);
            var grid = test((Grid<Allievo>) find(Grid.class).single());
            int riga = IntStream.range(0, grid.size()).filter(r -> grid.getRow(r).equals(canto3)).findFirst().orElseThrow();
            Badge badge = (Badge) grid.getCellComponent(riga, 0).getChildren().filter(Badge.class::isInstance).findFirst().orElseThrow();
            assertEquals("Dati mancanti", badge.getText());
            assertEquals("Manca codice fiscale di Allievo Canto3: serve per le ricevute. Completalo con il pulsante Modifica.",
                Tooltip.forComponent(badge).getText());
            // L'altro figlio ha i dati: niente badge
            assertTrue(grid.getCellComponent(1 - riga, 0).getChildren().noneMatch(Badge.class::isInstance));

            // Nel dialog di modifica l'avviso in alto
            test(pulsante(grid.getCellComponent(riga, 4), "Modifica")).click();
            Dialog dialog = find(Dialog.class).single();
            assertTrue(find(Span.class).from(dialog).all().stream()
                .anyMatch(s -> "Manca codice fiscale di Allievo Canto3: serve per le ricevute.".equals(s.getText())));
        } finally {
            canto3.setCodiceFiscale(codiceFiscale);
            allievoService.save(canto3);
        }
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
            test(pulsante(figli.getCellComponent(riga, 4), "Visualizza Dettagli")).click();

            var corsi = test((Grid<Corso>) find(Grid.class).from(find(Dialog.class).single()).single());
            assertEquals("Corso di Canto", corsi.getCellText(0, 0));
            // Prezzo di listino, pre-iscrizione e quota dovuta (senza sconto: nessun badge)
            assertEquals(List.of("600,00 €", "510,00 €"), List.of(corsi.getCellText(0, 5), corsi.getCellText(0, 6)));
            assertEquals(List.of("600,00 €"), corsi.getCellComponent(0, 7).getChildren()
                .map(c -> c instanceof Badge b ? b.getText() : ((Span) c).getText()).toList());
            assertEquals("Parziale 33%", ((Badge) corsi.getCellComponent(0, 8)).getText());
            test(pulsante(corsi.getCellComponent(0, 9), "Pagamenti")).click();

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
            // Prezzo di listino, pre-iscrizione e quota dovuta con il badge dello sconto
            assertEquals(List.of("600,00 €", "510,00 €"), List.of(corsi.getCellText(0, 7), corsi.getCellText(0, 8)));
            assertEquals(List.of("510,00 €", "Sconto -90,00 €"), corsi.getCellComponent(0, 9).getChildren()
                .map(c -> c instanceof Badge b ? b.getText() : ((Span) c).getText()).toList());
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
        String codiceFiscale = genitoreService.findByUsername("genitore3").orElseThrow().getCodiceFiscale();
        navigate(ProfiloView.class);
        assertTrue(find(TextField.class).withLabel("Username").single().isReadOnly());
        test(find(TextField.class).withLabel("Luogo di nascita").single()).setValue("Firenze");
        // Il codice fiscale del genitore è solo quello dell'intestazione dei documenti fiscali
        assertFalse(find(TextField.class).withLabel("Codice fiscale").exists());
        assertTrue(find(H3.class).withText("Intestazione dei documenti fiscali").exists());
        TextField nome = find(TextField.class).withLabel("Nome dell'intestatario").single();
        assertEquals("Genitore3", nome.getPlaceholder());
        TextField codice = find(TextField.class).withLabel("Codice fiscale dell'intestatario").single();
        assertEquals(codiceFiscale, codice.getValue());
        test(nome).setValue(" Anna ");
        test(codice).setValue("bncnna80a41h501r");
        test(find(Button.class).withText("Salva").single()).click();

        Genitore genitore = genitoreService.findByUsername("genitore3").orElseThrow();
        try {
            assertEquals("Firenze", genitore.getLuogoNascita());
            assertEquals("Anna", genitore.getIntestazioneNome());
            assertNull(genitore.getIntestazioneCognome());
            assertEquals("BNCNNA80A41H501R", genitore.getCodiceFiscale());
            assertEquals("Anna Cognome3", genitore.getNomeIntestatario());
            assertTrue(genitore.getUser().getRoles().contains("GENITORE"));
            assertTrue(find(Button.class).withText("Cambia password").exists());
        } finally {
            genitore.setIntestazioneNome(null);
            genitore.setCodiceFiscale(codiceFiscale);
            genitoreService.save(genitore);
        }
    }

    @Test
    @WithUserDetails("genitore3")
    @SuppressWarnings("unchecked")
    void genitore_modificaIDatiDiUnFiglio() {
        navigate(FigliView.class);
        var figli = test((Grid<Allievo>) find(Grid.class).single());
        int riga = IntStream.range(0, figli.size())
            .filter(r -> figli.getCellText(r, 0).equals("Allievo Canto3")).findFirst().orElseThrow();
        test(pulsante(figli.getCellComponent(riga, 4), "Modifica")).click();

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
            // La ricevuta è in Documenti: menu con anteprima in una nuova scheda e invio alla propria email
            navigate(DocumentiView.class);
            var documenti = test((Grid<DocumentoFamiglia>) find(Grid.class).single());
            int ricevuta = IntStream.range(0, documenti.size())
                .filter(r -> documenti.getCellText(r, 0).equals("Ricevuta di pagamento")).findFirst().orElseThrow();
            assertEquals("Allievo Canto3", documenti.getCellText(ricevuta, 1));
            assertAnteprimaEInvio(documenti.getCellComponent(ricevuta, 5));

            var pdf = documentiService.ricevutaPer(canto.getId(), figlio.getId(), "genitore3");
            assertTrue(pdf.nomeFile().startsWith("ricevuta-corso-di-canto-"));
            String testo;
            try (var documento = org.apache.pdfbox.Loader.loadPDF(pdf.contenuto())) {
                testo = new org.apache.pdfbox.text.PDFTextStripper().getText(documento);
            }
            assertTrue(testo.contains("Allievo Canto3"), testo);
            // L'indirizzo può andare a capo nella colonna dell'allievo
            assertTrue(testo.replaceAll("\\s+", " ").contains("Via Roma 3, 00184 Roma (RM) (del genitore)"), testo);
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
        // Con genitore: i suoi contatti e il suo indirizzo come riferimento, quelli propri facoltativi
        assertTrue(find(Span.class).withText("Genitore1 Cognome1").exists());
        assertTrue(find(Span.class).withText("Indirizzo: Via Roma 1, 00184 Roma (RM)").exists());
        assertTrue(find(com.vaadin.flow.component.details.Details.class).single().getSummaryText().contains("facoltativi"));
        assertFalse(find(Button.class).withText("Salva").exists());
        assertFalse(find(Button.class).withText("Cambia password").exists());
        // Codice fiscale nei dati per i documenti; l'intestatario dell'attestazione è del genitore
        assertTrue(find(H3.class).withText("Dati per i documenti").exists());
        assertFalse(find(TextField.class).withLabel("Nome dell'intestatario").all().stream().anyMatch(Component::isVisible));
    }

    @Test
    @WithUserDetails("allievo_canto1")
    void allievoAdulto_impostaLIntestazioneDeiDocumentiFiscali() {
        Allievo allievo = allievoService.findByUsername("allievo_canto1").orElseThrow();
        Genitore genitore = allievo.getGenitore();
        allievo.setGenitore(null);
        allievoService.save(allievo);
        try {
            navigate(ProfiloView.class);
            // Il resto resta in sola lettura; il proprio codice fiscale è quello dell'allievo
            assertTrue(find(TextField.class).withLabel("Codice fiscale").single().isReadOnly());
            assertTrue(find(H3.class).withText("Dati per i documenti").exists());
            var riquadro = find(com.vaadin.flow.component.details.Details.class).all().stream()
                .filter(d -> d.getSummaryText().equals("Intesta l'attestazione a un'altra persona")).findFirst().orElseThrow();
            assertFalse(riquadro.isOpened());
            riquadro.setOpened(true);
            TextField cognome = find(TextField.class).withLabel("Cognome dell'intestatario").single();
            assertFalse(cognome.isReadOnly());
            test(cognome).setValue("Bianchi");
            test(find(TextField.class).withLabel("Codice fiscale dell'intestatario").single()).setValue("BNCNNA80A41H501R");
            test(find(Button.class).withText("Salva").single()).click();

            Allievo salvato = allievoService.findByUsername("allievo_canto1").orElseThrow();
            assertEquals("Bianchi", salvato.getIntestazioneCognome());
            assertEquals("BNCNNA80A41H501R", salvato.getCodiceFiscaleIntestatario());
            assertEquals(allievo.getUser().getNome() + " Bianchi", salvato.getNomeIntestatario());
            assertEquals(allievo.getCodiceFiscale(), salvato.getCodiceFiscale());
        } finally {
            Allievo ripristino = allievoService.findByUsername("allievo_canto1").orElseThrow();
            ripristino.setGenitore(genitore);
            ripristino.setIntestazioneCognome(null);
            ripristino.setIntestazioneCodiceFiscale(null);
            allievoService.save(ripristino);
        }
    }

    @Test
    @WithUserDetails("allievo_chitarra1")
    void allievo_vedeSoloISuoiGiorniDiLezione() {
        Corso chitarra = corsoService.findAll().stream().filter(c -> c.getNome().equals("Corso di Chitarra")).findFirst().orElseThrow();
        Allievo allievo = allievoService.findByUsername("allievo_chitarra1").orElseThrow();
        GruppoCorso gruppo = corsoService.salvaGruppo(chitarra.getId(), new GruppoCorso(null, null, DayOfWeek.TUESDAY, LocalTime.of(17, 0)));
        corsoService.assegnaGruppo(chitarra.getId(), allievo.getId(), gruppo.getId());
        try {
            navigate(MieiCorsiView.class);
            var corsi = test(find(Grid.class).single());
            assertEquals("Giorni", ((Grid<?>) find(Grid.class).single()).getColumns().get(2).getHeaderText());
            assertEquals("Martedì 17:00", corsi.getCellText(0, 2));
        } finally {
            corsoService.assegnaGruppo(chitarra.getId(), allievo.getId(), null);
            corsoService.impostaOrario(chitarra.getId(), allievo.getId(), null);
            corsoService.eliminaGruppo(gruppo.getId());
        }
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
