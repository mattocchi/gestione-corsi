# Piano: rimborsi agli insegnanti e mini bilancio

## Richiesta

[F-2026-10-11/RIMBORSI.1] Area per admin e segreteria in cui registrare i pagamenti (rimborsi spese mensili) fatti agli insegnanti.
[F-2026-10-11/RIMBORSI.2] Prospetto del totale rimborsato fino a quel momento per ogni insegnante; la sezione si chiama "Rimborsi".
[F-2026-10-11/RIMBORSI.3] Mini bilancio per insegnante: quanto ha percepito e quanto si è incassato dai suoi corsi, effettivo e teorico (quote assegnate agli allievi, costo stimato sulle lezioni programmate).
[F-2026-10-11/RIMBORSI.4] Calcoli corretti con tariffa oraria dell'insegnante e durata delle lezioni configurabile per corso (es. 40 minuti).

## Analisi

- `Insegnante.tariffaOraria` (€/ora) esiste già; `Corso.durataLezioneMinuti` per corso; ogni partecipante ha il proprio orario di lezione (`Iscrizione.orarioLezione`, nei dati di esempio slot da 40 minuti consecutivi): le lezioni sono individuali.
- Ogni `Lezione` ha una `Presenza` per partecipante (creata alla generazione, all'aggiunta e all'iscrizione); le lezioni non effettuate sono escluse (`Lezione.isNonEffettuata`).
- Incassi degli allievi: `Pagamento` per `Iscrizione`; quota dovuta `Iscrizione.getQuotaDovuta()` (quota personalizzata o costo totale del corso).
- Anni scolastici: `Corso.annoScolastico` ("2026/2027"), `AnnoScolastico.di(data)` settembre–agosto, elenco in `AnnoScolasticoService`.

### Formule

- Ore di una lezione = numero di presenze della lezione (allievi iscritti, assenti compresi) × durata della lezione del corso / 60. Costo = ore × tariffa oraria attuale dell'insegnante. Le lezioni non effettuate costano 0.
- **Maturato**: costo delle lezioni completate. **Costo previsto**: costo di tutte le lezioni del corso non segnate come non effettuate (completate, da fare, aggiunte); per un corso senza lezioni generate si stima numero lezioni × iscritti × durata.
- **Incassato**: somma dei pagamenti degli allievi; **incasso previsto**: somma delle quote dovute.
- **Rimborsato**: somma dei rimborsi con mese di riferimento nell'anno scolastico; **da rimborsare** = maturato − rimborsato.
- Esempio: 7 allievi × 40 min = 4,67 h × 25 €/h = 116,67 € per giorno di lezione.

### Decisioni

- [F-2026-10-11/RIMBORSI.4] Lezioni individuali: tempo = durata × allievi iscritti alla lezione (assenti compresi); le non effettuate non contano.
- [F-2026-10-11/RIMBORSI.4] Si usa sempre la tariffa oraria attuale; senza tariffa il costo è 0 e il bilancio lo segnala.
- [F-2026-10-11/RIMBORSI.1] Rimborso: data, mese di riferimento, importo, metodo (Bonifico/Cassa), nota. Nel dialog l'importo proposto è il maturato del mese (lezioni completate nel mese meno i rimborsi già registrati per quel mese), modificabile.
- [F-2026-10-11/RIMBORSI.1][F-2026-10-11/RIMBORSI.2] Nuova vista di menu "Rimborsi" (ADMIN, SEGRETERIA): una riga per insegnante con tariffa, rimborsato, maturato, da rimborsare; azioni "Rimborsi" (dialog con elenco, aggiungi, modifica) e "Bilancio". La segreteria inserisce e modifica, solo l'admin elimina.
- [F-2026-10-11/RIMBORSI.2][F-2026-10-11/RIMBORSI.3] Periodo: anno scolastico scelto con un filtro (default quello corrente); corsi di quell'anno e rimborsi con mese di riferimento da settembre ad agosto.
- [F-2026-10-11/RIMBORSI.3] Bilancio: effettivo (incassato, rimborsato, saldo), maturato (costo lezioni completate, da rimborsare), teorico (incasso previsto, costo previsto, margine previsto), con dettaglio per corso.
- [F-2026-10-11/RIMBORSI.2] L'insegnante vede i propri rimborsi in sola lettura (nuova voce "I miei rimborsi", con totale e filtro anno); il bilancio resta interno.

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | Entità `Rimborso` (insegnante, data, mese di riferimento, importo, metodo, nota), repository, `RimborsoService` (elenco per insegnante e anno, salva con validazione, elimina); test | `entity/Rimborso.java`, `repository/RimborsoRepository.java`, `service/RimborsoService.java`, test | `./mvnw test` | [x] |
| 2 | Calcoli: costo delle lezioni (presenze × durata × tariffa), maturato del mese, prospetto per anno, bilancio per insegnante e anno; test con i dati di esempio | `service/BilancioService.java`, repository, test | `./mvnw test` | [x] |
| 3 | Vista "Rimborsi": filtro anno, griglia insegnanti con totali, dialog rimborsi (aggiungi/modifica con importo proposto, elimina solo admin); test | `views/admin/RimborsiView.java`, `views/admin/RimborsiDialog.java`, test, `docs/FUNZIONALITA.md` | `./mvnw test` | [ ] |
| 4 | Dialog "Bilancio": effettivo, maturato, teorico e dettaglio per corso (mobile 390px); test | `views/admin/BilancioDialog.java`, test, `docs/FUNZIONALITA.md` | `./mvnw test` | [ ] |
| 5 | Vista "I miei rimborsi" per l'insegnante (sola lettura, totale, filtro anno); test | `views/insegnante/MieiRimborsiView.java`, test, `docs/FUNZIONALITA.md` | `./mvnw test` | [ ] |
| 6 | `CLAUDE.md` e registro del piano | `CLAUDE.md`, piano | rilettura | [ ] |

## Fuori ambito

- Storico delle tariffe orarie; ricevute dei rimborsi; esportazioni.
- `.idea/.copilot-plugin` (modifica locale preesistente) non incluso.

## Registro
