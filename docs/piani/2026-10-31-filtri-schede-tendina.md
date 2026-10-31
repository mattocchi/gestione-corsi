# Filtri a schede o a tendina

## Richiesta

[F-2026-10-31/FILTRI.1] Filtri a schede o a tendina in Lezioni, Gestione Pagamenti, Riepilogo Iscritti, Cronologia
documenti, Gestione Corsi e Calendario, come il tipo di documento in Documenti.

## Analisi

- Oggi questi filtri sono `SegmentedControl` (pulsanti affiancati): `ElencoLezioni.filtro` (4), `CorsiView.filtroStato`
  (3) e `filtroTipo` (3), `ElencoPagamenti.filtroCorsi` (3) e `filtroPagamento` (4), `RiepilogoIscrittiView.filtroCorsi`
  (3), `EmailInviateView.filtroTipo` (7), `CalendarioView.vista` (2).
- `DocumentiView` usa già `Tabs` con una mappa scheda → tipo: conviene un componente riutilizzabile `views/Schede<T>`
  con la stessa interfaccia di valore del `SegmentedControl` (`AbstractCompositeField` su `Tabs`: `getValue`,
  `setValue`, `addValueChangeListener`), così le viste cambiano poco.
- La tendina è il `Select<T>` di Vaadin (etichetta, nessuna scelta vuota).

### Decisioni

- [F-2026-10-31/FILTRI.1] Filtro principale a schede: Lezioni (stato), Gestione Corsi e Riepilogo Iscritti (stato dei
  corsi), Gestione Pagamenti e Pagamenti del genitore (stato del pagamento), Calendario (Mese / Settimana tipo).
- [F-2026-10-31/FILTRI.1] Filtri secondari a tendina: Gestione Corsi (tipo), Gestione Pagamenti (stato del corso),
  Cronologia documenti (tipo di documento, sotto le schede Email inviate / Documenti aperti).
- [F-2026-10-31/FILTRI.1] Le scelte a pulsanti nei dialog (metodo di pagamento, cadenza, tipo di corso, festività,
  giorno del gruppo, insegnanti presenti) restano `SegmentedControl`.

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | Componente `Schede<T>` (Tabs con valore) e `DocumentiView` che lo usa | `views/Schede`, `DocumentiView`, test | `./mvnw test` | [x] |
| 2 | Lezioni / Le Mie Lezioni: stato a schede | `ElencoLezioni`, `ViewAccessTest` | `./mvnw test` | [x] |
| 3 | Gestione Corsi: stato a schede, tipo a tendina | `CorsiView`, `ViewAccessTest` | `./mvnw test` | [x] |
| 4 | Gestione Pagamenti e Pagamenti del genitore: pagamento a schede, stato del corso a tendina | `ElencoPagamenti`, `ViewAccessTest` | `./mvnw test` | [x] |
| 5 | Riepilogo Iscritti: stato dei corsi a schede | `RiepilogoIscrittiView`, `ViewAccessTest` | `./mvnw test` | [x] |
| 6 | Cronologia documenti: tipo a tendina | `EmailInviateView`, `ViewAccessTest` | `./mvnw test` | [x] |
| 7 | Calendario: Mese / Settimana tipo a schede | `CalendarioView`, test | `./mvnw test` | [x] |
| 8 | CLAUDE.md: quando schede, tendina o pulsanti affiancati | `CLAUDE.md` | — | [x] |
| 9 | Registro | questo piano | — | [x] |

## Fuori ambito

- Le scelte a pulsanti nei dialog (vedi Decisioni).

## Registro

- Passi 1-8 completati, un commit per passo; `./mvnw test`: 344 test, 0 errori (nuovo `SchedeTest`; in
  `ViewAccessTest` gli aiuti `scegliTipo` e `filtroStatoCorsi` lavorano su `Schede` e `Select`).
- Verificato sulla guida di Vaadin 25.3: `Tabs` serve alla navigazione sul posto tra contenuti; Documenti e Calendario
  cambiano contenuto, gli stati (lezioni, corsi, pagamenti) filtrano la stessa lista, uso previsto anche dalla guida
  (esempio Open / Completed / Cancelled). Schede larghe quanto la riga: su schermi stretti scorrono con i pulsanti.
- Il `Select` corregge un errore trovato dai test: con la voce vuota il generatore delle etichette riceve `null`.
- Novità per il genitore: in Pagamenti ha ora le schede dello stato del pagamento (decisione del chiarimento).
- `Select` è un componente nuovo per l'app: il bundle di produzione va rigenerato (`build.sh` cancella già
  `prod.bundle`; con `./mvnw clean package` cancellarlo a mano).
- Non verificato nel browser (desktop e 390px).
