# Piano: login con username o email

## Richiesta

Un utente deve poter accedere con lo username oppure con l'email ([F-2026-10-21/LOGIN.1]).

## Analisi

- `UserDetailsServiceImpl.loadUserByUsername` cerca solo con `UserRepository.findByUsername`: con l'email l'accesso fallisce come credenziali errate.
- L'email non è univoca (`User.email` ha solo `@Email`) e può stare su più account; il recupero password (`RecuperoPasswordService.richiedi`) già cerca prima lo username e poi `findByEmailIgnoreCase`.
- Il nome dell'utente autenticato deve restare lo username reale: tutta l'app usa `SecurityService.getUsername()`. Basta che lo `UserDetails` restituito abbia `user.getUsername()` (già così).
- `LoginView.italiano()` ha l'etichetta "Nome utente" e i messaggi d'errore riferiti al solo nome utente.

### Decisioni

- [F-2026-10-21/LOGIN.1] Si cerca prima lo username (esatto, come oggi); se non esiste, l'email senza distinguere maiuscole, considerando solo gli account attivi: si entra se ce n'è esattamente uno, altrimenti l'accesso fallisce e serve lo username.
- [F-2026-10-21/LOGIN.1] Email su più account: stesso messaggio generico delle credenziali errate, con un testo che suggerisce di usare il nome utente se l'email è condivisa (non rivela se l'email esiste).
- [F-2026-10-21/LOGIN.1] Lo username resta com'è (nessun cambio su maiuscole/minuscole).
- [F-2026-10-21/LOGIN.1] Etichetta del campo "Nome utente o email", messaggi d'errore aggiornati.

## Passi

| Passo | Cosa | Richiesta | File | Verifica | Stato |
|---|---|---|---|---|---|
| 1 | Ricerca per email (solo attivi, una sola) in `UserDetailsServiceImpl` e `UserRepository`; testi del login; test | [F-2026-10-21/LOGIN.1] | `security/UserDetailsServiceImpl`, `repository/UserRepository`, `views/LoginView`, test | `./mvnw test` | [x] |
| 2 | CLAUDE.md e registro | — | `CLAUDE.md`, questo piano | — | [x] |

## Fuori ambito

- Email univoca per account (vincolo e migrazione dei duplicati).
- Username senza distinzione di maiuscole.

## Registro

- Passo 1: `./mvnw test` 189 test, tutti verdi. Nuovo `security/LoginTest` che usa lo stesso `AuthenticationManager` del form: accesso con username, con email (anche maiuscole e spazi), password errata ed email sconosciuta rifiutate, email condivisa rifiutata finché l'altro account è attivo; il nome autenticato è sempre lo username.
- Non provato nel browser: il form invia gli stessi dati allo stesso `AuthenticationManager`.
- Restano aperti: email univoca per account e username senza distinzione di maiuscole (vedi Fuori ambito).
