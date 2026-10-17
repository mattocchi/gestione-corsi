# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run in development mode (default profile "dev", port 8080)
./mvnw

# Run unit + browserless view tests
./mvnw test

# Build production JAR (no -Pproduction needed in Vaadin 25: vaadin-dev is optional)
./mvnw clean package

# Run integration tests
./mvnw -Pit

# Build and copy JAR to release/ dir
./build.sh

# Deploy to remote server (cloud@cloud.attocchi.it via rsync); server runs deploy/start.sh
./deploy.sh
```

## Architecture

**Spring Boot 4.1.1 + Vaadin 25.3.1** (Flow, server-side Java UI; Vaadin 25.3 requires Spring Boot ≥ 4.1) for managing music courses. No separate frontend framework.
Use the Vaadin MCP server (`.mcp.json`) to check Vaadin 25.3 APIs before writing UI code — many older APIs are deprecated or removed.

### Roles and access

Five roles: `ADMIN` (full management), `SEGRETERIA` (courses, participants and lesson times, allievi (students); read-only lists of teachers and parents; no deletions, no user management), `INSEGNANTE` (teacher lessons), `ALLIEVO` (own courses; called *Studente* until 2026-10-11), `GENITORE` (children's courses). Views shared by admin and segreteria use `@RolesAllowed({"ADMIN", "SEGRETERIA"})` and hide admin-only actions (e.g. Elimina) with `securityService.hasRole("ADMIN")`.
- Views are protected with `@RolesAllowed` / `@PermitAll`; `MainLayout` itself is `@PermitAll` (layouts are access-checked too).
- The side menu is built from `@Menu` annotations via `MenuConfiguration.getMenuEntries()`, which already filters by access — don't add manual role checks to the menu.
- `SecurityService` wraps Vaadin's `AuthenticationContext` (`getUsername()`, `hasRole()`, `logout()`).
- `HomeView` is the role-based dashboard (`DashboardService` counts, one section of Vaadin `Card`s per role of the user). Browserless tests start on the Home route: navigate away and back to rebuild it after changing data. Start page: on the first navigation of the session (after login) `MainLayout.beforeEnter` forwards a user whose only role is INSEGNANTE and who has a single active course from Home to `LezioniInsegnanteView`. So in tests with insegnante1/insegnante2 that view is already open at start: reopen it via Home (`apriLezioni()` in `ViewAccessTest`).

### Package layout

```
it.attocchi/
├── entity/         JPA entities (id-based equals/hashCode)
├── repository/     Spring Data JPA repositories — used only by services (and DataInitializer)
├── service/        Business services, @Transactional(readOnly = true) by default
├── security/       SecurityConfig, SecurityService, UserDetailsServiceImpl
├── views/          Vaadin views by role (admin/, insegnante/, allievo/, genitore/) — depend on services only
├── Application.java       AppShellConfigurator: loads Aura theme + styles.css via @StyleSheet
└── DataInitializer.java   Seeds test data (dev profile only)
```

### Profiles and database

- `application.properties`: shared config; `spring.profiles.default=dev`.
- **dev** (`application-dev.properties`): H2 in-memory, `create-drop`, H2 console at `/h2-console`, `DataInitializer` seeds users (admin/admin, segreteria/segreteria, insegnante1/insegnante1, …).
- **prod** (`application-prod.properties`): MySQL, credentials from env vars `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`; `ddl-auto=update` (Flyway still to do). Started with `--spring.profiles.active=prod`. Schema changes that `update` can't apply (renamed tables/columns, changed stored values such as role names) need a manual SQL script in `docs/migrazioni/AAAA-MM-GG-<argomento>.sql`, to be run on MySQL before deploying.

### Key design patterns

- `spring.jpa.open-in-view=false`: everything a view displays must be loaded by the service (use `JOIN FETCH` queries for lazy collections). Browserless tests in `ViewAccessTest` catch `LazyInitializationException`.
- Theme is **Aura**: use unprefixed variants (`ButtonVariant.PRIMARY`), not `LUMO_*` or `LumoUtility`.
- Styling uses **Tailwind CSS** (experimental Vaadin feature flag `tailwindCss` in `src/main/resources/vaadin-featureflags.properties`, Tailwind 4 via Vite, no preflight): layout and element styles are utility classes added with `addClassNames("…")` in Java; combinations used in several places are constants in `views/Stili` (`TESTO_SECONDARIO`, `TITOLO_SEZIONE`, `RIEPILOGO_PAGAMENTI`, `LOGO`). Write class names literally in the source (the compiler scans `src/`; never build them at runtime). Colors, font sizes/weights and radii come from Aura tokens (`text-(--vaadin-text-color-secondary)`, `text-(length:--aura-font-size-s)`, `font-(--aura-font-weight-medium)`), Tailwind's scale for spacing and layout; `sm:` = 640px = `ViewSupport.MOBILE_BREAKPOINT`, `dark:` = prefers-color-scheme. Two utilities for the same property don't override each other by class order (the generated CSS order wins): choose one in Java instead. `styles.css` keeps only rules on Vaadin component internals (`::part`, `vaadin-grid-cell-content`). Keep the `HorizontalLayout`/`VerticalLayout` APIs (padding, spacing, alignment) rather than replacing them with utilities.
- Passwords are only set via `UserService.save(user, rawPassword)` (encoded there; null/blank keeps the existing one).
- People: `Allievo`, `Genitore`, `Insegnante` implement `Anagrafica` (codice fiscale, nascita, embedded `Indirizzo` mapped on the existing columns `indirizzo`/`cap`/`citta` + `provincia`); `getIndirizzo()` is never null. Edit forms reuse `views/UtenteFields` (user + contacts) and `views/AnagraficaFields` (validates `CodiceFiscale` and CAP/provincia); admin/segreteria save user + person in one transaction (`XxxService.salva(person, user, password)`), self-service profiles (`ProfiloView`, children in `FigliView`) go through service methods that copy only the allowed fields. An allievo without any address field shows the parent's one with "(del genitore)" (`Formati.indirizzo`), never copied. An allievo's codice fiscale and data di nascita are optional (MySQL columns made nullable by `docs/migrazioni/2026-10-13-allievi-dati-facoltativi.sql`) but required for receipts and confirmations: `Allievo.getDatiMancantiPerDocumenti()`, `DocumentiService.avvisoDatiMancanti(allievo)` (documents and emails are refused, the bulk send skips and counts them, `Ricevute.link` shows the warning instead of downloading, "Dati mancanti" badge in Gestione Allievi). A new allievo without username (`UtenteFields(user, userService, true)`) gets one generated by `UserService.usernameLibero(nome, cognome)`, a random password and a disabled user; access is granted later in Gestione Utenti ("Attivo").
- Payments: `Iscrizione.quotaPersonalizzata` (null = `Corso.costoTotale`) and `Pagamento` rates; computed by `PagamentoService` (`Situazione`, `RiepilogoPagamenti`, `Ricevuta`), allowed also on inactive courses. Status badge/colour via `views/StatoPagamentoUI`. The receipt is a PDF downloaded through `views/Ricevute.link(...)`. Permissions are checked in `DocumentiService.ricevutaPer`: admin/segreteria see every receipt, a genitore only their children's; anything else gets a 404. The payments list (one row per participant: quota, pagato, residuo, stato; filters; totals of the filtered rows) is `views/ElencoPagamenti`, shared by `admin/GestionePagamentiView` (admin/segreteria, edit actions) and `genitore/PagamentiFigliView` (read-only, children only); rows come from `PagamentoService.righePagamenti()` / `righePagamentiDeiFigli(username)`. The rate form is `QuotaPagamentiDialog.apriPagamento(...)`.
- Reimbursements to teachers: `Rimborso` (insegnante, data, `mese` stored as the first day of the month, importo, metodo, nota) via `RimborsoService`. School year = reference months from September to August (`RimborsoService.inizioAnno/fineAnno`). `admin/RimborsiView` ("Rimborsi", admin/segreteria; delete admin-only) has one row per teacher, plus `RimborsiDialog` (the amount offered by default is the cost of the month's completed lessons not yet reimbursed) and `BilancioDialog`. `insegnante/MieiRimborsiView` is read-only. Calculations live in `BilancioService`: lessons are individual, so lesson minutes = attendances (enrolled students, absent ones included) × `Corso.durataLezioneMinuti`, and cost = minutes × current `tariffaOraria` / 60, rounded on the total. Lessons not held cost nothing. Maturato = completed lessons; costo previsto = all lessons except those not held.
- Documents and email: FreeMarker templates in `src/main/resources/templates` (`documenti/` for the A4 page, which must be well-formed XHTML; `email/` for the bodies). `DocumentiService` renders them, with values pre-formatted through `Formati`, and turns the same HTML into a PDF with OpenHTMLtoPDF (`io.github.openhtmltopdf`), so the email, the attachment and the web download always match. `EmailService` sends a confirmation of enrolment or a receipt with the PDF attached. Recipient: the genitore, or the allievo if there's no genitore with an email. Only admin/segreteria can send; a receipt only when there are payments. Every attempt is stored in `InvioEmail` (metadata only; corso and allievo kept as id + name, no FK). SMTP comes from `app.mail.*` (env vars `MAIL_*`): `MailConfig` creates the `JavaMailSender` only when the host is set, otherwise sending is simulated. Don't use `spring.mail.*`. Association data for the documents: `app.associazione.*`. UI: `admin/InvioEmailDialog` (from Gestione Pagamenti, Quota e pagamenti, and bulk from Partecipanti), `admin/GrigliaInvii`, `admin/EmailInviateView`. Test email (admin only): `admin/EmailDiTestDialog` from "Email inviate" → `EmailService.inviaTest` sends to any address the plain test message or a template filled with fictitious data (`DocumentiService.ricevutaEsempio()`, never saved) with its PDF, subject `[TEST]`, not recorded in `InvioEmail`; the dialog shows the SMTP config (no password) and the full error. `Ricevute` and `InvioEmailDialog` get their service from `VaadinService.getCurrent().getInstantiator().getOrCreate(...)`, so it doesn't have to be passed to every dialog.
- `Corso.prezzoPreiscrizione`: optional pre-registration price, set by hand (help text shows the typical 15% discount), between 0 and `costoTotale` (checked in `CorsoService.save`). It is informational only and never changes the amount due: apply it through the custom quota. Allievi and genitori see costo totale, pre-iscrizione and the applied quota in their course lists (`views/PrezziCorso`).
- Inactive courses (`Corso.attivo=false`): no participant/schedule/lesson/attendance changes (checked in services), hidden to insegnante and allievo.
- School years: `AnnoScolastico` entity (table `anni_scolastici`, unique `nome` "AAAA/AAAA" with consecutive years, `attivo`), managed in `admin/AnniScolasticiView` (admin/segreteria; delete admin-only and only if no course uses it) via `AnnoScolasticoService`. `Corso.annoScolastico` stores the name as a plain string (no FK, no rename). The course dialog offers `scelteCorso(valoreAttuale)` (active years + the course's own). `AnniScolasticiIniziali` seeds the table at startup, in every profile, when it is empty.
- "Le Mie Lezioni" shows a badge next to the date from `views/insegnante/QuandoLezione`: Oggi, Questa settimana, Settimana prossima, plus Settimana scorsa and In ritardo for lessons not yet completed. Weeks run Monday to Sunday.
- Lessons (`Lezione`) are generated on demand with `CorsoService.generaLezioni` ("Genera Lezioni" button), not on save. Regenerating rebuilds only the scheduled lessons still to do: completed, `nonEffettuata` and `aggiunta` lessons are kept, and their numbers are not created again.
- Lesson list: `views/ElencoLezioni` is shared by `insegnante/LezioniInsegnanteView` (own courses) and `admin/LezioniView` ("Lezioni", admin/segreteria, every active course, filter by course). Changes go through `LezioneService` with the username: `completa`, `segnaNonEffettuata`/`ripristina`, `aggiungi` (a make-up lesson via `recuperoDi` takes the missed lesson's number; otherwise max + 1; attendances are created for the current participants), `eliminaAggiunta`. Allowed: the course's teacher, admin and segreteria; deleting: teacher or admin. Attendances of a lesson not held are kept but don't count (`PresenzeAllievoDialog`, dashboard). Use `Lezione.isDaFare()` (neither completed nor not held) for lessons still to do.
- Course participants are `Iscrizione` entities (table `corso_allievi`, key corso/allievo, `orarioLezione` = the participant's lesson start time); `Corso.getAllievi()` / `Allievo.getCorsi()` are derived. Change them only through `CorsoService.iscrivi` / `rimuoviIscrizione` / `impostaOrario` (they keep the attendances of not-yet-completed lessons in sync); `Corso.iscrizioni` cascades only REMOVE, so saving a course never touches participants. Participants are shown ordered by `Iscrizione.PER_ORARIO` (`PartecipantiDialog`).
- Dates shown in the UI (grids, titles, downloads) always go through `Formati.data(LocalDate)` → "mer 7 ottobre" (year appended only if not the current one). Sortable date columns keep a comparator on the `LocalDate`. Date and time inputs are always created with `ViewSupport.datePicker(label)` / `timePicker(label)` (Italian calendar, week from Monday, `dd/MM/yyyy`, 24h); every UI has locale `it_IT` (`views/LocaleItaliano`).
- Downloads use `DownloadHandler` (`StreamResource` is deprecated).
- HTML tables use `Table` (`addHeaderRow`/`addRow`): the `NativeTable*` classes are deprecated in 25.3.
- Production bundle: `src/main/bundles/prod.bundle` (git-ignored) is reused by `package` even when a route starts using a component it didn't load before (e.g. `Grid` on `HomeView`), leaving it undefined in the browser. After such changes delete it before `./build.sh` / `./mvnw clean package`.
- Mobile layout: use `views/ViewSupport` — `isMobile()` (breakpoint 640px via `Page.windowSizeSignal()`), icon-only `iconButton` + `configureActionsColumn` for grid actions, `dialog(title, width)` with buttons in `dialog.getFooter()`. Every new grid/dialog must stay usable at 390px width.
- Grids are tables on desktop and Vaadin `Card`s on mobile (the Grid has no native card mode): after adding all columns, declare the card with `views/CardMobile.per(grid).titolo(…).sottotitolo(…).stato(badge).campo("Etichetta", valore).azioni(…).applica()` — one `campo` per desktop column (text, number or component; null/blank hides it), the same actions method as the "Azioni" column (extract it to a `createAzioni` method). `applica()` adds a card column shown only on mobile and hides the others; columns hidden for other reasons before `applica()` stay hidden on desktop, later changes go through `setVisibileSuDesktop`. Don't use `hideOnMobile`-style column hiding, mobile-only summary columns or `BadgeVariant.ICON_ONLY` on mobile.
- Icons: only **Vaadin Icons** (`VaadinIcon` / `"vaadin:..."`), the default Vaadin collection. Grid action icons: Modifica `PENCIL`, Elimina `TRASH` (+ `ButtonVariant.ERROR`), Genera Lezioni `REFRESH`, Presenze `CLIPBOARD_CHECK`, Dettagli/corsi `BOOK`, Scarica `DOWNLOAD`, Partecipanti `USERS`, Quota e pagamenti / Rimborsi `EURO`, Aggiungi pagamento `PLUS`, Ricevuta `PRINT`, Bilancio `BAR_CHART`, Invia email `ENVELOPE`. Reuse these for the same action in new views.
- Short single-choice inputs/filters (2–4 options) use `views/SegmentedControl<T>` (one-tap buttons, radiogroup semantics) instead of ComboBox/Select; statuses in grids use the Vaadin `Badge` (stable in 25.3) with a Vaadin icon (in the card: `stato(...)`).
- Attendance status (`Presenza.StatoPresenza`) is edited with `views/StatoPresenzaToggle` (a `SegmentedControl`: three one-tap buttons, selected one filled: green/orange/red), never with a ComboBox/Select. Changes are saved immediately; show a notification only on error.
- Edit dialogs validate with `asRequired` + `writeBeanIfValid` (errors shown on the fields) before calling services; never let a missing value fail on the DB.
- Logo: `images/logo-mosson.png` (`MainLayout.LOGO`), on the login page and at the bottom of the drawer.
- Browserless tests use `browserless-test-spring` and `find(...)` (`$(...)` is deprecated). The first test run after using a new Vaadin component rebuilds the frontend dev bundle and can take several minutes; stop a running dev server meanwhile.
- Architecture fix plan and remaining work: `docs/PIANO-CORREZIONI.md`.

## Requests source: `docs/RICHIESTE.md`

`docs/RICHIESTE.md` is the source of the user's requests (Italian). It is organized by date (`## RICHIESTE del GG-MM-AAAA`) and by scope (`### AMBITO`), each request being a checkbox item `* [ ] …`.

