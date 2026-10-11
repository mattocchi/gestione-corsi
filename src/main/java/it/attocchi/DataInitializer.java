package it.attocchi;

import it.attocchi.entity.*;
import it.attocchi.service.UserService;
import it.attocchi.service.CorsoService;
import it.attocchi.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Popola il database con dati di test. Attivo solo nel profilo {@code dev}.
 */
@Component
@Profile("dev")
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserService userService;
    private final InsegnanteRepository insegnanteRepository;
    private final AllievoRepository allievoRepository;
    private final GenitoreRepository genitoreRepository;
    private final CorsoService corsoService;
    private final CorsoRepository corsoRepository;
    private final IscrizioneRepository iscrizioneRepository;

    public DataInitializer(UserService userService, InsegnanteRepository insegnanteRepository,
                          AllievoRepository allievoRepository, GenitoreRepository genitoreRepository,
                          CorsoService corsoService, CorsoRepository corsoRepository,
                          IscrizioneRepository iscrizioneRepository) {
        this.userService = userService;
        this.insegnanteRepository = insegnanteRepository;
        this.allievoRepository = allievoRepository;
        this.genitoreRepository = genitoreRepository;
        this.corsoService = corsoService;
        this.corsoRepository = corsoRepository;
        this.iscrizioneRepository = iscrizioneRepository;
    }

    @Override
    public void run(String... args) {
        if (userService.findAll().isEmpty()) {
            // Crea utente admin
            User admin = new User();
            admin.setUsername("admin");
            admin.setNome("Admin");
            admin.setCognome("Sistema");
            admin.setEmail("admin@example.com");
            admin.setRoles(new HashSet<>());
            admin.getRoles().add("ADMIN");
            userService.save(admin, "admin");

            // Crea utente della segreteria
            User segreteria = new User();
            segreteria.setUsername("segreteria");
            segreteria.setNome("Paola");
            segreteria.setCognome("Bianchi");
            segreteria.setEmail("segreteria@example.com");
            segreteria.setRoles(new HashSet<>());
            segreteria.getRoles().add("SEGRETERIA");
            userService.save(segreteria, "segreteria");

            // Crea insegnante1 per corso di canto
            User userInsegnante1 = new User();
            userInsegnante1.setUsername("insegnante1");
            userInsegnante1.setNome("Maria");
            userInsegnante1.setCognome("Verdi");
            userInsegnante1.setEmail("insegnante1@example.com");
            userInsegnante1.setRoles(new HashSet<>());
            userInsegnante1.getRoles().add("INSEGNANTE");
            userInsegnante1 = userService.save(userInsegnante1, "insegnante1");

            Insegnante insegnante1 = new Insegnante();
            insegnante1.setUser(userInsegnante1);
            insegnante1.setSpecializzazione("Canto");
            insegnante1.setTariffaOraria(new BigDecimal("25.00"));
            insegnante1 = insegnanteRepository.save(insegnante1);

            // Crea insegnante2 per corso di chitarra
            User userInsegnante2 = new User();
            userInsegnante2.setUsername("insegnante2");
            userInsegnante2.setNome("Marco");
            userInsegnante2.setCognome("Rossi");
            userInsegnante2.setEmail("insegnante2@example.com");
            userInsegnante2.setRoles(new HashSet<>());
            userInsegnante2.getRoles().add("INSEGNANTE");
            userInsegnante2 = userService.save(userInsegnante2, "insegnante2");

            Insegnante insegnante2 = new Insegnante();
            insegnante2.setUser(userInsegnante2);
            insegnante2.setSpecializzazione("Chitarra");
            insegnante2.setTariffaOraria(new BigDecimal("30.00"));
            insegnante2 = insegnanteRepository.save(insegnante2);

            // Crea genitori
            Genitore[] genitori = new Genitore[9];
            for (int i = 0; i < 9; i++) {
                User userGenitore = new User();
                userGenitore.setUsername("genitore" + (i + 1));
                userGenitore.setNome("Genitore" + (i + 1));
                userGenitore.setCognome("Cognome" + (i + 1));
                userGenitore.setEmail("genitore" + (i + 1) + "@example.com");
                userGenitore.setRoles(new HashSet<>());
                userGenitore.getRoles().add("GENITORE");
                userGenitore = userService.save(userGenitore, "genitore" + (i + 1));

                Genitore genitore = new Genitore();
                genitore.setUser(userGenitore);
                genitore.setCodiceFiscale(codiceFiscale("CGNGNT70A" + String.format("%02d", i + 1) + "H501"));
                genitore.setIndirizzo(new Indirizzo("Via Roma " + (i + 1), "00184", "Roma", "RM"));
                genitori[i] = genitoreRepository.save(genitore);
            }

            // Crea 7 allievi per corso di canto
            Allievo[] allieviCanto = new Allievo[7];
            for (int i = 0; i < 7; i++) {
                User userAllievo = new User();
                userAllievo.setUsername("allievo_canto" + (i + 1));
                userAllievo.setNome("Allievo");
                userAllievo.setCognome("Canto" + (i + 1));
                userAllievo.setEmail("allievo_canto" + (i + 1) + "@example.com");
                userAllievo.setRoles(new HashSet<>());
                userAllievo.getRoles().add("ALLIEVO");
                userAllievo = userService.save(userAllievo, "allievo" + (i + 1));

                Allievo allievo = new Allievo();
                allievo.setUser(userAllievo);
                allievo.setCodiceFiscale(codiceFiscale("CNTSTD" + String.format("%02d", (8 + i) % 100) + "C15H501"));
                allievo.setDataNascita(LocalDate.of(2008 + i, 3, 15));
                allievo.setGenitore(genitori[i]);
                allieviCanto[i] = allievoRepository.save(allievo);
            }

            // Crea 6 allievi per corso di chitarra (4 martedì + 2 sabato)
            Allievo[] allieviChitarra = new Allievo[6];
            for (int i = 0; i < 6; i++) {
                User userAllievo = new User();
                userAllievo.setUsername("allievo_chitarra" + (i + 1));
                userAllievo.setNome("Allievo");
                userAllievo.setCognome("Chitarra" + (i + 1));
                userAllievo.setEmail("allievo_chitarra" + (i + 1) + "@example.com");
                userAllievo.setRoles(new HashSet<>());
                userAllievo.getRoles().add("ALLIEVO");
                userAllievo = userService.save(userAllievo, "allievo" + (i + 8));

                Allievo allievo = new Allievo();
                allievo.setUser(userAllievo);
                allievo.setCodiceFiscale(codiceFiscale("CHTSTD" + String.format("%02d", (9 + i) % 100) + "E20H501"));
                allievo.setDataNascita(LocalDate.of(2009 + i, 5, 20));
                allievo.setGenitore(genitori[i + 2]);
                allieviChitarra[i] = allievoRepository.save(allievo);
            }

            // Crea corso di Canto - Giovedì
            Corso corsoCanto = new Corso();
            corsoCanto.setNome("Corso di Canto");
            corsoCanto.setDescrizione("Corso di canto per principianti e intermedi");
            corsoCanto.setDataInizio(LocalDate.of(2024, 10, 2)); // 2 ottobre 2024 (giovedì)
            corsoCanto.setAnnoScolastico(AnnoScolastico.di(corsoCanto.getDataInizio()));
            corsoCanto.setNumeroLezioni(30);
            corsoCanto.setDurataLezioneMinuti(40);
            corsoCanto.setOrarioInizio(LocalTime.of(17, 0));
            Set<DayOfWeek> giorniCanto = new HashSet<>();
            giorniCanto.add(DayOfWeek.THURSDAY);
            corsoCanto.setGiorniSettimana(giorniCanto);
            corsoCanto.setInsegnante(insegnante1);
            corsoCanto.setCostoTotale(new BigDecimal("600.00"));
            corsoCanto.setCostoPerLezione(new BigDecimal("20.00"));
            corsoCanto = corsoRepository.save(corsoCanto);
            // Orari di lezione individuali: i primi 5 in ordine inverso (Canto5 alle 17:00, Canto1 alle 19:40),
            // Canto6 e Canto7 ancora senza orario
            for (int i = 0; i < allieviCanto.length; i++) {
                Iscrizione iscrizione = new Iscrizione(corsoCanto, allieviCanto[i]);
                if (i < 5) {
                    iscrizione.setOrarioLezione(LocalTime.of(17, 0).plusMinutes(40L * (4 - i)));
                }
                iscrizioneRepository.save(iscrizione);
            }

            // Crea corso di Chitarra - Martedì e Sabato
            Corso corsoChitarra = new Corso();
            corsoChitarra.setNome("Corso di Chitarra");
            corsoChitarra.setDescrizione("Corso di chitarra classica e moderna");
            corsoChitarra.setDataInizio(LocalDate.of(2024, 9, 30)); // 30 settembre 2024 (lunedì, ma primo martedì è 1 ottobre)
            corsoChitarra.setAnnoScolastico(AnnoScolastico.di(corsoChitarra.getDataInizio()));
            corsoChitarra.setNumeroLezioni(30);
            corsoChitarra.setDurataLezioneMinuti(40);
            corsoChitarra.setOrarioInizio(LocalTime.of(18, 0));
            Set<DayOfWeek> giorniChitarra = new HashSet<>();
            giorniChitarra.add(DayOfWeek.TUESDAY);
            giorniChitarra.add(DayOfWeek.SATURDAY);
            corsoChitarra.setGiorniSettimana(giorniChitarra);
            corsoChitarra.setInsegnante(insegnante2);
            corsoChitarra.setCostoTotale(new BigDecimal("600.00"));
            corsoChitarra.setCostoPerLezione(new BigDecimal("20.00"));
            corsoChitarra = corsoRepository.save(corsoChitarra);
            for (Allievo s : allieviChitarra) {
                iscrizioneRepository.save(new Iscrizione(corsoChitarra, s));
            }

            // Genera lezioni per entrambi i corsi
            log.info("Generazione lezioni per corso di Canto...");
            corsoService.generaLezioni(corsoCanto);

            log.info("Generazione lezioni per corso di Chitarra...");
            corsoService.generaLezioni(corsoChitarra);

            log.info("""
                Dati di test inizializzati:
                  ADMIN:    admin/admin
                  SEGRETERIA: segreteria/segreteria
                  INSEGNANTI:  insegnante1/insegnante1 (Canto), insegnante2/insegnante2 (Chitarra)
                  GENITORI: genitore1..9 / genitore1..9
                  ALLIEVI: allievo_canto1..7 / allievo1..7, allievo_chitarra1..6 / allievo8..13
                  CORSI:    Canto (giovedì), Chitarra (martedì e sabato), 30 lezioni da 40 min""");
        }
    }

    /** Codice fiscale di esempio valido: i primi 15 caratteri più il carattere di controllo. */
    private static String codiceFiscale(String primi15) {
        return primi15 + CodiceFiscale.carattereDiControllo(primi15);
    }
}
