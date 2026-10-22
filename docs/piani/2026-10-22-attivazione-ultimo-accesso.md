# Piano: attivazione con il recupero password e ultimo accesso

## Richiesta

Un utente disattivato non deve poter entrare, ma se chiede il recupero password e imposta una nuova password si attiva da solo (utenti nuovi creati bloccati con password casuale) ([F-2026-10-22/ACCESSO.1]); registrare l'ultimo accesso di ogni utente ([F-2026-10-22/ACCESSO.2]).

## Analisi

- Il login rifiuta già gli utenti disattivati: `UserDetailsServiceImpl` passa `user.isEnabled()` allo `UserDetails` di Spring Security (`DisabledException` → errore generico di accesso).
- `RecuperoPasswordService` oggi ignora gli account disattivati: `richiedi` non invia il link, `valido` non accetta un link di un account disattivato; `reimposta` cambia solo la password.
- Nessun dato sull'ultimo accesso. Spring Security pubblica `AuthenticationSuccessEvent` a ogni autenticazione riuscita dell'`AuthenticationManager` usato dal form di login (lo stesso di `security/LoginTest`).

### Decisioni

- [F-2026-10-22/ACCESSO.1] Possono riattivarsi da sole con il recupero password solo gli utenti disattivati che non hanno mai fatto un accesso (`ultimoAccesso` vuoto) e hanno un'email: impostata la nuova password diventano attivi. Chi è stato disattivato dopo aver usato l'app resta bloccato. Gli utenti già esistenti all'inizio non hanno l'ultimo accesso e contano come "mai entrati".
- [F-2026-10-22/ACCESSO.2] Nuovo `User.ultimoAccesso` (colonna `ultimo_accesso`, data e ora), aggiornato a ogni accesso riuscito; non si registrano i tentativi falliti.
- [F-2026-10-22/ACCESSO.2] Gestione Utenti: colonna "Ultimo accesso" (es. "mer 22 ottobre 14:30", "Mai" se vuoto), ordinabile, anche nella card mobile.

## Passi

| Passo | Cosa | Richiesta | File | Verifica | Stato |
|---|---|---|---|---|---|
| 1 | `User.ultimoAccesso`, listener di `AuthenticationSuccessEvent`, colonna in Gestione Utenti; test | [F-2026-10-22/ACCESSO.2] | `entity/User`, `service/UserService`, `security/UltimoAccessoListener`, `views/admin/UtentiView`, test | `./mvnw test` | [x] |
| 2 | Recupero password per i disattivati mai entrati e attivazione alla nuova password; test | [F-2026-10-22/ACCESSO.1] | `service/RecuperoPasswordService`, `RecuperoPasswordServiceTest` | `./mvnw test` | [x] |
| 3 | CLAUDE.md e registro | — | `CLAUDE.md`, questo piano | — | [ ] |

## Fuori ambito

- Registro dei tentativi di accesso falliti; ultimo accesso negli elenchi delle persone.

## Registro
