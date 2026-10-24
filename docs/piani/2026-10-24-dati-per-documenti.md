# Piano: dati dell'allievo obbligatori per tipo di documento

## Richiesta

[F-2026-10-24/CONFERMA.7] Per le conferme di iscrizione rendere obbligatoria solo la data di nascita dell'allievo; per le ricevute di pagamento serve anche il codice fiscale.

## Analisi

- Oggi `Allievo.getDatiMancantiPerDocumenti()` (codice fiscale e data di nascita) e `DocumentiService.avvisoDatiMancanti(allievo)` valgono per ogni documento: rifiuto del PDF (`DocumentiService.conDati`), dell'email (`EmailService.verificaInviabile`), invio a tutti e anteprima (`inviaConferme`, `destinatariConferme`), `InvioEmailDialog`, badge in Documenti (`ElencoDocumentiService`) e in Gestione Allievi.
- Il riquadro Allievo (`documenti/comune.ftlh`, `datiIscrizione`) stampa sempre il codice fiscale e non stampa la data di nascita; le email non mostrano questi dati.
- Il riepilogo annuale non controlla questi dati e non cambia.

### Decisioni

- [F-2026-10-24/CONFERMA.7] Conferma: obbligatoria solo la data di nascita; ricevuta: data di nascita e codice fiscale.
- [F-2026-10-24/CONFERMA.7] Conferma senza codice fiscale: la riga "Codice fiscale" non compare.
- [F-2026-10-24/CONFERMA.7] "Data di nascita" stampata nel riquadro Allievo di conferma e ricevuta (formato 12/03/2015).
- [F-2026-10-24/CONFERMA.7] Gestione Allievi: badge "Dati mancanti" se manca uno dei due, tooltip con i documenti per cui servono; in Documenti l'avviso è quello del tipo della riga.

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | Dati mancanti per tipo (`Allievo.getDatiMancantiPer(tipo)`, `DocumentiService.avvisoDatiMancanti(allievo, tipo)`) usati da PDF, email, invio a tutti, dialog e Documenti; test | `Allievo`, `DocumentiService`, `EmailService`, `ElencoDocumentiService`, `InvioEmailDialog`, test | `./mvnw test` | [x] |
| 2 | Riquadro Allievo: data di nascita, codice fiscale solo se presente; test sul PDF | `comune.ftlh`, `DocumentiService`, `Formati`, `DocumentiServiceTest` | `./mvnw test` | [x] Anche nel PDF di prova (data di nascita fittizia) |
| 3 | Gestione Allievi: tooltip del badge con i documenti bloccati; test | `AllieviView`, `DocumentiService`, `ViewAccessTest` | `./mvnw test` | [x] Anche i testi di aiuto dei campi in `AnagraficaFields` |
| 4 | `CLAUDE.md` e registro finale | `CLAUDE.md`, questo piano | build e test | [x] |

## Fuori ambito

- Riepilogo annuale (invariato); nessuna modifica allo schema.

## Registro

- `./mvnw test`: 281 test superati. Nuovi: `EmailServiceTest.senzaCodiceFiscale_confermaSiRicevutaNo` e `senzaDataDiNascita_niente_documentiNeEmail`, `DocumentiServiceTest.conferma_senzaCodiceFiscaleDellAllievo_senzaLaRiga` e `avvisoDatiMancanti_diceACosaServono`, `FormatiTest.dataNumerica_inCifre`; la conferma più lunga resta su una pagina con la riga "Data di nascita".
- Browser: non verificato (modifiche a servizi e template, coperte dai test sul PDF).
- Lasciato aperto: nessuno.
