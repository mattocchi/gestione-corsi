# Piano: dati facoltativi dell'allievo

## Richiesta

[F-2026-10-13/ALLIEVI.1][F-2026-10-13/ALLIEVI.2] Username, codice fiscale e data di nascita non obbligatori quando si
crea o modifica un allievo; codice fiscale e data di nascita servono solo per stampare ricevuta o conferma.

## Analisi

- `Allievo.codiceFiscale` è `@NotBlank` e `dataNascita` `@NotNull`: Hibernate li valida al salvataggio e, con
  `ddl-auto`, crea le colonne `NOT NULL`; in produzione (`update`) le colonne restano `NOT NULL` → serve uno script
  in `docs/migrazioni`.
- `AnagraficaFields(binder, obbligatori=true)` è usato per gli allievi in `AllieviView`, `FigliView` (genitore) e
  `ProfiloView` (allievo); `AllievoService.aggiornaDaGenitore` rifiuta CF o data mancanti.
- Nome e cognome sono sull'`User` collegato: un allievo senza utente richiederebbe un refactoring ampio, quindi
  l'utente resta, con username generato e accesso disattivato. `UserDetailsServiceImpl` usa già `User.enabled`, ma
  in Gestione Utenti manca il campo per riattivarlo.
- Documenti: `DocumentiService.documentoHtml/emailHtml/ricevutaPer` (scarico web e allegati), `EmailService.invia` e
  `inviaConferme`; UI: `Ricevute.link` (in `ElencoPagamenti`, `FigliView`, `QuotaPagamentiDialog`) e `InvioEmailDialog`.

### Decisioni

- [F-2026-10-13/ALLIEVI.1] Username vuoto per un nuovo allievo: si genera uno username da nome e cognome (es. `mario.rossi`, poi `mario.rossi2`…), password casuale e utente disattivato (nessun accesso); la password è richiesta solo se si inserisce lo username.
- [F-2026-10-13/ALLIEVI.1] Per poter dare l'accesso in seguito, Gestione Utenti riceve il campo "Attivo" (conseguenza tecnica della decisione precedente; lo username si modifica già lì).
- [F-2026-10-13/ALLIEVI.1] Codice fiscale e data di nascita facoltativi ovunque per gli allievi (Gestione Allievi, I miei figli, Profilo); il codice fiscale, se inserito, deve essere valido. Script MySQL per togliere `NOT NULL`.
- [F-2026-10-13/ALLIEVI.1] Gestione Allievi mostra un badge arancione "Dati mancanti" (solo icona su mobile) per chi non ha codice fiscale o data di nascita.
- [F-2026-10-13/ALLIEVI.2] Senza codice fiscale o data di nascita dell'allievo ricevuta e conferma non si generano: scarico ricevuta e invio email disabilitati con tooltip, il servizio rifiuta la generazione; l'invio multiplo delle conferme salta questi allievi e li conta nel riepilogo ("dati mancanti").

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | CF e data di nascita facoltativi: entità, campi dei form allievo, servizio genitore; script MySQL; nota "necessari per ricevute e conferme" | `Allievo.java`, `AnagraficaFields.java`, `AllieviView.java`, `FigliView.java`, `ProfiloView.java`, `AllievoService.java`, `docs/migrazioni/2026-10-13-allievi-dati-facoltativi.sql`, test | `./mvnw test` | [ ] |
| 2 | Username facoltativo: generato, password casuale, utente disattivato; campo "Attivo" in Gestione Utenti | `UtenteFields.java`, `AllievoService.java`, `UserService.java`, `AllieviView.java`, `UtentiView.java`, test | `./mvnw test` | [ ] |
| 3 | Documenti solo con i dati: controllo nei servizi, riepilogo invio multiplo, link ricevuta e dialog email disabilitati | `DocumentiService.java`, `EmailService.java`, `Ricevute.java`, `ElencoPagamenti.java`, `FigliView.java`, `QuotaPagamentiDialog.java`, `InvioEmailDialog.java`, test | `./mvnw test` | [ ] |
| 4 | Badge "Dati mancanti" in Gestione Allievi; CLAUDE.md; completa ALLIEVI.1 e ALLIEVI.2 | `AllieviView.java`, `CLAUDE.md`, `docs/FUNZIONALITA.md`, test | `./mvnw test`, browser | [ ] |

## Fuori ambito

- Allievo senza utente collegato (scartato).
- Dati del genitore sui documenti: restano come oggi (facoltativi, stampati se presenti).

## Registro

