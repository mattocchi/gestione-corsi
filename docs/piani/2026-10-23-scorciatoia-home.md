# Piano: scorciatoia nella schermata Home del telefono

## Richiesta

[F-2026-10-23/SCORCIATOIA.1] Aggiungere il supporto per creare, quando l'utente usa la web app dal telefono, una scorciatoia (icona) nella schermata Home.

## Analisi

- Vaadin 25.3 supporta le PWA con l'annotazione `@PWA` sulla classe `AppShellConfigurator` (`Application`): genera `manifest.webmanifest`, le icone nelle varie misure a partire da `icons/icon.png` (almeno 512×512), il service worker e i tag per iOS nella `<head>` (doc `v25.3/flow/configuration/pwa.md`, API `com.vaadin.flow.server.PWA`: `name`, `shortName` ≤ 12 caratteri, `description`, `themeColor`, `backgroundColor`, `offlinePath`, `offlineResources`).
- In `META-INF/resources` ci sono già `icons/icon.png` (icona generica di un template) e `offline.html` + `images/offline.png` del template "Vaadin CRM", mai usati.
- Il prompt di installazione si ottiene in modo diverso:
  - Android (Chrome, Edge, Samsung Internet): evento `beforeinstallprompt`, che arriva presto, anche prima che la vista Vaadin sia pronta; va quindi intercettato da uno script caricato nella `<head>` (`AppShellSettings.addInlineFromFile` in `Application.configurePage`) e conservato per il clic sul pulsante. `prompt()` richiede un gesto dell'utente: va chiamato nel listener JavaScript del clic, non dopo un giro al server.
  - iPhone/iPad: nessun prompt programmabile; si mostrano le istruzioni (Condividi → "Aggiungi alla schermata Home").
  - App già aperta dalla scorciatoia: `display-mode: standalone` (o `navigator.standalone` su iOS) → niente invito.
- Le risorse PWA (manifest, service worker, icone, pagina offline) sono già pubbliche per `VaadinSecurityConfigurer`: da verificare con un test senza login.
- Su desktop l'invito non compare: la detezione è lato browser (sistema operativo mobile dallo user agent / `userAgentData.mobile`, più iPad che si presenta come Mac con touch).

### Decisioni

- [F-2026-10-23/SCORCIATOIA.1] Invito con un banner su smartphone più una voce "Aggiungi alla Home" nel menu laterale; su Android il pulsante apre l'installazione nativa, su iPhone (o se il browser non la offre) mostra le istruzioni.
- [F-2026-10-23/SCORCIATOIA.1] Per tutti i ruoli dopo l'accesso e anche sulla pagina di accesso (lì solo il banner, non c'è menu).
- [F-2026-10-23/SCORCIATOIA.1] Icona quadrata 512×512 ricavata dalla "M" rossa del logo Mosson su fondo bianco (`icons/icon.png`, sostituibile).
- [F-2026-10-23/SCORCIATOIA.1] Nome sotto l'icona "Corsi Mosson"; nome completo "Area Corsi Mosson".
- [F-2026-10-23/SCORCIATOIA.1] Pagina offline in italiano con il logo, al posto del vecchio `offline.html` del template; l'app non funziona offline.
- [F-2026-10-23/SCORCIATOIA.1] Il banner chiuso non ricompare su quel dispositivo (memoria del browser); resta la voce di menu. Mai mostrato se l'app è già aperta dalla scorciatoia.

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | PWA: `@PWA` su `Application` (nomi, descrizione, colori, pagina offline), icona 512×512 dalla "M" del logo, `offline.html` in italiano, rimozione di `images/offline.png` del template; verifica che manifest, icone e pagina offline siano accessibili senza login | `Application.java`, `MainLayout.java` (`NOME_BREVE`), `META-INF/resources/icons/icon.png`, `META-INF/resources/offline.html`, `images/offline.png` | `./mvnw test`; `curl` senza login sull'app avviata | [x] Niente test MockMvc: le risorse le serve la servlet di Vaadin, che MockMvc non esegue. Verificato con `curl`: tutte 200, manifest con "Corsi Mosson" e le icone generate |
| 2 | Script globale nella `<head>` (cattura `beforeinstallprompt`, piattaforma, app già installata) e componente `views/ScorciatoiaHome`: banner chiudibile e voce di menu, dialog con le istruzioni per iPhone/Android; nel layout principale (sotto l'intestazione, voce sopra il logo del drawer) e nella pagina di accesso | `src/main/resources/scorciatoia-home.js`, `Application.java`, `views/ScorciatoiaHome.java`, `MainLayout.java`, `LoginView.java` | `./mvnw test`; browser a 390px con user agent mobile | [x] Nel browser (Chrome desktop, l'estensione non emula lo smartphone né fa screenshot): su computer banner e voce nascosti e intestazione invariata; Chrome emette `beforeinstallprompt` (app installabile); mostrato a mano, il banner sta in 358px con i due pulsanti; "Aggiungi" senza invito del browser apre le istruzioni; "Chiudi" salva la scelta nel browser |
| 3 | Documentazione in `CLAUDE.md` | `CLAUDE.md` | rilettura | [x] |
| 4 | Registro finale | questo piano | build e test | [ ] |

## Fuori ambito

- Funzionamento offline dell'app (le viste Flow richiedono il server): offline c'è solo la pagina di avviso.
- Notifiche push.
- Schermate di avvio personalizzate per iOS (si usano quelle generate da Vaadin dall'icona).

## Registro

_(da compilare durante l'esecuzione)_
