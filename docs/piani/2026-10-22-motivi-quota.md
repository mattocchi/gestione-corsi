# Piano: suggerimenti per il motivo della quota personalizzata

## Richiesta

Nel dialog "Quota e pagamenti" (Gestione Pagamenti) il campo del motivo della quota personalizzata deve proporre i motivi già inseriti, lasciando scriverne uno nuovo, usando i valori già nel database senza nuove tabelle ([F-2026-10-22/PAGAMENTI.1]).

## Analisi

- `QuotaPagamentiDialog`: `TextField` "Motivo" salvato a ogni cambio con `PagamentoService.impostaQuota(corso, allievo, quota, nota)`; abilitato solo con una quota personalizzata. Il motivo sta in `Iscrizione.notaQuota` (colonna di `corso_allievi`).
- Vaadin 25.3 (MCP, Combo Box → Custom Value Entry): `ComboBox.setAllowCustomValue(true)` + `addCustomValueSetListener` per accettare valori nuovi; già usato in `PartecipantiDialog`.

### Decisioni

- [F-2026-10-22/PAGAMENTI.1] Suggerimenti dai motivi di tutte le iscrizioni (tutti i corsi e anni), senza doppioni ignorando maiuscole e spazi ai lati (si mostra la grafia più usata), prima i più usati e a parità in ordine alfabetico; scrivendo si filtrano.
- [F-2026-10-22/PAGAMENTI.1] Si può scrivere un motivo nuovo; nessuna tabella nuova.
- [F-2026-10-22/PAGAMENTI.1] La "Nota" dei pagamenti resta un campo di testo libero.

## Passi

| Passo | Cosa | Richiesta | File | Verifica | Stato |
|---|---|---|---|---|---|
| 1 | `PagamentoService.motiviQuota()` (query su `corso_allievi`); "Motivo" come `ComboBox` con valori liberi; test | [F-2026-10-22/PAGAMENTI.1] | `repository/IscrizioneRepository`, `service/PagamentoService`, `views/admin/QuotaPagamentiDialog`, test | `./mvnw test` | [x] |
| 2 | CLAUDE.md e registro | — | `CLAUDE.md`, questo piano | — | [x] |

## Fuori ambito

- Suggerimenti per la nota dei pagamenti.

## Registro

- Passo 1: `./mvnw test` 198 test verdi; nuovo `segreteria_motivoQuotaSuggerisceIMotiviGiaUsati` (motivi da più corsi senza doppioni, prima i più usati, scelta di un motivo suggerito); `segreteria_impostaQuotaPersonalizzata` ora scrive il motivo come valore nuovo della ComboBox (spazi tolti).
- Non verificato nel browser. Nessuna modifica al database.
