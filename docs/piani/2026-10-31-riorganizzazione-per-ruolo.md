# Riorganizzazione generale per ruolo

## Richiesta

[F-2026-10-31/RIORGANIZZAZIONE.1] Su un branch dedicato, una riorganizzazione generale strutturata e chiara della web
application per chi la usa, ruolo per ruolo.

## Analisi

- Il menu nasce da `@Menu` (`MenuConfiguration.getMenuEntries()`, già filtrato per accesso) in `MainLayout.createSideNav`,
  in una sola lista: admin 16 voci, segreteria 15, senza gruppi; "Documenti" (ordine 42) è lontano da "Cronologia
  documenti" (10.8). Nomi misti ("Gestione Corsi", "Insegnanti", "I Miei Corsi", "I miei allievi").
- Admin e segreteria non hanno "Il mio profilo" (`ProfiloView` è per INSEGNANTE, GENITORE, ALLIEVO). C'è già
  `UserService.aggiornaContatti` / `cambiaPassword`.
- Anni scolastici, Luoghi, Festività sono tre viste (`anni-scolastici`, `luoghi`, `festivita`) usate di rado.
- Vaadin 25.3 `SideNav`: più liste affiancate con `setLabel`, richiudibili con `setCollapsible` (guida Side Navigation,
  "Labeled Collapsible List"). `@RouteAlias` permette di tenere i vecchi indirizzi su una nuova vista.
- Chi ha più ruoli (es. insegnante e genitore, segreteria e genitore) oggi vede le voci mescolate.

### Decisioni

- [F-2026-10-31/RIORGANIZZAZIONE.1] Branch `riorganizzazione-per-ruolo` (da `develop`); i commit restano lì finché
  non si decide di unirlo.
- Menu staff a gruppi con titolo: in alto Home e Calendario; **Corsi e lezioni** (Corsi, Lezioni, Iscritti),
  **Persone** (Allievi, Genitori, Insegnanti, Utenti solo admin), **Economia** (Pagamenti, Rimborsi), **Documenti**
  (Documenti, Cronologia), **Impostazioni** richiudibile e chiuso (una voce dopo il passo 3); in fondo Il mio profilo
  e Guida.
- Insegnante, genitore, allievo con un solo ruolo: lista semplice (Home, Calendario, voci del ruolo, Documenti, Il mio
  profilo, Guida). Con più ruoli: Home, Calendario e Documenti in alto, un gruppo con titolo per ruolo
  ("Staff" con i suoi gruppi, "Insegnante", "Famiglia", "Allievo"), profilo e guida in fondo.
- Nomi brevi e coerenti: Corsi, Allievi, Pagamenti, Utenti, Iscritti, Cronologia; maiuscola solo all'inizio
  (I miei corsi, I miei figli, Le mie lezioni). Gli indirizzi delle pagine non cambiano.
- Il mio profilo anche per admin e segreteria: nome, cognome, email, telefono e password modificabili, username e ruoli
  in sola lettura; se l'utente ha anche una scheda (insegnante, genitore, allievo) resta il profilo di quella scheda.
- Impostazioni: Anni scolastici, Luoghi e Festività in una pagina a schede; i vecchi indirizzi aprono la scheda giusta.
- Guida: una pagina per ruolo, compatta: per ogni voce del menu a cosa serve e le 2-4 operazioni principali, con il
  collegamento alla pagina; con più ruoli una scheda per ruolo.
- Home: sezioni nell'ordine e coi nomi dei gruppi del menu; card cliccabili che aprono la pagina collegata (con il
  filtro giusto dove serve, es. "Da incassare" → Pagamenti sulla scheda Da pagare).

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1.1 | Menu a gruppi per ruolo: `views/MenuApp` (gruppo di ogni voce, regole per ruolo), `MainLayout` con più `SideNav` | `MainLayout`, `MenuApp`, `ViewAccessTest` | `./mvnw test` | [ ] |
| 1.2 | Nomi brevi e coerenti: `@Menu`, `@PageTitle`, titoli e test | viste, test | `./mvnw test` | [ ] |
| 2.1 | Il mio profilo per admin e segreteria | `ProfiloView`, test | `./mvnw test` | [ ] |
| 3.1 | Impostazioni a schede con Anni scolastici, Luoghi, Festività; vecchi indirizzi come alias | `ImpostazioniView`, viste admin, `MenuApp`, test | `./mvnw test` | [ ] |
| 4.1 | Home: sezioni nell'ordine e coi nomi dei gruppi | `HomeView`, test | `./mvnw test` | [ ] |
| 4.2 | Home: card cliccabili verso le pagine (filtri da indirizzo dove servono) | `HomeView`, viste di destinazione, test | `./mvnw test` | [ ] |
| 5.1 | Guida: pagina per ruolo con i testi, voce in fondo al menu | `GuidaView`, risorse, test | `./mvnw test` | [ ] |
| 6.1 | CLAUDE.md: menu, nomi, profilo, impostazioni, guida, home | `CLAUDE.md` | — | [ ] |
| 6.2 | Registro | questo piano | — | [ ] |

## Fuori ambito

- Unione del branch in `develop`/`main` e push: su richiesta.
- Cambi di indirizzo delle pagine, di permessi o di dati.

## Registro
