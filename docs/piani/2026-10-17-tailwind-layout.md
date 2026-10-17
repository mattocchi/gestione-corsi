# Piano: layout delle API Java convertito in Tailwind

Branch: `feature/tabelle-responsive-card`.

## Richiesta

[F-2026-10-17/TAILWIND.3] Oltre al CSS custom, convertire in Tailwind anche il layout fatto con le API Java.

## Analisi

- ~100 chiamate in 30 file: `setSizeFull`, `expand`, `setPadding`, `setSpacing`, `setWrap`, `setAlignItems`,
  `setDefaultVerticalComponentAlignment`, `setJustifyContentMode`, `setWidthFull`, `setWidth("…rem")`, `setMaxWidth("100%")`
  su `VerticalLayout`/`HorizontalLayout`, campi, filtri e card. 21 `FormLayout`.
- Il costruttore di `VerticalLayout` (bytecode 25.3.1) chiama `setWidth("100%")`, `setSpacing(true)`, `setPadding(true)`:
  larghezza inline (uguale a `w-full`), padding e spacing come attributi `theme` stilati con `:host([theme~=…])` nello
  shadow DOM. Le regole del documento (anche nel layer `utilities` di Tailwind) vincono su quelle `:host`, quindi
  `p-0`/`gap-0` neutralizzano i default; gli stili inline invece vincerebbero sulle classi, per questo le API di
  dimensione e allineamento (che scrivono stili inline) vanno tolte, non affiancate.
- La doc Vaadin (*Utility Classes*) avverte che le API dei layout possono entrare in conflitto con le utility: per
  ogni elemento si usa o l'una o l'altra cosa.

### Conversione

| API Java | Classe Tailwind |
|---|---|
| `setSizeFull()` | `size-full` |
| `setWidthFull()` | `w-full` |
| `setWidth("10rem")` / `"11rem"` / `"18rem"` | `w-40` / `w-44` / `w-72` |
| `setMaxWidth("100%")` | `max-w-full` |
| `setPadding(false)` | `p-0` |
| `setSpacing(false)` | `gap-0` |
| `setSpacing(true)`, `setWrap(false)` su `HorizontalLayout` | nulla (sono già i default) |
| `setWrap(true)` | `flex-wrap` |
| `setAlignItems(CENTER)`, `setDefaultVerticalComponentAlignment(CENTER)` | `items-center` |
| `setDefaultVerticalComponentAlignment(BASELINE)` | `items-baseline` |
| `setJustifyContentMode(CENTER)` | `justify-center` |
| `expand(componente)` | `grow` sul componente |

### Decisioni

- [F-2026-10-17/TAILWIND.3] `VerticalLayout`/`HorizontalLayout` restano; tutta la loro configurazione (padding, spacing, allineamenti, wrap, expand, dimensioni) passa a classi Tailwind.
- [F-2026-10-17/TAILWIND.3] Anche le dimensioni dei singoli componenti (campi, filtri, card, viste) diventano classi; restano via API le larghezze delle colonne della Grid e dei Dialog (non sono CSS dell'elemento: colonna e overlay).
- [F-2026-10-17/TAILWIND.3] `FormLayout` resta com'è (logica responsive propria).

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | Supporto comune e shell: `ViewSupport` (azioni, filtro anno), `MainLayout`, `LoginView`, `CardMobile`; `ElencoPagamenti.filtro` riceve la classe di larghezza | | test, browser | [x] |
| 2 | Viste di gestione: Corsi, Allievi, Genitori, Insegnanti, Utenti, Anni scolastici, Rimborsi, Email inviate, Lezioni, Gestione Pagamenti | `admin/*View.java` | test, browser | [x] |
| 3 | Dialog: Partecipanti, Quota e pagamenti, Rimborsi, Bilancio, Invio email, Email di test, Presenze, Pagamenti dell'allievo | | test, browser | [x] |
| 4 | Liste condivise e viste degli altri ruoli: `ElencoLezioni`, `ElencoPagamenti`, Profilo, I miei corsi, Figli, Pagamenti figli, Le mie lezioni, I miei rimborsi | | test, browser | [x] |
| 5 | CLAUDE.md (regola e tabella), verifica finale desktop/390px, build di produzione, registro | | browser, `clean package` | [ ] |

## Fuori ambito

- `FormLayout`, larghezze delle colonne della Grid e dei Dialog.
- Sostituzione dei layout Vaadin con `Div`.

## Registro

