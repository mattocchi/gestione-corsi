# Piano: pulsanti tagliati nella colonna "Azioni" delle griglie (desktop)

## Richiesta
Controllo UI di tutte le griglie desktop: in GENITORE alcuni pulsanti non si vedono.

## Analisi
- Controllo con Chrome headless (1440×900) di tutte le griglie di admin, genitore e insegnante, misurando nel DOM i
  pulsanti che escono dalla cella.
- Problema solo nelle righe con il pulsante ricevuta (`MenuPdf.soloIcona`, un `MenuBar`): Gestione Pagamenti,
  Pagamenti del genitore (e allievo adulto), dettaglio corsi in "I Miei Figli".
- Causa: la colonna "Azioni" ha larghezza automatica, calcolata quando il `MenuBar` non ha ancora disegnato il suo
  pulsante (largo 4px): la colonna resta stretta e taglia gli ultimi pulsanti. Con Aura il pulsante del menu ha anche
  la freccia del sottomenu (circa 70px), quindi una larghezza più piccola manda la voce nel pulsante "…".
- Le altre griglie (Corsi, Allievi, Genitori, Insegnanti, Utenti, Lezioni, Rimborsi, Festività, Anni scolastici,
  Email inviate, Riepilogo iscritti, Home, Le Mie Lezioni, I Miei Rimborsi) non hanno pulsanti tagliati.

## Passi
| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | Larghezza fissa del menu ricevuta nelle righe (`flex-none w-[4.5rem]`) | `views/MenuPdf.java` | compile, test, controllo headless delle griglie | [x] |
| 2 | CLAUDE.md e registro | `CLAUDE.md`, piano | — | [x] |

## Fuori ambito
- Controllo su mobile (le card non usano la colonna "Azioni").
- Il badge "Stato" coperto dalla colonna "Azioni" fissa quando la griglia scorre in orizzontale: comportamento normale
  della colonna bloccata a destra.

## Registro
- Passo 1: dopo la correzione nessun pulsante tagliato né voce finita nel menu "…" in Gestione Pagamenti, Pagamenti
  del genitore e dettaglio corsi dei figli; le altre griglie restano a posto (controllo headless 1440×900).
- `./mvnw test`: 232 test, tutti verdi.
- Prima del prossimo build di produzione: cancellare `src/main/bundles/prod.bundle` (nuova classe Tailwind e MenuBar).
