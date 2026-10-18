# Piano: due insegnanti per corso e presenza degli insegnanti alle lezioni

## Richiesta

[F-2026-10-18/INSEGNANTI.1–3] Assegnare fino a due insegnanti a un corso; indicare per ogni lezione se erano presenti
entrambi o uno solo; lo possono indicare insegnante, segreteria e admin (non allievi e genitori).

## Analisi

- `Corso.insegnante` (unico, obbligatorio nel dialog) è usato da: "Le Mie Lezioni" e corsi dell'insegnante
  (`LezioneRepository`/`CorsoRepository.findByInsegnante…`), permessi su lezioni (`LezioneService.insegnanteDelCorso`),
  dashboard e inoltro iniziale (`countByInsegnanteUserUsernameAndAttivoTrue`), costi (`BilancioService`: lezioni e
  presenze per `lezione.corso.insegnante`), documenti (`DocumentiService`), viste (Corsi, Lezioni, I miei corsi, Figli,
  Insegnanti "N. Corsi").
- Per conservare lo storico serve sapere chi era presente a ogni lezione: insieme `Lezione.insegnantiPresenti`
  (tabella `lezione_insegnanti`). Le lezioni esistenti non hanno righe: un avvio idempotente registra gli insegnanti del
  corso sulle lezioni senza insegnanti (nessuno script manuale in produzione; una lezione non resta mai senza insegnanti).
- `spring.jpa.open-in-view=false`: l'insieme si carica con la lezione (EAGER con `@BatchSize`), il secondo insegnante
  con il corso (`@ManyToOne`, EAGER come il primo).

### Decisioni

- [F-2026-10-18/INSEGNANTI.1] Titolare obbligatorio + secondo insegnante facoltativo (diverso dal primo), con gli stessi diritti: entrambi vedono il corso in "Le Mie Lezioni", completano lezioni, segnano presenze, aggiungono ed eliminano le lezioni aggiunte.
- [F-2026-10-18/INSEGNANTI.1] Allievi, genitori e conferma di iscrizione PDF mostrano entrambi i nomi; la presenza degli insegnanti alle lezioni non è visibile a loro.
- [F-2026-10-18/INSEGNANTI.2] Ogni lezione nasce con entrambi presenti; nel dialog "Gestisci Presenze", solo per i corsi con due insegnanti, tre pulsanti "Entrambi / Solo X / Solo Y" (almeno uno presente), salvati subito; modificabile anche dopo il completamento, non sui corsi non attivi né sulle lezioni non effettuate.
- [F-2026-10-18/INSEGNANTI.2] Rimborsi e bilancio: ogni insegnante matura il costo delle lezioni a cui era presente, con la sua tariffa (con entrambi presenti il corso costa per due).
- [F-2026-10-18/INSEGNANTI.2] Lo storico resta: le lezioni completate o non effettuate conservano gli insegnanti presenti; cambiando gli insegnanti del corso si aggiornano solo le lezioni ancora da fare.
- [F-2026-10-18/INSEGNANTI.2] Nessun badge nell'elenco lezioni per le lezioni con un solo insegnante.
- [F-2026-10-18/INSEGNANTI.3] Presenza degli insegnanti modificabile da un insegnante del corso, segreteria e admin (stessi controlli di "completa"); allievi e genitori non vi accedono.

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | Modello: `Corso.secondoInsegnante`, `Lezione.insegnantiPresenti`; generazione, aggiunta e salvataggio del corso li tengono allineati; avvio che completa le lezioni esistenti | `entity/`, `CorsoService`, `LezioneService`, `InsegnantiPresentiIniziali` | test | [x] |
| 2 | Accesso del secondo insegnante: Le Mie Lezioni, corsi, dashboard, permessi | repository, `LezioneService`, `CorsoService`, `DashboardService` | test | [x] |
| 3 | Costi per presenza: rimborsi, proposta di rimborso, bilancio | `BilancioService`, repository | test | [x] |
| 4 | Presenza degli insegnanti: servizio e controllo nel dialog Gestisci Presenze | `LezioneService`, `ElencoLezioni` | test, browser | [x] |
| 5 | Interfaccia: secondo insegnante nel dialog del corso, nomi di entrambi in Corsi, Lezioni, I miei corsi, Figli, Insegnanti, conferma PDF | viste, `DocumentiService` | test, browser desktop/390px | [x] |
| 6 | CLAUDE.md, verifica finale, registro | | browser | [ ] |

## Fuori ambito

- Più di due insegnanti; badge nell'elenco lezioni; visibilità della presenza degli insegnanti ad allievi e genitori.

## Registro

