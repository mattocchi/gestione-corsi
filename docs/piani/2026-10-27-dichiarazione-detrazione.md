# Piano: dichiarazione delle spese conforme alle disposizioni sulla detrazione

## Richiesta

[F-2026-10-27/DETRAZIONE.1] Rendere la dichiarazione delle spese (attestazione) conforme alle direttive dei PDF in `documentazione/` (vademecum del Tavolo Permanente delle Federazioni Bandistiche, aprile 2024, e scheda "Spese per corsi di musica", art. 15 c. 1 lett. e-quater TUIR).

## Analisi

- Dai PDF: detrazione del 19% delle spese per iscrizione annuale e abbonamento di ragazzi tra 5 e 18 anni (basta una parte dell'anno) a scuole/bande riconosciute; massimo 1.000 € per ragazzo, reddito fino a 36.000 € (verifiche a carico della famiglia, non dell'associazione); solo pagamenti tracciabili, il contante non dà diritto; periodo = anno d'imposta (nel 2024 i dati del 2023).
- Il documento deve riportare: dati identificativi dell'allievo (cognome e nome, luogo e data di nascita, codice fiscale, residenza), l'iscrizione e la frequenza ai corsi nel periodo (anno), l'importo totale versato con modalità tracciabile (in numeri e in lettere), la dichiarazione che l'associazione possiede i requisiti di legge; intestatario (Spett., indirizzo, codice fiscale); luogo e data, dati dell'associazione (indirizzo, codice fiscale/partita IVA), firma. Fac-simile 1.1 "Ricevuta spese per corsi musicali per ragazzi di età compresa tra 5 e 18 anni (non ancora maggiorenni)".
- Oggi `PagamentoService.attestazione` somma tutte le rate dell'anno solare dei corsi pubblicati (anche "Cassa" e servizi) e il template mostra allievo, genitore, elenco rate e totale.

### Decisioni

- [F-2026-10-27/DETRAZIONE.1] Periodo: anno solare (periodo d'imposta), come oggi.
- [F-2026-10-27/DETRAZIONE.1] Nel totale solo i pagamenti con bonifico (tracciabili); "Cassa" escluso; nessun nuovo metodo.
- [F-2026-10-27/DETRAZIONE.1] Solo corsi: i pagamenti dei servizi non entrano.
- [F-2026-10-27/DETRAZIONE.1] Si emette per tutti; fuori dalla fascia 5-18 anni il testo si adatta (titolo senza fascia d'età, "l'allievo" invece di "il minore", senza la frase sulla detrazione del 19%) e lo staff vede il badge "Fuori età 5-18" (anche nel dialog d'invio).
- [F-2026-10-27/DETRAZIONE.1] Intestatario: il genitore registrato (o l'allievo adulto); nessuna ripartizione tra genitori.
- [F-2026-10-27/DETRAZIONE.1] Bloccata finché mancano i dati del fac-simile (luogo e data di nascita, codice fiscale e residenza dell'allievo, propria o del genitore; codice fiscale e indirizzo dell'intestatario): badge "Dati mancanti", niente PDF né email.
- [F-2026-10-27/DETRAZIONE.1] Nuova impostazione `app.associazione.luogo` (ASSOCIAZIONE_LUOGO) per "Luogo e data"; se vuota resta solo la data.
- Scelte tecniche: sotto la dichiarazione resta l'elenco dei versamenti (data, corso, importo) come dettaglio; importo in lettere nel formato "centoventi/00".

## Passi

| Passo | Cosa | File | Richiesta | Verifica | Stato |
|---|---|---|---|---|---|
| 1 | Dati: solo bonifico e corsi; fascia 5-18; dati mancanti; importo in lettere; `app.associazione.luogo`; test | `PagamentoService`, `PagamentoRepository`, `DocumentiService`, `Formati`, `AssociazioneProperties`, test | — | `./mvnw test` | [ ] |
| 2 | Template PDF ed email secondo il fac-simile; test sul testo del PDF | `documenti/attestazione.ftlh`, `email/attestazione.ftlh`, test | — | `./mvnw test` | [ ] |
| 3 | Documenti: badge "Dati mancanti" e "Fuori età 5-18", PDF e invio bloccati senza dati, avviso nel dialog d'invio; test | `ElencoDocumentiService`, `DocumentiView`, `EmailService`, `InvioEmailDialog`, test | DETRAZIONE.1 | `./mvnw test` | [ ] |
| 4 | `CLAUDE.md`, `deploy/README.md` (ASSOCIAZIONE_LUOGO) e registro | docs | — | test | [ ] |

## Fuori ambito

- Verifica dei requisiti della famiglia (reddito, limite 1.000 €): a carico dell'avente diritto.
- Ripartizione della spesa tra i genitori; nuovi metodi di pagamento (carta/POS, assegno).
- I PDF in `documentazione/` restano file dell'utente (non aggiunti al repository da questo piano).

## Registro

