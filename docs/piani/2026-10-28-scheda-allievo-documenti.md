# Scheda dell'allievo: dati base e dati per i documenti

## Richiesta

Riorganizzare la scheda dell'allievo come quella del genitore, separando i dati base da quelli per l'intestazione dei
documenti, utili per gli allievi adulti — [F-2026-10-28/INTESTATARIO.2]

## Analisi

- `SchedaAllievo` (Gestione Allievi, I Miei Figli, Profilo dell'allievo) ha la sezione "Allievo" con nome, cognome,
  codice fiscale, data e luogo di nascita (`AnagraficaFields.getDatiPersonali`), poi genitore, contatti, la sezione
  "Intestazione dei documenti fiscali" (solo adulto, `conIntestazione`) e l'accesso.
- Nel Profilo dell'allievo adulto l'intestazione è aggiunta dopo la scheda, con il proprio Salva.

### Decisioni

- [F-2026-10-28/INTESTATARIO.2] Stessa scheda per tutti gli allievi: "Allievo" = nome, cognome, data e luogo di
  nascita; poi "Genitore" e contatti; poi "Dati per i documenti" con il codice fiscale dell'allievo; infine l'accesso.
- [F-2026-10-28/INTESTATARIO.2] Solo per l'allievo adulto, dentro "Dati per i documenti", nome, cognome e codice
  fiscale dell'intestatario in un riquadro chiuso "Intesta l'attestazione a un'altra persona", aperto se già
  compilato. Il genitore resta come ora (il suo codice fiscale è già quello dell'intestatario).
- Nel Profilo dell'allievo adulto il riquadro è dentro la scheda (resto in sola lettura) con il Salva sotto la scheda.

## Passi

| Passo | Cosa | File | Verifica | Stato | Richiesta |
|---|---|---|---|---|---|
| 1 | `SchedaAllievo` con le nuove sezioni, riquadro dell'intestatario (`IntestazioneFields.getRiquadro`), Profilo dell'allievo adulto; test e CLAUDE.md | `SchedaAllievo`, `AnagraficaFields`, `IntestazioneFields`, `ProfiloView`, `ViewAccessTest`, `CLAUDE.md` | `./mvnw test` | [ ] | F-2026-10-28/INTESTATARIO.2 |
| 2 | Registro | piano | — | [ ] | — |

## Fuori ambito

- Scheda del genitore (già divisa) e dell'insegnante.

## Registro

