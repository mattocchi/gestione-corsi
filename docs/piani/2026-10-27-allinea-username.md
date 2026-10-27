# Piano: allineare gli username generati a nome e cognome

## Richiesta

[F-2026-10-27/USERNAME.1] Gli utenti non possono cambiare username: una procedura che allinei a nome e cognome gli username di default non più attuali dopo il cambio di nome e cognome.

## Analisi

- Gli username generati vengono da `UserService.usernameLibero(nome, cognome)`: "nome.cognome" semplificati (minuscolo, senza accenti né spazi), con un numero se già usato ("mario.rossi2").
- Lo username è la chiave di accesso e il nome del principal: `SecurityService.getUsername()` alimenta i service. Riferimenti per stringa solo storici (`InvioEmail.inviatoDa`, lasciato com'è). Il cookie "Ricordami" è firmato con lo username: dopo il cambio non vale più (si rientra).
- Una sessione aperta con il vecchio username non trova più l'utente: va chiusa. Spring Security non tiene un registro delle sessioni (`SessionRegistry` da aggiungere).

### Decisioni

- [F-2026-10-27/USERNAME.1] Si allineano solo gli username nel formato generato ("parola.parola", con eventuale numero) che non corrispondono più a nome e cognome attuali; quelli scelti a mano (admin, genitore1, allievo_canto1…) non si toccano.
- [F-2026-10-27/USERNAME.1] Solo l'admin, in Gestione Utenti: pulsante "Allinea username" con anteprima vecchio → nuovo e una casella per riga (tutte spuntate), poi conferma; per il singolo utente anche un pulsante nel suo dialog di modifica.
- [F-2026-10-27/USERNAME.1] Nessuna email all'utente (si entra anche con l'email).
- [F-2026-10-27/USERNAME.1] Vale anche per l'utente di chi esegue: se cambia, viene disconnesso e rientra; le sessioni aperte degli utenti allineati si chiudono.

## Passi

| Passo | Cosa | File | Richiesta | Verifica | Stato |
|---|---|---|---|---|---|
| 1 | `UserService.usernameDaAllineare()` (proposte) e `allineaUsername(ids, admin)` (solo admin, nuovo username libero); test | `UserService`, test | — | `./mvnw test` | [x] |
| 2 | Registro delle sessioni (`SessionRegistry`) e chiusura delle sessioni degli utenti allineati | `SecurityConfig`, `UserService` o componente security | — | `./mvnw test` | [x] Componente `security/SessioniUtenti`; anche l'accesso diretto dall'invito registra la sessione |
| 3 | Gestione Utenti: "Allinea username" con anteprima e conferma; pulsante nel dialog dell'utente; logout se cambia il proprio; test | `UtentiView`, `ViewAccessTest` | USERNAME.1 | `./mvnw test`; browser | [ ] |
| 4 | `CLAUDE.md` e registro | `CLAUDE.md`, questo piano | — | test | [ ] |

## Fuori ambito

- Cambio dello username a mano o da parte dell'utente.
- Aggiornamento dei riferimenti storici (`InvioEmail.inviatoDa`).

## Registro

