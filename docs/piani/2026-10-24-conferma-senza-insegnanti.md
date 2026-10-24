# Piano: conferma senza insegnanti

## Richiesta

[F-2026-10-24/CONFERMA.8] Nella conferma di iscrizione togliere la riga che riporta gli insegnanti.

## Analisi

- La riga "Insegnante"/"Insegnanti" è nella macro `datiIscrizione` di `documenti/comune.ftlh`, condivisa da conferma e ricevuta.
- L'email di conferma (`email/conferma.ftlh`) nomina gli insegnanti nella frase iniziale.

### Decisioni

- [F-2026-10-24/CONFERMA.8] Tolti dalla conferma, PDF ed email; la ricevuta mantiene la riga.

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | Parametro `insegnanti` della macro (falso nella conferma), frase dell'email senza insegnanti; test | `comune.ftlh`, `conferma.ftlh` (documenti ed email), `DocumentiServiceTest` | `./mvnw test` | [x] |
| 2 | `CLAUDE.md` e registro finale | `CLAUDE.md`, questo piano | build e test | [x] |

## Fuori ambito

- Ricevuta invariata.

## Registro

- `./mvnw test`: 281 test superati; `DocumentiServiceTest` verifica che conferma (PDF, email, esempio) non nomini gli insegnanti e che la ricevuta sì.
- Lasciato aperto: nessuno.
