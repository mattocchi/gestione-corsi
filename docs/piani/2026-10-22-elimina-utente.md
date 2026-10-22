# Piano: eliminazione di un utente da Gestione Utenti

## Richiesta

Da admin, "Elimina" sulla riga di un utente in Gestione Utenti non elimina l'utente e non mostra nessun errore.

## Analisi

- Log di produzione (2026-10-22 17:47): `DataIntegrityViolationException … Cannot delete or update a parent row: a foreign key constraint fails [delete from users where id=?]`, gestita dal `DefaultErrorHandler` di Vaadin: nessun messaggio a video.
- `UtentiView.deleteUser` chiama `UserService.delete` subito al clic, senza conferma e senza gestire l'errore. Puntano a `users`: le schede `allievi`, `genitori`, `insegnanti` (`user_id`) e i link di `recupero_password`.
- Gli altri controlli di eliminazione esistono già: allievo con pagamenti (`AllievoService.delete`), insegnante con corsi, lezioni o rimborsi (`InsegnanteService.motivoPerNonEliminare`), genitore (figli che restano senza genitore, `GenitoreService.elimina`).

### Decisioni

- Eliminando un utente si eliminano anche le sue schede, se non hanno collegamenti: allievo senza pagamenti, insegnante senza corsi/lezioni/rimborsi, genitore (i figli restano senza genitore); e i suoi link di recupero password. Se una scheda ha collegamenti non si elimina nulla e il messaggio dice cosa sistemare prima (decisione dell'utente, 2026-10-22).
- Tecnica: dialog di conferma che elenca le schede che saranno eliminate; l'admin non può eliminare il proprio utente; ogni errore diventa una notifica.

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | `SchedeRuoliService.schedeDaEliminare` / `eliminaUtente` (schede libere, link di recupero, blocco con motivo, non sé stessi); test | `service/SchedeRuoliService`, `SchedeRuoliServiceTest` | `./mvnw test` | [ ] |
| 2 | Gestione Utenti: conferma con l'elenco delle schede, errori come notifica; test | `views/admin/UtentiView`, `ViewAccessTest` | `./mvnw test` | [ ] |
| 3 | CLAUDE.md e registro | `CLAUDE.md`, questo piano | — | [ ] |

## Fuori ambito

- Eliminare in cascata pagamenti, corsi, lezioni o rimborsi.

## Registro
