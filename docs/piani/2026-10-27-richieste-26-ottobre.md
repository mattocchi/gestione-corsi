# Piano: richieste del 26 ottobre 2026

## Richiesta

Le otto voci di `docs/RICHIESTE.md`, sezione "RICHIESTE del 26 OTTOBRE 2026" (senza ambito: riferimenti `[2026-10-26/RICHIESTE.1]` … `[2026-10-26/RICHIESTE.8]`): preventivo e riepilogo delle lezioni del mese nel rimborso, tipo Servizio oltre a Corso, luoghi dei corsi e delle lezioni, attestazione spese per anno solare, invito ad accedere, sospensione degli account inattivi, scheda allievo riorganizzata, lettera di incarico per gli insegnanti.

## Analisi

- Rimborsi: `BilancioService.propostaRimborso(insegnanteId, mese)` = costo delle lezioni completate del mese meno i rimborsi del mese; minuti = presenze (assenti compresi) × `Corso.durataLezioneMinuti`, solo lezioni con l'insegnante presente. `RimborsiDialog` propone l'importo al cambio del mese; `MieiRimborsiView` è in sola lettura.
- Corsi: `Corso` non ha un tipo; compare in Gestione Corsi, Calendario, Lezioni, dashboard, Riepilogo Iscritti, Pagamenti, Documenti, I Miei Corsi/Figli. Chi lo svolge è `Corso.insegnante`/`secondoInsegnante`.
- Luoghi: nessuna entità; `Lezione` ha data, orario, note. Le viste di gestione semplici (Festività, Anni scolastici) sono il modello per una vista "Luoghi".
- Documenti: `TipoDocumento` (CONFERMA_ISCRIZIONE, RICEVUTA, RIEPILOGO) con template FreeMarker `email/` e `documenti/`; `DocumentiService` (PDF e permessi), `ElencoDocumentiService` (righe), `DocumentiView` (staff con `SegmentedControl` per tipo; genitori/allievi adulti solo PDF), `EmailService` + `InvioEmail` (metadati, `tipo` VARCHAR). I pagamenti (`Pagamento.data`) permettono il totale per anno solare.
- Accesso: `RecuperoPasswordService` (token SHA-256, `RecuperoPassword`, `puoRecuperare`: attivi o disattivati mai entrati), `NuovaPasswordView`, `User.enabled`, `User.ultimoAccesso` (nessuna data di creazione), `LoginView.italiano()` con un solo messaggio "Credenziali errate". `UserDetailsServiceImpl` cerca per username o email tra gli account attivi.
- Scheda allievo: `AllieviView` (dialog con sezioni "Utente e genitore", `UtenteFields`, `AnagraficaFields`, indirizzo del genitore mostrato se l'allievo non ne ha), `FigliView` (dialog del figlio), `ProfiloView` (allievo in sola lettura).

### Decisioni

- [2026-10-26/RICHIESTE.1] Ore di una data = allievi previsti (assenti compresi) × durata della lezione, come il costo attuale; preventivo € = ore × tariffa oraria corrente.
- [2026-10-26/RICHIESTE.1] Il preventivo comprende tutte le lezioni del mese tranne le non effettuate; si vede nel dialog del rimborso sotto il mese: elenco delle date (corso, ore della data), totale ore e preventivo €. L'importo proposto resta il maturato non rimborsato (solo lezioni completate).
- [2026-10-26/RICHIESTE.1] L'insegnante vede lo stesso riepilogo del mese in sola lettura in I Miei Rimborsi (scelta del mese tecnica: ore e preventivo €, già visibili nel suo bilancio).
- [2026-10-26/RICHIESTE.2] Campo "Tipo" del corso: Corso (default) o Servizio; il servizio ha tutte le funzioni del corso (lezioni, presenze, insegnanti, rimborsi, eventuali iscritti e pagamenti).
- [2026-10-26/RICHIESTE.2] Badge e filtro per tipo in Gestione Corsi; badge "Servizio" ovunque compare il corso (anche nelle viste dell'insegnante); per i servizi l'etichetta "Insegnante" diventa "Incaricato". Chi svolge il servizio è un Insegnante (nessun ruolo nuovo).
- [2026-10-26/RICHIESTE.2] (chiarimento in esecuzione) Per i servizi ogni lezione vale la sua durata una volta, indipendentemente dagli iscritti (mar 6-8 = 2 h); costo = ore × tariffa dell'incaricato. I corsi restano allievi × durata. Vale per costi, maturato, ore donate e preventivo del rimborso.
- [2026-10-26/RICHIESTE.3] Vista "Luoghi" per admin e segreteria; eliminazione solo admin e solo se nessun corso o lezione lo usa (altrimenti si disattiva).
- [2026-10-26/RICHIESTE.3] Il corso ha luoghi abilitati e un luogo predefinito (tra gli abilitati), facoltativi. Sulla lezione si può scegliere qualsiasi luogo attivo (vuoto = predefinito del corso); l'elenco propone prima gli abilitati del corso. Lo possono fare l'insegnante del corso, admin e segreteria.
- [2026-10-26/RICHIESTE.3] Il luogo compare ovunque c'è la lezione: Gestione Corsi, Calendario (dettaglio e settimana tipo), Lezioni / Le Mie Lezioni, dashboard "Prossima lezione", I Miei Corsi / I Miei Figli; badge quando la lezione è in un luogo diverso dal predefinito.
- [2026-10-26/RICHIESTE.4] Attestazione spese: un PDF per allievo e anno solare, con i pagamenti di tutti i corsi con data nell'anno (elenco rate e totale), dati dell'allievo e di chi paga (genitore o allievo adulto) con codice fiscale. Nuovo tipo in Documenti con PDF ed email; permessi come gli altri documenti (staff tutte, genitore dei figli, allievo senza genitore la propria); solo anni con almeno un pagamento.
- [2026-10-26/RICHIESTE.5] Invito ad accedere inviato da admin e segreteria in Gestione Utenti, per riga ("Invita") e per i selezionati, solo ad account con email mai entrati; colonna con l'ultimo invito.
- [2026-10-26/RICHIESTE.5] Link valido 7 giorni; impostata la password l'account si attiva e l'utente entra subito nella Home. Scaduto, restano "Password dimenticata?" o un nuovo invito.
- [2026-10-26/RICHIESTE.5] In fondo alle email ai genitori/allievi (conferme, ricevute, riepiloghi, attestazioni) un testo fisso con il link all'app per consultare documenti e pagamenti, e il rimando a "Password dimenticata?" per il primo accesso.
- [2026-10-26/RICHIESTE.6] Controllo ogni notte: sospesi gli account con ultimo accesso più vecchio di 6 mesi, tranne admin e chi non è mai entrato.
- [2026-10-26/RICHIESTE.6] Nuovo stato "Sospeso per inattività" (badge e filtro in Gestione Utenti), distinto dalla disattivazione manuale: il recupero password riattiva solo i sospesi; i disattivati a mano dopo l'uso restano bloccati. Nessuna email alla sospensione.
- [2026-10-26/RICHIESTE.6] Al login, solo con nome e password corretti di un account sospeso: "Account sospeso per inattività: usa «Password dimenticata?» per riattivarlo"; con password errata resta "Credenziali errate".
- [2026-10-26/RICHIESTE.7] Scheda allievo in due modalità secondo il genitore: con genitore, dati dell'allievo (nome, nascita, codice fiscale) in evidenza, riquadro in sola lettura con contatti e indirizzo del genitore, contatti e indirizzo propri in una sezione "Dati propri (facoltativi)" chiusa; adulto, contatti e indirizzo propri in evidenza. Vale per Gestione Allievi, il dialog del figlio in I Miei Figli e il profilo dell'allievo.
- [2026-10-26/RICHIESTE.8] Lettera di incarico registrata (insegnante, corso, anno scolastico) da admin e segreteria: proposti tariffa oraria / forfait, giorni e orari del corso, modificabili; poi i dati restano fissati anche se la tariffa cambia.
- [2026-10-26/RICHIESTE.8] Compenso: tariffa oraria, una tantum o entrambi, più una nota. Bilancio e rimborsi continuano a usare i dati dell'insegnante. Vale anche per i servizi.
- [2026-10-26/RICHIESTE.8] Staff: tipo "Lettere di incarico" in Documenti con PDF, Anteprima e invio per email (registrato in Email inviate). Insegnante: voce Documenti con tutte le proprie lettere, anche degli anni passati, solo PDF.
- Scelte tecniche: le voci si fanno in quest'ordine perché la lettera usa il tipo Servizio e la sospensione usa il recupero/invito; nuove colonne nullable (null = valore di default) così `ddl-auto=update` non ha bisogno di script, salvo dove indicato.

## Passi

| Passo | Cosa | File | Richiesta | Verifica | Stato |
|---|---|---|---|---|---|
| 1.1 | `Corso.tipo` (`TipoCorso` CORSO/SERVIZIO, null = CORSO) con etichette ("Corso"/"Servizio", "Insegnante"/"Incaricato"); campo nel dialog, colonna/badge e filtro in Gestione Corsi; test | `entity`, `CorsiView`, test | RICHIESTE.2 | `./mvnw test` | [x] Nessun servizio nei dati di esempio (cambierebbe i conteggi di molti test) |
| 1.2 | Badge "Servizio" ed etichetta "Incaricato" nelle altre viste (Calendario, Lezioni, dashboard, Riepilogo Iscritti, I Miei Corsi/Figli, I miei allievi); CLAUDE.md | viste, `CLAUDE.md` | RICHIESTE.2 | `./mvnw test`; browser | [x] Riepilogo Iscritti: "(servizio)" nel nome (colonna gerarchica di testo); browser rimandato alla verifica finale |
| 1.3 | `BilancioService`: minuti di una lezione di un servizio = durata (una volta), per costi, maturato, proposta di rimborso e ore donate; test | `BilancioService`, test | RICHIESTE.2 | `./mvnw test` | [x] |
| 2.1 | Entità `Luogo` + `LuogoService` + vista "Luoghi" (admin/segreteria, elimina admin solo se non usato); test | `entity`, `service`, `admin/LuoghiView` | RICHIESTE.3 | `./mvnw test` | [x] `usati()` vuoto finché corsi e lezioni non usano i luoghi (2.2, 2.3) |
| 2.2 | Luoghi abilitati e predefinito del corso (dialog del corso, colonna in Gestione Corsi) | `Corso`, `CorsoService`, `CorsiView` | RICHIESTE.3 | `./mvnw test` | [x] Dati di esempio: Aula 1, Aula 2, Palestra (Canto in Aula 1 con Palestra abilitata, Chitarra in Aula 2) |
| 2.3 | `Lezione.luogo` impostato in Gestisci Presenze (`LezioneService.impostaLuogo`, permessi come le note); luogo e badge "Luogo diverso" in Lezioni/Le Mie Lezioni, Calendario, settimana tipo, dashboard, I Miei Corsi/Figli; CLAUDE.md | `Lezione`, `LezioneService`, viste | RICHIESTE.3 | `./mvnw test`; browser | [x] Calendario mese: luogo nel tooltip della lezione (la cella non ha spazio) |
| 3 | Riepilogo delle lezioni del mese (`BilancioService.lezioniDelMese`: data, corso, ore, totale ore, preventivo €) nel dialog del rimborso e in I Miei Rimborsi; test; CLAUDE.md | `BilancioService`, `RimborsiDialog`, `MieiRimborsiView` | RICHIESTE.1 | `./mvnw test`; browser | [x] |
| 4 | Scheda allievo in due modalità (con genitore / adulto) in Gestione Allievi, I Miei Figli e Profilo; test; CLAUDE.md | `AllieviView`, `FigliView`, `ProfiloView`, campi condivisi | RICHIESTE.7 | `./mvnw test`; browser 390px | [ ] |
| 5.1 | Attestazione spese: dati per allievo e anno solare (`PagamentoService`), template PDF ed email, `TipoDocumento.ATTESTAZIONE`, permessi in `DocumentiService`; test | service, template | RICHIESTE.4 | `./mvnw test` | [ ] |
| 5.2 | Attestazioni in Documenti (staff con email, famiglie solo PDF); CLAUDE.md | `ElencoDocumentiService`, `DocumentiView`, `InvioEmailDialog` | RICHIESTE.4 | `./mvnw test`; browser | [ ] |
| 6.1 | Entità `LetteraIncarico` (insegnante, corso, anno, tariffa oraria, una tantum, giorni e orari, nota, data) + service con proposta dai dati attuali; template PDF ed email; `TipoDocumento.LETTERA_INCARICO`; test | entity, service, template | RICHIESTE.8 | `./mvnw test` | [ ] |
| 6.2 | Documenti: tipo "Lettere di incarico" per lo staff (crea/modifica, PDF, invio); voce Documenti per l'insegnante con le proprie lettere; CLAUDE.md | `DocumentiView`, `MainLayout`, dialog | RICHIESTE.8 | `./mvnw test`; browser | [ ] |
| 7.1 | Invito: token d'invito (7 giorni) sul meccanismo del recupero, email d'invito, "Invita"/"Invita selezionati" e colonna ultimo invito in Gestione Utenti; impostata la password accesso automatico; test | `RecuperoPasswordService`, `UtentiView`, `NuovaPasswordView`, template | RICHIESTE.5 | `./mvnw test`; browser | [ ] |
| 7.2 | Footer con il link all'app nelle email di documenti; CLAUDE.md | template `email/` | RICHIESTE.5 | `./mvnw test` | [ ] |
| 8.1 | `User.sospesoPerInattivita`; job notturno (`@Scheduled`) che sospende dopo 6 mesi (non admin, non mai entrati); recupero/invito riattiva i sospesi; test | `User`, `UserService`, `RecuperoPasswordService` | RICHIESTE.6 | `./mvnw test` | [ ] |
| 8.2 | Messaggio di account sospeso al login (solo con password corretta); badge e filtro in Gestione Utenti; test; CLAUDE.md | `SecurityConfig`/`LoginView`, `UtentiView` | RICHIESTE.6 | `./mvnw test`; browser | [ ] |
| 9 | Registro finale | questo piano | — | build e test | [ ] |

## Fuori ambito

- La lettera di incarico non diventa la fonte del compenso per bilancio e rimborsi (restano tariffa e forfait dell'insegnante).
- Nessuna email di avviso alla sospensione né preavviso.
- Nessun nuovo ruolo per chi svolge i servizi.

## Registro

