# Stampa del riepilogo dei corsi attivi

## Richiesta

[F-2026-10-31/STAMPA.1] Una stampa (anteprima, PDF, invio per email) del riepilogo dei corsi attivi della dashboard di
admin e segreteria, con la stessa struttura e chiarezza; solo per admin e segreteria.

## Analisi

- La dashboard di admin/segreteria (`HomeView`) ha tre sezioni: "Riepilogo generale" (`DashboardService.riepilogoGenerale`,
  `etaAllievi`), "Pagamenti dei corsi attivi" (`PagamentoService.riepilogoCorsiAttivi`, `BilancioService.costiPrevistiPerCorso`,
  `oreDonateTotali`: riquadri + una riga per corso con costo insegnanti e margine) e "Gruppi dei corsi attivi"
  (`DashboardService.riepilogoGruppi`: riquadri + una riga per gruppo).
- Gli iscritti per corso e gruppo con quota/pagato/residuo ci sono già in `PagamentoService.iscrittiPerCorso()` (usato da
  Riepilogo Iscritti, ordinati con `Iscrizione.ORDINE`): basta tenere i corsi attivi.
- I PDF nascono da FreeMarker + OpenHTMLtoPDF in `DocumentiService` (`templates/documenti/comune.ftlh` per intestazione e
  stili); gli invii sono registrati in `InvioEmail` (`tipo` VARCHAR(32) dopo `2026-10-23-invii-riepilogo.sql`: un nuovo
  valore dell'enum non richiede migrazioni). Il filtro tipo di "Cronologia documenti" e il `SegmentedControl` di Documenti
  usano `TipoDocumento.values()`: il nuovo tipo vi compare da solo.
- `MenuPdf.conTesto` dà "Anteprima in una nuova scheda" e "Scarica PDF"; l'invio alla propria email esiste già per le
  famiglie (`DocumentiView.inviaAMe`, `EmailService.verificaDestinatarioProprio`).

### Decisioni

- [F-2026-10-31/STAMPA.1] Contenuto: le tre sezioni della dashboard (riquadri e tabelle) più, per ogni corso attivo,
  l'elenco nominativo degli iscritti divisi per gruppo, con orario e stato del pagamento (quota, pagato, residuo).
- [F-2026-10-31/STAMPA.1] Costo insegnanti, margine previsto e ore donate inclusi, come in dashboard.
- [F-2026-10-31/STAMPA.1] La stampa si apre da Documenti: nuovo tipo "Riepilogo corsi" (solo admin e segreteria).
- [F-2026-10-31/STAMPA.1] Email solo all'indirizzo del proprio profilo ("Invia alla mia email"), registrata in Cronologia
  documenti › Email inviate.
- Tecniche: corsi attivi alla data della stampa (come la dashboard), A4 orizzontale per le tabelle larghe; anteprima e
  scarico dello staff non vanno in "Documenti aperti" (come gli altri documenti dello staff).

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | Dati e PDF: `TipoDocumento.RIEPILOGO_CORSI`, `RiepilogoCorsiService` (sezioni della dashboard + iscritti dei corsi attivi), template `documenti/riepilogo-corsi.ftlh`, `DocumentiService.riepilogoCorsiPer(username)` (solo staff) | entity, service, templates, `DocumentiServiceTest` | `./mvnw test` | [x] |
| 2 | Email: `email/riepilogo-corsi.ftlh`, `EmailService.inviaRiepilogoCorsiAMe` registrato in `InvioEmail` | `EmailService`, templates, `EmailServiceTest` | `./mvnw test` | [x] |
| 3 | Documenti: tipo "Riepilogo corsi" con descrizione, PDF (anteprima/scarica), "Invia alla mia email", ultimo invio | `DocumentiView`, `ViewAccessTest` | `./mvnw test` | [x] |
| 4 | CLAUDE.md: documentazione della stampa | `CLAUDE.md` | — | [x] |
| 5 | Registro | questo piano | — | [x] |

## Fuori ambito

- Scelta di un anno scolastico o di corsi non attivi (la dashboard mostra i soli corsi attivi).
- Invio ad altri indirizzi.

## Registro

- Passi 1-4 completati, un commit per passo; `./mvnw test`: 342 test, 0 errori.
- Nuovi test: `DocumentiServiceTest.riepilogoCorsi_…` (sezioni, iscritti, pagine numerate, solo staff),
  `EmailServiceTest.riepilogoCorsi_…` (invio all'email del profilo, registrato, solo staff),
  `ViewAccessTest.segreteria_documenti_riepilogoCorsi…` (nessun filtro, link PDF, invio e ultimo invio).
- PDF controllato a vista sui dati di sviluppo, anche con un corso a gruppi (gruppi con nome, senza nome e
  "Senza gruppo"): una sezione o un corso che sta in una pagina non si spezza; intestazione delle tabelle ripetuta.
- Nessuna migrazione: `invii_email.tipo` è VARCHAR(32) e "RIEPILOGO_CORSI" ci sta.
- Aperto: il selettore del tipo in Documenti ha ora 6 opzioni; su smartphone le etichette più lunghe si accorciano
  (succedeva già con "Lettere di incarico"). Non verificato nel browser.
