# Piano: anni scolastici amministrabili

## Richiesta

[F-2026-10-11/ANNO.1] Rendere la lista di valori di *Anno Scolastico* (scelta nel dialog del corso) amministrabile.

## Analisi

- Oggi `entity/AnnoScolastico` è una classe di utilità: `di(LocalDate)` (settembre–agosto) ed `elenco(oggi, valoreAttuale)`, che calcola anno precedente, corrente e successivo più il valore già assegnato al corso. `CorsiView` usa `elenco` per la ComboBox e propone `di(dataInizio)` finché il campo è vuoto.
- `Corso.annoScolastico` è una stringa "2026/2027" (obbligatoria nel dialog), usata anche da `RicevutaView`, dai filtri di `CorsiView` e di `ElencoPagamenti` (che leggono i valori dai corsi, non dall'elenco: restano invariati).
- Scelta tecnica: nuova entità `AnnoScolastico` (tabella `anni_scolastici`: `id`, `nome` unico, `attivo`) che prende il posto della classe di utilità (mantiene il metodo statico `di`); `Corso.annoScolastico` resta una stringa senza chiave esterna (nessuna rinomina prevista, quindi nessun disallineamento possibile; nessuna migrazione dei corsi esistenti). In produzione `ddl-auto=update` crea la nuova tabella: nessuno script SQL manuale.

### Decisioni

- [F-2026-10-11/ANNO.1] Gestione in una nuova vista di menu "Anni scolastici" (griglia + dialog), come le altre viste di amministrazione.
- [F-2026-10-11/ANNO.1] Accesso: ADMIN e SEGRETERIA; l'eliminazione è solo dell'ADMIN (pulsante nascosto alla segreteria).
- [F-2026-10-11/ANNO.1] Operazioni: aggiungi, attiva/disattiva, elimina solo se nessun corso lo usa; niente rinomina. Un anno disattivato non compare tra le scelte del corso, ma resta sui corsi che lo hanno già (e nel loro dialog).
- [F-2026-10-11/ANNO.1] Formato obbligatorio `AAAA/AAAA` con anni consecutivi (es. 2026/2027), valore unico.
- [F-2026-10-11/ANNO.1] Popolamento: all'avvio (tutti i profili), se la tabella è vuota, si inseriscono anno precedente, corrente e successivo più gli anni già usati dai corsi esistenti.
- [F-2026-10-11/ANNO.1] Dialog del corso: l'anno della data di inizio è proposto solo se è nell'elenco ed è attivo; altrimenti il campo resta vuoto.

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | Entità `AnnoScolastico` (id, nome unico, attivo; `di` e `valido` statici), `AnnoScolasticoRepository`, `AnnoScolasticoService` (elenco, scelte per il corso, salva con validazione formato/unicità, attiva/disattiva, elimina se non usato, popolamento iniziale) e popolamento all'avvio; test del service | `entity/AnnoScolastico.java`, `repository/AnnoScolasticoRepository.java`, `repository/CorsoRepository.java`, `service/AnnoScolasticoService.java`, `AnniScolasticiIniziali.java`, test | `./mvnw test` | [x] |
| 2 | `CorsiView`: scelte dell'anno dal service (attivi + valore del corso), proposta dalla data di inizio solo se attivo; test | `views/admin/CorsiView.java`, `ViewAccessTest` | `./mvnw test` | [x] nello stesso commit del passo 1: senza `AnnoScolastico.elenco` CorsiView non compilava |
| 3 | Vista `AnniScolasticiView` (menu, admin/segreteria, Nuovo, attiva/disattiva, Elimina solo admin e solo se non usato, mobile 390px); test browserless; chiude la richiesta | `views/admin/AnniScolasticiView.java`, `ViewAccessTest`, `docs/FUNZIONALITA.md` | `./mvnw test`, browser | [x] |
| 4 | `CLAUDE.md`: nota sugli anni scolastici | `CLAUDE.md` | rilettura | [ ] |

## Fuori ambito

- Rinomina di un anno (e aggiornamento dei corsi che lo usano).
- I filtri per anno di `CorsiView` ed `ElencoPagamenti` continuano a mostrare gli anni presenti nei corsi.
- `.idea/.copilot-plugin` (modifica locale preesistente) non incluso.

## Registro
