# Piano: data di fine del corso e aggiornamento delle lezioni

## Richiesta

[F-2026-10-27/LEZIONI.1] Numero di lezioni o data di fine, facoltativi in alternativa: le lezioni si generano per numero o fino alla data di fine.
[F-2026-10-27/LEZIONI.2] Funzione di aggiornamento delle lezioni programmate quando cambiano data di fine o numero di lezioni e soprattutto quando entra una festività.

## Analisi

- `Corso.numeroLezioni` è obbligatorio (`@NotNull`, colonna NOT NULL in MySQL): è il numero di lezioni per partecipante. Usato da `CorsoService.generaLezioni`, `ConteggioLezioni` (previste/mancanti: I miei allievi, `PresenzeAllievoDialog`), la stima del costo senza lezioni in `BilancioService`, le colonne "N. Lezioni" (Gestione Corsi, I Miei Corsi, I Miei Figli).
- `generaLezioni` oggi cancella e ricrea tutte le lezioni da fare (tranne aggiunte e spostate): note, luogo, stati di presenza già impostati si perdono. Salta le festività senza consumare numeri.

### Decisioni

- [F-2026-10-27/LEZIONI.1] Obbligatorio almeno uno tra numero di lezioni e data di fine; con entrambi le lezioni si fermano al primo limite raggiunto. Data di fine non prima della data di inizio.
- [F-2026-10-27/LEZIONI.2] "Genera Lezioni" diventa "Aggiorna lezioni" incrementale: le lezioni da fare ancora valide restano com'erano (note, luogo, presenze); si tolgono le lezioni da fare che cadono in una festività o oltre la fine, si aggiungono le mancanti, poi si rinumera. Anteprima di cosa cambia prima di confermare.
- [F-2026-10-27/LEZIONI.2] Una lezione da fare che cade in una festività si toglie: la programmazione slitta in avanti (con il numero di lezioni) o si perde (con la sola data di fine).
- [F-2026-10-27/LEZIONI.2] Aggiornamento a mano per corso; dopo aver salvato un corso con fine, numero, giorni o cadenza cambiati, e dopo aver salvato o eliminato una festività, un avviso propone l'aggiornamento (per la festività: dei corsi attivi con cambiamenti).
- Scelte tecniche: senza numero di lezioni le lezioni "previste" di un allievo sono le sue lezioni (svolte + da fare) e la stima del costo senza lezioni conta le date del corso fino alla fine; le lezioni da fare mantenute riprendono l'orario del corso e i partecipanti del piano (presenze esistenti conservate, mancanti aggiunte, di troppo tolte).

## Passi

| Passo | Cosa | File | Richiesta | Verifica | Stato |
|---|---|---|---|---|---|
| 1 | `Corso.dataFine`, `numeroLezioni` facoltativo (almeno uno); generazione fino alla data di fine; dialog del corso; "Lezioni" nelle liste; previste e stima del costo senza numero; script SQL; test | `Corso`, `CorsoService`, `CorsiView`, viste, `ConteggioLezioni`, `BilancioService`, `docs/migrazioni` | LEZIONI.1 | `./mvnw test` | [x] Colonna "Lezioni" (`Formati.lezioniCorso`: "30", "fino al …" o entrambi) |
| 2 | Aggiornamento incrementale in `CorsoService` (`anteprimaAggiornamento`, `aggiornaLezioni`, piano comune); test | `CorsoService`, test | — | `./mvnw test` | [x] `generaLezioni` resta come alias di `aggiornaLezioni` |
| 3 | Gestione Corsi: "Aggiorna lezioni" con anteprima; avviso dopo il salvataggio del corso; test | `CorsiView`, test | — | `./mvnw test` | [ ] |
| 4 | Festività: avviso con i corsi attivi da aggiornare e "Aggiorna"; test | `FestivitaView`, `CorsoService`, test | LEZIONI.2 | `./mvnw test` | [ ] |
| 5 | `CLAUDE.md` e registro | `CLAUDE.md`, questo piano | — | test | [ ] |

## Fuori ambito

- Aggiornamento automatico senza conferma.

## Registro

