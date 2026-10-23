# Piano: numero della lezione per gruppo

## Richiesta

Nei corsi con due gruppi (es. lun e ven) la lezione numero 1 deve essere quella della prima settimana per entrambi i gruppi, così che ognuno abbia le sue 30 lezioni; se un gruppo salta una lezione la recupera per arrivare a 30 ([F-2026-10-23/NUMERO.1]).

## Analisi

- `CorsoService.generaLezioni` assegna un contatore unico per corso in ordine di data (con i gruppi lun/ven: lun 1, 3, 5…, ven 2, 4, 6…); il numero di lezioni per partecipante è già rispettato (ogni gruppo ha le sue 30 date).
- `LezioneService.aggiungi`: un recupero (`recuperoDi`) prende il numero della lezione saltata; un'altra lezione aggiunta prende "massimo + 1".
- Il numero è solo un'etichetta: presenze e recuperi sono legati alla lezione (id), non al numero. Si mostra in `ElencoLezioni` (colonna "N. Lezione", titoli dei dialog) e in `PresenzeAllievoDialog`.
- La sezione si chiama "NUMERO LEZIONE": chiave `NUMERO`.

### Decisioni

- [F-2026-10-23/NUMERO.1] Nei corsi con gruppi la numerazione è per giorno della settimana (ogni giorno di gruppo conta 1, 2, 3…; due gruppi nello stesso giorno condividono la lezione e il numero). Una lezione non effettuata tiene il numero; il suo recupero prende lo stesso numero. Una lezione aggiunta (non recupero) prende il numero successivo del suo giorno.
- [F-2026-10-23/NUMERO.1] I corsi senza gruppi restano come oggi.
- [F-2026-10-23/NUMERO.1] Si rinumerano tutte le lezioni dei corsi con gruppi, anche quelle svolte: all'avvio e ogni volta che cambiano le lezioni (generazione, aggiunta, eliminazione) o i gruppi.
- [F-2026-10-23/NUMERO.1] Nei corsi con gruppi accanto al numero si mostra il gruppo (nome, se uno solo con nome in quel giorno) o il giorno: "5 · Piccoli", "5 · lun".
- Tecnica: la numerazione si ricalcola dalle lezioni (`NumerazioneLezioni.rinumera(corsoId)`), per date crescenti; se si elimina l'ultimo gruppo il corso torna a un contatore unico per data.

## Passi

| Passo | Cosa | Richiesta | File | Verifica | Stato |
|---|---|---|---|---|---|
| 1 | `NumerazioneLezioni` (ricalcolo per giorno nei corsi con gruppi) chiamata da generazione, lezioni aggiunte/eliminate, gruppi e all'avvio; test | — | `service/NumerazioneLezioni`, `CorsoService`, `LezioneService`, avvio, test | `./mvnw test` | [ ] |
| 2 | Etichetta "N · gruppo/giorno" in Lezioni, Le Mie Lezioni e presenze dell'allievo; test | [F-2026-10-23/NUMERO.1] | `views/ElencoLezioni`, `views/PresenzeAllievoDialog`, `views/Formati`, test | `./mvnw test` | [ ] |
| 3 | CLAUDE.md e registro | — | `CLAUDE.md`, questo piano | — | [ ] |

## Fuori ambito

- Numerazione per giorno nei corsi senza gruppi.

## Registro
