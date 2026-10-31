# Stampa del riepilogo corsi senza il riepilogo generale

## Richiesta

[F-2026-10-31/STAMPA.2] Togliere dalla stampa "Riepilogo dei corsi attivi" la parte iniziale con le card.

## Analisi

- Il PDF (`templates/documenti/riepilogo-corsi.ftlh`) si apre con "Riepilogo generale": riquadri con corsi, allievi,
  iscritti, età, genitori e insegnanti (`DocumentiService.riepilogoCorsiHtml`, modello `generale`).
- `RiepilogoCorsi.generale()` serve ancora all'email (numero di iscritti); `eta` serve solo a quella sezione.

### Decisioni

- [F-2026-10-31/STAMPA.2] Si toglie solo la sezione "Riepilogo generale"; restano riquadri e tabelle di Pagamenti e
  Gruppi e gli iscritti.

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | Template e modello senza "Riepilogo generale"; `RiepilogoCorsi` senza `eta`; test e CLAUDE.md | template, `DocumentiService`, `RiepilogoCorsiService`, `DocumentiServiceTest`, `CLAUDE.md` | `./mvnw test` | [x] |
| 2 | Registro | questo piano | — | [ ] |

## Fuori ambito

- La dashboard non cambia.

## Registro
