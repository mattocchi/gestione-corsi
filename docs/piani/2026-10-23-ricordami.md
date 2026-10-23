# Piano: ricordami al login

## Richiesta

Proporre un meccanismo di "ricordami" all'accesso ([F-2026-10-23/RICORDAMI.1]); proposta approvata.

## Analisi

- Login con `LoginView` (`LoginForm` di Vaadin, invio nativo a `/login` gestito dal form login di Spring Security, configurato da `VaadinSecurityConfigurer` in `SecurityConfig`). La sessione scade dopo l'inattività o alla chiusura del browser.
- Vaadin 25.3 (MCP): `LoginForm` non ha la casella "ricordami" né un'area per campi aggiuntivi (`getCustomFormArea` c'è solo in `LoginOverlay`).
- Spring Security: `TokenBasedRememberMeServices` crea un cookie firmato con scadenza; la firma include la password, quindi un cambio password invalida i cookie; in rientro controlla lo stato dell'account (disattivato = rifiutato); è anche un `LogoutHandler` (cancella il cookie all'uscita). Il rientro passa dall'`AuthenticationManager` e pubblica `AuthenticationSuccessEvent` (ultimo accesso aggiornato da `UltimoAccessoListener`).

### Decisioni

- [F-2026-10-23/RICORDAMI.1] Casella "Ricordami su questo dispositivo" sotto il form di accesso, non spuntata di default.
- [F-2026-10-23/RICORDAMI.1] Cookie firmato (`TokenBasedRememberMeServices`) valido 30 giorni; annullato al cambio password e all'uscita; non vale per gli utenti disattivati; nessuna tabella.
- [F-2026-10-23/RICORDAMI.1] Chiave di firma da `app.remember-me.key` (variabile `REMEMBER_ME_KEY`); da impostare in produzione.
- Tecnica: la casella è fuori dal form nativo: imposta un cookie tecnico `ricordami=1` che il server legge al login (`rememberMeRequested`). Senza `REMEMBER_ME_KEY` si usa una chiave casuale a ogni avvio (con un avviso nel log): funziona, ma i cookie non sopravvivono a un riavvio.

## Passi

| Passo | Cosa | Richiesta | File | Verifica | Stato |
|---|---|---|---|---|---|
| 1 | `RememberMeServices` in `SecurityConfig` (cookie firmato 30 giorni, richiesto dal cookie `ricordami`); proprietà e variabile d'ambiente; test sul filtro di login | — | `security/SecurityConfig`, `application*.properties`, `deploy/…`, test | `./mvnw test` | [x] |
| 2 | Casella "Ricordami su questo dispositivo" in `LoginView`; test | [F-2026-10-23/RICORDAMI.1] | `views/LoginView`, `ViewAccessTest` | `./mvnw test` | [x] |
| 3 | CLAUDE.md, README di deploy e registro | — | `CLAUDE.md`, `deploy/README.md`, questo piano | — | [x] |

## Fuori ambito

- Elenco dei dispositivi ricordati e revoca dal gestionale (token in database).

## Registro

- Passo 1: `./mvnw test` 223 test verdi; nuovo `security/RicordamiTest` sul filtro di login reale (MockMvc): senza la casella nessun cookie; con la casella cookie di 30 giorni che fa rientrare; cambio password o utente disattivato: non rientra; uscita (`/logout`): cookie cancellato.
- Passo 2: `./mvnw test` 223 test verdi; `login_inItaliano` verifica la casella non spuntata.
- Non verificato nel browser (casella → cookie → login). L'uscita dall'app passa da `AuthenticationContext.logout()` di Vaadin, che usa gli stessi gestori di uscita di Spring Security verificati sul `/logout`.
- **Produzione**: impostare `REMEMBER_ME_KEY` in `gestione-corsi.env` (es. `openssl rand -base64 32`) e ricaricare con `pm2 reload ecosystem.config.js --update-env`; senza, gli accessi ricordati si perdono a ogni riavvio (che con il watch di pm2 avviene a ogni deploy).
