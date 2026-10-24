# Piano: togliere la nota sul prezzo di listino

## Richiesta

[F-2026-10-24/CONFERMA.4] Nella conferma di iscrizione togliere la frase "L'importo dovuto è la quota indicata sopra, già scontata: il prezzo di listino è solo un riferimento."

## Analisi

- La frase è `importi.nota`, preparata da `DocumentiService.importi` (variante senza "già scontata" per la maggiorazione) e mostrata dalla macro condivisa `templates/importi.ftlh`, usata da conferma e ricevuta, nel PDF e nell'email.
- Lo stile `p.nota` di `documenti/comune.ftlh` serve solo a quella frase; `DocumentiServiceTest.scontoApplicato_…` ne verifica la presenza.

### Decisioni

- [F-2026-10-24/CONFERMA.4] Tolta ovunque: conferma e ricevuta, PDF ed email, con lo sconto e con la maggiorazione.

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | Tolta la nota da service, macro e stili; test aggiornato (la frase non c'è più) | `DocumentiService`, `importi.ftlh`, `documenti/comune.ftlh`, `DocumentiServiceTest` | `./mvnw test` | [ ] |
| 2 | Registro finale | questo piano | build e test | [ ] |

## Fuori ambito

- Nessuno.

## Registro