- **Reference**: each item is identified as `[AAAA-MM-GG/AMBITO.n]`, where `AMBITO` is a short upper-case key of the section: its title, or its main word for long titles (`### CORSI` → `CORSI`, `### DASHBOARD INIZIALE` → `DASHBOARD`, `### Docenti diventano Insegnanti` → `INSEGNANTI`) and `n` is the 1-based position of the item in that section, e.g. `[2026-10-10/LOGO.2]`. Don't renumber or reorder items written by the user.
- **Implementation session** — whenever the user asks to implement the requests (all open items `[ ]`, a date, or a scope), always follow these phases in order:
  1. **Read-only analysis**: read the open items, the code they touch and the Vaadin docs (MCP) needed. No file changes yet.
  2. **Clarification round — mandatory, before writing the plan.** Ask *all* the questions needed to make every open item explicit, with `AskUserQuestion` (up to 4 questions per call, several calls if needed; concrete options with the recommended one first and a short description of the consequences). Don't fill gaps with silent assumptions and don't postpone questions to the execution. Always ask about:
     - incomplete or contradictory items (e.g. a truncated sentence) and terms that can be read in more than one way;
     - who: which roles see/do what, including roles the item doesn't mention but are affected (e.g. a new view: who else can access it? may they delete?);
     - where and how in the UI: which view, dialog or menu entry; new view vs extension of an existing one; behaviour on mobile;
     - data: new fields and whether they are mandatory, defaults, effect on existing data and on production (schema changes, renames, migrations);
     - side effects and edge cases (e.g. what happens to related records when something is added/removed);
     - anything not requested that you would add or change "by the way" (validation, refactoring): propose it, don't just do it.
     Skip a question only when the answer is unambiguous from the item text or from the code. When the round is over, write a short summary of the decisions.
  3. **Record the answers**: in the plan (*Analisi*, sub-section **Decisioni**, one line per decision with the item reference) and, in the same plan commit, under the item in `docs/RICHIESTE.md` as an indented line `_Chiarimento AAAA-MM-GG: …_` (never rewrite the user's text). Items the user chooses to leave open get `_(da specificare: …)_` and go in *Fuori ambito*.
  4. **Plan and execution, without further approval**: follow the workflow below as already defined — plan in `docs/piani/` committed alone (each step names the request(s) it completes, column *Richiesta*), then one step at a time: implement, verify, mark the step in the plan, mark the request in `docs/RICHIESTE.md` when the step completes it, commit; new work goes in the plan as new steps; close with the *Registro*.
  5. **Questions during execution**: if a new ambiguity appears, ask immediately when the decision is the user's (behaviour, data, production, scope) and record the answer as in phase 3 (commit the plan/RICHIESTE update before continuing); purely technical choices are decided and noted in the plan.
- **Completion**: the commit that completes an item also marks it in `docs/RICHIESTE.md` as `* [x] … _(completata AAAA-MM-GG)_` and has the reference in the subject: `<step id> [AAAA-MM-GG/AMBITO.n] <short description in Italian>`, e.g. `3 [2026-10-10/LOGO.2] Logo in fondo al menu laterale`. One commit per completed request; if a step completes several closely tied items, list all references (`[2026-10-10/LOGO.1][2026-10-10/LOGO.3]`). Preparatory steps that complete no item use the normal subject. An item found already implemented is marked with a note (`_(già presente, verificata AAAA-MM-GG)_`) in its own commit.
- Never mark an item completed if it is only partially done: leave `[ ]` and add a short note under it.

## Feature requests from prompts: `docs/FUNZIONALITA.md`

When a prompt asks for a new feature or a change to the application's behaviour, UI or data (not for questions, reviews, bug fixes or changes to tooling/configuration/docs, which follow the normal workflow), first record it, then process it. A `UserPromptSubmit` hook in `.claude/settings.json` reminds you of this on every prompt; when in doubt whether a prompt is a feature request, ask.

1. **Record**: add the request to `docs/FUNZIONALITA.md`, same format as `RICHIESTE.md`: section `## FUNZIONALITA del GG-MM-AAAA` (today, created if missing), scope `### AMBITO` (reuse an existing scope of that date when it fits), one `* [ ] …` item per distinct request, with the user's words reported faithfully (split a multi-request prompt into items, don't rephrase or add interpretations). Reference: `[F-AAAA-MM-GG/AMBITO.n]` (the `F-` prefix distinguishes it from `RICHIESTE.md` items). Commit the file alone: `Funzionalità: <short description>`.
2. **Process**: immediately run the *implementation session* described above on the new items, with `docs/FUNZIONALITA.md` in place of `docs/RICHIESTE.md` for clarifications (`_Chiarimento …_`), completion marks and commit references (e.g. `2 [F-2026-10-11/CORSI.1] …`).

