---
description: Build del JAR di produzione con ./build.sh (copiato in release/)
argument-hint: "[pulito]"
allowed-tools: Bash(./build.sh), Bash(rm -f src/main/bundles/prod.bundle), Bash(ls -l release/*)
---

Esegui la build di produzione del progetto.

1. Se l'argomento è `pulito` ($ARGUMENTS), cancella prima `src/main/bundles/prod.bundle` con `rm -f src/main/bundles/prod.bundle` (serve quando una route usa un componente Vaadin che il bundle non caricava, vedi CLAUDE.md).
2. Esegui `./build.sh` dalla root del progetto (timeout 10 minuti).
3. Se fallisce, riporta l'errore Maven rilevante e fermati.
4. Se riesce, mostra il JAR prodotto con `ls -l release/*` (nome, dimensione, data) e ricorda che si pubblica con `/deploy-test` o `/deploy`.
