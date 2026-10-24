# Piano: cambio data della lezione solo ad admin e segreteria

## Richiesta

[F-2026-10-24/CAMBIO.1] Solo admin e segreteria vedono "Cambia data" e possono spostare una lezione programmata.

## Analisi

- `LezioneService.sposta` usa `verificaPermesso`: oggi anche l'insegnante del corso può spostare la lezione.
- `ElencoLezioni` (Gestisci Presenze, condiviso da Lezioni e Le Mie Lezioni) mostra "Cambia data" su ogni lezione non completata.
- Il cambio data vale già solo per le lezioni da fare (`isDaFare`).

### Decisioni

- [F-2026-10-24/CAMBIO.1] Pulsante visibile e spostamento consentito solo ai ruoli ADMIN e SEGRETERIA; l'insegnante non lo vede e il servizio lo rifiuta.

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | `LezioneService.puoSpostare(username)` usato da `sposta` e da `ElencoLezioni`; test (servizio: insegnante rifiutato; vista: insegnante senza pulsante, segreteria sposta) | `LezioneService`, `ElencoLezioni`, `LezioneServiceTest`, `ViewAccessTest` | `./mvnw test` | [x] |
| 2 | `CLAUDE.md` e registro finale | `CLAUDE.md`, questo piano | build e test | [ ] |

## Fuori ambito

- "Nuova lezione" ed eliminazione delle lezioni aggiunte: permessi invariati.

## Registro

