# Piano: riepilogo dei pagamenti nella dashboard del genitore

## Richiesta

Nella dashboard del genitore una o due card di riepilogo dei pagamenti ([F-2026-10-22/DASHBOARD.3]).

## Analisi

- `HomeView`, ruolo GENITORE: sezione "I miei figli" con la sola card "Corsi dei figli".
- `PagamentoService.righePagamentiDeiFigli(username)` dà già una riga per iscrizione dei figli (corsi attivi e non) con la `Situazione` (quota, pagato, residuo).

### Decisioni

- [F-2026-10-22/DASHBOARD.3] Due card nella sezione "I miei figli": "Pagato" (dettaglio "N% della quota") e "Da pagare" (dettaglio "su X di quota", o "tutto saldato"), per tutti i figli insieme.
- [F-2026-10-22/DASHBOARD.3] Solo le iscrizioni ai corsi attivi; senza iscrizioni a corsi attivi le card non compaiono.

## Passi

| Passo | Cosa | Richiesta | File | Verifica | Stato |
|---|---|---|---|---|---|
| 1 | `PagamentoService.situazioneDeiFigli(username)` (corsi attivi); card in `HomeView`; test | [F-2026-10-22/DASHBOARD.3] | `service/PagamentoService`, `views/HomeView`, `ViewAccessTest` | `./mvnw test` | [x] |
| 2 | CLAUDE.md e registro | — | `CLAUDE.md`, questo piano | — | [x] |

## Fuori ambito

- Dettaglio per figlio nella dashboard (c'è già in Pagamenti).

## Registro

- Passo 1: `./mvnw test` 211 test verdi; `genitore_dashboardConICorsiDeiFigli` verifica le due card (genitore3: 1.200 € da pagare) e il nuovo `genitore_dashboardConIPagamentiDeiCorsiAttivi` verifica che un corso non attivo non conti e il "tutto saldato".
- Non verificato nel browser. Nessuna modifica al database.
