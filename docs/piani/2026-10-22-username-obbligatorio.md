# Piano: username vuoto nel salvataggio di un utente

## Richiesta

Creando un genitore compare "Errore nel salvataggio: Validation failed for classes [it.attocchi.entity.User] … propertyPath=username … must not be blank": il salvataggio arriva al database con lo username vuoto.

## Analisi

- `User.username` è `@NotBlank`: la violazione scatta solo al persist, quindi nessun controllo nel dialog l'ha fermata. L'eccezione è intercettata dal dialog e mostrata come notifica, quindi non compare nel log del server: non si sa da quale dialog arrivi. Le strade possibili sono due:
  - `UtenteFields` (dialog di genitori, insegnanti, allievi): lo username è `asRequired`, ma un valore di soli spazi non è "vuoto" per il Binder; il setter poi lo riduce a `null` → violazione al persist.
  - `UtentiView` (Gestione Utenti, anche per creare un utente con ruolo GENITORE): nessun campo è validato (`binder.writeBean` senza `asRequired`); username, nome o cognome vuoti arrivano al database. La password obbligatoria per un utente nuovo è controllata solo dal servizio.
- Regola del progetto (CLAUDE.md): i dialog validano con `asRequired` + `writeBeanIfValid`, mai un valore mancante che fallisce sul database.

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | `UtenteFields`: username obbligatorio rifiuta anche i soli spazi; test sul dialog Nuovo Genitore | `views/UtenteFields`, `ViewAccessTest` | `./mvnw test` | [x] |
| 2 | `UtentiView`: username (non vuoto, univoco per un nuovo utente), nome, cognome obbligatori e password obbligatoria per un nuovo utente, errori sui campi (`writeBeanIfValid`); test | `views/admin/UtentiView`, `ViewAccessTest` | `./mvnw test` | [x] |
| 3 | Registro | questo piano | — | [x] |

## Fuori ambito

- Registrare nel log gli errori mostrati come notifica.

## Registro

- Passo 1: riprodotto con `segreteria_nuovoGenitoreConUsernameDiSoliSpazi_erroreSulCampo` (falliva: il campo risultava valido), poi corretto; `./mvnw test` 201 test verdi.
- Passo 2: riprodotto con `admin_nuovoUtenteSenzaDatiObbligatori_erroriSuiCampi` (falliva: nessun errore sui campi), poi corretto; il test verifica anche username di soli spazi, username già in uso e il salvataggio con spazi ai lati (salvato senza, con la scheda genitore creata); `./mvnw test` 202 test verdi.
- Il caso esatto dell'utente non è nel log del server (l'errore è mostrato solo come notifica): corrette entrambe le strade possibili.
- Non verificato nel browser. Nessuna modifica al database.
