package it.attocchi;

import it.attocchi.entity.*;
import it.attocchi.repository.AllievoRepository;
import it.attocchi.repository.GenitoreRepository;
import it.attocchi.repository.InsegnanteRepository;
import it.attocchi.service.CorsoService;
import it.attocchi.service.CronologiaDocumentiService;
import it.attocchi.service.EmailService;
import it.attocchi.service.LetteraIncaricoService;
import it.attocchi.service.LezioneService;
import it.attocchi.service.PagamentoService;
import it.attocchi.service.PresenzaService;
import it.attocchi.service.RimborsoService;
import it.attocchi.service.UserService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Dati demo dell'anno scolastico in corso (2026/2027) e del precedente, creati da {@link DataInitializer} attraverso
 * i servizi: famiglie con più figli e allievi adulti, corsi con gruppi, lezioni passate svolte (con assenze, una non
 * effettuata e il recupero), pagamenti in tutti gli stati, quote personalizzate, il kit come opzione aggiuntiva di
 * Propedeutica, un servizio, un corso non pubblicato,
 * un corso archiviato con i bonifici per le attestazioni, rimborsi, una lettera di incarico, invii e documenti aperti.
 * Persone e corsi sono nuovi: i dati di prova su cui si basano i test restano come sono.
 */
@Component
@Profile("dev")
class DatiDemo {

    /** Chi segna le lezioni e crea lettere e invii. */
    private static final String ADMIN = "admin";
    private static final String SEGRETERIA = "segreteria";

    private final UserService userService;
    private final InsegnanteRepository insegnanteRepository;
    private final GenitoreRepository genitoreRepository;
    private final AllievoRepository allievoRepository;
    private final CorsoService corsoService;
    private final LezioneService lezioneService;
    private final PresenzaService presenzaService;
    private final PagamentoService pagamentoService;
    private final RimborsoService rimborsoService;
    private final LetteraIncaricoService letteraIncaricoService;
    private final EmailService emailService;
    private final CronologiaDocumentiService cronologiaDocumentiService;

    DatiDemo(UserService userService, InsegnanteRepository insegnanteRepository, GenitoreRepository genitoreRepository,
             AllievoRepository allievoRepository, CorsoService corsoService, LezioneService lezioneService,
             PresenzaService presenzaService, PagamentoService pagamentoService, RimborsoService rimborsoService,
             LetteraIncaricoService letteraIncaricoService, EmailService emailService,
             CronologiaDocumentiService cronologiaDocumentiService) {
        this.userService = userService;
        this.insegnanteRepository = insegnanteRepository;
        this.genitoreRepository = genitoreRepository;
        this.allievoRepository = allievoRepository;
        this.corsoService = corsoService;
        this.lezioneService = lezioneService;
        this.presenzaService = presenzaService;
        this.pagamentoService = pagamentoService;
        this.rimborsoService = rimborsoService;
        this.letteraIncaricoService = letteraIncaricoService;
        this.emailService = emailService;
        this.cronologiaDocumentiService = cronologiaDocumentiService;
    }

