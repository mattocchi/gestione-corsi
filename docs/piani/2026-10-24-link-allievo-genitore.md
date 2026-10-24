# Piano: link tra allievo e genitore

## Richiesta

[F-2026-10-24/COLLEGAMENTI.1] Consultando un allievo o un genitore, un link per passare al genitore o all'allievo collegato.

## Analisi

- `admin/AllieviView` (Gestione Allievi) e `admin/GenitoriView` (Genitori), admin e segreteria, hanno ognuna il proprio dialog di modifica (`openDialog`): campo "Genitore" (ComboBox legato al binder) e campo "Figli" (`MultiSelectComboBox`).
- Le viste non leggono parametri dall'indirizzo; i dialog si aprono con `ViewSupport.dialog`.
- `UtenteFields` ha un suo `Binder<User>`: per sapere se ci sono modifiche serve un `hasChanges()`.

### Decisioni

- [F-2026-10-24/COLLEGAMENTI.1] Link nei dialog di modifica: sotto il campo Genitore dell'allievo, sotto il campo Figli del genitore (uno per figlio); collegamenti salvati, non quelli appena scelti nel campo.
- [F-2026-10-24/COLLEGAMENTI.1] Il link porta a `allievi?id=…` / `genitori?id=…` con il dialog di quella persona aperto; Indietro torna alla pagina di prima.
- [F-2026-10-24/COLLEGAMENTI.1] Con modifiche non salvate chiede conferma ("Lasciare le modifiche?").
- Scelte tecniche: pulsante terziario piccolo con icona, il dialog si chiude prima di navigare; un `id` inesistente apre solo la lista.

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | Parametro `id` nelle due viste (apre il dialog); helper del link con conferma delle modifiche; `UtenteFields.hasChanges()` | `AllieviView`, `GenitoriView`, nuovo `admin/SchedaCollegata`, `UtenteFields` | `./mvnw test` | [x] |
| 2 | Link nei dialog; test browserless (allievo → genitore, genitore → figlio, conferma con modifiche) | `AllieviView`, `GenitoriView`, `ViewAccessTest` | `./mvnw test`; browser | [x] Il dialog di una vista si chiude lasciando la pagina (`onDetach`): con Indietro restava aperto sopra l'altra |
| 3 | `CLAUDE.md` e registro finale | `CLAUDE.md`, questo piano | build e test | [ ] |

## Fuori ambito

- Link nelle liste (colonne Genitore e Figli) e in altre viste.

## Registro

