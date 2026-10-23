# gestione-corsi – avvio in produzione

Dal computer di sviluppo:

```bash
bash build.sh     # JAR in release/
bash deploy.sh    # copia il JAR sul server
```

## Server

Requisiti: Java 21, MySQL 8, Node.js con pm2 (`npm install -g pm2`).

Cartella dell'applicazione (es. `~/gestionecorsi`), con i file di questa cartella `deploy/`:

```
~/gestionecorsi/
├── gestione-corsi.jar          JAR dell'applicazione (nome stabile, vedi APP_JAR)
├── ecosystem.config.js         configurazione pm2
├── gestione-corsi.env          variabili d'ambiente e segreti (NON in git, permessi 600)
├── start.sh                    avvio manuale senza pm2, per provare la configurazione
└── logs/                       log di pm2 (creata automaticamente)
```

### Database (una volta sola)

```bash
mysql -u root -p < setup-mysql.sql    # prima cambiare 'your_password' nello script
```

### Primo avvio

```bash
cd ~/gestionecorsi
cp gestione-corsi.env.example gestione-corsi.env
chmod 600 gestione-corsi.env
nano gestione-corsi.env               # almeno DB_PASSWORD; DB_URL/DB_USERNAME/PORT se diversi

./start.sh                            # facoltativo: prova a mano, Ctrl+C per fermare
pm2 start ecosystem.config.js
pm2 save                              # ricorda i processi attivi
pm2 startup                           # stampa il comando (sudo ...) per riavviare pm2 al boot: eseguirlo
```

Variabili di `gestione-corsi.env`:

| Variabile | Obbligatoria | Default | Uso |
|---|---|---|---|
| `DB_PASSWORD` | sì | – | password MySQL |
| `DB_URL` | no | `jdbc:mysql://localhost:3306/gestione_corsi?...` | URL JDBC |
| `DB_USERNAME` | no | `gestione_corsi_user` | utente MySQL |
| `PORT` | no | `8080` | porta HTTP |
| `REMEMBER_ME_KEY` | consigliata | casuale a ogni avvio | chiave di firma dei cookie "Ricordami su questo dispositivo" (`openssl rand -base64 32`); senza, gli accessi ricordati si perdono a ogni riavvio |
| `APP_JAR` | no | `gestione-corsi.jar` | JAR da avviare |
| `JAVA_BIN` | no | `java` | eseguibile Java 21 |
| `JAVA_OPTS` | no | `-Xms256m -Xmx512m ...` | opzioni della JVM |

`DB_URL` deve contenere `allowPublicKeyRetrieval=true` (con `useSSL=false`): MySQL 8 usa `caching_sha2_password` e dopo ogni suo riavvio (anche per gli aggiornamenti automatici notturni) il primo accesso richiede la chiave pubblica del server. Senza, l'app non riesce più ad aprire connessioni (`Public Key Retrieval is not allowed`) finché non viene riavviata.

Il profilo `prod` è fissato in `ecosystem.config.js` e `start.sh`: senza, Spring partirebbe con il profilo
`dev` (H2 in memoria e utenti di prova).

### Email (SMTP)

Conferme di iscrizione e ricevute partono per email solo se in `gestione-corsi.env` è impostata `MAIL_HOST`
(con `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`, `MAIL_STARTTLS`); senza, l'invio è simulato:
registrato nella vista "Email inviate" con esito "Simulata" e scritto nel log. I dati dell'associazione in testata
a ricevute e conferme (e l'IBAN per il bonifico) sono le variabili `ASSOCIAZIONE_*`. Dopo averle cambiate:
`pm2 reload ecosystem.config.js --update-env`.

### Aggiornamento

1. Backup del database (`mysqldump gestione_corsi > backup-AAAA-MM-GG.sql`).
2. Eseguire gli script di `docs/migrazioni/` non ancora applicati, nell'ordine e come indicato in ciascuno
   (alcuni vanno eseguiti **prima** dell'avvio della nuova versione).
3. Copiare il nuovo JAR come `gestione-corsi.jar`. Attenzione: oggi `deploy.sh` lo carica come
   `gestione-corsi-1.0-SNAPSHOT.jar`: rinominarlo, oppure impostare `APP_JAR` con quel nome.
4. `pm2 reload ecosystem.config.js --update-env`

Dopo una modifica di `gestione-corsi.env` serve lo stesso comando del punto 4 (`pm2 restart gestione-corsi`
riusa le variabili lette al primo avvio).

### Comandi utili

```bash
pm2 list                              # stato
pm2 logs gestione-corsi               # log in tempo reale (anche in logs/out.log)
pm2 describe gestione-corsi           # dettagli, riavvii, percorsi dei log
pm2 stop gestione-corsi
pm2 install pm2-logrotate             # rotazione dei log (una volta sola)
```

Nota: `pm2 save` scrive le variabili d'ambiente, password compresa, in `~/.pm2/dump.pm2`: lasciare la
cartella `~/.pm2` leggibile solo dall'utente che esegue pm2.
