# RICHIESTE DI FUNZIONALITÀ (DAI PROMPT)

Richieste di funzionalità fatte direttamente nei prompt a Claude Code, registrate prima di essere implementate.
Stesso formato di `RICHIESTE.md`: sezioni per data e per ambito, una voce `* [ ]` per richiesta, testo dell'utente riportato fedelmente.
Riferimento di ogni voce: `[F-AAAA-MM-GG/AMBITO.n]` (vedi `CLAUDE.md`, *Feature requests from prompts*).

## FUNZIONALITA del 11-10-2026

### ANNO SCOLASTICO

* [x] rendi la lista di valori di Anno Scolastico amministrabile da Admin _(completata 2026-10-11)_
  _Chiarimento 2026-10-11: nuova vista di menu "Anni scolastici" per ADMIN e SEGRETERIA (eliminazione solo ADMIN e solo se nessun corso usa l'anno); aggiungi e attiva/disattiva (un anno disattivato non si può più scegliere nei corsi ma resta su quelli che lo hanno), niente rinomina; formato AAAA/AAAA con anni consecutivi, unico; elenco popolato all'avvio, se vuoto, con anno precedente, corrente e successivo più gli anni dei corsi esistenti; nel corso l'anno della data di inizio è proposto solo se attivo._

### CORSI

* [x] aggiungi ai corsi la possibilita di inserire oltre al prezzo pieno, attuale, anche un prezzo con pre-iscrizione che in genere è il 15% rispetto al prezzo pieno ma lo assegnamo a mano _(completata 2026-10-11)_
  _Chiarimento 2026-10-11: 15% = sconto (valore tipico 85% del costo totale), indicato solo come aiuto, nessuna proposta automatica; prezzo solo informativo (non cambia la quota dovuta, per applicarlo si usa la quota personalizzata); facoltativo, tra 0 e il costo totale; l'etichetta "Costo Totale" resta; mostrato nel dialog del corso, in "Quota e pagamenti" e a genitori e allievi nelle liste dei corsi, con costo totale e prezzo effettivamente applicato._

### REGISTRO INSEGNANTE

* [x] Per l'insegnante semplifichiamo l'accesso al registro. Se quando si logga ha un solo corso lo portiamo all'elenco delle lezioni direttamente. _(completata 2026-10-11)_
  _Chiarimento 2026-10-11: solo al login (la Home resta raggiungibile dal menu), solo per chi ha unicamente il ruolo INSEGNANTE e un solo corso attivo._
* [x] Se la prima lezione in lista è oggi vale la pena enfatizzare con un badge che è oggi. _(completata 2026-10-11)_
  _Chiarimento 2026-10-11: badge "Oggi" (blu pieno) su tutte le lezioni di oggi, accanto alla data (su mobile nel riepilogo)._
* [x] Fare lo stesso con colori diversi per evidenziare settimana scorsa, se ne ha ancora una da compilare o settimana prossima per quelle programmate settimana prossima. _(completata 2026-10-11)_
  _Chiarimento 2026-10-11: settimane di calendario lun–dom; "Settimana scorsa" arancione solo per le lezioni non completate, "Settimana prossima" grigio; in più "In ritardo" rosso per le non completate più vecchie e "Questa settimana" blu chiaro per gli altri giorni della settimana corrente._

### LEZIONI NON EFFETTUATE

* [x] Dobbiamo dare all'insegnante la possibilità di indicare che una lezione non è stata fatta, perché ad esempio era assente. _(completata 2026-10-11)_
  _Chiarimento 2026-10-11: solo l'insegnante del corso; pulsante "Non effettuata" con motivo facoltativo, ripristinabile; presenze conservate ma escluse (allievi e genitori vedono "Lezione non effettuata"); filtro "Non effettuate" in Le Mie Lezioni; in Home riquadro dedicato, escluse da fatte/da fare; "Genera Lezioni" le conserva._
* [x] E quindi la possibilità di inserire una nuova lezione qualora quella programmata non fosse stata effettuata. In questo modo può inserire delle nuove e gestione le presenze e indicare quelle programmate  che non ha potuto fare _(completata 2026-10-11)_
  _Chiarimento 2026-10-11: "Recupera" sulla lezione non effettuata (collegata, stesso numero) e "Nuova lezione" libera; data obbligatoria, orario proposto dal corso, presenze dei partecipanti attuali; le lezioni aggiunte non completate si possono eliminare; "Genera Lezioni" le conserva._
* [x] Fai in modo che anche admin e segreteria possano usare la funzionalità appena realizzata per gli insegnanti _(completata 2026-10-11)_
  _Chiarimento 2026-10-11: si riferisce a lezioni non effettuate e nuove lezioni (LEZIONI.1–2, che quindi non sono più solo dell'insegnante); nuova vista di menu "Lezioni" per ADMIN e SEGRETERIA con le stesse funzioni di "Le Mie Lezioni" (presenze e Segna Completata compresi) su tutti i corsi attivi, filtro per corso; eliminare le lezioni aggiunte solo admin (e l'insegnante del corso)._

### RIMBORSI

* [x] Dobbiamo mettere a disposizione di segreteria e admin una nuova funzionalità per indicare i pagamenti che vengono effettuati agli insegnanti. Serve quindi un'area in cui indichiamo i pagamenti che facciamo verso gli insegnanti _(completata 2026-10-11)_
  _Chiarimento 2026-10-11: nuova vista di menu "Rimborsi" (admin e segreteria; solo admin elimina); rimborso con data, mese di riferimento, importo (proposto: maturato del mese), metodo Bonifico/Cassa e nota._
* [x] con una vista di un prospetto di quello che è il totale pagato fino a  quel momento per quell'insegnante. Questa sezione si chiama rimborsi perché effettivamente sono dei rimborsi spese che vengono dati mensilmente agli insegnanti collaboratori dell'associazione. _(completata 2026-10-11)_
  _Chiarimento 2026-10-11: una riga per insegnante con rimborsato, maturato e da rimborsare per anno scolastico (filtro, default l'anno corrente); l'insegnante vede i propri rimborsi in sola lettura in "I miei rimborsi"._
* [x] Alla luce di questa nuova informazione, che raccogliamo possiamo creare una visualizzazione di un mini bilancio per l'insegnante di quanto ha percepito e quanto abbiamo incassato dei corsi che gestisce è una proiezione anche non solo sull'effettivo, ma anche su quello che è teorico, ovvero previsto di incassare sulla base dei prezzi assegnati agli allievi e il costo stimato sul totale delle elezioni programmate. _(completata 2026-10-11)_
  _Chiarimento 2026-10-11: "elezioni" = lezioni; bilancio per anno scolastico con effettivo (incassato, rimborsato, saldo), maturato (lezioni completate, da rimborsare) e teorico (quote assegnate, costo di tutte le lezioni programmate escluse le non effettuate, margine), con dettaglio per corso; solo admin e segreteria._
* [x] Ti evidenzio un dettaglio che l'insegnante ha un rimborso con un costo orario, mentre le lezioni sono di 40 minuti o comunque configurabili per ogni corso. Di conseguenza il calcoli vanno fatti correttamente _(completata 2026-10-11)_
  _Chiarimento 2026-10-11: lezioni individuali: ore = allievi iscritti alla lezione (assenti compresi) × durata del corso / 60, × tariffa oraria attuale; le lezioni non effettuate non costano._

### EMAIL

* [x] Per completare alcune funzionalità ora occorre predisporre il programma perché possa inviare delle email. _(completata 2026-10-11)_
  _Chiarimento 2026-10-11: invio reale se è configurato l'SMTP (variabili MAIL_HOST, MAIL_PORT, MAIL_USERNAME, MAIL_PASSWORD, MAIL_FROM), altrimenti simulato e registrato, con qualunque profilo._
* [x] In particolare come segretaria o admin è necessario poter inviare sia conferma di iscrizione ad ogni corso in cui il figlio allievo risulta escritto riportante la conferma di iscrizione con importo da pagare, _(completata 2026-10-11)_
  _Chiarimento 2026-10-11: una conferma per iscrizione (corso), email HTML con PDF allegato: corso, allievo, anno, insegnante, quota, pagato, residuo e IBAN per il bonifico; al genitore, o all'allievo se non c'è un genitore con email; da Gestione Pagamenti, dal dialog Quota e pagamenti e, per tutti gli iscritti di un corso, dal dialog Partecipanti; dati dell'associazione da configurazione._
* [x] che le ricevute via email al genitore, per ogni corso in cui ha fatto dei pagamenti. Il genitore può scaricare via web ma segreteria o admin devono poter inviare via email. _(completata 2026-10-11)_
  _Chiarimento 2026-10-11: email con riepilogo e ricevuta in PDF allegato, solo per i corsi con pagamenti; lo scarico web della ricevuta diventa lo stesso PDF (sostituisce la pagina stampabile)._
* [x] Ogni invio deve essere registrato come data invio e almeno i metadati di invio. _(completata 2026-10-11)_
  _Chiarimento 2026-10-11: data e ora, tipo, corso, allievo, destinatario, oggetto, allegato, chi ha inviato, esito (inviata/simulata/errore); niente corpo; consultabile nel dialog Quota e pagamenti e nella nuova vista "Email inviate"._
* [x] Valuterei di usare freemarker per il template HTML della email mentre JasperReport per la produzione PDF delle ricevute in modo da avere consistenza. _(completata 2026-10-11: FreeMarker + OpenHTMLtoPDF)_
  _Chiarimento 2026-10-11: scelti FreeMarker + OpenHTMLtoPDF 1.1.85 (un solo template HTML per email, allegato e scarico web) al posto di JasperReports._

## FUNZIONALITA del 13-10-2026

### EMAIL

* [x] aggiungi in admin la possibilità di inviare una email di test ad un destinatario inseribile da ui per testare la configurazione in produzione _(completata 2026-10-13)_
  _Chiarimento 2026-10-13: solo ADMIN, pulsante "Email di test" nella vista "Email inviate"; destinatario libero obbligatorio; il dialog mostra la configurazione SMTP (senza password) e avvisa se l'invio è simulato; esito ed errore nel dialog e nel log, nessuna registrazione in "Email inviate"._
* [x] e che permetta di selezionare uno dei template disponibili tra le email esistenti per verificare anche la resa html in client di posta _(completata 2026-10-13)_
  _Chiarimento 2026-10-13: scelta tra "Solo testo", "Conferma di iscrizione" e "Ricevuta"; template compilati con dati fittizi, PDF allegato come negli invii reali, oggetto con prefisso "[TEST]"._

### ALLIEVI

* [x] rendi non obbligatorio lo username, codice fiscale, data di nascia nel momento di inserimento di un nuovo allievo o modifica _(completata 2026-10-13)_
  _Chiarimento 2026-10-13: username vuoto → username generato da nome e cognome, password casuale e accesso disattivato (riattivabile da Gestione Utenti con il nuovo campo "Attivo"); codice fiscale e data di nascita facoltativi anche per il genitore (I miei figli) e nel profilo, il codice fiscale se inserito deve essere valido; badge "Dati mancanti" in Gestione Allievi._
* [x] il dato codice fiscale e data di nascita sono obbligatori solo per stampa di eventuale ricevuta o conferma _(completata 2026-10-13)_
  _Chiarimento 2026-10-13: senza codice fiscale o data di nascita dell'allievo ricevuta e conferma non si generano: scarico e invio email disabilitati con avviso, l'invio multiplo delle conferme salta questi allievi e li conta nel riepilogo._

## FUNZIONALITA del 17-10-2026

### TABELLE RESPONSIVE

* [x] Su un branch a parte prepara una versione con tutte le tabelle in modalità responsive per fare in modo che in modalità desktop ci siano le tabelle con le righe nelle liste, mentre in modalità mobile ci sono delle card con i dati visibili in griglia in desktop riportati dentro _(completata 2026-10-17)_
  _Chiarimento 2026-10-17: tutte le Grid, anche nei dialog e nella Home; branch `feature/tabelle-responsive-card`; card con il dato principale come titolo, badge di stato, tutti gli altri dati come "Etichetta: valore" e le azioni in fondo; card sotto i 640px, tabella da tablet in su._
* [x] La card mi sembra di ricordare che vadim ha già questa funzionalità. Nativa USA la parte nativa se questo non fosse confermabile e devi realizzare viste personalizzate, ti chiedo di chiedermi conferma prima di procedere _(completata 2026-10-17)_
  _Chiarimento 2026-10-17: verificato che Vaadin 25.3 non ha una modalità card nativa per la Grid; confermato l'approccio con un unico helper che aggiunge alla stessa Grid una colonna con una Card Vaadin visibile solo su mobile._
* [x] Vorrei ottenere minore complessità possibile e pure un design che risponde desktop mobile _(completata 2026-10-17)_

### TAILWIND

* [x] Su questo stesso branch ora attiva la modalità sperimentale per supportare taiwind css _(completata 2026-10-17)_
  _Chiarimento 2026-10-17: flag `com.vaadin.experimental.tailwindCss` attivo in tutti i profili sul branch `feature/tabelle-responsive-card`._
* [x] Riscrivi quindi la parte di layout o css custom con taiwind css _(completata 2026-10-17)_
  _Chiarimento 2026-10-17: approccio ibrido (in `styles.css` restano solo le regole sugli interni dei componenti Vaadin, es. `::part` della Grid); combinazioni ripetute come costanti Java in `views/Stili`; colori e tipografia dai token del tema Aura, Tailwind per layout e spaziature; convertiti anche i `getStyle().set`, restano le API di `HorizontalLayout`/`VerticalLayout`._
* [x] Oltre al css custom ora concerti anche il layout fatto con le api Java in taiwind css _(completata 2026-10-17)_
  _Chiarimento 2026-10-17: `VerticalLayout`/`HorizontalLayout` restano ma padding, spacing, allineamenti, wrap, expand e dimensioni passano a classi Tailwind; convertite anche le dimensioni di campi, filtri e viste; restano via API le larghezze delle colonne della Grid e dei Dialog; `FormLayout` invariato._

### FESTIVITA

* [x] Aggiungi un admin una sezione per gestire le festività italiane sia quelle annuali che quelle con date variabili _(completata 2026-10-17)_
  _Chiarimento 2026-10-17: vista "Festività" per admin e segreteria (eliminazione solo admin); tipi: ogni anno (giorno o periodo, anche a cavallo d'anno), date precise (giorno o periodo), Pasqua e Pasquetta calcolate in automatico (niente periodi relativi alla Pasqua); festività nazionali precaricate se la tabella è vuota (4 ottobre compreso), ognuna disattivabile._
* [x] Questo per fare in modo che quando si generano le lezioni vengono escluse, ovviamente saltando, le festività note _(completata 2026-10-17)_
  _Chiarimento 2026-10-17: valgono per tutti i corsi; la generazione salta i giorni festivi mantenendo il numero di lezioni; le lezioni già generate si sistemano con "Genera Lezioni" e nelle Lezioni un badge "Festività" segnala quelle da fare in un giorno festivo; nuova lezione o recupero a mano in un giorno festivo consentiti con avviso._

## FUNZIONALITA del 18-10-2026

### LOGIN

* [x] localizzazione in italiano della login Page _(completata 2026-10-18)_
  _Chiarimento 2026-10-18: titolo, campi, pulsanti ed errori in italiano; "Password dimenticata?" apre la pagina di recupero._
* [x] implementazione della procedura di recupero password che invia un link temporaneo valido per 36ore (parametro configurabile da file di configurazione applicazione) su una pagina che permette all'utente che clicca sul link ricevuto via email di impostare una nuova password. Una volta impostata la nuova password viene visualizzata una pagina di conferma e l'utente può cliccare per andare alla login. _(completata 2026-10-18)_
  _Chiarimento 2026-10-18: richiesta con username o email (un link per ogni account con quell'email); indirizzo del link dal nuovo parametro `app.url` (APP_URL), validità `app.password-reset.validita-ore` (default 36); link monouso, una nuova richiesta annulla i precedenti, token salvato solo come hash; nuova password di almeno 8 caratteri con conferma (regola estesa al profilo); account inesistenti, senza email o disattivati: nessun invio e messaggio sempre uguale; email di avviso dopo il cambio; solo dal login, non registrate in "Email inviate"._

### NOME APPLICAZIONE

* [ ] rinomina in login o altre sezioni il testo "Gestione Corsi Musica" in qualcosa di più efficace per insegnanti, segreteria, genitori e allievi
  _Chiarimento 2026-10-18: nuovo nome "Area Corsi Mosson" in intestazione, accesso e pagine del recupero password; sottotitolo "Lezioni, presenze e pagamenti per insegnanti, segreteria, famiglie e allievi" su accesso e recupero password; titolo della scheda di ogni pagina "Pagina | Area Corsi Mosson"._
