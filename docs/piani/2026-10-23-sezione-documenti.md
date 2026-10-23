# Piano: sezione "Documenti"

## Richiesta

[F-2026-10-23/DOCUMENTI.1] Togliere i pulsanti per i PDF sparsi nelle viste e fare una sezione "Documenti" in cui generare, inviare o vedere in anteprima i documenti; per genitori e allievi è l'area di tutta la loro documentazione.

## Analisi

- Documenti esistenti:
  - **Conferma di iscrizione** (per iscrizione): solo come email con PDF allegato (`InvioEmailDialog.apri` da Gestione Pagamenti e Quota e pagamenti, `inviaConferme` da Partecipanti); le famiglie oggi non la vedono.
  - **Ricevuta** (per iscrizione, se ci sono pagamenti): `Ricevute.pulsante` in Gestione Pagamenti / Pagamenti del genitore e dell'allievo adulto (`ElencoPagamenti`), I Miei Figli, Quota e pagamenti; email da `InvioEmailDialog`.
  - **Riepilogo annuale** (famiglia per genitore e anno, o allievo adulto per anno): `RiepilogoFamigliaDialog` da Genitori, Pagamenti del genitore e dell'allievo adulto; solo PDF.
- PDF e permessi in `DocumentiService` (`ricevutaPer`, `riepilogoFamigliaPer`, `riepilogoAllievoPer`: staff tutto, genitore i figli, allievo adulto sé); email in `EmailService` (`invia`, `anteprima`, `destinatariConferme`, `inviaConferme`, registrazione in `InvioEmail`, solo admin/segreteria). `MenuPdf` per anteprima/scarica.
- `InvioEmail` (tabella `invii_email`) ha `corsoId`/`allievoId` facoltativi ma nessun riferimento al genitore e all'anno; `tipo` è un `TipoDocumento` salvato come stringa: su MySQL Hibernate crea una colonna `enum('CONFERMA_ISCRIZIONE','RICEVUTA')` che `ddl-auto=update` non modifica → per un nuovo tipo serve uno script SQL.
- `EmailDiTestDialog` ed `EmailInviateView` scorrono `TipoDocumento.values()`: vanno adattati al nuovo tipo.

### Decisioni

