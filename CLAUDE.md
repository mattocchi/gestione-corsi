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

Four roles: `ADMIN` (full management), `DOCENTE` (teacher lessons), `STUDENTE` (own courses), `GENITORE` (children's courses).
- Views are protected with `@RolesAllowed` / `@PermitAll`; `MainLayout` itself is `@PermitAll` (layouts are access-checked too).
- The side menu is built from `@Menu` annotations via `MenuConfiguration.getMenuEntries()`, which already filters by access — don't add manual role checks to the menu.
- `SecurityService` wraps Vaadin's `AuthenticationContext` (`getUsername()`, `hasRole()`, `logout()`).

### Package layout

```
it.attocchi/
├── entity/         JPA entities (id-based equals/hashCode)
├── repository/     Spring Data JPA repositories — used only by services (and DataInitializer)
├── service/        Business services, @Transactional(readOnly = true) by default
├── security/       SecurityConfig, SecurityService, UserDetailsServiceImpl
├── views/          Vaadin views by role (admin/, docente/, studente/, genitore/) — depend on services only
├── Application.java       AppShellConfigurator: loads Aura theme + styles.css via @StyleSheet
└── DataInitializer.java   Seeds test data (dev profile only)
```

### Profiles and database

- `application.properties`: shared config; `spring.profiles.default=dev`.
- **dev** (`application-dev.properties`): H2 in-memory, `create-drop`, H2 console at `/h2-console`, `DataInitializer` seeds users (admin/admin, docente1/docente1, …).
- **prod** (`application-prod.properties`): MySQL, credentials from env vars `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`; `ddl-auto=update` (Flyway still to do). Started with `--spring.profiles.active=prod`.

### Key design patterns

- `spring.jpa.open-in-view=false`: everything a view displays must be loaded by the service (use `JOIN FETCH` queries for lazy collections). Browserless tests in `ViewAccessTest` catch `LazyInitializationException`.
- Theme is **Aura**: use unprefixed variants (`ButtonVariant.PRIMARY`), not `LUMO_*` or `LumoUtility`.
- Passwords are only set via `UserService.save(user, rawPassword)` (encoded there; null/blank keeps the existing one).
- Lessons (`Lezione`) are generated on demand with `CorsoService.generaLezioni` ("Genera Lezioni" button), not on save.
- Dates shown in the UI (grids, titles, downloads) always go through `Formati.data(LocalDate)` → "mer 7 ottobre" (year appended only if not the current one). Sortable date columns keep a comparator on the `LocalDate`. `DatePicker` inputs keep the locale numeric format.
- Downloads use `DownloadHandler` (`StreamResource` is deprecated).
- Mobile layout: use `views/ViewSupport` — `hideOnMobile(grid, columns...)` (breakpoint 640px via `Page.windowSizeSignal()`), icon-only `iconButton` + `configureActionsColumn` for grid actions, `dialog(title, width)` with buttons in `dialog.getFooter()`. Every new grid/dialog must stay usable at 390px width. Don't rely on `setAutoWidth(true)` for columns of grids that hide columns on mobile: the width is not recalculated after hiding (columns stay at 100px) and is computed only from the rows visible at first render — use explicit widths. When more than ~3 columns are needed on mobile, add a mobile-only summary column (`LitRenderer`, two lines) and hide the single columns, as in `LezioniDocenteView`.
- Icons: only **Vaadin Icons** (`VaadinIcon` / `"vaadin:..."`), the default Vaadin collection. Grid action icons: Modifica `PENCIL`, Elimina `TRASH` (+ `ButtonVariant.ERROR`), Genera Lezioni `REFRESH`, Presenze `CLIPBOARD_CHECK`, Dettagli/corsi `BOOK`, Scarica `DOWNLOAD`. Reuse these for the same action in new views.
- Short single-choice inputs/filters (2–4 options) use `views/SegmentedControl<T>` (one-tap buttons, radiogroup semantics) instead of ComboBox/Select; statuses in grids use the Vaadin `Badge` (stable in 25.3) with a Vaadin icon, `bindThemeVariant(BadgeVariant.ICON_ONLY, ViewSupport.isMobile())` on mobile.
- Attendance status (`Presenza.StatoPresenza`) is edited with `views/StatoPresenzaToggle` (a `SegmentedControl`: three one-tap buttons, selected one filled: green/orange/red), never with a ComboBox/Select. Changes are saved immediately; show a notification only on error.
- Browserless tests use `browserless-test-spring` and `find(...)` (`$(...)` is deprecated).
- Architecture fix plan and remaining work: `docs/PIANO-CORREZIONI.md`.

## Workflow: plan first, then execute with one commit per step

Every user request that changes the repository (code, configuration, build, docs) follows this workflow — no exceptions for "small" changes. Pure questions, explanations or reviews without changes don't need a plan.

### 1. Plan first

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
