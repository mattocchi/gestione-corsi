# Piano: anni scolastici non attivi nei filtri

## Richiesta

[F-2026-10-27/ANNI.1] Nei filtri (anche della finestra attestazioni) devono restare gli anni scolastici precedenti, anche non attivi, se ci sono dati: un anno non attivo non si usa per nuovi inserimenti ma si usa per cercare nello storico.

## Analisi

- `AnnoScolastico.attivo` limita già solo il dialog del corso (`AnnoScolasticoService.scelteCorso`).
- I filtri prendono gli anni in modi diversi: dai dati della lista (Gestione Corsi, Riepilogo Iscritti, Gestione Pagamenti, Documenti) o dall'elenco degli anni (Rimborsi, I miei rimborsi); così un anno può mancare (es. dati in un anno non più nell'elenco, o un anno dell'elenco senza dati nella vista corrente) e i non attivi non sono distinti.
- Le attestazioni sono per anno solare (periodo d'imposta) e il filtro elenca già gli anni con pagamenti.

### Decisioni

- [F-2026-10-27/ANNI.1] In tutti i filtri per anno scolastico: anni dell'elenco (attivi o no) più quelli dei dati, dal più recente; i non attivi con "(archivio)". Solo il dialog del corso propone i soli anni attivi.
- [F-2026-10-27/ANNI.1] Attestazioni: per anno solare, con tutti gli anni passati con pagamenti (anche dei corsi archiviati).

## Passi

| Passo | Cosa | File | Richiesta | Verifica | Stato |
|---|---|---|---|---|---|
| 1 | `AnnoScolasticoService.perFiltro(anniDeiDati)` e etichetta "(archivio)"; filtri di Documenti, Rimborsi, I miei rimborsi, Gestione Pagamenti, Gestione Corsi, Riepilogo Iscritti; test; CLAUDE.md; registro | service, viste, test | ANNI.1 | `./mvnw test` | [x] Gestione Pagamenti: anni dell'elenco solo per lo staff (le famiglie vedono gli anni dei loro dati) |

## Fuori ambito

- Il filtro per anno solare delle attestazioni resta com'è.

## Registro

- `./mvnw test`: 330 test superati. Nuovi: `AnnoScolasticoServiceTest.perFiltro_ancheGliAnniNonAttiviEQuelliDeiDati`, `ViewAccessTest.segreteria_filtraPerUnAnnoScolasticoNonAttivo` (2024/2025 disattivato resta nel filtro di Gestione Corsi come "(archivio)" e filtra i suoi corsi).
- Problema trovato e corretto: con l'anno corrente sempre in elenco, Riepilogo Iscritti si apriva su un anno senza corsi; la selezione iniziale resta sull'anno corrente solo se ha dati.
- Non verificato nel browser.
- Lasciato aperto: nessuno.

