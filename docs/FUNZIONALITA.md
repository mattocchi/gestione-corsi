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

* [x] rinomina in login o altre sezioni il testo "Gestione Corsi Musica" in qualcosa di più efficace per insegnanti, segreteria, genitori e allievi _(completata 2026-10-18)_
  _Chiarimento 2026-10-18: nuovo nome "Area Corsi Mosson" in intestazione, accesso e pagine del recupero password; sottotitolo "Lezioni, presenze e pagamenti per insegnanti, segreteria, famiglie e allievi" su accesso e recupero password; titolo della scheda di ogni pagina "Pagina | Area Corsi Mosson"._

### INSEGNANTI DEL CORSO

* [x] aggiunti funzionalità per assegnare fino a due insegnanti ad un corso _(completata 2026-10-18)_
  _Chiarimento 2026-10-18: titolare obbligatorio + secondo facoltativo con gli stessi diritti (Le Mie Lezioni, lezioni, presenze); allievi, genitori e conferma PDF vedono entrambi i nomi._
* [x] e la possibilità di indicare nella lezione se sono presenti entrambi o uno dei due _(completata 2026-10-18)_
  _Chiarimento 2026-10-18: di default entrambi presenti; nel dialog "Gestisci Presenze" (solo corsi con due insegnanti) "Entrambi / Solo X / Solo Y"; rimborsi e bilancio: ogni insegnante matura le lezioni a cui era presente; le lezioni svolte restano a chi c'era se cambiano gli insegnanti del corso; nessun badge nell'elenco lezioni._
* [x] chi può indicare se erano presenti gli insegnanti è il ruolo insegnante, segreteria, o admin (non allievi o genitori). _(completata 2026-10-18)_
  _Chiarimento 2026-10-18: un insegnante del corso, segreteria e admin (stessi controlli del completamento della lezione)._

### RIEPILOGO FAMIGLIA

* [x] Puoi preparare una stampa di riepilogo per famiglia, quindi per genitore in cui riepilogo tutti i corsi a cui i figli sono iscritti e il totale della spesa. _(completata 2026-10-18)_
  _Chiarimento 2026-10-18: PDF per anno scolastico (scelto tra quelli dei corsi dei figli, attivi o no) con figlio, corso, quota, pagato e residuo, subtotale per figlio e totali della famiglia; lo scaricano admin e segreteria da "Genitori" e il genitore il proprio da "Pagamenti"; niente invio email._

## FUNZIONALITA del 19-10-2026

### RIEPILOGO ISCRITTI

