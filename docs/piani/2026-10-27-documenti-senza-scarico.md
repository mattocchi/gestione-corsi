# Documenti senza scarico per famiglie e insegnanti

## Richiesta

In Documenti solo admin e segreteria scaricano i PDF; allievi, genitori e insegnanti hanno l'anteprima o l'invio via
email, un invio più formale di un semplice download — [F-2026-10-27/SCARICO.1]

## Analisi

- Famiglie (genitori e allievi adulti) e insegnanti aprono i PDF solo da `DocumentiView` (`azioniFamiglia`,
  `pdfLettera`) con `MenuPdf.soloIcona`, che ha due link: anteprima inline e "Scarica PDF". Lo staff usa lo stesso
  menu più "Invia per email" (`InvioEmailDialog`).
- `EmailService` invia solo per admin e segreteria (`verificaPermesso`) e al destinatario dell'allievo
  (`destinatarioDi`). Serve un invio "a me" che controlli i permessi come l'anteprima (`DocumentiService.*Per`,
  `LetteraIncaricoService.lettera`) e mandi all'email del profilo dell'utente, riusando corpo, oggetto e registro
  (`spedisciERegistra`).
- La cronologia (`AperturaDocumento.Modalita`) ha ANTEPRIMA/SCARICO: si aggiunge EMAIL ("Inviato per email"),
  VARCHAR 16 già sufficiente, nessuna migrazione.
- L'anteprima inline può comunque essere salvata dal visualizzatore del browser (accettato).

### Decisioni

- [F-2026-10-27/SCARICO.1] Genitori, allievi adulti e insegnanti: menu con "Anteprima in una nuova scheda" e
  "Invia alla mia email"; "Scarica PDF" solo ad admin e segreteria.
- [F-2026-10-27/SCARICO.1] L'email va solo all'indirizzo del profilo dell'utente, non modificabile; senza email
  l'invio è bloccato con un avviso. Conferma prima di inviare con l'indirizzo.
- [F-2026-10-27/SCARICO.1] Ogni invio è registrato in "Email inviate" (inviato da = l'utente) e in "Documenti aperti"
  con la modalità "Inviato per email" (solo se non in errore).
- [F-2026-10-27/SCARICO.1] Anche anteprime e invii degli insegnanti (lettere di incarico) entrano nella cronologia.

## Passi

| Passo | Cosa | File | Verifica | Stato | Richiesta |
|---|---|---|---|---|---|
| 1 | `EmailService`: invio dei propri documenti all'email del profilo (iscrizione, riepilogo, attestazione, lettera) con i permessi dell'anteprima; `Modalita.EMAIL` e badge in `GrigliaAperture` | `EmailService`, `AperturaDocumento`, `GrigliaAperture`, `EmailServiceTest` | `./mvnw test` | [x] | — |
| 2 | Documenti: menu "Anteprima" + "Invia alla mia email" per famiglie e insegnanti (conferma, cronologia anche per gli insegnanti), scarico solo per lo staff; CLAUDE.md | `MenuPdf`, `DocumentiView`, `ViewAccessTest`, `CLAUDE.md` | `./mvnw test` | [x] | F-2026-10-27/SCARICO.1 |
| 3 | Registro | piano | — | [ ] | — |

## Fuori ambito

- Impedire del tutto il salvataggio dell'anteprima dal visualizzatore PDF del browser.
- Invio a indirizzi diversi da quello del profilo.

## Registro

