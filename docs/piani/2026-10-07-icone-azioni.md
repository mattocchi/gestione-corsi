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
| 6 | Verifica finale: build con test, controllo visivo nel browser, registro | piano | build + browser | [ ] |

## Fuori ambito

- Icone del menu laterale: invariate.
- Nessun cambio di libreria: si resta su Vaadin Icons.

## Registro

_(compilato durante l'esecuzione)_
