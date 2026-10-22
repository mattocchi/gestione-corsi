# Piano: anteprima delle email e pulsanti espliciti

## Richiesta

Un pulsante per vedere l'anteprima delle email (conferma o altre) che partirebbero, e pulsanti che dicano esplicitamente quale email inviano ([F-2026-10-22/EMAIL.1]).

## Analisi

- `admin/InvioEmailDialog.apri(corso, allievo, …)`: dialog del singolo iscritto con "Invia conferma di iscrizione" e "Invia ricevuta"; si apre da "Invia email" (azione in Gestione Pagamenti, pulsante in Quota e pagamenti). `inviaConferme(corso, …)`: invio a tutti gli iscritti da "Invia conferme" in Partecipanti, senza dire a chi.
- `EmailService.invia` costruisce oggetto (tipo – corso anno – allievo), corpo (`DocumentiService.emailHtml`) e allegato (`documentoPdf`, `nomeFile`); `inviaConferme` salta chi non ha i dati per i documenti o un destinatario.
- Vaadin 25.3 `IFrame.setSrcdoc` + `setSandbox()` per mostrare l'HTML dell'email isolato dalla pagina (MCP, `get_java_symbol`).

### Decisioni

- [F-2026-10-22/EMAIL.1] Pulsante "Anteprima" accanto a ogni invio del dialog del singolo iscritto (conferma di iscrizione, ricevuta): destinatario, oggetto, testo come nell'email (riquadro isolato) e PDF allegato da aprire; nessun invio né registrazione.
- [F-2026-10-22/EMAIL.1] Nel dialog di invio delle conferme a tutti: elenco di chi riceve la conferma (a quale indirizzo) e di chi è saltato (senza email o senza codice fiscale/data di nascita), con l'anteprima sul primo iscritto che la riceve.
- [F-2026-10-22/EMAIL.1] Pulsanti rinominati: "Invia email" → "Email: conferma o ricevuta" (tooltip dell'azione in Gestione Pagamenti e pulsante in Quota e pagamenti); "Invia conferme" → "Invia conferme di iscrizione a tutti" (Partecipanti).
- [F-2026-10-22/EMAIL.1] Nessuna anteprima delle email di sistema (recupero password, password cambiata).
- Tecnica: oggetto e corpo vengono dallo stesso codice dell'invio (`EmailService.anteprima`), così l'anteprima coincide con l'email; stessi permessi (admin e segreteria).

## Passi

| Passo | Cosa | Richiesta | File | Verifica | Stato |
|---|---|---|---|---|---|
| 1 | `EmailService.anteprima(...)` e `destinatariConferme(...)` (oggetto in comune con `invia`); test | — | `service/EmailService`, `EmailServiceTest` | `./mvnw test` | [x] |
| 2 | Dialog di anteprima, pulsanti "Anteprima", elenco destinatari nell'invio a tutti, nomi dei pulsanti; test | [F-2026-10-22/EMAIL.1] | `views/admin/InvioEmailDialog`, `views/ElencoPagamenti`, `views/admin/QuotaPagamentiDialog`, `views/admin/PartecipantiDialog`, `ViewAccessTest` | `./mvnw test` | [x] |
| 3 | CLAUDE.md e registro | — | `CLAUDE.md`, questo piano | — | [ ] |

## Fuori ambito

- Anteprima delle email di sistema.

## Registro
