# Quota di ogni iscritto nella pagina del corso

## Richiesta

[F-2026-10-31/PAGINE.3] Nella scheda Iscritti della pagina del corso indicare per ogni allievo la quota personalizzata
con l'eventuale nota, o che prende il prezzo di base.

## Analisi

- `Iscrizione.getQuotaCorso()` (personalizzata o `Corso.costoTotale`), `getQuotaDovuta()` (più le opzioni attive),
  `getNotaQuota()`; oggi la scheda Iscritti mostra solo il badge di stato del pagamento.
- Le famiglie vedono già un badge sconto/maggiorazione rispetto al listino (`PrezziCorso`): stesso stile.

### Decisioni

- [F-2026-10-31/PAGINE.3] Colonna "Quota" nella griglia (e campo nella card su smartphone): in grande la quota dovuta,
  opzioni comprese; sotto "Prezzo di listino" o "Personalizzata · <nota>" con badge "-X €"/"+X €" rispetto al listino;
  con opzioni attive anche "Corso X € + opzioni Y €".
- Modifica invariata: pulsante "Quota e pagamenti"; la colonna si aggiorna alla chiusura del dialog.

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | Colonna "Quota" nella scheda Iscritti | `PartecipantiCorso`, test | `./mvnw test` | [ ] |
| 2 | Guida, CLAUDE.md e Registro | `guida/staff.md`, `CLAUDE.md`, questo file | test della Guida | [ ] |

## Fuori ambito

- Modifica della quota direttamente nella cella.

## Registro
