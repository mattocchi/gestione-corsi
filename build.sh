#!/bin/bash
set -e

JAR=gestione-corsi-1.1.0.jar

# File frontend generati da zero: se un dev server o ./mvnw test li ha riscritti in modalità sviluppo,
# Copilot finirebbe nel bundle di produzione e la pagina resterebbe vuota (./mvnw clean non li cancella)
rm -f src/main/bundles/prod.bundle
rm -rf src/main/frontend/generated

./mvnw clean package -DskipTests

if unzip -l "target/$JAR" | grep -q 'VAADIN/build/copilot-'; then
  echo "ERRORE: il JAR contiene Copilot (codice di sviluppo): c'era un dev server o un test in esecuzione? Fermalo e rilancia." >&2
  exit 1
fi

cp "target/$JAR" release/

./mvnw clean
