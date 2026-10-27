# Piano: nell'email di invito e reimpostazione, accesso con username o email

## Richiesta

[F-2026-10-27/ACCESSO.2] Nell'email di invito e di reimpostazione dire subito che si può entrare indistintamente con lo username o con l'email.

## Analisi

- `templates/email/invito.ftlh` (invito e reimpostazione) dice solo "Il tuo nome utente è …".
- Il login accetta lo username o l'email (`UserDetailsServiceImpl`), ma l'email vale solo se è di un unico account (attivo o sospeso): con un'email condivisa da più account serve lo username.

### Decisioni

- [F-2026-10-27/ACCESSO.2] All'inizio dell'email: "Per entrare puoi usare indistintamente il nome utente X oppure la tua email Y".
- Scelta tecnica: se la stessa email è di più account, l'email dice di usare il nome utente (l'email da sola non basterebbe).

## Passi

| Passo | Cosa | File | Richiesta | Verifica | Stato |
|---|---|---|---|---|---|
| 1 | Template e modello dell'email con username o email (email condivisa → solo username); test; registro | `invito.ftlh`, `DocumentiService`, `RecuperoPasswordService`, test, questo piano | ACCESSO.2 | `./mvnw test` | [ ] |

## Fuori ambito

- Le altre email (recupero password, documenti).

## Registro

