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

* [ ] Per ogni Partecipante al corso è necessario poter segnare la quota pagata. Quindi per ogni corso e per ogni partecipante admin, e segreteria devono poter aggiungere la quota pagata. Potenzialmente si può pagare la quota del corso in più rate quindi deve essere possibile inserire la data del pagamento e l'importo pagato, oltre al metodo che può essere bonifico o cassa. Default bonifico.

* [ ] Per ogni allievo del corso deve essere possibile indicare esattamente la quota da pagare del corso perché potrebbe differire dalla quota piena. Questo perché ci sono delle condizioni che permettono di applicare un pezzo personalizzato che ruolo admin o segreteria possono impostare

* [ ] Deve essere possibile impostare il costo orario di ogni insegnante inteso come euro all'ora che si può inserire nelle informazioni dell'insegnante e può essere inserito da admin o segreteria mentre solo in visualizzazione per ruolo insegnante.

* [ ] Insegnante quindi può gestire il proprio profilo aggiornando le proprie informazioni ad esclusione della tariffa oraria in futuro altri parametri che sono impostati solo da admin o segreteria. Per ogni insegnante deve essere aggiunta la possibilità di inserire indirizzo di residenza in formato italiano, data di nascita e luogo di nascita, e codice fiscale: in pratica tutti i dati anagrafici.

* [ ] Per ogni allievo deve essere possibile indicare tutti i dati anagrafici ed anche per ogni genitore, ciascuno le proprie informazioni, se un allievo non ha una informazione specifica per indirizzo è carino visualizzare quello del genitore.

* [ ] Nella dashboard segreteria ora è possibile visualizzare due nuove informazioni ovvero il totale delle quote che risultano pagante sia in termini di quanti hanno già pagato almeno una parte rispetto a quelli che devono pagare, quanti soldi risultano incassati e quanti ancora da incassare sia in valore assoluto che percentuale sul totale di tutti i corsi che dei singoli corsi attivi.

* [ ] Ogni corso fa riferimento ad uno specifico anno scolastico quindi ogni corso deve poter essere indicato a quale anno scolastico fa riferimento e se è attivo o non attivo. Per i corsi non attivi non è poi possibile inserire allievi (per segreteria) o completare lezioni e presenza per gli insegnanti. I corsi non attivi sono disponibili in consultazione solo a segreteria ed admin ed i genitori perché possono anche successivamente stampare la ricevuta del corso.