    /** Crea i dati demo; {@code oggi} decide quali lezioni sono già state svolte. */
    void crea(Luogo aula1, Luogo aula2, Luogo palestra, LocalDate oggi) {
        // Insegnanti: con tariffa, senza tariffa (ore donate), con forfettario, incaricato di un servizio
        Insegnante giulia = insegnante("Giulia", "Conti", "Pianoforte", "28.00", null);
        Insegnante elena = insegnante("Elena", "Marchi", "Propedeutica musicale", null, null);
        Insegnante andrea = insegnante("Andrea", "Galli", "Batteria", "30.00", "200.00");
        Insegnante paolo = insegnante("Paolo", "Neri", null, null, null);

        // Famiglie
        Genitore laura = genitore("Laura", "Ferri", "FRRRRT78L02H501", new Indirizzo("Via dei Mille 12", "00185", "Roma", "RM"));
        // L'attestazione la intesta al marito, che fa la detrazione (il codice fiscale è il suo)
        laura.setIntestazioneNome("Roberto");
        laura.setIntestazioneCognome("Ferri");
        laura = genitoreRepository.save(laura);
        Genitore davide = genitore("Davide", "Russo", "RSSDVD76B11H501", new Indirizzo("Via Appia Nuova 210", "00183", "Roma", "RM"));
        Genitore chiara = genitore("Chiara", "Colombo", "CLMCHR84R45H501", new Indirizzo("Via Tuscolana 88", "00181", "Roma", "RM"));
        Genitore simone = genitore("Simone", "Greco", "GRCSMN81T19H501", new Indirizzo("Viale Marconi 5", "00146", "Roma", "RM"));
        Genitore anna = genitore("Anna", "Fontana", "FNTNNA79E63H501", new Indirizzo("Via Cassia 1020", "00189", "Roma", "RM"));

        Allievo luca = allievo("Luca", "Ferri", LocalDate.of(2014, 4, 12), "FRRLCU14D12H501", laura);
        Allievo sara = allievo("Sara", "Ferri", LocalDate.of(2019, 9, 3), "FRRSRA19P43H501", laura);
        Allievo marta = allievo("Marta", "Russo", LocalDate.of(2012, 1, 25), "RSSMRT12A65H501", davide);
        Allievo tommaso = allievo("Tommaso", "Russo", LocalDate.of(2018, 6, 30), "RSSTMS18H30H501", davide);
        // Senza codice fiscale: "Dati mancanti" per le ricevute
        Allievo matteo = allievo("Matteo", "Colombo", LocalDate.of(2017, 11, 8), null, chiara);
        Allievo alice = allievo("Alice", "Greco", LocalDate.of(2013, 2, 17), "GRCLCA13B57H501", simone);
        Allievo nicolo = allievo("Nicolò", "Fontana", LocalDate.of(2011, 7, 21), "FNTNCL11L21H501", anna);
        // Allievi adulti, senza genitore
        Allievo francesca = allievo("Francesca", "Moretti", LocalDate.of(1985, 5, 10), "MRTFNC85E50H501", null);
        francesca.setIndirizzo(new Indirizzo("Via Nomentana 300", "00162", "Roma", "RM"));
        francesca = allievoRepository.save(francesca);
        Allievo stefano = allievo("Stefano", "Rizzi", LocalDate.of(1979, 12, 2), "RZZSFN79T02H501", null);
        stefano.setIndirizzo(new Indirizzo("Via Prenestina 45", "00176", "Roma", "RM"));
        stefano = allievoRepository.save(stefano);

        // Pianoforte: due giorni, un gruppo per giorno e un'allieva senza gruppo (tutti e due i giorni)
        Corso pianoforte = corso("Pianoforte", "Pianoforte per ragazzi, dai principianti agli avanzati",
            LocalDate.of(2026, 9, 14), Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), LocalTime.of(17, 0), 45, 28, null,
            "650.00", giulia, aula1);
        pianoforte.setPrezzoPreiscrizione(new BigDecimal("552.50"));
        pianoforte.setLuoghiAbilitati(new HashSet<>(Set.of(aula1, palestra)));
        pianoforte = corsoService.save(pianoforte);
        iscrivi(pianoforte, luca, nicolo, marta, alice);
        GruppoCorso principianti = gruppo(pianoforte, "Principianti", DayOfWeek.MONDAY, LocalTime.of(16, 30));
        GruppoCorso avanzati = gruppo(pianoforte, "Avanzati", DayOfWeek.WEDNESDAY, LocalTime.of(18, 0));
        assegna(pianoforte, principianti, luca, nicolo);
        assegna(pianoforte, avanzati, marta);
        corsoService.generaLezioni(pianoforte);

        // Propedeutica: due insegnanti (una senza tariffa), due gruppi nello stesso giorno a orari diversi
        Corso propedeutica = corso("Propedeutica musicale", "Primo approccio alla musica con ritmo, voce e strumentario",
            LocalDate.of(2026, 9, 19), Set.of(DayOfWeek.SATURDAY), LocalTime.of(10, 0), 50, 25, null,
            "380.00", elena, palestra);
        propedeutica.setInsegnantiFacoltativi(new ArrayList<>(List.of(giulia)));
        // Opzione aggiuntiva: il kit, solo per chi lo prende
        propedeutica = corsoService.salva(propedeutica,
            List.of(new OpzioneCorso(null, "Kit strumentario", new BigDecimal("30.00"))));
        iscrivi(propedeutica, sara, matteo, tommaso);
        GruppoCorso piccoli = gruppo(propedeutica, "Piccoli", DayOfWeek.SATURDAY, LocalTime.of(10, 0));
        GruppoCorso grandi = gruppo(propedeutica, "Grandi", DayOfWeek.SATURDAY, LocalTime.of(11, 0));
        assegna(propedeutica, piccoli, sara, matteo);
        assegna(propedeutica, grandi, tommaso);
        Long kit = corsoService.opzioni(propedeutica.getId()).getFirst().getId();
        corsoService.impostaOpzione(propedeutica.getId(), sara.getId(), kit, true);
        corsoService.impostaOpzione(propedeutica.getId(), tommaso.getId(), kit, true);
        corsoService.generaLezioni(propedeutica);

