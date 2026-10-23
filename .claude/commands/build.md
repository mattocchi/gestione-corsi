---
description: Build del JAR di produzione con ./build.sh (copiato in release/)
allowed-tools: Bash(./build.sh), Bash(ls -l release/*)
---

Esegui la build di produzione del progetto.

1. Assicurati che non ci siano dev server o test in esecuzione sul progetto (riscriverebbero i file frontend generati). `build.sh` cancella da sé `prod.bundle` e `src/main/frontend/generated`.
2. Esegui `./build.sh` dalla root del progetto (timeout 10 minuti).
3. Se fallisce, riporta l'errore (Maven, oppure "il JAR contiene Copilot") e fermati.
4. Se riesce, mostra il JAR prodotto con `ls -l release/*` (nome, dimensione, data) e ricorda che si pubblica con `/deploy-test` o `/deploy`.
