# Piano: genitore facoltativo e inserimento rapido dell'allievo nel corso

## Richiesta

Genitore non obbligatorio, perché alcuni allievi sono adulti ([F-2026-10-19/ALLIEVI.1]); inserimento rapido di un allievo che non esiste (nome e cognome) direttamente nel corso, completando poi le informazioni ([F-2026-10-19/ALLIEVI.2]).

## Analisi

- Il genitore è obbligatorio solo nel form di Gestione Allievi (`AllieviView`: `asRequired("Scegli il genitore dell'allievo")`). La colonna `allievi.genitore_id` è nullable (`@JoinColumn` senza `nullable = false`, nessun vincolo creato da `ddl-auto`), e documenti, email (destinatario = allievo se manca il genitore), pagamenti, filtri e indirizzo già gestiscono `getGenitore() == null`.
- Gli allievi si aggiungono a un corso da `PartecipantiDialog` (ComboBox "Aggiungi un allievo" + `CorsoService.iscrivi`). La ComboBox di Vaadin 25.3 accetta valori personalizzati (`setAllowCustomValue`, `addCustomValueSetListener`): il testo scritto che non corrisponde a nessun allievo diventa l'avvio dell'inserimento rapido.
- `AllievoService.salva(allievo, user, password)` con username vuoto genera già username libero, password casuale e utente disattivato: l'inserimento rapido lo riusa. Codice fiscale e data di nascita mancanti sono già segnalati in Gestione Allievi (badge "Dati mancanti").

### Decisioni

- [F-2026-10-19/ALLIEVI.1] Genitore solo facoltativo (aiuto "Lascia vuoto per gli allievi adulti"), nessun nuovo campo; ricevute, conferme ed email vanno all'allievo stesso, come già oggi quando il genitore manca.
- [F-2026-10-19/ALLIEVI.2] Dal campo "Aggiungi un allievo" di Partecipanti: si scrive nome e cognome e, se non esiste, si apre un mini-form "Nuovo allievo" con nome e cognome già compilati (prima parola = nome, il resto = cognome, modificabili); Salva crea l'allievo e lo iscrive subito al corso.
- [F-2026-10-19/ALLIEVI.2] Mini-form: nome e cognome obbligatori, email e telefono facoltativi; utente generato e disattivato, nessun genitore; il resto si completa da Gestione Allievi.
- [F-2026-10-19/ALLIEVI.2] Omonimi: se esiste già un allievo con lo stesso nome e cognome (senza distinzione di maiuscole), il mini-form lo segnala con il pulsante per iscrivere quello esistente; si può comunque creare l'omonimo.
- Decisione tecnica: la ComboBox non ha una voce "+ Nuovo allievo" nativa nell'elenco; l'inserimento parte dal valore personalizzato (Invio o uscita dal campo dopo aver scritto un nome non trovato), spiegato dal testo di aiuto del campo. Chi: admin e segreteria (chi apre Partecipanti), solo per i corsi attivi.

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | Genitore facoltativo nel form dell'allievo | `admin/AllieviView`, `ViewAccessTest` | `./mvnw test` | [x] |
| 2 | Servizio: creazione rapida (nome, cognome, email, telefono) e ricerca omonimi | `AllievoService`, test | `./mvnw test` | [x] |
| 3 | Partecipanti: nome non trovato → mini-form "Nuovo allievo" (omonimi, crea e iscrive) | `admin/PartecipantiDialog`, `admin/NuovoAllievoRapidoDialog`, `ViewAccessTest` | `./mvnw test`, browser desktop e 390px | [x] |
| 4 | CLAUDE.md e registro | `CLAUDE.md`, questo piano | — | [x] |

## Fuori ambito

- Flag "maggiorenne" o controlli sull'età.
- Inserimento rapido da altre viste (Gestione Allievi ha già il form completo).

## Registro

- Passo 1: genitore facoltativo in `AllieviView` (pulsante per svuotarlo, aiuto "Lascia vuoto per gli allievi adulti"); test aggiornato e nuovo test di creazione dell'allievo adulto.
- Passo 2: `AllievoService.creaRapido` e `omonimi`, test in `AllievoServiceTest`.
- Passo 3: `NuovoAllievoRapidoDialog` e valore personalizzato nella ComboBox di `PartecipantiDialog`; test browserless (creazione con email non valida rifiutata, poi creata e iscritta; omonimo proposto e iscritto; omonimo già iscritto non proposto). Nel test, l'allievo va ricaricato prima di eliminarlo: la copia letta prima ha ancora l'iscrizione appena rimossa.
- Verifica finale: `./mvnw test` 171 test verdi; browser desktop (Partecipanti di Canto: "Paolo Adulto" + Invio → mini-form → Crea e iscrivi → nell'elenco; in Gestione Allievi con badge "Dati mancanti"; un nome già presente nell'elenco con Invio viene selezionato, non creato) e 390px (mini-form senza scorrimento orizzontale).
- Produzione: nessuno script SQL; `allievi.genitore_id` è già nullable (mai dichiarato `nullable = false`).