        // Batteria: ogni 2 settimane, fino a una data invece di un numero di lezioni
        Corso batteria = corso("Batteria", "Batteria per adulti", LocalDate.of(2026, 9, 18), Set.of(DayOfWeek.FRIDAY),
            LocalTime.of(18, 30), 60, null, LocalDate.of(2027, 5, 28), "350.00", andrea, aula2);
        batteria.setCadenzaSettimane(2);
        batteria = corsoService.save(batteria);
        iscrivi(batteria, francesca, stefano);
        corsoService.generaLezioni(batteria);

        // Servizio: chi lo svolge è un "incaricato"
        Corso pulizia = corso("Pulizia sede", "Turni di pulizia della sede con i soci volontari",
            LocalDate.of(2026, 9, 17), Set.of(DayOfWeek.THURSDAY), LocalTime.of(20, 0), 90, 20, null, "30.00", paolo, aula1);
        pulizia.setTipo(TipoCorso.SERVIZIO);
        pulizia = corsoService.save(pulizia);
        iscrivi(pulizia, francesca, stefano);
        corsoService.generaLezioni(pulizia);

        // Ukulele: in preparazione, non pubblicato (le famiglie non lo vedono)
        Corso ukulele = corso("Ukulele", "Ukulele per ragazzi, da gennaio", LocalDate.of(2027, 1, 12),
            Set.of(DayOfWeek.TUESDAY), LocalTime.of(17, 30), 40, 20, null, "300.00", giulia, aula2);
        ukulele.setPubblicato(false);
        ukulele = corsoService.save(ukulele);
        iscrivi(ukulele, marta, alice);
        corsoService.generaLezioni(ukulele);

        // Lezioni: prima nota, una lezione non effettuata con il recupero, poi svolte tutte quelle passate
        Lezione primaPianoforte = lezioneService.findByCorso(pianoforte.getId()).getFirst();
        lezioneService.impostaNote(primaPianoforte.getId(), "Verificare i libri di testo di Nicolò",
            "Portare il quaderno pentagrammato", ADMIN);
        Long propedeuticaId = propedeutica.getId();
        lezioneService.findByCorso(propedeuticaId).stream()
            .filter(l -> l.getData().equals(LocalDate.of(2026, 10, 10)))
            .findFirst()
            .ifPresent(saltata -> {
                lezioneService.segnaNonEffettuata(saltata.getId(), "Insegnante malata", ADMIN);
                lezioneService.aggiungi(propedeuticaId, LocalDate.of(2026, 11, 13), LocalTime.of(17, 0),
                    saltata.getId(), ADMIN);
            });
        for (Corso corso : List.of(pianoforte, propedeutica, batteria, pulizia)) {
            svolgiFinoA(corso, oggi);
        }
        assenza(pianoforte, nicolo, 1, Presenza.StatoPresenza.ASSENTE_GIUSTIFICATA);
        assenza(pianoforte, luca, 2, Presenza.StatoPresenza.ASSENTE_INGIUSTIFICATA);
        assenza(propedeutica, matteo, 0, Presenza.StatoPresenza.ASSENTE_GIUSTIFICATA);
        assenza(batteria, stefano, 0, Presenza.StatoPresenza.ASSENTE_GIUSTIFICATA);

        // Pagamenti dell'anno: saldati, parziali, nessuno, quote personalizzate, bonifico e cassa
        paga(pianoforte, marta, "2026-09-10", "650.00", MetodoPagamento.BONIFICO);
        paga(pianoforte, luca, "2026-09-20", "300.00", MetodoPagamento.BONIFICO);
        pagamentoService.impostaTipoQuota(pianoforte.getId(), alice.getId(), TipoQuota.PREISCRIZIONE);
        paga(pianoforte, alice, "2026-06-15", "276.25", MetodoPagamento.BONIFICO);
        paga(pianoforte, alice, "2026-10-15", "276.25", MetodoPagamento.BONIFICO);
        paga(propedeutica, sara, "2026-09-19", "380.00", MetodoPagamento.CASSA);
        pagamentoService.impostaQuota(propedeutica.getId(), tommaso.getId(), new BigDecimal("340.00"), "Secondo figlio");
        paga(propedeutica, tommaso, "2026-10-03", "170.00", MetodoPagamento.CASSA);
        paga(batteria, francesca, "2026-10-02", "175.00", MetodoPagamento.BONIFICO);
        paga(pulizia, stefano, "2026-09-17", "30.00", MetodoPagamento.CASSA);

