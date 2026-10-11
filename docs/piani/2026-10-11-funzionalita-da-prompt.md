# Piano: richieste di funzionalità dai prompt in docs/FUNZIONALITA.md

## Richiesta

Aggiornare la configurazione di Claude Code perché i prompt che chiedono funzionalità vengano prima segnati nel file `FUNZIONALITA` e poi processati.

## Analisi

- Oggi le richieste arrivano da `docs/RICHIESTE.md`, scritto a mano dall'utente; `CLAUDE.md` (*Requests source*) definisce formato, riferimenti `[AAAA-MM-GG/AMBITO.n]` e la sessione di implementazione in 5 fasi (analisi, domande, registrazione delle risposte, piano ed esecuzione, domande durante l'esecuzione).
- Le richieste fatte direttamente nel prompt non lasciano traccia in un elenco: vanno registrate in un file con lo stesso formato e poi passate allo stesso flusso.
- Il riconoscimento del prompt non può essere fatto da un hook (non sa classificare il testo): l'hook `UserPromptSubmit` aggiunge a ogni prompt un promemoria nel contesto, la regola vera sta in `CLAUDE.md`.
- `.claude/settings.local.json` è ignorato da git (ignore globale): l'hook va in `.claude/settings.json`, condiviso nel repository.

### Decisioni

- File separato `docs/FUNZIONALITA.md`, stesso formato di `RICHIESTE.md`; `RICHIESTE.md` resta per le richieste scritte a mano.
- Dopo la registrazione la voce si processa subito con la sessione di implementazione già definita (domande, piano, un commit per passo).
- Meccanismo: regola in `CLAUDE.md` + hook `UserPromptSubmit` in `.claude/settings.json`.
- (tecnica) Riferimenti con prefisso `F-` (`[F-AAAA-MM-GG/AMBITO.n]`) per non confonderli con quelli di `RICHIESTE.md` della stessa data e ambito.
- (tecnica) Sono "richieste di funzionalità" le nuove funzionalità e le modifiche al comportamento, all'interfaccia o ai dati dell'applicazione; domande, revisioni, correzioni di bug e modifiche a strumenti/configurazione/documentazione seguono il workflow normale senza registrazione.

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | Creare `docs/FUNZIONALITA.md` con intestazione e istruzioni di formato | `docs/FUNZIONALITA.md` | rilettura | [ ] |
| 2 | `CLAUDE.md`: sezione sulle richieste di funzionalità dai prompt (riconoscimento, registrazione testuale, riferimenti `F-`, commit di registrazione, poi sessione di implementazione) | `CLAUDE.md` | rilettura | [ ] |
| 3 | Hook `UserPromptSubmit` con il promemoria | `.claude/settings.json` | JSON valido, comando eseguito a mano | [ ] |

## Fuori ambito

- Nessuna modifica al codice dell'applicazione.
- Le voci già presenti in `RICHIESTE.md` non vengono spostate.
- `.idea/.copilot-plugin` (modifica locale preesistente) non incluso.

## Registro

