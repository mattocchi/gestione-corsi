-- Script per creare il database e l'utente per gestione-corsi

-- Crea il database
CREATE DATABASE IF NOT EXISTS gestione_corsi
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

-- Crea l'utente per localhost (modifica la password 'your_password')
CREATE USER IF NOT EXISTS 'gestione_corsi_user'@'localhost' IDENTIFIED BY 'your_password';

-- Crea l'utente per connessioni remote (modifica la password 'your_password')
CREATE USER IF NOT EXISTS 'gestione_corsi_user'@'%' IDENTIFIED BY 'your_password';

-- Assegna i privilegi necessari all'utente localhost sul database
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, INDEX, ALTER, REFERENCES
ON gestione_corsi.*
TO 'gestione_corsi_user'@'localhost';

-- Assegna i privilegi necessari all'utente remoto sul database
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, INDEX, ALTER, REFERENCES
ON gestione_corsi.*
TO 'gestione_corsi_user'@'%';

-- Applica i cambiamenti
FLUSH PRIVILEGES;

-- Verifica i privilegi assegnati
SHOW GRANTS FOR 'gestione_corsi_user'@'localhost';
SHOW GRANTS FOR 'gestione_corsi_user'@'%';
