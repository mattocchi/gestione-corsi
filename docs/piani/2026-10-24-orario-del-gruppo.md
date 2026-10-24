# Piano: gruppo e suo orario nella prossima lezione, nel calendario e negli altri elenchi

## Richiesta

[F-2026-10-24/ORARIO.1] Nei corsi con più gruppi (es. propedeutica alle 10 e alle 11) la prossima lezione e la lezione a calendario devono mostrare il gruppo e il suo orario (soprattutto quando l'allievo non ha un orario suo), non sempre l'orario della lezione; verificare gli altri punti dove serve.

## Analisi

- Dashboard (`DashboardService.lezioniDi`): orario = `Iscrizione.orarioLezione`, altrimenti quello della lezione; il gruppo è ignorato. Il riquadro mostra `Formati.numeroLezione` ("5 · sab" se quel giorno ci sono più gruppi).
- Calendario (`CalendarioService.vociDi`): per allievi e genitori l'orario del gruppo, altrimenti quello della lezione; l'orario proprio dell'allievo è ignorato. Lo staff ha già una voce per gruppo (`perGruppo`).
- Insegnante in dashboard: un riquadro per corso all'orario della lezione; il clic apre il registro (`?lezione=ID`), che ha le schede per gruppo (`ElencoLezioni.schedePerGruppo`) e si apre sulla prima.
- Altri punti con l'orario: "Orario" di Le Mie Lezioni / Lezioni (`ElencoLezioni`, orario della lezione), "I miei allievi" (solo `orarioLezione`), dialog delle lezioni dell'allievo (`PresenzeAllievoDialog`, nessun orario); il dettaglio aperto dalla dashboard non ha la riga "Gruppo".
- `Iscrizione.gruppo` è `@ManyToOne` EAGER: disponibile ovunque si carichi l'iscrizione.

### Decisioni

- [F-2026-10-24/ORARIO.1] Orario della lezione di un allievo (dashboard, calendario e altri elenchi): il suo orario se c'è, altrimenti quello del suo gruppo, altrimenti quello della lezione.
- [F-2026-10-24/ORARIO.1] Riquadro della prossima lezione: data e orario del gruppo/allievo, sotto "Lezione 5 · Grandi · Mario Rossi" (nome del gruppo se ce l'ha; per un gruppo senza nome restano giorno e orario).
- [F-2026-10-24/ORARIO.1] Insegnante: nella prossima lezione di un corso con più gruppi un riquadro per gruppo (come nel calendario, più "Senza gruppo" se serve), ognuno apre il registro della lezione sulla scheda di quel gruppo.
- [F-2026-10-24/ORARIO.1] Anche: "I miei allievi" con l'orario del gruppo se l'allievo non ne ha uno suo; "Orario" di Le Mie Lezioni / Lezioni con gli orari dei gruppi di quel giorno ("10:00 · 11:00"); riga "Gruppo" nel dettaglio aperto dalla prossima lezione; colonna "Orario" nel dialog delle lezioni dell'allievo (I Miei Corsi, I Miei Figli, I miei allievi).
- Scelte tecniche: la regola dell'orario è un metodo di `Iscrizione` (`orarioDi(lezione)`); una lezione con un orario diverso da quello del corso (spostata o aggiunta a un'altra ora) ha il suo; la dashboard riusa le voci del `CalendarioService`; "Gestisci presenze" del calendario apre anch'esso la scheda del gruppo (`?gruppo=ID`).

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | `Iscrizione.orarioDi(lezione)`; `CalendarioService`: voci dell'allievo con quell'orario, voci dello staff e dell'allievo pubbliche per la dashboard; test | `Iscrizione`, `CalendarioService`, test | `./mvnw test` | [x] |
| 2 | `DashboardService`: prossime lezioni dalle voci del calendario (insegnante: una per gruppo della prima lezione del corso; famiglie: con gruppo e orario); test | `DashboardService`, `DashboardServiceTest` | `./mvnw test` | [x] La regola di `orarioDi` cambiata: l'orario della lezione vale quando è diverso da quello del corso (non "spostata o aggiunta"), perché "Nuova lezione" e "Cambia data" propongono l'orario del corso e avrebbero riportato tutti alle 10; a pari orario la voce "Senza gruppo" dopo i gruppi |
| 3 | `HomeView`: riquadro con il gruppo; registro aperto sulla scheda del gruppo (`?gruppo=ID`, anche dal calendario); dettaglio con la riga "Gruppo"; test browserless | `HomeView`, `DettaglioLezioneDialog`, `ElencoLezioni`, `LezioniView`, `LezioniInsegnanteView`, `CalendarioView`, `Formati`, `ViewAccessTest` | `./mvnw test` | [x] `DettaglioLezioneDialog.apri` / `apriPresenze` ricevono la `VoceCalendario`; numero con il gruppo in `Formati.numeroLezione(voce, …)` |
| 4 | Orario dei gruppi in Le Mie Lezioni / Lezioni e in "I miei allievi"; colonna "Orario" nel dialog delle lezioni dell'allievo; test | `Iscrizione`, `ElencoLezioni`, `MieiAllieviView`, `PresenzeAllievoDialog`, `MieiCorsiView`, `FigliView`, `ViewAccessTest` | `./mvnw test` | [x] `Iscrizione.orarioAbituale()` (suo o del gruppo) per "I miei allievi"; in Le Mie Lezioni / Lezioni gli orari dei gruppi solo se la lezione è all'orario del corso, altrimenti il suo |
| 5 | `CLAUDE.md` e registro finale | `CLAUDE.md`, questo piano | build e test | [x] |

## Fuori ambito

- Documenti (la conferma mostra già "con lezione il sabato alle 10:00" dal gruppo).
- Correzione dell'orario lasciato all'allievo quando il suo gruppo viene eliminato (problema già segnalato).

## Registro

- `./mvnw test`: 271 test, tutti superati. Nuovi: `IscrizioneTest` (orario suo, del gruppo, della lezione), `DashboardServiceTest.prossimaLezione_conDueGruppiOgnunoConIlSuoOrario`, in `CalendarioServiceTest` l'orario proprio dell'allievo, in `ViewAccessTest` un riquadro per gruppo per l'insegnante (apre la scheda "Grandi"), il riquadro del genitore con "· Piccoli" alle 10 e il dettaglio con il gruppo, gli orari "10:00 · 11:00" in Le Mie Lezioni e la colonna "Orario" nelle lezioni dell'allievo.
- Cambio di regola durante l'esecuzione (passo 2): l'orario della lezione vince solo se diverso da quello del corso, perché "Nuova lezione" e "Cambia data" propongono l'orario del corso.
- Browser: non verificato (nei dati di sviluppo non ci sono gruppi); coperto dai test browserless.
- Nessuno script SQL. Lasciato aperto: l'orario che resta all'allievo quando il suo gruppo viene eliminato (già segnalato).
