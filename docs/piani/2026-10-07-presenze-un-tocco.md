# Piano: assegnazione presenze con un tocco

## Richiesta

Semplificare l'assegnazione di Presente / Assente giustificato / Assente ingiustificato nel dialog presenze del docente, in modo che sia comoda anche da smartphone, senza dover aprire una lista di selezione.

## Analisi

Oggi (`LezioniDocenteView.createPresenzaEditor`) ogni studente ha una `ComboBox`: servono due tocchi (aprire la tendina, scegliere) e a ogni cambio compare una notifica che su mobile copre la lista. Lo stato attuale si legge solo dal testo della tendina.

Alternative valutate (documentazione Vaadin 25.3):
- `RadioButtonGroup` orizzontale (variante Aura): un tocco, ma i punti radio sono bersagli piccoli e con tre etichette lunghe a ~300px va a capo.
- **Pulsanti segmentati** (scelta): tre pulsanti affiancati della stessa larghezza, quello attivo pieno e colorato (verde Presente, arancione Giustificato, rosso Ingiustificato). Un tocco per cambiare stato, stato leggibile a colpo d'occhio, bersagli grandi.

Dettagli:
- Componente riusabile `StatoPresenzaToggle` basato su `AbstractCompositeField<…, StatoPresenza>` (vero campo con `getValue`/`setValue`/value change), con semantica accessibile `radiogroup`/`radio` + `aria-checked` ed etichetta completa come `aria-label` e tooltip.
- Etichette brevi sui pulsanti ("Presente", "Giustif.", "Ingiust.") per stare in una riga anche a 390px.
- Riga di ogni studente flessibile: nome · stato · note sulla stessa riga su desktop, che va a capo su schermi stretti (stato a tutta larghezza).
- Salvataggio immediato a ogni tocco come oggi; niente notifica a ogni cambio (il pulsante colorato è il riscontro), notifica solo in caso di errore.

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | Componente `StatoPresenzaToggle` + test unitario (valore, evento, aria-checked) | `views/StatoPresenzaToggle.java`, `StatoPresenzaToggleTest.java` | `./mvnw test` | [x] |
| 2 | Stili del toggle e della riga presenza (pulsanti uguali, a capo su mobile) | `styles.css` | `./mvnw -q compile` | [x] |
| 3 | Dialog presenze del docente: toggle al posto della ComboBox, riga flessibile, notifica solo su errore | `LezioniDocenteView` | `./mvnw test` | [x] |
| 4 | Test browserless: il docente segna "Assente giustificato" con un tocco e il valore viene salvato | `ViewAccessTest` | `./mvnw test` | [ ] |
| 5 | `CLAUDE.md`: stato presenza sempre con `StatoPresenzaToggle` | `CLAUDE.md` | — | [ ] |
| 6 | Verifica finale: build, browser a 390px e 1280px (tocco, colori, salvataggio), registro | piano | build + browser | [ ] |

## Fuori ambito

- Pulsante "Segna tutti presenti": non necessario, alla generazione delle lezioni tutti risultano già Presenti.
- Viste in sola lettura di studenti e genitori: lo stato resta come testo; si può valutare un badge colorato coerente con il toggle.

## Registro

_(compilato durante l'esecuzione)_
