-- Numero di lezioni facoltativo se c'è la data di fine (2026-10-27).
-- ddl-auto=update aggiunge da solo la colonna corsi.data_fine, ma non toglie il NOT NULL da numero_lezioni.
-- Da eseguire su MySQL prima del deploy (i corsi esistenti hanno già il numero: nessun dato cambia).

ALTER TABLE corsi MODIFY numero_lezioni INT NULL;
