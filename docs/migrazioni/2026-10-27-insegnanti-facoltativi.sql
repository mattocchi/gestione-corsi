-- Fino a 3 insegnanti facoltativi per corso (2026-10-27).
-- Il secondo insegnante (corsi.secondo_insegnante_id) diventa il primo dei facoltativi nella nuova tabella
-- corso_insegnanti. Da eseguire su MySQL PRIMA del deploy della nuova versione.

CREATE TABLE IF NOT EXISTS corso_insegnanti (
    corso_id      BIGINT  NOT NULL,
    ordine        INTEGER NOT NULL,
    insegnante_id BIGINT  NOT NULL,
    PRIMARY KEY (corso_id, ordine),
    CONSTRAINT fk_corso_insegnanti_corso FOREIGN KEY (corso_id) REFERENCES corsi (id),
    CONSTRAINT fk_corso_insegnanti_insegnante FOREIGN KEY (insegnante_id) REFERENCES insegnanti (id)
);

INSERT INTO corso_insegnanti (corso_id, ordine, insegnante_id)
SELECT id, 0, secondo_insegnante_id
FROM corsi
WHERE secondo_insegnante_id IS NOT NULL
  AND secondo_insegnante_id <> insegnante_id
  AND id NOT IN (SELECT corso_id FROM corso_insegnanti);

-- La vecchia colonna non è più usata dall'applicazione: si svuota (resta per sicurezza; si può eliminare in seguito
-- con ALTER TABLE corsi DROP FOREIGN KEY <nome>, DROP COLUMN secondo_insegnante_id).
UPDATE corsi SET secondo_insegnante_id = NULL WHERE secondo_insegnante_id IS NOT NULL;
