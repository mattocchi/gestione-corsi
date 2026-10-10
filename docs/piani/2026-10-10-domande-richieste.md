# Piano: domande di chiarimento all'avvio dell'implementazione delle RICHIESTE

## Richiesta

Aggiornare le istruzioni per Claude Code: ogni volta che parte una sessione di implementazione di `docs/RICHIESTE.md`, prima fare tutte le domande necessarie a rendere esplicite le informazioni ambigue, poi procedere con il flusso già definito (piano, aggiornamento e avanzamento del piano, avanzamento della richiesta, commit).

## Analisi

- Oggi `CLAUDE.md` (sezione *Requests source*) dice solo che le voci ambigue sono "lasciate aperte o chieste": nella sessione del 10-10 le domande sono state fatte solo per le decisioni sui dati di produzione e per la voce incompleta, mentre altre interpretazioni (es. cosa vede la segreteria in dashboard, dove mostrare i partecipanti, eliminazione per la segreteria) sono state decise in autonomia e solo annotate nel piano.
- Il workflow generale (*plan first*) prevede già di procedere senza attendere l'approvazione del piano: va mantenuto, spostando le domande **prima** della stesura del piano.
- Le risposte vanno conservate: nel piano (*Analisi → Decisioni*) e sotto la voce in `RICHIESTE.md` come chiarimento, così la fonte resta esplicita per le sessioni successive.

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | `CLAUDE.md`: sessione di implementazione delle RICHIESTE in fasi (analisi in sola lettura → giro completo di domande → piano → esecuzione con un commit per passo/richiesta → chiusura); cosa chiedere, come registrare le risposte, domande durante l'esecuzione | `CLAUDE.md` | rilettura | [ ] |

## Fuori ambito

- Nessuna modifica al codice dell'applicazione.
- `build.sh` ha modifiche locali dell'utente non legate a questa richiesta: non incluse.

## Registro

- (da compilare durante l'esecuzione)
