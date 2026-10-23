# Piano: comandi Claude per build e deploy

## Richiesta

Aggiungere comandi Claude Code (slash command di progetto) per eseguire `build.sh`, `deploy.sh` e `deploy-test.sh`.

## Analisi

- `build.sh`: `./mvnw clean package -Pproduction -DskipTests`, copia `target/gestione-corsi-1.1.0.jar` in `release/`, poi `./mvnw clean`.
- `deploy.sh` / `deploy-test.sh`: rsync (ssh porta 2022) di `release/gestione-corsi-1.1.0.jar` su `cloud@cloud.attocchi.it:~/gestionecorsi/` come `gestione-corsi.jar` (produzione) o `gestione-corsi-test.jar` (test). Non riavviano il servizio (lo fa `deploy/start.sh` / `start-test.sh` sul server).
- I comandi di progetto stanno in `.claude/commands/<nome>.md` (frontmatter `description`, `allowed-tools`, `disable-model-invocation`); la cartella `.claude` non è ignorata da git.
- CLAUDE.md: il `prod.bundle` riutilizzato può lasciare componenti non definiti; il comando di build lo ricorda.

### Decisioni

- Tre comandi: `/build`, `/deploy-test`, `/deploy`. I deploy hanno `disable-model-invocation: true` (solo l'utente li avvia) e controllano che il JAR in `release/` esista; `/deploy` (produzione) chiede conferma prima dell'rsync, salvo argomento `si`.
- `/build` accetta l'argomento `pulito` per cancellare prima `src/main/bundles/prod.bundle`.

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | Comandi `/build`, `/deploy-test`, `/deploy` | `.claude/commands/*.md` | lettura dei file | [x] |
| 2 | CLAUDE.md (sezione Commands) e registro | `CLAUDE.md`, questo piano | — | [x] |

## Fuori ambito

- Riavvio remoto esplicito: non serve, pm2 ha `watch: true` e riavvia l'app quando cambia il JAR (i comandi indicano il comando pm2 se non ripartisse).
- Modifiche agli script stessi (es. versione `1.1.0` scritta a mano).

## Registro

- Creati `.claude/commands/build.md`, `deploy-test.md`, `deploy.md`; nessuno eseguito (la build e i deploy li avvia l'utente).
- Verificato in `deploy/ecosystem.config.js` e `deploy/start-test.sh` che pm2 usa `watch`: il riavvio al cambio del JAR è automatico.
- I comandi compaiono in Claude Code dalla prossima sessione (o dopo il ricaricamento dei comandi).
