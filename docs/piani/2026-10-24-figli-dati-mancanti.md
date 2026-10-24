# Piano: dati mancanti dei figli in "I miei figli"

## Richiesta

[F-2026-10-24/FIGLI.1] Con il profilo genitore, in "I miei figli", evidenziare se mancano dati del figlio, come il badge che vede la segreteria.

## Analisi

- La segreteria vede il badge "Dati mancanti" in Gestione Allievi (`AllieviView.datiMancanti`) e in Documenti (`DocumentiView.datiMancanti(avviso)`), con l'avviso di `DocumentiService.avvisoDatiMancanti(allievo)` (codice fiscale per le ricevute, data di nascita per conferme e ricevute).
- `genitore/FigliView` mostra i figli (Nome, Data Nascita, N. Corsi) senza badge; il dialog "Dati di …" (Modifica) permette al genitore di completare codice fiscale e data di nascita.

### Decisioni

- [F-2026-10-24/FIGLI.1] Badge "Dati mancanti" accanto al nome (su mobile come stato della card), tooltip con cosa manca, a cosa serve e "Completali con il pulsante Modifica".
- [F-2026-10-24/FIGLI.1] Nel dialog "Dati di …" un avviso in alto con i dati mancanti; i campi restano facoltativi.
- [F-2026-10-24/FIGLI.1] Niente in dashboard.

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | Badge nella lista e nella card, avviso nel dialog (badge condiviso `DocumentiView.datiMancanti` reso pubblico); test browserless | `FigliView`, `DocumentiView`, `ViewAccessTest` | `./mvnw test` | [x] |
| 2 | `CLAUDE.md` e registro finale | `CLAUDE.md`, questo piano | build e test | [ ] |

## Fuori ambito

- Dashboard del genitore; allievi adulti (non hanno "I miei figli").

## Registro

