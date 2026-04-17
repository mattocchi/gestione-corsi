package it.attocchi;

import it.attocchi.entity.*;
import it.attocchi.service.UserService;
import it.attocchi.service.CorsoService;
import it.attocchi.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserService userService;
    private final DocenteRepository docenteRepository;
    private final StudenteRepository studenteRepository;
    private final GenitoreRepository genitoreRepository;
    private final CorsoService corsoService;
    private final CorsoRepository corsoRepository;

    public DataInitializer(UserService userService, DocenteRepository docenteRepository,
                          StudenteRepository studenteRepository, GenitoreRepository genitoreRepository,
                          CorsoService corsoService, CorsoRepository corsoRepository) {
        this.userService = userService;
        this.docenteRepository = docenteRepository;
        this.studenteRepository = studenteRepository;
        this.genitoreRepository = genitoreRepository;
        this.corsoService = corsoService;
        this.corsoRepository = corsoRepository;
    }

    @Override
    public void run(String... args) {
        if (userService.findAll().isEmpty()) {
            // Crea utente admin
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword("admin");
            admin.setNome("Admin");
            admin.setCognome("Sistema");
            admin.setEmail("admin@example.com");
            admin.setRoles(new HashSet<>());
            admin.getRoles().add("ADMIN");
            userService.save(admin);

            // Crea docente1 per corso di canto
            User userDocente1 = new User();
            userDocente1.setUsername("docente1");
            userDocente1.setPassword("docente1");
            userDocente1.setNome("Maria");
            userDocente1.setCognome("Verdi");
            userDocente1.setEmail("docente1@example.com");
            userDocente1.setRoles(new HashSet<>());
            userDocente1.getRoles().add("DOCENTE");
            userDocente1 = userService.save(userDocente1);

            Docente docente1 = new Docente();
            docente1.setUser(userDocente1);
            docente1.setSpecializzazione("Canto");
            docente1 = docenteRepository.save(docente1);

            // Crea docente2 per corso di chitarra
            User userDocente2 = new User();
            userDocente2.setUsername("docente2");
            userDocente2.setPassword("docente2");
            userDocente2.setNome("Marco");
            userDocente2.setCognome("Rossi");
            userDocente2.setEmail("docente2@example.com");
            userDocente2.setRoles(new HashSet<>());
            userDocente2.getRoles().add("DOCENTE");
            userDocente2 = userService.save(userDocente2);

            Docente docente2 = new Docente();
            docente2.setUser(userDocente2);
            docente2.setSpecializzazione("Chitarra");
            docente2 = docenteRepository.save(docente2);

            // Crea genitori
            Genitore[] genitori = new Genitore[9];
            for (int i = 0; i < 9; i++) {
                User userGenitore = new User();
                userGenitore.setUsername("genitore" + (i + 1));
                userGenitore.setPassword("genitore" + (i + 1));
                userGenitore.setNome("Genitore" + (i + 1));
                userGenitore.setCognome("Cognome" + (i + 1));
                userGenitore.setEmail("genitore" + (i + 1) + "@example.com");
                userGenitore.setRoles(new HashSet<>());
                userGenitore.getRoles().add("GENITORE");
                userGenitore = userService.save(userGenitore);

                Genitore genitore = new Genitore();
                genitore.setUser(userGenitore);
                genitore.setCodiceFiscale("GEN" + String.format("%02d", i + 1) + "70A01H501Z");
                genitori[i] = genitoreRepository.save(genitore);
            }

            // Crea 7 studenti per corso di canto
            Studente[] studentiCanto = new Studente[7];
            for (int i = 0; i < 7; i++) {
                User userStudente = new User();
                userStudente.setUsername("studente_canto" + (i + 1));
                userStudente.setPassword("studente" + (i + 1));
                userStudente.setNome("Studente");
                userStudente.setCognome("Canto" + (i + 1));
                userStudente.setEmail("studente_canto" + (i + 1) + "@example.com");
                userStudente.setRoles(new HashSet<>());
                userStudente.getRoles().add("STUDENTE");
                userStudente = userService.save(userStudente);

                Studente studente = new Studente();
                studente.setUser(userStudente);
                studente.setCodiceFiscale("STC" + String.format("%02d", i + 1) + "05A01H501Z");
                studente.setDataNascita(LocalDate.of(2008 + i, 3, 15));
                studente.setGenitore(genitori[i]);
                studentiCanto[i] = studenteRepository.save(studente);
            }

            // Crea 6 studenti per corso di chitarra (4 martedì + 2 sabato)
            Studente[] studentiChitarra = new Studente[6];
            for (int i = 0; i < 6; i++) {
                User userStudente = new User();
                userStudente.setUsername("studente_chitarra" + (i + 1));
                userStudente.setPassword("studente" + (i + 8));
                userStudente.setNome("Studente");
                userStudente.setCognome("Chitarra" + (i + 1));
                userStudente.setEmail("studente_chitarra" + (i + 1) + "@example.com");
                userStudente.setRoles(new HashSet<>());
                userStudente.getRoles().add("STUDENTE");
                userStudente = userService.save(userStudente);

                Studente studente = new Studente();
                studente.setUser(userStudente);
                studente.setCodiceFiscale("STG" + String.format("%02d", i + 1) + "06A01H501Z");
                studente.setDataNascita(LocalDate.of(2009 + i, 5, 20));
                studente.setGenitore(genitori[i + 2]);
                studentiChitarra[i] = studenteRepository.save(studente);
            }

            // Crea corso di Canto - Giovedì
            Corso corsoCanto = new Corso();
            corsoCanto.setNome("Corso di Canto");
            corsoCanto.setDescrizione("Corso di canto per principianti e intermedi");
            corsoCanto.setDataInizio(LocalDate.of(2024, 10, 2)); // 2 ottobre 2024 (giovedì)
            corsoCanto.setNumeroLezioni(30);
            corsoCanto.setDurataLezioneMinuti(40);
            corsoCanto.setOrarioInizio(LocalTime.of(17, 0));
            Set<DayOfWeek> giorniCanto = new HashSet<>();
            giorniCanto.add(DayOfWeek.THURSDAY);
            corsoCanto.setGiorniSettimana(giorniCanto);
            corsoCanto.setDocente(docente1);
            corsoCanto.setCostoTotale(new BigDecimal("600.00"));
            corsoCanto.setCostoPerLezione(new BigDecimal("20.00"));

            Set<Studente> studentiSetCanto = new HashSet<>();
            for (Studente s : studentiCanto) {
                studentiSetCanto.add(s);
            }
            corsoCanto.setStudenti(studentiSetCanto);
            corsoCanto = corsoRepository.save(corsoCanto);

            // Crea corso di Chitarra - Martedì e Sabato
            Corso corsoChitarra = new Corso();
            corsoChitarra.setNome("Corso di Chitarra");
            corsoChitarra.setDescrizione("Corso di chitarra classica e moderna");
            corsoChitarra.setDataInizio(LocalDate.of(2024, 9, 30)); // 30 settembre 2024 (lunedì, ma primo martedì è 1 ottobre)
            corsoChitarra.setNumeroLezioni(30);
            corsoChitarra.setDurataLezioneMinuti(40);
            corsoChitarra.setOrarioInizio(LocalTime.of(18, 0));
            Set<DayOfWeek> giorniChitarra = new HashSet<>();
            giorniChitarra.add(DayOfWeek.TUESDAY);
            giorniChitarra.add(DayOfWeek.SATURDAY);
            corsoChitarra.setGiorniSettimana(giorniChitarra);
            corsoChitarra.setDocente(docente2);
            corsoChitarra.setCostoTotale(new BigDecimal("600.00"));
            corsoChitarra.setCostoPerLezione(new BigDecimal("20.00"));

            Set<Studente> studentiSetChitarra = new HashSet<>();
            for (Studente s : studentiChitarra) {
                studentiSetChitarra.add(s);
            }
            corsoChitarra.setStudenti(studentiSetChitarra);
            corsoChitarra = corsoRepository.save(corsoChitarra);

            // Genera lezioni per entrambi i corsi
            System.out.println("Generazione lezioni per corso di Canto...");
            corsoService.generaLezioni(corsoCanto);

            System.out.println("Generazione lezioni per corso di Chitarra...");
            corsoService.generaLezioni(corsoChitarra);

            System.out.println("\n===========================================");
            System.out.println("Dati di test inizializzati con successo!");
            System.out.println("===========================================");
            System.out.println("ADMIN:");
            System.out.println("  - admin/admin");
            System.out.println("\nDOCENTI:");
            System.out.println("  - docente1/docente1 (Canto)");
            System.out.println("  - docente2/docente2 (Chitarra)");
            System.out.println("\nGENITORI:");
            for (int i = 0; i < 9; i++) {
                System.out.println("  - genitore" + (i + 1) + "/genitore" + (i + 1));
            }
            System.out.println("\nSTUDENTI CANTO (7):");
            for (int i = 0; i < 7; i++) {
                System.out.println("  - studente_canto" + (i + 1) + "/studente" + (i + 1));
            }
            System.out.println("\nSTUDENTI CHITARRA (6):");
            for (int i = 0; i < 6; i++) {
                System.out.println("  - studente_chitarra" + (i + 1) + "/studente" + (i + 8));
            }
            System.out.println("\nCORSI:");
            System.out.println("  - Corso di Canto: 7 studenti, Giovedì, 30 lezioni da 40 min");
            System.out.println("  - Corso di Chitarra: 6 studenti, Martedì e Sabato, 30 lezioni da 40 min");
            System.out.println("===========================================\n");
        }
    }
}
