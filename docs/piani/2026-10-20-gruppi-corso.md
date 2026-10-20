# Piano: gruppi del corso al posto dei giorni per allievo

## Richiesta

Alcuni corsi hanno gruppi per giorno della settimana, altri no. Sostituire l'assegnazione dei giorni al singolo allievo con l'assegnazione (facoltativa) a un gruppo del corso, che ha un giorno e un orario di riferimento; i gruppi li vedono admin, segreteria e insegnanti ([F-2026-10-20/GRUPPI.1], [F-2026-10-20/GRUPPI.2], [F-2026-10-20/GRUPPI.3]).

## Analisi

- Oggi `Iscrizione.giorni` (tabella `iscrizione_giorni`, vuoto = tutti i giorni) è impostato in `PartecipantiDialog` con `CorsoService.impostaGiorni`. Tutto il resto passa da `Iscrizione.giorniEffettivi()` / `frequenta(data)`: generazione delle lezioni, iscrizione, lezioni aggiunte e ripristinate, conferma, I Miei Corsi / I Miei Figli (`CorsoService.giorniDellAllievo`), Riepilogo Iscritti. Conviene quindi tenere quei metodi e calcolarli dal gruppo: generazione, presenze e costi non cambiano.
- `orarioLezione` del partecipante resta (lezioni individuali in sequenza); il gruppo propone il suo orario.
- Produzione: MySQL con `ddl-auto=update` crea la nuova tabella e la colonna `gruppo_id`; la tabella `iscrizione_giorni` non viene eliminata, quindi i dati si convertono all'avvio leggendoli con SQL (in dev/test la tabella non esiste).
- Gli insegnanti vedono gli allievi solo in "Gestisci Presenze" (`ElencoLezioni`).

### Decisioni

- [F-2026-10-20/GRUPPI.2] Un gruppo ha un solo giorno (tra quelli del corso), un orario di riferimento e un nome facoltativo; si mostra "Martedì 17:00" o, con il nome, "Principianti – mar 17:00".
- [F-2026-10-20/GRUPPI.1] Ogni allievo appartiene al massimo a un gruppo del corso (facoltativo, solo se il corso ha gruppi); senza gruppo frequenta tutti i giorni del corso. Chi viene due giorni a settimana resta senza gruppo. I giorni per allievo spariscono.
- [F-2026-10-20/GRUPPI.1] Assegnando un gruppo all'allievo senza orario gli si propone l'orario del gruppo; resta modificabile.
- [F-2026-10-20/GRUPPI.1] Cambiando il gruppo di un allievo, il giorno di un gruppo o eliminando un gruppo si aggiornano solo le presenze delle lezioni da fare (come oggi con i giorni); le nuove date con "Genera Lezioni".
- [F-2026-10-20/GRUPPI.1] Conversione all'avvio: chi ha un solo giorno passa al gruppo di quel giorno (creato se manca, senza nome, con l'orario del corso); chi ne ha più d'uno resta senza gruppo (tutti i giorni).
- [F-2026-10-20/GRUPPI.3] Gruppi gestiti nel dialog Partecipanti (sezione "Gruppi") da admin e segreteria; eliminare un gruppo solo admin, i suoi allievi restano senza gruppo.
- [F-2026-10-20/GRUPPI.3] Gli insegnanti vedono il gruppo dell'allievo in "Gestisci Presenze", senza modifiche.
- [F-2026-10-20/GRUPPI.1] Il gruppo si vede anche in Riepilogo Iscritti (riga dell'iscritto, colonna "Giorni / Gruppo"), I Miei Corsi / I Miei Figli (al posto dei giorni) e nella conferma di iscrizione ("con lezione il martedì alle 17:00").
- Tecnica: un giorno del corso usato da un gruppo non si può togliere dal corso finché il gruppo lo usa (messaggio con il nome del gruppo).
- Tecnica: per i corsi non attivi gruppi e assegnazioni sono in sola lettura, come partecipanti e orari.

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | Entità `GruppoCorso` e repository; `Iscrizione.gruppo` al posto di `giorni` (`giorniEffettivi` dal gruppo); `CorsoService`: gruppi del corso, salva/elimina gruppo, assegna gruppo (orario proposto, presenze sincronizzate), blocco dei giorni usati, eliminazione del corso; Partecipanti: sezione Gruppi e colonna Gruppo; test | `entity/GruppoCorso`, `entity/Iscrizione`, `repository/GruppoCorsoRepository`, `service/CorsoService`, `views/admin/PartecipantiDialog`, `views/admin/GruppoDialog`, `views/Formati`, test | `./mvnw test` | [x] |
| 2 | Conversione all'avvio dei giorni per allievo in gruppi (`GruppiIniziali`), test | `GruppiIniziali`, `service/CorsoService`, test | `./mvnw test` | [x] |
| 3 | Gruppo in Gestisci Presenze, Riepilogo Iscritti, I Miei Corsi / I Miei Figli e conferma; test | `views/ElencoLezioni`, `service/LezioneService`, `views/admin/RiepilogoIscrittiView`, `views/allievo/MieiCorsiView`, `views/genitore/FigliView`, `service/DocumentiService`, test | `./mvnw test`, browser | [x] |
| 4 | CLAUDE.md e registro | `CLAUDE.md`, questo piano | — | [x] |

## Fuori ambito

- Lezioni collettive per gruppo (una lezione per gruppo con costo a ore): le lezioni restano individuali e i costi invariati.
- Un allievo in più gruppi dello stesso corso; gruppi con più giorni.
- Eliminazione della tabella `iscrizione_giorni` in produzione (resta vuota dopo la conversione; si può eliminare a mano).

## Registro

- Passo 1: `GruppoCorso` e `Iscrizione.gruppo`; `giorniEffettivi()` / `frequenta()` restano l'unico punto usato da generazione, iscrizione e lezioni aggiunte, quindi calendario, presenze e costi non sono cambiati. Partecipanti: sezione Gruppi, colonna Gruppo visibile solo con gruppi (gli indici delle colonne nei test dei corsi senza gruppi si spostano di uno).
- Passo 2: `GruppiIniziali` legge `iscrizione_giorni` con SQL e la svuota; testato creando la tabella in H2. Nella conversione l'orario dell'allievo non viene toccato.
- Passo 3: gruppo in Gestisci Presenze (colonna e sottotitolo della card), Riepilogo Iscritti (anche nella ricerca), I Miei Corsi / I Miei Figli e conferma.
- Verifica: `./mvnw test` 187 test verdi; browser (dev, segreteria, finestra stretta): sezione Gruppi, nuovo gruppo, card con il campo Gruppo; la scelta del giorno nel dialog del gruppo ora è senza etichetta laterale, come il metodo di pagamento.
- Produzione: al primo avvio `ddl-auto=update` crea `corso_gruppi` e `corso_allievi.gruppo_id`, poi `GruppiIniziali` converte i giorni già assegnati. Nessuno script SQL da eseguire.
