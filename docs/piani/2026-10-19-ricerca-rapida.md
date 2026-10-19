# Piano: ricerca rapida negli elenchi

## Richiesta

Per admin e segreteria, una ricerca rapida negli elenchi di utenti, insegnanti, genitori e allievi: si scrive un testo e si cerca nei campi dell'oggetto dell'elenco ([F-2026-10-19/RICERCA.1]).

## Analisi

- Gli elenchi (`UtentiView`, `InsegnantiView`, `GenitoriView`, `AllieviView`) caricano tutto in memoria con `grid.setItems(...)`, che restituisce un `GridListDataView` filtrabile (`setFilter`); `CorsiView` filtra già così stato e anno. Ogni `setItems` (dopo un salvataggio) crea un nuovo data view: il filtro va riapplicato.
- I dati collegati da cercare sono già caricati dalle query degli elenchi (figli del genitore, corsi di allievi e insegnanti, genitore dell'allievo, ruoli dell'utente), quindi niente `LazyInitializationException`.
- Riepilogo Iscritti è una `TreeGrid` ricostruita in `applicaFiltri()`: la ricerca si aggiunge a quei filtri.

### Decisioni

- [F-2026-10-19/RICERCA.1] Si cerca in tutti i dati della persona: nome, cognome, username, email, telefono, codice fiscale, indirizzo e i collegamenti mostrati (figli del genitore, genitore dell'allievo, specializzazione e corsi dell'insegnante, corsi dell'allievo, ruoli dell'utente). Senza distinguere maiuscole e accenti; con più parole devono comparire tutte, in qualsiasi campo e ordine ("rossi mario").
- [F-2026-10-19/RICERCA.1] Elenchi: Gestione Utenti, Insegnanti, Genitori, Gestione Allievi, e in più Gestione Corsi (nome, anno, insegnanti, descrizione) e Riepilogo Iscritti (corso, anno, insegnanti; allievo, genitore, email).
- [F-2026-10-19/RICERCA.1] Campo "Cerca" con lente e pulsante per svuotarlo sopra la tabella, filtra mentre si scrive (dopo una breve pausa) e mostra "N di M"; a tutta larghezza su mobile. Si combina con i filtri già presenti.
- Decisione tecnica: nel Riepilogo Iscritti un corso compare se corrisponde lui (con tutti gli iscritti) o almeno un suo iscritto (con i soli iscritti corrispondenti); i totali della riga del corso restano quelli del corso intero, il totale generale somma i corsi mostrati. Il conteggio "N di M" lì conta i corsi.

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | Componente `RicercaRapida` (campo, confronto senza maiuscole/accenti, parole multiple, conteggio "N di M") | `views/RicercaRapida`, test | `./mvnw test` | [x] |
| 2 | Ricerca in Gestione Utenti, Insegnanti, Genitori e Gestione Allievi | le quattro viste, `ViewAccessTest` | `./mvnw test` | [ ] |
| 3 | Ricerca in Gestione Corsi e Riepilogo Iscritti | `CorsiView`, `RiepilogoIscrittiView`, `ViewAccessTest` | `./mvnw test`, browser desktop e 390px | [ ] |
| 4 | CLAUDE.md e registro | `CLAUDE.md`, questo piano | — | [ ] |

## Fuori ambito

- Ricerca negli altri elenchi (Gestione Pagamenti ha già i suoi filtri per corso, allievo e genitore).
- Evidenziazione del testo trovato nelle righe.

## Registro

