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
| 2 | `CLAUDE.md` e registro finale | `CLAUDE.md`, questo piano | build e test | [x] |

## Fuori ambito

- "Nuova lezione" ed eliminazione delle lezioni aggiunte: permessi invariati.

## Registro

- `./mvnw test`: 283 test superati. `LezioneServiceTest.sposta_…` verifica il rifiuto all'insegnante del corso; in `ViewAccessTest` `insegnante_nonVedeCambiaData` e `segreteria_cambiaLaDataDiUnaLezione` (al posto del vecchio test dell'insegnante).
- Browser: non verificato (solo visibilità di un pulsante, coperta dai test browserless).
- Lasciato aperto: nessuno.
