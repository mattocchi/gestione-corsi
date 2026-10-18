# Piano: login in italiano e recupero password

## Richiesta

[F-2026-10-18/LOGIN.1] Pagina di login in italiano.
[F-2026-10-18/LOGIN.2] Recupero password: link temporaneo via email valido 36 ore (configurabile), pagina per impostare la
nuova password, pagina di conferma con il pulsante per andare al login.

## Analisi

- `LoginView` usa `LoginForm` con i testi di default (inglese), compreso "Forgot password", visibile ma senza effetto.
  Vaadin 25.3: `LoginI18n` (`Form`: title, username, password, submit, forgotPassword; `ErrorMessage`: title, message,
  username, password, `additionalInformation`) e `AbstractLogin.addForgotPasswordListener`.
- Email: `EmailService` invia con `JavaMailSender` solo se `app.mail.host` è impostato, altrimenti simula e scrive nel log;
  corpi HTML con FreeMarker in `templates/email`, dati dell'associazione da `app.associazione.*`.
- Non esiste un indirizzo pubblico dell'app in configurazione: serve per il link (nuovo `app.url`).
- `User`: username, email (facoltativa, più account possono avere la stessa email, es. genitore e figlio), `enabled`.
  Password cifrate con BCrypt solo in `UserService`; il cambio password nel profilo oggi non ha lunghezza minima.
- Sicurezza (OWASP): token casuale di 256 bit (SecureRandom, Base64 URL), in database solo l'hash SHA-256; risposta sempre
  uguale (nessuna enumerazione degli account); link monouso con scadenza; indirizzo del link da configurazione (no
  "password reset poisoning" dall'header Host); nessun token nei log.

### Decisioni

- [F-2026-10-18/LOGIN.1] Login interamente in italiano (titolo, campi, pulsanti, errori); "Password dimenticata?" porta alla pagina di recupero.
- [F-2026-10-18/LOGIN.2] Richiesta con username o email in un unico campo; per un'email di più account arriva un link per ciascun account, con lo username nell'email.
- [F-2026-10-18/LOGIN.2] Indirizzo del link dal nuovo parametro `app.url` (variabile `APP_URL`, default `http://localhost:8080`); validità `app.password-reset.validita-ore` (variabile `PASSWORD_RESET_VALIDITA_ORE`, default 36).
- [F-2026-10-18/LOGIN.2] Nuova password: almeno 8 caratteri e conferma uguale; la stessa regola vale anche nel cambio password del profilo.
- [F-2026-10-18/LOGIN.2] Account inesistenti, senza email o disattivati: nessun invio e sempre lo stesso messaggio generico; gli account disattivati restano tali.
- [F-2026-10-18/LOGIN.2] Link monouso valido 36 ore; una nuova richiesta annulla i link precedenti dello stesso account; token salvato solo come hash.
- [F-2026-10-18/LOGIN.2] Dopo il cambio, email di avviso "password cambiata" all'utente.
- [F-2026-10-18/LOGIN.2] Solo dalla pagina di login (nessuna azione in Gestione Utenti); email di recupero non registrate in "Email inviate", solo nel log (senza token).

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | Login in italiano e "Password dimenticata?" verso la pagina di recupero | `LoginView` | test, browser | [x] |
| 2 | Entità `RecuperoPassword` (hash, utente, scadenza, uso), repository, `PasswordResetService` (richiesta, verifica, reimpostazione), configurazione `app.url` e `app.password-reset.validita-ore`, template email (link e avviso) | `entity/`, `repository/`, `service/`, `config/`, `templates/email/`, `application.properties` | test di servizio | [x] |
| 3 | Viste pubbliche: richiesta del link, nuova password, conferma con pulsante "Vai al login" | `views/RecuperoPasswordView`, `views/NuovaPasswordView` | `ViewAccessTest`, browser desktop/390px | [x] |
| 4 | Regola minima della password anche nel profilo | `ProfiloView`, `UserService` | test | [x] |
| 5 | CLAUDE.md, `deploy/*.env.example` (APP_URL), verifica finale, registro | | browser | [ ] |

## Fuori ambito

- Invio del link da Gestione Utenti; registrazione in "Email inviate"; limitazione del numero di richieste (rate limiting).

## Registro

