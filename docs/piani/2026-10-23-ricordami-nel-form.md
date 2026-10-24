# Piano: casella "Ricordami" prima di "Password dimenticata?"

## Richiesta

[F-2026-10-23/RICORDAMI.2] Nella pagina di accesso mettere la casella "Ricordami" prima dell'opzione per recuperare la password.

## Analisi

- `LoginView`: la casella "Ricordami su questo dispositivo" è aggiunta sotto il `LoginForm`, che contiene già il pulsante "Password dimenticata?" (sotto "Accedi"): oggi la casella viene dopo il link.
- Il componente web `vaadin-login-form` (25.3) ha lo slot `custom-form-area`, tra la password e il pulsante di invio (`node_modules/@vaadin/login/src/vaadin-login-form.js`); l'API Java del `LoginForm` non lo espone (c'è `getCustomFormArea()` solo in `LoginOverlay`), ma basta aggiungere la casella come figlia dell'elemento con l'attributo `slot="custom-form-area"`.
- La scelta arriva al server con il cookie tecnico `ricordami` (`SecurityConfig.COOKIE_RICORDAMI`), non come campo del form: spostarla non cambia il funzionamento.

### Decisioni

- [F-2026-10-23/RICORDAMI.2] Casella dentro il form, sotto la password e sopra "Accedi"; "Password dimenticata?" resta sotto "Accedi".

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | Casella nello slot `custom-form-area` del `LoginForm`; test che sia figlia del form in quello slot; `CLAUDE.md` | `LoginView.java`, `security/LoginTest` o `ViewAccessTest`, `CLAUDE.md` | `./mvnw test`; browser | [x] Ripreso il 2026-10-24 dallo stash (lasciato a metà il 23/10) |
| 2 | Registro finale | questo piano | build e test | [x] |

## Fuori ambito

- Nessun altro cambiamento al login.

## Registro

- Il passo 1 era rimasto nello stash `stash@{0}` dal 23/10 (interrotto da un'altra richiesta) e non era mai stato committato: per questo in produzione la casella era ancora sotto il form. Ripreso il 2026-10-24, lo stash si applica senza conflitti.
- `./mvnw test`: 277 test superati, nuovo `ViewAccessTest.login_ricordamiNelFormSopraAccedi`.
- Browser (dev): casella sotto la password e sopra "Accedi", "Password dimenticata?" sotto; spuntandola si imposta il cookie `ricordami=1` e l'accesso con admin funziona.
- Lasciato aperto: lo stash `stash@{0}` si può eliminare (contenuto ora committato).
