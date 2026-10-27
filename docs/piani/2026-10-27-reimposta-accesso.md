# Piano: reimpostare l'accesso di un utente con l'invito

## Richiesta

[F-2026-10-27/ACCESSO.1] Usare l'invito anche per reimpostare un utente da admin: l'invito disabilita l'account e si riparte con l'attivazione su invito e la scelta della password.

## Analisi

- L'invito (`RecuperoPasswordService.invita`, `admin/InvitoUI`) vale solo per chi ha un'email e non è mai entrato (`invitabile`); il link (`RecuperoPassword.invito`, 7 giorni) imposta la password, attiva l'account ed entra (`AccessoDiretto`). Il clic invia subito, senza anteprima.
- Un link è valido solo se l'account può recuperare (`puoRecuperare`: attivo, mai entrato o sospeso per inattività): un account disattivato dopo l'uso oggi non potrebbe usare un invito.
- Le sessioni di un utente si chiudono con `security/SessioniUtenti`; il "Ricordami" è firmato anche con la password.

### Decisioni

- [F-2026-10-27/ACCESSO.1] Solo l'admin, in Gestione Utenti, e non sul proprio utente; la segreteria continua a invitare solo chi non è mai entrato.
- [F-2026-10-27/ACCESSO.1] Lo stesso pulsante: "Invita ad accedere" per chi non è mai entrato, "Reimposta accesso" per chi è già entrato (solo admin); disabilitato solo senza email (o, per la segreteria, per chi è già entrato).
- [F-2026-10-27/ACCESSO.1] Reimpostando, l'account si blocca subito: disattivato, password vecchia non più valida (anche il "Ricordami"), sessioni chiuse; il link (7 giorni) fa scegliere la nuova password, riattiva ed entra. Vale anche per gli account disattivati a mano.
- [F-2026-10-27/ACCESSO.1] Prima dell'invio un dialog con destinatario, "Anteprima" del messaggio e "Invia"; per la reimpostazione l'avviso che l'account viene bloccato. "Invita selezionati" chiede conferma con il numero (solo inviti: chi è già entrato è saltato).
- Scelte tecniche: un link d'invito è valido anche se l'account è disattivato dopo l'uso (lo emette lo staff); disattivando a mano un account attivo i suoi link aperti si annullano.

## Passi

| Passo | Cosa | File | Richiesta | Verifica | Stato |
|---|---|---|---|---|---|
| 1 | Service: reimpostazione (solo admin, non se stesso: disattiva, password casuale, link d'invito, email "accesso reimpostato"), anteprima dell'email, link d'invito validi per account disattivati, link annullati alla disattivazione manuale; test | `RecuperoPasswordService`, `UserService`, template, test | — | `./mvnw test` | [x] Proprio account escluso anche dall'invito semplice |
| 2 | `InvitoUI`: etichetta secondo il caso, dialog con anteprima e invio, chiusura delle sessioni dopo la reimpostazione; conferma per "Invita selezionati"; test | `InvitoUI`, `UtentiView`, test | ACCESSO.1 | `./mvnw test` | [ ] |
| 3 | `CLAUDE.md` e registro | `CLAUDE.md`, questo piano | — | test | [ ] |

## Fuori ambito

- Reimpostazione multipla di utenti già entrati.

## Registro

