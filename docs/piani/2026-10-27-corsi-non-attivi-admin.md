# Corsi non attivi modificabili dall'admin

## Richiesta

"rendi modificabile un corso anche se non attivo da admin" — [F-2026-10-27/CORSI.1]

## Analisi

- La scheda del corso (Modifica in Gestione Corsi) è già modificabile anche per un corso non attivo.
- Bloccati oggi da `CorsoService.verificaAttivo` / `corsoAttivo`: iscrizioni, orari, gruppi, Aggiorna lezioni
  (`CorsoService`), lezioni (`LezioneService.save`, `verificaPermesso`) e presenze (`PresenzaService.save`).
- UI: `PartecipantiDialog` in sola lettura, "Aggiorna lezioni" disabilitato e nessun avviso dopo il salvataggio
  (`CorsiView`); `LezioniView` mostra solo le lezioni dei corsi attivi.
- Il ruolo dell'utente si legge dal `SecurityContextHolder` (impostato anche nei test con `@WithUserDetails`): i metodi
  di `CorsoService` non ricevono lo username.

### Decisioni

- [F-2026-10-27/CORSI.1] Per l'admin un corso non attivo si comporta come uno attivo: partecipanti, orari, gruppi,
  Aggiorna lezioni, lezioni e presenze.
- [F-2026-10-27/CORSI.1] Segreteria e insegnanti restano bloccati sui corsi non attivi.
- [F-2026-10-27/CORSI.1] In Lezioni il filtro corso dell'admin include anche i corsi non attivi, con "(non attivo)":
  scelto uno, se ne vedono e gestiscono lezioni e presenze (anche "Nuova lezione"); Calendario e dashboard restano sui
  corsi attivi.

## Passi

| Passo | Cosa | File | Verifica | Stato | Richiesta |
|---|---|---|---|---|---|
| 1 | `CorsoService.verificaModificabile` (attivo, oppure admin corrente) e `puoModificare` al posto di `verificaAttivo`; `LezioneService`/`PresenzaService`; UI di `PartecipantiDialog`, `CorsiView`; filtro corsi non attivi in `LezioniView`/`ElencoLezioni`; test; CLAUDE.md | service, views, test | `./mvnw test` | [x] | F-2026-10-27/CORSI.1 |

## Fuori ambito

- Calendario, dashboard e viste degli insegnanti continuano a ignorare i corsi non attivi.
- "Aggiorna tutti" dopo le festività resta sui soli corsi attivi.

## Registro

- Passo 1: `CorsoService.verificaModificabile`/`puoModificare` (admin dal `SecurityContextHolder`), `LezioneService.verificaPermesso` con i ruoli dello username, `PartecipantiDialog`, `CorsiView` (Aggiorna lezioni e avviso dopo il salvataggio), filtro di Lezioni con i corsi non attivi per l'admin. Nuovo test `ViewAccessTest.admin_modificaUnCorsoNonAttivo`; il test della segreteria in sola lettura resta valido. `./mvnw test`: 331 test, tutti verdi. Non verificato nel browser.