        // Anno scorso: corso archiviato con i bonifici del 2025 e del 2026 (attestazioni dei due anni)
        Corso pianoforteAnnoScorso = corso("Pianoforte", "Pianoforte per ragazzi", LocalDate.of(2025, 9, 15),
            Set.of(DayOfWeek.MONDAY), LocalTime.of(17, 0), 45, 25, null, "600.00", giulia, aula1);
        pianoforteAnnoScorso = corsoService.save(pianoforteAnnoScorso);
        iscrivi(pianoforteAnnoScorso, luca, marta, alice);
        corsoService.generaLezioni(pianoforteAnnoScorso);
        svolgiFinoA(pianoforteAnnoScorso, oggi);
        paga(pianoforteAnnoScorso, luca, "2025-10-10", "300.00", MetodoPagamento.BONIFICO);
        paga(pianoforteAnnoScorso, luca, "2026-01-20", "300.00", MetodoPagamento.BONIFICO);
        paga(pianoforteAnnoScorso, marta, "2025-09-30", "600.00", MetodoPagamento.BONIFICO);
        // La cassa non vale per la detrazione
        paga(pianoforteAnnoScorso, alice, "2025-10-01", "200.00", MetodoPagamento.CASSA);
        paga(pianoforteAnnoScorso, alice, "2025-11-28", "400.00", MetodoPagamento.BONIFICO);
        Corso archiviato = corsoService.findById(pianoforteAnnoScorso.getId());
        archiviato.setAttivo(false);
        corsoService.save(archiviato);

        // Rimborso di settembre a Giulia e sua lettera di incarico
        Rimborso rimborso = new Rimborso();
        rimborso.setData(LocalDate.of(2026, 10, 5));
        rimborso.setMese(YearMonth.of(2026, 9));
        rimborso.setImporto(new BigDecimal("126.00"));
        rimborso.setMetodo(MetodoPagamento.BONIFICO);
        rimborso.setNota("Lezioni di settembre");
        rimborsoService.salva(giulia.getId(), rimborso);
        LetteraIncarico lettera = letteraIncaricoService.proposta(giulia.getId(), pianoforte.getId());
        lettera.setDataEmissione(LocalDate.of(2026, 9, 1));
        letteraIncaricoService.salva(lettera, ADMIN);

