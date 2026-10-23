# Piano: dashboard con la prossima lezione e le ore donate

## Richiesta

- [F-2026-10-24/DASHBOARD.1] In dashboard la prossima lezione prevista, prima dei riquadri di riepilogo; cliccandola si va alla lezione (per l'insegnante il registro delle presenze).
- [F-2026-10-24/DASHBOARD.2] Per gli insegnanti senza tariffa, un riquadro con il totale delle ore che stanno donando sui loro corsi attivi (lezioni × durata × allievi).

## Analisi

- `HomeView`: titolo "Ciao …" e poi una sezione di `Card` per ruolo (`HomeView.sezione`, `indicatore`); dati da `DashboardService`.
- Lezioni dell'insegnante: `LezioneRepository.findDeiCorsiAttiviDellInsegnante`; dell'allievo: le sue presenze (`PresenzaRepository.findByAllievoId`), come in `CalendarioService`; `Lezione.isDaFare()`.
- Apertura del registro: `LezioniInsegnanteView` con `?lezione=ID` (`ElencoLezioni.PARAMETRO_LEZIONE`), già usato dal Calendario. Il dettaglio in sola lettura di una lezione è un metodo privato di `CalendarioView` (`dettagli`).
- Minuti di lezione dell'insegnante in `BilancioService`: presenze attese (assenti compresi) × `Corso.durataLezioneMinuti`, sulle lezioni a cui era presente (`findConInsegnantePresente`); `haTariffa` = tariffa > 0; "maturato" = lezioni completate, "previsto" = tutte tranne le non effettuate.

### Decisioni

- [F-2026-10-24/DASHBOARD.1] La vedono insegnanti (lezioni dei loro corsi), allievi (le proprie) e genitori (dei figli, con il nome); non admin e segreteria.
- [F-2026-10-24/DASHBOARD.1] Una per corso (per il genitore una per figlio e corso): la prima lezione da fare con data da oggi in avanti (comprese quelle di oggi), badge "Oggi" o "Domani".
- [F-2026-10-24/DASHBOARD.1] Clic: l'insegnante va al registro delle presenze di quella lezione (Le Mie Lezioni); allievi e genitori vedono il dettaglio in sola lettura del Calendario.
- [F-2026-10-24/DASHBOARD.1] Scelte tecniche: per allievi e genitori l'orario è quello della lezione dell'allievo (`Iscrizione.orarioLezione`) se c'è; il dettaglio del Calendario diventa un dialog riusabile; la sezione "Prossime lezioni" (o "Prossima lezione" se è una) viene subito dopo il saluto, con riquadri cliccabili anche da tastiera.
- [F-2026-10-24/DASHBOARD.2] In grande le ore delle lezioni svolte (completate) dei corsi attivi, sotto "su X previste" (tutte tranne le non effettuate); gli assenti contano, come nel bilancio; solo lezioni a cui l'insegnante era presente.
- [F-2026-10-24/DASHBOARD.2] Il riquadro compare se l'insegnante non ha la tariffa oraria (o è zero), anche con il rimborso forfettario; lo vede solo l'insegnante, nella sezione "I miei corsi".
- [F-2026-10-24/DASHBOARD.2] Scelta tecnica: ore mostrate come "12 h 30 min".

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | `DashboardService.prossimeLezioni(username)`: una per corso (insegnante), per corso (allievo), per figlio e corso (genitore), con l'orario dell'allievo; test | `DashboardService`, test | `./mvnw test` | [x] Tre metodi (insegnante, allievo, figli) chiamati da `HomeView` per ruolo: togliere un ruolo lascia la scheda |
| 2 | Dettaglio della lezione del Calendario estratto in `views/DettaglioLezioneDialog` (nessun cambiamento visibile) | `CalendarioView`, `DettaglioLezioneDialog` | `./mvnw test` | [x] Il dialog accetta anche l'orario della lezione dell'allievo (dalla dashboard); il Calendario passa `null` e resta com'era |
| 3 | `HomeView`: sezione delle prossime lezioni prima dei riepiloghi, riquadri cliccabili (registro per l'insegnante, dettaglio per allievo e genitore); test browserless | `HomeView`, `ViewAccessTest` | `./mvnw test`; browser | [x] Riquadri con `role=button`, clic e Invio; verifica nel browser insieme al passo 5 |
| 4 | `BilancioService.oreDonate(username)`: minuti svolti e previsti sui corsi attivi per l'insegnante senza tariffa; test | `BilancioService`, test | `./mvnw test` | [ ] |
| 5 | `HomeView`: riquadro "Ore donate" nella sezione dell'insegnante; test browserless | `HomeView`, `ViewAccessTest` | `./mvnw test` | [ ] |
| 6 | Documentazione in `CLAUDE.md` | `CLAUDE.md` | rilettura | [ ] |
| 7 | Registro finale | questo piano | build e test | [ ] |

## Fuori ambito

- Prossima lezione per admin e segreteria; ore donate viste da admin e segreteria.

## Registro

_(da compilare durante l'esecuzione)_
