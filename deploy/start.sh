#!/bin/bash
# Avvio manuale in produzione, senza pm2 (es. per provare la configurazione): stesse variabili e opzioni
# di ecosystem.config.js, lette da gestione-corsi.env accanto a questo script. Ctrl+C per fermare.
# chmod +x start.sh
set -euo pipefail

cd "$(dirname "$0")"
ENV_FILE=gestione-corsi.env
if [[ ! -f $ENV_FILE ]]; then
    echo "Manca $ENV_FILE: copia gestione-corsi.env.example in $ENV_FILE e compilalo" >&2
    exit 1
fi

# Lettura riga per riga (non "source"): i valori possono contenere & e ? (es. DB_URL)
while IFS= read -r riga || [[ -n $riga ]]; do
    if [[ $riga =~ ^[[:space:]]*(export[[:space:]]+)?([A-Za-z_][A-Za-z0-9_]*)[[:space:]]*=[[:space:]]*(.*)$ ]]; then
        nome="${BASH_REMATCH[2]}"
        valore="${BASH_REMATCH[3]%"${BASH_REMATCH[3]##*[![:space:]]}"}"
        if [[ $valore =~ ^\"(.*)\"$ || $valore =~ ^\'(.*)\'$ ]]; then
            valore="${BASH_REMATCH[1]}"
        fi
        export "$nome=$valore"
    fi
done < "$ENV_FILE"

if [[ -z ${DB_PASSWORD:-} ]]; then
    echo "DB_PASSWORD mancante in $ENV_FILE" >&2
    exit 1
fi

read -ra OPZIONI_JVM <<< "${JAVA_OPTS:--Xms256m -Xmx512m -XX:+ExitOnOutOfMemoryError -Dfile.encoding=UTF-8 -Duser.timezone=Europe/Rome}"
exec "${JAVA_BIN:-java}" "${OPZIONI_JVM[@]}" -jar "${APP_JAR:-gestione-corsi.jar}" --spring.profiles.active=prod
