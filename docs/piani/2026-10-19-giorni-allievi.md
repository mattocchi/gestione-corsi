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
| 1 | Giorni del partecipante: `Iscrizione.giorni`, giorni effettivi, `CorsoService.impostaGiorni` con aggiornamento delle presenze da fare; presenze per giorno in iscrizione, lezione aggiunta e ripristinata | `Iscrizione`, `CorsoService`, `LezioneService`, test | `./mvnw test` | [x] |
| 2 | Generazione: numero lezioni per ogni allievo, presenze solo nei suoi giorni, giorni senza iscritti saltati | `CorsoService.generaLezioni`, `CorsiView` (aiuto del campo), test | `./mvnw test` | [x] |
| 3 | Partecipanti: scelta dei giorni per allievo (desktop e card mobile) | `admin/PartecipantiDialog`, `ViewAccessTest` | `./mvnw test`, browser desktop e 390px | [x] |
| 4 | Giorni dell'allievo in I Miei Corsi, I Miei Figli, conferma di iscrizione e Riepilogo Iscritti | `Corso` (giorni caricati sempre), `CorsoService.giorniDellAllievo`, `MieiCorsiView`, `FigliView`, `DocumentiService`, template `conferma`, `RiepilogoIscrittiView`, test | `./mvnw test`, browser | [x] |
| 5 | CLAUDE.md e registro | `CLAUDE.md`, questo piano | — | [x] |

## Fuori ambito

- Orario diverso per giorno dello stesso allievo (resta un solo orario per partecipante).
- Quote diverse in base ai giorni (si usa la quota personalizzata).

## Registro

- Passo 1: `Iscrizione.giorni` (`@ElementCollection` EAGER, tabella `iscrizione_giorni` con chiave corso/allievo), `giorniEffettivi`, `frequenta`; `CorsoService.impostaGiorni` con allineamento delle presenze da fare; presenze per giorno in iscrizione, lezione aggiunta/recupero e ripristinata. Test in `CorsoServiceTest`.
- Passo 2: nuova `generaLezioni` (numero lezioni per allievo; programmate conservate contate per chi frequenta quel giorno, date e numeri non ripetuti; limite di 10 anni). Test: Chitarra con tre allievi il martedì e tre il sabato → 60 lezioni, 3 presenze ciascuna, 30 lezioni per allievo; con tutti i giorni a tutti di nuovo 30 date con 6 presenze. Aiuto "Per ogni allievo, nei giorni che frequenta" sul campo Numero Lezioni.
- Passo 3: colonna/campo "Giorni" (caselle orizzontali, variante `AURA_HORIZONTAL`) in Partecipanti, dialog a 720px con la colonna. Togliere tutti i giorni = tutti. Aggiornati gli indici di colonna nei test di Partecipanti.
- Passo 4: `Corso.giorniSettimana` EAGER per usare i giorni effettivi nelle viste; colonna Giorni in I Miei Corsi, nei corsi di I Miei Figli e nel Riepilogo Iscritti (colonna nome con larghezza minima 14rem); frase nella conferma PDF ed email. Aggiornati gli indici di colonna nei test di allievo e genitore.
- Verifica finale: `./mvnw test` 176 test verdi; browser desktop (Partecipanti di Chitarra: tolto "sab" ad Allievo Chitarra1, salvato subito; Riepilogo Iscritti con "mar" per lui e "mar, sab" per gli altri) e 390px (card dei partecipanti con le caselle dei giorni, senza scorrimento orizzontale).
- Produzione: nessuno script SQL (`ddl-auto=update` crea `iscrizione_giorni`; senza righe tutti frequentano tutti i giorni, come oggi). Le lezioni già generate non cambiano finché non si impostano i giorni (che aggiornano le lezioni da fare) o si rigenera il calendario.
