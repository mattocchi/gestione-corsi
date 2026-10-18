# Piano: nome dell'applicazione

## Richiesta

[F-2026-10-18/NOME.1] Sostituire "Gestione Corsi Musica" (login e altre sezioni) con un nome più efficace per
insegnanti, segreteria, genitori e allievi.

## Analisi

- Il testo è in tre punti: intestazione dopo l'accesso (`MainLayout`), pagina di accesso (`LoginView`) e pagine del
  recupero password (`PaginaPubblica`); il suffisso "| Gestione Corsi" è nei titoli della scheda di accesso e recupero
  password. Le altre viste hanno titoli di scheda senza nome dell'applicazione.
- `LocaleItaliano` (`VaadinServiceInitListener`) configura già ogni UI: punto unico per il suffisso dei titoli
  (`UI.addAfterNavigationListener`, titolo corrente da `UIInternals.getTitle()`).
- Il menu "Gestione Corsi" (vista dei corsi) è un'altra cosa e resta.

### Decisioni

- [F-2026-10-18/NOME.1] Nuovo nome "Area Corsi Mosson", in una sola costante usata da intestazione, accesso e pagine pubbliche.
- [F-2026-10-18/NOME.1] Sottotitolo solo su accesso e pagine del recupero password: "Lezioni, presenze e pagamenti per insegnanti, segreteria, famiglie e allievi".
- [F-2026-10-18/NOME.1] Titolo della scheda di ogni pagina nel formato "Pagina | Area Corsi Mosson".

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | Costante del nome e sottotitolo; intestazione condivisa da accesso e pagine pubbliche | `MainLayout`, `LoginView`, `PaginaPubblica` | test, browser | [ ] |
| 2 | Suffisso del nome nel titolo della scheda di tutte le pagine | `LocaleItaliano`, `@PageTitle` delle pagine pubbliche | test | [ ] |
| 3 | CLAUDE.md, verifica, registro | | browser desktop/390px | [ ] |

## Fuori ambito

- Nome delle voci di menu e delle email (l'associazione nelle email resta `app.associazione.nome`).

## Registro

