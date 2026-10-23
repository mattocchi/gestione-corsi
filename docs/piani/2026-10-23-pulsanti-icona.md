# Piano: pulsanti con sola icona con l'aspetto di pulsanti

## Richiesta

Dare ai pulsanti con la sola icona l'aspetto di un pulsante, per far capire che sono cliccabili, su desktop e mobile ([F-2026-10-23/PULSANTI.1]).

## Analisi

- I pulsanti con la sola icona nascono da `ViewSupport.iconButton` (azioni nelle righe delle tabelle e nelle card mobile, frecce del calendario) e da `MenuPdf.soloIcona` (stampante delle ricevute nelle righe): entrambi con la variante terziaria (`ButtonVariant.TERTIARY` / `MenuBarVariant.TERTIARY`), cioè solo l'icona, senza fondo né bordo.
- Gli altri pulsanti terziari hanno un testo (Chiudi/Annulla, Logout, "Espandi tutti", lezioni del calendario) e non sono in questione.

### Decisioni

- [F-2026-10-23/PULSANTI.1] Ovunque (desktop e mobile): i pulsanti con la sola icona usano lo stile normale dei pulsanti Aura (fondo e bordo); "Elimina" resta rosso (`ERROR`). I pulsanti con testo restano come sono.

## Passi

| Passo | Cosa | Richiesta | File | Verifica | Stato |
|---|---|---|---|---|---|
| 1 | Stile normale in `ViewSupport.iconButton` e `MenuPdf.soloIcona` | [F-2026-10-23/PULSANTI.1] | `views/ViewSupport`, `views/MenuPdf` | `./mvnw test` | [x] |
| 2 | CLAUDE.md e registro | — | `CLAUDE.md`, questo piano | — | [ ] |

## Fuori ambito

- Stili diversi tra desktop e mobile.

## Registro
