# Cronologia dei documenti aperti da genitori e allievi

## Richiesta

Registrare ogni volta che un genitore o un allievo apre un documento (anteprima o PDF scaricato) e mostrarlo ad admin
e segreteria come cronologia — [F-2026-10-27/CRONOLOGIA.1]

## Analisi

- Genitori e allievi adulti aprono i PDF solo dall'elenco famiglia di `DocumentiView` (`azioniFamiglia`), con
  `MenuPdf.soloIcona`: due link ("Anteprima in una nuova scheda" inline, "Scarica PDF"), ognuno con il proprio
  `DownloadHandler` che genera il PDF a ogni richiesta (permessi in `DocumentiService.*Per`).
- Registrare nel `DownloadHandler` di `MenuPdf` permette di distinguere anteprima e scarico; la registrazione avviene
  solo se il PDF è stato generato (niente righe per i 404).
- Registro degli invii email come modello: `InvioEmail` (solo metadati, nomi senza chiavi esterne), `GrigliaInvii`,
  `EmailInviateView` (admin e segreteria). Nuova tabella: `ddl-auto=update` la crea, nessuno script di migrazione.

### Decisioni

- [F-2026-10-27/CRONOLOGIA.1] Solo genitori e allievi adulti (l'elenco famiglia di Documenti); staff e insegnanti no.
- [F-2026-10-27/CRONOLOGIA.1] Registrate senza scadenza: data e ora, utente (username e nome), tipo, anteprima o
  scarico, allievo, corso, anno, nome del file.
- [F-2026-10-27/CRONOLOGIA.1] Visibile in "Email inviate", rinominata "Cronologia documenti" (stessa route
  `email-inviate`): schede "Email inviate" e "Documenti aperti", filtri per tipo e ricerca in comune.
- [F-2026-10-27/CRONOLOGIA.1] L'admin può eliminare una riga (con conferma), la segreteria no.

## Passi

| Passo | Cosa | File | Verifica | Stato | Richiesta |
|---|---|---|---|---|---|
| 1 | Entità `AperturaDocumento`, repository, `CronologiaDocumentiService` (registra, elenco, elimina solo admin); `MenuPdf` con la registrazione per anteprima/scarico; `DocumentiView` registra per l'elenco famiglia; test | entity, repository, service, `MenuPdf`, `DocumentiView`, test | `./mvnw test` | [ ] | |
| 2 | "Cronologia documenti": schede in `EmailInviateView`, griglia dei documenti aperti con Elimina (admin); test; CLAUDE.md | `EmailInviateView`, `GrigliaAperture`, test, CLAUDE.md | `./mvnw test` | [ ] | F-2026-10-27/CRONOLOGIA.1 |

## Fuori ambito

- Aperture fatte da staff e insegnanti; anteprime dei PDF dentro i dialog di invio email (solo staff).

## Registro