* [x] Per la segreteria prepara una visualizzazione chiara e semplice di tutti gli iscritti corso per corso, con importo assegnato (da pagare) e importo pagato fino al momento in cui si visualizza la schermata. Una specie di riepilogo. _(completata 2026-10-19)_
  _Chiarimento 2026-10-19: nuova voce di menu "Riepilogo Iscritti" per admin e segreteria, in sola lettura (Gestione Pagamenti resta com'è); filtri stato del corso (default Attivi) e anno scolastico (default corrente); compaiono anche i corsi senza iscritti; su mobile card del corso con i totali e pulsante per mostrare gli iscritti come card._
* [x] Potrebbe essere utile utilizzare sempre una griglia con un livello di raggruppamento? _(completata 2026-10-19)_
  _Chiarimento 2026-10-19: sì, griglia ad albero (TreeGrid) con un livello: riga del corso con n. iscritti e totali di da pagare, pagato e residuo, sotto gli iscritti con allievo, genitore, da pagare, pagato, residuo e stato; corsi aperti all'avvio con "Espandi tutti / Comprimi tutti" e totale generale in fondo._
* [x] Nella visualizzazione di riepilogo dei corsi vorrei vedere anche il costo stimato totale per l'insegnante in modo da fare in mini bilancio preventivo tra quanto dobbiamo incassare e quanto dovremmo spendere. Per quelli in cui ho assegnato un pezzo orario all'insegnante _(completata 2026-10-19)_
  _Chiarimento 2026-10-19: nella riga del corso le colonne "Costo insegnante" (calcolato come il costo previsto del Bilancio dei Rimborsi, sommando entrambi gli insegnanti) e "Margine previsto" (Da pagare − costo), con totali in fondo e su mobile; senza tariffa compare "Senza tariffa" e il corso è escluso dai totali di costo e margine; visibile ad admin e segreteria._

### ALLIEVI

* [x] GENITORE NON OBBLIGAORIO, alcuni allievi sono adulti. _(completata 2026-10-19)_
  _Chiarimento 2026-10-19: il genitore diventa solo facoltativo (nessun flag "adulto"); senza genitore ricevute, conferme ed email vanno all'allievo._
* [x] INSERIMENTO RAPIDO ALLIEVO SE NON ESISTE (NOME E COGNOME), in modo da insere direttamente sul corso le persone e poi completare le informazioni _(completata 2026-10-19)_
  _Chiarimento 2026-10-19: in Partecipanti si scrive il nome nel campo "Aggiungi un allievo"; se non esiste si apre un mini-form con nome e cognome (obbligatori), email e telefono (facoltativi); Salva crea l'allievo (utente disattivato, senza genitore) e lo iscrive al corso; se esiste un omonimo lo segnala e permette di iscrivere quello esistente o crearne comunque uno nuovo; il resto si completa da Gestione Allievi._

### GIORNI DEGLI ALLIEVI

* [x] ho un caso di corso con due giorni a settimana e due allievi, ma un giorno fa un allievo e un giorno fa l'altro allievo. puoi implementare questa funzioanlità per gestirlo? _(completata 2026-10-19)_
  _Chiarimento 2026-10-19: per ogni partecipante si scelgono i giorni tra quelli del corso (default tutti) in Partecipanti; ogni lezione ha le presenze solo di chi frequenta quel giorno (rimborsi e bilancio seguono); "Numero lezioni" diventa il numero di lezioni per ogni allievo; cambiando i giorni si aggiornano solo le lezioni da fare; i giorni dell'allievo si vedono in I Miei Corsi, I Miei Figli, conferma di iscrizione e Riepilogo Iscritti._

### RICERCA RAPIDA

* [x] in admin e segreteria aggiungi una ricerca rapida per utenti, insegnanti, genitori, allievi, nelle liste in modo da cercare velocemente un nome, inserendo un testo cerca nei campi dell'oggetto dell'elenco _(completata 2026-10-19)_
  _Chiarimento 2026-10-19: campo "Cerca" sopra la tabella che filtra mentre si scrive ("N di M"), in tutti i dati della persona e nei collegamenti mostrati, senza distinguere maiuscole e accenti, con più parole tutte richieste; anche in Gestione Corsi e Riepilogo Iscritti._

### SCHEDE DAI RUOLI

* [x] ad un utente, che era inserito come allievo rapido, ho aasegnato il ruolo insegnante e genitore, ma non lo trovo negli elenco insegnante e genitori, sia che sia attivo che non _(completata 2026-10-19)_
  _Chiarimento 2026-10-19: salvando l'utente in Gestione Utenti con il ruolo INSEGNANTE o GENITORE, se manca la scheda viene creata in automatico (togliere il ruolo non la cancella); i dati anagrafici sono copiati da una scheda già esistente dello stesso utente (allievo, insegnante o genitore); all'avvio si creano le schede mancanti per chi ha già il ruolo._

## FUNZIONALITA del 20-10-2026

### GRUPPI

* [x] Il punto più critico é che ci sono corsi con gruppi per giorno della settimana di altri no. Quindi servirebbe modificare l'assegnazione di un giorno all'allievo con assegnazione ad un gruppo se c'è nel corso allora é assegnato ad un giorno. Non é obbligatorio quindi posso assegnare allevi ad un gruppo solo se ci sono gruppi nel corso. Secondo me vale la pena sostituire la funzionalità di assegnare un giorno ad un allievo con assegnare un gruppo ad un allievo se il corso lo prevede. _(completata 2026-10-20)_
  _Chiarimento 2026-10-20: ogni allievo appartiene al massimo a un gruppo del corso (facoltativo); senza gruppo frequenta tutti i giorni; assegnando il gruppo si propone il suo orario all'allievo (modificabile); i cambi aggiornano solo le lezioni da fare; i giorni già assegnati si convertono in gruppi all'avvio (un solo giorno → gruppo di quel giorno, più giorni → nessun gruppo); il gruppo si vede in Riepilogo Iscritti, I Miei Corsi / I Miei Figli e nella conferma di iscrizione._
* [x] Un gruppo ha quindi un possibile giorno tra quello del gruppo ed un orario di riferimento. _(completata 2026-10-20)_
  _Chiarimento 2026-10-20: un solo giorno tra quelli del corso, orario di riferimento e nome facoltativo (mostrato "Martedì 17:00" o "Principianti – mar 17:00")._
* [x] É possibile creare gruppi nel corso che vedono admin segreteria e anche insegnanti, anche se per gli insegnanti é irrilevante perché si trovano l'allievo nel giorno in cui deve fare il corso. _(completata 2026-10-20)_
  _Chiarimento 2026-10-20: i gruppi si gestiscono nel dialog Partecipanti da admin e segreteria (eliminazione solo admin, gli allievi restano senza gruppo); gli insegnanti vedono il gruppo dell'allievo in Gestisci Presenze, in sola lettura._
* [x] Aggiungi la visualizzazione del gruppo nel riepilogo iscritti in modo che siano chiari per ogni corso o gruppi. _(completata 2026-10-20)_
  _Chiarimento 2026-10-20: nei corsi con gruppi un livello in più corso → gruppi → iscritti (più "Senza gruppo (tutti i giorni)" se serve), i corsi senza gruppi restano come sono; la riga del gruppo mostra solo nome e numero di iscritti; all'avvio e con "Espandi tutti" sono aperti corsi e gruppi._

## FUNZIONALITA del 21-10-2026

### LOGIN

* [x] verifica che un utente possa effettivamente fare login con username o email _(completata 2026-10-21)_
  _Chiarimento 2026-10-21: oggi il login accetta solo lo username; si aggiunge l'email (senza distinguere maiuscole): se non è uno username si cerca tra gli account attivi con quell'email e si entra solo se è uno solo, altrimenti errore generico che suggerisce di usare il nome utente; campo "Nome utente o email"; lo username resta com'è._

### GRUPPI

* [x] Nei gruppi di di allievi di corso vorrei poter assegnare un nome oltre al giorno. Un caso d'uso potrebbe essere nel corso propedeutica che è i sabati ogni 15gg vorrei poter inserire due gruppi "piccoli" e "grandi" semplicemente i piccoli sono alle 10, i grandi alle 11 _(completata 2026-10-21)_
  _Chiarimento 2026-10-21: il nome del gruppo esiste già (campo "Nome" del gruppo, in Partecipanti; due gruppi possono avere lo stesso giorno): si aggiunge al corso la cadenza "Ogni settimana" (default) / "Ogni 2 settimane", con le settimane alterne contate dalla data di inizio; una lezione che cade in una festività si sposta alla settimana dopo e il ritmo riparte da lì; la cadenza si vede in Gestione Corsi._
* [x] Aggiungi la visualizzazione di quanti gruppi ha un corso dove necessario. _(completata 2026-10-21)_
  _Chiarimento 2026-10-21: numero di gruppi in Gestione Corsi (colonna "Gruppi", anche nella card) e in Riepilogo Iscritti sulla riga del corso._
* [x] Aggiungi anche un recap in dashboard _(completata 2026-10-21)_
  _Chiarimento 2026-10-21: riepilogo dei gruppi dei soli corsi attivi; admin e segreteria: sezione "Gruppi dei corsi attivi" con riquadri (corsi con gruppi, gruppi, allievi senza gruppo nei corsi con gruppi) e tabella con una riga per gruppo (corso, gruppo, iscritti); l'insegnante vede la stessa tabella per i suoi corsi._

## FUNZIONALITA del 22-10-2026

### PARTECIPANTI

* [x] nella vista dei partecipanti ai corsi metti come ordine predefinito il gruppo e poi l'orario se sono specificati in modo da vederli ordinati per gruppo prima e poi per orario, se non sono specificati usa l'ordine di inserimento anzichè l'ordine alfabetico che potrebbe presumbilmente rispettare la sequenza _(completata 2026-10-22)_
  _Chiarimento 2026-10-22: ordine = gruppo (per giorno, orario e nome; chi non ha gruppo in fondo), poi orario (chi non ce l'ha dopo), poi ordine di inserimento; si registra da ora la data di iscrizione, le iscrizioni già esistenti seguono l'ordine di creazione dell'allievo e vengono prima delle nuove; vale in Partecipanti, Gestisci Presenze e Riepilogo Iscritti._
* [x] sulla vista "gestione pagamenti" se filtro per corso vorrei che l'ordine dei partecipanti fosse coerente con quanto appena gestito, ovvero ordine per gruppo o orario se specificati in alternativa per inserimento _(completata 2026-10-22)_
  _Chiarimento 2026-10-22: in Gestione Pagamenti l'elenco resta per corso e dentro ogni corso segue l'ordine dei partecipanti (gruppo, orario, inserimento), con o senza filtro; le intestazioni di colonna riordinano ancora a mano; nuova colonna "Gruppo"; i Pagamenti del genitore restano per corso e nome._

### ALLIEVI

* [x] aggiungi un fitro per corso nell'elenco "gestione allievi" _(completata 2026-10-22)_
  _Chiarimento 2026-10-22: un corso alla volta tra tutti (prima gli attivi), indicato con l'anno scolastico, più la scelta "Senza corsi" per gli allievi non iscritti a nessun corso; si combina con la ricerca rapida._

### PAGAMENTI

* [x] nella finetra gestione pagamenti, in popup assegna in quota personalizzata dove inserisco il motivo aggiungi un suggeritore che propone altre motivazioni inserite in precedenza, lasciando possibilità di scrivere nuova motivazione, in modo semplice senza creare una tabella ma con i valori presenti in db _(completata 2026-10-22)_
  _Chiarimento 2026-10-22: il campo "Motivo" propone i motivi già usati nelle quote personalizzate di tutti i corsi, senza doppioni (maiuscole e spazi ignorati), prima i più usati; si può sempre scriverne uno nuovo; nessuna tabella nuova; la "Nota" dei pagamenti resta un campo libero._

### RIMBORSI

* [x] all'insegnante aggiungi un possibile rimborso una tantum che si aggiunge al costo orario se specificato, se sono specificati entrambi si sommano, se solo uno dei due si applica quello _(completata 2026-10-22)_
  _Chiarimento 2026-10-22: "una tantum" = rimborso forfettario annuo: un importo nella scheda dell'insegnante (impostato da admin e segreteria, in sola lettura nel Profilo dell'insegnante), valido per ogni anno scolastico in cui l'insegnante ha corsi; si somma al costo delle lezioni (tariffa oraria) ed è maturato per intero dall'inizio dell'anno (prospetto Rimborsi e Bilancio: maturato, da rimborsare, costo previsto); la proposta del rimborso mensile resta solo oraria (il forfait si registra a mano); non entra nel costo insegnante dei singoli corsi del Riepilogo Iscritti; se cambia vale anche per gli anni passati._

### DASHBOARD

* [x] in dashboard admin e segreteria aggiungi una card simile ad allievi per il conteggio non degli allievi ma degli iscritti, un allievo può fare piu corsi, qundi vorrei vedere sia il numero degli allievi sia il totale degli iscritti _(completata 2026-10-22)_
  _Chiarimento 2026-10-22: card "Iscritti" nel Riepilogo generale accanto ad "Allievi", con il numero di iscrizioni ai soli corsi attivi._
* [x] nella dashboard admin e segreteria, dove c'e' l'elenco dei corsi sotto le prime card, aggiungi due colonne con il costo stimato per l'insegnante o gli insegnanti del corso e quindi un mini prospetto tra quello che dovremmo incassare dagli iscritti e spendere per gli insegnanti per vedere semplicemente se il corso è in attivo o perdita _(completata 2026-10-22)_
  _Chiarimento 2026-10-22: nella tabella dei corsi attivi della dashboard due colonne: "Costo insegnanti" (costo previsto delle lezioni con la tariffa oraria, come nel Riepilogo Iscritti; "Senza tariffa" se nessun insegnante ha la tariffa; senza il forfait annuo) e "Margine" = quote dovute dagli iscritti − costo, con badge verde "Attivo" o rosso "Perdita"; due card con i totali "Costo insegnanti previsto" e "Margine previsto" (esclusi i corsi senza tariffa)._
* [x] nella dashboard GENITORE vorrei aggiungere anche una o due card di riepilogo dei pagamenti _(completata 2026-10-22)_
  _Chiarimento 2026-10-22: due card "Pagato" (con la percentuale della quota) e "Da pagare" (con la quota totale), per tutti i figli insieme e solo sui corsi attivi; non compaiono se i figli non hanno iscrizioni a corsi attivi._

### GENITORI

* [x] anche dalla scheda genitore vorrei poter associare una allievo, non solo dall'allievo. _(completata 2026-10-22)_
  _Chiarimento 2026-10-22: campo a scelta multipla "Figli" nel dialog Nuovo/Modifica Genitore, salvato con Salva (anche per un genitore nuovo); un allievo che ha già un altro genitore si può scegliere (compare "figlio di …") e passa a questo genitore, con un avviso al salvataggio; togliere un figlio lo lascia senza genitore._
* [x] Inoltre se sto creando un genitore genera automaticament username se lo lascio vuoto, come abbiamo già fatto in fase di inserimento veloce degli allievi _(completata 2026-10-22)_
  _Chiarimento 2026-10-22: come per gli allievi: username vuoto → generato da nome e cognome, utente disattivato con password casuale (accesso da attivare in Gestione Utenti), la password serve solo se si scrive lo username; vale anche per i nuovi insegnanti._
* [x] posso eliminare un profilo Genitore pur mantenendo l'utente se ha altri ruoli oltre a genitore? (alla proposta di un'azione "Elimina" nell'elenco Genitori che elimina solo la scheda: "si") _(completata 2026-10-22)_
  _Chiarimento 2026-10-22: azione "Elimina" nell'elenco Genitori, solo per l'admin, con conferma: elimina solo la scheda (mai l'utente, anche se GENITORE era il suo unico ruolo, con avviso), i figli restano senza genitore e all'utente si toglie il ruolo GENITORE; lo stesso per gli insegnanti, bloccato se l'insegnante ha corsi, lezioni o rimborsi (all'utente si toglie il ruolo INSEGNANTE)._

### ACCESSO

* [x] vorrei fare in modo che quando un utente viene creato che è disattivato non possa fare il login, ma se la persona richiede il reset password inserendo l'email e imposta una nuova password allora automaticamente si attiva, questo potrebbe sbloccare utenti nuovi creati con password casuali e inizialmente bloccati ma che poi ciascuno quando tenta di entrare, conoscendo la sua email riesce ad attivarsi autonomamente. _(completata 2026-10-22)_
  _Chiarimento 2026-10-22: un utente disattivato già oggi non può entrare; con il recupero password (username o email) può riattivarsi da solo solo chi non ha mai fatto un accesso (ultimo accesso vuoto): impostata la nuova password l'utente diventa attivo; chi è stato disattivato dopo aver usato l'app resta bloccato. Gli utenti già esistenti non hanno ancora l'ultimo accesso registrato e all'inizio contano come "mai entrati"._
* [x] inoltre sarebbe utile registrare ultimo login per ogni utente in modo da sapere quando è entrato l'ultima volta _(completata 2026-10-22)_
  _Chiarimento 2026-10-22: si registra data e ora dell'ultimo accesso riuscito (non i tentativi falliti), mostrato in Gestione Utenti (colonna "Ultimo accesso", "Mai" se vuoto, ordinabile, anche nella card)._

### RUOLI E SCHEDE

* [x] ad un utente ho aggiunto il ruolo Allievo ma apparentemente non mi ha creato la scheda allievo. Puoi controllare nella gestione utenti che se aggiungo o rimuovo ruoli venga creata la relativa scheda? _(completata 2026-10-22)_
  _Chiarimento 2026-10-22: oggi solo INSEGNANTE e GENITORE creano la scheda; anche il ruolo ALLIEVO crea la scheda allievo (dati anagrafici copiati da un'altra scheda dello stesso utente), e all'avvio si creano le schede mancanti (in produzione 1 utente); togliendo un ruolo la scheda resta (con iscrizioni, pagamenti, figli, lezioni) ed è segnalata come incongruenza._
* [x] c'e' modo di evidenziare incogruenze? _(completata 2026-10-22)_
  _Chiarimento 2026-10-22: in Gestione Utenti un badge di avviso sulla riga dell'utente per ogni incongruenza ("Allievo senza scheda", "Scheda genitore senza ruolo", …), un filtro "Solo incongruenze" e un pulsante che crea tutte le schede mancanti (non tocca le schede senza ruolo)._

### EMAIL

* [x] aggiunti un pulsante per vedere le anteprime di email di conferma o altre che partirebbero, rendi esplici i pulsanti di quali email andrebbero inviate _(completata 2026-10-22)_
  _Chiarimento 2026-10-22: anteprima (destinatario, oggetto, testo come nell'email e PDF allegato da aprire, senza inviare) accanto a ogni invio nel dialog del singolo iscritto (conferma di iscrizione, ricevuta) e nel dialog "invia conferme a tutti" (anteprima sul primo iscritto con email ed elenco di chi la riceve e di chi viene saltato); pulsanti rinominati: "Invia email" → "Email: conferma o ricevuta", "Invia conferme" → "Invia conferme di iscrizione a tutti"; niente anteprime delle email di sistema._
* [x] vorrei aggiugnere delle frasi per esplicitare alcuni concetti nellemail di conferma iscrizione, te le scrivo qui, rendiamole anche piu efficaci: "insegnante quando cancella deve recuperare o altro giorno o in coda al corso, per garantire il totale delle lezioni previste / se invece l'assenza è dello studente, può essere giustificata o non giustificata. se avvisa per tempo, quindi almeno una settimana prima si può valutare con insegnante se possibile fare un reciper se invece è a ridosso viene considerata non giustificata e quindi addebitata / in caso di rinucia al corso, non è possibile ottenere il rimborso per le lezioni non godude. Sono previde delle eccezioni che vanno valutate caso su caso a giudizio insindacabile della segreteria dei corsi." _(completata 2026-10-22)_
  _Chiarimento 2026-10-22: testo riscritto approvato, sezione "Lezioni, assenze e recuperi" con tre punti (lezione annullata dall'insegnante: sempre recuperata in un altro giorno o in coda al corso; assenza dell'allievo: giustificata se si avvisa la segreteria dei corsi almeno una settimana prima, con recupero da concordare con l'insegnante se possibile, altrimenti non giustificata, non recuperata e comunque dovuta; rinuncia: nessun rimborso delle lezioni non frequentate, eccezioni caso per caso a giudizio insindacabile della segreteria dei corsi), sia nell'email sia nel PDF di conferma._

## FUNZIONALITA del 23-10-2026

### QUOTA APPLICATA

* [x] vorrei rendere piu esplcito in fase di conferma iscrizione sia email che pdf ma eventualmente anche visualizzione in portale che il PREZZO PREDEFINTO del CORSO non è quello appplicato ma che è quello "da pagare" quello veritiero e realmente applicato. questo per enfatizzare che c'e' uno sconto applicato e non generare dubbi su quale sia l'importo effettivo dovuto. _(completata 2026-10-23)_
  _Chiarimento 2026-10-23: nella conferma e nella ricevuta (email e PDF), con uno sconto: prezzo di listino barrato, riga "Sconto applicato (motivo)" con l'importo, QUOTA DOVUTA in evidenza, già pagato, DA PAGARE, e la frase "L'importo dovuto è la quota indicata sopra, già scontata: il prezzo di listino è solo un riferimento."; senza sconto solo la quota dovuta; nel portale (I Miei Corsi, I Miei Figli) colonne "Prezzo di listino", "Pre-iscrizione", "Quota dovuta" in grassetto e badge "Sconto −X €"._

### NUMERO LEZIONE

* [x] vorrei sistemare il conteggio del numero di lezione, esempio nel caso in cui un corso ha due gruppi, uno il lun e uno il ven la lezione numero 1 è sia per il gruppo 1 che il gruppo 2 quella settimana, se un gruppo salta la lezione, in modo che entrambi i gruppo hanno 30 lezioni, il gruppo che ha saltato dovrà valutare di recuperarle per arrivare a 30. _(completata 2026-10-23)_
  _Chiarimento 2026-10-23: nei corsi con gruppi ogni giorno di gruppo ha la sua numerazione (lun 1..30, ven 1..30; due gruppi nello stesso giorno condividono la lezione); una lezione non effettuata tiene il suo numero e il recupero prende lo stesso numero; i corsi senza gruppi restano come oggi; si rinumerano tutte le lezioni dei corsi con gruppi, anche quelle svolte; accanto al numero si mostra il gruppo o il giorno (es. "5 · Piccoli", "5 · lun")._

### RICORDAMI

* [x] proponi un meccanismo di ricordami al login _(completata 2026-10-23)_
  _Chiarimento 2026-10-23: proposta approvata: casella "Ricordami su questo dispositivo" sotto il form di accesso, non spuntata di default; cookie firmato (meccanismo standard di Spring Security, nessuna tabella) valido 30 giorni, annullato al cambio password e all'uscita, non valido per gli utenti disattivati; chiave di firma dalla variabile d'ambiente `REMEMBER_ME_KEY`; il rientro automatico aggiorna l'ultimo accesso._

### CALENDARIO

* [x] per admin, segreteria, insegnanti, allievi aggiungi una vista "calendario" in cui si visualizzano i corsi secondo il livello corretto, ok una vista mensile, penso vaadin abbia un calendario mensile _(completata 2026-10-23)_
  _Chiarimento 2026-10-23: Vaadin 25.3 non ha un componente calendario: si realizza una griglia mensile (lun–dom) con mese precedente/successivo e "Oggi", su smartphone un elenco giorno per giorno; mostra le lezioni secondo il ruolo (admin e segreteria tutti i corsi attivi con filtro per corso, insegnante i suoi corsi, allievo le sue lezioni, anche i genitori le lezioni dei figli) con orario, corso, gruppo e stato, e le festività; cliccando una lezione i dettagli in sola lettura e, per insegnanti, admin e segreteria, "Gestisci presenze"._

### ALLIEVI ADULTI

* [x] mi servirebbe implementare qualcosa per cui un allievo che è adulto non deve passare per genitore, ma in qualche modo se è un adulto deve vedere se stesso e i pagamenti come fosse un genitore di se stesso _(completata 2026-10-23)_
  _Chiarimento 2026-10-23: "adulto" = allievo senza genitore (la regola già usata per email e documenti); vede la voce "Pagamenti" come il genitore (quota, pagato, residuo, rate, ricevute, solo per sé), le card "Pagato" e "Da pagare" in dashboard, lo stato del pagamento in I Miei Corsi e un riepilogo PDF per anno scolastico; chi ha un genitore non vede nulla di questo (i pagamenti restano al genitore)._

### PDF

* [x] vorrei che i generatori PDF fossero disponibili come anteprima INLINE su nuovo tab oltre che download, quindi i pulsanti di PDF potrebbero avere tipo il selettore ANTEPRIMA PDF o DOWNLOAD? _(completata 2026-10-23)_
  _Chiarimento 2026-10-23: un solo pulsante con un menu ("Anteprima in una nuova scheda" / "Scarica PDF") per tutti i PDF: ricevute (azione nelle righe e pulsante in Quota e pagamenti), riepilogo famiglia / allievo e allegato nell'anteprima delle email._

### PULSANTI

* [x] ha senso in mobile e desktop di modificare i pulsanti che sono solo icone con un button per enfatizzare che è un punto cliccabile, un button appunto? (alla proposta di applicarlo ovunque o solo su mobile: "everywhere") _(completata 2026-10-23)_
  _Chiarimento 2026-10-23: ovunque (desktop e mobile): i pulsanti con la sola icona (azioni nelle righe e nelle card, frecce del calendario, stampante delle ricevute) passano dallo stile "terziario" (solo icona) allo stile normale dei pulsanti Aura (fondo e bordo); i pulsanti con testo restano come sono; Elimina resta rosso._

### DATI PER RICEVUTE

* [ ] bisogna fare in modo che un GENITORE o un ALLIEVO ADULTO (un allievo senza genitore) possa COMPLETARE i propri dati obbligatori come ALLIEVO o COME GENITORE per poter stampare le ricevute

### DATA LEZIONE

* [x] permetti ad admin, segreteria, insegnante di modificare una data della lezione generata _(completata 2026-10-23)_
  _Chiarimento 2026-10-23: solo le lezioni da fare (generate o aggiunte); pulsante "Cambia data" nel dialog "Gestisci Presenze" con data e orario; restano gli stessi partecipanti e, nei corsi con gruppi, lo stesso numero; rifiutata se il corso ha già una lezione quel giorno, avviso se è festivo; "Genera Lezioni" conserva lo spostamento (data originale memorizzata, badge "Spostata dal …"); nessuna email._

### NOTE LEZIONE

* [x] aggiunti delle note che ADMIN, SEGRETERIA o INSEGNANTE possono aggiungere alla lezione oltre alle note che già si possono mettere su singolo allievo nella lezione _(completata 2026-10-23)_
  _Chiarimento 2026-10-23: due note per lezione, "Note per lo staff" (solo admin, segreteria e insegnanti del corso) e "Note per allievi e genitori"; si scrivono nel dialog "Gestisci Presenze" e si salvano subito, modificabili sempre (anche su lezioni completate o non effettuate) sui corsi attivi; allievi e genitori vedono le note pubbliche nel dettaglio della lezione del Calendario e nell'elenco delle presenze dell'allievo._
* [x] per favore sposta le note della lezione sia quelle per lo staff che per gli altri in fondo dopo l'elenco delle presenze prima dei pulsanti _(completata 2026-10-23)_
  _Chiarimento 2026-10-23: nel dialog "Gestisci Presenze" le due note vanno dopo l'elenco degli allievi, sotto un titolo "Note della lezione"; nella lezione non effettuata (che non ha l'elenco delle presenze) restano dove sono._

### SCORCIATOIA HOME

* [x] Vorrei aggiungere a vaadin il supporto per creare un link quando l'utente visualizza la web app per creare una scorciatoia in home mobile _(completata 2026-10-23)_
  _Chiarimento 2026-10-23: l'app diventa installabile (PWA) con icona quadrata ricavata dalla "M" del logo e nome "Corsi Mosson"; su smartphone un banner "Aggiungi alla schermata Home" (anche nella pagina di accesso), che una volta chiuso non ricompare su quel dispositivo, più la voce "Aggiungi alla Home" nel menu laterale, per tutti i ruoli; su Android apre l'installazione del browser, su iPhone mostra le istruzioni (Condividi → Aggiungi alla schermata Home); nulla se l'app è già aperta dalla scorciatoia o su computer; senza connessione una pagina "Sei offline" in italiano._

### I MIEI ALLIEVI

* [x] Per gli insegnanti vorrei fare una nuova sezione, i miei allievi, in cui si vedono gli allievi dei corsi e cliccando su vedono le informazioni sull'allievo che sono ad esempio le lezioni fatte, quante mancano e l'elenco delle lezioni _(completata 2026-10-23)_
  _Chiarimento 2026-10-23: voce di menu "I miei allievi" solo per gli insegnanti, in sola lettura; una riga per allievo e corso sui corsi attivi dell'insegnante (gruppo, orario, lezioni svolte/previste, mancanti, da fare), con filtro per corso e ricerca; nessun dato personale o contatto; cliccando, un dialog con svolte (lezioni completate, presente o assente), da fare (in calendario), mancanti (previste meno svolte), presenze e assenze, e l'elenco delle lezioni con le note (anche quelle per lo staff); lo stesso dialog, che allievi e genitori già vedono, mostra "Da fare" sulle lezioni non svolte e non le conta tra le presenze._

### DOCUMENTI

* [ ] Vorrei togliere tutti i pulsanti in giro per la generazione dei PDF e fare una sezione dedicata "documenti" in cui é possibile generare ed inviare o consultare in anteprima. E nel caso di genitori e allievi é l'area preposta per tutta la documentazione.
  _Chiarimento 2026-10-23: si tolgono dalle altre viste i pulsanti dei PDF (ricevuta, riepilogo famiglia/allievo) e gli invii email dei documenti (conferma o ricevuta, conferme a tutti); restano "Email inviate" e l'email di test. Per admin e segreteria "Documenti" è organizzata per tipo (conferme di iscrizione, ricevute, riepiloghi annuali) con filtri per anno scolastico e corso, ricerca, anteprima/scarica del PDF, invio email con anteprima, colonna "Ultimo invio" e, scelto un corso, l'invio delle conferme a tutti; il riepilogo annuale si può ora inviare per email (registrato in "Email inviate", serve uno script SQL su MySQL prima del deploy). Genitori e allievi senza genitore vedono conferme di iscrizione, ricevute (se ci sono pagamenti) e riepiloghi annuali di tutti i corsi, anche passati, solo in anteprima o da scaricare; l'allievo con genitore non ha la voce._
