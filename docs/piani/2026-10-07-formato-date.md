# Piano: formato date leggibile ("mer 7 ottobre")

## Richiesta

Ovunque l'interfaccia mostra una data, usare un formato del tipo "mer 7 ottobre", così si legge subito il giorno della settimana.

## Analisi

Oggi le date sono mostrate con il `toString()` di `LocalDate` (`2024-10-03`). Punti trovati:

| Dove | Data mostrata |
|---|---|
| `CorsiView` griglia | Data Inizio |
| `StudentiView` griglia | Data Nascita |
| `LezioniDocenteView` griglia (ordinabile) | Data lezione |
| `LezioniDocenteView` titolo dialog presenze | Data lezione |
| `FigliView` griglia | Data Nascita |
| `MieiCorsiView` griglia | Data Inizio |
| `PresenzeStudenteDialog` griglia | Data lezione |
| `FigliView` riepilogo .txt scaricabile | Data inizio e date delle presenze |

Scelte:
- Formato `EEE d MMMM` con `Locale.ITALIAN` → "mer 7 ottobre".
- **Anno**: aggiunto solo se diverso dall'anno corrente ("gio 3 ottobre 2024"). Senza anno date di nascita e lezioni di anni passati sarebbero ambigue; per le date dell'anno in corso il formato resta esattamente quello richiesto.
- Le colonne ordinabili mantengono l'ordinamento per data (comparator su `LocalDate`), non alfabetico sulla stringa.
- Un solo punto di formattazione (`views/Formati`), riusato da griglie, titoli e riepilogo.
- Le colonne data devono restare leggibili a 390px (regola responsive del progetto).

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | Classe `Formati.data(LocalDate)` + test unitario (anno corrente / anno diverso / null) | `views/Formati.java`, `FormatiTest.java` | `./mvnw test` | [x] |
| 2 | Viste admin: Data Inizio corsi, Data Nascita studenti | `CorsiView`, `StudentiView` | `./mvnw test` | [x] |
| 3 | Vista docente: colonna Data (ordinamento per data) e titolo del dialog presenze | `LezioniDocenteView` | `./mvnw test` | [x] |
| 4 | Viste genitore/studente: Data Nascita figli, Data Inizio corsi, Data nel dialog presenze | `FigliView`, `MieiCorsiView`, `PresenzeStudenteDialog` | `./mvnw test` | [x] |
| 5 | Riepilogo scaricabile: data inizio e date delle presenze | `FigliView` | `./mvnw -q compile` | [x] |
| 6 | Regola nel `CLAUDE.md`: le date in UI passano da `Formati.data` | `CLAUDE.md` | — | [x] |
| 7 | Verifica finale: build con test, controllo nel browser a 390px delle viste con date, registro | piano | build + browser | [x] |

## Fuori ambito

- **Campi di inserimento** (`DatePicker` in Corsi e Studenti): restano nel formato numerico del locale, perché digitare il giorno della settimana renderebbe l'inserimento più scomodo. Si può valutare di mostrare il giorno come helper text sotto il campo.
- Localizzazione dell'interfaccia in altre lingue: il formato è fisso in italiano come il resto della UI.

## Registro

- Passi 1–6 completati, un commit per passo; `FormatiTest` conferma "mer 7 ottobre" (anno corrente) e "gio 3 ottobre 2024" (altro anno).
- Build finale `./mvnw clean package`: BUILD SUCCESS, 14/14 test verdi (3 nuovi).
- Browser a 390px: Le Mie Lezioni (356px su 356px, "gio 3 ottobre 2024"), titolo del dialog presenze "Corso di Canto – Lezione 1 (gio 3 ottobre 2024)", dialog presenze del genitore (340px su 340px); a 1280px I Miei Figli mostra "lun 15 marzo 2010".
- Ordinamento della colonna Data verificato cronologico: crescente da "gio 3 ottobre 2024", decrescente da "gio 24 aprile 2025".
- I dati di test sono del 2024–2025, quindi oggi (2026) mostrano tutti l'anno; le date dell'anno corrente compaiono senza anno.