        // Storico: invii (simulati senza SMTP) e documenti aperti dalle famiglie
        emailService.invia(TipoDocumento.CONFERMA_ISCRIZIONE, pianoforte.getId(), luca.getId(), SEGRETERIA);
        emailService.invia(TipoDocumento.RICEVUTA, pianoforte.getId(), marta.getId(), SEGRETERIA);
        emailService.inviaConferme(propedeutica.getId(), SEGRETERIA);
        cronologiaDocumentiService.registra("laura.ferri", TipoDocumento.CONFERMA_ISCRIZIONE,
            AperturaDocumento.Modalita.ANTEPRIMA, luca.getNomeCompleto(), pianoforte.getNome(), null,
            "conferma-iscrizione-luca-ferri.pdf");
        cronologiaDocumentiService.registra("francesca.moretti", TipoDocumento.RICEVUTA,
            AperturaDocumento.Modalita.EMAIL, francesca.getNomeCompleto(), batteria.getNome(), null,
            "ricevuta-francesca-moretti.pdf");
    }

    private User utente(String nome, String cognome, String ruolo) {
        String username = username(nome, cognome);
        User user = new User();
        user.setUsername(username);
        user.setNome(nome);
        user.setCognome(cognome);
        user.setEmail(username + "@example.com");
        user.setRoles(new HashSet<>(Set.of(ruolo)));
        return userService.save(user, username);
    }

    /** "Nicolò Fontana" → "nicolo.fontana". */
    private static String username(String nome, String cognome) {
        String testo = (nome + "." + cognome).toLowerCase(Locale.ITALIAN);
        return Normalizer.normalize(testo, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    private Insegnante insegnante(String nome, String cognome, String specializzazione, String tariffa, String forfait) {
        Insegnante insegnante = new Insegnante();
        insegnante.setUser(utente(nome, cognome, "INSEGNANTE"));
        insegnante.setSpecializzazione(specializzazione);
        insegnante.setTariffaOraria(tariffa != null ? new BigDecimal(tariffa) : null);
        insegnante.setRimborsoForfettario(forfait != null ? new BigDecimal(forfait) : null);
        return insegnanteRepository.save(insegnante);
    }

    private Genitore genitore(String nome, String cognome, String primi15, Indirizzo indirizzo) {
        Genitore genitore = new Genitore();
        genitore.setUser(utente(nome, cognome, "GENITORE"));
        genitore.setCodiceFiscale(codiceFiscale(primi15));
        genitore.setIndirizzo(indirizzo);
        return genitoreRepository.save(genitore);
    }

    private Allievo allievo(String nome, String cognome, LocalDate nascita, String primi15, Genitore genitore) {
        Allievo allievo = new Allievo();
        allievo.setUser(utente(nome, cognome, "ALLIEVO"));
        allievo.setDataNascita(nascita);
        allievo.setLuogoNascita("Roma");
        allievo.setCodiceFiscale(primi15 != null ? codiceFiscale(primi15) : null);
        allievo.setGenitore(genitore);
        return allievoRepository.save(allievo);
    }

    /** Corso attivo e pubblicato; {@code numeroLezioni} o {@code dataFine} (almeno uno). */
    private static Corso corso(String nome, String descrizione, LocalDate inizio, Set<DayOfWeek> giorni, LocalTime orario,
                               int durata, Integer numeroLezioni, LocalDate dataFine, String costo, Insegnante insegnante,
                               Luogo luogo) {
        Corso corso = new Corso();
        corso.setNome(nome);
        corso.setDescrizione(descrizione);
        corso.setDataInizio(inizio);
        corso.setDataFine(dataFine);
        corso.setAnnoScolastico(AnnoScolastico.di(inizio));
        corso.setNumeroLezioni(numeroLezioni);
        corso.setDurataLezioneMinuti(durata);
        corso.setOrarioInizio(orario);
        corso.setGiorniSettimana(new HashSet<>(giorni));
        corso.setPubblicato(true);
        corso.setInsegnante(insegnante);
        corso.setLuogo(luogo);
        corso.setLuoghiAbilitati(new HashSet<>(Set.of(luogo)));
        corso.setCostoTotale(new BigDecimal(costo));
        return corso;
    }

    private void iscrivi(Corso corso, Allievo... allievi) {
        for (Allievo allievo : allievi) {
            corsoService.iscrivi(corso.getId(), allievo.getId());
        }
    }

    private GruppoCorso gruppo(Corso corso, String nome, DayOfWeek giorno, LocalTime orario) {
        return corsoService.salvaGruppo(corso.getId(), new GruppoCorso(null, nome, giorno, orario));
    }

    private void assegna(Corso corso, GruppoCorso gruppo, Allievo... allievi) {
        for (Allievo allievo : allievi) {
            corsoService.assegnaGruppo(corso.getId(), allievo.getId(), gruppo.getId());
        }
    }

    /** Segna svolte le lezioni da fare prima di {@code oggi}. */
    private void svolgiFinoA(Corso corso, LocalDate oggi) {
        lezioneService.findByCorso(corso.getId()).stream()
            .filter(Lezione::isDaFare)
            .filter(l -> l.getData().isBefore(oggi))
            .forEach(l -> lezioneService.completa(l.getId(), ADMIN));
    }

    /** L'allievo era assente alla sua {@code n}-esima lezione svolta (da 0), se c'è. */
    private void assenza(Corso corso, Allievo allievo, int n, Presenza.StatoPresenza stato) {
        List<Presenza> svolte = presenzaService.findByCorsoAndAllievo(corso.getId(), allievo.getId()).stream()
            .filter(p -> p.getLezione().isCompletata())
            .toList();
        if (n < svolte.size()) {
            Presenza presenza = svolte.get(n);
            presenza.setStato(stato);
            presenzaService.save(presenza);
        }
    }

    private void paga(Corso corso, Allievo allievo, String data, String importo, MetodoPagamento metodo) {
        Pagamento pagamento = new Pagamento();
        pagamento.setData(LocalDate.parse(data));
        pagamento.setImporto(new BigDecimal(importo));
        pagamento.setMetodo(metodo);
        pagamentoService.salva(corso.getId(), allievo.getId(), pagamento);
    }

    /** Codice fiscale di esempio valido: i primi 15 caratteri più il carattere di controllo. */
    private static String codiceFiscale(String primi15) {
        return primi15 + CodiceFiscale.carattereDiControllo(primi15);
    }
}
