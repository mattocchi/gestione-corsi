# Piano: ordine dei partecipanti in Gestione Pagamenti

## Richiesta

In Gestione Pagamenti, filtrando per corso, i partecipanti devono seguire lo stesso ordine introdotto per i partecipanti: gruppo o orario se ci sono, altrimenti inserimento ([F-2026-10-22/PARTECIPANTI.2]).

## Analisi

- `views/ElencoPagamenti` è condiviso da `admin/GestionePagamentiView` (gestione) e `genitore/PagamentiFigliView`; le righe arrivano già ordinate da `PagamentoService.righe(...)` con `PER_CORSO_E_ALLIEVO` (nome del corso, poi nome dell'allievo); il filtro per corso agisce sulle righe caricate senza riordinarle.
- `Iscrizione.ORDINE` (gruppo, orario, inserimento) è già usato in Partecipanti, Gestisci Presenze e Riepilogo Iscritti.

### Decisioni

- [F-2026-10-22/PARTECIPANTI.2] Gestione Pagamenti sempre per corso (nome, anno scolastico) e dentro ogni corso in `Iscrizione.ORDINE`, con o senza filtro per corso; le intestazioni delle colonne continuano a riordinare a mano.
- [F-2026-10-22/PARTECIPANTI.2] I Pagamenti del genitore restano per corso e nome del figlio.
- [F-2026-10-22/PARTECIPANTI.2] Nuova colonna "Gruppo" (vuota senza gruppo, ordinabile) solo in Gestione Pagamenti, anche nella card mobile.

## Passi

| Passo | Cosa | Richiesta | File | Verifica | Stato |
|---|---|---|---|---|---|
| 1 | `righePagamenti()` per corso e `Iscrizione.ORDINE`; colonna/campo "Gruppo" in gestione; test | [F-2026-10-22/PARTECIPANTI.2] | `service/PagamentoService`, `views/ElencoPagamenti`, test | `./mvnw test` | [x] |
| 2 | CLAUDE.md e registro | — | `CLAUDE.md`, questo piano | — | [ ] |

## Fuori ambito

- Ordine dei Pagamenti del genitore.

## Registro
