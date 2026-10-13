# Piano: email di test

## Richiesta

[F-2026-10-13/EMAIL.1][F-2026-10-13/EMAIL.2] Dall'amministrazione poter inviare un'email di test a un destinatario
inserito a mano, per verificare la configurazione SMTP in produzione, scegliendo uno dei template email esistenti per
controllarne la resa HTML nei client di posta.

## Analisi

- `EmailService.invia` lavora solo su un'iscrizione reale e registra sempre in `InvioEmail`; il destinatario è
  calcolato (genitore o allievo), non libero.
- `DocumentiService` rende `email/<template>.ftlh` (corpo) e `documenti/<template>.ftlh` (PDF) a partire da
  `PagamentoService.Ricevuta`; il modello (`modello(Ricevuta)`) si può riusare con entità transient di esempio, così
  i template di test restano allineati a quelli reali.
- `MailConfig` crea il `JavaMailSender` solo con `app.mail.host` valorizzato; altrimenti l'invio è simulato.
- `InvioEmail.tipo` è un enum `STRING`: su MySQL Hibernate usa una colonna `enum(...)`, quindi un nuovo tipo
  richiederebbe una migrazione. Non serve: i test non si registrano.

### Decisioni

- [F-2026-10-13/EMAIL.1] Solo ADMIN: pulsante "Email di test" nella vista "Email inviate" (la segreteria vede la vista ma non il pulsante; il servizio rifiuta chi non è admin).
- [F-2026-10-13/EMAIL.1] Destinatario libero inserito nel dialog (obbligatorio, email valida).
- [F-2026-10-13/EMAIL.1] Nessuna registrazione in "Email inviate": esito ed eventuale errore SMTP completo mostrati nel dialog, più il log.
- [F-2026-10-13/EMAIL.1] Il dialog mostra la configurazione SMTP in uso (host, porta, utente, mittente, STARTTLS; mai la password) e avvisa se l'invio è simulato.
- [F-2026-10-13/EMAIL.2] Template a scelta: "Solo testo" (breve messaggio di prova, nessun allegato), "Conferma di iscrizione", "Ricevuta".
- [F-2026-10-13/EMAIL.2] I template sono compilati con dati fittizi di esempio (nessun dato reale), con il PDF allegato come negli invii reali; oggetto con prefisso "[TEST]".

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | `DocumentiService`: corpo email e PDF di esempio con dati fittizi; template `email/test.ftlh` | `DocumentiService.java`, `templates/email/test.ftlh`, `DocumentiServiceTest.java` | `./mvnw test` | [x] |
| 2 | `EmailService.inviaTest` (solo admin, senza registro) e `configurazione()` senza password | `EmailService.java`, `EmailServiceTest.java` | `./mvnw test` | [x] |
| 3 | `EmailDiTestDialog` e pulsante in "Email inviate" solo per admin; completa EMAIL.1 e EMAIL.2 | `views/admin/EmailDiTestDialog.java`, `EmailInviateView.java`, `ViewAccessTest.java`, `CLAUDE.md`, `docs/FUNZIONALITA.md` | `./mvnw test`, browser | [x] |

## Fuori ambito

- Registrazione dei test nel registro invii (scelto di no).
- Anteprima del template nel browser senza inviare.

## Registro

- Passi 1–3 completati; `./mvnw test`: 126 test, 0 fallimenti (nuovi: esempio in `DocumentiServiceTest`, email di test in `EmailServiceTest` e `EmailServiceSmtpTest`, dialog e visibilità per ruolo in `ViewAccessTest`).
- Browser (dev, admin): pulsante visibile in "Email inviate", dialog con avviso "Server email non configurato", invio "Ricevuta" a un indirizzo di prova → badge "Simulata", nessuna riga aggiunta al registro. Non riuscito il ridimensionamento della finestra a 390px: il dialog è tutto verticale con `maxWidth 100%`, come gli altri.
- Da verificare in produzione (con `MAIL_*` impostate): ricezione reale e resa dei template nei client di posta.
