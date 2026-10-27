# Piano: fino a 3 insegnanti facoltativi per corso

## Richiesta

[F-2026-10-27/INSEGNANTI.1] Aggiungere fino a 3 insegnanti opzionali per corso.

## Analisi

- Oggi `Corso.insegnante` (titolare, obbligatorio) e `Corso.secondoInsegnante` (colonna `secondo_insegnante_id`, facoltativo, stessi diritti). `Corso.getInsegnanti()` / `getNomiInsegnanti()` / `haInsegnante(username)` sono usati ovunque; le query "dei corsi dell'insegnante" fanno LEFT JOIN su titolare e secondo (`CorsoRepository`, `IscrizioneRepository`, `LezioneRepository`, `PresenzaRepository`, `InsegnanteRepository.findAllWithCorsi` con `Insegnante.corsiComeSecondo`).
- `Lezione.insegnantiPresenti` è già un insieme: costi e presenze degli insegnanti funzionano con qualsiasi numero.
- Gestisci Presenze: con due insegnanti `SegmentedControl` "Entrambi / Solo X / Solo Y" (`ElencoLezioni`, `LezioneService.impostaInsegnantiPresenti`).

### Decisioni

- [F-2026-10-27/INSEGNANTI.1] Titolare più fino a 3 insegnanti facoltativi (massimo 4 per corso); il secondo insegnante di oggi diventa il primo dei facoltativi.
- [F-2026-10-27/INSEGNANTI.1] I facoltativi hanno gli stessi diritti del titolare (lezioni, presenze, note, allievi, costi per le lezioni a cui sono presenti, lettere di incarico).
- [F-2026-10-27/INSEGNANTI.1] Nel dialog del corso un campo a scelta multipla "Altri insegnanti" (fino a 3, diversi dal titolare, nell'ordine di scelta).
- [F-2026-10-27/INSEGNANTI.1] Gestisci Presenze: con due insegnanti resta "Entrambi / Solo X / Solo Y"; con più di due una casella per insegnante (tutte spuntate di default, almeno una).
- [F-2026-10-27/INSEGNANTI.1] Database: tabella dedicata `corso_insegnanti` (corso, insegnante, ordine); script `docs/migrazioni/2026-10-27-insegnanti-facoltativi.sql` da eseguire in produzione prima del deploy, che copia i secondi insegnanti esistenti.

## Passi

| Passo | Cosa | File | Richiesta | Verifica | Stato |
|---|---|---|---|---|---|
| 1 | `Corso.insegnantiFacoltativi` (lista ordinata, tabella `corso_insegnanti`) al posto di `secondoInsegnante`; query dei corsi dell'insegnante; validazione in `CorsoService.save` (max 3, distinti, non il titolare); script di migrazione; test | entity, repository, `CorsoService`, `docs/migrazioni`, test | — | `./mvnw test` | [ ] |
| 2 | Dialog del corso: "Altri insegnanti" a scelta multipla; test | `CorsiView`, `ViewAccessTest` | INSEGNANTI.1 | `./mvnw test` | [ ] |
| 3 | Gestisci Presenze: caselle degli insegnanti presenti con più di due insegnanti; test | `ElencoLezioni`, test | INSEGNANTI.1 | `./mvnw test` | [ ] |
| 4 | `CLAUDE.md` e registro | `CLAUDE.md`, questo piano | — | build e test | [ ] |

## Fuori ambito

- Nessun limite diverso per i servizi (stesse regole dei corsi).

## Registro

