package it.attocchi.service;

import it.attocchi.entity.Allievo;
import it.attocchi.entity.Genitore;
import it.attocchi.entity.Insegnante;
import it.attocchi.entity.User;
import it.attocchi.repository.AllievoRepository;
import it.attocchi.repository.GenitoreRepository;
import it.attocchi.repository.RecuperoPasswordRepository;
import it.attocchi.repository.InsegnanteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Schede di insegnante, genitore e allievo create dal ruolo. */
@SpringBootTest
class SchedeRuoliServiceTest {

    @Autowired
    private SchedeRuoliService schedeRuoliService;

    @Autowired
    private AllievoService allievoService;

    @Autowired
    private UserService userService;

    @Autowired
    private InsegnanteRepository insegnanteRepository;

    @Autowired
    private GenitoreRepository genitoreRepository;

    @Autowired
    private AllievoRepository allievoRepository;

    @Autowired
    private RecuperoPasswordRepository recuperoPasswordRepository;

    @Test
    void ruoliInsegnanteEGenitore_creanoLeSchedeConLAnagraficaDellAllievo() {
        Allievo allievo = allievoService.creaRapido("Sara", "Ruoli", null, null);
        allievo.setCodiceFiscale("RLUSRA80A41H501X");
        allievo.getIndirizzo().setComune("Roma");
        allievoService.save(allievo);
        User user = allievo.getUser();
        try {
            user.getRoles().add("INSEGNANTE");
            user.getRoles().add("GENITORE");
            User salvato = schedeRuoliService.salvaUtente(user, null);

            Insegnante insegnante = insegnanteRepository.findByUserId(user.getId()).orElseThrow();
            Genitore genitore = genitoreRepository.findByUserId(user.getId()).orElseThrow();
            assertEquals("RLUSRA80A41H501X", insegnante.getCodiceFiscale());
            assertEquals("Roma", genitore.getIndirizzo().getComune());
            assertEquals("Sara Ruoli", genitore.getNomeCompleto());

            // Nessun doppione salvando di nuovo; togliere il ruolo non cancella la scheda
            // Come dalla UI: l'utente ricaricato dopo il salvataggio
            User ricaricato = userService.findAll().stream().filter(u -> u.getId().equals(salvato.getId())).findFirst().orElseThrow();
            ricaricato.getRoles().remove("GENITORE");
            schedeRuoliService.salvaUtente(ricaricato, null);
            assertEquals(0, schedeRuoliService.completaTutte());
            assertTrue(genitoreRepository.findByUserId(user.getId()).isPresent());
        } finally {
            insegnanteRepository.findByUserId(user.getId()).ifPresent(insegnanteRepository::delete);
            genitoreRepository.findByUserId(user.getId()).ifPresent(genitoreRepository::delete);
            allievoService.delete(allievoService.findById(allievo.getId()));
            userService.delete(userService.findAll().stream().filter(u -> u.getId().equals(user.getId())).findFirst().orElseThrow());
        }
    }

    @Test
    void avvio_creaLeSchedeMancantiPerIRuoliGiaAssegnati() {
        Allievo allievo = allievoService.creaRapido("Ugo", "Avvio", null, null);
        User user = allievo.getUser();
        try {
            // Ruolo assegnato senza scheda (come prima di questa funzionalità)
            user.getRoles().add("INSEGNANTE");
            userService.save(user, null);
            assertTrue(insegnanteRepository.findByUserId(user.getId()).isEmpty());

            assertEquals(1, schedeRuoliService.completaTutte());
            assertTrue(insegnanteRepository.findByUserId(user.getId()).isPresent());
        } finally {
            insegnanteRepository.findByUserId(user.getId()).ifPresent(insegnanteRepository::delete);
            allievoService.delete(allievoService.findById(allievo.getId()));
            userService.delete(userService.findAll().stream().filter(u -> u.getId().equals(user.getId())).findFirst().orElseThrow());
        }
    }

