# Piano di correzione architettura — gestione-corsi

Riferimento: revisione dell'architettura del 2026-10-07 rispetto alla documentazione ufficiale
di **Vaadin 25.1** (server MCP Vaadin) con **Spring Boot 4.0.5** e **Java 21**.

Legenda stato: `[ ]` da fare · `[x]` fatto · `[~]` fatto parzialmente / rimandato (vedi nota)

Ogni fase termina con una verifica (`./mvnw compile` / `./mvnw test`) prima di passare alla successiva.

---

## Fase 1 — Problemi critici

| # | Problema | Correzione | File | Stato |
|---|----------|------------|------|-------|
| 1.1 | `SecurityService.getAuthenticatedUser()` usa `.get()` → `NoSuchElementException` con utente non autenticato; logica ruoli duplicata | Restituire `Optional`, delegare `hasRole` / `getUsername` ad `AuthenticationContext` (che offre già `hasRole`, `getPrincipalName`) | `security/SecurityService.java`, `views/HomeView.java` | [x] |
| 1.2 | Nessuna separazione dev/prod: avviato senza profilo il JAR usa **H2 in memoria `create-drop`** (dati persi a ogni riavvio) | Profili Spring: `application.properties` (comune), `application-dev.properties` (H2), `application-prod.properties` (MySQL via variabili d'ambiente). `spring.profiles.default=dev`. Driver MySQL nel pom. `start.sh` con `--spring.profiles.active=prod` | `application*.properties`, `pom.xml`, `deploy/start.sh` | [x] |
| 1.3 | `DataInitializer` crea `admin/admin` e utenti con password banali anche in produzione | `@Profile("dev")` | `DataInitializer.java` | [x] |
| 1.4 | `Dockerfile` usa Java 17, il progetto richiede Java 21 | `eclipse-temurin:21-jre` | `Dockerfile` | [x] |
| 1.5 | `CorsiView`: `Binder` come campo condiviso → i binding si accumulano ad ogni apertura del dialog | `Binder` locale a `openDialog` (come nelle altre view) | `views/admin/CorsiView.java` | [x] |

## Fase 2 — Allineamento configurazione a Vaadin 25

| # | Problema | Correzione | Stato |
|---|----------|------------|-------|
| 2.1 | Tema incoerente: `styles.css` importa **Aura** ma il codice usa `LumoUtility` e `ButtonVariant.LUMO_*` (non funzionano con Aura) | Tema Aura dichiarato con `@StyleSheet(Aura.STYLESHEET)` prima di `styles.css`; rimosso `@import` da CSS; varianti senza prefisso (`PRIMARY`, `ERROR`, `SUCCESS`); niente `LumoUtility`; rimossi stili residui del template CRM | [x] |
| 2.2 | `vaadin.whitelisted-packages` deprecato (+ `dev.hilla` inutile) | `vaadin.allowed-packages=com.vaadin,org.vaadin,it.attocchi` | [x] |
| 2.3 | `spring.mustache.check-template-location` inutile | Rimosso | [x] |
| 2.4 | Repository `vaadin-prereleases` con versione stabile; `line-awesome` non usato | Rimossi da `pom.xml` e il relativo `permitAll` da `SecurityConfig` | [x] |
| 2.5 | `-Pproduction` in `build.sh`/`CLAUDE.md`: il profilo non esiste (in Vaadin 25 non serve, `vaadin-dev` è `optional`) | `./mvnw clean package` | [x] |
| 2.6 | H2 console attiva sempre | Solo nel profilo `dev` | [x] |

## Fase 3 — UI secondo le API Vaadin 25.1

| # | Problema | Correzione | Stato |
|---|----------|------------|-------|
| 3.1 | Menu a mano con `RouterLink` + `if (hasRole…)` | `SideNav` + `@Menu` sulle view + `MenuConfiguration.getMenuEntries()` (già filtrato per controllo accessi) | [x] |
| 3.2 | Password in `TextField` | `PasswordField` (Utenti, Studenti) | [x] |
| 3.3 | `StreamResource` deprecato (forRemoval) in `FigliView` | `DownloadHandler` (lambda con `DownloadEvent`) | [x] |

## Fase 4 — Service layer e persistenza

| # | Problema | Correzione | Stato |
|---|----------|------------|-------|
| 4.1 | Le view usano direttamente i repository (`DocenteRepository`, `UserRepository`, `GenitoreRepository`, `StudenteRepository`) | Nuovi `DocenteService`, `GenitoreService`; le view dipendono solo dai service | [x] |
| 4.2 | Lookup "utente corrente → Docente/Studente/Genitore" duplicato in 3 view | Metodi `findByUsername` nei service + `SecurityService.getUsername()` | [x] |
| 4.3 | Collezioni lazy lette nella UI (dipendenza da open-in-view) | Query con `JOIN FETCH` per i dati mostrati; `@Transactional(readOnly = true)` sui service; `spring.jpa.open-in-view=false` | [x] |
| 4.4 | `UserService.save` decide se cifrare la password guardando il prefisso `$2a$` | Password cifrata solo tramite metodo esplicito (`save(user, rawPassword)`) | [x] |
| 4.5 | Entity senza `equals`/`hashCode` (usate in `Set` e `ComboBox`) | `equals`/`hashCode` basati sull'id | [x] |
| 4.6 | `System.out.println` in `DataInitializer` | Logger SLF4J | [x] |

## Fase 5 — Test

| # | Correzione | Stato |
|---|------------|-------|
| 5.1 | Test unitario Spring di `CorsoService.generaLezioni` (numero lezioni, giorni, presenze) | [x] |
| 5.2 | Test browserless (`SpringBrowserlessTest`) di accesso alle view per ruolo e rendering delle griglie (intercetta anche errori lazy-loading) | [x] |

## Fase 6 — Documentazione

| # | Correzione | Stato |
|---|------------|-------|
| 6.1 | `CLAUDE.md`: comandi build, profili, service, generazione lezioni (è manuale, non al salvataggio) | [x] |

## Fase 7 — Design responsive per mobile

Verifica fatta nel browser con viewport di 390px (smartphone), app avviata con i dati di test.

| Vista / sezione | Problema misurato | Correzione | Stato |
|---|---|---|---|
| Login, Home, header | OK a 390px; titolo H1 della Home molto grande, titolo dell'header senza troncamento | Titoli con dimensione fluida (`clamp`) e titolo header con ellissi | [x] |
| Gestione Corsi | Griglia 700px in 356px: nomi troncati, i 3 pulsanti di azione fuori schermo | Colonne secondarie nascoste sotto 640px; azioni come icone con tooltip; griglia che occupa l'altezza disponibile | [x] |
| Gestione Studenti | Griglia 600px in 356px | Come sopra | [x] |
| Gestione Utenti | Griglia 600px in 356px | Come sopra | [x] |
| Le Mie Lezioni (docente) | 6 colonne con pulsante testuale; dialog presenze da 800px con ComboBox + TextArea da 200px affiancati e colonne duplicate | Colonne secondarie nascoste; dialog presenze con colonna unica "Presenza" (stato e note impilati, larghezza piena) | [x] |
| I Miei Figli / I Miei Corsi | Dialog da 1000px/800px con griglie a 4-5 colonne e pulsanti testuali; dialog presenze duplicato nelle due viste | Colonne secondarie nascoste, azioni a icona, nota mostrata come tooltip su mobile, statistiche che vanno a capo; dialog presenze unico condiviso | [x] |
| Tutti i dialog di modifica | Senza titolo; Salva/Annulla in fondo al contenuto (su mobile vanno raggiunti scorrendo tutto il form) | `setHeaderTitle` + pulsanti nel footer del Dialog, che resta visibile; larghezza massima con limite della viewport | [x] |

**Esito (verificato nel browser a 390px e a 1280px):** nessuna griglia o pagina sborda più in orizzontale (tutte 356px su 356px a 390px); su desktop le griglie mostrano ancora tutte le colonne. Dialog di modifica con titolo e footer sempre visibile; dialog presenze del docente utilizzabile a 296px; titolo della Home a 25,75px invece di andare su tre righe.

Approccio: helper `views/ViewSupport` basato su `Page.windowSizeSignal()` (Vaadin 25.3) e `Signal.effect`, breakpoint unico a 640px. Le utility responsive di Lumo non si usano perché il tema è Aura. Nuovo `views/PresenzeStudenteDialog` condiviso da genitore e studente (prima il codice era duplicato).

## Fuori ambito (decisioni rimandate)

- **Migrazioni DB (Flyway)** al posto di `ddl-auto=update` in produzione: richiede lo schema MySQL di partenza, da fare quando il DB di produzione è stabilizzato.
- **Packaging per feature** (`corsi/`, `studenti/`…) suggerito da Vaadin 25: utile ma ad alto churn, non necessario alle dimensioni attuali.
- **DTO** al posto delle entity nella UI: valutare se il dominio cresce.

---

## Registro esecuzione (2026-10-07)

**Fase 1** — `SecurityService` delega ad `AuthenticationContext` e restituisce `Optional`. Creati `application-dev.properties`/`application-prod.properties` (MySQL via `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`), aggiunto `mysql-connector-j`, `start.sh` e `Dockerfile` avviano con `--spring.profiles.active=prod`. `DataInitializer` con `@Profile("dev")` e logger (anticipa 4.6). `Binder` locale in `CorsiView`.

**Fase 2** — `@StyleSheet(Aura.STYLESHEET)` + `styles.css` ripulito; `ButtonVariant.PRIMARY/ERROR/SUCCESS`; nessun `LumoUtility`. `vaadin.allowed-packages`. Rimossi repo prerelease, `line-awesome` e relativo permit. `build.sh` usa `./mvnw clean package`.

**Fase 3** — `MainLayout` con `SideNav` + `MenuConfiguration`; `@Menu` (titolo, ordine, icona) su tutte le view. `PasswordField`. `DownloadHandler` in `FigliView`.

**Fase 4** — Nuovi `DocenteService`, `GenitoreService`; lookup utente corrente via `findByUsername` (query `findByUserUsername`); nessuna view usa repository. Service `@Transactional(readOnly = true)`. Fetch esplicito di `giorniSettimana` (Corsi) e `corsi` (figli del genitore); lezioni del docente con una query unica invece di N+1. `generaLezioni` ricarica il corso nella transazione. `spring.jpa.open-in-view=false`. `UserService.save(user, rawPassword)`. `equals`/`hashCode` sull'id in tutte le entity.

**Fase 5** — `CorsoServiceTest` (2 test) e `ViewAccessTest` browserless (9 test: redirect anonimo, menu filtrato, griglie e dialog per ogni ruolo, accesso negato). Contro-prova: togliendo il `JOIN FETCH` dei figli il test fallisce con `LazyInitializationException`.

**Verifica finale** — `./mvnw clean package`: BUILD SUCCESS, 11/11 test verdi; il JAR non contiene `vaadin-dev` e include il bundle frontend di produzione; avvio del JAR ok (`/login` → 200, `/` → 302 verso login).

### Problemi preesistenti emersi (non corretti, da decidere)

- **Eliminazioni con vincoli FK**: eliminare uno studente con presenze, o un utente collegato a docente/studente/genitore, fallisce con errore del DB (la UI mostra solo una notifica generica). Serve decidere: disattivazione logica o cancellazione a cascata.
- **`generaLezioni` con lezioni completate**: le lezioni completate vengono mantenute ma la numerazione ripartisce da 1 e vengono rigenerate tutte le N lezioni, creando duplicati di data/numero.
- **Nessuna conferma** prima di "Elimina" (consigliato `ConfirmDialog`).

## Aggiornamento versioni (2026-10-07)

- **Vaadin 25.1.2 → 25.3.1** (ultima stabile su Maven Central) e **Spring Boot 4.0.5 → 4.1.1**: Vaadin 25.3 richiede Spring Boot ≥ 4.1, e Flow 25.3.1 è compilato proprio su Spring Boot 4.1.1.
- Test: `browserless-test-junit6` sostituito da `browserless-test-spring`, che in 25.3 è la dipendenza documentata per `SpringBrowserlessTest` e include junit6. Nei test `$()` (deprecato) è diventato `find()`.
- Il plugin Vaadin ha rigenerato `package.json`, `package-lock.json` e `tsconfig.json` e ha creato `vite.config.ts`: vanno committati.
- Verifica: nessun avviso di deprecazione in compilazione, 11/11 test verdi, `./mvnw clean package` ok (ripetuto due volte), JAR avviato in production mode senza WARN.
- Nota: la prima build subito dopo il cambio di versione è fallita una volta in Vite (plugin mancanti in `target/plugins`), mentre il frontend veniva rigenerato; le build pulite successive sono passate. Se si ripresenta, rilanciare la build.
