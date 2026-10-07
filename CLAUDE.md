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
- Downloads use `DownloadHandler` (`StreamResource` is deprecated).
- Mobile layout: use `views/ViewSupport` — `hideOnMobile(grid, columns...)` (breakpoint 640px via `Page.windowSizeSignal()`), icon-only `iconButton` + `configureActionsColumn` for grid actions, `dialog(title, width)` with buttons in `dialog.getFooter()`. Every new grid/dialog must stay usable at 390px width.
- Browserless tests use `browserless-test-spring` and `find(...)` (`$(...)` is deprecated).
- Architecture fix plan and remaining work: `docs/PIANO-CORREZIONI.md`.

## Working on a plan: one commit per step

When executing a plan in `docs/` (e.g. `docs/PIANO-CORREZIONI.md`), commit after **every single step** (each numbered row such as `1.3`, `7.2`), not at the end of a phase or of the whole plan. The user has authorized these commits in advance; push is still only on explicit request.

For each step:
1. Implement only that step.
2. Verify it: at least `./mvnw -q compile`; run `./mvnw test` when the step touches services, repositories, views or tests. Do not commit a step that doesn't compile or breaks tests — fix it first.
3. In the same commit, mark the step `[x]` in the plan (or `[~]` with a short note if partial/deferred).
4. Stage only the files of that step (`git add <files>`, never `git add -A` with unrelated changes) and commit on the current branch (`develop`), not on `main`.
5. Commit message: `<step id> <short description in Italian>` as subject, e.g. `1.5 CorsiView: Binder locale al dialog`, with a body explaining what changed and why if not obvious.

If a step turns out to be already done or unnecessary, mark it in the plan with a note and commit just the plan update. Pre-existing uncommitted changes of the user go in a separate commit, never mixed into a step.