    @Test
    void ruoloAllievo_creaLaSchedaConLAnagraficaDelGenitore() {
        // Un genitore che diventa anche allievo (es. adulto iscritto a un corso)
        User user = userService.findByUsername("genitore9");
        Genitore genitore = genitoreRepository.findByUserId(user.getId()).orElseThrow();
        assertTrue(allievoRepository.findByUserId(user.getId()).isEmpty());
        try {
            user.getRoles().add("ALLIEVO");
            schedeRuoliService.salvaUtente(user, null);
            Allievo allievo = allievoRepository.findByUserId(user.getId()).orElseThrow();
            assertEquals(genitore.getCodiceFiscale(), allievo.getCodiceFiscale());
            assertEquals(genitore.getIndirizzo().getVia(), allievo.getIndirizzo().getVia());
            assertEquals(null, allievo.getGenitore());
        } finally {
            allievoRepository.findByUserId(user.getId()).ifPresent(allievoRepository::delete);
            User ripristino = userService.findByUsername("genitore9");
            ripristino.getRoles().remove("ALLIEVO");
            userService.save(ripristino, null);
        }
    }

    @Test
    void eliminaUtente_conLeSchedeLibereEIlinkDiRecupero() {
        // Allievo creato con l'inserimento rapido (non iscritto) che è anche genitore di un altro allievo
        Allievo allievo = allievoService.creaRapido("Ada", "Elimina", "ada.elimina@example.com", null);
        User user = allievo.getUser();
        user.getRoles().add("GENITORE");
        schedeRuoliService.salvaUtente(user, null);
        Genitore genitore = genitoreRepository.findByUserId(user.getId()).orElseThrow();
        Allievo figlio = allievoService.creaRapido("Figlio", "Elimina", null, null);
        figlio.setGenitore(genitore);
        allievoService.save(figlio);
        recuperoPasswordRepository.save(new it.attocchi.entity.RecuperoPassword(userService.findByUsername(user.getUsername()),
            "hash-di-prova", java.time.LocalDateTime.now().plusHours(1)));
        try {
            var verifica = schedeRuoliService.verificaEliminazione(user.getId(), "admin");
            assertTrue(verifica.possibile());
            assertEquals(java.util.List.of("scheda allievo", "scheda genitore (1 figlio resta senza genitore)"), verifica.schede());

            schedeRuoliService.eliminaUtente(user.getId(), "admin");
            assertEquals(null, userService.findByUsername(user.getUsername()));
            assertTrue(allievoRepository.findById(allievo.getId()).isEmpty());
            assertTrue(genitoreRepository.findById(genitore.getId()).isEmpty());
            assertEquals(null, allievoService.findById(figlio.getId()).getGenitore());
        } finally {
            Allievo f = allievoService.findById(figlio.getId());
            allievoService.delete(f);
            userService.delete(f.getUser());
        }
    }

    @Test
    void eliminaUtente_bloccatoConCollegamentiOSeSteso() {
        // Allievo iscritto a un corso, insegnante con un corso, il proprio utente
        User allievo = userService.findByUsername("allievo_canto1");
        assertTrue(schedeRuoliService.verificaEliminazione(allievo.getId(), "admin").motivoBlocco().contains("iscritto"));
        User insegnante = userService.findByUsername("insegnante1");
        assertTrue(schedeRuoliService.verificaEliminazione(insegnante.getId(), "admin").motivoBlocco().startsWith("la scheda insegnante"));
        User admin = userService.findByUsername("admin");
        assertEquals("non puoi eliminare il tuo utente", schedeRuoliService.verificaEliminazione(admin.getId(), "admin").motivoBlocco());
        assertThrows(IllegalStateException.class, () -> schedeRuoliService.eliminaUtente(allievo.getId(), "admin"));
        assertTrue(userService.findByUsername("allievo_canto1") != null);
    }
}
