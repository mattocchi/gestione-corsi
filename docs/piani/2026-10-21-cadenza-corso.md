# Piano: nome dei gruppi e corsi ogni 2 settimane

## Richiesta

Gruppi del corso con un nome oltre al giorno; caso d'uso: propedeutica il sabato ogni 15 giorni con i gruppi "Piccoli" alle 10 e "Grandi" alle 11 ([F-2026-10-21/GRUPPI.1]).

## Analisi

- Il nome del gruppo esiste già (`GruppoCorso.nome`, campo "Nome" di `GruppoDialog`, mostrato da `Formati.gruppo` come "Piccoli – sab 10:00"), anche in produzione (JAR del 2026-10-20). Nessun vincolo impedisce due gruppi nello stesso giorno: i due gruppi fanno lezione negli stessi sabati, ognuno con il suo orario.
- Manca la cadenza ogni 2 settimane: `CorsoService.generaLezioni` scorre i giorni dalla data di inizio e crea una lezione in ogni giorno del corso, ogni settimana (salvo festività, già esistenti e giorni in cui nessuno ha bisogno di lezioni).
- Gestione Corsi non mostra i giorni del corso nella tabella, solo nel dialog (`CheckboxGroup` "Giorni Settimana").

### Decisioni

- [F-2026-10-21/GRUPPI.1] Nome del gruppo: già presente, nessuna modifica.
- [F-2026-10-21/GRUPPI.1] Nuovo campo del corso "Cadenza": "Ogni settimana" (default, corsi esistenti invariati) o "Ogni 2 settimane" (`SegmentedControl` nel dialog del corso). Nessun valore libero.
- [F-2026-10-21/GRUPPI.1] Le settimane alterne si contano dalla data di inizio: per ogni giorno del corso la prima lezione è il primo di quei giorni dalla data di inizio, poi ogni 2 settimane.
- [F-2026-10-21/GRUPPI.1] Una lezione che cade in una festività si sposta alla settimana dopo e il ritmo riparte da lì (con la cadenza settimanale il comportamento resta quello di oggi).
- [F-2026-10-21/GRUPPI.1] La cadenza si vede solo in Gestione Corsi: nuova colonna "Giorni" (es. "sab, ogni 2 settimane") anche nella card mobile.
- Tecnica: colonna `cadenza_settimane` (intero, nullable, aggiunta da `ddl-auto=update`; null = ogni settimana): nessuno script di migrazione. Come per gli altri campi, la cadenza modifica le lezioni solo con "Genera Lezioni". Le date già conservate alla rigenerazione (svolte, non effettuate) contano come lezione fatta per il ritmo.

## Passi

| Passo | Cosa | Richiesta | File | Verifica | Stato |
|---|---|---|---|---|---|
| 1 | `Corso.cadenzaSettimane`; `generaLezioni` con cadenza e festività che spostano di una settimana; test | — | `entity/Corso`, `service/CorsoService`, `CorsoServiceTest` | `./mvnw test` | [ ] |
| 2 | Gestione Corsi: "Cadenza" nel dialog, colonna/campo "Giorni"; `Formati.giorniECadenza`; test | [F-2026-10-21/GRUPPI.1] | `views/admin/CorsiView`, `views/Formati`, `ViewAccessTest`, `FormatiTest` | `./mvnw test` | [ ] |
| 3 | CLAUDE.md e registro | — | `CLAUDE.md`, questo piano | — | [ ] |

## Fuori ambito

- Cadenza diversa per gruppo, cadenze oltre le 2 settimane.
- Cadenza mostrata ad allievi, genitori, insegnanti e nella conferma di iscrizione.

## Registro
