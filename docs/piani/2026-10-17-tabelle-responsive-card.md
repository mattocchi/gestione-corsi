# Piano: tabelle responsive con card su mobile

Branch: `feature/tabelle-responsive-card` (da `develop`).

## Richiesta

[F-2026-10-17/TABELLE.1–3] Tutte le tabelle responsive: su desktop la Grid con le righe, su mobile delle card con
dentro gli stessi dati della riga; usare la funzionalità nativa di Vaadin se esiste, altrimenti chiedere conferma
prima di fare viste personalizzate; minor complessità possibile.

## Analisi

- Documentazione Vaadin 25.3 (MCP: Grid, Card, Virtual List, Responsive Layouts, `get_new_apis` di
  `vaadin-grid-flow`): la Grid **non** ha una modalità card nativa né una trasformazione automatica righe → card.
  Esistono il componente `Card` (header, titolo, sottotitolo, `headerSuffix`, footer) e la `VirtualList`.
  La guida "Responsive Layouts" consiglia di non ricostruire ciò che i componenti già offrono: non c'è nulla da
  riusare per questo caso, serve un minimo di codice personalizzato (confermato dall'utente).
- Oggi su mobile: `ViewSupport.hideOnMobile` nasconde alcune colonne (17 Grid) e alcune viste hanno una colonna
  riepilogo solo mobile scritta a mano (`ElencoLezioni`, `ElencoPagamenti`, `HomeView`, `PrezziCorso`,
  `RimborsiView`). 21 Grid in tutto, comprese quelle nei dialog.
- Alcune colonne hanno una visibilità dinamica indipendente dal mobile (`ElencoLezioni` corso/insegnante,
  `ElencoPagamenti` genitore, `FigliView` indirizzo): l'helper deve rispettarla.
- Soluzione scelta: una sola classe `views/CardMobile<T>` che aggiunge alla Grid una colonna con `ComponentRenderer`
  che produce una `Card` Vaadin (titolo, badge di stato, campi "Etichetta: valore", azioni nel footer). Il segnale
  `ViewSupport.isMobile()` mostra solo questa colonna su mobile e solo le altre su desktop. Stessa Grid, stessi dati,
  filtri, refresh e test: per ogni vista solo la dichiarazione dei campi della card.

### Decisioni

- [F-2026-10-17/TABELLE.2] Niente card native nella Grid: confermato l'approccio "Grid + colonna card" (helper unico), non la VirtualList.
- [F-2026-10-17/TABELLE.1] Ambito: tutte le Grid, anche quelle nei dialog e nella Home.
- [F-2026-10-17/TABELLE.1] Contenuto card: dato principale come titolo, poi tutti gli altri dati della riga desktop come "Etichetta: valore", badge di stato in evidenza, azioni (icone) in fondo.
- [F-2026-10-17/TABELLE.3] Breakpoint invariato: card sotto 640px (`ViewSupport.MOBILE_BREAKPOINT`), tabella da tablet in su.

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | Helper `CardMobile<T>` + stili; applicato a Gestione Corsi | `views/CardMobile.java`, `styles.css`, `admin/CorsiView.java` | test, browser 390px | [x] |
| 2 | Anagrafiche: Allievi, Genitori, Insegnanti, Utenti, Anni scolastici | `admin/*View.java` | test | [x] |
| 3 | Lezioni e presenze: `ElencoLezioni` (sostituisce il riepilogo mobile), `PresenzeAllievoDialog`, `PartecipantiDialog` | | test | [x] |
| 4 | Pagamenti: `ElencoPagamenti`, `QuotaPagamentiDialog`, `PagamentiAllievoDialog`, `GrigliaInvii` | | test | [ ] |
| 5 | Rimborsi e bilancio: `RimborsiView`, `RimborsiDialog`, `BilancioDialog`, `MieiRimborsiView` | | test | [ ] |
| 6 | Home, allievo e genitore: `HomeView`, `MieiCorsiView`, `FigliView`, `PrezziCorso` | | test | [ ] |
| 7 | Pulizia: rimozione di `hideOnMobile` e stili riepilogo non più usati; CLAUDE.md (layout mobile) | | test | [ ] |
| 8 | Verifica nel browser desktop/390px delle viste principali; registro | | browser | [ ] |

## Fuori ambito

- Tabelle HTML dei documenti PDF/email (non sono Grid).
- Ordinamento su mobile: l'intestazione delle colonne non è visibile in modalità card (come oggi per le colonne riepilogo).

## Registro

