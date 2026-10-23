# Piano: quota applicata esplicita nella conferma, nella ricevuta e nel portale

## Richiesta

Rendere esplicito, nella conferma di iscrizione (email e PDF) e nel portale, che il prezzo predefinito del corso non è quello applicato: l'importo vero è la quota dovuta, così che lo sconto sia evidente e non ci siano dubbi sull'importo ([F-2026-10-23/QUOTA.1]).

## Analisi

- `DocumentiService` passa ai template `corso.quota` ("450,00 € (quota piena 600,00 €, fratelli)", nella sezione Corso dei PDF, macro `datiIscrizione`) e `totali` (quota dovuta, pagato, residuo). La conferma mostra "Quota del corso" = quota dovuta; la ricevuta solo "Totale pagato" e "Residuo da pagare".
- Portale: `views/PrezziCorso` (I Miei Corsi, I Miei Figli) con le colonne "Costo totale", "Pre-iscrizione", "Applicato".
- Quota personalizzata: `Iscrizione.quotaPersonalizzata` (null = `Corso.costoTotale`) con il motivo in `notaQuota`.
- La sezione si chiama "QUOTA APPLICATA": chiave `QUOTA`.

### Decisioni

- [F-2026-10-23/QUOTA.1] Conferma e ricevuta (email e PDF), con uno sconto: "Prezzo di listino del corso" barrato, "Sconto applicato (motivo)" con l'importo in negativo, **QUOTA DOVUTA** in evidenza, "Già pagato", **DA PAGARE**, e la frase "L'importo dovuto è la quota indicata sopra, già scontata: il prezzo di listino è solo un riferimento."
- [F-2026-10-23/QUOTA.1] Senza sconto: solo "Quota dovuta", "Già pagato", "Da pagare".
- [F-2026-10-23/QUOTA.1] Portale: colonne "Prezzo di listino", "Pre-iscrizione", "Quota dovuta" (in grassetto) con badge "Sconto −X €" quando c'è uno sconto; lo stesso nelle card mobile.
- Tecnica: motivo dello sconto = `notaQuota`; se manca e la quota è il prezzo di pre-iscrizione, "pre-iscrizione"; altrimenti solo "Sconto applicato". Una quota personalizzata più alta del listino si mostra come "Maggiorazione applicata". Nella sezione Corso dei PDF resta solo "Quota dovuta". Il blocco degli importi è una macro unica (`templates/importi.ftlh`) per email e PDF.

## Passi

| Passo | Cosa | Richiesta | File | Verifica | Stato |
|---|---|---|---|---|---|
| 1 | Modello `importi` in `DocumentiService`; macro `importi.ftlh` in conferma e ricevuta (email e PDF); test | — | `service/DocumentiService`, `templates/…`, `DocumentiServiceTest` | `./mvnw test` | [x] |
| 2 | Portale: colonne rinominate, quota dovuta in grassetto e badge dello sconto; test | [F-2026-10-23/QUOTA.1] | `views/PrezziCorso`, `ViewAccessTest` | `./mvnw test` | [x] |
| 3 | CLAUDE.md e registro | — | `CLAUDE.md`, questo piano | — | [ ] |

## Fuori ambito

- Riepilogo famiglia (PDF): resta con la quota dovuta per corso.
- Grassetto nei PDF: il generatore usa il font di base senza la variante grassetta (titoli e totali non sono mai stati in grassetto); per evidenziare la quota dovuta si usano righe più grandi su fondo grigio. Per avere il grassetto bisognerebbe incorporare un font (es. con licenza libera) nei PDF.

## Registro
