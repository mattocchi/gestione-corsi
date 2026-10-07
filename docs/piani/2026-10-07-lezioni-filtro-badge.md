# Piano: filtro per stato e badge in "Le Mie Lezioni"

## Richiesta

Migliorare la vista "Le Mie Lezioni" del docente (e di altri profili che la usano):
- filtro per stato delle lezioni: **Tutte / Completate / Da fare**, di default **Da fare**;
- al posto della colonna "Completata: Sì/No", un'icona o un badge più leggibile.

## Analisi

- La vista `LezioniDocenteView` è usata solo dal ruolo **DOCENTE** (`@RolesAllowed("DOCENTE")`); nessun altro profilo ha una vista delle lezioni, quindi la modifica riguarda solo questa vista.
- Oggi la griglia mostra tutte le lezioni del docente (30 per corso nei dati di test) con una colonna testuale "Completata" Sì/No, nascosta su mobile.
- **Filtro**: per coerenza con il dialog presenze e per l'uso da smartphone si usano gli stessi pulsanti segmentati (un tocco, selezione evidenziata). Il componente `StatoPresenzaToggle` viene generalizzato in `SegmentedControl<T>` riusabile, e `StatoPresenzaToggle` diventa una sua specializzazione (comportamento invariato). Il filtro è applicato in memoria sulla griglia (`GridListDataView.setFilter`): le lezioni di un docente sono poche centinaia al massimo.
- **Badge**: il componente `Badge` di Vaadin (stabile in 25.3.1, nessun feature flag, incluso in `vaadin-core`) con icona Vaadin Icons:
  - Completata → badge verde (`SUCCESS`) con icona `CHECK`;
  - Da fare → badge neutro con icona `CLOCK`.
  Su desktop badge con testo; su mobile solo icona (`ICON_ONLY`, il testo resta per i lettori di schermo), così la colonna stato resta visibile anche sul telefono.
- Dopo "Segna Completata" nel dialog presenze la griglia si ricarica mantenendo il filtro: con "Da fare" la lezione completata sparisce dall'elenco.

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | Generalizzare il toggle in `SegmentedControl<T>`; `StatoPresenzaToggle` lo estende senza cambi di comportamento | `views/SegmentedControl.java`, `StatoPresenzaToggle.java`, `styles.css` | `./mvnw test` (test del toggle invariati) | [x] |
| 2 | Filtro Da fare / Completate / Tutte sopra la griglia, default Da fare, mantenuto dopo il ricaricamento | `LezioniDocenteView` | `./mvnw test` | [x] |
| 3 | Colonna "Stato" con `Badge` (testo su desktop, solo icona su mobile) al posto di Sì/No | `LezioniDocenteView` | `./mvnw test` | [x] |
| 4 | Test browserless: default solo da fare; completando una lezione sparisce da "Da fare" e compare in "Completate"; "Tutte" mostra entrambe | `ViewAccessTest` | `./mvnw test` | [x] |
| 5 | `CLAUDE.md`: `SegmentedControl` per scelte singole brevi e `Badge` per gli stati | `CLAUDE.md` | — | [x] |
| 6 | Verifica finale: build, browser a 390px e 1280px, registro | piano | build + browser | [ ] |

## Fuori ambito

- Conteggio delle lezioni nelle etichette del filtro (es. "Da fare (28)"): possibile miglioramento successivo.
- Stato "in ritardo" (lezione passata ma non completata) con badge arancione: utile, ma è un'informazione nuova non richiesta; da valutare.
- Viste studente/genitore: non mostrano le lezioni come elenco del docente.

## Registro

_(compilato durante l'esecuzione)_
