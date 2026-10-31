# Quota piena, pre-iscrizione o personalizzata

## Richiesta

[F-2026-10-31/PAGINE.4] Nel dialog "Quota e pagamenti" una scelta tra quota piena e pre-iscrizione, oppure la quota
personalizzata con il motivo. [F-2026-10-31/PAGINE.5] Verificare che conferma e ricevuta non riportino nulla in più con
la quota piena, indichino la pre-iscrizione come nota della quota e il motivo per una quota personalizzata.

## Analisi

- Oggi l'iscrizione ha solo `quotaPersonalizzata` (null = piena) e `notaQuota`; la pre-iscrizione si applica a mano
  copiando il prezzo, e `DocumentiService.importi` la riconosce se la quota coincide con `Corso.prezzoPreiscrizione` e
  manca il motivo.
- La quota e il suo motivo compaiono in `DocumentiService.importi` (conferma, ricevuta), `rigaRiepilogo` (riepilogo
  annuale), `PagamentiAllievoDialog` (famiglie), `PartecipantiCorso` (colonna Quota). Nessuna query calcola la quota in
  SQL: basta cambiare `Iscrizione.getQuotaCorso()`.
- Verifica dei documenti: quota piena senza righe in più e personalizzata con il motivo sono già così; la pre-iscrizione
  ora diventa esplicita invece che dedotta.

### Decisioni

- [F-2026-10-31/PAGINE.4] `Iscrizione.tipoQuota` (`TipoQuota` PIENA / PREISCRIZIONE / PERSONALIZZATA, colonna
  `tipo_quota` VARCHAR creata da `ddl-auto`, null = dedotto: piena senza quota personalizzata, altrimenti
  personalizzata). Con PREISCRIZIONE la quota è `Corso.prezzoPreiscrizione` e lo segue se cambia.
- Dialog: `SegmentedControl` "Quota piena · 150,00 €" / "Pre-iscrizione · 130,00 €" / "Personalizzata"; importo e
  motivo visibili solo con Personalizzata, salvati come oggi. Pre-iscrizione disattivata (tooltip) senza prezzo.
- All'avvio (`QuotePreiscrizioneIniziali`) le quote personalizzate uguali al prezzo di pre-iscrizione con motivo vuoto
  o "pre-iscrizione" diventano PREISCRIZIONE.
- [F-2026-10-31/PAGINE.5] Documenti: piena nessuna riga; pre-iscrizione listino barrato + "Sconto applicato
  (pre-iscrizione)"; personalizzata con il motivo. Motivo unico in `Iscrizione.getMotivoQuota()`, usato anche da
  riepilogo annuale, famiglie e colonna Quota.
- Scelta tecnica: togliere il prezzo di pre-iscrizione da un corso con iscritti in pre-iscrizione è rifiutato
  (`CorsoService.save`), altrimenti la loro quota resterebbe senza importo.

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | Dati: `TipoQuota`, quota del corso e motivo da `Iscrizione`, servizio, conversione all'avvio, controllo sul corso | entità, `PagamentoService`, `CorsoService`, `QuotePreiscrizioneIniziali`, `DatiDemo`, test | `./mvnw test` | [x] |
| 2 | Dialog "Quota e pagamenti" con la scelta a pulsanti; colonna Quota e famiglie col motivo | `QuotaPagamentiDialog`, `PartecipantiCorso`, `PagamentiAllievoDialog`, test | `./mvnw test` | [x] |
| 3 | Documenti: pre-iscrizione esplicita, verifica dei tre casi su conferma e ricevuta | `DocumentiService`, test | test sul PDF e sull'email | [x] |
| 4 | Guida, CLAUDE.md e Registro | `guida/staff.md`, `CLAUDE.md`, questo file | test della Guida | [x] |

## Fuori ambito

- Quote già inviate nei documenti: restano com'erano.

## Registro

- 2026-10-31: `./mvnw test` 370 test, 0 errori (nuovi: `TipoQuotaTest` su pre-iscrizione che segue il prezzo, prezzo non
  eliminabile e conversione all'avvio; dialog con le tre scelte; conferma con quota piena senza righe in più,
  pre-iscrizione e personalizzata con e senza motivo, nel PDF e nell'email). Non provato nel browser.
- Produzione: la colonna `tipo_quota` la crea `ddl-auto=update`; nessuno script SQL. Al primo avvio le quote
  personalizzate uguali al prezzo di pre-iscrizione del corso, senza motivo o con motivo "pre-iscrizione", diventano
  pre-iscrizione (riga nel log con il numero); le altre restano personalizzate.
- Le quote in pre-iscrizione seguono il prezzo del corso: cambiandolo cambia la quota di quegli iscritti.
