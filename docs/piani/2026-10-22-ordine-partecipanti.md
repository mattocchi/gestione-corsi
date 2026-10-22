# Piano: ordine dei partecipanti per gruppo, orario e inserimento

## Richiesta

Ordinare i partecipanti dei corsi per gruppo e poi per orario, quando ci sono; altrimenti per ordine di inserimento invece che alfabetico ([F-2026-10-22/PARTECIPANTI.1]).

## Analisi

- `Iscrizione.PER_ORARIO` (orario, senza orario in fondo, poi nome) è usato solo da `CorsoService.findPartecipanti` → tabella del dialog Partecipanti.
- Gestisci Presenze (`ElencoLezioni`) mostra le presenze nell'ordine di `PresenzaService.findByLezione` (nessun ordine esplicito); Riepilogo Iscritti ordina gli iscritti di ogni corso per nome (`PagamentoService.PER_ALLIEVO_E_CORSO` in `iscrittiPerCorso`).
- `Iscrizione` (tabella `corso_allievi`, chiave corso/allievo) non registra quando è stata creata: l'ordine di inserimento non esiste nei dati.

### Decisioni

- [F-2026-10-22/PARTECIPANTI.1] Ordine: gruppo (`GruppoCorso.PER_GIORNO`, chi non ha gruppo in fondo), poi orario di lezione (chi non ce l'ha dopo), poi ordine di inserimento.
- [F-2026-10-22/PARTECIPANTI.1] Nuovo campo `Iscrizione.iscrittoIl` (colonna `iscritto_il`, data e ora, impostata alla creazione dell'iscrizione). Le iscrizioni esistenti restano senza (nessuno script: `ddl-auto=update` aggiunge la colonna nullable), vengono prima delle nuove e tra loro seguono l'ordine di creazione dell'allievo (id).
- [F-2026-10-22/PARTECIPANTI.1] Stesso ordine in Partecipanti, Gestisci Presenze e Riepilogo Iscritti (sotto il corso e sotto ogni gruppo).
- Tecnica: `Iscrizione.ORDINE` sostituisce `PER_ORARIO`; a parità di giorno, orario e nome due gruppi restano separati per id. In Gestisci Presenze le presenze di allievi non più iscritti vanno in fondo, per nome.

## Passi

| Passo | Cosa | Richiesta | File | Verifica | Stato |
|---|---|---|---|---|---|
| 1 | `Iscrizione.iscrittoIl` e `Iscrizione.ORDINE`; `findPartecipanti` (dialog Partecipanti); test | — | `entity/Iscrizione`, `service/CorsoService`, test | `./mvnw test` | [x] |
| 2 | Stesso ordine in Gestisci Presenze (`LezioneService.presenzeInOrdine`) e Riepilogo Iscritti; test | [F-2026-10-22/PARTECIPANTI.1] | `service/LezioneService`, `views/ElencoLezioni`, `service/PagamentoService`, test | `./mvnw test` | [ ] |
| 3 | CLAUDE.md e registro | — | `CLAUDE.md`, questo piano | — | [ ] |

## Fuori ambito

- Mostrare la data di iscrizione nell'interfaccia.
- Ricostruire la data delle iscrizioni già esistenti.

## Registro
