# Piano: filtro per corso in Gestione Allievi

## Richiesta

Aggiungere un filtro per corso all'elenco "Gestione Allievi" ([F-2026-10-22/ALLIEVI.1]).

## Analisi

- `AllieviView` carica gli allievi con i loro corsi (`AllievoService.findAll()`, colonna "N. Corsi") e ha solo la ricerca rapida, collegata con `RicercaRapida.collega`.
- Con altri filtri il pattern del progetto è `addRicercaListener` + `corrisponde(...)` + `mostraConteggio` (es. `CorsiView.applicaFiltri`); il filtro per corso esistente in `ElencoLezioni` è una `ComboBox<Corso>` con "Tutti i corsi" come placeholder.

### Decisioni

- [F-2026-10-22/ALLIEVI.1] `ComboBox` "Tutti i corsi" (vuoto = nessun filtro), un corso alla volta tra tutti i corsi, prima gli attivi poi per nome, etichetta "Nome (anno scolastico)".
- [F-2026-10-22/ALLIEVI.1] Prima scelta "Senza corsi": allievi non iscritti a nessun corso.
- Tecnica: si combina con la ricerca rapida; "N di M" conta gli allievi del filtro scelto.

## Passi

| Passo | Cosa | Richiesta | File | Verifica | Stato |
|---|---|---|---|---|---|
| 1 | Filtro per corso in `AllieviView` (con "Senza corsi"), test | [F-2026-10-22/ALLIEVI.1] | `views/admin/AllieviView`, `ViewAccessTest` | `./mvnw test` | [x] |
| 2 | CLAUDE.md e registro | — | `CLAUDE.md`, questo piano | — | [x] |

## Fuori ambito

- Scelta di più corsi insieme; filtro per anno scolastico.

## Registro

- Passo 1: `./mvnw test` 196 test verdi; nuovo `segreteria_filtraAllieviPerCorso` (scelte del filtro, Chitarra → 6 allievi, insieme alla ricerca, "Senza corsi" → l'allievo appena creato, filtro tolto → tutti).
- Non verificato nel browser. Nessuna modifica al database.
