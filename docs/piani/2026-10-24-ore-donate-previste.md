# Piano: ore donate previste in grande e totale per admin e segreteria

## Richiesta

[F-2026-10-24/DASHBOARD.3] Nel riquadro "Ore donate" dell'insegnante mostrare in grande tutte le ore calcolate (previste) e nella descrizione quelle già fatte; mostrare lo stesso dato ad admin e segreteria come totale di tutti gli insegnanti.

## Analisi

- `BilancioService.oreDonate(username)` → `OreDonate(minutiSvolti, minutiPrevisti)` per l'insegnante senza tariffa (anche con forfait), sui corsi attivi, lezioni a cui era presente, allievi attesi × durata; previste = tutte tranne le non effettuate.
- `HomeView`: oggi il riquadro mostra in grande le svolte e sotto "su X previste". La sezione "Pagamenti dei corsi attivi" (admin/segreteria, metodo `pagamenti`) ha già "Costo insegnanti previsto" e "Margine previsto".

### Decisioni

- [F-2026-10-24/DASHBOARD.3] Insegnante: in grande le ore previste, sotto "di cui X già svolte".
- [F-2026-10-24/DASHBOARD.3] Admin e segreteria: un riquadro "Ore donate" nella sezione "Pagamenti dei corsi attivi" della dashboard, solo il totale di tutti gli insegnanti senza tariffa (previste in grande, sotto "di cui X già svolte · N insegnanti"), nessun elenco per insegnante.
- [F-2026-10-24/DASHBOARD.3] Se nessun insegnante è senza tariffa il riquadro non compare.
- Scelte tecniche: il totale è la somma del calcolo dell'insegnante (stessi criteri, anche gli insegnanti con forfait); la risposta "in dashboard e in pagamenti dei corsi attivi" è intesa come la sezione "Pagamenti dei corsi attivi" della dashboard.

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | `BilancioService.oreDonateTotali()`: somma di svolte e previste su tutti gli insegnanti senza tariffa, con il loro numero; test | `BilancioService`, `BilancioServiceTest` | `./mvnw test` | [x] |
| 2 | `HomeView`: riquadro dell'insegnante con le previste in grande e "di cui X già svolte"; riquadro del totale in "Pagamenti dei corsi attivi"; test browserless | `HomeView`, `ViewAccessTest` | `./mvnw test` | [x] |
| 3 | `CLAUDE.md` e registro finale | `CLAUDE.md`, questo piano | build e test | [x] |

## Fuori ambito

- Elenco delle ore donate per insegnante ad admin e segreteria.

## Registro

- `./mvnw test`: 267 test, tutti superati. Nuovi o aggiornati: `BilancioServiceTest.oreDonate_…` (totale con un insegnante senza tariffa), `ViewAccessTest.insegnanteSenzaTariffa_vedeLeOreDonate` (previste in grande) e `segreteria_vedeIlTotaleDelleOreDonate`.
- Browser: non verificato (nei dati di sviluppo gli insegnanti hanno tutti la tariffa); coperto dai test browserless.
- Nessuno script SQL. Lasciato aperto: nessuno.
