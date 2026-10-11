# Piano: lezioni non effettuate e nuove lezioni dell'insegnante

## Richiesta

[F-2026-10-11/LEZIONI.1] L'insegnante può indicare che una lezione programmata non è stata fatta (es. era assente).
[F-2026-10-11/LEZIONI.2] Può inserire nuove lezioni (anche al posto di quelle non effettuate) e gestirne le presenze.

## Analisi

- `Lezione`: `data`, orari, `numeroLezione`, `note`, `completata`, presenze (una per partecipante, default Presente). Le crea solo `CorsoService.generaLezioni` ("Genera Lezioni" di admin/segreteria), che cancella tutte le lezioni non completate e le ricrea dalla 1 alla N (ricreando anche i numeri delle lezioni già completate: doppioni).
- `CorsoService.iscrivi` / `rimuoviIscrizione` sincronizzano le presenze delle lezioni non completate.
- L'insegnante usa `LezioniInsegnanteView` (filtro Da fare / Completate / Tutte, dialog presenze con "Segna Completata"); allievi e genitori vedono le presenze in `PresenzeAllievoDialog` (con statistiche); la Home dell'insegnante conta lezioni fatte / da fare (`DashboardService`).
- Scelte tecniche: su `Lezione` i campi `nonEffettuata` (boolean), `motivoNonEffettuata`, `aggiunta` (inserita dall'insegnante, non programmata) e `recuperoDi` (lezione non effettuata che recupera, facoltativa). Nuove colonne aggiunte da `ddl-auto=update` (boolean NOT NULL con valore 0 sulle righe esistenti): nessuno script SQL. Le operazioni dell'insegnante passano da `LezioneService` con lo username, che verifica che il corso sia suo e attivo. Una lezione non effettuata non si può completare; per ripristinarla va prima eliminato il suo recupero. Numero della lezione: il recupero prende quello della lezione saltata, una lezione libera il successivo al massimo del corso. "Genera Lezioni" non ricrea i numeri delle lezioni programmate conservate (corregge anche i doppioni delle completate).

### Decisioni

- [F-2026-10-11/LEZIONI.1] Pulsante "Non effettuata" nel dialog della lezione con motivo facoltativo a testo libero.
- [F-2026-10-11/LEZIONI.1] Presenze della lezione non effettuata conservate ma escluse: allievi e genitori vedono la riga "Lezione non effettuata" e le statistiche non la contano.
- [F-2026-10-11/LEZIONI.1][F-2026-10-11/LEZIONI.2] Solo l'insegnante del corso; admin e segreteria invariati.
- [F-2026-10-11/LEZIONI.1] Una lezione non effettuata si può ripristinare (torna da fare).
- [F-2026-10-11/LEZIONI.1] "Le Mie Lezioni": filtro Da fare / Completate / Non effettuate / Tutte, badge di stato grigio "Non effettuata"; non contano tra le da fare e non hanno il badge "In ritardo".
- [F-2026-10-11/LEZIONI.1] Home dell'insegnante: escluse da fatte/da fare, nuovo riquadro "Lezioni non effettuate".
- [F-2026-10-11/LEZIONI.2] "Recupera" sulla lezione non effettuata (collegata, stesso numero) e "Nuova lezione" libera sopra la lista (scelta del corso se ne ha più di uno); data obbligatoria, orario proposto dal corso; presenze dei partecipanti attuali (default Presente). La lezione di recupero mostra "Recupero" accanto al numero.
- [F-2026-10-11/LEZIONI.2] Una lezione aggiunta (recupero o libera) non completata si può eliminare; le programmate no.
- [F-2026-10-11/LEZIONI.1][F-2026-10-11/LEZIONI.2] "Genera Lezioni" conserva completate, non effettuate e lezioni aggiunte; ricrea solo le programmate ancora da fare.

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | `Lezione`: nuovi campi; `LezioneService`: segna non effettuata, ripristina, aggiungi (recupero/libera, con presenze), elimina aggiunta (controllo insegnante e corso attivo); `CorsoService`: `generaLezioni` conserva e non duplica, `iscrivi` salta le non effettuate; conteggi della dashboard; test dei service | `entity/Lezione.java`, `repository/LezioneRepository.java`, `service/LezioneService.java`, `service/CorsoService.java`, `service/DashboardService.java`, test | `./mvnw test` | [ ] |
| 2 | Non effettuate nella UI: dialog della lezione (Non effettuata con motivo, Ripristina), filtro e badge in `LezioniInsegnanteView`, `QuandoLezione`, riga "Lezione non effettuata" e statistiche in `PresenzeAllievoDialog`, riquadro in Home; test | `views/insegnante/*`, `views/PresenzeAllievoDialog.java`, `views/HomeView.java`, test, `docs/FUNZIONALITA.md` | `./mvnw test` | [ ] |
| 3 | Nuove lezioni nella UI: "Nuova lezione" e "Recupera" (dialog data/orario/corso), "Recupero" accanto al numero, Elimina per le aggiunte; test | `views/insegnante/LezioniInsegnanteView.java`, test, `docs/FUNZIONALITA.md` | `./mvnw test` | [ ] |
| 4 | `CLAUDE.md` e registro del piano | `CLAUDE.md`, piano | rilettura | [ ] |

## Fuori ambito

- Vista delle lezioni per admin e segreteria.
- Spostamento di data di una lezione programmata (si usa non effettuata + recupero).
- `.idea/.copilot-plugin` (modifica locale preesistente) non incluso.

## Registro