- [F-2026-10-23/DOCUMENTI.1] Si tolgono dalle altre viste i pulsanti PDF (Ricevuta, Riepilogo famiglia/allievo) e gli invii email dei documenti ("Email: conferma o ricevuta", "Invia conferme di iscrizione a tutti"); restano "Email inviate" e l'email di test dell'admin.
- [F-2026-10-23/DOCUMENTI.1] Staff (admin e segreteria): sezione per tipo di documento (Conferme di iscrizione, Ricevute, Riepiloghi annuali); elenco di iscritti per corso (conferme e ricevute) o di famiglie/allievi adulti per anno (riepiloghi), filtri per anno scolastico e corso, ricerca; su ogni riga anteprima/scarica PDF e invio email (con anteprima dell'email); con un corso scelto "Invia conferme di iscrizione a tutti".
- [F-2026-10-23/DOCUMENTI.1] Il riepilogo annuale si può inviare per email (nuova email "Riepilogo pagamenti AAAA/AAAA" con il PDF, al genitore o all'allievo adulto), registrata in "Email inviate".
- [F-2026-10-23/DOCUMENTI.1] Script SQL `docs/migrazioni/2026-10-23-invii-riepilogo.sql` (colonna `tipo` di `invii_email` in testo) da eseguire su MySQL prima del deploy.
- [F-2026-10-23/DOCUMENTI.1] Invii a tutti: solo le conferme di un corso, come oggi.
- [F-2026-10-23/DOCUMENTI.1] Colonna "Ultimo invio" nell'elenco dello staff: data e destinatario dell'ultima email non in errore di quel documento.
- [F-2026-10-23/DOCUMENTI.1] Genitori e allievi senza genitore: "Documenti" con conferma di iscrizione (nuova per loro) e ricevuta (se ci sono pagamenti) di ogni corso, anche passato, dei figli o propri, e il riepilogo per anno scolastico; solo anteprima e scarica. L'allievo con genitore non ha la voce (i documenti sono del genitore).
- Scelte tecniche: una sola vista `views/DocumentiView` (route `documenti`) che mostra l'elenco dello staff ad admin e segreteria e quello della famiglia agli altri; voce nascosta all'allievo con genitore (come "Pagamenti"); `InvioEmail` riceve `genitoreId` e `anno` (colonne nuove facoltative, aggiunte da `ddl-auto=update`) per ritrovare l'ultimo invio dei riepiloghi; con dati mancanti (codice fiscale, data di nascita) la riga mostra l'avviso invece del PDF, come oggi.

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | Tipo di documento `RIEPILOGO`, `InvioEmail.genitoreId` e `anno`, script SQL; "Email inviate" con il filtro Riepiloghi, email di test solo per conferma e ricevuta | `TipoDocumento`, `InvioEmail`, `docs/migrazioni/2026-10-23-invii-riepilogo.sql`, `EmailInviateView`, `EmailDiTestDialog` | `./mvnw test` | [x] `tipo` mappato come testo (`@JdbcTypeCode(VARCHAR)`), così un nuovo tipo non richiederà altri script |
| 2 | Servizi: PDF della conferma per le famiglie (`DocumentiService.confermaPer`), email del riepilogo (template, destinatario, `anteprimaRiepilogo`, `inviaRiepilogo`); test | `DocumentiService`, `EmailService`, `templates/email/riepilogo.ftlh`, test | `./mvnw test` | [x] |
| 3 | `ElencoDocumentiService`: righe dello staff (iscrizioni con destinatario, pagamenti, dati mancanti, ultimo invio; riepiloghi per famiglia/allievo adulto e anno) e documenti della famiglia; test | `service/ElencoDocumentiService.java`, repository, test | `./mvnw test` | [x] Le ricevute elencate sono solo quelle delle iscrizioni con pagamenti |
| 4 | `InvioEmailDialog`: invio di un singolo documento (conferma, ricevuta o riepilogo) con destinatario e anteprima; resta l'invio delle conferme a tutti | `views/admin/InvioEmailDialog.java` | `./mvnw test` | [x] Il vecchio dialog combinato (conferma o ricevuta) resta finché i suoi pulsanti non sono tolti al passo 7; il dialog nuovo è provato dai test della vista (passo 5) |
| 5 | `DocumentiView` per admin e segreteria: tipo, filtri, ricerca, PDF, invio, ultimo invio, conferme a tutti; card su mobile; test browserless | `views/DocumentiView.java`, `ViewAccessTest` | `./mvnw test` | [x] Anno iniziale: quello corrente se ha documenti, altrimenti il più recente |
| 6 | `DocumentiView` per genitori e allievi senza genitore (voce nascosta all'allievo con genitore); test browserless | `views/DocumentiView.java`, `MainLayout`, `ViewAccessTest` | `./mvnw test` | [x] Filtro per anno solo con più anni; l'allievo con genitore che apre l'indirizzo vede un avviso |
| 7 | Via i pulsanti PDF ed email dalle altre viste; rimozione di `Ricevute` e `RiepilogoFamigliaDialog` se non più usati; test aggiornati | `ElencoPagamenti`, `QuotaPagamentiDialog`, `FigliView`, `GenitoriView`, `PagamentiFigliView`, `MieiPagamentiView`, `PartecipantiDialog`, test | `./mvnw test`; browser | [ ] |
| 8 | Documentazione in `CLAUDE.md` | `CLAUDE.md` | rilettura | [ ] |
| 9 | Registro finale | questo piano | build e test | [ ] |

## Fuori ambito

- Invio a tutti di ricevute e riepiloghi (decisione: solo le conferme).
- Documenti per gli insegnanti (nessun PDF per loro oggi).
- Completamento dei dati mancanti da parte di genitori e allievi (richiesta aperta [F-2026-10-23/DATI.1]).

## Registro

_(da compilare durante l'esecuzione)_
