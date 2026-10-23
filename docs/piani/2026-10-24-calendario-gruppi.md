# Piano: gruppi separati con il loro orario nel calendario

## Richiesta

[F-2026-10-24/CALENDARIO.1] Nel calendario, se un corso ha più gruppi con un orario di inizio, mostrare separatamente nel giorno ogni gruppo con il suo orario.

## Analisi

- Nei corsi con gruppi c'è una sola `Lezione` per data, condivisa dai gruppi di quel giorno; le presenze sono solo di chi frequenta quel giorno (`Iscrizione.frequenta`, `getDataDelCalendario` per le lezioni spostate).
- `CalendarioService.lezioniDelMese` restituisce `VoceCalendario(lezione, perChi)`; `CalendarioView.pulsante` mostra l'orario della lezione, il corso e, dal numero della lezione, " · Piccoli" (un solo gruppo con nome quel giorno) o " · sab".
- `GruppoCorso`: giorno, orario (facoltativo), nome; `Iscrizione.gruppo` (null = tutti i giorni). Il dettaglio è `DettaglioLezioneDialog`.

### Decisioni

- [F-2026-10-24/CALENDARIO.1] Admin, segreteria e insegnanti: una voce per ogni gruppo con orario che ha allievi a quella lezione, con l'orario e il nome del gruppo (anche se è uno solo); allievi e genitori: solo la voce del gruppo dell'allievo, con il suo orario.
- [F-2026-10-24/CALENDARIO.1] Se alla lezione ci sono anche allievi senza gruppo o di un gruppo senza orario, una voce in più all'orario della lezione ("Senza gruppo", o il nome del gruppo se sono tutti di un gruppo senza orario).
- [F-2026-10-24/CALENDARIO.1] Il dettaglio è quello della lezione con in più la riga "Gruppo" (nome, giorno e orario); "Gestisci presenze" come oggi.
- Scelte tecniche: chi c'è in ogni gruppo si ricava dalle presenze della lezione (valgono recuperi e spostamenti); corsi senza gruppi come oggi; la dashboard non cambia.

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | `CalendarioService`: `VoceCalendario` con gruppo e orario, una voce per gruppo con orario (staff) o per il gruppo dell'allievo (famiglie), voce in più senza gruppo; test | `CalendarioService`, `PresenzaRepository`, `CalendarioServiceTest` | `./mvnw test` | [ ] |
| 2 | `CalendarioView`: orario e gruppo della voce; `DettaglioLezioneDialog` con la riga "Gruppo"; test browserless | `CalendarioView`, `DettaglioLezioneDialog`, `ViewAccessTest` | `./mvnw test` | [ ] |
| 3 | `CLAUDE.md` e registro finale | `CLAUDE.md`, questo piano | build e test | [ ] |

## Fuori ambito

- Prossima lezione della dashboard (resta una per corso, con l'orario dell'allievo).

## Registro

_(da compilare durante l'esecuzione)_
