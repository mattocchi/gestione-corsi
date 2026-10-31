# Opzioni aggiuntive del corso per iscritto

## Richiesta

[F-2026-10-31/OPZIONI.1] Su alcuni corsi un importo una tantum in più che vale solo per alcuni iscritti (es.
Propedeutica 130 € in pre-iscrizione o 150 € pieno, più "opzione kit +30 €"): nel corso più opzioni con importo e
motivazione, su ogni iscritto l'attivazione sì/no, e l'opzione esplicitata nella conferma di iscrizione.

## Analisi

- La quota dell'iscritto è `Iscrizione.getQuotaDovuta()` (quota personalizzata o `Corso.costoTotale`), usata da
  `PagamentoService` (`Situazione`, righe dei pagamenti, iscritti per corso, riepiloghi), `BilancioService` (incasso
  previsto), `PartecipantiDialog`: sommando lì le opzioni attive tutti i totali, gli stati del pagamento, le ricevute,
  i riepiloghi e la stampa dei corsi le comprendono senza altre modifiche.
- Gli importi di conferma e ricevuta (email e PDF) vengono dal macro `templates/importi.ftlh` con il modello
  `DocumentiService.importi(...)`: prezzo di listino barrato e variazione solo con una quota personalizzata.
- Famiglie: `views/PrezziCorso` (I miei figli, I miei corsi) mostra listino, pre-iscrizione e quota dovuta con il
  badge sconto/maggiorazione calcolato tra quota dovuta e listino (con le opzioni diventerebbe una falsa
  "maggiorazione"); `PagamentiAllievoDialog` mostra la nota della quota personalizzata.
- I gruppi del corso sono il modello per una tabella figlia (`corso_gruppi`, `GruppoCorsoRepository`, eliminati in
  `CorsoService.delete`, copiati in `duplica`).
- Nuove tabelle con `ddl-auto=update`: nessuno script SQL per la produzione.

### Decisioni

- [F-2026-10-31/OPZIONI.1] Nel dialog del corso una sezione "Opzioni aggiuntive" con più righe (descrizione e importo),
  salvate con il corso; admin e segreteria le creano e modificano, eliminare solo l'admin e solo se nessun iscritto
  l'ha attiva.
- Attivazione per iscritto in Partecipanti: una casella per opzione (colonna "Opzioni", visibile solo se il corso ne
  ha), salvata subito.
- La quota dovuta è sempre quota del corso (piena, pre-iscrizione o personalizzata) + opzioni attive (130 + 30 = 160 €).
- L'importo è quello del corso: cambiandolo cambia la quota di chi ha l'opzione (gli invii già fatti restano).
- Conferma e ricevuta (email e PDF): una riga per opzione attiva sopra la QUOTA DOVUTA; riepilogo annuale della
  famiglia con i soli totali.
- Genitori e allievi adulti vedono le opzioni attive nei prezzi del corso (I miei figli, I miei corsi), non quelle
  non attivate.
- "Duplica" copia le opzioni (senza attivazioni).
- Dati demo: Propedeutica con "Kit strumentario" +30 € attivo per Sara Ferri e Tommaso Russo.
- Scelte tecniche: entità `OpzioneCorso` (tabella `corso_opzioni`), attivazioni in `iscrizione_opzioni` (`@ManyToMany`
  EAGER su `Iscrizione`); `Situazione` porta le opzioni attive per mostrare il dettaglio; le attivazioni valgono anche
  per i corsi non attivi, come quote e pagamenti.

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | Dati e regole: `OpzioneCorso`, `Iscrizione.opzioni`, quota dovuta con le opzioni, `CorsoService` (opzioni del corso, salvataggio con il corso, attivazione, duplica, eliminazione del corso), `Situazione` con le opzioni | entità, repository, `CorsoService`, `PagamentoService`, test | `./mvnw test` | [x] |
| 2 | Dialog del corso: sezione "Opzioni aggiuntive" | `CorsiView` | test browserless | [x] |
| 3 | Partecipanti: colonna "Opzioni" con le caselle | `PartecipantiDialog` | test browserless | [x] |
| 4 | Conferma e ricevuta: righe delle opzioni negli importi | `DocumentiService`, `importi.ftlh` | test sul PDF | [x] |
| 5 | Famiglie: opzioni attive nei prezzi del corso e nel dettaglio pagamenti | `PrezziCorso`, `PagamentiAllievoDialog` | test browserless | [x] |
| 6 | Dati demo, Guida e CLAUDE.md | `DatiDemo`, `guida/staff.md`, `CLAUDE.md`, test | `./mvnw test` | [x] |
| 7 | Registro | questo file | — | [ ] |

## Fuori ambito

- Colonna delle opzioni in Pagamenti e Iscritti (la quota mostrata le comprende già).
- Importo bloccato al momento dell'attivazione (vale sempre quello del corso).

## Registro
