# Piano: genitore creato al volo e conferma di iscrizione (rate, regole, una pagina)

## Richiesta

- [F-2026-10-24/ALLIEVI.1] Nel dialog dell'allievo (admin e segreteria) creare al volo il genitore con nome, cognome, email e cellulare.
- [F-2026-10-24/CONFERMA.1] Nella conferma di iscrizione: pagamento in unica soluzione oppure in due rate entro il 30 novembre e il 31 gennaio.
- [F-2026-10-24/CONFERMA.2] Rendere più gradevoli le frasi di "Lezioni, assenze e recuperi", con le stesse disposizioni.
- [F-2026-10-24/CONFERMA.3] Conferma in PDF su una sola pagina A4.

## Analisi

- `AllieviView`: campo "Genitore" (`ComboBox<Genitore>`, facoltativo) nella sezione "Utente e genitore". Modello per la creazione al volo: `NuovoAllievoRapidoDialog` + `AllievoService.creaRapido` / `omonimi` (valori personalizzati del ComboBox, utente generato disattivato via `salva`).
- `GenitoreService.salva(genitore, user, password)` crea utente e genitore; `UtenteFields`: il cellulare è `User.telefono`.
- Conferma: `templates/documenti/conferma.ftlh` (+ `comune.ftlh`, `importi.ftlh`, `regole-corso.ftlh`) e `templates/email/conferma.ftlh`; regole in un'unica macro per PDF ed email. Coordinate del bonifico solo se c'è l'IBAN.
- PDF di oggi (dati di sviluppo, senza IBAN né sconto): una pagina piena per circa tre quarti; con sconto, IBAN e il nuovo testo sulle rate non ci starebbe. Margini 18×16 mm, corpo 10,5 pt, sezioni Allievo, Genitore e Corso una sotto l'altra, "Quota dovuta" sia in Corso sia in Importo.

### Decisioni

- [F-2026-10-24/ALLIEVI.1] Nel campo "Genitore" si scrive un nome non presente e con Invio si apre "Nuovo genitore" (nome e cognome dal testo, email e cellulare facoltativi); anche un pulsante "+" accanto al campo. Obbligatori solo nome e cognome; avviso sugli omonimi (si può scegliere quello esistente); utente generato e disattivato, l'accesso si dà poi in Gestione Utenti.
- [F-2026-10-24/CONFERMA.1] Testo: "Il pagamento può avvenire in un'unica soluzione entro il 30 novembre AAAA oppure in due rate: la prima entro il 30 novembre AAAA e la seconda entro il 31 gennaio AAAA+1", anni dall'anno scolastico del corso; nel PDF e nell'email, prima delle coordinate del bonifico; senza importi delle rate.
- [F-2026-10-24/CONFERMA.2] Tono colloquiale con titoli "Se l'insegnante non può fare lezione", "Se l'allievo non può venire", "Se decidete di ritirarvi" (testo approvato), nel PDF e nell'email.
- [F-2026-10-24/CONFERMA.3] Impaginazione compatta: margini e spazi ridotti, Allievo e Genitore affiancati, tolta "Quota dovuta" dalla tabella Corso (resta in Importo), nessun'altra informazione tolta, testo ≥ 9,5 pt.
- Scelte tecniche: il genitore creato al volo è salvato subito (come l'allievo rapido nei partecipanti) e scelto nel campo; se poi si annulla il dialog dell'allievo il genitore resta. Corso senza anno scolastico: le date senza anno. Il test verifica che la conferma con sconto, genitore, IBAN e rate sia di una pagina.

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | `GenitoreService.creaRapido` e `omonimi`; test | `GenitoreService`, test | `./mvnw test` | [ ] |
| 2 | `NuovoGenitoreRapidoDialog`; in `AllieviView` valori personalizzati del campo "Genitore" e pulsante "+"; test browserless | `NuovoGenitoreRapidoDialog`, `AllieviView`, `ViewAccessTest` | `./mvnw test` | [ ] |
| 3 | Modalità di pagamento (unica soluzione o due rate) nella conferma PDF ed email; test | `DocumentiService`, template, `DocumentiServiceTest` | `./mvnw test` | [ ] |
| 4 | Regole "Lezioni, assenze e recuperi" riscritte; test | `regole-corso.ftlh`, `DocumentiServiceTest` | `./mvnw test` | [ ] |
| 5 | Conferma PDF compatta su una pagina A4; test sul numero di pagine | `comune.ftlh`, `conferma.ftlh`, `DocumentiServiceTest` | `./mvnw test`, PDF guardato | [ ] |
| 6 | `CLAUDE.md` e registro finale | `CLAUDE.md`, questo piano | build e test | [ ] |

## Fuori ambito

- Rate registrate come scadenze nei pagamenti (solo testo informativo nella conferma).
- Ricevuta e riepilogo: impaginazione invariata, salvo gli stili comuni.

## Registro

