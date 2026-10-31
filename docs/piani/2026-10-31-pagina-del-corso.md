# Pagina del corso al posto dei dialog

## Richiesta

[F-2026-10-31/PAGINE.1] In Corsi la modifica del corso e la gestione degli iscritti diventano una pagina dedicata invece
di un popup. [F-2026-10-31/PAGINE.2] Dalla pagina degli iscritti il pulsante "Invia conferme di iscrizione a tutti", come
in Documenti.

## Analisi

- Oggi `CorsiView.openDialog` costruisce il form del corso (binder, campi, `OpzioniCorsoFields`) in un `Dialog`, usato da
  Modifica, Nuovo Corso e Duplica; `PartecipantiDialog` è un `Dialog` con aggiunta rapida, gruppi, griglia dei
  partecipanti (orario, gruppo, opzioni, pagamento) e "Quota e pagamenti".
- L'invio a tutti è `InvioEmailDialog.inviaConferme(corso, dopo)` (elenco di chi riceve/chi è saltato, anteprima,
  invio); `EmailService.inviaConferme` rifiuta i corsi non pubblicati.
- Route con parametri di Vaadin (`corsi/:corso/:scheda?`, `BeforeEnterObserver`, `RouteParameters`); modifiche non
  salvate con `BeforeLeaveObserver` (`postpone()` + conferma). Il menu ha `setMatchNested(true)`: "Corsi" resta
  evidenziato anche in `corsi/5`.
- Molti test browserless aprono i due dialog da Corsi: vanno adattati alla pagina.

### Decisioni

- [F-2026-10-31/PAGINE.1] Una sola pagina del corso (`corsi/<id>`, `corsi/<id>/iscritti`, `corsi/nuovo`) con le schede
  "Dati del corso" e "Iscritti"; in Corsi Modifica apre la prima, Partecipanti la seconda.
- Dopo "Salva" si resta sulla pagina (notifica); un corso nuovo, salvato, prende il suo indirizzo e la scheda Iscritti si
  attiva. "Torna ai corsi" riporta all'elenco.
- Uscendo con modifiche non salvate (altra scheda, menu, Torna ai corsi) si chiede conferma.
- Anche "Nuovo Corso" e "Duplica" usano la pagina; il dialog del corso sparisce. Per un corso nuovo la scheda Iscritti
  è disattivata finché non è salvato.
- In testa alla pagina: "Aggiorna lezioni", "Duplica" e (solo admin) "Elimina" con conferma, poi torna a Corsi.
- [F-2026-10-31/PAGINE.2] Nella scheda Iscritti "Invia conferme di iscrizione a tutti" apre lo stesso dialog di
  Documenti (che lo mantiene); per un corso non pubblicato o senza iscritti il pulsante è disattivato con il motivo nel
  tooltip.
- Scelte tecniche: `admin/CorsoView` (pagina), `admin/DatiCorsoForm` (form estratto da `CorsiView`),
  `admin/PartecipantiCorso` (contenuto di `PartecipantiDialog`, che viene eliminato); si costruisce solo la scheda
  aperta.

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | Pagina del corso: form dei dati estratto, schede, Iscritti come componente, modifiche non salvate, azioni in testa; Corsi naviga alla pagina | `CorsoView`, `DatiCorsoForm`, `PartecipantiCorso`, `CorsiView`, `OpzioniCorsoFields`, test | `./mvnw test` | [ ] |
| 2 | Invio delle conferme a tutti dalla scheda Iscritti | `PartecipantiCorso`, test | `./mvnw test` | [ ] |
| 3 | Guida e CLAUDE.md | `guida/staff.md`, `CLAUDE.md` | test della Guida | [ ] |
| 4 | Registro | questo file | — | [ ] |

## Fuori ambito

- Gli altri dialog di modifica (Allievi, Genitori, Insegnanti, Utenti…) restano popup.
- "Quota e pagamenti" di un iscritto resta un dialog sopra la pagina.

## Registro
