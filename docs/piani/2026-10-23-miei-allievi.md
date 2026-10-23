# Piano: "I miei allievi" per gli insegnanti

## Richiesta

[F-2026-10-23/ALLIEVI.1] Nuova sezione per gli insegnanti, "I miei allievi", con gli allievi dei loro corsi; cliccando su un allievo si vedono le sue informazioni: lezioni fatte, quante mancano e l'elenco delle lezioni.

## Analisi

- Corsi dell'insegnante: titolare o secondo insegnante (`Corso.haInsegnante`, query con LEFT JOIN su entrambi, `CorsoRepository.findDellInsegnante`); i corsi non attivi sono nascosti all'insegnante.
- Partecipanti = `Iscrizione` (gruppo, orario, `Iscrizione.ORDINE`); presenze = `Presenza` (una per lezione a cui l'allievo è atteso; creata con stato `PRESENTE` anche per le lezioni future).
- Stato della lezione: `Lezione.isCompletata()`, `isNonEffettuata()`, `isDaFare()`; lezioni previste per partecipante = `Corso.numeroLezioni`.
- `PresenzeAllievoDialog` (allievi in I Miei Corsi, genitori in I Miei Figli) mostra già le presenze di un allievo in un corso, ma: le lezioni future risultano "Presente" e sono contate tra le presenze; "Totale lezioni" comprende quelle da fare; non ha le note per lo staff.
- Menu: voci dell'insegnante "Le Mie Lezioni" (order 20) e "I miei rimborsi" (21).

### Decisioni

- [F-2026-10-23/ALLIEVI.1] Voce di menu "I miei allievi" solo per il ruolo INSEGNANTE, sola lettura (le presenze si gestiscono sempre da "Le Mie Lezioni").
- [F-2026-10-23/ALLIEVI.1] Elenco: una riga per allievo e corso, solo corsi attivi dell'insegnante (titolare o secondo); colonne allievo, corso, gruppo, orario, svolte/previste, mancanti, da fare; filtro per corso e ricerca rapida; card su mobile.
- [F-2026-10-23/ALLIEVI.1] Conteggi: svolte = lezioni completate a cui l'allievo era atteso (presente o assente); da fare = lezioni ancora in calendario; mancanti = previste (`numeroLezioni`) − svolte (mai sotto zero); presenze e assenze giustificate/ingiustificate solo sulle svolte; le lezioni non effettuate non contano.
- [F-2026-10-23/ALLIEVI.1] Nessun dato personale o contatto: solo nome, corso, gruppo e orario.
- [F-2026-10-23/ALLIEVI.1] Dettaglio in un dialog (clic sulla riga o pulsante `BOOK`) con i conteggi e l'elenco delle lezioni; l'insegnante vede anche le note per lo staff.
- [F-2026-10-23/ALLIEVI.1] Il dialog delle presenze è lo stesso per insegnanti, allievi e genitori: le lezioni non svolte mostrano "Da fare" e non contano tra presenze/assenze; in alto per tutti svolte, da fare e mancanti sulle previste.

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | Conteggio delle lezioni di un allievo in un corso (`service/ConteggioLezioni`) e servizio `MieiAllieviService`: righe allievo-corso dei corsi attivi dell'insegnante con i conteggi, lezioni di un allievo solo per l'insegnante del corso; test | `service/ConteggioLezioni.java`, `service/MieiAllieviService.java`, `IscrizioneRepository`, `PresenzaRepository`, `MieiAllieviServiceTest` | `./mvnw test` | [x] |
| 2 | `PresenzeAllievoDialog`: "Da fare" sulle lezioni non svolte, riepilogo con `ConteggioLezioni` (svolte, da fare, mancanti, presenze, assenze), colonna facoltativa delle note per lo staff; I Miei Corsi e I Miei Figli aggiornati | `views/PresenzeAllievoDialog.java`, `allievo/MieiCorsiView.java`, `genitore/FigliView.java`, `ViewAccessTest` | `./mvnw test` | [x] |
| 3 | Vista `insegnante/MieiAllieviView` ("I miei allievi"): griglia, filtro per corso, ricerca, card su mobile, dettaglio; test browserless (insegnante vede solo i suoi allievi, dettaglio, accesso negato agli altri ruoli) | `views/insegnante/MieiAllieviView.java`, `ViewAccessTest` | `./mvnw test`; browser | [ ] |
| 4 | Documentazione in `CLAUDE.md` | `CLAUDE.md` | rilettura | [ ] |
| 5 | Registro finale | questo piano | build e test | [ ] |

## Fuori ambito

- Contatti e dati personali dell'allievo (decisione sopra).
- Accesso di admin e segreteria alla vista (hanno già Gestione Allievi, Partecipanti e Riepilogo Iscritti).
- Modifica delle presenze dal dettaglio.

## Registro

_(da compilare durante l'esecuzione)_
