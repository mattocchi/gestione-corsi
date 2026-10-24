# Piano: età degli allievi

## Richiesta

[F-2026-10-24/ETA.1] Nell'elenco allievi data di nascita in formato gg/mm/aaaa ed età in anni compiuti a oggi.
[F-2026-10-24/ETA.2] In dashboard di admin e segreteria card con età media, più giovane e più anziano.

## Analisi

- `admin/AllieviView` e `genitore/FigliView` mostrano "Data Nascita" con `Formati.data` ("mar 5 marzo 2012"); `Formati.dataNumerica` (dd/MM/yyyy) esiste già per i documenti.
- `HomeView` "Riepilogo generale" (admin/segreteria) usa `DashboardService.riepilogoGenerale()` e le card `indicatore(...)`.
- La data di nascita è facoltativa: chi non ce l'ha resta senza età ed è escluso dalle statistiche.

### Decisioni

- [F-2026-10-24/ETA.1] Gestione Allievi e I miei figli: "Data Nascita" 05/03/2012 e colonna "Età" a parte (anni compiuti, ordinabile); su mobile due campi della card.
- [F-2026-10-24/ETA.2] Card "Età media", "Più giovane", "Più anziano" con le sole età in anni (media arrotondata), sugli allievi iscritti ad almeno un corso attivo con data di nascita; nessuna card se non c'è nessuno.
- Scelta tecnica: le card stanno nel "Riepilogo generale", dopo "Iscritti ai corsi attivi".

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | `Allievo.getEta(oggi)`; colonne "Data Nascita" (gg/mm/aaaa) ed "Età" in Gestione Allievi e I miei figli; test | `Allievo`, `AllieviView`, `FigliView`, test | `./mvnw test` | [x] `Formati.eta` e `Formati.PER_ETA` condivisi |
| 2 | `DashboardService.etaAllievi()` e card nel Riepilogo generale; test | `IscrizioneRepository`, `DashboardService`, `HomeView`, test | `./mvnw test` | [x] |
| 3 | `CLAUDE.md` e registro finale | `CLAUDE.md`, questo piano | build e test | [ ] |

## Fuori ambito

- Altre viste con date (restano con `Formati.data`).

## Registro

