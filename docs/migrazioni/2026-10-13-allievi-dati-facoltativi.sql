-- Migrazione: codice fiscale e data di nascita dell'allievo facoltativi  (richiesta [F-2026-10-13/ALLIEVI.1])
--
-- Da eseguire sul database MySQL di produzione PRIMA o subito dopo il deploy della nuova versione:
-- ddl-auto=update non toglie il vincolo NOT NULL creato quando i campi erano obbligatori, e senza questo script
-- il salvataggio di un allievo senza codice fiscale o data di nascita fallisce sul database.
-- Nessun dato viene modificato.

ALTER TABLE allievi MODIFY codice_fiscale VARCHAR(255) NULL;
ALTER TABLE allievi MODIFY data_nascita DATE NULL;

-- Verifica: IS_NULLABLE deve essere YES per entrambe
SELECT column_name, is_nullable, column_type
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'allievi' AND column_name IN ('codice_fiscale', 'data_nascita');
