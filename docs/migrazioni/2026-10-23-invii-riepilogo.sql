-- Migrazione: invio per email del riepilogo annuale  (richiesta [F-2026-10-23/DOCUMENTI.1])
--
-- Da eseguire sul database MySQL di produzione PRIMA del deploy della nuova versione.
-- Hibernate aveva creato la colonna tipo di invii_email come ENUM('CONFERMA_ISCRIZIONE','RICEVUTA'): ddl-auto=update
-- non la modifica e senza questo script la registrazione dell'invio di un riepilogo (tipo RIEPILOGO) fallisce.
-- La colonna diventa testo, come la mappa ora l'entity; i valori esistenti restano gli stessi.
-- Le nuove colonne genitore_id e anno le aggiunge ddl-auto=update all'avvio.

ALTER TABLE invii_email MODIFY tipo VARCHAR(32) NOT NULL;

-- Verifica: column_type deve essere varchar(32)
SELECT column_name, column_type, is_nullable
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'invii_email' AND column_name = 'tipo';
