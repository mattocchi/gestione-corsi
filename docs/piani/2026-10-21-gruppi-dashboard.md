# Piano: numero di gruppi dei corsi e riepilogo in dashboard

## Richiesta

Mostrare quanti gruppi ha un corso dove serve ([F-2026-10-21/GRUPPI.2]) e aggiungere un riepilogo dei gruppi nella dashboard ([F-2026-10-21/GRUPPI.3]).

## Analisi

- Gestione Corsi (`CorsiView`) non mostra i gruppi; i corsi arrivano da `CorsoService.findAll()` (`findAllWithAllievi`), i gruppi stanno in `corso_gruppi` (`GruppoCorsoRepository`).
- Riepilogo Iscritti ha già i gruppi del corso in `PagamentoService.IscrittiCorso.gruppi()`; la riga del corso mostra i giorni nella colonna "Giorni / Gruppo" (anche campo "Giorni" della card).
- Dashboard (`HomeView` + `DashboardService`): sezioni di riquadri per ruolo, gli admin hanno anche la tabella dei pagamenti con `CardMobile`. Le relazioni `Iscrizione.gruppo`, `Iscrizione.corso`, `GruppoCorso.corso`, `Corso.insegnante` e `Insegnante.user` sono EAGER: niente problemi di caricamento con `open-in-view=false`.

### Decisioni

- [F-2026-10-21/GRUPPI.2] Numero di gruppi in Gestione Corsi: nuova colonna "Gruppi" (e campo della card mobile), vuota se il corso non ne ha.
- [F-2026-10-21/GRUPPI.2] In Riepilogo Iscritti sulla riga del corso, nella colonna "Giorni / Gruppo": es. "sab · 2 gruppi" (solo per i corsi con gruppi).
- [F-2026-10-21/GRUPPI.3] Solo corsi attivi.
- [F-2026-10-21/GRUPPI.3] Admin e segreteria: sezione "Gruppi dei corsi attivi" con i riquadri "Corsi con gruppi", "Gruppi", "Allievi senza gruppo" (iscritti senza gruppo nei corsi che hanno gruppi) e una tabella con una riga per gruppo: corso, gruppo (`Formati.gruppo`), iscritti.
- [F-2026-10-21/GRUPPI.3] Insegnante: la stessa tabella limitata ai suoi corsi attivi (titolare o secondo), nella dashboard sotto "I miei corsi".
- Tecnica: per admin/segreteria la sezione c'è sempre (tabella vuota "Nessun corso attivo con gruppi"); per l'insegnante la tabella compare solo se i suoi corsi hanno gruppi. Righe ordinate per corso e poi `GruppoCorso.PER_GIORNO`.

## Passi

| Passo | Cosa | Richiesta | File | Verifica | Stato |
|---|---|---|---|---|---|
| 1 | `CorsoService.numeroGruppi()`; colonna/campo "Gruppi" in Gestione Corsi; "· N gruppi" sulla riga del corso in Riepilogo Iscritti; test | [F-2026-10-21/GRUPPI.2] | `service/CorsoService`, `repository/GruppoCorsoRepository`, `views/admin/CorsiView`, `views/admin/RiepilogoIscrittiView`, `ViewAccessTest` | `./mvnw test` | [ ] |
| 2 | `DashboardService.riepilogoGruppi()` / `gruppiDellInsegnante(username)`; sezione in `HomeView` per admin/segreteria e tabella per l'insegnante; test | [F-2026-10-21/GRUPPI.3] | `service/DashboardService`, `views/HomeView`, test | `./mvnw test` | [ ] |
| 3 | CLAUDE.md e registro | — | `CLAUDE.md`, questo piano | — | [ ] |

## Fuori ambito

- Numero di gruppi in Lezioni / Le Mie Lezioni e nel dialog Partecipanti.
- Riepilogo dei gruppi per allievi e genitori.

## Registro
