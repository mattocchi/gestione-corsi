# Piano: richieste del 10-10-2026 Revisione 1

## Richiesta

Implementare le 7 voci della sezione "RICHIESTE del 10-10-2026 Revisione 1" di `docs/RICHIESTE.md`: pagamenti a rate e quota personalizzata per partecipante, tariffa oraria e profilo dell'insegnante, anagrafiche complete di insegnanti/allievi/genitori, riepilogo pagamenti in dashboard, anno scolastico e stato attivo dei corsi.

## Analisi

- **Riferimenti**: la sezione non ha sottotitoli di ambito; le voci sono identificate come `[2026-10-10/REV1.n]` (n = posizione nella sezione, 1–7).
- **Stato attuale del codice**:
  - `Iscrizione` (tabella `corso_studenti`) ha solo `orarioLezione`; nessun dato economico per partecipante. `Corso.costoTotale` è la quota piena.
  - `Corso.attivo` esiste già nel modello (default `true`) ma non è modificabile né usato; non c'è anno scolastico.
  - `Insegnante` ha solo specializzazione e biografia. `Studente` ha codice fiscale, data/luogo di nascita, indirizzo, città, CAP; `Genitore` codice fiscale, indirizzo, città, CAP. Nome, cognome, email e telefono sono su `User`.
  - Le viste Insegnanti e Genitori sono in sola lettura; un insegnante o genitore creato da Gestione Utenti non ha la scheda anagrafica.
  - Il genitore scarica un riepilogo `.txt` (FigliView); non esiste una ricevuta.
- **Indirizzo**: nuovo `@Embeddable Indirizzo` (via e civico, CAP, comune, provincia) riusato da Studente, Genitore e Insegnante; per Studente e Genitore mappa le colonne esistenti `indirizzo`, `cap`, `citta` (dati di produzione invariati) e aggiunge `provincia`.
- **Pagamenti**: nuova entity `Pagamento` (data, importo, metodo `BONIFICO`/`CASSA`, nota) legata all'`Iscrizione` (chiave corso/studente). Quota dovuta = quota personalizzata se impostata, altrimenti `costoTotale` del corso.
- **Ricevuta**: route a sé (senza `MainLayout`) con stile di stampa e pulsante "Stampa" (`window.print()`); nessuna nuova dipendenza.
- **Produzione** (`ddl-auto=update`): nuove colonne e la tabella `pagamenti` vengono aggiunte da Hibernate; l'anno scolastico dei corsi esistenti va calcolato con lo script `docs/migrazioni/2026-10-10-anno-scolastico.sql`.
- API Vaadin verificate con il MCP: `Card`, `Badge`, `BigDecimalField`, `TimePicker`, testers browserless (`TextFieldTester`, `NumberFieldTester`, `DatePickerTester`, `TimePickerTester`, `ComboBoxTester`).

### Decisioni (giro di domande del 10-10-2026)

| Voce | Decisione |
|---|---|
| REV1.1/REV1.2 | Quota e rate in un dialog "Quota e pagamenti" per partecipante, aperto dal dialog Partecipanti del corso (admin e segreteria). |
| REV1.1 | Rate: aggiungi, modifica, elimina; data (default oggi), importo, metodo Bonifico/Cassa con pulsanti a un tocco (default Bonifico), nota facoltativa. |
| REV1.1 | Il genitore vede per ogni figlio e corso quota, pagato, residuo e rate (anche nella ricevuta); allievo e insegnante no. |
| REV1.1 | Partecipante, corso o allievo con pagamenti registrati: rimozione/eliminazione bloccata con avviso. |
| REV1.2 | Quota personalizzata facoltativa con nota del motivo; vuota = quota piena del corso (segue le modifiche del costo). |
| REV1.3 | Tariffa oraria (€/ora) solo come dato: impostata da admin/segreteria, visibile all'insegnante nel profilo; nessun calcolo. |
| REV1.4/REV1.5 | Indirizzo: via e civico, CAP (5 cifre), Comune, Provincia (sigla 2 lettere). Codice fiscale: formato e carattere di controllo (obbligatorio per gli allievi, facoltativo per insegnanti e genitori). |
| REV1.4 | "Il mio profilo": anagrafica, indirizzo, nome, cognome, email, telefono, specializzazione e biografia (insegnante), cambio password con conferma; username e tariffa in sola lettura. |
| REV1.4/REV1.5 | Admin e segreteria creano e modificano insegnanti e genitori (utente + anagrafica) dalle viste Insegnanti e Genitori. |
| REV1.5 | Genitore: "Il mio profilo" e modifica dei dati dei figli; allievo: proprio profilo in sola lettura. |
| REV1.5 | Indirizzo del genitore mostrato con "(del genitore)" in Gestione Studenti, nel profilo dell'allievo e nella ricevuta, se l'allievo non ha nessun campo dell'indirizzo; il dato non viene copiato. |
| REV1.6 | Admin e segreteria, solo corsi attivi: partecipanti saldati / con pagamento parziale / senza pagamenti (quota 0 = saldato), incassato e da incassare in € e %; riquadri con i totali e griglia con una riga per corso attivo. |
| REV1.7 | Anno scolastico "2026/2027" scelto da elenco (anno precedente, corrente, successivo), obbligatorio, proposto dalla data di inizio (settembre–agosto); corsi esistenti: anno calcolato dallo script SQL, restano attivi. |
| REV1.7 | Corso non attivo: bloccati aggiunta/rimozione partecipanti, orari, Genera Lezioni, presenze e completamento lezioni; quote e pagamenti restano modificabili; il corso si può riattivare. |
| REV1.7 | Insegnante e allievo non vedono i corsi non attivi (né lezioni né conteggi); admin, segreteria e genitori sì. Gestione Corsi: filtro Attivi/Non attivi/Tutti (default Attivi) e per anno scolastico. |
| REV1.7 | Ricevuta: pagina stampabile (logo, allievo, genitore, corso, anno, quota, rate, residuo) al posto del riepilogo `.txt`. |

