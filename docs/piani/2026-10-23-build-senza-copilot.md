# Piano: build di produzione senza codice di sviluppo

## Richiesta

Dopo il deploy della 1.1.0 la pagina `/login` in produzione è vuota e non si può accedere.

## Analisi

- Riprodotto in locale con lo stesso JAR (`release/gestione-corsi-1.1.0.jar` delle 16:11): il server risponde 200, ma nel browser il JS si ferma con `Uncaught TypeError: Cannot read properties of undefined (reading 'push')` in `VAADIN/build/frontend-*.js`, su `window.Vaadin.devToolsPlugins.push(...)`.
- Nel JAR c'erano ~40 file `copilot-*` (Vaadin Copilot / dev tools): `src/main/frontend/generated/vaadin.ts` aveva il contenuto della modalità sviluppo (import di `copilot.js` e `vaadin-dev-tools.js`) ed è stato compilato nel bundle di produzione (anche `src/main/bundles/prod.bundle` delle 16:11 li conteneva). In produzione l'oggetto `devToolsPlugins` non esiste, quindi tutto il frontend si blocca.
- Causa più probabile: un processo in modalità sviluppo (dev server o `./mvnw test`, che rigenerano `src/main/frontend/generated`) attivo durante la build. `./mvnw clean` non cancella né `src/main/frontend/generated` né `prod.bundle`.
- Ricostruzione con `prod.bundle` e `src/main/frontend/generated` cancellati: 0 file Copilot nel JAR, `/login` mostra `vaadin-login-form` e la casella "Ricordami", nessun errore in console.
- `-Pproduction` in `build.sh` non esiste più nel `pom.xml` (Maven avvisa): in Vaadin 25 non serve.

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | `build.sh`: cancella `prod.bundle` e `src/main/frontend/generated` prima della build, toglie `-Pproduction`, si ferma con errore se il JAR contiene Copilot | `build.sh` | `./build.sh`, 0 file `copilot` nel JAR | [x] |
| 2 | CLAUDE.md e registro | `CLAUDE.md`, questo piano | — | [ ] |

## Fuori ambito

- Il deploy del JAR corretto: lo avvia l'utente (`/deploy`).

## Registro
