# Pubblicazione dei documenti per le famiglie

## Richiesta

[F-2026-10-31/PUBBLICAZIONE.1] Per ogni tipo di stampa decidere se è pubblica: admin e segreteria vedono sempre
tutto, genitori e allievi adulti solo i documenti pubblicati. L'attestazione per la detrazione è disponibile l'anno
dopo per i pagamenti dell'anno precedente; la conferma di iscrizione già nell'anno in corso, quando il corso è
pubblicato.

## Analisi

- Oggi l'unica regola per le famiglie è il corso pubblicato (`ElencoDocumentiService.documentiDi`, `pubblicate(...)`;
  `DocumentiService.documentoPer`). Le attestazioni compaiono per ogni anno solare con pagamenti, anche quello in corso
  (`documentiDi` da `PagamentoRepository.totaliPerAllievoEAnno`; `DocumentiService.attestazionePer` controlla solo
  che l'allievo sia della famiglia).
- I PDF e gli invii alla propria email delle famiglie passano tutti da `DocumentiService.documentoPer`,
  `riepilogoFamigliaPer`, `riepilogoAllievoPer`, `attestazionePer` (`EmailService.inviaAMe`, `inviaRiepilogoAMe`,
  `inviaAttestazioneAMe` li richiamano): il controllo va lì e in `documentiDi`.
- La pagina Impostazioni a schede (`admin/ImpostazioniView`) esiste su questo branch.

### Decisioni

- [F-2026-10-31/PUBBLICAZIONE.1] Branch `riorganizzazione-per-ruolo`.
- Nuova scheda "Documenti" in Impostazioni (admin e segreteria): per conferme, ricevute, riepiloghi annuali e
  attestazioni l'interruttore "Visibile alle famiglie" con accanto la regola automatica. Un documento è disponibile
  alle famiglie solo con l'interruttore acceso **e** la regola rispettata:
  - conferma e riepilogo annuale: corso pubblicato (come oggi);
  - ricevuta: corso pubblicato e almeno un pagamento (come oggi);
  - attestazione: anno pubblicato a mano dallo staff nella stessa scheda (elenco degli anni con pagamenti), mai prima
    del 1° gennaio dell'anno successivo.
- Primo avvio: interruttori accesi, attestazioni pubblicate per gli anni già finiti, non per l'anno in corso.
- Per le famiglie un documento non pubblico non compare; anteprima e invio alla propria email sono bloccati anche
  dall'indirizzo diretto.
- Admin e segreteria vedono e inviano sempre; nel dialog di invio un avviso "Non ancora pubblico per le famiglie".
- Le lettere di incarico (insegnanti) e il riepilogo dei corsi (solo staff) non cambiano.

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | Dati e servizio: `PubblicazioneDocumento` (tipo → visibile), `AttestazioniPubblicate` (anni), `PubblicazioneDocumentiService`, valori iniziali all'avvio | entity, repository, service, test | `./mvnw test` | [x] |
| 2 | Regole applicate alle famiglie: elenco di Documenti, PDF e invio alla propria email | `ElencoDocumentiService`, `DocumentiService`, test | `./mvnw test` | [ ] |
| 3 | Impostazioni › Documenti: interruttori, regole e anni delle attestazioni | `admin/DocumentiScheda`, `ImpostazioniView`, test | `./mvnw test` | [ ] |
| 4 | Avviso nel dialog di invio dello staff per i documenti non pubblici | `InvioEmailDialog`, test | `./mvnw test` | [ ] |
| 5 | Guida dello staff e CLAUDE.md | `guida/staff.md`, `CLAUDE.md` | `./mvnw test` | [ ] |
| 6 | Registro | questo piano | — | [ ] |

## Fuori ambito

- Pubblicazione per singolo corso o singolo allievo (resta "corso pubblicato").
- Migrazioni SQL: le due tabelle nuove le crea `ddl-auto=update`, i valori iniziali li scrive l'app all'avvio.

## Registro
