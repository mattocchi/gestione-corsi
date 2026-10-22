# Piano: regole su recuperi, assenze e rinuncia nella conferma di iscrizione

## Richiesta

Aggiungere alla conferma di iscrizione alcune frasi che rendano espliciti recupero delle lezioni annullate, assenze dell'allievo e rinuncia al corso, riscritte in modo più efficace ([F-2026-10-22/EMAIL.2]).

## Analisi

- Il testo dell'email è `templates/email/conferma.ftlh` (HTML con stili in linea), il PDF allegato è `templates/documenti/conferma.ftlh` (XHTML ben formato per OpenHTMLtoPDF, stili da `documenti/comune.ftlh`). FreeMarker carica i template da `/templates`.

### Decisioni

- [F-2026-10-22/EMAIL.2] Testo approvato (sezione "Lezioni, assenze e recuperi"):
  - **Lezione annullata dall'insegnante** – Viene sempre recuperata, in un altro giorno o in coda al corso: il numero di lezioni previsto è garantito.
  - **Assenza dell'allievo** – Se avvisate la segreteria dei corsi almeno una settimana prima, l'assenza è giustificata e, se possibile, si concorda con l'insegnante un recupero. Con un preavviso più breve (o senza avviso) l'assenza non è giustificata: la lezione non viene recuperata ed è comunque dovuta.
  - **Rinuncia al corso** – Non è previsto il rimborso delle lezioni non frequentate. Eventuali eccezioni sono valutate caso per caso, a giudizio insindacabile della segreteria dei corsi.
- [F-2026-10-22/EMAIL.2] Nell'email (prima dei saluti) e nel PDF di conferma (in fondo).
- Tecnica: il testo sta in un solo file, `templates/regole-corso.ftlh` (macro usata dai due template, XHTML valido), così email e PDF non possono divergere.

## Passi

| Passo | Cosa | Richiesta | File | Verifica | Stato |
|---|---|---|---|---|---|
| 1 | Macro `regole-corso.ftlh` e sezione nell'email e nel PDF di conferma; test | [F-2026-10-22/EMAIL.2] | `templates/regole-corso.ftlh`, `templates/email/conferma.ftlh`, `templates/documenti/conferma.ftlh`, `DocumentiServiceTest` | `./mvnw test` | [x] |
| 2 | CLAUDE.md e registro | — | `CLAUDE.md`, questo piano | — | [x] |

## Fuori ambito

- Testo configurabile dall'interfaccia; regole nella ricevuta.

## Registro

- Passo 1: `./mvnw test` 210 test verdi; `conferma_importoDaPagareEIstruzioniPerIlBonifico` verifica le tre regole nel testo del PDF generato e nell'email.
- Non verificato a vista: il PDF e l'email si possono controllare con il pulsante "Anteprima" nel dialog di invio.
