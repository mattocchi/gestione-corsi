# Piano: card degli iscritti in dashboard

## Richiesta

Nella dashboard di admin e segreteria, accanto ad "Allievi", una card con il totale degli iscritti (un allievo può fare più corsi) ([F-2026-10-22/DASHBOARD.1]).

## Analisi

- `HomeView` → sezione "Riepilogo generale" con le card Corsi, Allievi, Genitori, Insegnanti da `DashboardService.riepilogoGenerale()` (conteggi delle tabelle).
- Le iscrizioni sono `Iscrizione` (tabella `corso_allievi`); `IscrizioneRepository.findByCorsoAttivo()` esiste già, serve un conteggio.

### Decisioni

- [F-2026-10-22/DASHBOARD.1] Card "Iscritti" subito dopo "Allievi", con il numero di iscrizioni ai soli corsi attivi.

## Passi

| Passo | Cosa | Richiesta | File | Verifica | Stato |
|---|---|---|---|---|---|
| 1 | Conteggio iscrizioni dei corsi attivi in `RiepilogoGenerale`; card "Iscritti"; test | [F-2026-10-22/DASHBOARD.1] | `repository/IscrizioneRepository`, `service/DashboardService`, `views/HomeView`, `ViewAccessTest` | `./mvnw test` | [x] |
| 2 | CLAUDE.md e registro | — | `CLAUDE.md`, questo piano | — | [x] |

## Fuori ambito

- Iscritti dei corsi non attivi.

## Registro

- Passo 1: `./mvnw test` 200 test verdi; nuovo `segreteria_dashboardContaGliIscrittiDeiCorsiAttivi` (allievo in due corsi: 13 allievi, 14 iscritti; corso non attivo escluso); indici delle card della dashboard aggiornati nei test esistenti.
- Etichetta "Iscritti ai corsi attivi" (senza riga di dettaglio, che rimpicciolisce il numero) per restare coerente con la card "Allievi".
- Non verificato nel browser. Nessuna modifica al database.
