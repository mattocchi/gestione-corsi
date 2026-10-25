#!/bin/bash

# Trova la cartella corrente dello script
SCRIPT_DIR="$(dirname "$(readlink -f "$0")")"
ENV_FILE="$SCRIPT_DIR/gestione-corsi.env"

#source $ENV_FILE

## Caricamento sicuro del file .env (gestisce spazi, apici e caratteri speciali)
#if [ -f "$ENV_FILE" ]; then
#    while IFS= read -r line || [ -n "$line" ]; do
#        # Rimuove gli spazi iniziali e finali
#        line=$(echo "$line" | xargs)
#        # Ignora le righe vuote e i commenti
#        [[ -z "$line" || "$line" =~ ^# ]] && continue
#        # Estrae chiave e valore dividendo solo sul primo carattere '='
#        if [[ "$line" =~ ^([^=]+)=(.*)$ ]]; then
#            key="${BASH_REMATCH[1]}"
#            val="${BASH_REMATCH[2]}"
#            # Rimuove eventuali apici o virgolette esterne dal valore
#            val="${val%\"}"
#            val="${val#\"}"
#            val="${val%\'}"
#            val="${val#\'}"
#            # Esporta la variabile d'ambiente
#            export "$key"="$val"
#        fi
#    done < "$ENV_FILE"
#else
#    echo "Errore: File .env non trovato in $SCRIPT_DIR"
#    exit 1
#fi

# Configurazione
DB_USER="${DB_USERNAME:-root}"
DB_PASS="${DB_PASSWORD}"
#BACKUP_DIR="/var/backups/mysql"
DIR="$SCRIPT_DIR/backups" # Crea una sottocartella 'backups' nella folder corrente
DATE=$(date +%Y-%m-%d_%H-%M-%S)
BACKUP_FILE="$BACKUP_DIR/all_databases_$DATE.sql.gz"
DB_NAME=gestione_corsi

# Crea la cartella se non esiste
mkdir -p "$BACKUP_DIR"

## Esegue il dump e comprime il file
#mysqldump -u "$DB_USER" -p"$DB_PASS" --all-databases --single-transaction --quick | gzip > "$BACKUP_FILE"

# Esegue il backup (singolo DB o tutti)
if [ -n "$DB_NAME" ]; then
    FILE="$DIR/${DB_NAME}_$DATE.sql.gz"
    mysqldump -u "$USER" -p"$PASS" "$DB_NAME" --single-transaction --quick | gzip > "$FILE"
else
    FILE="$DIR/all_databases_$DATE.sql.gz"
    mysqldump -u "$USER" -p"$PASS" --all-databases --single-transaction --quick | gzip > "$FILE"
fi

# Rimuove i backup più vecchi di 7 giorni
find "$BACKUP_DIR" -type f -name "*.sql.gz" -mtime +7 -delete

echo "Backup completato con successo: $BACKUP_FILE"