# Ricevuta senza insegnanti

## Richiesta

"nella ricevuta di pagamento rimuovi l'informazione \"Insegnanti\"" — [F-2026-10-27/RICEVUTA.1]

## Analisi

- La ricevuta in PDF (`templates/documenti/ricevuta.ftlh`) usa la macro `datiIscrizione` di `comune.ftlh` con
  `insegnanti=true` (predefinito): riga "Insegnante"/"Incaricato". La conferma la usa già con `insegnanti=false`.
- L'email della ricevuta (`templates/email/ricevuta.ftlh`) non nomina gli insegnanti.
- Tolto l'unico uso, il parametro `insegnanti` della macro non serve più.

### Decisioni

- [F-2026-10-27/RICEVUTA.1] Nessun documento dell'iscrizione (conferma e ricevuta) nomina gli insegnanti: si toglie
  la riga e il parametro dalla macro. Il riepilogo della famiglia (insegnanti sotto il nome del corso) non cambia.

## Passi

| Passo | Cosa | File | Verifica | Stato | Richiesta |
|---|---|---|---|---|---|
| 1 | Tolta la riga degli insegnanti da `datiIscrizione`; test della ricevuta; CLAUDE.md | templates, `DocumentiServiceTest`, CLAUDE.md | `./mvnw test` | [ ] | F-2026-10-27/RICEVUTA.1 |

## Fuori ambito

- Riepilogo della famiglia e lettera di incarico.

## Registro

