# Piano: invio email di conferme di iscrizione e ricevute

## Richiesta

[F-2026-10-11/EMAIL.1] Predisporre l'applicazione all'invio di email.
[F-2026-10-11/EMAIL.2] Admin e segreteria inviano la conferma di iscrizione per ogni corso dell'allievo, con l'importo da pagare.
[F-2026-10-11/EMAIL.3] Admin e segreteria inviano al genitore le ricevute dei corsi con pagamenti (il genitore le scarica anche dal web).
[F-2026-10-11/EMAIL.4] Ogni invio è registrato con data e metadati.
[F-2026-10-11/EMAIL.5] Proposta: FreeMarker per l'HTML delle email, JasperReports per i PDF delle ricevute, per coerenza.

## Analisi

- Nessuna dipendenza email/template oggi. La ricevuta è la pagina stampabile `RicevutaView` (route `ricevuta/:corsoId/:allievoId`, controllo genitore/admin/segreteria), collegata da `ElencoPagamenti`, `FigliView` e `QuotaPagamentiDialog` con `RicevutaView.link(...)`; dati da `PagamentoService.ricevuta()`.
- Email: `User.email` di genitore e allievo (`Allievo.getGenitore()`).
- Parere sulla tecnologia (EMAIL.5): FreeMarker è adatto (starter Spring Boot); JasperReports introdurrebbe un secondo template (`.jrxml`) da allineare a quello HTML, una libreria pesante e problemi tipici di font. Per la coerenza conviene un solo template FreeMarker → HTML, convertito in PDF con OpenHTMLtoPDF: identico nell'email, nell'allegato e nello scarico dal web. Jasper resta da valutare per report complessi futuri.
- Spring Boot 4.1.1 gestisce le versioni di `spring-boot-starter-freemarker` e `spring-boot-starter-mail`. OpenHTMLtoPDF: fork mantenuto `io.github.openhtmltopdf:openhtmltopdf-pdfbox`, versione 1.1.85 (1 settembre 2026, scelta dall'utente; l'ultima è 1.1.97 del 9 ottobre).
- Scelte tecniche: `JavaMailSender` esiste solo se è configurato `spring.mail.host` (da `MAIL_HOST`): `EmailService` lo ottiene come `ObjectProvider` e, se manca, simula l'invio (log + registro con esito "simulato"). Invio sincrono con notifica dell'esito; l'invio multiplo procede per partecipante e riporta un riepilogo. Un invio fallito viene comunque registrato con esito "errore" e messaggio.

### Decisioni

- [F-2026-10-11/EMAIL.5] FreeMarker + OpenHTMLtoPDF 1.1.85 al posto di JasperReports: un solo template per ricevuta e conferma.
- [F-2026-10-11/EMAIL.2][F-2026-10-11/EMAIL.3] Email HTML + PDF allegato. Conferma: corso, allievo, anno, insegnante, quota da pagare, già pagato, residuo, istruzioni per il bonifico. Ricevuta: riepilogo nel corpo e ricevuta completa in PDF; inviabile solo se ci sono pagamenti.
- [F-2026-10-11/EMAIL.2][F-2026-10-11/EMAIL.3] Destinatario: email del genitore; se l'allievo non ha genitore o il genitore non ha email, quella dell'allievo; senza email l'invio non è possibile (pulsante disabilitato con spiegazione).
- [F-2026-10-11/EMAIL.2][F-2026-10-11/EMAIL.3] Invio da Gestione Pagamenti (azione sulla riga del partecipante) e dal dialog Quota e pagamenti; invio multiplo delle conferme a tutti gli iscritti di un corso dal dialog Partecipanti. Solo admin e segreteria.
- [F-2026-10-11/EMAIL.4] Registro: data e ora, tipo, corso, allievo, destinatario, oggetto, allegato, utente che ha inviato, esito (inviata / simulata / errore con messaggio); niente corpo. Visibile nel dialog Quota e pagamenti e in una nuova vista "Email inviate" (admin e segreteria) con filtri.
- [F-2026-10-11/EMAIL.1] Invio reale se è configurato l'SMTP (`MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`), altrimenti simulato, con qualunque profilo (anche lo staging).
- [F-2026-10-11/EMAIL.2][F-2026-10-11/EMAIL.3] Dati dell'associazione (nome, indirizzo, codice fiscale, email, IBAN) da configurazione `app.associazione.*`, in testata dei documenti; IBAN nelle istruzioni di pagamento della conferma. Valori reali da fornire o impostare sul server.
- [F-2026-10-11/EMAIL.3] Lo scarico web della ricevuta diventa il PDF dello stesso template (per genitore, admin e segreteria); la pagina stampabile `RicevutaView` viene sostituita.

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | Dipendenze (FreeMarker, Mail, OpenHTMLtoPDF 1.1.85) e configurazione: `app.associazione.*`, SMTP da variabili d'ambiente, mittente | `pom.xml`, `application.properties`, `Application.java`, `config/AssociazioneProperties.java`, `config/MailProperties.java`, `deploy/gestione-corsi.env.example` | `./mvnw test` | [x] |
| 2 | Template FreeMarker di ricevuta e conferma (documento + corpo email) e `DocumentiService` (HTML e PDF); test | `src/main/resources/templates/*.ftlh`, `service/DocumentiService.java`, test, `docs/FUNZIONALITA.md` | `./mvnw test` | [x] |
| 3 | Ricevuta web in PDF: link di download con controllo dei permessi al posto di `RicevutaView`; test | `views/Ricevute.java`, viste che usano la ricevuta, `RicevutaView.java` (rimossa), test | `./mvnw test` | [ ] |
| 4 | `InvioEmail` (registro) e `EmailService`: destinatario, invio reale o simulato con PDF allegato, registrazione di ogni tentativo; test | `entity/InvioEmail.java`, `repository/InvioEmailRepository.java`, `service/EmailService.java`, test, `docs/FUNZIONALITA.md` | `./mvnw test` | [ ] |
| 5 | Invio dalla UI: azione sulla riga di Gestione Pagamenti, pulsanti nel dialog Quota e pagamenti, "Invia conferme" nel dialog Partecipanti; test | `views/ElencoPagamenti.java`, `views/admin/QuotaPagamentiDialog.java`, `views/admin/PartecipantiDialog.java`, `views/admin/InvioEmailDialog.java`, test, `docs/FUNZIONALITA.md` | `./mvnw test` | [ ] |
| 6 | Registro nella UI: sezione "Email inviate" nel dialog Quota e pagamenti e vista "Email inviate" con filtri; test | `views/admin/EmailInviateView.java`, `views/admin/QuotaPagamentiDialog.java`, test, `docs/FUNZIONALITA.md` | `./mvnw test` | [ ] |
| 7 | `CLAUDE.md`, `deploy/README.md` (SMTP), registro del piano | `CLAUDE.md`, `deploy/README.md`, piano | rilettura | [ ] |

## Fuori ambito

- Scarico web della conferma di iscrizione per il genitore; invio multiplo delle ricevute.
- Invio asincrono/coda, reinvio automatico degli errori.
- JasperReports (vedi decisione).
- `.idea/.copilot-plugin` (modifica locale preesistente) non incluso.

## Registro
