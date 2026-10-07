#!/bin/bash
# chmod +x start.sh
# Richiede le variabili d'ambiente DB_PASSWORD (e opzionalmente DB_URL, DB_USERNAME)
java -jar gestione-corsi-1.1.0.jar --spring.profiles.active=prod
