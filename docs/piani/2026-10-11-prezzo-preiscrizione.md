# Piano: prezzo con pre-iscrizione nei corsi

## Richiesta

[F-2026-10-11/CORSI.1] Oltre al prezzo pieno (oggi "Costo Totale"), poter inserire nel corso un prezzo con pre-iscrizione, in genere scontato del 15% ma assegnato a mano.

## Analisi

- `Corso.costoTotale` è il prezzo pieno: è la quota dovuta da ogni partecipante salvo `Iscrizione.quotaPersonalizzata` (`Iscrizione.getQuotaDovuta()`), usata da `PagamentoService`, `ElencoPagamenti`, ricevuta e dialog dei pagamenti.
- Il campo si modifica in `CorsiView` (BigDecimalField "Costo Totale", validato non negativo); `QuotaPagamentiDialog` mostra in alto "quota piena …".
- Genitori e allievi oggi non vedono i prezzi nelle liste dei corsi (`MieiCorsiView`, dialog "Corsi di …" in `FigliView`); `PagamentoService.situazioniDelloAllievo(allievoId)` dà già per ogni corso la quota dovuta (prezzo applicato).
- Nuova colonna nullable `prezzo_preiscrizione` su `corsi`: `ddl-auto=update` la aggiunge, nessuno script SQL.

### Decisioni

- [F-2026-10-11/CORSI.1] "15%" = sconto del 15%: il valore tipico è l'85% del prezzo pieno.
- [F-2026-10-11/CORSI.1] Prezzo solo informativo: non cambia la quota dovuta dei partecipanti; per applicarlo si usa a mano la quota personalizzata.
- [F-2026-10-11/CORSI.1] Nessuna proposta automatica: campo facoltativo "Prezzo con pre-iscrizione" con testo d'aiuto sul valore tipico (es. "di solito 510,00 €").
- [F-2026-10-11/CORSI.1] L'etichetta "Costo Totale" resta; il nuovo prezzo è facoltativo, non negativo e non superiore al costo totale.
- [F-2026-10-11/CORSI.1] "Quota e pagamenti": la riga in alto mostra anche il prezzo con pre-iscrizione. Nessuna colonna nuova in Gestione Corsi.
- [F-2026-10-11/CORSI.1] Genitori e allievi lo vedono: nelle liste dei corsi di "I Miei Corsi" e "I Miei Figli" (dialog "Corsi di …") colonne Costo totale, Pre-iscrizione e prezzo Applicato (quota dovuta del partecipante); su mobile riassunte in una colonna.

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | `Corso.prezzoPreiscrizione`; controllo in `CorsoService.save` (≥ 0, ≤ costo totale); campo nel dialog di `CorsiView` con aiuto sul valore tipico; test | `entity/Corso.java`, `service/CorsoService.java`, `views/admin/CorsiView.java`, test | `./mvnw test` | [x] |
| 2 | `QuotaPagamentiDialog`: riga "quota piena … · pre-iscrizione …"; test | `views/admin/QuotaPagamentiDialog.java`, `ViewAccessTest` | `./mvnw test` | [x] |
| 3 | `MieiCorsiView` e `FigliView`: colonne Costo totale, Pre-iscrizione, Applicato (riassunto su mobile); test; chiude la richiesta | `views/allievo/MieiCorsiView.java`, `views/genitore/FigliView.java`, `views/PrezziCorso.java`, `ViewAccessTest`, `docs/FUNZIONALITA.md` | `./mvnw test`, browser | [x] |
| 4 | `CLAUDE.md`: nota sul prezzo con pre-iscrizione | `CLAUDE.md` | rilettura | [ ] |

## Fuori ambito

- Applicazione automatica del prezzo ai partecipanti (tariffa per iscrizione, scadenza della pre-iscrizione).
- Ricevuta ed elenco pagamenti invariati.
- `.idea/.copilot-plugin` (modifica locale preesistente) non incluso.

## Registro
