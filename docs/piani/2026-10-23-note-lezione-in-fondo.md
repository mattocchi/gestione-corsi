# Piano: note della lezione dopo l'elenco delle presenze

## Richiesta

[F-2026-10-23/NOTE.2] Nel dialog "Gestisci Presenze" spostare le note della lezione (per lo staff e per allievi e genitori) in fondo, dopo l'elenco delle presenze e prima dei pulsanti.

## Analisi

- `ElencoLezioni.openPresenzeDialog`: oggi `createNoteLezione(lezione)` è aggiunto subito dopo il titolo, prima di "Insegnanti presenti" e della sezione "Allievi" con la griglia delle presenze; i pulsanti sono nel footer del dialog.
- Per la lezione non effettuata il dialog non ha l'elenco delle presenze: avviso, note, "Recuperata il …" e pulsanti.
- `ViewAccessTest` cerca le caselle per etichetta: lo spostamento non lo tocca.

### Decisioni

- [F-2026-10-23/NOTE.2] Le note vanno dopo la griglia delle presenze con un titolo di sezione "Note della lezione" (`ViewSupport.sezione`), come "Allievi" e "Insegnanti presenti".
- [F-2026-10-23/NOTE.2] Il dialog della lezione non effettuata resta com'è (avviso, note, eventuale "Recuperata il …").

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | Note della lezione in fondo al dialog "Gestisci Presenze", con il titolo "Note della lezione" | `views/ElencoLezioni.java`, `CLAUDE.md` se serve | `./mvnw test` | [x] |
| 2 | Registro finale | questo piano | build e test | [x] |

## Fuori ambito

- Il dialog della lezione non effettuata (decisione sopra).

## Registro

- `./mvnw test`: tutti i test superati (compreso il test delle note in `ViewAccessTest`, che cerca le caselle per etichetta).
- Nessuna verifica visiva nel browser: l'estensione non riesce a fare screenshot in questa sessione; il cambiamento è solo l'ordine dei componenti nel dialog.
- Lasciato aperto: nessuno.
