# Piano: allievi adulti che gestiscono da sé i pagamenti

## Richiesta

Un allievo adulto non deve passare per un genitore: deve vedere sé stesso e i propri pagamenti come se fosse il genitore di sé stesso ([F-2026-10-23/ADULTI.1]).

## Analisi

- Il genitore è facoltativo (`Allievo.genitore`): senza genitore email e documenti vanno già all'allievo. L'allievo con utente vede solo "I Miei Corsi" (corsi, presenze, prezzi) e in dashboard il numero dei corsi.
- Il genitore ha "Pagamenti" (`genitore/PagamentiFigliView` con `views/ElencoPagamenti` in sola lettura, ricevute con `Ricevute.link`), il "Riepilogo famiglia" (`RiepilogoFamigliaDialog`, `DocumentiService.riepilogoFamigliaPer`, template `riepilogo-famiglia.ftlh`) e le card Pagato/Da pagare (`PagamentoService.situazioneDeiFigli`).
- I permessi sui PDF (`DocumentiService.puoVedere`) ammettono solo admin, segreteria e il genitore.
- Il menu viene da `MenuConfiguration.getMenuEntries()`, filtrato solo per ruolo: per mostrare "Pagamenti" solo agli allievi senza genitore serve un filtro in `MainLayout`.
- La sezione si chiama "ALLIEVI ADULTI": chiave `ADULTI`.

### Decisioni

- [F-2026-10-23/ADULTI.1] "Adulto" = allievo senza genitore; nessun dato nuovo.
- [F-2026-10-23/ADULTI.1] Voce "Pagamenti" per l'allievo adulto (come quella del genitore, solo per sé): quota, pagato, residuo, stato, rate e ricevute; con il "Riepilogo" PDF per anno scolastico.
- [F-2026-10-23/ADULTI.1] Card "Pagato" e "Da pagare" nella sua sezione della dashboard (corsi attivi, come il genitore).
- [F-2026-10-23/ADULTI.1] In I Miei Corsi una colonna con lo stato del pagamento (badge Saldato, Parziale N%, Da pagare).
- [F-2026-10-23/ADULTI.1] Chi ha un genitore non vede nulla di tutto questo.
- Tecnica: la voce di menu è nascosta dal filtro in `MainLayout` per gli allievi con genitore e la vista mostra un avviso se ci si arriva dall'indirizzo; i permessi dei PDF ammettono anche l'allievo stesso quando non ha genitore; il riepilogo usa lo stesso template del riepilogo famiglia con l'intestazione "Allievo".

## Passi

| Passo | Cosa | Richiesta | File | Verifica | Stato |
|---|---|---|---|---|---|
| 1 | Servizi: `AllievoService.gestisceDaSe`, righe/situazione/anni/riepilogo dell'allievo in `PagamentoService`, permessi e riepilogo PDF in `DocumentiService`; test | — | `service/…`, `templates/documenti/riepilogo-famiglia.ftlh`, test | `./mvnw test` | [x] |
| 2 | Viste: "Pagamenti" dell'allievo con riepilogo, filtro del menu, card in dashboard, stato in I Miei Corsi; test | [F-2026-10-23/ADULTI.1] | `views/allievo/…`, `views/RiepilogoFamigliaDialog`, `views/MainLayout`, `views/HomeView`, test | `./mvnw test` | [x] |
| 3 | CLAUDE.md e registro | — | `CLAUDE.md`, questo piano | — | [ ] |

## Fuori ambito

- Criterio per età (data di nascita) o casella esplicita nella scheda.

## Registro
