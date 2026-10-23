# Piano: modifica della data di una lezione

## Richiesta

Permettere ad admin, segreteria e insegnante di modificare la data di una lezione generata ([F-2026-10-23/DATA.1]).

## Analisi

- Le lezioni si gestiscono in `views/ElencoLezioni` (Le Mie Lezioni, Lezioni, anche dal Calendario) tramite `LezioneService`; i permessi (`gestibile`/`verificaPermesso`) sono già quelli richiesti: insegnante del corso, admin, segreteria, corso attivo.
- `CorsoService.generaLezioni` elimina e ricrea le lezioni programmate da fare: una lezione solo spostata andrebbe persa e ricreata alla data originale. Serve ricordare la data originale.
- Le presenze si creano con `Iscrizione.frequenta(lezione.getData())` (iscrizione, `sincronizzaPresenze`, conteggio in generazione, ripristino) e la numerazione per giorno dei corsi con gruppi usa il giorno della data (`NumerazioneLezioni`): per una lezione spostata va usata la data originale, così restano gli stessi partecipanti e lo stesso numero.

### Decisioni

- [F-2026-10-23/DATA.1] Si spostano solo le lezioni da fare (né completate né non effettuate), generate o aggiunte a mano.
- [F-2026-10-23/DATA.1] Si memorizza la data originale (`Lezione.dataOriginale`, colonna facoltativa `data_originale`, aggiunta da `ddl-auto=update`, nessuno script); "Genera Lezioni" conserva la lezione spostata e non ne crea un'altra alla data originale; badge "Spostata" con "Spostata dal …"; riportata alla data originale torna normale.
- [F-2026-10-23/DATA.1] Si cambiano data e orario d'inizio (fine ricalcolata con la durata del corso).
- [F-2026-10-23/DATA.1] Pulsante "Cambia data" nel piede del dialog "Gestisci Presenze".
- [F-2026-10-23/DATA.1] Stessi partecipanti e, nei corsi con gruppi, stesso numero del giorno originale.
- [F-2026-10-23/DATA.1] Rifiutata se il corso ha già un'altra lezione quel giorno; in un giorno festivo solo un avviso.
- [F-2026-10-23/DATA.1] Nessuna email agli allievi/genitori; nessuna build di produzione alla fine.

## Passi

| Passo | Cosa | Richiesta | File | Verifica | Stato |
|---|---|---|---|---|---|
| 1 | `Lezione.dataOriginale` + `getDataDelCalendario()`; `LezioneService.sposta`; generazione, presenze e numerazione sulla data del calendario; test | — | `entity/Lezione`, `service/LezioneService`, `service/CorsoService`, `service/NumerazioneLezioni`, `LezioneServiceTest` | `./mvnw test` | [x] |
| 2 | "Cambia data" nel dialog presenze, badge "Spostata", riga nel dettaglio del Calendario; test browserless | [F-2026-10-23/DATA.1] | `views/ElencoLezioni`, `views/CalendarioView`, `ViewAccessTest` | `./mvnw test` | [x] |
| 3 | CLAUDE.md e registro | — | `CLAUDE.md`, questo piano | — | [x] |

## Fuori ambito

- Avviso via email dello spostamento.
- Spostamento di lezioni completate o non effettuate.

## Registro

- Passo 1: `./mvnw test` 233 test verdi; nuovo `sposta_tienePartecipantiENumero_eGeneraLezioniLaConserva` (permessi, giorno occupato, presenze e numero invariati, rigenerazione che conserva lo spostamento, ritorno alla data originale, lezioni completate rifiutate).
- Passo 2: `./mvnw test` 234 test verdi; nuovo `insegnante_cambiaLaDataDiUnaLezione` (dialog con data e orario della lezione, salvataggio, badge "Spostata" nella riga).
- Chiusura: `./mvnw test` 234 test verdi. Nessuna build di produzione (scelta dell'utente). Non verificato nel browser. Non c'è un test dedicato alla numerazione per giorno di una lezione spostata in un corso con gruppi (la logica usa `getDataDelCalendario()` come per la rigenerazione, coperta dal test del passo 1). Produzione: la colonna `data_originale` la aggiunge `ddl-auto=update`, nessuno script.
