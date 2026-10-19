# Piano: giorni di lezione del singolo allievo

## Richiesta

Corso con due giorni a settimana e due allievi, dove ognuno frequenta un giorno diverso: gestire i giorni del singolo partecipante ([F-2026-10-19/GIORNI.1]).

## Analisi

- `Corso.giorniSettimana` (tabella `corso_giorni_settimana`, collezione LAZY) vale per tutti; `CorsoService.generaLezioni` crea `numeroLezioni` date tra i giorni del corso e per ogni lezione la presenza di **tutti** gli iscritti. Le presenze si creano anche in `CorsoService.iscrivi`, `LezioneService.aggiungi` e `LezioneService.ripristina`.
- Rimborsi e bilancio contano le presenze (minuti = presenze × durata): se le presenze seguono i giorni dell'allievo, i costi seguono da soli.
- `Iscrizione` ha già i dati del singolo partecipante (orario, quota): è il posto per i suoi giorni. Una nuova `@ElementCollection` (tabella `iscrizione_giorni`) la crea `ddl-auto=update`; nessuna riga = tutti i giorni del corso, quindi i dati esistenti restano invariati.
- Le viste di allievo e genitore e la conferma di iscrizione oggi non mostrano i giorni.

### Decisioni

- [F-2026-10-19/GIORNI.1] Giorni per partecipante: in Partecipanti si scelgono, per ogni allievo, i giorni tra quelli del corso; nessuna scelta = tutti i giorni (default). Ogni lezione ha le presenze solo di chi frequenta quel giorno; rimborsi e bilancio seguono le presenze.
- [F-2026-10-19/GIORNI.1] "Numero lezioni" del corso = lezioni **per ogni allievo**: la generazione continua finché ogni partecipante ha il suo numero di lezioni nei propri giorni (con due giorni e un allievo per giorno, 30 lezioni = 30 date a testa, 60 in tutto). Se tutti frequentano tutti i giorni (o il corso non ha iscritti) il risultato è identico a oggi.
- [F-2026-10-19/GIORNI.1] Cambiando i giorni di un allievo si aggiornano solo le lezioni ancora da fare (tolte le sue presenze nei giorni non più suoi, aggiunte nei nuovi); le lezioni svolte restano come storico.
- [F-2026-10-19/GIORNI.1] I giorni dell'allievo si vedono in "I Miei Corsi" (allievo), nei corsi di "I Miei Figli" (genitore), nella conferma di iscrizione (PDF ed email) e nel Riepilogo Iscritti.
- Conseguenze tecniche (decise qui):
  - nei giorni del corso che nessun iscritto frequenta non si generano lezioni (se il corso ha iscritti);
  - i giorni salvati che non sono più del corso (corso modificato) si ignorano; se non ne resta nessuno valgono tutti i giorni del corso;
  - lezione aggiunta a mano: se è un recupero, presenze di chi aveva la lezione saltata; altrimenti di chi frequenta quel giorno della settimana, o di tutti se non è un giorno del corso; lezione ripristinata: presenze di chi frequenta quel giorno;
  - la scelta dei giorni compare solo per i corsi con più di un giorno; giorni mostrati abbreviati ("mar, gio").

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | Giorni del partecipante: `Iscrizione.giorni`, giorni effettivi, `CorsoService.impostaGiorni` con aggiornamento delle presenze da fare; presenze per giorno in iscrizione, lezione aggiunta e ripristinata | `Iscrizione`, `CorsoService`, `LezioneService`, test | `./mvnw test` | [ ] |
| 2 | Generazione: numero lezioni per ogni allievo, presenze solo nei suoi giorni, giorni senza iscritti saltati | `CorsoService.generaLezioni`, `CorsiView` (aiuto del campo), test | `./mvnw test` | [ ] |
| 3 | Partecipanti: scelta dei giorni per allievo (desktop e card mobile) | `admin/PartecipantiDialog`, `ViewAccessTest` | `./mvnw test`, browser desktop e 390px | [ ] |
| 4 | Giorni dell'allievo in I Miei Corsi, I Miei Figli, conferma di iscrizione e Riepilogo Iscritti | viste, `DocumentiService`, template, `PagamentoService`, test | `./mvnw test`, browser | [ ] |
| 5 | CLAUDE.md e registro | `CLAUDE.md`, questo piano | — | [ ] |

## Fuori ambito

- Orario diverso per giorno dello stesso allievo (resta un solo orario per partecipante).
- Quote diverse in base ai giorni (si usa la quota personalizzata).

## Registro

