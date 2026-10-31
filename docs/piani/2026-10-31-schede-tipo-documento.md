# Schede per il tipo di documento in Documenti

## Richiesta

[F-2026-10-31/DOCUMENTI.1] Cambiare il selettore grafico del tipo di documento in Documenti.

## Analisi

- Admin e segreteria scelgono il tipo con un `SegmentedControl` (pulsanti affiancati, `flex-1 min-w-0`): con 6 tipi le
  etichette lunghe ("Lettere di incarico", "Riepilogo corsi") si accorciano su smartphone; CLAUDE.md riserva il
  `SegmentedControl` alle scelte di 2-4 opzioni.
- `Tabs` di Vaadin 25.3 (`add(Tab...)`, `getSelectedTab`, `setSelectedTab`, `addSelectedChangeListener`) è già usato in
  `ElencoLezioni`: su schermi stretti le schede scorrono di lato con il nome intero.

### Decisioni

- [F-2026-10-31/DOCUMENTI.1] Schede (Tabs) con il nome intero, che su smartphone scorrono; solo il selettore del tipo
  di Documenti per admin e segreteria.

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | `DocumentiView`: `Tabs` al posto del `SegmentedControl` del tipo; test aggiornati | `DocumentiView`, `ViewAccessTest` | `./mvnw test` | [ ] |
| 2 | CLAUDE.md: selettore del tipo in Documenti | `CLAUDE.md` | — | [ ] |
| 3 | Registro | questo piano | — | [ ] |

## Fuori ambito

- Il filtro per tipo di "Cronologia documenti" (`EmailInviateView`, 7 opzioni con "Tutte") resta un `SegmentedControl`:
  da valutare a parte.

## Registro
