# Piano: schede di insegnante e genitore create dal ruolo

## Richiesta

Un utente (creato con l'inserimento rapido come allievo) a cui sono stati assegnati i ruoli Insegnante e Genitore non compare negli elenchi Insegnanti e Genitori ([F-2026-10-19/SCHEDE.1]).

## Analisi

- Gli elenchi Insegnanti e Genitori mostrano le schede `Insegnante` / `Genitore` (tariffa, specializzazione, figli, anagrafica), non gli utenti con il ruolo. Le schede nascono solo da "Nuovo Insegnante" / "Nuovo Genitore" (`InsegnanteService.salva`, `GenitoreService.salva`); in Gestione Utenti (`UtentiView` → `UserService.save`) il ruolo cambia solo i permessi.
- Le schede non hanno campi obbligatori oltre all'utente (nome e cognome sono dell'utente dal 17/04/2026, prima della produzione): una scheda vuota si può creare. `Anagrafica.copia` copia codice fiscale, nascita e indirizzo tra schede.
- Avvio: i `CommandLineRunner` esistenti (`FestivitaIniziali`, `InsegnantiPresentiIniziali`) sono il modello per sistemare i dati di produzione senza script SQL.

### Decisioni

- [F-2026-10-19/SCHEDE.1] Salvando un utente in Gestione Utenti con il ruolo INSEGNANTE o GENITORE, se manca la scheda viene creata subito; tariffa, specializzazione e figli si completano poi negli elenchi. Togliere il ruolo non cancella la scheda.
- [F-2026-10-19/SCHEDE.1] La nuova scheda copia codice fiscale, data e luogo di nascita e indirizzo da una scheda già esistente dello stesso utente (allievo, insegnante o genitore); poi ognuna si modifica separatamente.
- [F-2026-10-19/SCHEDE.1] All'avvio (tutti i profili) si creano le schede mancanti per gli utenti che hanno già il ruolo, come quello segnalato.
- Fuori ambito (non richiesto): lo stesso per il ruolo ALLIEVO (la scheda allievo resta creata da Gestione Allievi o dall'inserimento rapido).

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | `SchedeRuoliService` (crea le schede mancanti con l'anagrafica copiata), salvataggio da Gestione Utenti, avvio `SchedeRuoliIniziali`, test | `service/SchedeRuoliService`, `UtentiView`, `SchedeRuoliIniziali`, test | `./mvnw test` | [x] |
| 2 | CLAUDE.md e registro | `CLAUDE.md`, questo piano | — | [ ] |

## Fuori ambito

- Scheda allievo creata dal ruolo ALLIEVO.
- Scelta di un utente esistente nei dialog "Nuovo Insegnante" / "Nuovo Genitore".

## Registro

