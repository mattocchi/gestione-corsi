-- Migrazione: anno scolastico dei corsi esistenti  (richiesta [2026-10-10/REV1.7])
--
-- Da eseguire sul database MySQL di produzione DOPO il primo avvio della nuova versione:
-- la colonna corsi.anno_scolastico viene creata da Hibernate (ddl-auto=update) all'avvio.
-- L'anno è ricavato dalla data di inizio: da settembre è già l'anno successivo.
-- La colonna corsi.attivo esiste già (default 1): i corsi esistenti restano attivi.

UPDATE corsi
SET anno_scolastico = CASE
        WHEN MONTH(data_inizio) >= 9 THEN CONCAT(YEAR(data_inizio), '/', YEAR(data_inizio) + 1)
        ELSE CONCAT(YEAR(data_inizio) - 1, '/', YEAR(data_inizio))
    END
WHERE anno_scolastico IS NULL;

-- Verifica
SELECT id, nome, data_inizio, anno_scolastico, attivo FROM corsi ORDER BY data_inizio;
