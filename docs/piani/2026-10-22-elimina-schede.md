# Piano: eliminare la scheda di genitore o insegnante mantenendo l'utente

## Richiesta

Poter eliminare la scheda Genitore mantenendo l'utente quando ha altri ruoli ([F-2026-10-22/GENITORI.3]); lo stesso per gli insegnanti, deciso nel giro di domande.

## Analisi

- Negli elenchi Genitori e Insegnanti non c'è un'azione di eliminazione; in Gestione Utenti "Elimina" cancella l'intero utente. Si elimina solo l'allievo (Gestione Allievi, solo admin).
- Collegamenti: un genitore ha i figli (`Allievo.genitore`); un insegnante può avere corsi (titolare o secondo), lezioni a cui era presente (`lezione_insegnanti`) e rimborsi.
- Le conferme di eliminazione del progetto sono dialog con "Annulla" ed "Elimina" (es. gruppi in `PartecipantiDialog`).

### Decisioni

- [F-2026-10-22/GENITORI.3] Azione "Elimina" (`TRASH`, rossa) nell'elenco Genitori solo per l'admin, con dialog di conferma che dice quanti figli restano senza genitore.
- [F-2026-10-22/GENITORI.3] Si elimina solo la scheda, mai l'utente: i figli restano senza genitore, all'utente si toglie il ruolo GENITORE; se era il suo unico ruolo l'utente resta senza ruoli (la conferma lo segnala; si elimina, se serve, da Gestione Utenti).
- [F-2026-10-22/GENITORI.3] Lo stesso negli Insegnanti (solo admin): eliminazione bloccata con un messaggio se l'insegnante ha corsi, lezioni o rimborsi; all'utente si toglie il ruolo INSEGNANTE.

## Passi

| Passo | Cosa | Richiesta | File | Verifica | Stato |
|---|---|---|---|---|---|
| 1 | `GenitoreService.elimina` e azione "Elimina" con conferma in Genitori; test | [F-2026-10-22/GENITORI.3] | `service/GenitoreService`, `views/admin/GenitoriView`, test | `./mvnw test` | [x] |
| 2 | `InsegnanteService.elimina` (bloccata se ha collegamenti) e azione in Insegnanti; test | [F-2026-10-22/GENITORI.3] | `service/InsegnanteService`, repository, `views/admin/InsegnantiView`, test | `./mvnw test` | [x] |
| 3 | CLAUDE.md e registro | — | `CLAUDE.md`, questo piano | — | [ ] |

## Fuori ambito

- Eliminazione dell'utente insieme alla scheda.

## Registro