## Workflow: plan first, then execute with one commit per step

Every user request that changes the repository (code, configuration, build, docs) follows this workflow — no exceptions for "small" changes. Pure questions, explanations or reviews without changes don't need a plan.

### 1. Plan first

For `docs/RICHIESTE.md` implementation sessions the mandatory clarification round (see *Requests source*) comes before this step.

Before touching any code, write the plan to a new file `docs/piani/AAAA-MM-GG-<argomento>.md` (Italian, e.g. `docs/piani/2026-10-07-responsive-mobile.md`). Analysis and read-only checks (reading code, Vaadin MCP docs, running the app) can happen before writing it, to make the plan accurate.

The plan contains:
- **Richiesta**: the user's request, in one or two sentences.
- **Analisi**: what was found (problems, constraints, references to docs/APIs checked).
- **Passi**: a table of numbered steps (`1`, `2`, … or `1.1`, `1.2` if grouped in phases) with columns *Passo · Cosa · File · Verifica · Stato* (`[ ]`, `[x]`, `[~]`). Each step must be small enough to be one coherent commit.
- **Fuori ambito**: what is deliberately not done, and open decisions.
- **Registro**: filled in during execution (outcome, test results, problems found).

Commit the plan alone as the first commit: `Piano: <argomento>`. Then show the user a short summary of the steps and **proceed with the execution without waiting for approval**, unless the plan contains a decision that is genuinely the user's (e.g. version numbers, deleting data, changing production behaviour): in that case ask first.

