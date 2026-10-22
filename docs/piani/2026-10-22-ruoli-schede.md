# Piano: schede dai ruoli anche per gli allievi e incongruenze

## Richiesta

Assegnando il ruolo ALLIEVO a un utente non si crea la scheda allievo: controllare che aggiungendo o togliendo ruoli in Gestione Utenti le schede siano coerenti ([F-2026-10-22/RUOLI.1]) ed evidenziare le incongruenze ([F-2026-10-22/RUOLI.2]).

## Analisi

- `SchedeRuoliService.salvaUtente` (Gestione Utenti) e `completaTutte` (all'avvio, `SchedeRuoliIniziali`) creano le schede mancanti solo per INSEGNANTE e GENITORE: per scelta del 2026-10-19 il ruolo ALLIEVO non creava la scheda. Togliere un ruolo non elimina la scheda.
- Produzione (controllo in sola lettura del 2026-10-22): 1 utente con il ruolo ALLIEVO senza scheda; nessuna scheda senza ruolo, nessun insegnante o genitore senza scheda.
- La sezione si chiama "RUOLI E SCHEDE": chiave `RUOLI`.

### Decisioni

- [F-2026-10-22/RUOLI.1] Anche il ruolo ALLIEVO crea la scheda allievo (anagrafica copiata da un'altra scheda dello stesso utente, se c'è; senza genitore); all'avvio si creano le schede mancanti, compresi gli allievi.
- [F-2026-10-22/RUOLI.1] Togliendo un ruolo la scheda resta ed è segnalata come incongruenza.
- [F-2026-10-22/RUOLI.2] Gestione Utenti: badge di avviso sulla riga per ogni incongruenza ("Allievo senza scheda", "Scheda insegnante senza ruolo", …), filtro "Solo incongruenze", pulsante "Crea schede mancanti" (non tocca le schede senza ruolo).

## Passi

| Passo | Cosa | Richiesta | File | Verifica | Stato |
|---|---|---|---|---|---|
| 1 | Scheda allievo dal ruolo ALLIEVO (salvataggio e avvio); test | [F-2026-10-22/RUOLI.1] | `service/SchedeRuoliService`, `SchedeRuoliIniziali`, `SchedeRuoliServiceTest` | `./mvnw test` | [x] |
| 2 | `SchedeRuoliService.incongruenze()`; badge, filtro e pulsante in Gestione Utenti; test | [F-2026-10-22/RUOLI.2] | `service/SchedeRuoliService`, `views/admin/UtentiView`, test | `./mvnw test` | [x] |
| 3 | CLAUDE.md e registro | — | `CLAUDE.md`, questo piano | — | [x] |

## Fuori ambito

- Eliminare la scheda quando si toglie un ruolo; card delle incongruenze in dashboard.

## Registro

- Passo 1: `./mvnw test` 207 test verdi; nuovo `ruoloAllievo_creaLaSchedaConLAnagraficaDelGenitore` (un genitore con il ruolo ALLIEVO riceve la scheda allievo con codice fiscale e indirizzo della scheda genitore, senza genitore).
- Passo 2: `./mvnw test` 208 test verdi; nuovo `admin_incongruenzeTraRuoliESchede` (badge "Scheda genitore senza ruolo" e "Allievo senza scheda", filtro, pulsante che crea solo la scheda mancante); indici delle colonne di Gestione Utenti aggiornati.
- Non verificato nel browser. Nessuna modifica allo schema. Al primo avvio in produzione si crea la scheda dell'utente con il ruolo ALLIEVO che ne è privo (1 utente al controllo del 2026-10-22).
