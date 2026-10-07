# Piano: icone delle azioni nelle griglie

## Richiesta

Sostituire le icone delle azioni nelle griglie con icone più riconoscibili, restando sulla libreria standard **Vaadin Icons** (scelta confermata dall'utente).

## Analisi

Le icone delle azioni sono create con `ViewSupport.iconButton(VaadinIcon, etichetta, ...)`; l'etichetta resta come tooltip e nome accessibile, cambia solo l'icona. Set scelto dall'utente:

| Azione | Attuale | Nuova | Dove |
|---|---|---|---|
| Modifica | `EDIT` | `PENCIL` | Corsi, Studenti, Utenti |
| Elimina | `TRASH` | `TRASH` (invariata) | Corsi, Studenti, Utenti |
| Genera Lezioni | `CALENDAR_CLOCK` | `REFRESH` | Corsi |
| Presenze | `CHECK_SQUARE_O` | `CLIPBOARD_CHECK` | Le Mie Lezioni, I Miei Corsi, I Miei Figli (dialog corsi) |
| Dettagli figlio | `EYE` | `BOOK` (come la voce di menu "I Miei Corsi") | I Miei Figli |
| Scarica Riepilogo | `DOWNLOAD` | `DOWNLOAD` (invariata) | I Miei Figli (dialog corsi) |

Tutte le nuove costanti esistono in `VaadinIcon` di Vaadin 25.3 (verificato con il server MCP Vaadin).

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | Modifica → `PENCIL` | `CorsiView`, `StudentiView`, `UtentiView` | `./mvnw test` | [x] |
| 2 | Genera Lezioni → `REFRESH` | `CorsiView` | `./mvnw -q compile` | [x] |
| 3 | Presenze → `CLIPBOARD_CHECK` | `LezioniDocenteView`, `MieiCorsiView`, `FigliView` | `./mvnw test` | [x] |
| 4 | Dettagli figlio → `BOOK` | `FigliView` | `./mvnw test` | [x] |
| 5 | Convenzione icone delle azioni nel `CLAUDE.md` | `CLAUDE.md` | — | [x] |
| 6 | *(emerso nella verifica)* Colonne data con larghezza automatica: il nuovo formato "mer 2 ottobre 2024" viene troncato | `CorsiView`, `StudentiView`, `FigliView`, `MieiCorsiView` | `./mvnw test` + browser | [x] |
| 7 | Verifica finale: build con test, controllo visivo nel browser, registro | piano | build + browser | [x] |

## Fuori ambito

- Icone del menu laterale: invariate.
- Nessun cambio di libreria: si resta su Vaadin Icons.

## Registro

- Passi 1–5: icone sostituite e convenzione documentata in `CLAUDE.md`, un commit per passo.
- Passo 6 emerso durante la verifica visiva: a 900px la colonna "Data Inizio" troncava il nuovo formato data ("mer 2 ottobre ..."), effetto del piano `2026-10-07-formato-date`. Corretto con larghezza automatica sulle colonne data.
- Build `./mvnw clean package`: BUILD SUCCESS, 14/14 test verdi.
- Browser: icone verificate su tutte le viste e i dialog (Modifica=pencil, Genera Lezioni=refresh, Elimina=trash, Presenze=clipboard-check, Dettagli=book, Scarica=download); a 900px e 390px nessuna data troncata e nessuna griglia che sborda in Corsi, Studenti, I Miei Corsi, I Miei Figli.