Existing plan `docs/PIANO-CORREZIONI.md` stays as the reference for the architecture review; new requests get their own file in `docs/piani/`.

### 2. Execute: one commit per step

Commit after **every single step** of the plan, not at the end of a phase or of the whole plan. The user has authorized these commits in advance; push is still only on explicit request.

For each step:
1. Implement only that step.
2. Verify it: at least `./mvnw -q compile`; run `./mvnw test` when the step touches services, repositories, views or tests. Do not commit a step that doesn't compile or breaks tests — fix it first.
3. In the same commit, mark the step `[x]` in the plan (or `[~]` with a short note if partial/deferred).
4. Stage only the files of that step (`git add <files>`, never `git add -A` with unrelated changes) and commit on the current branch (`develop`), not on `main`.
5. Commit message: `<step id> <short description in Italian>` as subject, e.g. `1.5 CorsiView: Binder locale al dialog`, with a body explaining what changed and why if not obvious.

If a step turns out to be already done or unnecessary, mark it in the plan with a note and commit just the plan update. If new work emerges during execution, add it to the plan as new steps (committing the plan change) rather than doing it silently. Pre-existing uncommitted changes of the user go in a separate commit, never mixed into a step.

### 3. Close

Last commit: fill in the **Registro** of the plan with the final verification (build, tests, browser checks) and anything left open. Then report to the user with the list of commits.
