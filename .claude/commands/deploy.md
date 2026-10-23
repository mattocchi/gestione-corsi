---
description: Copia il JAR di release/ sul server di PRODUZIONE con ./deploy.sh
argument-hint: "[si]"
disable-model-invocation: true
allowed-tools: Bash(./deploy.sh), Bash(ls -l release/*)
---

Pubblica la build in **produzione** (`~/gestionecorsi/gestione-corsi.jar` su cloud.attocchi.it).

1. Controlla con `ls -l release/*` che il JAR esista e mostrane data e dimensione. Se manca, fermati e suggerisci `/build`.
2. Se l'argomento non è `si` ($ARGUMENTS), chiedi conferma esplicita all'utente prima di procedere (è la produzione); ricorda di verificare se servono script SQL in `docs/migrazioni/` da eseguire su MySQL prima del deploy.
3. Esegui `./deploy.sh` dalla root del progetto (timeout 10 minuti).
4. Riporta l'esito dell'rsync (byte trasferiti o errore). Sul server pm2 (`watch`) riavvia l'app da solo quando cambia il JAR; se non ripartisse: `pm2 reload ecosystem.config.js --update-env`.
