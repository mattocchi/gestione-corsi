-- Migrazione: Studenti -> Allievi  (richiesta [2026-10-11/RICHIESTE.3])
--
-- Da eseguire sul database MySQL di produzione PRIMA di avviare la nuova versione.
-- Con spring.jpa.hibernate.ddl-auto=update Hibernate non rinomina tabelle e colonne:
-- senza questo script creerebbe tabelle `allievi` e `corso_allievi` vuote e colonne
-- `allievo_id` vuote, e gli utenti con ruolo STUDENTE non potrebbero più accedere ai loro corsi.
--
-- Richiede MySQL 8.0+ (RENAME COLUMN aggiorna anche le foreign key che usano la colonna).
-- Fare prima un backup:
--   mysqldump gestione_corsi > backup-prima-allievi.sql

START TRANSACTION;

-- Ruolo degli utenti (gli username restano invariati)
UPDATE user_roles SET role = 'ALLIEVO' WHERE role = 'STUDENTE';

COMMIT;

-- DDL (in MySQL ogni istruzione DDL fa commit implicito)
RENAME TABLE studenti TO allievi;
RENAME TABLE corso_studenti TO corso_allievi;
ALTER TABLE corso_allievi RENAME COLUMN studente_id TO allievo_id;
ALTER TABLE pagamenti RENAME COLUMN studente_id TO allievo_id;
ALTER TABLE presenze RENAME COLUMN studente_id TO allievo_id;

-- Le foreign key e gli indici esistenti seguono la rinomina. All'avvio Hibernate potrebbe
-- aggiungere una seconda foreign key o un indice equivalente con un nome diverso: è innocuo.

-- Verifica
SELECT COUNT(*) AS allievi FROM allievi;
SELECT COUNT(*) AS iscrizioni FROM corso_allievi WHERE allievo_id IS NOT NULL;
SELECT COUNT(*) AS pagamenti FROM pagamenti WHERE allievo_id IS NOT NULL;
SELECT COUNT(*) AS presenze FROM presenze WHERE allievo_id IS NOT NULL;
SELECT COUNT(*) AS utenti_allievo FROM user_roles WHERE role = 'ALLIEVO';
SELECT COUNT(*) AS utenti_studente_rimasti FROM user_roles WHERE role = 'STUDENTE';
