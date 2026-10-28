# Intestatario dei documenti fiscali

## Richiesta

L'utente resta con nome, cognome ed email; per l'attestazione delle spese si può indicare un intestatario diverso
(nome, cognome, codice fiscale, es. il coniuge che detrae), gestito in modo chiaro nel Profilo; senza nome e cognome
diversi si usano quelli dell'utente — [F-2026-10-28/INTESTATARIO.1]

## Analisi

- L'attestazione (`DocumentiService.attestazioneHtml`) intesta lo "Spett." al genitore (`getNomeCompleto` = nome
  dell'utente, `Genitore.codiceFiscale`, indirizzo del genitore) o all'allievo adulto (i suoi dati);
  `Attestazione.datiMancanti` chiede il codice fiscale e l'indirizzo del genitore.
- `Genitore.codiceFiscale` è già facoltativo e oggi compare in "Dati anagrafici" (`AnagraficaFields`) nel Profilo del
  genitore e nel dialog di Genitori.
- L'allievo adulto ha un Profilo in sola lettura (`SchedaAllievo`); il suo codice fiscale è quello dello studente,
  necessario per ricevute e attestazione, quindi non può diventare quello dell'intestatario.
- Nuove colonne facoltative: `ddl-auto=update` le aggiunge, nessuno script.

### Decisioni

- [F-2026-10-28/INTESTATARIO.1] Genitore: il campo codice fiscale esistente (stessa colonna, nessuna migrazione) passa
  dalla sezione "Dati anagrafici" a una nuova sezione "Intestazione dei documenti fiscali", con aiuto esplicito
  (serve solo per intestare l'attestazione delle spese); nella stessa sezione nome e cognome dell'intestatario,
  facoltativi, con quelli dell'utente come segnaposto e valore usato se vuoti.
- [F-2026-10-28/INTESTATARIO.1] Allievo adulto: stessa sezione con nome, cognome e codice fiscale dell'intestatario,
  tutti facoltativi; se vuoti si usano i dati dell'allievo (il suo codice fiscale resta quello dello studente).
- [F-2026-10-28/INTESTATARIO.1] Indirizzo dell'intestatario: quello del genitore (o la residenza dell'allievo adulto).
- [F-2026-10-28/INTESTATARIO.1] Solo l'attestazione delle spese usa l'intestatario; conferme, ricevute e riepiloghi no.
- [F-2026-10-28/INTESTATARIO.1] Lo impostano il genitore e l'allievo adulto dal Profilo, admin e segreteria nel dialog
  di Genitori e nella scheda dell'allievo (Gestione Allievi) quando non ha genitore.
- Le attestazioni si generano al momento: una modifica vale anche per i PDF degli anni passati richiesti dopo.

## Passi

| Passo | Cosa | File | Verifica | Stato | Richiesta |
|---|---|---|---|---|---|
| 1 | Dati e attestazione: `Genitore.intestazioneNome/Cognome`, `Allievo.intestazioneNome/Cognome/CodiceFiscale`, intestatario dell'attestazione con i ripieghi, dati mancanti "codice fiscale dell'intestatario" | entity, `PagamentoService`, `DocumentiService`, test | `./mvnw test` | [x] | — |
| 2 | Genitore: sezione "Intestazione dei documenti fiscali" (codice fiscale spostato lì) nel Profilo e nel dialog di Genitori | `views/IntestazioneFields`, `AnagraficaFields`, `ProfiloView`, `GenitoriView`, `GenitoreService`, test | `./mvnw test` | [ ] | — |
| 3 | Allievo adulto: stessa sezione modificabile nel Profilo e nella scheda di Gestione Allievi; CLAUDE.md | `ProfiloView`, `SchedaAllievo`, `AllieviView`, `AllievoService`, test, `CLAUDE.md` | `./mvnw test` | [ ] | F-2026-10-28/INTESTATARIO.1 |
| 4 | Registro | piano | — | [ ] | — |

## Fuori ambito

- Indirizzo proprio dell'intestatario.
- Intestatario diverso per figlio o per anno.
- Controllo di coerenza tra codice fiscale e nome/cognome dell'intestatario (si verifica solo la validità).

## Registro

