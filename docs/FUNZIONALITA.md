# RICHIESTE DI FUNZIONALITÀ (DAI PROMPT)

Richieste di funzionalità fatte direttamente nei prompt a Claude Code, registrate prima di essere implementate.
Stesso formato di `RICHIESTE.md`: sezioni per data e per ambito, una voce `* [ ]` per richiesta, testo dell'utente riportato fedelmente.
Riferimento di ogni voce: `[F-AAAA-MM-GG/AMBITO.n]` (vedi `CLAUDE.md`, *Feature requests from prompts*).

## FUNZIONALITA del 11-10-2026

### ANNO SCOLASTICO

* [x] rendi la lista di valori di Anno Scolastico amministrabile da Admin _(completata 2026-10-11)_
  _Chiarimento 2026-10-11: nuova vista di menu "Anni scolastici" per ADMIN e SEGRETERIA (eliminazione solo ADMIN e solo se nessun corso usa l'anno); aggiungi e attiva/disattiva (un anno disattivato non si può più scegliere nei corsi ma resta su quelli che lo hanno), niente rinomina; formato AAAA/AAAA con anni consecutivi, unico; elenco popolato all'avvio, se vuoto, con anno precedente, corrente e successivo più gli anni dei corsi esistenti; nel corso l'anno della data di inizio è proposto solo se attivo._

### CORSI

* [x] aggiungi ai corsi la possibilita di inserire oltre al prezzo pieno, attuale, anche un prezzo con pre-iscrizione che in genere è il 15% rispetto al prezzo pieno ma lo assegnamo a mano _(completata 2026-10-11)_
  _Chiarimento 2026-10-11: 15% = sconto (valore tipico 85% del costo totale), indicato solo come aiuto, nessuna proposta automatica; prezzo solo informativo (non cambia la quota dovuta, per applicarlo si usa la quota personalizzata); facoltativo, tra 0 e il costo totale; l'etichetta "Costo Totale" resta; mostrato nel dialog del corso, in "Quota e pagamenti" e a genitori e allievi nelle liste dei corsi, con costo totale e prezzo effettivamente applicato._

### REGISTRO INSEGNANTE

* [x] Per l'insegnante semplifichiamo l'accesso al registro. Se quando si logga ha un solo corso lo portiamo all'elenco delle lezioni direttamente. _(completata 2026-10-11)_
  _Chiarimento 2026-10-11: solo al login (la Home resta raggiungibile dal menu), solo per chi ha unicamente il ruolo INSEGNANTE e un solo corso attivo._
* [x] Se la prima lezione in lista è oggi vale la pena enfatizzare con un badge che è oggi. _(completata 2026-10-11)_
  _Chiarimento 2026-10-11: badge "Oggi" (blu pieno) su tutte le lezioni di oggi, accanto alla data (su mobile nel riepilogo)._
* [x] Fare lo stesso con colori diversi per evidenziare settimana scorsa, se ne ha ancora una da compilare o settimana prossima per quelle programmate settimana prossima. _(completata 2026-10-11)_
  _Chiarimento 2026-10-11: settimane di calendario lun–dom; "Settimana scorsa" arancione solo per le lezioni non completate, "Settimana prossima" grigio; in più "In ritardo" rosso per le non completate più vecchie e "Questa settimana" blu chiaro per gli altri giorni della settimana corrente._

### LEZIONI NON EFFETTUATE

* [ ] Dobbiamo dare all'insegnante la possibilità di indicare che una lezione non è stata fatta, perché ad esempio era assente.
* [ ] E quindi la possibilità di inserire una nuova lezione qualora quella programmata non fosse stata effettuata. In questo modo può inserire delle nuove e gestione le presenze e indicare quelle programmate  che non ha potuto fare