## Passi

| Passo | Cosa | Richiesta | File | Verifica | Stato |
|---|---|---|---|---|---|
| 1 | Anno scolastico e stato attivo nel dialog del corso (elenco anni proposto dalla data di inizio, checkbox Attivo), colonne Anno e Stato (badge) in Gestione Corsi; dati di esempio; script SQL per i corsi esistenti | (REV1.7) | `Corso`, `AnnoScolastico`, `CorsiView`, `DataInitializer`, `docs/migrazioni/…sql` | `./mvnw test` | [x] |
| 2 | Gestione Corsi: filtri per stato (Attivi/Non attivi/Tutti, default Attivi) e per anno scolastico | (REV1.7) | `CorsiView` | `./mvnw test` | [ ] |
| 3 | Corsi non attivi: blocchi nei service (iscrizioni, orari, Genera Lezioni, presenze, completamento) e UI in sola lettura nel dialog Partecipanti; insegnante e allievo non vedono i corsi non attivi (lezioni, corsi, dashboard) | (REV1.7) | `CorsoService`, `PresenzaService`, `LezioneService`, repository, `PartecipantiDialog`, `CorsiView`, `LezioniInsegnanteView`, `MieiCorsiView`, `DashboardService` | `./mvnw test` | [ ] |
| 4 | Modello pagamenti: quota personalizzata e nota su `Iscrizione`, entity `Pagamento` con `MetodoPagamento`, `PagamentoService` con riepilogo quota/pagato/residuo | — | `entity/`, `repository/`, `service/` | `./mvnw test` | [ ] |
| 5 | Dialog "Quota e pagamenti" dal dialog Partecipanti: quota personalizzata con nota | REV1.2 | `QuotaPagamentiDialog`, `PartecipantiDialog` | `./mvnw test` | [ ] |
| 6 | Rate nel dialog: aggiunta, modifica, eliminazione (data, importo, metodo a un tocco, nota); pagato/residuo anche nel dialog Partecipanti | (REV1.1) | `QuotaPagamentiDialog`, `PartecipantiDialog`, `PagamentoService` | `./mvnw test` | [ ] |
| 7 | Blocco di rimozione partecipante / eliminazione corso o allievo con pagamenti registrati | (REV1.1) | `CorsoService`, `StudenteService`, view | `./mvnw test` | [ ] |
| 8 | Genitore: quota, pagato, residuo e rate per figlio e corso nel dettaglio di I Miei Figli | REV1.1 | `FigliView` | `./mvnw test` | [ ] |
| 9 | Anagrafiche: `Indirizzo` embeddable (colonne esistenti), nuovi campi di Insegnante (anagrafica, indirizzo, tariffa oraria) e Genitore (nascita, provincia), validatore del codice fiscale con test | — | `entity/`, `views/CodiceFiscale` (o `service/`), test | `./mvnw test` | [ ] |
| 10 | Vista Insegnanti modificabile: creazione e modifica di insegnante (utente + anagrafica + tariffa) per admin e segreteria | (REV1.3, REV1.4) | `InsegnantiView`, `InsegnanteService`, form anagrafica condiviso | `./mvnw test` | [ ] |
| 11 | "Il mio profilo" per l'insegnante: dati modificabili, cambio password, username e tariffa in sola lettura | REV1.3, REV1.4 | `views/ProfiloView`, `UserService` | `./mvnw test` | [ ] |
| 12 | Vista Genitori modificabile: creazione e modifica di genitore (utente + anagrafica) per admin e segreteria | (REV1.5) | `GenitoriView`, `GenitoreService` | `./mvnw test` | [ ] |
| 13 | Gestione Studenti: anagrafica completa (provincia, controllo codice fiscale) e indirizzo del genitore "(del genitore)" se manca quello dell'allievo | (REV1.5) | `StudentiView`, `Studente` | `./mvnw test` | [ ] |
| 14 | Profilo del genitore (propri dati + modifica dei figli) e dell'allievo (sola lettura, con indirizzo del genitore) | REV1.5 | `ProfiloView`, `FigliView` | `./mvnw test` | [ ] |
| 15 | Dashboard admin/segreteria: riepilogo pagamenti dei corsi attivi (riquadri + griglia per corso) | REV1.6 | `DashboardService`, `HomeView`, `styles.css` | `./mvnw test` + browser | [ ] |
| 16 | Ricevuta stampabile (genitore per i propri figli, admin e segreteria) al posto del riepilogo `.txt`, disponibile anche per i corsi non attivi | REV1.7 | `views/RicevutaView`, `FigliView`, `QuotaPagamentiDialog`, `styles.css` | `./mvnw test` + browser | [ ] |
| 17 | Verifica finale (build, browser 390px/1280px), `CLAUDE.md`, registro | — | piano, `CLAUDE.md` | build + browser | [ ] |

## Fuori ambito

- **CORSI.2** (voce incompleta del 10-10-2026): resta aperta.
- Calcolo del compenso dell'insegnante (ore × tariffa): la tariffa è solo un dato (decisione REV1.3).
- Ricevuta in PDF generato dal server: si usa la stampa/salvataggio PDF del browser.
- Profili self-service per l'admin e la segreteria.

## Registro

- (da compilare durante l'esecuzione)
