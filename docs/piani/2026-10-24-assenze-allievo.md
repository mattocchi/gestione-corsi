# Piano: assenze dell'allievo nella conferma

## Richiesta

[F-2026-10-24/CONFERMA.5] Nella conferma di iscrizione, sostituire la regola "Se l'allievo non può venire" con il testo dato dall'utente ("Assenze dell'allievo (solo lezioni individuali)").

## Analisi

- Le regole sono nella macro `templates/regole-corso.ftlh`, usata dal PDF e dall'email (`email=true`, stili in linea).
- Il PDF deve stare in una pagina A4: `DocumentiServiceTest.conferma_suUnaSolaPagina…` controlla il caso più lungo; stili compatti in `documenti/comune.ftlh` (`ul.regole`).
- `DocumentiServiceTest` verifica le frasi della regola attuale, da aggiornare.

### Decisioni

- [F-2026-10-24/CONFERMA.5] Titolo "Assenze dell'allievo (solo lezioni individuali)", al posto di "Se l'allievo non può venire".
- [F-2026-10-24/CONFERMA.5] Testo dell'utente invariato: primo paragrafo intero, i tre casi in un sotto-elenco puntato; nel PDF e nell'email.
- [F-2026-10-24/CONFERMA.5] Se la conferma non sta più in una pagina A4, compattare ancora (spazi, interlinea, testo fino a 9 pt).

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | Nuova regola con sotto-elenco nella macro (PDF ed email), stili del sotto-elenco, test sulle frasi nuove e sulla pagina unica | `regole-corso.ftlh`, `documenti/comune.ftlh`, `DocumentiServiceTest` | `./mvnw test` | [ ] |
| 2 | `CLAUDE.md` e registro finale | `CLAUDE.md`, questo piano | build e test | [ ] |

## Fuori ambito

- Le altre due regole restano come sono.

## Registro

