# Piano: corso pubblicato e duplicazione del corso

## Richiesta

[F-2026-10-27/PUBBLICAZIONE.1] Corso pubblicato / non pubblicato, distinto da attivo / non attivo (archivio): un corso non pubblicato è in preparazione e non è visibile a genitori e allievi.
[F-2026-10-27/PUBBLICAZIONE.2] "Duplica corso" per preparare il corso degli anni successivi, lasciando ad admin e segreteria le modifiche di dettaglio.

## Analisi

- `Corso.attivo` = archivio: i non attivi sono nascosti a insegnanti e allievi, non ai genitori (ricevute). La pubblicazione è un concetto diverso: riguarda solo genitori e allievi.
- I dati dei corsi arrivano a genitori e allievi da più service (corsi, calendario, dashboard, pagamenti, documenti, email): mappati prima dell'implementazione.

### Decisioni

- [F-2026-10-27/PUBBLICAZIONE.1] Un corso non pubblicato è nascosto del tutto a genitori e allievi (I Miei Corsi, I Miei Figli, calendario, dashboard, pagamenti, documenti); lo staff non può inviare loro email di quel corso (conferme, ricevute, riepiloghi e attestazioni non lo comprendono).
- [F-2026-10-27/PUBBLICAZIONE.1] Lo vedono e ci lavorano admin, segreteria e insegnanti del corso (partecipanti, lezioni, gruppi), con il badge "Non pubblicato"; pubblicano admin e segreteria (campo nel dialog del corso, filtro in Gestione Corsi).
- [F-2026-10-27/PUBBLICAZIONE.1] I corsi esistenti restano pubblicati (colonna nullable, null = pubblicato); un corso nuovo o duplicato nasce non pubblicato.
- [F-2026-10-27/PUBBLICAZIONE.2] "Duplica" (admin e segreteria, Gestione Corsi) copia le impostazioni: tipo, nome, descrizione, insegnanti, giorni, cadenza, orario, durata, numero di lezioni / data di fine, costi, luoghi e gruppi; anno scolastico successivo, date spostate di un anno sullo stesso giorno della settimana; non pubblicato, attivo; nessun partecipante, lezione o pagamento. Poi si apre il dialog del nuovo corso.

## Passi

| Passo | Cosa | File | Richiesta | Verifica | Stato |
|---|---|---|---|---|---|
| 1 | `Corso.pubblicato` (null = pubblicato, nuovi no); campo nel dialog, badge "Non pubblicato" e filtro in Gestione Corsi; test | `Corso`, `CorsiView`, test | — | `./mvnw test` | [x] Badge "Non pubblicato" accanto al nome (`TipoCorsoUI.nome`), quindi anche nelle viste degli insegnanti; corsi di esempio pubblicati |
| 2 | Corsi non pubblicati esclusi da tutto ciò che vedono genitori e allievi e dagli invii alle famiglie; badge per gli insegnanti; test | service e viste di allievi/genitori, `EmailService`, test | PUBBLICAZIONE.1 | `./mvnw test` | [x] Filtri in corsi, calendario e settimana tipo, dashboard, pagamenti, riepiloghi, attestazioni (anche il totale per lo staff), documenti; invii bloccati |
| 3 | `CorsoService.duplica` e azione "Duplica" in Gestione Corsi; test | `CorsoService`, `CorsiView`, test | PUBBLICAZIONE.2 | `./mvnw test` | [x] Date spostate di 52 settimane (stesso giorno della settimana); stesso nome |
| 4 | `CLAUDE.md` e registro | `CLAUDE.md`, questo piano | — | test | [x] |

## Fuori ambito

- Pubblicazione programmata a una data.

## Registro

- `./mvnw test`: 325 test superati. Nuovi: `PubblicazioneTest` (corsi, pagamenti, dashboard, calendario, documenti e invii delle famiglie senza il corso non pubblicato, lo staff lo vede), `CorsoServiceTest.duplica_impostazioniPerLAnnoSuccessivoSenzaPersone`, `ViewAccessTest.segreteria_duplicaUnCorsoEApreIlDialog`; esteso il test del servizio in Gestione Corsi (badge e filtro "Solo non pubblicati").
- Scelte tecniche: i totali delle attestazioni per lo staff escludono i pagamenti dei corsi non pubblicati, come il PDF; i documenti di un corso non pubblicato li scarica solo lo staff.
- Non verificato nel browser: coperto dai test browserless e di servizio.
- Produzione: nessuno script (colonna `pubblicato` nullable, null = pubblicato).
- Lasciato aperto: nessuno.

