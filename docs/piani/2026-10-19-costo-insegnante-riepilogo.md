# Piano: costo stimato dell'insegnante nel Riepilogo Iscritti

## Richiesta

Nel Riepilogo Iscritti vedere anche il costo stimato dell'insegnante per ogni corso, per un mini bilancio preventivo tra quanto dobbiamo incassare e quanto dovremmo spendere, per i corsi il cui insegnante ha la tariffa oraria ([F-2026-10-19/RIEPILOGO.3]).

## Analisi

- `BilancioService.bilancio(insegnanteId, anno)` calcola già, per insegnante e corso, il *costo previsto*: lezioni a cui l'insegnante è presente (escluse le non effettuate) × presenze della lezione × durata × tariffa; senza lezioni generate stima n. lezioni × iscritti × durata. Con due insegnanti presenti la lezione costa per entrambi. Il calcolo è dentro il ciclo di `bilancio`, non riusabile.
- `RiepilogoIscrittiView` riceve i corsi da `PagamentoService.iscrittiPerCorso()`; la riga del corso ha Da pagare/Pagato/Residuo, il footer i totali, la card mobile i campi.

### Decisioni

- [F-2026-10-19/RIEPILOGO.3] Costo stimato calcolato come il "costo previsto" del Bilancio (stessi numeri), sommando i costi di entrambi gli insegnanti del corso.
- [F-2026-10-19/RIEPILOGO.3] Solo sulla riga del corso due colonne: "Costo insegnante" e "Margine previsto" (= Da pagare − Costo insegnante), con totali in fondo e campi nella card mobile.
- [F-2026-10-19/RIEPILOGO.3] Corso senza nessun insegnante con tariffa: "Senza tariffa" al posto del costo, margine vuoto; i totali di costo e margine sommano solo i corsi con tariffa.
- [F-2026-10-19/RIEPILOGO.3] Visibile ad admin e segreteria, come tutto il Riepilogo Iscritti.
- Tecnica: con due insegnanti di cui uno solo con tariffa, il costo è quello di chi ha la tariffa.

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | `BilancioService.costiPrevistiPerCorso()` (calcolo del costo previsto estratto e condiviso con `bilancio`), conteggio iscritti per corso, test | `service/BilancioService`, `repository/IscrizioneRepository`, `BilancioServiceTest` | `./mvnw test` | [x] |
| 2 | Riepilogo Iscritti: colonne Costo insegnante e Margine previsto, totali, card mobile, test | `views/admin/RiepilogoIscrittiView`, `ViewAccessTest` | `./mvnw test`, browser | [x] |
| 3 | CLAUDE.md e registro | `CLAUDE.md`, questo piano | — | [x] |

## Fuori ambito

- Costo per singolo iscritto.
- Margine sull'incassato (Pagato − costo maturato): resta nel Bilancio dei Rimborsi.

## Registro

- Passo 1: `costiPrevistiPerCorso()` con il calcolo del costo previsto estratto da `bilancio` (stessi numeri); test sul confronto con i bilanci di ogni insegnante e sul corso senza tariffa.
- Passo 2: colonne "Costo insegnante" e "Margine previsto" dopo Stato (indici delle colonne esistenti invariati), totali solo dei corsi con tariffa, campi e footer nella card mobile.
- Verifica: `./mvnw test` 184 test verdi; browser (dev, segreteria): Canto 3.500,00 € / 700,00 €, Chitarra 3.600,00 € / 0,00 €, totale costo 7.100,00 €. Vista mobile non controllata nel browser (finestra non ridimensionabile): usa i campi standard di `CardMobile`.
