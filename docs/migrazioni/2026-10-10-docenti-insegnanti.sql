-- Migrazione: Docenti -> Insegnanti  (richiesta [2026-10-10/INSEGNANTI.1])
--
-- Da eseguire sul database MySQL di produzione PRIMA di avviare la nuova versione.
-- Con spring.jpa.hibernate.ddl-auto=update Hibernate non rinomina tabelle e colonne:
-- senza questo script creerebbe una tabella `insegnanti` vuota e una colonna
-- `corsi.insegnante_id` vuota, e gli utenti con ruolo DOCENTE non vedrebbero più le loro lezioni.
--
-- Richiede MySQL 8.0+ (RENAME COLUMN). Fare prima un backup:
--   mysqldump gestione_corsi > backup-prima-insegnanti.sql

START TRANSACTION;

-- Ruolo degli utenti
UPDATE user_roles SET role = 'INSEGNANTE' WHERE role = 'DOCENTE';

COMMIT;

-- DDL (in MySQL ogni istruzione DDL fa commit implicito)
RENAME TABLE docenti TO insegnanti;
ALTER TABLE corsi RENAME COLUMN docente_id TO insegnante_id;

-- Le foreign key esistenti seguono la rinomina. All'avvio Hibernate potrebbe aggiungere
-- una seconda foreign key equivalente con un nome diverso: è innocua.

-- Verifica
SELECT COUNT(*) AS insegnanti FROM insegnanti;
SELECT COUNT(*) AS corsi_con_insegnante FROM corsi WHERE insegnante_id IS NOT NULL;
SELECT COUNT(*) AS utenti_insegnante FROM user_roles WHERE role = 'INSEGNANTE';
