# Piano: vista calendario mensile

## Richiesta

Per admin, segreteria, insegnanti e allievi una vista "Calendario" mensile con i corsi secondo il livello di ciascuno ([F-2026-10-23/CALENDARIO.1]).

## Analisi

- Vaadin 25.3 non ha un componente calendario (catalogo dei componenti via MCP: nessun "Calendar"; c'è solo il calendario interno del Date Picker). Gli add-on della community (es. FullCalendar) sono una dipendenza esterna da verificare.
- Lezioni: `LezioneRepository.findByDataBetweenOrderByDataAsc` (tutte), `findDeiCorsiAttiviDellInsegnante` (insegnante, anche secondo); per allievi e genitori le lezioni si ricavano dalle presenze (`PresenzaRepository.findByAllievoId`), che esistono solo per chi frequenta quel giorno (gruppo). `Lezione.corso`, `insegnantiPresenti` e `Presenza.lezione` sono EAGER.
- Festività: `FestivitaService.calendario()` (`CalendarioFestivita`).
- Il dialog delle presenze è dentro `ElencoLezioni` (Lezioni / Le Mie Lezioni) e dipende da molte sue funzioni (recupero, nuova lezione, non effettuata).

### Decisioni

- [F-2026-10-23/CALENDARIO.1] Griglia mensile realizzata nel progetto (7 colonne lun–dom, mese precedente/successivo, "Oggi"); su smartphone un elenco dei giorni del mese con lezioni.
- [F-2026-10-23/CALENDARIO.1] Contenuto secondo il ruolo: admin e segreteria tutte le lezioni dei corsi attivi, con filtro per corso; insegnante quelle dei suoi corsi; allievo quelle a cui partecipa (solo il suo gruppo); genitore quelle dei figli (con il nome del figlio). Ogni lezione: orario, corso, gruppo o giorno, stato (da fare, completata, non effettuata); i giorni festivi con il nome della festività.
- [F-2026-10-23/CALENDARIO.1] Clic su una lezione: dettagli in sola lettura (corso, numero, data e orario, insegnanti, stato); per insegnanti, admin e segreteria anche "Gestisci presenze".
- Tecnica: "Gestisci presenze" apre Lezioni (admin, segreteria) o Le Mie Lezioni (insegnante) con il parametro `?lezione=ID`, che apre il dialog delle presenze già esistente: nessuna copia del registro. Voce di menu "Calendario" (`vaadin:calendar-user`) per tutti i ruoli tranne chi non ha lezioni da vedere.

## Passi

| Passo | Cosa | Richiesta | File | Verifica | Stato |
|---|---|---|---|---|---|
| 1 | `CalendarioService.lezioniDelMese(username, mese, corsoId)` secondo il ruolo; test | — | `service/CalendarioService`, test | `./mvnw test` | [x] |
| 2 | Apertura delle presenze da `?lezione=ID` in Lezioni e Le Mie Lezioni; test | — | `views/ElencoLezioni`, `admin/LezioniView`, `insegnante/LezioniInsegnanteView`, test | `./mvnw test` | [x] |
| 3 | `CalendarioView`: griglia mensile, elenco su mobile, filtro corso, festività, dettagli; test | [F-2026-10-23/CALENDARIO.1] | `views/CalendarioView`, test | `./mvnw test` (browser non riuscito, vedi registro) | [x] |
| 4 | CLAUDE.md e registro | — | `CLAUDE.md`, questo piano | — | [x] |

## Fuori ambito

- Vista settimanale o giornaliera; trascinare le lezioni per spostarle.

## Registro

- Passo 1: `./mvnw test` 226 test verdi; nuovo `CalendarioServiceTest` (ottobre 2024: admin/segreteria 14 lezioni, filtro Canto 5, insegnanti e allievi solo le proprie, genitore 14 con il nome dei figli).
- Passo 2: `./mvnw test` 227 test verdi; nuovo `lezioni_conIlParametroApreLePresenzeDellaLezione`.
- Passo 3: `./mvnw test` 229 test verdi; nuovi `admin_calendarioDelMeseConFiltroDettagliEPresenze` (voce di menu, mese, filtro, dettagli, "Gestisci presenze" che apre il registro sulla lezione) e `allievo_calendarioSoloLeSueLezioniSenzaPresenze`.
- **Verifica nel browser non riuscita**: avviata l'app in sviluppo, l'automazione di Chrome è bloccata da un'altra estensione sulla pagina di accesso ("Cannot access a chrome-extension:// URL of different extension") anche per gli screenshot; interrotta dopo tre tentativi. Aspetto della griglia (desktop) e dell'elenco (smartphone a 390px) da controllare a mano.
- Nessuna modifica al database; nessun componente Vaadin nuovo (il bundle di produzione non va rigenerato).
