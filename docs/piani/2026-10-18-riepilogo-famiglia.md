# Piano: stampa di riepilogo per famiglia

## Richiesta

Una stampa di riepilogo per famiglia (per genitore) con tutti i corsi a cui sono iscritti i figli e il totale della spesa ([F-2026-10-18/RIEPILOGO.1]).

## Analisi

- I documenti (ricevuta, conferma) sono template FreeMarker in `templates/documenti/` resi in PDF da `DocumentiService` (OpenHTMLtoPDF), con intestazione e stile comuni in `comune.ftlh`.
- Quote e pagato per iscrizione vengono da `PagamentoService.righe(...)` (`RigaPagamento`, `Situazione`); `IscrizioneRepository.findByGenitoreUsernameWithCorsoAllievo` esiste già per il genitore, serve l'equivalente per id del genitore (admin/segreteria).
- `Corso.annoScolastico` è una stringa facoltativa: i corsi senza anno formano una scelta a parte ("Senza anno scolastico").
- Scarico come per le ricevute (`Ricevute`): `DownloadHandler` con i permessi verificati nel servizio (404 se non autorizzato).

### Decisioni

- [F-2026-10-18/RIEPILOGO.1] Lo scaricano admin e segreteria da un'azione su ogni riga di "Genitori", e il genitore il proprio dalla pagina "Pagamenti" (pulsante "Riepilogo famiglia"); ogni altro caso è rifiutato.
- [F-2026-10-18/RIEPILOGO.1] Il riepilogo è per anno scolastico: si sceglie l'anno tra quelli dei corsi dei figli (proposto il più recente); entrano tutti i corsi di quell'anno, attivi o no.
- [F-2026-10-18/RIEPILOGO.1] Per ogni corso: figlio, corso, quota dovuta (con l'eventuale quota personalizzata), pagato, residuo; subtotale per figlio e totali della famiglia.
- [F-2026-10-18/RIEPILOGO.1] Solo PDF (stesso stile di ricevuta e conferma), niente invio email; nessun dato anagrafico oltre ai nomi.
- Decisione tecnica: niente blocco per codice fiscale o data di nascita mancanti (il riepilogo non li riporta); icona dell'azione `FILE_TEXT_O` (PRINT resta della ricevuta).

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | Dati e PDF del riepilogo: anni della famiglia, righe per anno con totali, permessi, template | `IscrizioneRepository`, `PagamentoService`, `DocumentiService`, `templates/documenti/riepilogo-famiglia.ftlh`, test | `./mvnw test` | [x] |
| 2 | Dialog "Riepilogo famiglia" (scelta anno + Scarica PDF) da Genitori e da Pagamenti del genitore | `views/RiepilogoFamigliaDialog`, `admin/GenitoriView`, `genitore/PagamentiFigliView`, `ViewAccessTest` | `./mvnw test`, browser desktop e 390px | [x] |
| 3 | CLAUDE.md e registro | `CLAUDE.md`, questo piano | — | [ ] |

## Fuori ambito

- Invio del riepilogo per email.
- Elenco delle singole rate (restano nelle ricevute).

## Registro

