# Piano: settimana tipo nel calendario

## Richiesta

[F-2026-10-24/SETTIMANA.1] Nel Calendario una vista "settimana tipo" con tutti i corsi attivi: non un calendario con le date ma la rappresentazione della settimana dei corsi.

## Analisi

- `CalendarioView` (tutti i ruoli) mostra il mese da `CalendarioService.lezioniDelMese` (voci per ruolo; nei corsi con gruppi una voce per gruppo); admin e segreteria hanno il filtro per corso.
- I dati della settimana tipo vengono dai corsi, non dalle lezioni: `Corso.giorniSettimana`, `orarioInizio`, `durataLezioneMinuti`, `cadenzaSettimane`, i gruppi (`GruppoCorso`: giorno, orario, nome) e le iscrizioni (`Iscrizione.gruppo`, `orarioAbituale()`).
- Componenti: `views/SegmentedControl` per le scelte brevi.

### Decisioni

- [F-2026-10-24/SETTIMANA.1] Ogni ruolo vede i suoi corsi attivi: admin e segreteria tutti (filtro per corso), insegnante i suoi, allievo i propri, genitore quelli dei figli (con il nome del figlio).
- [F-2026-10-24/SETTIMANA.1] Selettore "Mese | Settimana tipo" nella barra; nella settimana tipo non ci sono le frecce del mese né "Oggi".
- [F-2026-10-24/SETTIMANA.1] Per ogni giorno, ordinate per orario: orario inizio–fine, corso, gruppo, insegnanti, "ogni 2 sett." per i corsi quindicinali. Nei corsi con gruppi una voce per ogni gruppo del giorno al suo orario, più "Senza gruppo" all'orario del corso se qualcuno non ha gruppo; allievi e genitori solo il giorno e l'orario dell'allievo (`orarioAbituale`, altrimenti quello del corso).
- [F-2026-10-24/SETTIMANA.1] Sola lettura: voci non cliccabili.
- Scelte tecniche: griglia lun–dom da 640px in su, elenco dei soli giorni con corsi sotto (come il mese).

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | `CalendarioService.settimanaTipo(username, corsoId)` con le voci per ruolo; test | `CalendarioService`, `CalendarioServiceTest` | `./mvnw test` | [x] |
| 2 | `CalendarioView`: selettore e vista della settimana (griglia e elenco su mobile); test browserless; browser | `CalendarioView`, `ViewAccessTest` | `./mvnw test`; browser | [ ] |
| 3 | `CLAUDE.md` e registro finale | `CLAUDE.md`, questo piano | build e test | [ ] |

## Fuori ambito

- Festività ed eccezioni (lezioni spostate o aggiunte): la settimana tipo è solo la rappresentazione degli orari dei corsi.

## Registro

