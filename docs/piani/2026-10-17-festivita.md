# Piano: festività escluse dalla generazione delle lezioni

## Richiesta

[F-2026-10-17/FESTIVITA.1–2] Una sezione per gestire le festività italiane, fisse ogni anno e a data variabile, così che
"Genera Lezioni" salti le festività note.

## Analisi

- `CorsoService.generaLezioni` scorre i giorni dalla data di inizio e assegna la lezione N all'N-esimo giorno della
  settimana del corso; le lezioni programmate già fatte, non effettuate o aggiunte restano e i loro numeri non vengono
  ricreati. Saltando le festività (senza consumare un numero) il numero di lezioni resta quello del corso e la fine slitta.
- Modello di riferimento: `AnnoScolastico` (entità, `AnnoScolasticoService`, `AnniScolasticiIniziali` che popola la
  tabella vuota all'avvio in tutti i profili, `admin/AnniScolasticiView` per admin/segreteria con eliminazione solo admin).
- `DataInitializer` (`@Order(0)`) genera le lezioni dei corsi di prova: il precaricamento delle festività deve girare
  prima, così anche i dati dev saltano le festività (possibile aggiornamento dei test che contano date di lezioni).
- Produzione: nuova tabella `festivita` creata da `ddl-auto=update`, nessuno script manuale; il precaricamento parte al
  primo avvio perché la tabella è vuota.
- Pasqua: algoritmo gregoriano anonimo (Meeus/Jones/Butcher); Pasquetta = Pasqua + 1.

### Decisioni

- [F-2026-10-17/FESTIVITA.1] Tre tipi: *Ogni anno* (giorno/mese, facoltativamente fino a un altro giorno/mese, anche a cavallo d'anno, es. 23/12–6/1), *Date precise* (una data o un periodo dal…al), *Pasqua* e *Pasquetta* calcolate ogni anno. Niente periodi relativi alla Pasqua: le vacanze di Pasqua si inseriscono come date precise.
- [F-2026-10-17/FESTIVITA.1] Vista "Festività" per admin e segreteria (aggiunta, modifica, attiva/disattiva); eliminazione solo admin.
- [F-2026-10-17/FESTIVITA.1] Precaricate all'avvio se la tabella è vuota (tutti i profili): 1/1, 6/1, Pasqua, Pasquetta, 25/4, 1/5, 2/6, 15/8, 4/10, 1/11, 8/12, 25/12, 26/12; ognuna disattivabile.
- [F-2026-10-17/FESTIVITA.2] Le festività attive valgono per tutti i corsi; "Genera Lezioni" salta i giorni festivi senza consumare numeri di lezione.
- [F-2026-10-17/FESTIVITA.2] Lezioni già generate: si sistemano con "Genera Lezioni"; nelle Lezioni un badge "Festività" (con il nome nel tooltip) segnala le lezioni da fare in un giorno festivo.
- [F-2026-10-17/FESTIVITA.2] Nuova lezione o recupero a mano in un giorno festivo: consentiti, con avviso nel dialog.

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | Entità `Festivita` (+ tipo), repository, `FestivitaService` (elenco, salva con validazione, attiva, elimina, `calendario()` delle attive con Pasqua calcolata) | `entity/`, `repository/`, `service/` | test unitari (Pasqua, periodi a cavallo d'anno) | [x] |
| 2 | Precaricamento festività nazionali prima di `DataInitializer` | `FestivitaIniziali.java` | test | [x] |
| 3 | `generaLezioni` salta le festività | `CorsoService` | test, test esistenti aggiornati se servono | [x] |
| 4 | Vista "Festività" con dialog e card mobile | `admin/FestivitaView.java` | `ViewAccessTest`, browser desktop/390px | [x] |
| 5 | Lezioni: badge "Festività" e avviso nel dialog di nuova lezione/recupero | `ElencoLezioni` | test, browser | [x] |
| 6 | CLAUDE.md, verifica finale, registro | | browser | [ ] |

## Fuori ambito

- Periodi relativi alla Pasqua; festività diverse per corso.
- Spostamento automatico delle lezioni già generate quando si aggiunge una festività (serve "Genera Lezioni").

## Registro

