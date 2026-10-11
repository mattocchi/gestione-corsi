# RICHIESTE IMPLEMENTAZIONI

## RICHIESTE del 10-10-2026

### LOGO

* [x] alla schermata di Login, deve essere visualizzato un logo, responsive, al centro sopra i dati di login _(completata 2026-10-10)_
* [x] il Logo deve essere visibile anche in basso nella barra di menu sia desktop che mobile (sempre responsive) _(completata 2026-10-10)_
* [x] il logo da usare è nel repository nella cartella docs/loghi/MOSSON-Logo-h150.png _(completata 2026-10-10)_

### Docenti diventano Insegnantei

* [x] Rinominiamo Docenti in Insegnanti: adegua sia UI che Codice che dati di Esempio _(completata 2026-10-10; in produzione eseguire prima docs/migrazioni/2026-10-10-docenti-insegnanti.sql)_

### CORSI

* [x] un corso deve poter avere una data di inizio _(già presente, verificata 2026-10-10: campo "Data Inizio" nel dialog del corso e colonna in Gestione Corsi)_
* [ ] un corso deve poter avere 
  _(voce incompleta: da specificare, non implementata)_
* [x] la visualizzazione dei Corsi deve mostrare l'elenco dei Partecipanti in ordine di Inizio della Lezione quanto è assegnato un orario di Lezione ad un Partecipante _(completata 2026-10-10)_

### SEGRETERIA

* [x] aggiungere il ruolo SEGRETERIA oltre a quelli esistenti _(completata 2026-10-10)_
* [x] Il ruolo segreteria deve poter Assegnare Partecipati ai Corsi _(completata 2026-10-10)_
* [x] Il ruolo segreteria deve poter deve poter assegnare l'orario di inizio della lezione ad ogni Partecipante del Corso _(completata 2026-10-10)_
* [x] Il ruolo Segreteria deve poter vedere tutti i Corsi, Studenti e Insegnanti, _(completata 2026-10-10)_
* [x] Il ruolo Segreteria deve poter se necessario deve poter creare un nuovo Corso che assegna ad uno degli Insegnanti _(completata 2026-10-10)_
* [x] Il ruolo Segreteria deve poter assegnare la data di inizio del Corso _(completata 2026-10-10)_
* [x] Il ruolo Segreteria deve poter modificare il nome e cognome di un Studenti e creare uni Studenti che assegna ad uno dei Genitori _(completata 2026-10-10)_
* [x] il ruolo Segreteria deve poter vedere tutti i Genitori _(completata 2026-10-10)_
* [x] il ruolo Segreteria deve poter assegnare il costoTotale del corso _(completata 2026-10-10)_

### DASHBOARD INIZIALE

* [x] la prima schermata dopo il login che attualmente è vuota per ogni ruolo, deve poter visualizzare delle informazioni di riepilogo in stile dashboard in funzione di ogni ruolo _(completata 2026-10-10)_
* [x] il ruolo Admin in dashboard vede il numero totale di corsi, il numero totale di allievi, il numero totale di genitori, il numero totale di insegnanti _(completata 2026-10-10)_
* [x] il ruolo Genitore in dashboard vede il numero totale dei corsi a cui sono iscritti i suoi figli _(completata 2026-10-10)_
* [x] il ruolo Insegnate in dashboard vede il numero totale dei corsi a lui assegnato, il numero totale di lezioni fatte e il numero totale di lezioni da fare di tutti i suoi corsi _(completata 2026-10-10)_
* [x] il ruolo Allievo in dashboard vede il numero totale dei suoi corsi _(completata 2026-10-10)_

## RICHIESTE del 10-10-2026 Revisione 1

* [x] Per ogni Partecipante al corso è necessario poter segnare la quota pagata. Quindi per ogni corso e per ogni partecipante admin, e segreteria devono poter aggiungere la quota pagata. Potenzialmente si può pagare la quota del corso in più rate quindi deve essere possibile inserire la data del pagamento e l'importo pagato, oltre al metodo che può essere bonifico o cassa. Default bonifico. _(completata 2026-10-10)_
  _Chiarimento 2026-10-10: rate gestite da admin e segreteria nel dialog "Quota e pagamenti" del partecipante (dal dialog Partecipanti); aggiungi/modifica/elimina; data (default oggi), importo, metodo Bonifico/Cassa (default Bonifico), nota facoltativa. Il genitore vede quota, pagato, residuo e rate. Partecipante/corso/allievo con pagamenti non eliminabile (avviso)._

* [x] Per ogni allievo del corso deve essere possibile indicare esattamente la quota da pagare del corso perché potrebbe differire dalla quota piena. Questo perché ci sono delle condizioni che permettono di applicare un pezzo personalizzato che ruolo admin o segreteria possono impostare _(completata 2026-10-10)_
  _Chiarimento 2026-10-10: quota personalizzata facoltativa con nota del motivo, nello stesso dialog; vuota = quota piena del corso (segue le modifiche del costo del corso)._

* [x] Deve essere possibile impostare il costo orario di ogni insegnante inteso come euro all'ora che si può inserire nelle informazioni dell'insegnante e può essere inserito da admin o segreteria mentre solo in visualizzazione per ruolo insegnante. _(completata 2026-10-10)_
  _Chiarimento 2026-10-10: la tariffa è solo un dato (nessun calcolo di compensi); impostata da admin/segreteria nella vista Insegnanti, visibile all'insegnante in "Il mio profilo"._

