# Piano: accesso semplificato al registro dell'insegnante

## Richiesta

[F-2026-10-11/REGISTRO.1] L'insegnante con un solo corso, al login, va direttamente all'elenco delle lezioni.
[F-2026-10-11/REGISTRO.2] Badge "Oggi" sulla lezione di oggi.
[F-2026-10-11/REGISTRO.3] Badge di colori diversi per la settimana scorsa (lezioni ancora da compilare) e la settimana prossima.

## Analisi

- Dopo il login Vaadin porta a `/` (`HomeView`, dashboard). `LezioniInsegnanteView` ("Le Mie Lezioni") elenca le lezioni dei corsi attivi dell'insegnante in ordine di data, filtro di default "Da fare"; colonne Data (testo) e, su mobile, un riepilogo corso/data (`LitRenderer`).
- Corsi attivi dell'insegnante: `CorsoRepository.countByInsegnanteUserUsernameAndAttivoTrue` (già usato dalla dashboard).
- `BadgeVariant` (25.3): `FILLED`, `CONTRAST`, `WARNING`, `ERROR`, `SUCCESS`, `SMALL`, … (verificato con il MCP Vaadin).
- Scelte tecniche: "solo al login" = prima navigazione della sessione Vaadin (attributo di sessione impostato da `MainLayout.beforeEnter`): se è verso la Home e l'utente ha solo il ruolo INSEGNANTE con un corso attivo, si inoltra a `LezioniInsegnanteView`; le navigazioni successive verso la Home non sono deviate. La classificazione della data (`QuandoLezione`) è una funzione pura testata a parte; "Oggi", "Questa settimana" e "Settimana prossima" valgono anche per le lezioni completate, "Settimana scorsa" e "In ritardo" solo per quelle non completate.

### Decisioni

- [F-2026-10-11/REGISTRO.1] Solo al login, solo per chi ha unicamente il ruolo INSEGNANTE, contando i corsi attivi; la Home resta raggiungibile dal menu.
- [F-2026-10-11/REGISTRO.2] Badge "Oggi" su tutte le lezioni di oggi, non solo sulla prima in lista.
- [F-2026-10-11/REGISTRO.3] Settimane di calendario lunedì–domenica: "Settimana scorsa" per le lezioni non completate della settimana precedente, "Settimana prossima" per quelle della successiva.
- [F-2026-10-11/REGISTRO.3] In più: "In ritardo" per le lezioni non completate precedenti alla settimana scorsa e "Questa settimana" per gli altri giorni della settimana corrente.
- [F-2026-10-11/REGISTRO.2][F-2026-10-11/REGISTRO.3] Colori: Oggi blu pieno (FILLED), Questa settimana blu chiaro (default), Settimana prossima grigio (CONTRAST), Settimana scorsa arancione (WARNING), In ritardo rosso (ERROR).
- [F-2026-10-11/REGISTRO.2][F-2026-10-11/REGISTRO.3] Badge accanto alla data (colonna Data su desktop, riepilogo su mobile), nessuna colonna nuova.

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | `QuandoLezione` (enum con etichetta e variante, `di(lezione, oggi)`) e test unitario | `views/insegnante/QuandoLezione.java`, test | `./mvnw test` | [x] |
| 2 | `LezioniInsegnanteView`: badge accanto alla data (desktop e riepilogo mobile); test | `views/insegnante/LezioniInsegnanteView.java`, `styles.css`, `ViewAccessTest`, `docs/FUNZIONALITA.md` | `./mvnw test` | [x] |
| 3 | Pagina iniziale dell'insegnante con un solo corso: inoltro alla prima navigazione verso la Home; test | `views/MainLayout.java`, `service/DashboardService.java`, `ViewAccessTest`, `docs/FUNZIONALITA.md` | `./mvnw test` | [x] |
| 4 | `CLAUDE.md`: note su pagina iniziale e badge | `CLAUDE.md` | rilettura | [x] |

## Fuori ambito

- Filtro per corso o scorrimento automatico della lista alla data di oggi.
- `.idea/.copilot-plugin` (modifica locale preesistente) non incluso.

## Registro

- Passo 1: `QuandoLezioneTest` (settimane lun–dom, domenica come ultimo giorno, lezioni passate solo se da compilare).
- Passo 2: `./mvnw test` 87 test verdi (badge Oggi pieno, Settimana prossima grigio, In ritardo rosso sulle lezioni di prova del 2024).
- Passo 3: 89 test verdi (insegnante1 con un corso parte da Le Mie Lezioni e la Home resta raggiungibile; con due corsi resta la Home). Adeguati i test degli insegnanti che riaprono la vista.
- Browser non verificato in questa sessione: da controllare a mano il login di un insegnante con un solo corso e i badge su mobile.
- In produzione nessuna modifica allo schema.
