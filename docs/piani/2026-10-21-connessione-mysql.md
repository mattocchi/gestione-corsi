# Piano: connessione a MySQL persa dopo il riavvio del database

## Richiesta

Dopo una giornata di non utilizzo l'applicazione in produzione ha problemi di connessione al database.

## Analisi

- Log del server (`~/gestionecorsi/logs/out.log`): dalle 06:13 UTC del 2026-10-21 ogni nuova connessione fallisce con `SQLNonTransientConnectionException: Public Key Retrieval is not allowed` (662 tentativi falliti di Hikari fino alle 20:09).
- Alle 06:11 UTC `unattended-upgrade` ha aggiornato MySQL (8.0.46) e lo ha riavviato (`/var/log/apt/history.log`, `systemctl status mysql`).
- MySQL 8 autentica con `caching_sha2_password`: dopo un riavvio la cache delle password è vuota e il primo accesso di ogni utente richiede l'autenticazione completa; senza TLS (`useSSL=false`) il driver deve chiedere la chiave pubblica RSA al server, cosa vietata senza `allowPublicKeyRetrieval=true`. Le connessioni del pool vengono rinnovate ogni 30 minuti (`maxLifetime` di Hikari), quindi poco dopo il riavvio il pool resta vuoto e l'app non funziona finché non viene riavviata in un momento "fortunato".
- Non dipende dall'inattività ma dai riavvii notturni di MySQL per gli aggiornamenti automatici.

### Decisioni

- Si aggiunge `allowPublicKeyRetrieval=true` all'URL JDBC, mantenendo `useSSL=false` (database su localhost: rischio trascurabile). Scartata per ora la connessione TLS (`sslMode=REQUIRED`).
- Produzione corretta subito (2026-10-21 20:31): `DB_URL` in `~/gestionecorsi/gestione-corsi.env` (backup `gestione-corsi.env.bak-2026-10-21`) e `pm2 reload ecosystem.config.js --update-env`; app ripartita, `/login` risponde 200.

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | `allowPublicKeyRetrieval=true` nell'URL di default e nell'esempio di env; nota nel README di deploy | `application-prod.properties`, `deploy/gestione-corsi.env.example`, `deploy/README.md` | `./mvnw -q compile` | [ ] |
| 2 | Registro | questo piano | — | [ ] |

## Fuori ambito

- Connessione TLS a MySQL (`sslMode=REQUIRED`).
- Escludere MySQL dagli aggiornamenti automatici o riavviare l'app dopo l'aggiornamento: non serve più con questa correzione.

## Registro
