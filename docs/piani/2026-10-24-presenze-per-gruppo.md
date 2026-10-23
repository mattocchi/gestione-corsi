# Piano: registro delle presenze diviso per gruppo

## Richiesta

[F-2026-10-24/PRESENZE.1] Nel registro delle presenze della lezione l'insegnante vede gli allievi per gruppo, scegliendo il gruppo come una scheda (tab), per inserire presenze e altro.

## Analisi

- Il registro è il dialog "Gestisci Presenze" di `ElencoLezioni.openPresenzeDialog`, condiviso da "Le Mie Lezioni" (insegnante) e "Lezioni" (admin e segreteria): una griglia di `Presenza` nell'ordine dei partecipanti (`LezioneService.presenzeInOrdine`), con la colonna "Gruppo" nei corsi con gruppi (`gruppiDeiPartecipanti`) e card su mobile.
- Nei corsi con gruppi la lezione è una per giorno e ha le presenze di tutti i gruppi di quel giorno e di chi è senza gruppo.
- Vaadin 25.3: `Tabs`/`Tab` con `addSelectedChangeListener`; la griglia si filtra con `GridListDataView.setFilter`.

### Decisioni

- [F-2026-10-24/PRESENZE.1] Una scheda per ogni gruppo con allievi alla lezione, in ordine di orario ("Piccoli 10:00 (3)"), una "Senza gruppo" se c'è chi non ne ha e "Tutti" in fondo; all'apertura il primo gruppo.
- [F-2026-10-24/PRESENZE.1] "Segna Completata" e "Non effettuata" restano per tutta la lezione; le schede filtrano solo l'elenco degli allievi (presenze e note come oggi), senza azioni per gruppo.
- [F-2026-10-24/PRESENZE.1] Per insegnanti, admin e segreteria (stesso dialog), solo nelle lezioni con almeno due schede di gruppo (più gruppi, o un gruppo e chi è senza).

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | Schede per gruppo nel dialog "Gestisci Presenze" che filtrano la griglia; test browserless | `ElencoLezioni`, `ViewAccessTest` | `./mvnw test` | [x] |
| 2 | `CLAUDE.md` e registro finale | `CLAUDE.md`, questo piano | build e test | [ ] |

## Fuori ambito

- Completamento o "non effettuata" per gruppo; "tutti presenti" per gruppo.

## Registro

_(da compilare durante l'esecuzione)_
