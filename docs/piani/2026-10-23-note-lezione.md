# Piano: note della lezione

## Richiesta

Note sulla lezione che admin, segreteria o insegnante possono aggiungere, oltre alle note per singolo allievo ([F-2026-10-23/NOTE.1]).

## Analisi

- `Lezione.note` esiste già (colonna `note`) ma nessuna view la usa: diventa la nota per lo staff, senza modifiche allo schema. La nota pubblica è una colonna nuova facoltativa (`note_pubbliche`), aggiunta da `ddl-auto=update`, nessuno script.
- Le note per allievo sono in `Presenza.note`, modificate nel dialog "Gestisci Presenze" di `views/ElencoLezioni` e salvate subito.
- Allievi e genitori vedono le lezioni nel dettaglio del Calendario (`CalendarioView.dettagli`) e in `PresenzeAllievoDialog` (I Miei Corsi / I Miei Figli).
- Permessi come le altre modifiche alla lezione (`LezioneService.gestibile`): insegnante del corso, admin, segreteria, corso attivo.

### Decisioni

- [F-2026-10-23/NOTE.1] Due note: "Note per lo staff" (solo admin, segreteria e insegnanti del corso) e "Note per allievi e genitori".
- [F-2026-10-23/NOTE.1] Si scrivono nel dialog "Gestisci Presenze", sopra l'elenco degli allievi, salvate subito (notifica solo in caso di errore).
- [F-2026-10-23/NOTE.1] Modificabili sempre (da fare, completate, non effettuate), sui corsi attivi.
- [F-2026-10-23/NOTE.1] Allievi e genitori vedono le note pubbliche nel dettaglio del Calendario e in `PresenzeAllievoDialog`; lo staff vede entrambe anche nel dettaglio del Calendario.

## Passi

| Passo | Cosa | Richiesta | File | Verifica | Stato |
|---|---|---|---|---|---|
| 1 | `Lezione.notePubbliche`; `LezioneService.impostaNote` (permessi, testo ripulito, vuoto = null); test | — | `entity/Lezione`, `service/LezioneService`, `LezioneServiceTest` | `./mvnw test` | [ ] |
| 2 | Le due aree di testo nel dialog presenze (anche per le lezioni non effettuate); note nel dettaglio del Calendario secondo il ruolo e in `PresenzeAllievoDialog`; test browserless | [F-2026-10-23/NOTE.1] | `views/ElencoLezioni`, `views/CalendarioView`, `views/PresenzeAllievoDialog`, `ViewAccessTest` | `./mvnw test` | [ ] |
| 3 | CLAUDE.md e registro | — | `CLAUDE.md`, questo piano | — | [ ] |

## Fuori ambito

- Invio via email delle note pubbliche.

## Registro
