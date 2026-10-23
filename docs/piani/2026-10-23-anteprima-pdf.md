# Piano: PDF in anteprima in una nuova scheda o da scaricare

## Richiesta

Rendere i PDF disponibili come anteprima inline in una nuova scheda oltre che come download, con un selettore "Anteprima PDF / Download" sui pulsanti ([F-2026-10-23/PDF.1]).

## Analisi

- PDF oggi solo da scaricare: ricevuta (`views/Ricevute.link`: icona nelle righe di Gestione Pagamenti, Pagamenti di genitore e allievo, I Miei Figli, pulsante "Ricevuta" in Quota e pagamenti), riepilogo famiglia / allievo (`RiepilogoFamigliaDialog`, "Scarica PDF"), allegato nell'anteprima email (`InvioEmailDialog`).
- Vaadin 25.3 (MCP, "Handle Downloads"): `DownloadHandler…inline()` con `Anchor(handler, AttachmentType.INLINE, …)` mostra il file nel browser; con target `_blank` in una nuova scheda. Un `MenuBar` con una sola voce principale è il "drop-down button" consigliato (MCP, Menu Bar). Aprire la scheda dal server dopo il clic (`Page.open`) rischia il blocco dei popup: le voci del menu sono link.
- `MenuBar` non è ancora usato nel progetto: componente nuovo → bundle di sviluppo ricostruito al primo test e `src/main/bundles/prod.bundle` da cancellare prima del prossimo build di produzione.

### Decisioni

- [F-2026-10-23/PDF.1] Un solo pulsante per ogni PDF che apre un menu con "Anteprima in una nuova scheda" e "Scarica PDF" (nelle righe delle tabelle l'icona della stampante, nei dialog il testo del pulsante).
- [F-2026-10-23/PDF.1] Vale per tutti i PDF: ricevute, riepilogo famiglia / allievo e allegato nell'anteprima delle email.
- Tecnica: componente comune `views/MenuPdf` (MenuBar con due link: `inline()` + `_blank` e download) costruito da un fornitore del PDF; permessi invariati (stessi metodi di `DocumentiService`).

## Passi

| Passo | Cosa | Richiesta | File | Verifica | Stato |
|---|---|---|---|---|---|
| 1 | `MenuPdf`; ricevute, riepilogo e allegato email lo usano; test | [F-2026-10-23/PDF.1] | `views/MenuPdf`, `views/Ricevute`, chiamanti, `RiepilogoFamigliaDialog`, `admin/InvioEmailDialog`, test | `./mvnw test` | [x] |
| 2 | CLAUDE.md e registro (promemoria `prod.bundle`) | — | `CLAUDE.md`, questo piano | — | [x] |

## Fuori ambito

- Anteprima integrata nella pagina (visualizzatore PDF dentro un dialog).

## Registro

- Passo 1: `./mvnw test` 232 test verdi (primo avvio con la ricostruzione del bundle per `MenuBar`); i test del riepilogo famiglia e della ricevuta del genitore verificano i due link (anteprima con `_blank` e senza `download`, scarico con `download`).
- Non verificato nel browser (apertura della nuova scheda e visualizzatore PDF).
- **Prima del prossimo build di produzione cancellare `src/main/bundles/prod.bundle`**: `MenuBar` è un componente nuovo per il progetto.
