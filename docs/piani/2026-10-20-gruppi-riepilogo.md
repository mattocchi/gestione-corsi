# Piano: gruppi nel Riepilogo Iscritti

## Richiesta

Nel Riepilogo Iscritti rendere chiari i gruppi di ogni corso ([F-2026-10-20/GRUPPI.4]).

## Analisi

- `RiepilogoIscrittiView` è una `TreeGrid` a un livello (corso → iscritti) con righe `Voce(corso, iscritto)`; il gruppo dell'iscritto compare solo nella colonna "Giorni / Gruppo".
- I gruppi del corso (`GruppoCorso`) non arrivano alla vista: `PagamentoService.IscrittiCorso` ha solo corso e iscritti.

### Decisioni

- [F-2026-10-20/GRUPPI.4] Nei corsi con gruppi un livello in più: corso → gruppi → iscritti; sotto il corso una riga per gruppo e, se qualcuno non ne ha, "Senza gruppo (tutti i giorni)". I corsi senza gruppi restano corso → iscritti.
- [F-2026-10-20/GRUPPI.4] La riga del gruppo mostra solo nome e numero di iscritti (niente importi, costo o margine).
- [F-2026-10-20/GRUPPI.4] All'avvio e con "Espandi tutti" sono aperti corsi e gruppi; "Comprimi tutti" lascia le righe dei corsi.
- Tecnica: anche i gruppi senza iscritti compaiono sotto il corso; con la ricerca, se il corso corrisponde si vedono tutti i gruppi, altrimenti solo quelli con iscritti trovati. Su mobile la card del gruppo è rientrata sotto il corso, con "Mostra/Nascondi iscritti", e le card dei suoi iscritti rientrano di un altro livello.

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | Gruppi del corso in `IscrittiCorso`; livello gruppo nella TreeGrid (righe, espansione, ricerca, card mobile, stile della riga), test | `service/PagamentoService`, `views/admin/RiepilogoIscrittiView`, `styles.css`, `ViewAccessTest` | `./mvnw test`, browser | [x] |
| 2 | CLAUDE.md e registro | `CLAUDE.md`, questo piano | — | [x] |

## Fuori ambito

- Importi per gruppo.

## Registro

- Passo 1: `IscrittiCorso` porta i gruppi del corso; `Voce` ha tre tipi di riga (corso, gruppo, iscritto) e le righe figlie sono calcolate in `applicaFiltri` (mappa `figli`). I corsi senza gruppi sono invariati (stesso numero di righe nei test esistenti).
- Verifica: `./mvnw test` 187 test verdi; browser (dev, segreteria, finestra stretta): gruppo "Martedì 18:00" con 1 iscritto e "Senza gruppo (tutti i giorni)" sotto Chitarra, card rientrate su due livelli. Tolto il corsivo della riga del gruppo, che nella card tagliava "Nascondi iscritti".
