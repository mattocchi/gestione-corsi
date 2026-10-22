# Piano: costo insegnanti e margine dei corsi in dashboard

## Richiesta

Nella tabella dei corsi della dashboard (admin e segreteria) aggiungere il costo stimato degli insegnanti e un mini prospetto tra quanto si dovrebbe incassare dagli iscritti e quanto si spende per gli insegnanti, per vedere se il corso è in attivo o in perdita ([F-2026-10-22/DASHBOARD.2]).

## Analisi

- `HomeView.pagamenti(...)`: card dei totali e tabella "Pagamenti dei corsi attivi" da `PagamentoService.riepilogoCorsiAttivi()` (`RiepilogoPagamenti`: nome del corso, saldati/parziali/senza pagamenti, incassato, da incassare; ultima riga = totale).
- Il Riepilogo Iscritti calcola già "Costo insegnante" con `BilancioService.costiPrevistiPerCorso()` (solo insegnanti con tariffa oraria, costo previsto delle lezioni; corsi senza tariffa assenti) e "Margine previsto" = quote dovute − costo.
- Badge colorati con `BadgeVariant.SUCCESS`/`ERROR` (come `StatoPagamentoUI`).

### Decisioni

- [F-2026-10-22/DASHBOARD.2] Colonna "Costo insegnanti": costo previsto come nel Riepilogo Iscritti; "Senza tariffa" se nessun insegnante del corso ha la tariffa oraria. Il rimborso forfettario annuo non entra.
- [F-2026-10-22/DASHBOARD.2] Colonna "Margine" = quote dovute dagli iscritti − costo insegnanti, con badge verde "Attivo" (margine ≥ 0) o rosso "Perdita".
- [F-2026-10-22/DASHBOARD.2] Due card "Costo insegnanti previsto" e "Margine previsto" dei corsi attivi, esclusi i corsi senza tariffa (indicati nel dettaglio della card).
- Tecnica: `RiepilogoPagamenti` porta anche l'id del corso e la quota totale; colonne e card anche nella card mobile.

## Passi

| Passo | Cosa | Richiesta | File | Verifica | Stato |
|---|---|---|---|---|---|
| 1 | `RiepilogoPagamenti` con id del corso e quota; colonne "Costo insegnanti" e "Margine" e due card in `HomeView`; test | [F-2026-10-22/DASHBOARD.2] | `service/PagamentoService`, `views/HomeView`, `ViewAccessTest` | `./mvnw test` | [x] |
| 2 | CLAUDE.md e registro | — | `CLAUDE.md`, questo piano | — | [x] |

## Fuori ambito

- Forfait annuo nel costo dei corsi.

## Registro

- Passo 1: `./mvnw test` 206 test verdi; `segreteria_dashboardConRiepilogoPagamenti` verifica costo (Canto 3.500 €), margine (700 €, "Attivo") e le due nuove card; indici delle card dei gruppi aggiornati.
- Non verificato nel browser. Nessuna modifica al database.
