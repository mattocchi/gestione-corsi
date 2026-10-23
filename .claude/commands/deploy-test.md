---
description: Copia il JAR di release/ sul server di TEST con ./deploy-test.sh
disable-model-invocation: true
allowed-tools: Bash(./deploy-test.sh), Bash(ls -l release/*)
---

Pubblica la build sull'ambiente di **test** (`~/gestionecorsi/gestione-corsi-test.jar` su cloud.attocchi.it).

1. Controlla con `ls -l release/*` che il JAR esista e mostrane data e dimensione. Se manca, fermati e suggerisci `/build`.
2. Esegui `./deploy-test.sh` dalla root del progetto (timeout 10 minuti).
3. Riporta l'esito dell'rsync (byte trasferiti o errore). Sul server pm2 (`watch`) riavvia l'app da solo quando cambia il JAR; se non ripartisse: `pm2 restart test-gestionecorsi`.
