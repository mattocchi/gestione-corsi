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
* [ ] la visualizzazione dei Corsi deve mostrare l'elenco dei Partecipanti in ordine di Inizio della Lezione quanto è assegnato un orario di Lezione ad un Partecipante

### SEGRETERIA

* [ ] aggiungere il ruolo SEGRETERIA oltre a quelli esistenti
* [ ] Il ruolo segreteria deve poter Assegnare Partecipati ai Corsi
* [ ] Il ruolo segreteria deve poter deve poter assegnare l'orario di inizio della lezione ad ogni Partecipante del Corso
* [ ] Il ruolo Segreteria deve poter vedere tutti i Corsi, Studenti e Insegnanti, 
* [ ] Il ruolo Segreteria deve poter se necessario deve poter creare un nuovo Corso che assegna ad uno degli Insegnanti
* [ ] Il ruolo Segreteria deve poter assegnare la data di inizio del Corso
* [ ] Il ruolo Segreteria deve poter modificare il nome e cognome di un Studenti e creare uni Studenti che assegna ad uno dei Genitori
* [ ] il ruolo Segreteria deve poter vedere tutti i Genitori
* [ ] il ruolo Segreteria deve poter assegnare il costoTotale del corso 

### DASHBOARD INIZIALE

* [ ] la prima schermata dopo il login che attualmente è vuota per ogni ruolo, deve poter visualizzare delle informazioni di riepilogo in stile dashboard in funzione di ogni ruolo
* [ ] il ruolo Admin in dashboard vede il numero totale di corsi, il numero totale di allievi, il numero totale di genitori, il numero totale di insegnanti
* [ ] il ruolo Genitore in dashboard vede il numero totale dei corsi a cui sono iscritti i suoi figli
* [ ] il ruolo Insegnate in dashboard vede il numero totale dei corsi a lui assegnato, il numero totale di lezioni fatte e il numero totale di lezioni da fare di tutti i suoi corsi
* [ ] il ruolo Allievo in dashboard vede il numero totale dei suoi corsi