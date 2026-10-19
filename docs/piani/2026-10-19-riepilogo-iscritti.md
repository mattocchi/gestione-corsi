# Piano: riepilogo degli iscritti corso per corso

## Richiesta

Per la segreteria, una vista chiara e semplice di tutti gli iscritti corso per corso, con importo da pagare e importo pagato fino a oggi; possibilmente una griglia con un livello di raggruppamento ([F-2026-10-19/RIEPILOGO.1], [F-2026-10-19/RIEPILOGO.2]).

## Analisi

- La `Grid` di Vaadin 25.3 non raggruppa le righe (solo le colonne, con `HeaderRow.join`); il raggruppamento su un livello si ottiene con `TreeGrid` (docs "Tree Grid", `TreeData`/`TreeDataProvider`): righe dei corsi espandibili con gli iscritti come figli.
- I dati esistono già: `PagamentoService.righePagamenti()` (una `RigaPagamento` per iscrizione, con quota dovuta e pagato a oggi, anche corsi non attivi). Servono anche i corsi senza iscritti.
- `Gestione Pagamenti` (`ElencoPagamenti`) resta l'elenco piatto con le azioni; la nuova vista è in sola lettura.
- Mobile: `CardMobile` funziona su qualsiasi `Grid` (anche `TreeGrid`, che la estende); serve solo distinguere la card del corso da quella dell'iscritto (classi per riga) e un pulsante per aprire/chiudere il corso.

### Decisioni

- [F-2026-10-19/RIEPILOGO.1] Nuova voce di menu "Riepilogo Iscritti" per admin e segreteria, in sola lettura, accanto a Gestione Pagamenti (che resta invariata).
- [F-2026-10-19/RIEPILOGO.2] Sì: `TreeGrid` con un livello. Riga del corso con n. iscritti e somme di da pagare, pagato e residuo; sotto, gli iscritti (allievo, genitore, da pagare, pagato a oggi, residuo, stato). Tutti i corsi aperti all'avvio, pulsanti "Espandi tutti / Comprimi tutti", riga di totale generale in fondo.
- [F-2026-10-19/RIEPILOGO.1] Filtri come nelle altre viste: stato del corso Attivi/Non attivi/Tutti (default Attivi) e anno scolastico (default quello corrente, svuotabile = tutti gli anni); i totali seguono i filtri. Compaiono anche i corsi senza iscritti.
- [F-2026-10-19/RIEPILOGO.1] Mobile: card del corso con i totali e pulsante per mostrare/nascondere gli iscritti, che appaiono come card rientrate sotto il corso.
- Decisione tecnica: "da pagare" = quota dovuta (`Iscrizione.getQuotaDovuta()`, personalizzata o costo del corso), "pagato" = somma delle rate registrate al momento dell'apertura della vista.

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | Dati: iscritti raggruppati per corso (anche corsi senza iscritti) con totali | `PagamentoService`, `PagamentoServiceTest` | `./mvnw test` | [x] |
| 2 | Vista "Riepilogo Iscritti": TreeGrid corsi → iscritti, filtri, totale generale, espandi/comprimi, card su mobile | `views/admin/RiepilogoIscrittiView`, `CardMobile`, `ViewAccessTest` | `./mvnw test`, browser desktop e 390px | [ ] |
| 3 | CLAUDE.md e registro | `CLAUDE.md`, questo piano | — | [ ] |

## Fuori ambito

- Azioni sui pagamenti dalla nuova vista (restano in Gestione Pagamenti).
- Stampa o esportazione del riepilogo.

## Registro