* [x] Insegnante quindi può gestire il proprio profilo aggiornando le proprie informazioni ad esclusione della tariffa oraria in futuro altri parametri che sono impostati solo da admin o segreteria. Per ogni insegnante deve essere aggiunta la possibilità di inserire indirizzo di residenza in formato italiano, data di nascita e luogo di nascita, e codice fiscale: in pratica tutti i dati anagrafici. _(completata 2026-10-10)_
  _Chiarimento 2026-10-10: indirizzo = via e civico, CAP (5 cifre), Comune, Provincia (sigla); codice fiscale con controllo di formato e carattere di controllo. "Il mio profilo": anagrafica, contatti, specializzazione, biografia e cambio password; username e tariffa in sola lettura. Admin e segreteria creano e modificano gli insegnanti dalla vista Insegnanti._

* [x] Per ogni allievo deve essere possibile indicare tutti i dati anagrafici ed anche per ogni genitore, ciascuno le proprie informazioni, se un allievo non ha una informazione specifica per indirizzo è carino visualizzare quello del genitore. _(completata 2026-10-10)_
  _Chiarimento 2026-10-10: admin e segreteria creano/modificano allievi e genitori; il genitore ha "Il mio profilo" e può aggiornare i dati dei figli; l'allievo vede il proprio profilo in sola lettura. Se l'allievo non ha nessun campo dell'indirizzo si mostra quello del genitore "(del genitore)" in Gestione Studenti, nel profilo e nella ricevuta (senza copiarlo)._

* [x] Nella dashboard segreteria ora è possibile visualizzare due nuove informazioni ovvero il totale delle quote che risultano pagante sia in termini di quanti hanno già pagato almeno una parte rispetto a quelli che devono pagare, quanti soldi risultano incassati e quanti ancora da incassare sia in valore assoluto che percentuale sul totale di tutti i corsi che dei singoli corsi attivi. _(completata 2026-10-10)_
  _Chiarimento 2026-10-10: visibile ad admin e segreteria, calcolato sui corsi attivi: partecipanti saldati / con pagamento parziale / senza pagamenti, incassato e da incassare in € e %; riquadri con i totali e griglia con una riga per corso attivo._

* [x] Ogni corso fa riferimento ad uno specifico anno scolastico quindi ogni corso deve poter essere indicato a quale anno scolastico fa riferimento e se è attivo o non attivo. Per i corsi non attivi non è poi possibile inserire allievi (per segreteria) o completare lezioni e presenza per gli insegnanti. I corsi non attivi sono disponibili in consultazione solo a segreteria ed admin ed i genitori perché possono anche successivamente stampare la ricevuta del corso. _(completata 2026-10-10)_
  _Chiarimento 2026-10-10: anno scolastico "2026/2027" da elenco, obbligatorio, proposto dalla data di inizio; corsi esistenti calcolati con script SQL. Corso non attivo: bloccati partecipanti, orari, Genera Lezioni, presenze e completamento lezioni; quote e pagamenti restano modificabili. Insegnante e allievo non vedono i corsi non attivi. Filtri per stato (default Attivi) e anno in Gestione Corsi. Ricevuta come pagina stampabile al posto del riepilogo .txt._


## RICHIESTE del 11-10-2026

* [ ] Localizzazione in italiano del calendario e degli orari. Verifica che tutti gli input Calendario e Orario funzionino correttamente in UI in formato Italiano.
  _Chiarimento 2026-10-11: lingua italiana per tutta l'app; calendario in italiano (mesi, giorni, Oggi/Annulla) con settimana dal lunedì, data sempre gg/mm/aaaa (accettato anche g/m/aa), ora a 24 ore; vale per tutti i campi data/ora, esistenti e futuri._

* [] Aggiungi una sezione "gestione pagamenti" dedicata ai pagamenti per semplificare inserimento e gestione dei dati in cui mediante elenco corso, partecipante possiamo come admin o segreteria assegnare il prezzo effettivo del corso o aggiungere i pagamenti. Le funzionalità sono già disponibili questa è solo una visualizzazione e gestione semplificata. Il ruolo genitore può visualizzare questa sezione per i propri figli.
Per admin e segreteria nella sezione aggiungi dei filtri per poter filtrare per corso o per allievo o per genitore in modo da semplificare l'operatività.
  _Chiarimento 2026-10-11: voce "Gestione Pagamenti" dopo Gestione Corsi per admin e segreteria; una riga per partecipante e corso (corso, allievo, genitore, quota, pagato, residuo, stato) con "Aggiungi pagamento", "Quota e pagamenti" e "Ricevuta"; filtri per corso, allievo, genitore, stato del corso (default Attivi), stato del pagamento e anno scolastico; totali dei partecipanti filtrati. Il genitore ha la voce "Pagamenti" con i soli figli (anche corsi non attivi) in sola lettura, con dettaglio rate e ricevuta._

* [x] Rinomina Studente in Allievo che ritengo più adeguato per questo contesto _(completata 2026-10-11; in produzione eseguire prima docs/migrazioni/2026-10-11-studenti-allievi.sql)_
  _Chiarimento 2026-10-11: rinomina completa come per Insegnanti: interfaccia, codice, route /allievi, ruolo ALLIEVO, dati di esempio e database (allievi, corso_allievi, allievo_id); in produzione eseguire prima docs/migrazioni/2026-10-11-studenti-allievi.sql. Username esistenti invariati._
