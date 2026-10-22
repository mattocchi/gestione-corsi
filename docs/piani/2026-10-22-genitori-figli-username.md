# Piano: figli dalla scheda genitore e username generato

## Richiesta

Associare gli allievi anche dalla scheda del genitore ([F-2026-10-22/GENITORI.1]); creando un genitore con lo username vuoto, generarlo come per gli allievi ([F-2026-10-22/GENITORI.2]).

## Analisi

- Il genitore di un allievo è `Allievo.genitore` (`Genitore.figli` è `mappedBy`): oggi si sceglie solo nel dialog dell'allievo (`AllieviView`, ComboBox "Genitore").
- `GenitoriView` usa `UtenteFields(user, userService)` (username obbligatorio) e `GenitoreService.salva(genitore, user, password)`. Per gli allievi `UtenteFields(user, userService, true)` + `AllievoService.salva` generano lo username con `UserService.usernameLibero` e disattivano l'utente.
- `MultiSelectComboBox` non è ancora usato nel progetto: nuovo componente → ricostruzione del bundle di sviluppo al primo test e `src/main/bundles/prod.bundle` da cancellare prima del prossimo build di produzione (CLAUDE.md).

### Decisioni

- [F-2026-10-22/GENITORI.1] Campo `MultiSelectComboBox` "Figli" nel dialog Nuovo/Modifica Genitore, salvato con Salva insieme al resto (anche per un genitore nuovo).
- [F-2026-10-22/GENITORI.1] Si possono scegliere anche allievi con un altro genitore (etichetta "… (figlio di …)"): passano a questo genitore e al salvataggio un avviso elenca chi è stato spostato. Togliere un figlio lo lascia senza genitore.
- [F-2026-10-22/GENITORI.2] Genitore nuovo con username vuoto: username generato da nome e cognome, utente disattivato e password casuale (accesso da attivare in Gestione Utenti); la password serve solo se si scrive lo username.
- [F-2026-10-22/GENITORI.2] Lo stesso per i nuovi insegnanti.
- Tecnica: la generazione passa in `UserService.accessoGeneratoSeManca(user, password)`, usato da allievi, genitori e insegnanti.

## Passi

| Passo | Cosa | Richiesta | File | Verifica | Stato |
|---|---|---|---|---|---|
| 1 | Username generato per genitori e insegnanti nuovi (servizio comune, dialog con accesso facoltativo); test | [F-2026-10-22/GENITORI.2] | `service/UserService`, `AllievoService`, `GenitoreService`, `InsegnanteService`, `views/admin/GenitoriView`, `InsegnantiView`, test | `./mvnw test` | [x] |
| 2 | Campo "Figli" nel dialog del genitore, `GenitoreService.salva` con i figli e avviso degli spostati; test | [F-2026-10-22/GENITORI.1] | `service/GenitoreService`, `views/admin/GenitoriView`, test | `./mvnw test` | [x] |
| 3 | CLAUDE.md e registro (promemoria: cancellare `prod.bundle` prima del build) | — | `CLAUDE.md`, questo piano | — | [ ] |

## Fuori ambito

- Associare figli dal profilo del genitore stesso.

## Registro
