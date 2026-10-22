# Piano: rimborso forfettario annuo dell'insegnante

## Richiesta

Aggiungere all'insegnante un rimborso "una tantum" che si somma al costo orario; se c'è solo uno dei due vale quello ([F-2026-10-22/RIMBORSI.1]).

## Analisi

- `Insegnante.tariffaOraria` (impostata in `InsegnantiView` da admin/segreteria, sola lettura in `ProfiloView`) è l'unica base dei costi in `BilancioService`: prospetto Rimborsi (maturato = lezioni completate dell'anno), `propostaRimborso` (mese), `bilancio` (costo maturato/previsto per corso), `costiPrevistiPerCorso` (Riepilogo Iscritti).
- `ProfiloView` salva tramite un metodo del servizio che copia solo i campi permessi: il nuovo campo non è modificabile dall'insegnante.

### Decisioni

- [F-2026-10-22/RIMBORSI.1] "Una tantum" = rimborso forfettario annuo: nuovo campo `Insegnante.rimborsoForfettario` (colonna `rimborso_forfettario`, facoltativo, ≥ 0), unico per tutti gli anni: se cambia vale anche per gli anni passati, come la tariffa.
- [F-2026-10-22/RIMBORSI.1] Lo impostano admin e segreteria nella scheda dell'insegnante; l'insegnante lo vede in sola lettura nel Profilo.
- [F-2026-10-22/RIMBORSI.1] Vale per ogni anno scolastico in cui l'insegnante ha corsi (titolare, secondo o con lezioni svolte in corsi di quell'anno); si somma al costo orario: con solo la tariffa vale il costo orario, con solo il forfait vale il forfait.
- [F-2026-10-22/RIMBORSI.1] Maturato per intero dall'inizio dell'anno: entra nel maturato del prospetto Rimborsi, nel Bilancio (costo maturato, da rimborsare, costo previsto, margine).
- [F-2026-10-22/RIMBORSI.1] La proposta del rimborso mensile resta solo oraria: il forfait si registra a mano come rimborso del mese scelto.
- [F-2026-10-22/RIMBORSI.1] Non entra nel "Costo insegnante" dei singoli corsi del Riepilogo Iscritti.
- Tecnica: nel Bilancio il forfait è una riga/indicatore separato (non ripartito sui corsi); colonna "Forfait annuo" nella lista Insegnanti; nel dialog Rimborsi accanto alla tariffa.

## Passi

| Passo | Cosa | Richiesta | File | Verifica | Stato |
|---|---|---|---|---|---|
| 1 | Campo `rimborsoForfettario` (entity, validazione, scheda Insegnanti con colonna, Profilo in sola lettura) | — | `entity/Insegnante`, `service/InsegnanteService`, `views/admin/InsegnantiView`, `views/ProfiloView`, test | `./mvnw test` | [x] |
| 2 | `BilancioService`: forfait nel prospetto e nel bilancio dell'anno; Bilancio e dialog Rimborsi lo mostrano; test | [F-2026-10-22/RIMBORSI.1] | `service/BilancioService`, `views/admin/BilancioDialog`, `views/admin/RimborsiDialog`, `BilancioServiceTest` | `./mvnw test` | [x] |
| 3 | CLAUDE.md e registro | — | `CLAUDE.md`, questo piano | — | [x] |

## Fuori ambito

- Importo diverso per anno scolastico; forfait nella proposta mensile; forfait ripartito sui corsi.

## Registro

- Passo 1: `./mvnw test` 198 test verdi; il test di creazione dell'insegnante imposta anche il forfait, il profilo dell'insegnante lo mostra in sola lettura; indici della lista Insegnanti aggiornati (nuova colonna "Forfait annuo").
- Passo 2: `./mvnw test` 199 test verdi; nuovo `forfaitAnnuo_siSommaAlCostoOrarioSoloNegliAnniConCorsi` (tariffa + forfait, proposta mensile solo oraria, anno senza corsi a zero, solo forfait senza tariffa).
- Non verificato nel browser. Produzione: `ddl-auto=update` aggiunge la colonna `rimborso_forfettario` (nullable), nessuno script.
